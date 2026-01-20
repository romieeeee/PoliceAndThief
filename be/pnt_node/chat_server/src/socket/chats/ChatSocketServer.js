import { ChatController } from "./controller/ChatController.js";

const chatSocketServer = (io) => {
    io.on("connection", (socket) => {
        console.log("websocket is connected!");

        socket.data.isIntentionalExit = false; // 사용자의 요청에 의해서 소켓이 종료되었는지 판별하기 위한 변수
        const chatController = new ChatController(io, socket);

        // 채팅방 관련 이벤트
        socket.on("post join room", chatController.joinRoom);
        socket.on("post message", chatController.sendMessage);
        socket.on("post prev chat", chatController.getPrevChat);
        socket.on("post sync chat", chatController.syncChat);

        socket.on("request leave chat room", chatController.disconnect);


        socket.on("disconnect", () => {
            if (socket.data.isIntentionalExit) {
                console.log("socket의 연결이 정상적으로 끊어졌습니다.");
                return;
            }
        });
    });
}
export default chatSocketServer;