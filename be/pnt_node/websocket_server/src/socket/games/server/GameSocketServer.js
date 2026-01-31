import { GameController } from "../controller/GameController.js";
import { resolveInSocket } from "../../../global/auth/JwtResolver.js";
import { RedisClient } from "../../utils/client/RedisClient.js";
import MessagingQueue from "../../../global/mq/MessagingQueue.js";

const redisClient = new RedisClient();

const gameSocketServer = (io) => {
    io.use(resolveInSocket);

    // 연결 타이머가 만료되었을 때만, inGameConnected = false로 변경 : 이건 ExpiredChannel에서 수행
    io.on("connection", async (socket) => {
        const storedGameId = await redisClient.getStoredRoomId(socket, "game");

        const mq = await MessagingQueue.create();

        socket.data.isIntentionalExit = false; // 사용자의 요청에 의해서 소켓이 종료되었는지 판별하기 위한 변수
        const gameController = new GameController(io, socket, mq);

        let isActiveRoom = null;
        let integerGameId = null;

        if (storedGameId) {
            integerGameId = parseInt(storedGameId);
            isActiveRoom = await gameController.isActiveRoom(integerGameId);
        }
        // game 방이 유효한지 검사 로직 필요.
        if (isActiveRoom) {
            try {
                await redisClient.deleteByCompletedReconnect(socket, "game", storedGameId);
                await gameController.gameMemberService.updateInGameConnected(integerGameId, socket.data.memberId, true);
                console.log("reconnect", storedGameId);

                socket.emit("reconnect", { gameId: storedGameId });
            } catch (error) {
                console.error(`[GameSocketServer] Reconnect failed partially for user ${socket.data.memberId}:`, error.message);
                // 진행을 막지 않음. 소켓은 이미 룸에 조인되어 있음(deleteByCompletedReconnect 내부에서).
            }
        }

        console.log("websocket is connected!");

        // 게임 관련 이벤트
        socket.on("post join room", gameController.joinRoom);
        socket.on("post gps", gameController.postGps);
        socket.on("post arrest", gameController.postArrest);
        socket.on("post skill use", gameController.postSkillUse);
        socket.on("post mission image", gameController.postMissionImage);
        socket.on("post after game end", gameController.postGameEndAfter);
        socket.on("post sync game info", gameController.syncGameInfo);

        socket.on("post reset game", gameController.gameReset);

        socket.on("post disconnect", gameController.disconnect);

        socket.on("post retry end game", gameController.retryEndGame);

        socket.on("post update access token", gameController.postUpdateAccessToken);

        socket.on("disconnect", async () => {
            if (socket.data.isIntentionalExit) {
                console.log("socket의 연결이 정상적으로 끊어졌습니다.");
                return;
            } else {
                // 비정상적인 소켓 종료 => 채팅방 퇴장 db 처리 X
                console.log(`socket의 연결이 비정상적으로 끊어졌습니다. (Room: ${socket.data.gameId})`);

                if (socket.data.gameId) {
                    await redisClient.pubReconnectTimer("game", socket, socket.data.gameId);
                }
            }
        });
    });
}

export default gameSocketServer;