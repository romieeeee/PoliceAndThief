import redisDB from "../../global/db/redis/RedisDB.js";
import expiredChannel from "./channels/ExpiredChannel.js";

import { ChatRoomService } from "../chats/application/ChatRoomService.js";
import { WebSocketReconnect } from "./reconnect/ReconnectCacheExpired.js";
import { GameService } from "../games/application/GameService.js";
import { GameMemberPosition } from "../../global/db/sequelize/status/GameMemberPosition.js"; 
import logger from "../../global/config/logger.js";
import { GameController } from "../games/controller/GameController.js";
import { RedisClient } from "./client/RedisClient.js";

export class RedisEvent {
    constructor(chatIo, readyRoomIo, gameIo) {
        this.pubClient = redisDB.getPubClient();
        this.subClient = redisDB.getSubClient();
        this.chatIo = chatIo;
        this.readyRoomIo = readyRoomIo;
        this.gameIo = gameIo;
        this.chatRoomService = new ChatRoomService();

        this.webSocketReconnect = new WebSocketReconnect(chatIo, readyRoomIo, gameIo);
        this.gameService = new GameService();
        this.gameController = new GameController();
        this.redisClient = new RedisClient();
    }

    listen = async () => {


        // 2. 만료 이벤트 설정
        await this.pubClient.config("SET", "notify-keyspace-events", "Ex");
        await this.webSocketReconnect.listen();

        // 3. 만료 이벤트 리스너
        // __keyevent@0__:expired 채널을 구독 (0번 DB 기준)
        await this.subClient.subscribe("__keyevent@0__:expired", "game:event:trigger");

        this.subClient.on("message", async (channel, message) => {
            if (channel === "__keyevent@0__:expired") {
                expiredChannel(message, this.pubClient, this.chatIo, this.readyRoomIo, this.gameIo);
            } else if (channel === "game:event:trigger") {
                // message: JSON string { type: 'ARREST_CHECK', gameId, memberId }
                try {
                    const payload = JSON.parse(message);
                    logger.info(`[RedisEvent] game:event:trigger: ${payload}`);
                    if (payload.type === 'ARREST_CHECK') {
                        const { gameId } = payload;
                        /* Logic to check game end */
                        const isGameEnd = await this.gameService.checkGameHaveToFinish(gameId);

                        if (isGameEnd) {
                            // Game End Logic
                            logger.info(`[RedisEvent] game:end: ${gameId}`);
                            this.gameController.gameEnd(this.gameIo, this.redisClient, gameId, GameMemberPosition.POLICE);
                        }
                    }
                } catch (e) {
                    logger.error("RedisEvent processing error", e);
                }
            }
        });
    }
}