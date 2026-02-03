import GameMission from "../../../global/db/sequelize/entity/GameMission";

export class GameMissionService {

    findOne = async (id) => {
        return await GameMission.findOne({
            where: {
                id: id,
                isDeleted: false
            }
        });
    }

    update = async (id, payload) => {
        return await GameMission.update(payload, {
            where: {
                id: id,
                isDeleted: false
            }
        });
    }
}