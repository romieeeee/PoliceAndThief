import mongoose from "mongoose";

const DB_URL = "mongodb://localhost:27017";

class MongoDB {
    async create() {
        mongoose.set('debug', true);

        await mongoose.connect(DB_URL, {
            dbName: "pnt"
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