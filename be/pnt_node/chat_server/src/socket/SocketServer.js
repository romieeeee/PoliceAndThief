import { Server } from "socket.io";
import { Redis } from "ioredis";
import { createAdapter } from "@socket.io/redis-adapter";
import { SocketController } from "./chats/controller/ChatController.js";

export const socketServer = async (httpServer) => {

    const pubClient = new Redis({
        host: "localhost",
        port: 6379
    })

    const subClient = pubClient.duplicate();

    const io = new Server(httpServer, {
        adapter: createAdapter(pubClient, subClient),
    });


    io.on("connection", (socket) => {
        console.log("websocket is connected!");

        const socketController = new SocketController(io, socket);

        // 채팅방 관련 이벤트
        socket.on("post join room", socketController.joinRoom);
        socket.on("post message", socketController.sendMessage);
        socket.on("get prev chat", () => {});


        socket.on("disconnect", () => {
            console.log("disconnected");
        });
    });
} 