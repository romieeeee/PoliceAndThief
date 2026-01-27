import redisDB from "../../global/db/redis/RedisDB.js";
import { ChatRoomService } from "../chats/application/ChatRoomService.js";

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

        this.subClient.on("message", async (channel, message) => {
            if (channel === "__keyevent@0__:expired") {
                const key = message;

                // Key format: websocket:reconnect:timer:<namespace>:<roomId>:<userId>
                // Example: websocket:reconnect:timer:chat:123:user456
                if (key.startsWith("websocket:reconnect:timer:")) {
                    const parts = key.split(":");
                    const namespace = parts[3];
                    const roomId = parts[4];
                    const userId = parts[5];

                    // [중복 방지 핵심] 
                    // "내가 처리할게"라고 Lock을 걸어봄. (setNX: 없으면 세팅하고 true, 있으면 false)
                    // 락 자체도 5초 뒤에 사라지게 설정 (데드락 방지)
                    const isMine = await this.pubClient.set(`websocket:reconnect:lock:${userId}`, "locked", "NX", "EX", 5);

                    if (isMine) {
                        console.log(`[Process ${process.pid}] Winner! Handling disconnect for ${userId}`);

                        if (namespace === "chat") {
                            this.chatDisconnect(userId, roomId);
                        } else if (namespace === "readyRoom") {
                            this.readyRoomDisconnect(userId, roomId);
                        } else if (namespace === "game") {
                            this.gameDisconnect(userId, roomId);
                        }
                    } else {
                        console.log(`[Process ${process.pid}] User ${userId} expired, but handled by another process.`);
                    }
                }
            }
        });
    }

    chatDisconnect = async (userId, roomId) => {
        await this.deleteKeys(userId, roomId, "chat");

        console.log("user_left", { userId, roomId });

        this.chatIo.to(roomId).emit("user left", { userId });
    }

    readyRoomDisconnect = async (userId, roomId) => {
        await this.deleteKeys(userId, roomId, "readyRoom");
        this.readyRoomIo.to(roomId).emit("user left", { userId });
    }

    gameDisconnect = async (userId, roomId) => {
        await this.deleteKeys(userId, roomId, "game");

        this.gameIo.to(roomId).emit("user left", { userId });
    }

    deleteKeys = async (userId, roomId, namespace) => {
        this.pubClient.del(`websocket:reconnect:lock:${userId}`);
        this.pubClient.del(`websocket:reconnect:info:${namespace}:${userId}`);
        this.pubClient.del(`websocket:reconnect:timer:${namespace}:${roomId}:${userId}`);
    }
}