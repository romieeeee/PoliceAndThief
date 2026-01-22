import mq from "../../../global/mq/MessagingQueue.js";
import { MQConfig } from "../../../global/mq/MQConfig.js";
import { ChatPushService } from "../application/ChatPushService.js";
import { sendChatPush } from "../../../global/fcm.js";

class ChatPushMq {
    channel;
    chatPushService;

    create = async () => {
        this.channel = await mq.createChannel(MQConfig.MQ_ALARM);
        this.chatPushService = new ChatPushService();
        return this;
    }       

    consume = async () => {
        this.channel.consume(MQConfig.MQ_ALARM, async (msg) => {
            const data = JSON.parse(msg.content.toString());

            // const memberIds = await this.chatPushService.getNotConnectedInRoom(data.chatRoomId);

            // const tokens = await this.chatPushService.getUserTokens(memberIds);

            // await sendChatPush(tokens, data);
            console.log("data consumed", data);
        });
    }
}

const chatPushMq = new ChatPushMq();

export default chatPushMq;