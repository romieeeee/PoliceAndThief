const expiredChannel = async (message, pubClient, chatIo, readyRoomIo, gameIo) => {

    const key = message;

    // Key format: websocket:reconnect:timer:<namespace>:<roomId>:<userId>
    // Example: websocket:reconnect:timer:chat:123:user456
    if (key.startsWith("websocket:reconnect:timer:")) {
        const parts = key.split(":");
        const namespace = parts[3];
        const roomId = parts[4];
        const memberId = parts[5];

        // [중복 방지 핵심] 
        // "내가 처리할게"라고 Lock을 걸어봄. (setNX: 없으면 세팅하고 true, 있으면 false)
        // 락 자체도 5초 뒤에 사라지게 설정 (데드락 방지)
        const isMine = await pubClient.set(`websocket:reconnect:lock:${memberId}`, "locked", "NX", "EX", 5);

        if (isMine) {
            console.log(`[Process ${process.pid}] Winner! Handling disconnect for ${memberId}`);

            if (namespace === "chat") {
                chatDisconnect(memberId, roomId, chatIo, pubClient);
            } else if (namespace === "readyRoom") {
                readyRoomDisconnect(memberId, roomId, readyRoomIo, pubClient);
            } else if (namespace === "game") {
                gameDisconnect(userId, roomId, gameIo, pubClient);
            }
        } else {
            console.log(`[Process ${process.pid}] User ${userId} expired, but handled by another process.`);
        }
    }
}

const chatDisconnect = async (memberId, roomId, chatIo, pubClient) => {
    await deleteKeys(memberId, roomId, "chat", pubClient);

    console.log("user_left", { memberId, roomId });

    chatIo.to(roomId).emit("user left", { memberId });
}

const readyRoomDisconnect = async (memberId, roomId, readyRoomIo, pubClient) => {
    await deleteKeys(memberId, roomId, "readyRoom", pubClient);
    readyRoomIo.to(roomId).emit("user left", { memberId });
}

const gameDisconnect = async (memberId, roomId, gameIo, pubClient) => {
    await deleteKeys(userId, roomId, "game", pubClient);

    await pubClient.hdel(`room:${roomId}:locations`, userId);

    gameIo.to(roomId).emit("user left", { userId });
}

const deleteKeys = async (memberId, roomId, namespace, pubClient) => {
    await pubClient.del(`websocket:reconnect:lock:${memberId}`);
    await pubClient.del(`websocket:reconnect:info:${namespace}:${memberId}`);
    await pubClient.del(`websocket:reconnect:timer:${namespace}:${roomId}:${memberId}`);
}

export default expiredChannel;