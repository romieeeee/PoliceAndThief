import dotenv from 'dotenv';
import Redis from 'ioredis';
import { GpsController } from './controller/GpsController.js';

dotenv.config();

const STREAM_NAME = `post-gps-stream`;
const GROUP_NAME = 'gps-consumer-group';
const CONSUMER_NAME = `consumer-${process.env.HOSTNAME || 'instance'}-${Math.random().toString(36).substr(2, 5)}`;

const redis = new Redis({
  host: process.env.REDIS_HOST,
  port: process.env.REDIS_PORT
});

const gpsController = new GpsController();

// Create Consumer Group (ignore error if exists)
try {
  await redis.xgroup('CREATE', STREAM_NAME, GROUP_NAME, '$', 'MKSTREAM');
  console.log(`Created consumer group ${GROUP_NAME}`);
} catch (e) {
  if (!e.message.includes('BUSYGROUP')) {
    console.error('Error creating consumer group:', e);
  }
}

const processMessage = async (message) => {
  // message format from ioredis xreadgroup: [streamName, [[id, [field, value, field, value...]]]]
  // But here we will process individual message entries passed to this function
  const id = message[0];
  const fields = message[1];

  // Parse fields array into object: ['data', '{"gameId":1...}'] -> { data: '...' }
  const payloadStr = fields[1];
  if (!payloadStr) return;

  try {
    const payload = JSON.parse(payloadStr);
    await gpsController.postGps(payload);
  } catch (e) {
    console.error('Error processing message:', e);
  }

  // ACK
  await redis.xack(STREAM_NAME, GROUP_NAME, id);
};

const loop = async () => {
  while (true) {
    try {
      // Block for 5 seconds waiting for new messages
      const response = await redis.xreadgroup(
        'GROUP', GROUP_NAME, CONSUMER_NAME,
        'BLOCK', 5000,
        'COUNT', 10,
        'STREAMS', STREAM_NAME, '>'
      );

      if (response) {
        // response is array of streams: [[streamName, [messages]]]
        const streamName = response[0][0];
        const messages = response[0][1];

        // Process concurrently
        await Promise.all(messages.map(processMessage));
      }
    } catch (error) {
      console.error('Consumer Loop Error:', error);
      await new Promise(resolve => setTimeout(resolve, 1000)); // Backoff
    }
  }
};

console.log(`Starting GPS Consumer ${CONSUMER_NAME} for group ${GROUP_NAME}`);
loop();