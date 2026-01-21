import MemberChatRoom from "../../../db/sequelize/entity/MemberChatRoom.js";

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
            throw new Error("BadRequestException");
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
            throw new Error("BadRequestException");
        }
    }
}