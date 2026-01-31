import io
import sys
from pathlib import Path

import numpy as np
import cv2
import torch
from PIL import Image
from fastapi import FastAPI, UploadFile, File, Form
from pydantic import BaseModel

# ---- yolov5 경로 연결 ----
YOLOV5_DIR = Path("yolov5").resolve()
if str(YOLOV5_DIR) not in sys.path:
    sys.path.append(str(YOLOV5_DIR))

from models.common import DetectMultiBackend
from utils.general import non_max_suppression, scale_boxes
from utils.torch_utils import select_device
from utils.augmentations import letterbox

app = FastAPI(title="YOLOv5 O/X Checker")

# ---- 모델은 서버 시작 시 1번만 로드 ----
device = select_device("")  # cuda 있으면 cuda
weights = YOLOV5_DIR / "yolov5s.pt"
model = DetectMultiBackend(str(weights), device=device)
model.eval()

names = model.names
if isinstance(names, dict):
    names = [names[i] for i in range(len(names))]

def _normalize_target(target: str) -> str:
    return target.strip().lower()

@torch.no_grad()
def check_object_in_image(
    pil_image: Image.Image,
    target_name: str,
    img_size: int = 640,
    conf_thres: float = 0.25,
    iou_thres: float = 0.45,
    max_det: int = 300,
):
    target = _normalize_target(target_name)

    im0 = cv2.cvtColor(np.array(pil_image.convert("RGB")), cv2.COLOR_RGB2BGR)

    im = letterbox(im0, new_shape=img_size, stride=model.stride, auto=True)[0]
    im = im.transpose((2, 0, 1))  # HWC -> CHW
    im = np.ascontiguousarray(im)

    im = torch.from_numpy(im).to(device)
    im = im.float() / 255.0
    if im.ndim == 3:
        im = im.unsqueeze(0)

    pred = model(im)
    pred = non_max_suppression(pred, conf_thres, iou_thres, max_det=max_det)

    if len(pred) == 0 or pred[0] is None or len(pred[0]) == 0:
        return "X", 0.0

    det = pred[0]
    det[:, :4] = scale_boxes(im.shape[2:], det[:, :4], im0.shape).round()

    best = 0.0
    for *xyxy, conf, cls in det:
        cls_name = names[int(cls)].lower()
        if cls_name == target:
            best = max(best, float(conf))

    return ("O", best) if best > 0 else ("X", 0.0)

class CheckResponse(BaseModel):
    result: str      # "O" or "X"
    similarity: float

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/check", response_model=CheckResponse)
async def check(
    image: UploadFile = File(...),
    target: str = Form(...),
    conf: float = Form(0.25),
):
    # 이미지 바이트 -> PIL
    content = await image.read()
    pil_img = Image.open(io.BytesIO(content)).convert("RGB")

    result, sim = check_object_in_image(pil_img, target_name=target, conf_thres=float(conf))
    return {"result": result, "similarity": round(sim, 6)}
