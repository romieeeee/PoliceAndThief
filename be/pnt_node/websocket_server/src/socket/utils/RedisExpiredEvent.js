import redisDB from "../../global/db/redis/RedisDB.js";
import { ChatRoomService } from "../chats/application/ChatRoomService.js";
import { RedisClient } from "./client/RedisClient.js";

export class WebSocketReconnect {
    constructor(chatIo, readyRoomIo, gameIo) {
        this.pubClient = redisDB.getPubClient();
        this.subClient = redisDB.getSubClient();
        this.chatIo = chatIo;
        this.readyRoomIo = readyRoomIo;
        this.gameIo = gameIo;
        this.chatRoomService = new ChatRoomService();
        this.redisClient = new RedisClient();
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
                if (key.startsWith(this.redisClient.RECONNECT_PREFIX)) {
                    const parts = key.split(":");
                    const namespace = parts[3];
                    const roomId = parts[4];
                    const memberId = parts[5];

                    // [중복 방지 핵심] 
                    // "내가 처리할게"라고 Lock을 걸어봄. (setNX: 없으면 세팅하고 true, 있으면 false)
                    // 락 자체도 5초 뒤에 사라지게 설정 (데드락 방지)
                    const isMine = await this.redisClient.setReconnectLock(memberId);

                    if (isMine) {
                        console.log(`[Process ${process.pid}] Winner! Handling disconnect for ${memberId}`);

                        if (namespace === "chat") {
                            this.chatDisconnect(memberId, roomId);
                        } else if (namespace === "readyRoom") {
                            this.readyRoomDisconnect(memberId, roomId);
                        } else if (namespace === "game") {
                            this.gameDisconnect(memberId, roomId);
                        }
                    } else {
                        console.log(`[Process ${process.pid}] User ${userId} expired, but handled by another process.`);
                    }
                }
            }
        });
    }

    chatDisconnect = async (memberId, roomId) => {
        await this.redisClient.deleteKeys(memberId, roomId, "chat");

        console.log("user_left", { memberId, roomId });

        this.chatIo.to(roomId).emit("user left", { memberId });
    }

    readyRoomDisconnect = async (memberId, roomId) => {
        await this.redisClient.deleteKeys(memberId, roomId, "readyRoom");
        this.readyRoomIo.to(roomId).emit("user left", { memberId });
    }

    gameDisconnect = async (memberId, roomId) => {
        await this.redisClient.deleteKeys(memberId, roomId, "game");

        // 게임에서 쓰는 redis cache들 삭제 (내 위치 정보 삭제, 경계 벗어남 패널티 횟수 관리 정보 삭제)
        await this.redisClient.deleteGameCaches(memberId, roomId);

        this.gameIo.to(roomId).emit("user left", { memberId });
    }

}