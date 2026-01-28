import { RedisClient } from "../client/RedisClient.js";
import { GameController } from "../../games/controller/GameController.js";
import { GameMemberPosition } from "../../../global/db/sequelize/status/GameMemberPosition.js";
import axios from "axios";

const redisClient = new RedisClient();
const gameController = new GameController();

const expiredChannel = async (message, pubClient, chatIo, roomIo, gameIo) => {

    const key = message;

    // Key format: websocket:reconnect:timer:<namespace>:<roomId>:<userId>
    // Example: websocket:reconnect:timer:chat:123:user456
    if (key.startsWith(redisClient.RECONNECT_PREFIX)) {
        const parts = key.split(":");
        const namespace = parts[3];
        const roomId = parts[4];
        const memberId = parts[5];

        // [중복 방지 핵심] 
        // "내가 처리할게"라고 Lock을 걸어봄. (setNX: 없으면 세팅하고 true, 있으면 false)
        // 락 자체도 5초 뒤에 사라지게 설정 (데드락 방지)
        const isMine = await redisClient.setReconnectLock(memberId);

        if (isMine) {
            console.log(`[Process ${process.pid}] Winner! Handling disconnect for ${memberId}`);

            if (namespace === "chat") {
                chatDisconnect(memberId, roomId, chatIo, pubClient);
            } else if (namespace === "room") {
                roomDisconnect(memberId, roomId, roomIo, pubClient);
            } else if (namespace === "game") {
                gameDisconnect(memberId, roomId, gameIo, pubClient);
            }
        } else {
            // console.log(`[Process ${process.pid}] User ${memberId} expired, but handled by another process.`);
        }
    }

    // Key format: room:game:timer:${gameId}
    // Example: room:game:timer:123
    else if (key.startsWith(redisClient.GAME_TIMER_PREFIX)) {
        const parts = key.split(":");
        const gameId = parts[3];

        if (gameId === 'lock') return;

        // 게임 종료 처리 => 컨트롤러에서 처리
        gameController.gameEnd(gameIo, redisClient, gameId, GameMemberPosition.THIEF);
    }
}

const chatDisconnect = async (memberId, roomId, chatIo) => {
    await redisClient.deleteKeys("chat", roomId, memberId);

    console.log("user_left", { memberId, roomId });

    chatIo.to(roomId).emit("user left", { memberId });
}

// => 비정상 로직이니까 만약 아무도 없다면 방 삭제
// => 이거는 그냥 api 호출하면 됨.
const roomDisconnect = async (memberId, roomId, roomIo) => {
    await redisClient.deleteKeys("room", roomId, memberId);

    const response = await axios.delete(`${process.env.SPRING_API_URL}/spring/rooms/${roomId}/members/me`, {
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${this.socket.data.accessToken}`
        }
    });

    if (response.status !== 200) {
        // 에러 처리 해야함. => 이건 무식한 방법이긴 한데 어쩔 수 없다. 유저가 이미 소켓을 끊은 상황이기때문에... => mq 넣는것 말고는 방법이 없는것 같다.
        await axios.delete(`${process.env.SPRING_API_URL}/spring/rooms/${roomId}/members/me`, {
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${this.socket.data.accessToken}`
            }
        });
    }
    console.log("user_left", { memberId, roomId });
    roomIo.to(roomId).emit("user left", { memberId });
}

// => 비정상 로직이니까 만약 아무도 없다면 방 삭제 => 연쇄로 다 삭제.
const gameDisconnect = async (memberId, roomId, gameIo) => {
    // 게임 접속 정보 업데이트
    await gameController.gameMemberService.updateInGameConnected(roomId, memberId, false);

    await redisClient.deleteKeys("game", roomId, memberId);

    // RedisClient를 사용하여 위치 정보 및 패널티 정보 삭제
    await redisClient.deleteLocation(memberId, roomId);

    gameIo.to(roomId).emit("user left", { memberId });
}


export default expiredChannel;