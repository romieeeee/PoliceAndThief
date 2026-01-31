import { Redis } from "ioredis";
import dotenv from "dotenv";

dotenv.config();

class RedisDB {
    constructor() {
        this.pubClient = new Redis({
            host: process.env.REDIS_HOST,
            port: process.env.REDIS_PORT
        });
        this.subClient = this.pubClient.duplicate();
        console.log("RedisDB is connected!");
    }
        
    getPubClient = () => {
        return this.pubClient;
    }

    getSubClient = () => {
        return this.subClient;
    }
}

const redisDB = new RedisDB();

export default redisDB;