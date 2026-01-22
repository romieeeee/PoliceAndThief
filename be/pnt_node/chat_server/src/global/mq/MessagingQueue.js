import amqplib from "amqplib";
import dotenv from "dotenv";
import { MQConfig } from "./MQConfig.js";

dotenv.config();

const MQ_URL = process.env.MQ_URL;

class MessagingQueue {
    connection = null;
    channel = null;
    queue = [MQConfig.MQ_ALARM, MQConfig.MQ_IMAGE, MQConfig.MQ_NEWS];
    exchangeName = MQConfig.EXCHANGE_NAME;

    create = async () => {
        this.connection = await amqplib.connect(MQ_URL);
        this.channel = await this.connection.createChannel();
        await this.createQueue();
        console.log("MQ is connected!");
        return this;
    }

    createQueue = async () => {
        await this.channel.assertExchange(this.exchangeName, "direct", { durable: true });

        for (let i = 0; i < this.queue.length; i++) {
            await this.channel.assertQueue(this.queue[i], { durable: true });
            await this.channel.bindQueue(this.queue[i], this.exchangeName, this.queue[i]);
        }
    }

    sendMessage = (message, queueKey) => {
        this.channel.publish(this.exchangeName, queueKey, Buffer.from(JSON.stringify(message)), (error) => {
            if (error) {
                console.error("Failed to send message:", error);
            }
        });
    }
}

const mq = new MessagingQueue().create();

export default mq;