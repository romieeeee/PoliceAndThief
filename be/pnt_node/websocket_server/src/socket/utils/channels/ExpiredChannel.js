import { RedisClient } from "../client/RedisClient.js";

const redisClient = new RedisClient();

const expiredChannel = async (message, pubClient, chatIo, readyRoomIo, gameIo) => {

    const key = message;

    // Key format: websocket:reconnect:timer:<namespace>:<roomId>:<userId>
    // Example: websocket:reconnect:timer:chat:123:user456
    if (key.startsWith(redisClient.RECONNECT_PREFIX)) {
        const parts = key.split(":");
        const namespace = parts[3];
        const roomId = parts[4];
        const memberId = parts[5];

        // [중복 방지 핵심] 
        // "내가 처리할게"라고 Lock을 걸어봄. (setNX: 없으면 세팅하고 true, 있으면 false)
        // 락 자체도 5초 뒤에 사라지게 설정 (데드락 방지)
        const isMine = await redisClient.setReconnectLock(memberId);

        if (isMine) {
            console.log(`[Process ${process.pid}] Winner! Handling disconnect for ${memberId}`);

            if (namespace === "chat") {
                chatDisconnect(memberId, roomId, chatIo, pubClient);
            } else if (namespace === "readyRoom") {
                readyRoomDisconnect(memberId, roomId, readyRoomIo, pubClient);
            } else if (namespace === "game") {
                gameDisconnect(memberId, roomId, gameIo, pubClient);
            }
        } else {
            // console.log(`[Process ${process.pid}] User ${memberId} expired, but handled by another process.`);
        }
    }
}

const chatDisconnect = async (memberId, roomId, chatIo) => {
    await redisClient.deleteKeys("chat", roomId, memberId);

    console.log("user_left", { memberId, roomId });

    chatIo.to(roomId).emit("user left", { memberId });
}

const readyRoomDisconnect = async (memberId, roomId, readyRoomIo) => {
    await redisClient.deleteKeys("readyRoom", roomId, memberId);

    readyRoomIo.to(roomId).emit("user left", { memberId });
}

const gameDisconnect = async (memberId, roomId, gameIo) => {
    await redisClient.deleteKeys("game", roomId, memberId);

    // RedisClient를 사용하여 위치 정보 및 패널티 정보 삭제
    await redisClient.deleteLocation(memberId, roomId);

    gameIo.to(roomId).emit("user left", { memberId });
}


export default expiredChannel;