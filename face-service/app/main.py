import os
from fastapi import FastAPI, UploadFile, File
from app.face_engine import FaceEngine
from app.schemas import FaceEmbeddingResponse

MODEL_NAME = os.getenv("FACE_MODEL", "buffalo_l")
CTX_ID = int(os.getenv("FACE_CTX_ID", "-1"))

app = FastAPI(title="WorkPulse Face Service", version="1.0.0")
face_engine = FaceEngine(model_name=MODEL_NAME, ctx_id=CTX_ID)

@app.get("/api/v1/health")
def health():
    return {"status": "UP", "service": "workpulse-face-service", "modelName": MODEL_NAME}

@app.post("/api/v1/face/embedding", response_model=FaceEmbeddingResponse)
async def embedding(file: UploadFile = File(...)):
    image_bytes = await file.read()
    result = face_engine.extract_embedding(image_bytes)
    return result
