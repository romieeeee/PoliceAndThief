import redisDB from "../../global/db/redis/RedisDB.js";
import expiredChannel from "./channels/ExpiredChannel.js";
import gpsTickChannel from "./channels/GPSTickChannel.js";

export class WebSocketReconnect {
    constructor(chatIo, readyRoomIo, gameIo) {
        this.pubClient = redisDB.getPubClient();
        this.subClient = redisDB.getSubClient();
        this.chatIo = chatIo;
        this.readyRoomIo = readyRoomIo;
        this.gameIo = gameIo;
        this.chatRoomService = new ChatRoomService();
    }

    listen = async () => {
        await this.pubClient.config("SET", "notify-keyspace-events", "Ex");

        // 2. 만료 이벤트 리스너 (모든 PM2 프로세스가 이걸 실행함)
        // __keyevent@0__:expired 채널을 구독 (0번 DB 기준)
        await this.subClient.subscribe("__keyevent@0__:expired");

        await this.subClient.subscribe("global:gps:tick");

        this.subClient.on("message", async (channel, message) => {
            if (channel === "__keyevent@0__:expired") {
                expiredChannel(message, this.pubClient, this.chatIo, this.readyRoomIo, this.gameIo);
            } else if (channel === "global:gps:tick") {
                gpsTimerChannel(message, this.pubClient, this.chatIo, this.readyRoomIo, this.gameIo);
            }
        });
    }
}