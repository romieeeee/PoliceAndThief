import { Server } from "socket.io";
import { Redis } from "ioredis";
import { createAdapter } from "@socket.io/redis-adapter";
import chatSocketServer from "./chats/ChatSocketServer.js";

export const socketServer = async (httpServer) => {

    const pubClient = new Redis({
        host: "localhost",
        port: 6379
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