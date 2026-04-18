from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.database import engine, Base
from app.models import user, estimation
from app.routers import auth, estimation as estimation_router
from app.routers.dashboard import router as dashboard_router

Base.metadata.create_all(bind=engine)

app = FastAPI(title="ValuAI API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(estimation_router.router)
app.include_router(dashboard_router)

@app.get("/health")
def health():
    return {"status": "ok", "service": "ValuAI API"}
