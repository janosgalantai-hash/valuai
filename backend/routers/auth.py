from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from datetime import datetime, timedelta
from app.core.database import get_db
from app.core.security import hash_password, verify_password, create_access_token, create_refresh_token
from app.models.user import User
from app.schemas.user import UserRegister, UserLogin, UserResponse, TokenResponse
from pydantic import BaseModel

router = APIRouter(prefix="/auth", tags=["auth"])

class RefreshRequest(BaseModel):
    refresh_token: str

@router.post("/register", response_model=UserResponse)
def register(data: UserRegister, db: Session = Depends(get_db)):
    if db.query(User).filter(User.email == data.email).first():
        raise HTTPException(status_code=400, detail="Email már foglalt")
    user = User(
        email=data.email,
        hashed_password=hash_password(data.password),
        trial_ends_at=datetime.utcnow() + timedelta(days=30)
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user

@router.post("/login")
def login(data: UserLogin, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == data.email).first()
    if not user or not verify_password(data.password, user.hashed_password):
        raise HTTPException(status_code=401, detail="Hibás email vagy jelszó")
    
    access_token = create_access_token({"sub": str(user.id)})
    refresh_token = create_refresh_token()
    
    user.refresh_token = refresh_token
    db.commit()
    
    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer"
    }

@router.post("/refresh")
def refresh(data: RefreshRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.refresh_token == data.refresh_token).first()
    if not user:
        raise HTTPException(status_code=401, detail="Érvénytelen refresh token")
    
    access_token = create_access_token({"sub": str(user.id)})
    refresh_token = create_refresh_token()
    
    user.refresh_token = refresh_token
    db.commit()
    
    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer"
    }
