export class GameController {
    constructor(io, socket, pubClient) {
        this.io = io;
        this.socket = socket;
        this.pubClient = pubClient;
    }

    joinRoom = async (payload) => {
        const gameId = `game:${payload.gameId}`;

        this.socket.join(gameId);
        this.socket.data.gameId = gameId;

        console.log(gameId, this.socket.data.memberId);

        console.log("rooms", this.io.sockets.rooms);

        // 채팅방 접속 db 처리 => is_connected = true로 처리
        try {
            // await this.chatRoomService.findGame(gameId);
            // await this.chatRoomService.findMemberGame(gameId, this.socket.data.memberId);

            const data = {
                "message": "joined room",
                "gameId": gameId
            }

            this.io.to(gameId).emit("get join room", data);
        } catch (error) {
            console.error("joinRoom error", error);
            if (handleErrors.includes(error.code)) {
                this.socket.emit("error", { ex: error.message, text: error.text, code: error.code });
            } else {
                this.socket.emit("error", { ex: "InternalServeError", text: "서버에 문제가 있습니다.", code: 500 });
            }
        }
    }


    postGps = async (payload) => {
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
    }

    postArrest = async (payload) => {
        console.log("postArrest", payload);
    }

    postSkillUse = async (payload) => {
        console.log("postSkillUse", payload);
    }

    postMissionImage = async (payload) => {
        console.log("postMissionImage", payload);
    }

    disconnect = async () => {
        console.log("disconnect");

        this.socket.data.isIntentionalExit = true;

        this.socket.disconnect();

        // redis gps 삭제
        await this.pubClient.hdel(`room:${this.socket.data.gameId}:locations`, this.socket.data.memberId);
    }
}