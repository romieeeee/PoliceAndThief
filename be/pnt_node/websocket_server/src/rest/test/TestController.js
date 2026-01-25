import { Router } from "express";
import jwt from "jsonwebtoken";

const secretKeyString = "123412341234123424ergfgdfgdsfgsdfg24ejgkskjvhlh233rdghjkasdhxzclkbhqop4eyt24twonlrkg48ytiodhljkashdfh";

const secretKey = Buffer.from(secretKeyString, 'base64');

export class TestController {
    constructor() {
        this.router = new Router();
        this.init();
    } 

    init = () => {
        this.router.post("/verify", this.testApi);
    }

    getRouter = () => {
        return this.router;
    }

    testApi = (req, res) => {
        const payload = req.body;

        const { accessToken } = payload;

        console.log(accessToken);

        try {
            const data = jwt.verify(accessToken, secretKey, {
                algorithms: ['HS512']
            });

            res.status(200).json(data);
        } catch (err) {
            console.log(err);
            res.status(400).json({message : err.message});
        }
    }
}