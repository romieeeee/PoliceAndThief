import { Router } from "express";
import { MissionController } from "./mission/controller/MissionController.js";

const router = Router();

export default router;

const missionController = new MissionController();

router.use("/mission", missionController.getRouter());
