import redisDB from "../../../global/db/redis/RedisDB";
import { Router } from "express";
import { Emitter } from "@socket.io/redis-emitter";
import { GameMissionService } from "../application/GameMissionService";
import { MissionStatus } from "../../../global/db/sequelize/status/MissionStatus";
import member from "../../../global/db/mongo/entity/member";

const GAME_NAMESPACE = "/game";

export class MissionController {
    constructor() {
        this.router = new Router();
        this.emitter = new Emitter(redisDB.getPubClient());
        this.init();
        this.gameMissionService = new GameMissionService();
    }

    init = () => {
        this.router.post("/complete", this.missionComplete);
    }

    getRouter = () => {
        return this.router;
    }

    missionComplete = async (req, res) => {
        try {
            const payload = req.body;
            const { gameId, missionId, memberId, success } = payload;
            const gameMission = await this.gameMissionService.findOne(gameId, missionId);
            if (!gameMission) {
                throw { code: 404, message: "GameMission not found" };
            }

            // 이미 성공했는지 여부 확인
            if (gameMission.status === MissionStatus.SUCCESS) {
                const resData = {
                    gameId: gameId,
                    missionId: missionId,
                    thiefId: gameMission.completedBy,
                    success: false,
                    reason: "ALREADY_COMPLETED",
                    completedAt: gameMission.completedAt // 기존 완료 시간 사용
                }
                this.emitter.of(GAME_NAMESPACE).to(gameId).emit("get mission result", resData);
                res.status(200).json({ message: "GameMission already completed" });
                return;
            }

            // 미션 업데이트 시작
            const completedAt = new Date().toISOString();

            // 성공시에만 업데이트
            if (success) {
                await this.gameMissionService.update(gameId, missionId, {
                    completedBy: memberId,
                    completedAt: completedAt,
                    status: MissionStatus.SUCCESS
                });
            }

            res.status(200).json({ message: "GameMission updated" });

            const resData = {
                gameId: gameId,
                missionId: missionId,
                thiefId: memberId,
                success: success,
                reason: "",
                completedAt: completedAt
            }

            if (!success) {
                resData.reason = "NOT_MATCHED";
            }

            this.emitter.of(GAME_NAMESPACE).to(gameId).emit("get mission result", resData);
        } catch (error) {
            console.error("missionComplete error", error);
            res.status(error.code || 500).json({ message: error.message });
        }
    }
}