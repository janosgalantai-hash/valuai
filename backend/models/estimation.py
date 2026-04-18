from sqlalchemy import Column, String, DateTime, ForeignKey, Integer, JSON
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from datetime import datetime
import uuid
from app.core.database import Base

class Estimation(Base):
    __tablename__ = "estimations"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    description = Column(String, nullable=True)
    status = Column(String, default="pending")  # pending, processing, done, failed
    result = Column(JSON, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    input_tokens = Column(Integer, default=0)
    output_tokens = Column(Integer, default=0)

    user = relationship("User", back_populates="estimations")
    images = relationship("EstimationImage", back_populates="estimation")

class EstimationImage(Base):
    __tablename__ = "estimation_images"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    estimation_id = Column(UUID(as_uuid=True), ForeignKey("estimations.id"), nullable=False)
    s3_key = Column(String, nullable=False)
    order = Column(Integer, default=0)

    estimation = relationship("Estimation", back_populates="images")
