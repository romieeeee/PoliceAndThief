const expiredChannel = async (message, pubClient, chatIo, readyRoomIo, gameIo) => {

    const key = message;

    // Key format: websocket:reconnect:timer:<namespace>:<roomId>:<userId>
    // Example: websocket:reconnect:timer:chat:123:user456
    if (key.startsWith("websocket:reconnect:timer:")) {
        const parts = key.split(":");
        const namespace = parts[3];
        const roomId = parts[4];
        const userId = parts[5];

        // [중복 방지 핵심] 
        // "내가 처리할게"라고 Lock을 걸어봄. (setNX: 없으면 세팅하고 true, 있으면 false)
        // 락 자체도 5초 뒤에 사라지게 설정 (데드락 방지)
        const isMine = await pubClient.set(`websocket:reconnect:lock:${userId}`, "locked", "NX", "EX", 5);

        if (isMine) {
            console.log(`[Process ${process.pid}] Winner! Handling disconnect for ${userId}`);

            if (namespace === "chat") {
                chatDisconnect(userId, roomId, chatIo, pubClient);
            } else if (namespace === "readyRoom") {
                readyRoomDisconnect(userId, roomId, readyRoomIo, pubClient);
            } else if (namespace === "game") {
                gameDisconnect(userId, roomId, gameIo, pubClient);
            }
        } else {
            console.log(`[Process ${process.pid}] User ${userId} expired, but handled by another process.`);
        }
    }
}

const chatDisconnect = async (userId, roomId, chatIo, pubClient) => {
    await deleteKeys(userId, roomId, "chat", pubClient);

    console.log("user_left", { userId, roomId });

    chatIo.to(roomId).emit("user left", { userId });
}

const readyRoomDisconnect = async (userId, roomId, readyRoomIo, pubClient) => {
    await deleteKeys(userId, roomId, "readyRoom", pubClient);
    readyRoomIo.to(roomId).emit("user left", { userId });
}

const gameDisconnect = async (userId, roomId, gameIo, pubClient) => {
    await deleteKeys(userId, roomId, "game", pubClient);

    gameIo.to(roomId).emit("user left", { userId });
}

const deleteKeys = async (userId, roomId, namespace, pubClient) => {
    await pubClient.del(`websocket:reconnect:lock:${userId}`);
    await pubClient.del(`websocket:reconnect:info:${namespace}:${userId}`);
    await pubClient.del(`websocket:reconnect:timer:${namespace}:${roomId}:${userId}`);
}

export default expiredChannel;