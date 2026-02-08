import { RedisClient } from "./RedisClient.js";
import { Emitter } from "@socket.io/redis-emitter";
import Redis from "ioredis";
import dotenv from "dotenv";

dotenv.config();

const redisClient = new RedisClient();
const pubClient = new Redis({
    host: process.env.REDIS_HOST,
    port: process.env.REDIS_PORT
});
const io = new Emitter(pubClient);

const NAMESPACE = "/game";

const interval = 1000; // 1초

const processGame = async (id) => {
    try {
        const gameId = parseInt(id);
        const startTime = await redisClient.getGameTimer(gameId);
        const endTime = await redisClient.getGameEnd(gameId);

        // 게임이 종료되었거나 시작 시간이 없으면 패스
        if (endTime || !startTime) {
            return;
        }

        const locations = await redisClient.getAllLocations(gameId);
        if (locations.length === 0) return;

        const skillUsedAt = await redisClient.getSkillUsedAt(gameId);
        const cctvUser = await redisClient.getCctvUser(gameId);

        const data = {
            gameId: parseInt(gameId),
            sec: 0,
            skillUsedAt: skillUsedAt ? skillUsedAt : null,
            cctvThiefId: cctvUser ? cctvUser : null,
            locations: locations
        };

        if (startTime) {
            data.sec = Math.round((Date.now() - startTime) / 1000);
        }

        // socket.io-emitter를 사용하여 특정 룸(gameId)에 브로드캐스트
        // volatile 플래그 사용: 메시지 유실 허용 (실시간성 보장)
        // 중요: 클라이언트는 "/game" 네임스페이스에 연결되어 있으므로, .of("/game")을 명시해야 함
        io.of(NAMESPACE).to(gameId).volatile.emit("get gps", data);

    } catch (error) {
        console.error(`Error processing game ${gameId}:`, error);
    }
}

const loop = async () => {
    try {
        const activeGames = await redisClient.getActiveGames();

        if (activeGames.length > 0) {
            // 병렬 처리: 모든 게임에 대해 동시에 작업 수행
            await Promise.all(activeGames.map(gameId => processGame(gameId)));
        }
    } catch (error) {
    } finally {
    }
};

// 1초마다 실행
setInterval(loop, 1000);
