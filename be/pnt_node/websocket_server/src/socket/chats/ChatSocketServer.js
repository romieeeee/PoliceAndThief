import { ChatController } from "./controller/ChatController.js";
import { resolveInSocket } from "../../global/auth/JwtResolver.js";

const chatSocketServer = (io, pubClient) => {
    io.use(resolveInSocket);

    io.on("connection", async (socket) => {
        const infoKey = `websocket:reconnect:info:chat:${socket.data.memberId}`;
        const storedChatRoomId = parseInt(await pubClient.get(infoKey));

        if (storedChatRoomId) {
            console.log(`[Reconnect] Restoring user ${socket.data.memberId} to room ${storedChatRoomId}`);
            socket.join(storedChatRoomId);
            socket.data.chatRoomId = storedChatRoomId;

            // Delete keys to cancel expiration event
            const timerKey = `websocket:reconnect:timer:chat:${storedChatRoomId}:${socket.data.memberId}`;
            await pubClient.del(infoKey);
            await pubClient.del(timerKey);

            socket.emit("reconnect", { chatRoomId: storedChatRoomId });
        }

        console.log("websocket is connected!");

        socket.data.isIntentionalExit = false; // 사용자의 요청에 의해서 소켓이 종료되었는지 판별하기 위한 변수
        const chatController = new ChatController(io, socket);

        // 채팅방 관련 이벤트
        socket.on("post join room", chatController.joinRoom);
        socket.on("post message", chatController.sendMessage);
        socket.on("post prev chat", chatController.getPrevChat);
        socket.on("post sync chat", chatController.syncChat);

        socket.on("post disconnect", chatController.disconnect);

        socket.on("disconnect", async () => {
            if (socket.data.isIntentionalExit) {
                console.log("socket의 연결이 정상적으로 끊어졌습니다.");
                return;
            } else {
                // 비정상적인 소켓 종료 => 채팅방 퇴장 db 처리 X
                console.log(`socket의 연결이 비정상적으로 끊어졌습니다. (Room: ${socket.data.chatRoomId})`);

                if (socket.data.chatRoomId) {
                    const timerKey = `websocket:reconnect:timer:chat:${socket.data.chatRoomId}:${socket.data.memberId}`;
                    const infoKey = `websocket:reconnect:info:chat:${socket.data.memberId}`;

                    // 1. Timer Key: Expiration event trigger (Value not important)
                    await pubClient.set(timerKey, "timer", "EX", 60);

                    // 2. Info Key: Data storage for reconnection (Value = chatRoomId)
                    // Set to 61s to ensure it survives slightly longer than the timer (race condition safety)
                    await pubClient.set(infoKey, socket.data.chatRoomId, "EX", 61);
                }
            }
        });
    });
}

export default chatSocketServer;