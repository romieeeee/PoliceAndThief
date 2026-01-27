import { ChatController } from "../controller/ChatController.js";
import { resolveInSocket } from "../../../global/auth/JwtResolver.js";
import { RedisClient } from "../../utils/client/RedisClient.js";

const redisClient = new RedisClient();

const chatSocketServer = (io) => {
    io.use(resolveInSocket);

    io.on("connection", async (socket) => {
        const storedChatRoomId = parseInt(await redisClient.getStoredRoomId(socket, "chat"));

        if (storedChatRoomId) {
            await redisClient.deleteByCompletedReconnect(socket, "chat", storedChatRoomId);

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
                    await redisClient.pubReconnectTimer("chat", socket, socket.data.chatRoomId);
                }
            }
        });
    });
}

export default chatSocketServer;