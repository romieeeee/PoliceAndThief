import mongoose from "mongoose";
import dotenv from "dotenv";

dotenv.config();

const DB_URL = process.env.MONGO_URL;

class MongoDB {
    async create() {
        mongoose.set('debug', true);

        await mongoose.connect(DB_URL, {
            dbName: process.env.MONGO_DB_NAME
        }).then(() => {
            console.log("MongoDB is connected!!");
        }).catch((err) => {
            console.error("MongoDB connection failed", err);
        });

        this.setEventListener();
    }

    setEventListener() {
        mongoose.connection.on('error', (error) => {
            console.error('몽고디비 연결 에러', error);
        });

        mongoose.connection.on('disconnected', () => {
            console.error('몽고디비 연결이 끊겼습니다. 연결을 재시도합니다.');
            this.connect(); // 재연결 시도
        });
    }
}

const db = new MongoDB();

export default () => db.create();