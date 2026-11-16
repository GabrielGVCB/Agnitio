import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from src.api import router as faces_router
from src.db import close_db, init_db
from src.face_engine import get_face_engine
from src.messaging import start_messaging, stop_messaging

logging.basicConfig(
    level=logging.INFO,
    format='{"timestamp":"%(asctime)s","level":"%(levelname)s","service":"vision-service","message":"%(message)s"}'
)
logger = logging.getLogger("vision-service")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Initializing vision-service database and messaging...")
    # Load the face engine eagerly: with FACE_ENGINE_TYPE=insightface a broken install or a
    # missing model must stop the service at startup, not surface later as wrong search results.
    engine = get_face_engine()
    logger.info(f"Face engine ready: {type(engine).__name__}")

    try:
        await init_db()
    except Exception as e:
        logger.warning(f"Database initialization warning (will retry on query): {e}")

    try:
        start_messaging()
    except Exception as e:
        logger.warning(f"Messaging start warning (broker may be starting): {e}")

    yield

    logger.info("Shutting down vision-service...")
    stop_messaging()
    await close_db()


app = FastAPI(
    title="Agnitio Vision Service",
    description="Facial detection, embedding extraction and pgvector search microservice",
    version="0.1.0",
    lifespan=lifespan
)

app.include_router(faces_router)


@app.get("/health", tags=["System"])
async def health():
    return {
        "status": "UP",
        "service": "vision-service"
    }


if __name__ == "__main__":
    import uvicorn
    from src.config import settings
    uvicorn.run("src.main:app", host="0.0.0.0", port=settings.PORT, reload=True)
