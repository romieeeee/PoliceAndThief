import GameMember from "../../../global/db/sequelize/entity/GameMember";
import Member from "../../../global/db/sequelize/entity/Member";
import MemberProfile from "../../../global/db/sequelize/entity/MemberProfile";
import { RedisClient } from "../../utils/client/RedisClient.js";

export class GameMemberService {
    constructor() {
        this.redisClient = new RedisClient();
    }

    getGameMembers = async (gameId) => {
        const res = await GameMember.findAll({
            where: {
                gameId: gameId,
                isDeleted: false
            },
            include: [
                {
                    model: Member,
                    attributes: ["id"],
                    include: [
                        {
                            model: MemberProfile,
                            attributes: ["nickname", "avatarUrl"]
                        }
                    ]
                }
            ]
        });

        return res;
    }

    findMemberGame = async (gameId, memberId) => {
        const res = await this.redisClient.getLocation(memberId, gameId);

        if (!res) {
            this.makeError("NotFoundException", "유저의 게임 접속 정보를 찾을 수 없습니다.", 404);
        }
        return res;
    }

    findMemberGameInDB = async (memberId, gameId) => {
        const res = await GameMember.findOne({
            where: {
                memberId: memberId,
                gameId: gameId,
                isDeleted: false
            }
        });

        if (!res) {
            this.makeError("NotFoundException", "유저의 게임 접속 정보를 찾을 수 없습니다.", 404);
        }

        return res;
    }

    updateMemberStatus = async (gameId, memberId, status) => {
        const location = await this.redisClient.getLocation(memberId, gameId);

        if (!location) {
            this.makeError("NotFoundException", "유저의 게임 접속 정보를 찾을 수 없습니다.", 404);
        }

        location.status = status;
        await this.redisClient.setLocation(memberId, gameId, location);
        return location;
    }

    findAllByGameId = async (gameId) => {
        const res = await GameMember.findAll({
            where: {
                gameId: gameId,
                isDeleted: false,
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
                    attributes: ["id"],
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
        const location = await this.redisClient.getLocation(memberId, gameId);
        if (!location) {
            this.makeError("NotFoundException", "유저의 게임 접속 정보를 찾을 수 없습니다.", 404);
        }

        location.isConnected = isConnected;
        await this.redisClient.setLocation(memberId, gameId, location);
        
        return true;
    }

    updateThiefStats = async (gameId, memberId, walk, longestSurvived) => {
        const res = await GameMember.update({ walk: walk, longestSurvived: longestSurvived }, {
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
        const res = await GameMember.update({ arrestCount: arrestCount, walk: walk }, {
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