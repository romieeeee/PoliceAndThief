import { GameService } from "../application/GameService.js";
import { GameMemberService } from "../application/GameMemberService.js";
import { sendError } from "../../../global/util/SocketError.js";
import { GameSettingService } from "../application/GameSettingService.js";
import { GameMemberPosition } from "../../../global/db/sequelize/status/GameMemberPosition.js";
import { GameMemberStatus } from "../../../global/db/sequelize/status/GameMemberStatus.js";
import { GameSkillService } from "../application/GameSkillService.js";
import { GameMemberStatService } from "../application/GameMemberStatService.js";
import { RedisClient } from "../../utils/client/RedisClient.js";
import { GameStatus } from "../../../global/db/sequelize/status/GameStatus.js";
import { GameMissionService } from "../application/GameMissionService.js";
import { TurfService } from "../application/TurfService.js";
import { MQConfig } from "../../../global/mq/MQConfig.js";


/**
 * ToDo CCTV
 */

export class GameController {
    constructor(io, socket, mq) {
        this.io = io;
        this.socket = socket;
        this.redisClient = new RedisClient();
        this.gameService = new GameService();
        this.gameMemberService = new GameMemberService();
        this.gameSettingService = new GameSettingService();
        this.gameSkillService = new GameSkillService();
        this.gameMemberStatService = new GameMemberStatService();
        this.gameMissionService = new GameMissionService();
        this.turfService = new TurfService();
        this.mq = mq;
    }

    /**
     * 게임 방 접속
     * {
     *  'gameId': 1
     * }
     */

    isActiveRoom = async (gameId) => {
        try {
            await this.gameService.findGame(gameId, GameStatus.IN_GAME);
            return true;
        } catch (error) {
            return false;
        }
    }

    /**
     * 게임 방 접속
     * {
     *  'gameId': 1
     * }
     */
    joinRoom = async (payload) => {
        // 채팅방 접속 db 처리 => is_connected = true로 처리
        try {
            console.log("joinRoom", payload);
            const gameId = payload.gameId;

            await this.gameService.findGame(payload.gameId, GameStatus.IN_GAME);
            await this.gameMemberService.findMemberGame(payload.gameId, this.socket.data.memberId);

            this.socket.join(gameId);
            this.socket.data.gameId = gameId;

            const data = {
                message: "joined room",
                gameId: payload.gameId,
                memberId: this.socket.data.memberId,
            }

            // gameSetting에서 참여자 수 들고오기
            // isGaneConnected true로 변경 => 변경이 됐는지 안됐는지 판별하여 
            // 게임 시작 시간 db에 저장
            await this.redisClient.setStarted(payload.gameId, this.socket.data.memberId);
            await this.gameMemberService.updateInGameConnected(payload.gameId, this.socket.data.memberId, true);
            await this.startGame();

            this.io.to(gameId).emit("get join room", data);
        } catch (error) {
            console.error("joinRoom error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    /**
     * redis에 게임 설정을 저장
     * redis에 게임 타이머를 저장
     * 게임 시작 시간을 db에 저장
     * 
     * 시작 이벤트 emit.
     * 
     * 현재 시간으로 부터 5초 뒤에 시작.
     */



    /**
     * GPS 위치 정보
     * 도둑의 탈옥 로직
     * 도둑의 이송 로직
     * 경계선 벗어남
     *  - 패널티 부여 및 초과시 체포
     * 
     * {
     *   "gameId": 10,
     *   "lat": 35.0,
     *   "lng": 129.0,
     *   "walk: 1 (걸음수),
     *   "longestSurvived" : 1 (초단위)
     * }
     */
    /**
     * redis에 게임 설정을 저장
     * redis에 게임 타이머를 저장
     * 게임 시작 시간을 db에 저장
     * 
     * 시작 이벤트 emit.
     * 
     * 현재 시간으로 부터 5초 뒤에 시작.
     */
    startGame = async () => {
        try {
            const gameId = this.socket.data.gameId;

            const integerGameId = parseInt(gameId);

            const gameSetting = await this.gameSettingService.findGameSetting(integerGameId);
            const startedCount = await this.redisClient.getStartedCount(integerGameId);
            console.log("startedCount", startedCount);

            if (startedCount < gameSetting.policeCount + gameSetting.thiefCount) {
                return;
            }

            const isMine = await this.redisClient.setGameSettingLock(integerGameId, gameSetting.timeLimit * 60);

            if (!isMine) {
                return;
            }

            await this.redisClient.setGameSetting(integerGameId, gameSetting);

            this.io.to(gameId).emit("get will start game", {
                message: "start game",
                gameId: integerGameId,
                willStartAt: new Date(Date.now() + 5000).toISOString(),
            });

            setTimeout(async () => {
                await this.redisClient.setGameTimer(integerGameId, gameSetting.timeLimit * 60);

                // cctv 작동
                await this.redisClient.setCctvTimer(integerGameId, gameSetting.cctvInterval);

                await this.gameService.updateGame({ gameId: integerGameId, startTime: new Date().toISOString() });

                this.io.to(gameId).emit("get start game", {
                    message: "start game",
                    gameId: integerGameId,
                    startTime: new Date().toISOString(),
                });
            }, 5000);
        } catch (error) {
            console.error("startGame error", error);
            sendError(this.socket, error, "GameError");
        }
    }


    /**
     * GPS 위치 정보
     * 도둑의 탈옥 로직
     * 도둑의 이송 로직
     * 경계선 벗어남
     *  - 패널티 부여 및 초과시 체포
     * 
     * {
     *   "gameId": 10,
     *   "lat": 35.0,
     *   "lng": 129.0,
     *   "walk: 1 (걸음수),
     *   "longestSurvived" : 1 (초단위)
     * }
     */
    postGps = async (payload) => {
        try {
            if (!payload || !payload.lat || !payload.lng) {
                sendError(this.socket, { code: 400, message: "Invalid payload: lat and lng are required" }, "GameError");
                return;
            }

            const gameId = this.socket.data.gameId;
            const gameTimer = await this.redisClient.getGameTimer(gameId);
            if (!gameTimer) {
                sendError(this.socket, { code: 400, message: "Game is not started or is finished" }, "GameError");
                return;
            }

            const { lat, lng, walk, longestSurvived } = payload;


            const memberId = this.socket.data.memberId; // 미들웨어에서 가져온 ID
            const gameMember = await this.gameMemberService.findMemberGame(gameId, memberId);
            const position = gameMember.givenPosition;
            const status = gameMember.status;
            const penalty = await this.redisClient.getPenalty(memberId, gameId) || 0;


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
                timestamp: new Date().toISOString() // 중요: 갱신 시간 기록
            };

            if (position === GameMemberPosition.THIEF && status === GameMemberStatus.FREE) {
                locationData.longestSurvived++;
            }

            console.log("postGps", locationData);

            // Hash에 저장 (이미 있으면 덮어쓰기됨 -> 자동 최신화)
            await this.redisClient.setLocation(memberId, gameId, locationData);

            // 경계선 확인 => turf.js 사용
            const gameSetting = await this.redisClient.getGameSetting(gameId);
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
                console.log("escape", res);
                return;
            }


            if (!isInBoundary && gameMember.givenPosition === GameMemberPosition.THIEF) {
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

                locationData.penalty = count === 3 ? 0 : count;
                await this.redisClient.setLocation(memberId, gameId, locationData);

                if (count >= 3) {
                    await this.gameMemberService.updateMemberStatus(gameId, memberId, GameMemberStatus.TRANSFER);
                    await this.redisClient.deletePenalty(memberId, gameId);

                    const isGameEnd = await this.gameService.checkGameHaveToFinish(gameId);

                    if (isGameEnd) {
                        await this.gameEnd(this.io, this.redisClient, gameId, GameMemberPosition.POLICE);
                        return;
                    }

                    this.io.to(gameId).emit("get arrest", {
                        gameId: gameId,
                        thiefId: memberId,
                        escapePointId: 3,
                        arrestedAt: new Date().toISOString(),
                    });
                    return;
                }
                this.socket.emit("out of boundary", res);
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
                console.log("modify from transfer to prison member status", res);
                return;
            }

            // 비프음
            const locationDatas = await this.redisClient.getAllLocations(gameId);
            const polices = locationDatas
                .find((data) => data.position === GameMemberPosition.POLICE)
                .map((data) => { return { policeId: data.memberId, lng: data.lng, lat: data.lat } });

            if (polices) {
                const nearPolice = this.turfService.checkNearPolice([lng, lat], polices);
                if (nearPolice) {
                    this.socket.emit("get beep use", {
                        gameId: gameId,
                        policeId: nearPolice.policeId,
                        thiefId: memberId,
                        distance: nearPolice.distance,
                    });
                }
            }

        } catch (error) {
            console.error("postGps error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    /**
     * {
        "gameId": 10,
        "policeId": 1,
        "thiefId": 2,
        "lat": 35.0,
        "lng": 129.0,
    } 
     */
    /**
     * 게임인포 동기화
     * 게임 timer는 get gps에서 계산해서 보내주므로 여기서는 계산하지 않음
     * need: 체포상태, 미션 상태
     * 
     * req : {
     *  gameId: 10
     * }
     * 
     * res : {
     *  gameId: 10,
     *  status: "IN_GAME" || "ENDED",
     *  members: [
     *    {
     *      memberId: 1,
     *      position: "POLICE",
     *      status: "FREE" || "PRISON" || "TRANSFER",
     *    },
     *  ],
     *  missions: [
     *    {
     *      "id" : "gameMissionId", 
     *       "missionId": "missionId" ,
     *       "gameId": "gameId", 
     *       "status": "SUCCESS" || "IN_PROGRESS", 
     *       "completedAt": "completedAt", 
     *       "completedBy": "completedBy"
     *    },
     *  ],
     * }
     */
    syncGameInfo = async (payload) => {
        try {
            console.log("syncGameInfo", payload);
            const { gameId } = payload;

            const game = await this.gameService.findGame(gameId);
            const gameMembers = await this.gameMemberService.findAllByGameId(gameId);
            const gameMissions = await this.gameMissionService.findAllByGameId(gameId);

            const res = {
                gameId: gameId,
                gameStatus: game.status,
                members: gameMembers.map(member => ({
                    memberId: member.memberId,
                    position: member.position,
                    status: member.status,
                })),
                missions: gameMissions
            };
            this.socket.emit("get sync game info", res);
            console.log("sync game info", res);
        } catch (error) {
            console.error("syncGameInfo error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    /**
     * {
        "gameId": 10,
        "policeId": 1,
        "thiefId": 2,
        "lat": 35.0,
        "lng": 129.0,
    } 
     */
    postArrest = async (payload) => {
        try {
            console.log("postArrest", payload);
            const { gameId, policeId, thiefId, lat, lng } = payload;


            const thief = await this.gameMemberService.findMemberGame(gameId, thiefId);

            // 도둑이 아닐때 체포 실패
            if (thief.givenPosition !== GameMemberPosition.THIEF) {
                const res = {
                    gameId: gameId,
                    policeId: policeId,
                    thiefId: thiefId,
                    result: "FAIL",
                    reason: "NOT_THIEF",
                    arrestedAt: new Date().toISOString(),
                };
                this.io.to(gameId).emit("get arrest", res);
                console.log("fail not thief", res);
                return;
            }

            // 이미 체포가 됐을때 다시 체포하면 실패
            if (thief.status && thief.status !== GameMemberStatus.FREE) {
                const res = {
                    gameId: gameId,
                    policeId: policeId,
                    thiefId: thiefId,
                    result: "FAIL",
                    reason: "ALREADY_CAUGHT",
                    arrestedAt: new Date().toISOString(),
                };
                this.io.to(gameId).emit("get arrest", res);
                console.log("fail already caught", res);
                return;
            }

            const police = await this.gameMemberService.findMemberGame(gameId, policeId);

            if (police.givenPosition !== GameMemberPosition.POLICE) {
                const res = {
                    gameId: gameId,
                    policeId: policeId,
                    thiefId: thiefId,
                    result: "FAIL",
                    reason: "ASSERTER_NOT_POLICE",
                    arrestedAt: new Date().toISOString(),
                };

                this.io.to(gameId).emit("get arrest", res);
                console.log("fail not police", res);
                return;
            }

            await this.gameService.processArrest(gameId, thiefId, policeId);

            // 정상 체포 로직
            const res = {
                gameId: gameId,
                policeId: policeId,
                thiefId: thiefId,
                result: "SUCCESS",
                reason: null,
                arrestedAt: new Date().toISOString(),
            };

            // 게임 종료 조건 검사
            const isGameEnd = await this.gameService.checkGameHaveToFinish(gameId);

            if (isGameEnd) {
                await this.gameEnd(this.io, this.redisClient, gameId, GameMemberPosition.POLICE);

                console.log("game end", gameId);
                return;
            }

            this.io.to(gameId).emit("get arrest", res);
            console.log("success arrest", res);

            // Validation and logic here
        } catch (error) {
            console.error("postArrest error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    /**
     * 스킬 사용
     * { 
     *  "gameId": 1,
     *  "policeId": 1
     * }
     */
    /**
     * 스킬 사용
     * { 
     *  "gameId": 1,
     *  "policeId": 1
     * }
     */
    postSkillUse = async (payload) => {
        try {
            console.log("postSkillUse", payload);
            const { gameId, policeId } = payload;

            // 게임 스킬 정보 조회
            const skill = await this.gameSkillService.findGameSkill(gameId, policeId);

            // 이미 사용된 스킬이면 실패
            if (skill.isUsed) {
                const res = {
                    gameId: gameId,
                    policeId: policeId,
                    result: "FAIL",
                    reason: "ALREADY_USED",
                    usedAt: new Date().toISOString(),
                };
                this.io.to(gameId).emit("get skill use", res);
                console.log("fail already used", res);
                return;
            }

            await this.gameSkillService.useSkill(skill.id);

            const res = {
                gameId: gameId,
                policeId: policeId,
                result: "SUCCESS",
                reason: null,
                startedAt: new Date(Date.now() + 5000).toISOString(),
            };

            this.io.to(gameId).emit("get skill use", res);
            console.log("success skill use", res);
        } catch (error) {
            console.error("postSkillUse error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    /**
     * mq에 이미지 전송
     * 
     * req : {
     *  gameId: 1,
     *  memberId: 1,
     *  gameMissionId: 1,
     *  imageUrl: "url" 
     * } 
     */
    /**
     * mq에 이미지 전송
     * 
     * req : {
     *  gameId: 1,
     *  memberId: 1,
     *  gameMissionId: 1,
     *  imageUrl: "url" 
     * } 
     */
    postMissionImage = async (payload) => {
        try {
            const gameId = payload.gameId;
            this.mq.sendMessage(payload, MQConfig.MQ_IMAGE);
            console.log("postMissionImage", payload);
        } catch (error) {
            console.error("postMissionImage error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    /**
     * gameId, winnerPosition
     * 
     * 게임 종료 로직
     * 1. 게임 타이머 삭제
     * 2. redis에서 멤버 스탯들 받아오기
     * 3. spring boot 서버로 요청보내기
     * 
     * 게임 상태 변경, 멤버 스탯 저장은 모두 스프링에서 진행
     */
    gameEnd = async (io, redisClient, gameId, winnerPosition) => {
        try {
            const isMine = await redisClient.setGameTimerLock(gameId);

            // 한 프로세스에서만 게임 종료 로직이 실행될 수 있게 락 설정
            if (!isMine) {
                return;
            }

            console.log("gameEnd", gameId, winnerPosition);

            // redis에서 게임 타이머 삭제
            await redisClient.deleteGameTimer(gameId);
            await redisClient.deleteGameSettingLock(gameId);

            // 패널티와 위치 정보는 5초 후 삭제
            setTimeout(async () => {
                await redisClient.deleteAllInGameCachesByGameId(gameId);
                await redisClient.deleteStartedCount(gameId);
            }, 5000);

            // 게임 종료 처리 => spring boot에 요청을 보내야함.


            // 게임 종료 알림
            // 게임 종료 알림
            io.to(gameId).emit("get end game", {
                gameId: gameId,
                winnerPosition: winnerPosition,
                message: winnerPosition === GameMemberPosition.THIEF
                    ? "시간이 모두 소진되었습니다. 게임이 종료되었습니다."
                    : "모든 도둑이 잡혔습니다. 게임이 종료되었습니다."
            });
        } catch (error) {
            console.error("gameEnd error", error);
            if (this.socket) {
                sendError(this.socket, error, "GameError");
            }
        }
    }

    /**
     * 게임 종료 후 개인이 보내는 이벤트
     * 
     * 이 이벤트가 일어나야만 redis gps 삭제 로직과 패널티 삭제로직이 실행됨.
     * 
     * {
     *   'gameId': 1,
     *   'memberId': 1,
     *   'position': 'thief',
     *   'walk': 1,
     *   'longestSurvived': 1
     * }
     */
    postGameEndAfter = async (payload) => {
        try {
            console.log("postGameEndAfter", payload);

            const { gameId, memberId, position, walk, longestSurvived } = payload;

            // 사용자의 게임 스탯 업데이트
            if (position === GameMemberPosition.THIEF) {
                await this.gameMemberService.updateThiefStats(gameId, memberId, walk, longestSurvived);
            } else {
                await this.gameMemberService.updatePoliceStats(gameId, memberId, walk);
            }

            // 사용자의 게임 캐시 삭제
            await this.redisClient.deleteGameCachesByMemberId(memberId, gameId);

        } catch (error) {
            console.error("postGameEndAfter error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    gameReset = async () => {
        try {
            console.log("gameReset");
            const gameId = this.socket.data.gameId;
            await this.redisClient.deleteAllGameCachesByGameId(gameId);
            this.io.to(gameId).emit("get reset game", {
                gameId: gameId,
                message: "게임관련 캐시가 모두 삭제되었습니다."
            });
        } catch (error) {
            console.error("gameReset error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    // custom disconnect
    disconnect = async () => {
        try {
            console.log("disconnect");

            this.socket.data.isIntentionalExit = true;

            await this.gameMemberService.updateInGameConnected(this.socket.data.gameId, this.socket.data.memberId, false);

            console.log("disconnect", this.socket.data);
            this.socket.disconnect();
        } catch (error) {
            console.error("disconnect error", error);
            // 소켓이 끊어지는 상황이므로 emit을 해도 클라이언트가 못 받을 수 있음.
            // 하지만 로깅은 중요함.
        }
    }
}