export class GameController {
    constructor(io, socket, pubClient) {
        this.io = io;
        this.socket = socket;
        this.pubClient = pubClient;
    }

    postGps = async (payload) => {
        const { chatRoomId, lat, lng, position, status } = payload;
        const userId = this.socket.member.memberId; // 미들웨어에서 가져온 ID

        const locationData = JSON.stringify({
            lat,
            lng,
            userId, // 클라이언트 편의를 위해 포함
            chatRoomId,
            position,
            status,
            timestamp: Date.now() // 중요: 갱신 시간 기록
        });

        // Hash에 저장 (이미 있으면 덮어쓰기됨 -> 자동 최신화)
        await this.pubClient.hset(`room:${chatRoomId}:locations`, userId, locationData);
    }

}