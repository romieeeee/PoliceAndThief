
import { RedisClient } from "../redis/RedisClient.js";

// Enum constants to avoid hardcoding strings
export const GameMemberPosition = {
    POLICE: 'POLICE',
    THIEF: 'THIEF'
};

export const GameMemberStatus = {
    FREE: 'FREE',
    PRISON: 'PRISON',
    TRANSFER: 'TRANSFER'
};

export class GameMemberService {
    constructor() {
        this.redisClient = new RedisClient();
    }

    findMemberGame = async (gameId, memberId) => {
        const res = await this.redisClient.getLocation(memberId, gameId);
        return res;
    }

    updateMemberStatus = async (gameId, memberId, status) => {
        const location = await this.redisClient.getLocation(memberId, gameId);

        if (!location) {
            return null;
        }

        location.status = status;
        await this.redisClient.setLocation(memberId, gameId, location);
        return location;
    }
}
