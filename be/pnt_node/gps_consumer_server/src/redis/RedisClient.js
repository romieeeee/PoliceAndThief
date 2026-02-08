import Redis from 'ioredis';
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

    publish = async (channel, message) => {
        await this.client.publish(channel, JSON.stringify(message));
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

    // ----------------------------------------------------
    // Game Logic Methods (Ported from websocket_server)
    // ----------------------------------------------------

    setLocation = async (memberId, gameId, location) => {
        await this.client.hset(this.getLocationKeyString(gameId), memberId, JSON.stringify(location));
    }

    getLocation = async (memberId, gameId) => {
        const location = await this.client.hget(this.getLocationKeyString(gameId), memberId);
        return location ? JSON.parse(location) : null;
    }

    getAllLocations = async (gameId) => {
        const locations = await this.client.hgetall(this.getLocationKeyString(gameId));
        return Object.entries(locations).map(([memberId, location]) => {
            return {
                memberId,
                gameId,
                ...JSON.parse(location)
            };
        });
    }

    getLocationKeyString = (gameId, memberId) => {
        if (memberId) {
            return `room:game:${gameId}:locations:${memberId}`;
        }
        return `room:game:${gameId}:locations`;
    }

    // Penalty
    increasePenalty = async (memberId, gameId) => {
        const lockKey = this.getPenaltyLockString(gameId, memberId);
        const isLocked = await this.client.get(lockKey);

        if (isLocked) {
            return;
        }

        const res = await this.client.hincrby(this.getPenaltyKeyString(gameId), memberId, 1);
        await this.client.set(lockKey, "1", "EX", 10);
        return res;
    }

    getPenalty = async (memberId, gameId) => {
        const penalty = await this.client.hget(this.getPenaltyKeyString(gameId), memberId);
        return parseInt(penalty) || 0;
    }

    deletePenalty = async (memberId, gameId) => {
        await this.client.hdel(this.getPenaltyKeyString(gameId), memberId);
    }

    getPenaltyKeyString = (gameId, memberId) => {
        return `room:game:penalty:${gameId}`;
    }

    getPenaltyLockString = (gameId, memberId) => {
        return `room:game:penalty:lock:${gameId}:${memberId}`;
    }

    // Game Setting
    getGameSetting = async (gameId) => {
        const gameSetting = await this.client.get(this.getGameSettingString(gameId));
        return JSON.parse(gameSetting);
    }

    getGameSettingString = (gameId) => {
        return `room:game:setting:${gameId}`;
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
