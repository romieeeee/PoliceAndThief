import Game from "../../../global/db/sequelize/entity/Game.js"
import db from "../../../global/db/sequelize/SequelizeDB.js";
import { GameMemberService } from "./GameMemberService.js";
import { GameMemberStatService } from "./GameMemberStatService.js";
import { GameMemberStatus } from "../../../global/db/sequelize/status/GameMemberStatus.js";
import { GameMemberPosition } from "../../../global/db/sequelize/status/GameMemberPosition.js";
import { GameStatus } from "../../../global/db/sequelize/status/GameStatus.js";

export class GameService {
    constructor() {
        this.gameMemberService = new GameMemberService();
        this.gameMemberStatService = new GameMemberStatService();
    }

    findGame = async (gameId, status) => {
        const whereCondition = {
            id: gameId,
            isDeleted: false
        };

        if (status !== undefined && status !== null) {
            whereCondition.status = status;
        }

        const res = await Game.findOne({
            where: whereCondition
        });

        if (!res) {
            this.makeError("NotFoundException", "게임을 찾을 수 없습니다.", 404);
        }

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

    // 게임 종료 조건 확인 => 모든 도둑이 잡혔을때 종료.
    // 게임 승리팀 상태를 비관적 락으로 처리해야할 수도 있음.
    checkGameHaveToFinish = async (gameId) => {
        const game = await this.findGame(gameId, GameStatus.IN_GAME);

        if (!game || game.status === GameStatus.ENDED) {
            return false;
        }

        const gameMembers = await this.gameMemberService.findAllByGameId(gameId);

        const thiefMembers = gameMembers
            .filter(member => member.givenPosition === GameMemberPosition.THIEF && member.status !== GameMemberStatus.FREE);

        const isGameEnd = thiefMembers.length === 0;

        return isGameEnd;
    }

    updateGame = async (payload) => {
        try {
            await Game.update(payload, {
                where: {
                    id: payload.gameId,
                    isDeleted: false,
                },
            });
        } catch (error) {
            console.error("updateGame Error", error);
            throw error;
        }
    }

    // 게임 종료 처리
    endGame = async (gameId, winnerTeam) => {
        try {
            // 게임 상태 변경 (END)
            await Game.update({
                status: GameStatus.ENDED,
                winTeam: winnerTeam,
                endedAt: new Date().toISOString(),
            }, {
                where: {
                    id: gameId,
                },
            });

            return true;
        } catch (error) {
            console.error("endGame Error", error);
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