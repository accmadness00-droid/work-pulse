from pydantic import BaseModel
from typing import List, Optional

class FaceEmbeddingResponse(BaseModel):
    success: bool
    modelName: Optional[str] = None
    embedding: Optional[List[float]] = None
    faceDetected: bool = False
    error: Optional[str] = None
