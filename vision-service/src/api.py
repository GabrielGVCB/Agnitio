import logging
from typing import Annotated
from fastapi import APIRouter, Depends, File, Form, Header, HTTPException, UploadFile, status
from src.config import settings
from src.db import delete_event_faces, search_similar_faces
from src.face_engine import get_face_engine
from src.schemas import DeleteFacesResponseDto, FaceSearchMatchDto, FaceSearchResponseDto

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/faces", tags=["Faces"])


async def verify_internal_api_key(
    x_internal_api_key: Annotated[str | None, Header(alias="X-Internal-Api-Key")] = None
) -> None:
    if not x_internal_api_key or x_internal_api_key != settings.INTERNAL_API_KEY:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing X-Internal-Api-Key header"
        )


@router.post(
    "/search",
    response_model=FaceSearchResponseDto,
    dependencies=[Depends(verify_internal_api_key)]
)
async def search_faces_by_selfie(
    eventId: Annotated[str, Form()],
    selfie: Annotated[UploadFile, File(...)],
    threshold: Annotated[float, Form()] = settings.DEFAULT_SIMILARITY_THRESHOLD,
    limit: Annotated[int, Form()] = settings.DEFAULT_MAX_RESULTS
) -> FaceSearchResponseDto:
    """
    Search for event photos matching a user's selfie using pgvector cosine similarity.
    LGPD Rule: The selfie image and its embedding are processed in memory and never persisted.
    """
    try:
        selfie_bytes = await selfie.read()
        if not selfie_bytes:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Empty selfie file uploaded"
            )

        face_engine = get_face_engine()
        query_embedding = face_engine.extract_selfie_embedding(selfie_bytes)

        # LGPD: Explicitly clear selfie bytes from memory
        del selfie_bytes

        if query_embedding is None:
            logger.info(f"No face detected in selfie for event {eventId}")
            return FaceSearchResponseDto(totalMatches=0, matches=[])

        matches_raw = await search_similar_faces(
            event_id=eventId,
            query_embedding=query_embedding,
            threshold=threshold,
            limit=limit
        )

        matches = [FaceSearchMatchDto(**m) for m in matches_raw]
        logger.info(f"Selfie search for event {eventId} returned {len(matches)} matches.")
        return FaceSearchResponseDto(totalMatches=len(matches), matches=matches)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error during selfie search for event {eventId}: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An error occurred during facial recognition search"
        )


@router.delete(
    "/events/{eventId}",
    response_model=DeleteFacesResponseDto,
    dependencies=[Depends(verify_internal_api_key)]
)
async def purge_event_faces(eventId: str) -> DeleteFacesResponseDto:
    """
    Purges all facial embeddings for an event (LGPD retention compliance).
    """
    try:
        deleted_count = await delete_event_faces(eventId)
        logger.info(f"Purged {deleted_count} face embeddings for event {eventId}.")
        return DeleteFacesResponseDto(eventId=eventId, deletedFacesCount=deleted_count)
    except Exception as e:
        logger.error(f"Failed to purge face embeddings for event {eventId}: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to purge event face embeddings"
        )
