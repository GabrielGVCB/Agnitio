from datetime import datetime, timezone
import uuid
from pydantic import BaseModel, Field


class FaceSearchMatchDto(BaseModel):
    photoId: str
    similarity: float = Field(ge=0.0, le=1.0)


class FaceSearchResponseDto(BaseModel):
    totalMatches: int
    matches: list[FaceSearchMatchDto]


class PhotoProcessedMessage(BaseModel):
    messageId: str
    correlationId: str
    occurredAt: str | float | int
    schemaVersion: int = 1
    photoId: str
    eventId: str
    originalStorageKey: str | None = None
    previewStorageKey: str | None = None
    thumbnailStorageKey: str | None = None
    width: int | None = None
    height: int | None = None


class PhotoFacesIndexedMessage(BaseModel):
    messageId: str = Field(default_factory=lambda: str(uuid.uuid4()))
    correlationId: str
    occurredAt: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    schemaVersion: int = 1
    photoId: str
    eventId: str
    facesCount: int


class DeleteFacesResponseDto(BaseModel):
    eventId: str
    deletedFacesCount: int
