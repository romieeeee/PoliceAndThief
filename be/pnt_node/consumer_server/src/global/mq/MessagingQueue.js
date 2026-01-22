import amqp from "amqplib";
import { MQConfig } from "./MQConfig.js";

class MessagingQueue {
    connection = null;

    create = async () => {
        this.connection = await amqp.connect(MQConfig.URL);
        this.connectionEvent();
        return this.connection;
    }

    createChannel = async (key) => {
        return await this.connection.createChannel(key);
    }

    connectionEvent = () => {
        this.connection.on("close", () => {
            console.log("Connection closed");
        });

        this.connection.on("error", (error) => {
            console.log("Connection error", error);
        });
    }
}

const mq = new MessagingQueue();

export default mq;