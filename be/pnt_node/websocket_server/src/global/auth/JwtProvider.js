import jwt from 'jsonwebtoken';
import dotenv from 'dotenv';

dotenv.config();

const secretKeyString = process.env.JWT_SECRET;
const secretKey = Buffer.from(secretKeyString, 'base64');

export const generateToken = (gameId, time) => {
    const payload = {
        gameId: gameId,
    };

    const options = {
        algorithm: 'HS512',
        expiresIn: `${time + 5}m`,
        issuer: 'pnt-websocket'
    };

    try {
        return jwt.sign(payload, secretKey, options);
    } catch (error) {
        console.error("토큰 생성 실패:", error);
        throw new Error("Token generation failed");
    }
};