import GameMission from "../../../global/db/sequelize/entity/GameMission";

export class GameMissionService {

    findOne = async (gameId, missionId) => {
        return await GameMission.findOne({
            where: {
                gameId: gameId,
                missionId: missionId,
                isDeleted: false
            }
        });
    }

    update = async (gameId, missionId, payload) => {
        return await GameMission.update(payload, {
            where: {
                gameId: gameId,
                missionId: missionId,
                isDeleted: false
            }
        });
    }
}