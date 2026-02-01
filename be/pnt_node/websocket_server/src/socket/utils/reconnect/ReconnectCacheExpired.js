import redisDB from "../../../global/db/redis/RedisDB.js";
import { ChatRoomService } from "../../chats/application/ChatRoomService.js";
import { RedisClient } from "../client/RedisClient.js";
import { GameController } from "../../games/controller/GameController.js";
import axios from "axios";


export class WebSocketReconnect {
    constructor(chatIo, roomIo, gameIo) {
        this.pubClient = redisDB.getPubClient();
        this.subClient = redisDB.getSubClient();
        this.chatIo = chatIo;
        this.roomIo = roomIo;
        this.gameIo = gameIo;
        this.chatRoomService = new ChatRoomService();
        this.redisClient = new RedisClient();
        this.gameController = new GameController();
    }

    listen = async () => {

        await this.subClient.subscribe("__keyevent@0__:expired");

        this.subClient.on("message", async (channel, message) => {
            if (channel === "__keyevent@0__:expired") {
                const key = message;

                // Key format: websocket:reconnect:timer:<namespace>:<roomId>:<memberId>
                if (key.startsWith(this.redisClient.RECONNECT_PREFIX)) {
                    const parts = key.split(":");
                    const namespace = parts[3];
                    const roomId = parts[4];
                    const memberId = parts[5];

                    // [중복 방지 핵심] 
                    const isMine = await this.redisClient.setReconnectLock(memberId);

                    if (isMine) {
                        console.log(`[Process ${process.pid}] Winner! Handling disconnect for ${memberId}`);

                        if (namespace === "chat") {
                            this.chatDisconnect(memberId, roomId);
                        } else if (namespace === "room") {
                            this.roomDisconnect(memberId, roomId);
                        } else if (namespace === "game") {
                            this.gameDisconnect(memberId, roomId);
                        }
                    } else {
                        // console.log(`[Process ${process.pid}] User ${memberId} expired, but handled by another process.`);
                    }
                }
            }
        });
    }

    chatDisconnect = async (memberId, roomId) => {
        try {
            await this.redisClient.deleteKeys("chat", roomId, memberId);

            console.log("user_left", { memberId, roomId });

            this.chatIo.to(roomId).emit("user left", { memberId });
        } catch (error) {
            console.error("Error in WebSocketReconnect (chat disconnect):", error.message);
        }
    }

    roomDisconnect = async (memberId, roomId) => {
        try {
            const accessToken = await this.redisClient.getAccessToken(memberId);

            await this.redisClient.deleteKeys("room", roomId, memberId);

            const response = await axios.delete(`${process.env.SPRING_BOOT_URL}/rooms/${roomId}/members/me`, {
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${accessToken}`
                }
            });

            await this.redisClient.deleteAccessToken(memberId);

            console.log("user_left", { memberId, roomId });
            this.roomIo.to(roomId).emit("get user left", { roomId: roomId, memberId: memberId });
        } catch (error) {
            console.error("Error in WebSocketReconnect (room disconnect):", error.response?.data || error.message);
        }
    }

    gameDisconnect = async (memberId, roomId) => {
        // 게임 접속 정보 업데이트
        try {
            await this.gameController.gameMemberService.updateInGameConnected(roomId, memberId, false);
            const isGameEnd = await this.gameController.gameService.checkGameHaveToFinish(roomId);

            if (isGameEnd) {
                await this.gameController.gameEnd(this.gameIo, this.redisClient, roomId, isGameEnd);
            }

            console.log("user_left", { memberId, roomId });
            this.gameIo.to(roomId).emit("get user left", { roomId: roomId, memberId: memberId });
        } catch (error) {
            console.error("Error in WebSocketReconnect (game disconnect):", error.message);
        }
    }

}