import { Server } from "socket.io";
import { Redis } from "ioredis";

export const socketServer = async (httpServer) => {

    const io = new Server(httpServer);

} 