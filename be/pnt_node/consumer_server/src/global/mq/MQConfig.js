import dotenv from "dotenv";

dotenv.config();

export const MQConfig = {
    URL: process.env.MQ_URL,
    MQ_IMAGE: process.env.MQ_IMAGE,
    MQ_ALARM: process.env.MQ_ALARM,
    MQ_NEWS: process.env.MQ_NEWS,
    EXCHANGE_NAME: process.env.EXCHANGE_NAME
};