import { ChatService } from "../application/ChatService.js";
import { ChatRoomService } from "../application/ChatRoomService.js";
import mq from "../../../global/mq/MessagingQueue.js";
import { MQConfig } from "../../../global/mq/MQConfig.js";
import { sendError } from "../../../global/util/SocketError.js";

export class ChatController {

    constructor(io, socket) {
        this.io = io;
        this.socket = socket;
        this.chatService = new ChatService();
        this.chatRoomService = new ChatRoomService();
    }

    joinRoom = async (payload) => {
        // 채팅방 접속 db 처리 => is_connected = true로 처리
        try {
            const { chatRoomId } = payload;
            console.log("join room = ", chatRoomId, "member = ", this.socket.data.memberId);
            await this.chatRoomService.findChatRoom(chatRoomId);
            await this.chatRoomService.findMemberChatRoom(chatRoomId, this.socket.data.memberId);

            this.socket.join(chatRoomId);
            this.socket.data.chatRoomId = chatRoomId;

            const data = {
                "message": "joined room",
                "chatRoomId": chatRoomId
            }

            this.io.to(chatRoomId).emit("get join room", data);
        } catch (error) {
            console.error("joinRoom error", error);
            sendError(this.socket, error, "ChatError");
        }
    }

    sendMessage = async (payload) => {
        try {
            if (!payload || !payload.content) {
                throw { code: 400, message: "Invalid payload: content is required" };
            }

            payload.memberId = this.socket.data.memberId;
            const chatRoomId = this.socket.data.chatRoomId;

            if (!chatRoomId) {
                throw { code: 400, message: "ChatRoomId is missing in socket data" };
            }

            const resData = await this.chatService.save({ chatRoomId, ...payload });

            // 푸시 알림 => 컨슈머에서 채팅방에 접속해 있지 않은 멤버를 확인후 푸시알림
            mq.sendMessage({ chatRoomId, ...payload }, MQConfig.MQ_ALARM);

            this.io.to(chatRoomId).emit("get message", resData);
        } catch (error) {
            console.error("sendMessage error", error);
            sendError(this.socket, error, "ChatError");
        }
    }

    /**
     * payload = {
     *  "cursor" : 1,
     *  "limit" : 10
     * }
     */
    getPrevChat = async (payload) => {
        try {
            const { chatRoomId, memberId } = this.socket.data;

            if (!chatRoomId) {
                throw { code: 400, message: "ChatRoomId is missing in socket data" };
            }

            console.log("get prev chat", payload);

            // 데이터 로딩 로직
            const data = await this.chatService.getPrevChat({ chatRoomId, memberId, ...payload });

            this.io.to(chatRoomId).emit("get prev chat", data);
        } catch (error) {
            console.error("getPrevChat error", error);
            sendError(this.socket, error, "ChatError");
        }
    }

    /**
     * payload = {
     *  "cursor" : 1
     * }
     */
    syncChat = async (payload) => {
        try {
            const { chatRoomId, memberId } = this.socket.data;

            if (!chatRoomId) {
                throw { code: 400, message: "ChatRoomId is missing in socket data" };
            }

            // 데이터 로딩 로직
            const data = await this.chatService.syncChat({ chatRoomId, memberId, ...payload });

            this.io.to(chatRoomId).emit("get sync chat", data);
        } catch (error) {
            console.error("syncChat error", error);
            sendError(this.socket, error, "ChatError");
        }
    }

    /**
     * 사용자 요청에 의한 채팅방 나가기 -> api 로 disconnect 호출
     * 
     * 웹소켓 연결 끊김 -> expiredChannel 에서 처리
     */
    disconnect = async () => {
        const memberId = this.socket.data.memberId;
        const chatRoomId = this.socket.data.chatRoomId;
        console.log(`${memberId} 님이 소켓 연결을 종료하였습니다.`);

        this.socket.data.isIntentionalExit = true;

        this.socket.disconnect();
    }
}