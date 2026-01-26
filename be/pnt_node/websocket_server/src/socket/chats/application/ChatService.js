import chatEntity from "../../../global/db/mongo/entity/chat.js";
import members from "../../../global/db/mongo/entity/member.js";
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

        const data = await chatModel.save();

        const member = await members.findOne({ memberId: chat.memberId }).exec();
        
        data.member = member;

        return this.parseChat(data);
    }

    getAggregationPipeline(matchStage, sortVariable, limit) {
        return [
            { $match: matchStage },
            { $sort: { _id: sortVariable } },
            { $limit: limit },
            {
                $lookup: {
                    from: "members",
                    localField: "memberId",
                    foreignField: "memberId",
                    as: "memberInfo"
                }
            },
            {
                $unwind: {
                    path: "$memberInfo",
                    preserveNullAndEmptyArrays: true
                }
            },
            {
                $project: {
                    _id: 1,
                    content: 1,
                    memberId: 1,
                    chatRoomId: 1,
                    avatarUrl: 1,
                    createdAt: 1,
                    member: "$memberInfo"
                }
            }
        ];
    }

    // {chatRoomId, cursor, limit}
    /**
     * 메시지의 _id를 기준으로 
     * 이전의 메시지들을 가져오기 위한 로직
     */
    async getPrevChat(payload) {
        const matchStage = {
            chatRoomId: payload.chatRoomId
        };

        if (payload.cursor) {
            matchStage._id = { $lt: payload.cursor };
        }

        const pipeline = this.getAggregationPipeline(matchStage, -1, payload.limit);
        const chats = await chatEntity.aggregate(pipeline);

        return { items: this.parseChats(chats), count: chats.length };
    }

    // {chatRoomId, cursor, limit}
    /**
     * 웹소켓이 끊기고 다시 연결되었을 때 
     * 마지막으로 받은 메시지의 _id를 기준으로 
     * 이후의 메시지들을 가져오기 위한 로직
     */
    async syncChat(payload) {
        const matchStage = {
            chatRoomId: payload.chatRoomId,
            _id: { $gt: payload.cursor }
        };

        const pipeline = this.getAggregationPipeline(matchStage, 1, payload.limit);
        const chats = await chatEntity.aggregate(pipeline);

        return { items: this.parseChats(chats), count: chats.length };
    }

    parseChat = (chat) => {
        return {
            id: chat._id,
            memberId: chat.memberId,
            avatarUrl: chat.member.avatarUrl,
            senderNickname: chat.member.nickname,
            chatRoomId: chat.chatRoomId,
            content: chat.content,
            createdAt: chat.createdAt,
        }
    }

    parseChats = (chats) => {
        return chats.map(this.parseChat);
    }
}