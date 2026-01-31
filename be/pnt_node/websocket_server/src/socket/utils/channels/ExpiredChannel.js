import { RedisClient } from "../client/RedisClient.js";
import { GameController } from "../../games/controller/GameController.js";
import { GameMemberPosition } from "../../../global/db/sequelize/status/GameMemberPosition.js";
import { GameMemberStatus } from "../../../global/db/sequelize/status/GameMemberStatus.js";
import axios from "axios";

const redisClient = new RedisClient();
const gameController = new GameController();

const expiredChannel = async (message, pubClient, chatIo, roomIo, gameIo) => {

    const key = message;
    console.log(`[ExpiredChannel] Received expired key: ${key}`);

    // Key format: room:game:timer:${gameId}
    // Example: room:game:timer:123
    if (key.startsWith(redisClient.GAME_TIMER_PREFIX)) {
        const parts = key.split(":");
        const gameId = parts[3];

        if (gameId === 'lock') return;

        // 게임 종료 처리 => 컨트롤러에서 처리
        console.log("game end", gameId);
        gameController.gameEnd(gameIo, redisClient, gameId, GameMemberPosition.THIEF);
    }

    // Key format: room:game:cctv:${gameId}
    // Example: room:game:cctv:123
    else if (key.startsWith(redisClient.CCTV_TIMER_PREFIX)) {
        const parts = key.split(":");
        const gameId = parts[3];

        // 게임 중인 유저들의 위치 정보 조회
        const locations = await redisClient.getAllLocations(gameId);

        // 1. 도둑만 필터링
        // 2. 상태가 FREE 인 유저만 필터링
        const thieves = locations.filter(player =>
            player.position === GameMemberPosition.THIEF &&
            (!player.status || player.status === GameMemberStatus.FREE)
            && player.isConnected === true && !redisClient.getMission(gameId, player.memberId)
        );

        // 도둑의 수가 적으면 CCTV를 보내지 않음. => 이건 정해야함.
        if (thieves.length > 0) {
            const randomIndex = Math.floor(Math.random() * thieves.length);
            const randomThief = thieves[randomIndex];

            gameIo.to(gameId).emit("get cctv", {
                gameId: parseInt(gameId),
                thiefId: randomThief.memberId,
                lng: randomThief.lng,
                lat: randomThief.lat
            });
            console.log(`[CCTV] Game ${gameId}: Sent CCTV data for thief ${randomThief.memberId}`);
        } else {
            console.log(`[CCTV] Game ${gameId}: No free thieves found.`);
        }

        const gameTimer = await redisClient.getGameTimer(gameId);
        const gameSetting = await redisClient.getGameSetting(gameId);

        if (gameTimer && gameSetting) {
            const cctvInterval = gameSetting.cctvInterval || 60;
            await redisClient.setCctvTimer(gameId, cctvInterval);
        }
    }
    // Key format: room:game:end:${gameId}
    // Example: room:game:end:123
    else if (key.startsWith(redisClient.GAME_END_PREFIX)) {
        try {
            const parts = key.split(":");
            const gameId = parseInt(parts[3]);

            await redisClient.deleteGameEnd(gameId);

            await redisClient.deleteAllInGameCachesByGameId(gameId);

            // 여긴 승환이가 다 만들어주면 그때 하면됨.
            const response = await axios.post(`${process.env.SPRING_BOOT_URL}/rooms/${gameId}/reset`, {
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${await redisClient.getGameToken(gameId)}`
                }
            });

            gameIo.to(gameId).emit("get game reset", { gameId: parseInt(gameId) });
        } catch (error) {
            console.error("Error in expiredChannel (game end):", error);
        }
    }
}


export default expiredChannel;