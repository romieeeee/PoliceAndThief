import chatEntity from "../../../global/db/mongo/entity/chat.js";
import moment from "moment-timezone";

export class ChatService {
    async save(chat) {
        const now = moment().tz("Asia/Seoul").format('YYYY-MM-DDTHH:mm:ssZ');
        chat.createdAt = now;

        /**
         * 정책을 정해서 avatarUrl이 없을 경우 기본 이미지 설정.
         */
        if (!chat.avatarUrl) {
            chat.avatarUrl = "default.png";
        }

        console.log("chat", chat);

        const chatModel = new chatEntity(chat);

        return await chatModel.save();
    }

    // {chatRoomId, cursor, limit}
    /**
     * 메시지의 _id를 기준으로 
     * 이전의 메시지들을 가져오기 위한 로직
     */
    async getPrevChat(payload) {
        const query = {
            chatRoomId: payload.chatRoomId
        };

        if (payload.cursor) {
            query._id = { $lt: payload.cursor };
        }

        const chats = await chatEntity.find(query)
            .sort({ _id: -1 })
            .limit(payload.limit);

        return { items: chats, count: chats.length };
    }

    // {chatRoomId, cursor, limit}
    /**
     * 웹소켓이 끊기고 다시 연결되었을 때 
     * 마지막으로 받은 메시지의 _id를 기준으로 
     * 이후의 메시지들을 가져오기 위한 로직
     */
    async syncChat(payload) {
        const query = {
            chatRoomId: payload.chatRoomId,
            _id: { $gt: payload.cursor }
        };

        const chats = await chatEntity.find(query)
            .sort({ _id: 1 })
            .limit(payload.limit);

        return { items: chats, count: chats.length };
    }
}