import { createServer } from "http";
import express from "express";
import cors from "cors";
import { socketServer } from "./socket/SocketServer.js";
import router from "./rest/Router.js";
import { database } from "./global/db/database.js";
import mq from "./global/mq/MessagingQueue.js";
import swaggerUi from "swagger-ui-express";
import specs from "./global/swagger/swagger.js";

const port = 8090;

(async () => {
    const app = express();
    const server = createServer(app);

    app.use(cors());
    app.use(express.json());
    app.use("/api-docs", swaggerUi.serve, swaggerUi.setup(specs));

    await mq.create();
    await database();
    await socketServer(server);

    app.use("/", router);

    app.get("/", (req, res) => {
        console.log("request in root");
        res.status(200).json({ "message": "ok" });
    });

    server.listen(port, () => {
        console.log("Listen in port:", port);
    });
})();