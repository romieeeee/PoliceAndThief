import { GameService } from "../application/GameService.js";
import { GameMemberService } from "../application/GameMemberService.js";
import { sendError } from "../../../global/util/SocketError.js";
import { GameSettingService } from "../application/GameSettingService.js";
import { GameMemberPosition } from "../../../global/db/sequelize/status/GameMemberPosition.js";
import { GameMemberStatus } from "../../../global/db/sequelize/status/GameMemberStatus.js";
import { GameSkillService } from "../application/GameSkillService.js";
import { GameMemberStatService } from "../application/GameMemberStatService.js";
import { RedisClient } from "../../utils/client/RedisClient.js";

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

    joinRoom = async (payload) => {

        // 채팅방 접속 db 처리 => is_connected = true로 처리
        try {
            const gameId = `game:${payload.gameId}`;

            await this.gameService.findGame(payload.gameId);
            await this.gameMemberService.findMemberGame(payload.gameId, this.socket.data.memberId);

            this.socket.join(gameId);
            this.socket.data.gameId = gameId;

            const data = {
                message: "joined room",
                gameId: payload.gameId,
                memberId: this.socket.data.memberId,
            }

            this.io.to(gameId).emit("get join room", data);
        } catch (error) {
            console.error("joinRoom error", error);
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
     */
    postGps = async (payload) => {
        try {
            if (!payload || !payload.lat || !payload.lng) {
                throw { code: 400, message: "Invalid payload: lat and lng are required" };
            }

            const { lat, lng, position, status } = payload;
            const gameId = this.socket.data.gameId;
            const integerGameId = parseInt(gameId.split(":")[1]);
            const memberId = this.socket.data.memberId; // 미들웨어에서 가져온 ID

            const locationData = JSON.stringify({
                lat,
                lng,
                memberId, // 클라이언트 편의를 위해 포함
                gameId: integerGameId,
                position,
                status,
                timestamp: Date.now() // 중요: 갱신 시간 기록
            });

            // Hash에 저장 (이미 있으면 덮어쓰기됨 -> 자동 최신화)
            await this.redisClient.setLocation(memberId, integerGameId, locationData);
            
            console.log("set gps", locationData);
            // 경계선 확인
            const isInBoundary = await this.gameSettingService.checkUserInBoundary(integerGameId, lng, lat);

            if (!isInBoundary) {
                this.socket.emit("alarm", {
                    gameId: integerGameId,
                    message: "out of boundary",
                    type: "outOfBoundary",
                    createdAt: Date.now(),
                });

                // 도둑일 때 경계선을 벗어났으면 패널티 부여 -> redis에서 관리.
                // 만약 3번 이상 벗어나면 자동으로 감옥으로 이송.
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
        "clientArrestId": "string"
    } 
     */
    postArrest = async (payload) => {
        try {
            console.log("postArrest", payload);
            const { gameId, policeId, thiefId, lat, lng, clientArrestId } = payload;
            const integerGameId = parseInt(gameId);
            const integerPoliceId = parseInt(policeId);
            const integerThiefId = parseInt(thiefId);
            const integerLat = parseInt(lat);
            const integerLng = parseInt(lng);

            const thief = await this.gameMemberService.findMemberGame(integerGameId, integerThiefId);

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

            const police = await this.gameMemberService.findMemberGame(integerGameId, integerPoliceId);

            if (police.position !== GameMemberPosition.POLICE) {
                const res = {
                    gameId: integerGameId,
                    policeId: integerPoliceId,
                    thiefId: integerThiefId,
                    result: "FAIL",
                    reason: "NOT_POLICE",
                    arrestedAt: new Date().toISOString(),
                };

                this.io.to(gameId).emit("get arrest", res);
                console.log("fail not police", res);
                return;
            }

            await this.gameService.processArrest(integerGameId, integerThiefId, integerPoliceId);

            // 정상 체포 로직
            const res = {
                gameId: integerGameId,
                policeId: integerPoliceId,
                thiefId: integerThiefId,
                result: "SUCCESS",
                reason: null,
                arrestedAt: new Date().toISOString(),
            };

            this.io.to(gameId).emit("get arrest", res);
            console.log("success arrest", res);

            // Validation and logic here
        } catch (error) {
            console.error("postArrest error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    postSkillUse = async (payload) => {
        try {
            console.log("postSkillUse", payload);
            const { gameId, policeId } = payload;
            const integerGameId = parseInt(gameId);
            const integerMemberId = parseInt(policeId);

            const skill = await this.gameSkillService.findGameSkill(integerGameId, integerPoliceId);

            await this.gameSkillService.useSkill(skill.id);

            const res = {
                gameId: integerGameId,
                policeId: integerMemberId,
                startedAt: new Date().toISOString(),
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

    // custom disconnect
    disconnect = async () => {
        try {
            console.log("disconnect");

            this.socket.data.isIntentionalExit = true;

            // redis gps 삭제
            if (this.socket.data.gameId && this.socket.data.memberId) {
                await this.redisClient.deleteLocation(this.socket.data.memberId, this.socket.data.gameId);
            }

            // have to spring 요청 -> in_game_connected = false로 변경

            console.log("disconnect", this.socket.data);
            this.socket.disconnect();
        } catch (error) {
            console.error("disconnect error", error);
            // 소켓이 끊어지는 상황이므로 emit을 해도 클라이언트가 못 받을 수 있음.
            // 하지만 로깅은 중요함.
        }
    }
}