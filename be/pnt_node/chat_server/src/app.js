import { createServer } from "http";
import express from "express";
import cors from "cors";
import { socketServer } from "./socket/SocketServer.js";

const port = 8090;

(async () => {
    const app = express();
    const server = createServer(app);

    app.use(cors());
    app.use(express.json());

    await socketServer(server);

    app.get("/", (req, res) => {
        console.log("request in root");
        res.status(200).json({ "message": "ok" });
    })


    server.listen(port, () => {
        console.log("Listen in port:", port);
    });
})();
