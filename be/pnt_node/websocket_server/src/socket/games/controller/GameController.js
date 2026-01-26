import { GameService } from "../application/GameService.js";
import { GameMemberService } from "../application/GameMemberService.js";
import { sendError } from "../../../global/util/SocketError.js";

export class GameController {
    constructor(io, socket, pubClient) {
        this.io = io;
        this.socket = socket;
        this.pubClient = pubClient;
        this.gameService = new GameService();
        this.gameMemberService = new GameMemberService();
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


    postGps = async (payload) => {
        try {
            if (!payload || !payload.lat || !payload.lng) {
                throw { code: 400, message: "Invalid payload: lat and lng are required" };
            }

            const { lat, lng, position, status } = payload;
            const gameId = this.socket.data.gameId;
            const memberId = this.socket.data.memberId; // 미들웨어에서 가져온 ID

            const locationData = JSON.stringify({
                lat,
                lng,
                memberId, // 클라이언트 편의를 위해 포함
                gameId,
                position,
                status,
                timestamp: Date.now() // 중요: 갱신 시간 기록
            });

            // Hash에 저장 (이미 있으면 덮어쓰기됨 -> 자동 최신화)
            await this.pubClient.hset(`room:${gameId}:locations`, memberId, locationData);
        } catch (error) {
            console.error("postGps error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    postArrest = async (payload) => {
        try {
            console.log("postArrest", payload);
            // Validation and logic here
        } catch (error) {
            console.error("postArrest error", error);
            sendError(this.socket, error, "GameError");
        }
    }

    postSkillUse = async (payload) => {
        try {
            console.log("postSkillUse", payload);
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
                await this.pubClient.hdel(`room:${this.socket.data.gameId}:locations`, this.socket.data.memberId);
            }

            // have to spring 요청 -> in_game_connected = false로 변경

            this.socket.disconnect();
        } catch (error) {
            console.error("disconnect error", error);
            // 소켓이 끊어지는 상황이므로 emit을 해도 클라이언트가 못 받을 수 있음.
            // 하지만 로깅은 중요함.
        }
    }
}