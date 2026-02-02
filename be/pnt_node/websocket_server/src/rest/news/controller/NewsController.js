import { Router } from "express";

import { Emitter } from "@socket.io/redis-emitter";
import { RedisClient } from "../../../socket/utils/client/RedisClient";

const GAME_NAMESPACE = "/game";

export class NewController {
    constructor() {
        this.redisClient = new RedisClient();
        this.router = Router();
        this.emitter = new Emitter(this.redisClient.pubClient);
        this.init();
    }

    init = () => {
        this.router.post("/complete", this.newsComplete);
    }

    getRouter = () => {
        return this.router;
    }

    newsComplete = async (req, res) => {
        try {
            const payload = req.body;
            const { gameId, newsId, success } = payload;

            if (success) {
                this.emitter.of(GAME_NAMESPACE).to(gameId).emit("get news", { gameId, newsId });
                await this.redisClient.setNews(gameId, newsId);
            }

            res.status(200).json({ message: "News sent successfully" });
        } catch (error) {
            console.error("sendNews error", error);
            res.status(error.code || 500).json({ message: error.message });
        }
    }
}   