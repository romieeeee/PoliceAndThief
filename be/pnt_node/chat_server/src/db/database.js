import connectMongo from "./mongo/MongoDB.js";

export default async () => {
    await connectMongo();
}
