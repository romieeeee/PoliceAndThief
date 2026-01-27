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

export class GameController {
    constructor(io, socket) {
        this.io = io;
        this.socket = socket;
        this.redisClient = new RedisClient();
        this.gameService = new GameService();
        this.gameMemberService = new GameMemberService();
        this.gameSettingService = new GameSettingService();
        this.gameSkillService = new GameSkillService();
        this.gameMemberStatService = new GameMemberStatService();
    }

    isActiveRoom = async (gameId) => {
        try {
            await this.gameService.findGame(gameId, GameStatus.PLAYING);
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
            const gameId = `game-${payload.gameId}`;

            await this.gameService.findGame(payload.gameId, GameStatus.RUNNING);
            await this.gameMemberService.findMemberGame(payload.gameId, this.socket.data.memberId);
            await this.gameMemberService.updateInGameConnected(payload.gameId, this.socket.data.memberId, true);

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
            await this.redisClient.setStarted(this.socket.data.gameId, this.socket.data.memberId);
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
    startGame = async () => {
        try {
            const gameId = this.socket.data.gameId;

            const integerGameId = parseInt(gameId.split("-")[1]);

            const gameSetting = await this.gameSettingService.findGameSetting(integerGameId);
            const startedCount = await this.redisClient.getStartedCount(gameId);

            if (startedCount < gameSetting.policeCount + gameSetting.thiefCount) {
                return;
            }

            const isMine = await this.redisClient.setGameSettingLock(gameId);

            if (!isMine) {
                return;
            }

            await this.redisClient.setGameSetting(gameId, gameSetting);

            this.io.to(gameId).emit("get start game", {
                message: "start game",
                gameId: integerGameId,
                willStartAt: new Date(Date.now() + 5000).toISOString(),
            });

            setTimeout(async () => {
                await this.redisClient.setGameTimer(gameId, gameSetting.timeLimit);
                
                await this.gameService.updateGame({gameId: integerGameId, startTime: new Date().toISOString()});
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
                throw { code: 400, message: "Invalid payload: lat and lng are required" };
            }

            const { lat, lng, walk, longestSurvived } = payload;
            const gameId = this.socket.data.gameId;
            const integerGameId = parseInt(gameId.split("-")[1]);
            const memberId = this.socket.data.memberId; // 미들웨어에서 가져온 ID
            const gameMember = await this.gameMemberService.findMemberGame(integerGameId, memberId);
            const position = gameMember.givenPosition;
            const status = gameMember.status;
            

            const locationData = JSON.stringify({
                lat,
                lng,
                memberId, // 클라이언트 편의를 위해 포함
                gameId: integerGameId,
                walk,
                longestSurvived,
                position,
                status,
                timestamp: new Date().toISOString() // 중요: 갱신 시간 기록
            });

            // Hash에 저장 (이미 있으면 덮어쓰기됨 -> 자동 최신화)
            await this.redisClient.setLocation(memberId, integerGameId, locationData);

            // 경계선 확인 => turf.js 사용
            const isInBoundary = await this.gameSettingService.checkUserInBoundary(integerGameId, lng, lat);

            if (!isInBoundary) {
                const res = {
                    gameId: integerGameId,
                    message: "out of boundary",
                    type: "outOfBoundary",
                    createdAt: new Date().toISOString(),
                };

                this.socket.emit("out of boundary", res);

                // 도둑일 때 경계선을 벗어났으면 패널티 부여 -> redis에서 관리.
                // 만약 3번 이상 벗어나면 자동으로 감옥으로 이송. -> 다음 패널티 체크 활성화까지 10초
                await this.redisClient.increasePenalty(memberId, gameId);

                return;
            }

            // 도둑이고, 상태가 PRISON 일때 탈옥 판별
            // 감옥에서 10m 이상 벗어났을때 탈옥 (오차범위 5m) 
            if (position === GameMemberPosition.THIEF && status === GameMemberStatus.PRISON
                && !(await this.gameSettingService.checkUserInPrison(integerGameId, lng, lat))) {

                await this.gameMemberService.updateMemberStatus(integerGameId, memberId, GameMemberStatus.FREE);

                const res = {
                    gameId: integerGameId,
                    thiefId: memberId,
                    escapePointId: 3,
                    escapedAt: new Date().toISOString(),
                };
                this.io.to(gameId).emit("get escape", res);
                console.log("escape", res);
                return;
            }

            // 이송 중이고, 감옥 범위 안에 들어왔을때
            if (position === GameMemberPosition.THIEF && status === GameMemberStatus.TRANSFER
                && await this.gameSettingService.checkUserInPrison(integerGameId, lng, lat)) {

                await this.gameMemberService.updateMemberStatus(integerGameId, memberId, GameMemberStatus.PRISON);
                const res = {
                    gameId: integerGameId,
                    thiefId: memberId,
                    status: GameMemberStatus.PRISON,
                    arrestedAt: new Date().toISOString(),
                };
                this.io.to(gameId).emit("modify member status", res);
                console.log("modify from transfer to prison member status", res);
                return;
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
    postArrest = async (payload) => {
        try {
            console.log("postArrest", payload);
            const { strGameId, policeId, thiefId, lat, lng } = payload;
            const integerGameId = parseInt(strGameId);

            const gameId = `game-${strGameId}`;

            const thief = await this.gameMemberService.findMemberGame(integerGameId, thiefId);

            // 도둑이 아닐때 체포 실패
            if (thief.position !== GameMemberPosition.THIEF) {
                const res = {
                    gameId: integerGameId,
                    policeId: integerPoliceId,
                    thiefId: integerThiefId,
                    result: "FAIL",
                    reason: "NOT_THIEF",
                    arrestedAt: new Date().toISOString(),
                };
                this.io.to(gameId).emit("get arrest", res);
                console.log("fail not thief", res);
                return;
            }

            // 이미 체포가 됐을때 다시 체포하면 실패
            if (thief.status !== GameMemberStatus.FREE) {
                const res = {
                    gameId: integerGameId,
                    policeId: integerPoliceId,
                    thiefId: integerThiefId,
                    result: "FAIL",
                    reason: "ALREADY_CAUGHT",
                    arrestedAt: new Date().toISOString(),
                };
                this.io.to(gameId).emit("get arrest", res);
                console.log("fail already caught", res);
                return;
            }

            const police = await this.gameMemberService.findMemberGame(integerGameId, policeId);

            if (police.position !== GameMemberPosition.POLICE) {
                const res = {
                    gameId: integerGameId,
                    policeId: policeId,
                    thiefId: integerThiefId,
                    result: "FAIL",
                    reason: "ASSERTER_NOT_POLICE",
                    arrestedAt: new Date().toISOString(),
                };

                this.io.to(gameId).emit("get arrest", res);
                console.log("fail not police", res);
                return;
            }

            await this.gameService.processArrest(integerGameId, thiefId, policeId);

            // 정상 체포 로직
            const res = {
                gameId: integerGameId,
                policeId: policeId,
                thiefId: integerThiefId,
                result: "SUCCESS",
                reason: null,
                arrestedAt: new Date().toISOString(),
            };

            // 게임 종료 조건 검사
            const isGameEnd = await this.gameService.checkGameHaveToFinish(integerGameId);

            if (isGameEnd) {
                await this.gameEnd(integerGameId, GameMemberPosition.THIEF);

                console.log("game end", integerGameId);
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

            // Validation and logic here
        } catch (error) {
            console.error("postSkillUse error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    postMissionImage = async (payload) => {
        try {
            console.log("postMissionImage", payload);
            // Validation and logic here
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
            // 패널티와 위치 정보는 5초 후 삭제
            setTimeout(async () => {
                await redisClient.deleteAllInGameCachesByGameId(gameId);
            }, 5000);

            // 게임 종료 처리 => spring boot에 요청을 보내야함.
            

            // 게임 종료 알림
            io.to(gameId).emit("get end game", {
                gameId: gameId,
                winnerPosition: winnerPosition,
                message: winnerPosition === GameMemberPosition.THIEF
                    ? "모든 도둑이 잡혔습니다. 게임이 종료되었습니다."
                    : "시간이 모두 소진되었습니다. 게임이 종료되었습니다."
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
            const stringGameId = `game-${gameId}`;
            await this.redisClient.deleteGameCachesByMemberId(memberId, stringGameId);


        } catch (error) {
            console.error("postGameEndAfter error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    // custom disconnect
    disconnect = async () => {
        try {
            console.log("disconnect");

            this.socket.data.isIntentionalExit = true;

            const integerGameId = parseInt(this.socket.data.gameId.split("-")[1]);

            await this.gameMemberService.updateInGameConnected(integerGameId, this.socket.data.memberId, false);

            // redis gps 삭제
            if (this.socket.data.gameId && this.socket.data.memberId) {
                await this.redisClient.deleteLocation(this.socket.data.memberId, this.socket.data.gameId);
            }

            console.log("disconnect", this.socket.data);
            this.socket.disconnect();
        } catch (error) {
            console.error("disconnect error", error);
            // 소켓이 끊어지는 상황이므로 emit을 해도 클라이언트가 못 받을 수 있음.
            // 하지만 로깅은 중요함.
        }
    }
}