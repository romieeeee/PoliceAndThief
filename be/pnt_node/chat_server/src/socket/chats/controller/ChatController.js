
export class SocketController {

    constructor(io, socket) {
        this.io = io;
        this.socket = socket;
    }

    joinRoom = (payload) => {
        const { chatRoomId, userId } = payload;

        this.socket.join(chatRoomId);
        this.socket.roomId = chatRoomId;
        this.socket.userId = userId;

        console.log(chatRoomId, userId);

        const data = {
            "message" : "joined room",
            "chatRoomId" : chatRoomId
        }

        this.io.to(chatRoomId).emit("get join room", data);
    }

    sendMessage = (payload) => {
        payload.userId = this.socket.userId;
        const chatRoomId = this.socket.roomId;
        // 대충 메시지 저장 로직

        console.log("send message", chatRoomId, payload.userId);

        this.io.to(chatRoomId).emit("get message", payload);
    }
}