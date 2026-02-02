import GameMission from "../../../global/db/sequelize/entity/GameMission";
import Mission from "../../../global/db/sequelize/entity/Mission";


export class GameMissionService {
    findAllByGameId = async (gameId) => {
        const res = await GameMission.findAll({
            attributes: ["id", "missionId" ,"gameId", "status", "completedAt", "completedBy"],
            where: {
                gameId: gameId,
                isDeleted: false
            },
            include: [
                {
                    model: Mission,
                    attributes: ["id", "description", "title", "keyword"]
                }
            ]
        });

        if (!res) {
            this.makeError("NotFoundException", "게임 미션 정보를 찾을 수 없습니다.", 404);
        }
        return res;
    }

    findMission = async (missionId) => {
        const res = await GameMission.findOne({
            where: {
                id: missionId,
                isDeleted: false,
                include: [

                ]
            }
        });

        if (!res) {
            this.makeError("NotFoundException", "미션 정보를 찾을 수 없습니다.", 404);
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