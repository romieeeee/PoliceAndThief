import { GameMemberService } from "../../games/application/GameMemberService.js";
import { GameService } from "../../games/application/GameService.js";
import { GameSettingService } from "../../games/application/GameSettingService.js";
import { RedisClient } from "../../utils/client/RedisClient.js";
import axios from "axios";
import { sendError } from "../../../global/util/SocketError.js";
import { JwtResolver, resolveInSocket, resolveInController } from "../../../global/auth/JwtResolver.js";

export class RoomController {
    constructor(io, socket) {
        this.io = io;
        this.socket = socket;
        this.redisClient = new RedisClient();
        this.gameSettingService = new GameSettingService();
        this.gameMemberService = new GameMemberService();
        this.gameService = new GameService();
    }

    /*
    * 게임 로비 입장 => db 입장 처리는 spring 에서 처리
    * 
    * 내려줄 정보 없음.
    * 
    */

    isActiveRoom = async (roomId) => {
        const room = await this.gameService.getGameById(roomId);

        if (room.status !== "WAITING") {
            this.makeError("NotFoundException", "방을 찾을 수 없습니다.", 404);
        }
        return true;
    }

    joinRoom = async (data) => {
        const { roomId } = data;

        this.socket.data.roomId = roomId;

        this.socket.join(roomId);

        this.io.to(roomId).emit("get join room", { roomId });
    }

    updateRoomInfo = async (data) => {
        const { roomId } = data;

        const accessToken = await this.redisClient.getAccessToken(this.socket.data.memberId);

        try {
            const response = await axios.patch(`${process.env.SPRING_BOOT_URL}/rooms/${roomId}/settings`, data, {
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${accessToken}`
                }
            });
            this.io.to(roomId).emit("get update room info", response.data);
        } catch (error) {
            if (error.response) {
                this.io.to(roomId).emit("get update room info", error.response.data);
            } else {
                sendError(this.socket, error, "RoomError");
            }
        }
    }

    updateReady = async (data) => {
        const { roomId } = data;

        const accessToken = await this.redisClient.getAccessToken(this.socket.data.memberId);

        try {
            const response = await axios.patch(`${process.env.SPRING_BOOT_URL}/rooms/${roomId}/ready`, data, {
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${accessToken}`
                }
            });
            console.log("updateReady", response.data);
            this.io.to(roomId).emit("get update ready", response.data);
        } catch (error) {
            console.log("updateReady error", error);
            if (error.response) {
                this.io.to(roomId).emit("get update ready", error.response.data);
            } else {
                sendError(this.socket, error, "RoomError");
            }
        }
    }

    updatePreferPosition = async (data) => {
        const roomId = this.socket.data.roomId;

        const accessToken = await this.redisClient.getAccessToken(this.socket.data.memberId);

        try {
            const response = await axios.post(`${process.env.SPRING_BOOT_URL}/rooms/${roomId}/position`, data, {
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${accessToken}`
                }
            });

            this.io.to(roomId).emit("get update prefer position", response.data);
        } catch (error) {
            if (error.response) {
                this.io.to(roomId).emit("get update prefer position", error.response.data);
            } else {
                sendError(this.socket, error, "RoomError");
            }
        }
    }

    nowReadyInfo = async (data) => {
        const roomId = this.socket.data.roomId;

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
        const roomId = this.socket.data.roomId;

        const room = await this.gameService.getGameById(roomId);
        const roomSetting = await this.gameSettingService.findGameSetting(roomId);
        const members = await this.gameMemberService.findMembersWithProfileByGameId(roomId);

        this.io.to(roomId).emit("get now room info", { room, roomSetting, members });
    }

    memberKick = async (data) => {
        const roomId = this.socket.data.roomId;

        const accessToken = await this.redisClient.getAccessToken(this.socket.data.memberId);

        try {
            const response = await axios.post(`${process.env.SPRING_BOOT_URL}/rooms/${roomId}/members/kick`, data, {
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${accessToken}`
                }
            });
            this.io.to(roomId).emit("get member kick", response.data);
        } catch (error) {
            if (error.response) {
                this.io.to(roomId).emit("get member kick", error.response.data);
            } else {
                sendError(this.socket, error, "RoomError");
            }
        }
    }

    postUpdateAccessToken = async (data) => {
        try {
            const accessToken = data.accessToken;

            resolveInController(accessToken);

            this.redisClient.setAccessToken(this.socket.data.memberId, accessToken);
            this.socket.data.accessToken = accessToken;

            console.log("update access token", this.socket.data.accessToken);

            this.socket.emit("get update access token", data.accessToken);
        } catch (error) {
            sendError(this.socket, error, "RoomError");
        }
    }

    updateRoomMap = async (data) => {
        const roomId = this.socket.data.roomId;

        const accessToken = await this.redisClient.getAccessToken(this.socket.data.memberId);

        try {
            const response = await axios.post(`${process.env.SPRING_BOOT_URL}/rooms/${roomId}/map`, data, {
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${accessToken}`
                }
            });
            this.io.to(roomId).emit("get update room map", response.data);
        } catch (error) {
            if (error.response) {
                this.io.to(roomId).emit("get update room map", error.response.data);
            } else {
                sendError(this.socket, error, "RoomError");
            }
        }
    }

    disconnect = async (data) => {
        const roomId = this.socket.data.roomId;

        this.socket.data.isIntentionalExit = true; // 사용자의 요청에 의해서 소켓이 종료되었는지 판별하기 위한 변수

        this.io.to(roomId).emit("get disconnect", { "message": "사용자가 방을 나갔습니다." });
        this.io.to(roomId).emit("get user left", { roomId: roomId, memberId: this.socket.data.memberId });
    }

}