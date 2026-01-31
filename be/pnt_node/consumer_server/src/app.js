import worker from "./worker";
import mq from "./global/mq/MessagingQueue.js";
import { database } from "./global/db/database.js";

(async () => {
    await database();
    await mq.create();
    await worker();
    console.log("start worker");
})();