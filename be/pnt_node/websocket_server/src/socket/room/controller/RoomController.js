import { GameMemberService } from "../../games/application/GameMemberService.js";
import { GameService } from "../../games/application/GameService.js";
import { GameSettingService } from "../../games/application/GameSettingService.js";
import { RedisClient } from "../../utils/client/RedisClient.js";
import axios from "axios";
import { sendError } from "../../../global/util/SocketError.js";

export class RoomController {
    constructor(io, socket) {
        this.io = io;
        this.socket = socket;
        this.redisClient = new RedisClient();
        this.gameSettingService = new GameSettingService();
        this.gameMemberService = new GameMemberService();
    }

    /*
    * 게임 로비 입장 => db 입장 처리는 spring 에서 처리
    * 
    * 내려줄 정보 없음.
    * 
    */
    joinRoom = async (data) => {
        const { roomId } = data;

        this.socket.data.roomId = roomId;

        this.socket.join(roomId);
    }

    /**
     *   "path": { "id": 1 },
     *   "playerCount": 1,
     *   "timeLimit": 1,
     *   "policeCount": 0,
     *   "thiefCount": 0,
     *   "prison": {
     *      "lat": 0,
     *      "lng": 0
     *   },
     *   "polygon": [
     *      {
     *          "lat": 0,
     *          "lng": 0
     *      },
     *      {
     *          "lat": 0,
     *          "lng": 0
     *      },
     *      {
     *          "lat": 0,
     *          "lng": 0
     *      },
     *      {
     *          "lat": 0,
     *          "lng": 0
     *      }
     *   ]
     */
    updateRoomInfo = async (data) => {
        const { roomId } = data;

        const response = await axios.patch(`${process.env.SPRING_API_URL}/spring/rooms/${roomId}/settings`, data, {
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${this.socket.data.accessToken}`
            }
        });

        this.io.to(roomId).emit("get update room info", response.data);
    }

    updateReady = async (data) => {
        const { roomId } = data;

        const response = await axios.patch(`${process.env.SPRING_API_URL}/spring/rooms/${roomId}/ready`, data, {
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${this.socket.data.accessToken}`
            }
        });

        this.io.to(roomId).emit("get update ready", response.data);
    }

    updatePreferPosition = async (data) => {
        const { roomId } = data;

        const response = await axios.patch(`${process.env.SPRING_API_URL}/spring/rooms/${roomId}/position`, data, {
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${this.socket.data.accessToken}`
            }
        });

        this.io.to(roomId).emit("get update prefer position", response.data);
    }

    nowReadyInfo = async (data) => {
        const { roomId } = data;

        const gameMembers = await this.gameMemberService.getGameMembers(parseInt(roomId));
        const readyInfo = gameMembers.map((gameMember) => {
            return {
                memberId: gameMember.memberId,
                isReady: gameMember.isReady,
                preferPosition: gameMember.preferPosition
            }
        })

        this.io.to(roomId).emit("get now ready info", readyInfo);
    }

    nowRoomInfo = async (data) => {
        const { roomId } = data;

        const room = await this.gameService.getRoomById(roomId);
        const roomSetting = await this.gameSettingService.getGameSettingByRoomId(roomId);
        const members = await this.gameMemberService.findMembersWithProfileByGameId(roomId);

        this.io.to(roomId).emit("get now room info", { room, roomSetting, members });
    }

    memberKick = async (data) => {
        const { roomId } = data;

        const response = await axios.post(`${process.env.SPRING_API_URL}/spring/rooms/${roomId}/members/kick`, data, {
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${this.socket.data.accessToken}`
            }
        });

        this.io.to(roomId).emit("get member kick", response.data);
    }

    disconnect = async (data) => {
        const { roomId } = data;

        const response = await axios.post(`${process.env.SPRING_API_URL}/spring/rooms/${roomId}/disconnect`, data, {
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${this.socket.data.accessToken}`
            }
        });

        this.socket.data.isIntentionalExit = true; // 사용자의 요청에 의해서 소켓이 종료되었는지 판별하기 위한 변수

        this.io.to(roomId).emit("get disconnect", response.data);
    }

}