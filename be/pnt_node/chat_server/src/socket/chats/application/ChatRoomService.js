import MemberChatRoom from "../../../global/db/sequelize/entity/MemberChatRoom.js";

export class ChatRoomService {
    connectChatRoom = async (chatRoomId, memberId) => {
        const [affectedCount] = await MemberChatRoom.update({
            is_connected: true
        }, {
            where: {
                chatRoomId: chatRoomId,
                memberId: memberId
            }
        });

        if (affectedCount === 0) {
            this.makeError("BadRequestException", 400);
        }
    }

    disconnectChatRoom = async (chatRoomId, memberId) => {
        const [affectedCount] = await MemberChatRoom.update({
            is_connected: false
        }, {
            where: {
                chatRoomId: chatRoomId,
                memberId: memberId
            }
        });

        if (affectedCount === 0) {
            this.makeError("BadRequestException", 400);
        }
    }

    makeError = (message, code) => {
        const error = new Error(message);
        error.code = code;
        throw error;
    }
}