import { Server } from "socket.io";
import { Redis } from "ioredis";
import { createAdapter } from "@socket.io/redis-adapter";
import chatSocketServer from "./chats/ChatSocketServer.js";
import dotenv from "dotenv";

dotenv.config();

export const socketServer = async (httpServer) => {

    const pubClient = new Redis({
        host: process.env.REDIS_HOST,
        port: process.env.REDIS_PORT
    })

    const subClient = pubClient.duplicate();

    const io = new Server(httpServer, {
        adapter: createAdapter(pubClient, subClient),
        cors: {
            origin: "*"
        }
    });


    chatSocketServer(io.of("/chat"));
} 