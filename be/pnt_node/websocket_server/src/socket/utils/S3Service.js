import { S3Client, GetObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import dotenv from "dotenv";

dotenv.config();

const region = process.env.AWS_REGION;
const accessKeyId = process.env.AWS_ACCESS_KEY;
const secretAccessKey = process.env.AWS_SECRET_KEY;

if (!region || !accessKeyId || !secretAccessKey) {
    console.error("⚠️ AWS Configuration missing!");
    console.error("AWS_REGION:", region);
    console.error("AWS_ACCESS_KEY_ID exists:", !!accessKeyId);
    console.error("AWS_SECRET_ACCESS_KEY exists:", !!secretAccessKey);
}

const clientConfig = {
    region: region,
};

if (accessKeyId && secretAccessKey) {
    clientConfig.credentials = {
        accessKeyId: accessKeyId,
        secretAccessKey: secretAccessKey,
    };
}

const s3Client = new S3Client(clientConfig);

export const getPresignedUrl = async (key) => {
    if (!key) return null;

    // 이미 http로 시작하는 URL이면 그대로 반환 (혹시 모를 예외 처리)
    if (key.startsWith("http")) return key;

    try {
        const command = new GetObjectCommand({
            Bucket: process.env.AWS_BUCKET_NAME,
            Key: key,
        });

        // 1시간(3600초) 유효한 URL 생성
        const url = await getSignedUrl(s3Client, command, { expiresIn: 3600 });
        return url;
    } catch (error) {
        console.error("Presigned URL 생성 실패:", error);
        return key;
    }
};
