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

        const isActiveRoom = await gameController.isActiveRoom(storedGameId);

        // game 방이 유효한지 검사 로직 필요.
        if (isActiveRoom) {
            await redisClient.deleteByCompletedReconnect(socket, "game", storedGameId);
            const integerGameId = parseInt(storedGameId.split("-")[1]);
            await gameController.gameMemberService.updateInGameConnected(integerGameId, socket.data.memberId, true);

            socket.emit("reconnect", { gameId: storedGameId });
        }

        console.log("websocket is connected!");

        // 게임 관련 이벤트
        socket.on("post join room", gameController.joinRoom);
        socket.on("post gps", gameController.postGps);
        socket.on("post arrest", gameController.postArrest);
        socket.on("post skill use", gameController.postSkillUse);
        socket.on("post mission image", gameController.postMissionImage);
        // socket.on("post after game end", gameController.postGameEndAfter);
        socket.on("post sync game info", gameController.syncGameInfo);

        socket.on("post disconnect", gameController.disconnect);

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