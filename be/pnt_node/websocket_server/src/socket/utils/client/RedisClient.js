import redisDB from "../../../global/db/redis/RedisDB.js";
import logger from "../../../global/config/logger.js";

export class RedisClient {
    constructor() {
        this.pubClient = redisDB.getPubClient();
        this.subClient = redisDB.getSubClient();
        this.RECONNECT_PREFIX = "websocket:reconnect:timer:";
        this.GAME_TIMER_PREFIX = "room:game:timer:";
        this.CCTV_TIMER_PREFIX = "room:game:cctv:";
        this.GAME_END_PREFIX = "room:game:end:";
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
        logger.info(`[Reconnect] Restoring user ${socket.data.memberId} to room ${storedRoomId}`);
        socket.join(storedRoomId);

        if (namespace === 'chat') {
            socket.data.chatRoomId = storedRoomId;
        } else if (namespace === 'game') {
            socket.data.gameId = storedRoomId;
        } else if (namespace === 'room') {
            socket.data.roomId = storedRoomId;
        }

        // Delete keys to cancel expiration event
        const timerKey = `websocket:reconnect:timer:${namespace}:${storedRoomId}:${socket.data.memberId}`;
        const infoKey = `websocket:reconnect:info:${namespace}:${socket.data.memberId}`;

        await this.pubClient.del(infoKey);
        await this.pubClient.del(timerKey);
    }

    // ... (skipping unchanged code) ...

    setGameSettingLock = async (gameId, time) => {
        const duration = parseInt(time);
        if (isNaN(duration)) {
            logger.warn(`[RedisClient] Invalid duration for GameSettingLock: ${time}. Defaulting to 3600s.`);
            return await this.pubClient.set(`room:game:setting:lock:${gameId}`, "locked", "NX", "EX", 5);
        }
        return await this.pubClient.set(`room:game:setting:lock:${gameId}`, "locked", "NX", "EX", duration);
    }

    // ... (skipping unchanged code) ...

    increasePenalty = async (memberId, gameId) => {
        const lockKey = this.getPenaltyLockString(gameId, memberId);
        const isLocked = await this.pubClient.get(lockKey);

        if (isLocked) {
            return;
        }

        const res = await this.pubClient.hincrby(this.getPenaltyKeyString(gameId), memberId, 1);
        await this.pubClient.set(lockKey, "1", "EX", 10);
        logger.info(`패널티 부여, member=${memberId}`);
        return res;
    }

    // ... (skipping unchanged code) ...

    setGameTimer = async (gameId, time) => {
        const duration = parseInt(time);
        if (isNaN(duration)) {
            logger.warn(`[RedisClient] Invalid duration for GameTimer: ${time}. Defaulting to 600s.`);
            return await this.pubClient.set(this.getGameTimerKeyString(gameId), Date.now().toString(), "EX", 600);
        }
        return await this.pubClient.set(this.getGameTimerKeyString(gameId), Date.now().toString(), "EX", duration);
    }

    // ... (skipping unchanged code) ...

    setCctvTimer = async (gameId, time) => {
        const duration = parseInt(time);
        if (isNaN(duration)) {
            logger.warn(`[RedisClient] Invalid duration for CctvTimer: ${time}. Defaulting to 60s.`);
            return await this.pubClient.set(this.getCctvTimerKeyString(gameId), "timer", "EX", 60);
        }
        return await this.pubClient.set(this.getCctvTimerKeyString(gameId), "timer", "EX", duration);
    }

    deleteCctvTimer = async (gameId) => {
        await this.pubClient.del(this.getCctvTimerKeyString(gameId));
    }

    getCctvTimerKeyString = (gameId) => {
        return `room:game:cctv:${gameId}`;
    }

    /**
     * 게임에 관한 모든 캐시 삭제
     * 
     * 개발때 쓸
     * 
     * 게임이 종료되고 
     */
    deleteAllGameCachesByGameId = async (gameId) => {
        await this.deleteAllLocations(gameId);
        await this.pubClient.del(this.getPenaltyKeyString(gameId));
        await this.pubClient.del(this.getGameSettingString(gameId));
        await this.pubClient.del(this.getGameSettingLockString(gameId));
        await this.deleteGameTimer(gameId);
        await this.deleteGameTimerLock(gameId);
        await this.pubClient.del(this.getStartedString(gameId));
        await this.deleteStartedCount(gameId);
        await this.deleteCctvTimer(gameId);
    }

    /**
     * 게임 종료 후 1분 동안만 유지
     */
    setGameEnd = async (gameId) => {
        await this.pubClient.set(this.getGameEndKeyString(gameId), "end", "EX", 60);
    }

    getGameEnd = async (gameId) => {
        const gameEnd = await this.pubClient.get(this.getGameEndKeyString(gameId));
        return gameEnd;
    }

    deleteGameEnd = async (gameId) => {
        await this.pubClient.del(this.getGameEndKeyString(gameId));
    }

    getGameEndKeyString = (gameId) => {
        return `room:game:end:${gameId}`;
    }

    getGameTimerLock = async (gameId) => {
        return await this.pubClient.get(this.getGameTimerLockKeyString(gameId));
    }

    getGameTimerKeyString = (gameId) => {
        return `room:game:timer:${gameId}`;
    }

    getGameTimerLockKeyString = (gameId) => {
        return `room:game:timer:lock:${gameId}`;
    }


    getPenaltyKeyString = (gameId, memberId) => {
        if (memberId) {
            return `room:game:penalty:${gameId}:${memberId}`;
        }
        return `room:game:penalty:${gameId}`;
    }

    getPenaltyLockString = (gameId, memberId) => {
        return `room:game:penalty:lock:${gameId}:${memberId}`;
    }

    getGameSettingString = (gameId) => {
        return `room:game:setting:${gameId}`;
    }

    getGameSettingLockString = (gameId) => {
        return `room:game:setting:lock:${gameId}`;
    }

    getStartedString = (gameId) => {
        return `room:game:started:${gameId}`;
    }
}