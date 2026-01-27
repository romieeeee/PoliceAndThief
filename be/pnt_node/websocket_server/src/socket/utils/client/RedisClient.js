import redisDB from "../../../global/db/redis/RedisDB.js";

export class RedisClient {
    constructor() {
        this.pubClient = redisDB.getPubClient();
        this.subClient = redisDB.getSubClient();
        this.RECONNECT_PREFIX = "websocket:reconnect:timer:";

    }

    /**
     * 웹소켓 연결 관련 레디스 캐시
     */
    setReconnectLock = async (memberId) => {
        return await this.pubClient.set(`websocket:reconnect:lock:${memberId}`, "locked", "NX", "EX", 5);
    }

    deleteKeys = async (namespace, roomId, memberId) => {
        await this.pubClient.del(`websocket:reconnect:lock:${memberId}`);
        await this.pubClient.del(`websocket:reconnect:info:${namespace}:${memberId}`);
        await this.pubClient.del(`websocket:reconnect:timer:${namespace}:${roomId}:${memberId}`);
    }

    deleteByCompletedReconnect = async (socket, namespace, storedRoomId) => {
        console.log(`[Reconnect] Restoring user ${socket.data.memberId} to room ${storedRoomId}`);
        socket.join(storedRoomId);

        if (namespace === 'chat') {
            socket.data.chatRoomId = storedRoomId;
        } else if (namespace === 'game') {
            socket.data.gameId = storedRoomId;
        }

        // Delete keys to cancel expiration event
        const timerKey = `websocket:reconnect:timer:${namespace}:${storedRoomId}:${socket.data.memberId}`;
        const infoKey = `websocket:reconnect:info:${namespace}:${socket.data.memberId}`;

        await this.pubClient.del(infoKey);
        await this.pubClient.del(timerKey);
    }

    getStoredRoomId = async (socket, namespace) => {
        const infoKey = `websocket:reconnect:info:${namespace}:${socket.data.memberId}`;
        const storedRoomId = await this.pubClient.get(infoKey);
        return storedRoomId;
    }

    pubReconnectTimer = async (namespace, socket, roomId) => {
        const timerKey = `websocket:reconnect:timer:${namespace}:${roomId}:${socket.data.memberId}`;
        const infoKey = `websocket:reconnect:info:${namespace}:${socket.data.memberId}`;

        // 1. Timer Key: Expiration event trigger (Value not important)
        await this.pubClient.set(timerKey, "timer", "EX", 60);

        // 2. Info Key: Data storage for reconnection (Value = chatRoomId)
        // Set to 61s to ensure it survives slightly longer than the timer (race condition safety)
        await this.pubClient.set(infoKey, roomId, "EX", 60);
    }


    /**
     * 게임 관련 레디스 캐시
     */
    setLocation = async (memberId, gameId, location) => {
        await this.pubClient.hset(this.getLocationKeyString(gameId), memberId, JSON.stringify(location));
    }

    getAllLocations = async (gameId) => {
        const locations = await this.pubClient.hgetall(this.getLocationKeyString(gameId));
        return Object.values(locations).map((location) => JSON.parse(location));
    }

    deleteLocation = async (memberId, gameId) => {
        await this.pubClient.hdel(this.getLocationKeyString(gameId), memberId);
    }

    // 경계선 밖으로 나갔을 시에 10초 동안 패널티 부여 안함.
    increasePenalty = async (memberId, gameId) => {
        const lockKey = this.getPenaltyLockString(gameId, memberId);
        const isLocked = await this.pubClient.get(lockKey);

        if (isLocked) {
            return;
        }

        const res = await this.pubClient.hincrby(this.getPenaltyKeyString(gameId), memberId, 1);
        await this.pubClient.set(lockKey, "1", "EX", 10);
        return res;
    }

    getPenalty = async (memberId, gameId) => {
        const penalty = await this.pubClient.hget(this.getPenaltyKeyString(gameId), memberId);
        return parseInt(penalty);
    }

    deletePenalty = async (memberId, gameId) => {
        await this.pubClient.hdel(this.getPenaltyKeyString(gameId), memberId);
    }

    deleteAllLocations = async (gameId) => {
        await this.pubClient.del(this.getLocationKeyString(gameId));
    }

    // 이 함수는 게임이 종료됐을때만 실행.
    deleteGameCaches = async (memberId, gameId) => {
        // 게임에서 쓰는 redis cache들 삭제 (내 위치 정보 삭제, 경계 벗어남 패널티 횟수 관리 정보 삭제)
        await this.pubClient.del(this.getLocationKeyString(gameId, memberId));
        await this.pubClient.del(this.getPenaltyKeyString(gameId, memberId));
    }

    getLocationKeyString = (gameId, memberId) => {
        if (memberId) {
            return `room:${gameId}:locations:${memberId}`;
        }
        return `room:${gameId}:locations`;
    }

    getPenaltyKeyString = (gameId, memberId) => {
        if (memberId) {
            return `room:${gameId}:penalty:${memberId}`;
        }

        return `room:${gameId}:penalty`;
    }

    getPenaltyLockString = (gameId, memberId) => {
        return `room:${gameId}:penalty:lock:${memberId}`;
    }
}