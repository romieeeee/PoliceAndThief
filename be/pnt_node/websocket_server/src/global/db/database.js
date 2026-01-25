import connectMongo from "./mongo/MongoDB.js";
import sequelizeDB from "./sequelize/SequelizeDB.js";

export const database = async () => {
    await connectMongo();
    await sequelizeDB.create();
}

export const sequelize = sequelizeDB;