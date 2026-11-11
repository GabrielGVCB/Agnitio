import io
import uuid
from unittest.mock import AsyncMock, patch
import pytest
from httpx import ASGITransport, AsyncClient
from src.config import settings
from src.main import app


@pytest.mark.asyncio
async def test_health_endpoint():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        response = await client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "UP"
        assert data["service"] == "vision-service"


@pytest.mark.asyncio
async def test_search_requires_api_key(sample_face_image_bytes):
    event_id = str(uuid.uuid4())
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        response = await client.post(
            "/api/v1/faces/search",
            data={"eventId": event_id},
            files={"selfie": ("selfie.jpg", sample_face_image_bytes, "image/jpeg")}
        )
        assert response.status_code == 401


@pytest.mark.asyncio
async def test_search_with_valid_api_key(sample_face_image_bytes):
    event_id = str(uuid.uuid4())
    expected_photo_id = str(uuid.uuid4())

    mock_results = [{"photoId": expected_photo_id, "similarity": 0.94}]

    with patch("src.api.search_similar_faces", new_callable=AsyncMock) as mock_search:
        mock_search.return_value = mock_results

        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post(
                "/api/v1/faces/search",
                headers={"X-Internal-Api-Key": settings.INTERNAL_API_KEY},
                data={"eventId": event_id, "threshold": 0.60, "limit": 10},
                files={"selfie": ("selfie.jpg", sample_face_image_bytes, "image/jpeg")}
            )

            assert response.status_code == 200
            data = response.json()
            assert data["totalMatches"] == 1
            assert data["matches"][0]["photoId"] == expected_photo_id
            assert data["matches"][0]["similarity"] == 0.94
            mock_search.assert_called_once()


@pytest.mark.asyncio
async def test_purge_event_faces():
    event_id = str(uuid.uuid4())

    with patch("src.api.delete_event_faces", new_callable=AsyncMock) as mock_delete:
        mock_delete.return_value = 12

        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.delete(
                f"/api/v1/faces/events/{event_id}",
                headers={"X-Internal-Api-Key": settings.INTERNAL_API_KEY}
            )

            assert response.status_code == 200
            data = response.json()
            assert data["eventId"] == event_id
            assert data["deletedFacesCount"] == 12
            mock_delete.assert_called_once_with(event_id)
