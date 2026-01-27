import { RedisClient } from "../client/RedisClient.js";

export class GpsTimerChannel {
    constructor(gameIo) {
        this.gameIo = gameIo;
        this.redisClient = new RedisClient();
    }

    start = () => {
        setInterval(async () => {
            const rooms = this.gameIo.adapter.rooms;

            for (const [roomId, sockets] of rooms) {
                if (!roomId.startsWith("game:")) continue;

                const locations = await this.redisClient.getAllLocations(roomId);

                if (Object.keys(locations).length === 0) continue;

                const data = {
                    gameId: parseInt(roomId.split(":")[1]),
                    locations: locations
                }
                // volatile: 클라이언트가 연결을 유지하지 않는 경우에도 데이터를 전송 -> tcp 보장 X
                // local: redis를 거치지 않고, 현재 연결되어있는 소켓에만 데이터를 전송
                this.gameIo.to(roomId).volatile.local.emit("get gps", data);
            }
        }, 1000);
    }
}