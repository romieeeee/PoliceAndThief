import GameMember from "../../../global/db/sequelize/entity/GameMember";

export class GameMemberService {
    findMemberGame = async (gameId, memberId) => {
        const res = await GameMember.findOne({
            where: {
                gameId: gameId,
                memberId: memberId,
                ready: true,
                isDeleted: false
            }
        });

        if (!res) {
            this.makeError("NotFoundException", "유저의 게임 접속 정보를 찾을 수 없습니다.", 404);
        }
        return res;
    }

    updateMemberStatus = async (gameId, memberId, status, options = {}) => {
        const res = await GameMember.update({ status }, {
            where: {
                gameId: gameId,
                memberId: memberId,
                isDeleted: false
            },
            ...options
        });

        if (!res) {
            this.makeError("NotFoundException", "유저의 게임 접속 정보를 찾을 수 없습니다.", 404);
        }
        return res;
    }

    makeError = (message, text, code) => {
        const error = new Error(message);
        error.code = code;
        error.text = text;
        throw error;
    }
}