import { GameController } from "./controller/GameController.js";
import { resolveInSocket } from "../../global/auth/JwtResolver.js";

const gameSocketServer = (io, pubClient) => {
    io.use(resolveInSocket);

    io.on("connection", async (socket) => {
        const infoKey = `websocket:reconnect:info:game:${socket.data.memberId}`;
        const storedGameId = await pubClient.get(infoKey);

        // game 방이 유효한지 검사 로직 필요.

        if (storedGameId) {
            console.log(`[Reconnect] Restoring user ${socket.data.memberId} to room ${storedGameId}`);
            socket.join(storedGameId);
            socket.data.gameId = storedGameId;

            // Delete keys to cancel expiration event
            const timerKey = `websocket:reconnect:timer:game:${storedGameId}:${socket.data.memberId}`;
            await pubClient.del(infoKey);
            await pubClient.del(timerKey);

            socket.emit("reconnect", { gameId: storedGameId });
        }

        console.log("websocket is connected!");

        socket.data.isIntentionalExit = false; // 사용자의 요청에 의해서 소켓이 종료되었는지 판별하기 위한 변수
        const gameController = new GameController(io, socket, pubClient);

        // 게임 관련 이벤트
        socket.on("post join room", gameController.joinRoom);
        socket.on("post gps", gameController.postGps);
        socket.on("post arrest", gameController.postArrest);
        socket.on("post skill use", gameController.postSkillUse);
        socket.on("post mission image", gameController.postMissionImage);

        socket.on("post disconnect", gameController.disconnect);

        socket.on("disconnect", async () => {
            if (socket.data.isIntentionalExit) {
                console.log("socket의 연결이 정상적으로 끊어졌습니다.");
                return;
            } else {
                // 비정상적인 소켓 종료 => 채팅방 퇴장 db 처리 X
                console.log(`socket의 연결이 비정상적으로 끊어졌습니다. (Room: ${socket.data.gameId})`);

                if (socket.data.gameId) {
                    const timerKey = `websocket:reconnect:timer:game:${socket.data.gameId}:${socket.data.memberId}`;
                    const infoKey = `websocket:reconnect:info:game:${socket.data.memberId}`;

                    // 1. Timer Key: Expiration event trigger (Value not important)
                    await pubClient.set(timerKey, "timer", "EX", 60);

                    // 2. Info Key: Data storage for reconnection (Value = chatRoomId)
                    // Set to 61s to ensure it survives slightly longer than the timer (race condition safety)
                    await pubClient.set(infoKey, socket.data.gameId, "EX", 61);
                }
            }
        });
    });
}

export default gameSocketServer;