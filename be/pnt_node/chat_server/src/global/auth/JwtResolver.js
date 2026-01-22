import jwt from "jsonwebtoken";
import dotenv from "dotenv";

dotenv.config();

const secretKeyString = process.env.JWT_SECRET;
const secretKey = Buffer.from(secretKeyString, 'base64');
const options = { algorithms: ['HS512'] };

export const resolveInSocket = (socket, next) => {
    const token = socket.handshake.auth.token;
    console.log(token);

    try {
        const data = jwt.verify(token, secretKey, options);

        console.log("data", data);

        socket.data.memberId = data.memberId;

        next();
    } catch (err) {
        const data = {
            message: err.name,
        }
        // 1. 유효기간 만료
        if (err.name === 'TokenExpiredError') {
            data.code = 401;
            next(new Error(JSON.stringify(data)));
        }
        
        // 2. 토큰 데이터나 서명이 잘못됨 (변조, 비밀키 불일치, 형식 오류 등)
        if (err.name === 'JsonWebTokenError') {
            data.code = 400;
            next(new Error(JSON.stringify(data)));
        }

        // 3. 기타 오류를 서버 내부 오류로 구분
        console.log("socket auth err", err);
        data.code = 500;
        data.message = "InternalServerError";

        next(new Error(JSON.stringify(data)));
    }
};

export const resolveInRest = (token) => {

}