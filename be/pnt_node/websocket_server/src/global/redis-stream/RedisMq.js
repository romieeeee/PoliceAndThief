import redisDB from "../db/redis/RedisDB";
import dotenv from "dotenv";

dotenv.config();

const STREAM_NAME = `post-gps-stream`;

class RedisMq {
    redisClient;

    create = () => {
        this.redisClient = redisDB.getPubClient();
    }

    publish = async (message) => {
        await this.redisClient.xadd(STREAM_NAME, "MAXLEN", "~", "1000", "*", "message", JSON.stringify(message));
    }

    close = async () => {
        await this.redisClient.disconnect();
    }
}

const redisMq = new RedisMq();
redisMq.create();

export default redisMq;
