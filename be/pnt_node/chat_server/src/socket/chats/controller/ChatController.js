import { ChatService } from "../application/ChatService.js";
import { ChatRoomService } from "../application/ChatRoomService.js";
import mq from "../../../global/mq/MessagingQueue.js";
import { MQConfig } from "../../../global/mq/MQConfig.js";

export class ChatController {

    constructor(io, socket) {
        this.io = io;
        this.socket = socket;
        this.chatService = new ChatService();
        this.chatRoomService = new ChatRoomService();
        this.mq = mq;
    }

    joinRoom = async (payload) => {
        const { chatRoomId } = payload;

        this.socket.join(chatRoomId);
        this.socket.data.chatRoomId = chatRoomId;

        console.log(chatRoomId, this.socket.data.memberId);

        // 채팅방 접속 db 처리 => is_connected = true로 처리
        try {
            await this.chatRoomService.connectChatRoom(chatRoomId, this.socket.data.memberId);

            const data = {
                "message": "joined room",
                "chatRoomId": chatRoomId
            }

            this.io.to(chatRoomId).emit("get join room", data);
        } catch (error) {
            console.error("joinRoom error", error);
            if (error.message === "BadRequestException") {
                this.socket.emit("error", { message: error.message, code: error.code });
            } else {
                this.socket.emit("error", { message: "Internal Server Error", code: 500 });
            }
        }
    }

    sendMessage = async (payload) => {
        payload.memberId = this.socket.data.memberId;
        const chatRoomId = this.socket.data.chatRoomId;

        console.log("send message", payload);

        const resData = await this.chatService.save({ chatRoomId, ...payload });

        // 푸시 알림 => 컨슈머에서 채팅방에 접속해 있지 않은 멤버를 확인후 푸시알림
        this.mq.sendMessage({ chatRoomId, ...payload }, MQConfig.MQ_ALARM);

        this.io.to(chatRoomId).emit("get message", resData);
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

    disconnect = async () => {
        const memberId = this.socket.data.memberId;
        console.log(`${memberId} 님이 방퇴장을 요청하였습니다.`);

        // 채팅방 퇴장 db 처리 => is_connected = false로 처리
        try {
            await this.chatRoomService.disconnectChatRoom(chatRoomId, memberId);

            this.socket.data.isIntentionalExit = true;

            this.socket.disconnect();
        } catch (error) {
            console.error("disconnect error", error);
            if (error.message === "BadRequestException") {
                this.socket.emit("error", { message: error.message, code: error.code });
            } else {
                this.socket.emit("error", { message: "Internal Server Error", code: 500 });
            }
        }
    }
}