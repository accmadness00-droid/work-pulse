import cv2
import numpy as np
from insightface.app import FaceAnalysis

class FaceEngine:
    def __init__(self, model_name: str = "buffalo_l", ctx_id: int = -1):
        self.model_name = model_name
        self.app = FaceAnalysis(name=model_name)
        # ctx_id=-1 CPU, ctx_id=0 GPU
        self.app.prepare(ctx_id=ctx_id)

    def extract_embedding(self, image_bytes: bytes):
        np_arr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
        if img is None:
            return {
                "success": False,
                "error": "INVALID_IMAGE",
                "faceDetected": False,
            }

        faces = self.app.get(img)
        if len(faces) == 0:
            return {
                "success": False,
                "error": "FACE_NOT_DETECTED",
                "faceDetected": False,
            }
        if len(faces) > 1:
            return {
                "success": False,
                "error": "MULTIPLE_FACES_DETECTED",
                "faceDetected": True,
            }

        emb = faces[0].embedding.astype(np.float32)
        norm = np.linalg.norm(emb)
        if norm == 0:
            return {
                "success": False,
                "error": "EMPTY_EMBEDDING",
                "faceDetected": True,
            }
        emb = emb / norm
        return {
            "success": True,
            "modelName": self.model_name,
            "embedding": emb.tolist(),
            "faceDetected": True,
        }
