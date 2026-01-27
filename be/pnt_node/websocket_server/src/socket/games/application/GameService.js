import Game from "../../../global/db/sequelize/entity/Game.js"
import db from "../../../global/db/sequelize/SequelizeDB.js";
import { GameMemberService } from "./GameMemberService.js";
import { GameMemberStatService } from "./GameMemberStatService.js";
import { GameMemberStatus } from "../../../global/db/sequelize/status/GameMemberStatus.js";

export class GameService {
    constructor() {
        this.gameMemberService = new GameMemberService();
        this.gameMemberStatService = new GameMemberStatService();
    }

    findGame = async (gameId) => {
        const res = await Game.findOne({
            where: {
                id: gameId,
                isDeleted: false,
                isFinished: false
            }
        });

        if (!res) {
            this.makeError("NotFoundException", "게임을 찾을 수 없습니다.", 404);
        }
        return res;
        return res;
    }

    processArrest = async (gameId, thiefId, policeId) => {
        const sequelize = db.getSequelize();
        const t = await sequelize.transaction();

        try {
            // thief 상태 변경 (TRANSFER)
            await this.gameMemberService.updateMemberStatus(gameId, thiefId, GameMemberStatus.TRANSFER, { transaction: t });

            // police 스탯 업데이트 (체포 횟수 증가)
            await this.gameMemberStatService.updateArrestCount(policeId, { transaction: t });

            await t.commit();
            return true;
        } catch (error) {
            await t.rollback();
            console.error("processArrest Transaction Error", error);
            throw error;
        }
    }

    makeError = (message, text, code) => {
        const error = new Error(message);
        error.code = code;
        error.text = text;
        throw error;
    }
}