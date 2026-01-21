import { Sequelize } from "sequelize";
import Member from "./entity/member.js";
import ChatRoom from "./entity/ChatRoom.js";
import MemberChatRoom from "./entity/MemberChatRoom.js";

class SequelizeDB {
    create = async () => {
        const sequelize = new Sequelize("pnt", "root", "1234", {
            host: "localhost",
            dialect: "postgres",
            port: 5432,
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

const db = new SequelizeDB().create();

export default db;
