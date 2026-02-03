import { RedisClient } from "../redis/RedisClient.js";
import { TurfService } from "../service/TurfService.js";
import { GameMemberService, GameMemberPosition, GameMemberStatus } from "../service/GameMemberService.js";
import { Emitter } from "@socket.io/redis-emitter";

const NAMESPACE = "/game";

export class GpsController {
    constructor() {
        this.redisClient = new RedisClient();
        this.turfService = new TurfService();
        this.gameMemberService = new GameMemberService();
        this.io = new Emitter(this.redisClient.client).of(NAMESPACE);
    }

    postGps = async (payload) => {
        try {
            console.log(payload);
            const memberId = parseInt(payload.memberId);
            const gameId = parseInt(payload.gameId);
            const lat = parseFloat(payload.lat);
            const lng = parseFloat(payload.lng);
            const walk = parseInt(payload.walk);
            const longestSurvived = parseInt(payload.longestSurvived); 

            const gameMember = await this.gameMemberService.findMemberGame(gameId, memberId);
            if (!gameMember) return; // Member not found in game, ignore

            const position = gameMember.position;
            const status = gameMember.status;
            const penalty = await this.redisClient.getPenalty(memberId, gameId) || 0;
            const missionCompleted = gameMember.missionCompleted;

            const locationData = {
                lat,
                lng,
                memberId, // 클라이언트 편의를 위해 포함
                gameId,
                walk,
                longestSurvived,
                position,
                status,
                penalty,
                missionCompleted,
                isConnected: true,
                timestamp: new Date().toISOString() // 중요: 갱신 시간 기록
            };

            if (position === GameMemberPosition.THIEF && status === GameMemberStatus.FREE) {
                locationData.longestSurvived++;
            }

            // Hash에 저장 (이미 있으면 덮어쓰기됨 -> 자동 최신화)
            await this.redisClient.setLocation(memberId, gameId, locationData);

            // 경계선 확인 => turf.js 사용
            const gameSetting = await this.redisClient.getGameSetting(gameId);
            if (!gameSetting) return; // Game setting not found

            const isInBoundary = await this.turfService.checkUserInBoundary([lng, lat], gameSetting.boundaryGeo.coordinates[0]);

            // 도둑이고, 상태가 PRISON 일때 탈옥 판별
            // 감옥에서 10m 이상 벗어났을때 탈옥 (오차범위 5m) 
            if (position === GameMemberPosition.THIEF && status === GameMemberStatus.PRISON
                && !(await this.turfService.checkUserInPrison([lng, lat], [gameSetting.prisonLng, gameSetting.prisonLat]))) {

                await this.gameMemberService.updateMemberStatus(gameId, memberId, GameMemberStatus.FREE);

                const res = {
                    gameId: gameId,
                    thiefId: memberId,
                    escapePointId: 3,
                    escapedAt: new Date().toISOString(),
                };

                this.io.to(gameId).emit("get escape", res);
                return;
            }


            if (!isInBoundary && position === GameMemberPosition.THIEF) {
                const res = {
                    gameId: gameId,
                    memberId: memberId,
                    message: "out of boundary",
                    type: "outOfBoundary",
                    createdAt: new Date().toISOString(),
                };

                // 도둑일 때 경계선을 벗어났으면 패널티 부여 -> redis에서 관리.
                // 만약 3번 이상 벗어나면 자동으로 감옥으로 이송. -> 다음 패널티 체크 활성화까지 10초
                const count = await this.redisClient.increasePenalty(memberId, gameId);

                await this.redisClient.setLocation(memberId, gameId, locationData);

                res.penalty = count || penalty;
                locationData.penalty = res.penalty;
                await this.redisClient.setLocation(memberId, gameId, locationData);

                if (count >= 3) {
                    locationData.status = GameMemberStatus.TRANSFER;
                    await this.redisClient.deletePenalty(memberId, gameId);

                    locationData.penalty = 0;
                    await this.redisClient.setLocation(memberId, gameId, locationData);

                    /*
                     * Send signal to main server to check game end condition (ARREST_CHECK)
                     */
                    await this.redisClient.publish("game:event:trigger", {
                        type: 'ARREST_CHECK',
                        gameId: gameId,
                        memberId: memberId
                    });

                    this.io.to(gameId).emit("get arrest", {
                        gameId: gameId,
                        policeId: null,
                        thiefId: memberId,
                        result: "SUCCESS",
                        reason: "PENALTY",
                        arrestedAt: new Date().toISOString(),
                    });
                    return;
                }
                this.io.to(gameId).emit("out of boundary", res);
            }

            // 이송 중이고, 감옥 범위 안에 들어왔을때
            if (position === GameMemberPosition.THIEF && status === GameMemberStatus.TRANSFER
                && await this.turfService.checkUserInPrison([lng, lat], [gameSetting.prisonLng, gameSetting.prisonLat])) {

                await this.gameMemberService.updateMemberStatus(gameId, memberId, GameMemberStatus.PRISON);
                const res = {
                    gameId: gameId,
                    thiefId: memberId,
                    status: GameMemberStatus.PRISON,
                    arrestedAt: new Date().toISOString(),
                };
                this.io.to(gameId).emit("modify member status", res);
                return;
            }

            if (position === GameMemberPosition.POLICE) {
                return;
            }

            // 비프음
            const locationDatas = await this.redisClient.getAllLocations(gameId);
            let polices;
            if (locationDatas) {
                polices = locationDatas
                    .filter(data => data.position === GameMemberPosition.POLICE && data.isConnected === true)
                    .map(({ memberId, lng, lat }) => ({ policeId: memberId, lng, lat }));

            }

            if (polices && polices.length > 0) {
                const nearPolice = this.turfService.checkNearPolice([lng, lat], polices);
                if (nearPolice) {
                    // Same issue: socket.emit unicast vs broadcast.
                    // Emit to game room, client filters if `thiefId` matches theirs.
                    // "get beep use" event.
                    this.io.to(gameId).emit("get beep use", {
                        gameId: gameId,
                        policeId: nearPolice.policeId,
                        thiefId: memberId,
                        distance: nearPolice.distance,
                    });
                }
            }
        } catch (error) {
            console.error("Error in postGps:", error);
        }
    }
}