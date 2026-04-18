from sqlalchemy import Column, String, DateTime, Boolean
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from datetime import datetime
import uuid
from app.core.database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    email = Column(String, unique=True, nullable=False, index=True)
    hashed_password = Column(String, nullable=False)
    is_active = Column(Boolean, default=True)
    trial_ends_at = Column(DateTime, nullable=True)
    refresh_token = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    estimations = relationship("Estimation", back_populates="user")
