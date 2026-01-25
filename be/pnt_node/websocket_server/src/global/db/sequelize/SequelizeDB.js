import { Sequelize } from "sequelize";
import Member from "./entity/Member.js";
import ChatRoom from "./entity/ChatRoom.js";
import MemberChatRoom from "./entity/MemberChatRoom.js";
import dotenv from "dotenv";

dotenv.config();

class SequelizeDB {
    create = async () => {
        const sequelize = new Sequelize(process.env.POSTGRES_DB_NAME, process.env.POSTGRES_USER, process.env.POSTGRES_PASSWORD, {
            host: process.env.POSTGRES_HOST,
            dialect: "postgres",
            port: process.env.POSTGRES_PORT,
        });
        Member.initiate(sequelize);
        ChatRoom.initiate(sequelize);
        MemberChatRoom.initiate(sequelize);

        Member.associate(sequelize.models);
        ChatRoom.associate(sequelize.models);
        MemberChatRoom.associate(sequelize.models);

        await this.sync(sequelize);

        return sequelize;
    }

    sync = async (sequelize) => {
        await sequelize.sync({ force: false }).then(() => {
            console.log("Database synchronized successfully.");
        }).catch((error) => {
            console.error("Failed to synchronize database:", error);
        });
    }
}

const db = new SequelizeDB();

export default db;
