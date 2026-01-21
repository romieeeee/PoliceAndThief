import connectMongo from "./mongo/MongoDB.js";
import sequelizeDB from "./sequelize/SequelizeDB.js";

export const mongoDB = async () => {
    await connectMongo();
}

export const sequelize = sequelizeDB;
