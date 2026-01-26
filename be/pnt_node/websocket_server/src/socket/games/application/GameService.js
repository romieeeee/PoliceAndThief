import Game from "../../../global/db/sequelize/entity/Game"

export class GameService {

    findGame = async (gameId) => {
        const res = await Game.findOne({
            where: {
                id: gameId,
                isDeleted: false,
                isFinished: false
            }
        });

        if (!res) {
            this.makeError("NotFoundException", "게임을 찾을 수 없습니다.", 404);
        }
        return res;
    }

    makeError = (message, text, code) => {
        const error = new Error(message);
        error.code = code;
        error.text = text;
        throw error;
    }
}