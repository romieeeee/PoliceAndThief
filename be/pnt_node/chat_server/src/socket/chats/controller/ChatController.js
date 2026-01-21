import { ChatService } from "../application/ChatService.js";

export class ChatController {

    constructor(io, socket) {
        this.io = io;
        this.socket = socket;
        this.chatService = new ChatService();
    }

    joinRoom = (payload) => {
        const { chatRoomId } = payload;

        this.socket.join(chatRoomId);
        this.socket.data.chatRoomId = chatRoomId;

        console.log(chatRoomId, this.socket.data.memberId);

        // 채팅방 접속 db 처리 => is_connected = true로 처리

        const data = {
            "message": "joined room",
            "chatRoomId": chatRoomId
        }

        this.io.to(chatRoomId).emit("get join room", data);
    }

    sendMessage = async (payload) => {
        payload.memberId = this.socket.data.memberId;
        const chatRoomId = this.socket.data.chatRoomId;

        console.log("send message", payload);

        await this.chatService.save({ chatRoomId, ...payload });

        // 푸시 알림 => 컨슈머에서 채팅방에 접속해 있지 않은 멤버를 확인후 푸시알림

        this.io.to(chatRoomId).emit("get message", payload);
    }

    /**
     * payload = {
     *  "cursor" : 1,
     *  "limit" : 10
     * }
     */
    getPrevChat = async (payload) => {
        const { chatRoomId, memberId } = this.socket.data;

        // 데이터 로딩 로직
        const data = await this.chatService.getPrevChat({ chatRoomId, memberId, ...payload });

        this.io.to(chatRoomId).emit("get prev chat", data);
    }

    /**
     * payload = {
     *  "cursor" : 1
     * }
     */
    syncChat = async (payload) => {
        const { chatRoomId, memberId } = this.socket.data;

        // 데이터 로딩 로직
        const data = await this.chatService.syncChat({ chatRoomId, memberId, ...payload });

        this.io.to(chatRoomId).emit("get sync chat", data);
    }

    disconnect = () => {
        const memberId = this.socket.data.memberId;
        console.log(`${memberId} 님이 방퇴장을 요청하였습니다.`);

        this.socket.data.isIntentionalExit = true;

        this.socket.disconnect();
    }
}