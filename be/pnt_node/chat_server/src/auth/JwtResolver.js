import jwt from "jsonwebtoken";

const secretKeyString = "123412341234123424ergfgdfgdsfgsdfg24ejgkskjvhlh233rdghjkasdhxzclkbhqop4eyt24twonlrkg48ytiodhljkashdfh";
const secretKey = Buffer.from(secretKeyString, 'base64');
const options = { algorithms: ['HS512'] };

export const resolveInSocket = (socket, next) => {
    const token = socket.handshake.auth.token;
    console.log(token);

    try {
        const data = jwt.verify(token, secretKey, options);

        console.log("data", data);

        socket.data.memberId = data.sub;

        next();
    } catch (err) {
        // 1. 유효기간 만료
        if (err.name === 'TokenExpiredError') {
            next(new Error('TokenExpiredError'));
        }
        
        // 2. 토큰 데이터나 서명이 잘못됨 (변조, 비밀키 불일치, 형식 오류 등)
        if (err.name === 'JsonWebTokenError') {
            next(new Error('JsonWebTokenError'));
        }

        // 3. 기타 오류를 서버 내부 오류로 구분
        console.log("socket auth err", err);
        next(new Error('InternalServerError'));
    }
};

export const resolveInRest = (token) => {

}