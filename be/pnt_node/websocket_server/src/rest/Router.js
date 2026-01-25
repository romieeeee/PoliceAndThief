import { Router } from "express";
import { TestController } from "./test/TestController.js";

const router = Router();

export default router;

const testController = new TestController();

router.use("/token", testController.getRouter());