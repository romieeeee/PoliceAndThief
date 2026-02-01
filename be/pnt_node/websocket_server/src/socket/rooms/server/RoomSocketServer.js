import { RoomController } from "../controller/RoomController.js";
import { resolveInSocket } from "../../../global/auth/JwtResolver.js";
import { RedisClient } from "../../utils/client/RedisClient.js";
import { sendError } from "../../../global/util/SocketError.js";

const redisClient = new RedisClient();

const roomSocketServer = (io) => {
    io.use(resolveInSocket);

    // 연결 타이머가 만료되었을 때만, inGameConnected = false로 변경 : 이건 ExpiredChannel에서 수행
    io.on("connection", async (socket) => {
        try {
            const storedRoomId = await redisClient.getStoredRoomId(socket, "room");
            console.log("storedRoomId", storedRoomId);

            socket.data.isIntentionalExit = false; // 사용자의 요청에 의해서 소켓이 종료되었는지 판별하기 위한 변수
            const roomController = new RoomController(io, socket);

            let isActiveRoom = null;
            const integerRoomId = parseInt(storedRoomId);

            if (integerRoomId) {
                isActiveRoom = await roomController.isActiveRoom(integerRoomId);
            }

            // game 방이 유효한지 검사 로직 필요.
            if (isActiveRoom && integerRoomId) {
                await redisClient.deleteByCompletedReconnect(socket, "room", integerRoomId);
                socket.data.roomId = integerRoomId;
                console.log("reconnect", integerRoomId);

                socket.emit("reconnect", { roomId: socket.data.roomId });
            }

            console.log("websocket is connected!");

            // 로비 관련 이벤트
            socket.on("post join room", roomController.joinRoom);

            socket.on("post update room info", roomController.updateRoomInfo);

            socket.on("post update ready", roomController.updateReady);

            socket.on('post now ready info', roomController.nowReadyInfo);

            socket.on('post update position', roomController.updatePreferPosition);

            socket.on("post now room info", roomController.nowRoomInfo);

            socket.on("post member kick", roomController.memberKick);

            socket.on("post disconnect", roomController.disconnect);

            socket.on("post update access token", roomController.postUpdateAccessToken);

            socket.on("post game start", roomController.gameStart);

            socket.on("post delegate owner", roomController.delegateOwner);

            socket.on("disconnect", async () => {
                if (socket.data.isIntentionalExit) {
                    console.log("socket의 연결이 정상적으로 끊어졌습니다.");
                    return;
                } else {
                    // 비정상적인 소켓 종료 => 채팅방 퇴장 db 처리 X
                    console.log(`socket의 연결이 비정상적으로 끊어졌습니다. (Room: ${socket.data.roomId})`);

                    if (socket.data.roomId) {
                        await redisClient.pubReconnectTimer("room", socket, socket.data.roomId);
                        console.log("reconnect timer published");
                    }
                }
            });
        } catch (error) {
            console.error("roomSocketServer error", error);
            sendError(socket, error, "RoomError");
        }
    });
}

export default roomSocketServer;