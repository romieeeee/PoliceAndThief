
export class ChatController {

    constructor(io, socket) {
        this.io = io;
        this.socket = socket;
        this.chatService;
    }

    joinRoom = (payload) => {
        const { chatRoomId, userId } = payload;

        this.socket.join(chatRoomId);
        this.socket.data.chatRoomId = chatRoomId;
        this.socket.data.userId = userId;

        console.log(chatRoomId, userId);

        // 대충 채팅방 접속 db 처리 => is_connected = true로 처리

        const data = {
            "message" : "joined room",
            "chatRoomId" : chatRoomId
        }

        this.io.to(chatRoomId).emit("get join room", data);
    }

    sendMessage = async (payload) => {
        payload.userId = this.socket.data.userId;
        const chatRoomId = this.socket.data.chatRoomId;

        console.log("send message", chatRoomId, payload.userId);

        // 대충 메시지 저장 로직

        // 푸시 알림 => 컨슈머에서 채팅방에 접속해 있지 않은 멤버를 확인후 푸시알림


        this.io.to(chatRoomId).emit("get message", payload);
    }

    /**
     * payload = {
     *  "cursor" : "string",
     *  "limit" : 10
     * }
     */
    getPrevChat = async (payload) => {
        const { chatRoomId, memberId } = this.socket.data;

        // 대충 데이터 로딩 로직
        const data = {};

        this.io.to(chatRoomId).emit("get prev chat", data);
    }

    /**
     * payload = {
     *  "cursor" : "string"
     * }
     */
    syncChat = async (payload) => {
        const { chatRoomId, memberId } = this.socket.data;

        // 대충 데이터 로딩 로직
        const data = {};

        this.io.to(chatRoomId).emit("get sync chat", data);
    }

    disconnect = () => {
        const userId = this.socket.data.userId;
        console.log(`${userId} 님이 방퇴장을 요청하였습니다.`);

        this.socket.data.isIntentionalExit = true;

        this.socket.disconnect();
    }
}