import GameMember from "../../../global/db/sequelize/entity/GameMember";
import Member from "../../../global/db/sequelize/entity/Member";
import MemberProfile from "../../../global/db/sequelize/entity/MemberProfile";

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

    findAllByGameId = async (gameId) => {
        const res = await GameMember.findAll({
            where: {
                gameId: gameId,
                isDeleted: false
            }
        });

        if (!res) {
            this.makeError("NotFoundException", "게임 멤버 정보를 찾을 수 없습니다.", 404);
        }
        return res;
    }

    findMembersWithProfileByGameId = async (gameId) => {
        const res = await GameMember.findAll({
            where: {
                gameId: gameId,
                isDeleted: false
            },
            include: [
                {
                    model: Member,
                    include: [
                        {
                            model: MemberProfile,
                            attributes: ["nickname", "avatarUrl"]
                        }
                    ]
                }
            ]
        });

        if (!res) {
            this.makeError("NotFoundException", "게임 멤버 정보를 찾을 수 없습니다.", 404);
        }

        return res;
    }

    updateInGameConnected = async (gameId, memberId, isConnected) => {
        const res = await GameMember.update({ isConnected }, {
            where: {
                gameId: gameId,
                memberId: memberId,
                isDeleted: false
            }
        });

        if (!res) {
            this.makeError("NotFoundException", "유저의 게임 접속 정보를 찾을 수 없습니다.", 404);
        }
        return res;
    }

    updateThiefStats = async (gameId, memberId, walk, longestSurvived) => {
        const res = await GameMember.update({ walk, longestSurvived }, {
            where: {
                gameId: gameId,
                memberId: memberId,
                isDeleted: false
            }
        });

        if (!res) {
            this.makeError("NotFoundException", "유저의 게임 접속 정보를 찾을 수 없습니다.", 404);
        }
        return res;
    }

    updatePoliceStats = async (gameId, memberId, arrestCount, walk) => {
        const res = await GameMember.update({ arrestCount, walk }, {
            where: {
                gameId: gameId,
                memberId: memberId,
                isDeleted: false
            }
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