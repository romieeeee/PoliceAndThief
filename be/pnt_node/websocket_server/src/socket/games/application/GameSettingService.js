import GameSetting from "../entity/GameSetting.js";
import sequelize from "../../global/db/sequelize/index.js";

export class GameSettingService {
    findGameSettingByLocation = async (lat, lng) => {
        const result = await GameSetting.findAll({
            where: sequelize.literal(`
                ST_DWithin(
                polygon::geography, 
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, 
                5
    )`),
            replacements: { lng, lat },
            raw: true
        });
    }
}