from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
from sqlalchemy.orm import Session
from typing import List, Optional
from datetime import datetime
import base64
from app.core.database import get_db
from app.core.redis import get_redis
from app.core.security import decode_token
from app.models.estimation import Estimation, EstimationImage
from app.models.user import User
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

router = APIRouter(prefix="/estimations", tags=["estimations"])
security = HTTPBearer()

def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security), db: Session = Depends(get_db)):
    payload = decode_token(credentials.credentials)
    if not payload:
        raise HTTPException(status_code=401, detail="Érvénytelen token")
    user = db.query(User).filter(User.id == payload["sub"]).first()
    if not user:
        raise HTTPException(status_code=401, detail="Felhasználó nem található")
    return user

def check_rate_limit(user_id: str, redis_client) -> bool:
    key = f"ratelimit:{user_id}"
    current = redis_client.get(key)
    if current and int(current) >= 10:
        return False
    pipe = redis_client.pipeline()
    pipe.incr(key)
    pipe.expire(key, 60)
    pipe.execute()
    return True

def check_subscription(user: User) -> bool:
    if user.trial_ends_at and user.trial_ends_at > datetime.utcnow():
        return True
    return False

@router.post("/")
async def create_estimation(
    description: Optional[str] = Form(None),
    currency: Optional[str] = Form("USD"),
    images: List[UploadFile] = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
    redis_client = Depends(get_redis)
):
    if not check_subscription(current_user):
        raise HTTPException(status_code=403, detail="Előfizetés szükséges")

    if not check_rate_limit(str(current_user.id), redis_client):
        raise HTTPException(status_code=429, detail="Túl sok kérés, várj egy percet")

    if len(images) < 1 or len(images) > 5:
        raise HTTPException(status_code=400, detail="1-5 képet tölts fel")

    estimation = Estimation(
        user_id=current_user.id,
        description=description,
        status="processing"
    )
    db.add(estimation)
    db.commit()
    db.refresh(estimation)

    images_base64 = []
    for i, image in enumerate(images):
        content = await image.read()
        b64 = base64.standard_b64encode(content).decode("utf-8")
        images_base64.append(b64)

        img_record = EstimationImage(
            estimation_id=estimation.id,
            s3_key=f"temp/{estimation.id}/{i}_{image.filename}",
            order=i
        )
        db.add(img_record)

    db.commit()

    try:
        from app.services.ai_service import analyze_images_and_estimate
        result = analyze_images_and_estimate(images_base64, description or "", currency or "USD")
        tokens = result.pop("_tokens", {"input": 0, "output": 0})
        estimation.input_tokens = tokens["input"]
        estimation.output_tokens = tokens["output"]
        estimation.result = result
        estimation.status = "done"
    except Exception as e:
        estimation.status = "failed"
        estimation.result = {"error": str(e)}

    db.commit()
    db.refresh(estimation)

    return {
        "id": str(estimation.id),
        "status": estimation.status,
        "result": estimation.result,
        "created_at": estimation.created_at
    }

@router.get("/")
def get_estimations(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    estimations = db.query(Estimation).filter(
        Estimation.user_id == current_user.id
    ).order_by(Estimation.created_at.desc()).all()

    return [
        {
            "id": str(e.id),
            "status": e.status,
            "description": e.description,
            "result": e.result,
            "created_at": e.created_at
        }
        for e in estimations
    ]
