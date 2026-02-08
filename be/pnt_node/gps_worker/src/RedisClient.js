import Redis from "ioredis";
import dotenv from "dotenv";

dotenv.config();

export class RedisClient {
    constructor() {
        this.client = new Redis({
            host: process.env.REDIS_HOST,
            port: process.env.REDIS_PORT
        });

        this.client.on('error', (err) => {
        });
    }

    getActiveGames = async () => {
        return await this.client.smembers("room:game:active:list");
    }

    getGameTimer = async (gameId) => {
        const gameTimer = await this.client.get(`room:game:timer:${gameId}`);
        return parseInt(gameTimer);
    }

    getGameEnd = async (gameId) => {
        const gameEnd = await this.client.get(`room:game:end:${gameId}`);
        return gameEnd;
    }

    getAllLocations = async (gameId) => {
        const locations = await this.client.hgetall(`room:game:${gameId}:locations`);
        return Object.entries(locations).map(([memberId, location]) => {
            return {
                memberId,
                gameId,
                ...JSON.parse(location)
            };
        });
    }

    // Skill Used Time
    getSkillUsedAt = async (gameId) => {
        return await this.client.get(`room:game:skill:use:lock:${gameId}`);
    }

    // CCTV User
    getCctvUser = async (gameId) => {
        return await this.client.get(`room:game:cctv:user:${gameId}`);
    }
}
