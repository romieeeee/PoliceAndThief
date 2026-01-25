import { Server } from "socket.io";
import { createAdapter } from "@socket.io/redis-adapter";
import chatSocketServer from "./chats/ChatSocketServer.js";
import redisDB from "../global/db/redis/RedisDB.js";
import { WebSocketReconnect } from "./utils/RedisExpiredEvent.js";
export const socketServer = async (httpServer) => {

    const pubClient = redisDB.getPubClient();
    const subClient = redisDB.getSubClient();

    const io = new Server(httpServer, {
        adapter: createAdapter(pubClient, subClient),
        cors: {
            origin: "*"
        }
    });

    const chatIo = io.of("/chat");
    const readyRoomIo = io.of("/readyRoom");
    const gameIo = io.of("/game");

    const webSocketReconnect = new WebSocketReconnect(chatIo, readyRoomIo, gameIo);
    await webSocketReconnect.listen();

    chatSocketServer(chatIo, pubClient);
} 