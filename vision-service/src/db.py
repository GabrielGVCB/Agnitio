import json
import logging
from typing import Any
import asyncpg
from pgvector.asyncpg import register_vector
from src.config import settings

logger = logging.getLogger(__name__)

_pool: asyncpg.Pool | None = None


async def get_db_pool() -> asyncpg.Pool:
    global _pool
    if _pool is None:
        async def init_connection(conn: asyncpg.Connection):
            await register_vector(conn)

        _pool = await asyncpg.create_pool(
            settings.database_url,
            init=init_connection,
            min_size=2,
            max_size=10
        )
    return _pool


async def init_db() -> None:
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        await conn.execute("CREATE EXTENSION IF NOT EXISTS vector;")
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS face_embeddings (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                event_id UUID NOT NULL,
                photo_id UUID NOT NULL,
                embedding vector(512) NOT NULL,
                bbox JSONB,
                detection_score FLOAT NOT NULL DEFAULT 1.0,
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
            );

            CREATE INDEX IF NOT EXISTS idx_face_embeddings_event_id 
                ON face_embeddings(event_id);
            CREATE INDEX IF NOT EXISTS idx_face_embeddings_photo_id 
                ON face_embeddings(photo_id);
            CREATE INDEX IF NOT EXISTS idx_face_embeddings_hnsw 
                ON face_embeddings USING hnsw (embedding vector_cosine_ops);
        """)
    logger.info("Database initialized successfully with pgvector and face_embeddings schema.")


async def close_db() -> None:
    global _pool
    if _pool is not None:
        await _pool.close()
        _pool = None
        logger.info("Database pool closed.")


async def insert_face_embeddings(
    event_id: str,
    photo_id: str,
    faces: list[dict[str, Any]]
) -> int:
    """
    Inserts extracted faces into pgvector.
    faces is a list of dicts with:
      'embedding': list[float] of length 512
      'bbox': dict (e.g. {'x': ..., 'y': ..., 'w': ..., 'h': ...})
      'score': float
    """
    if not faces:
        return 0

    pool = await get_db_pool()
    async with pool.acquire() as conn:
        async with conn.transaction():
            # Idempotence: remove existing embeddings for this photo before re-inserting
            await conn.execute(
                "DELETE FROM face_embeddings WHERE photo_id = $1::uuid;",
                photo_id
            )
            for face in faces:
                await conn.execute(
                    """
                    INSERT INTO face_embeddings (event_id, photo_id, embedding, bbox, detection_score)
                    VALUES ($1::uuid, $2::uuid, $3, $4::jsonb, $5);
                    """,
                    event_id,
                    photo_id,
                    face["embedding"],
                    json.dumps(face.get("bbox", {})),
                    face.get("score", 1.0)
                )
    return len(faces)


async def search_similar_faces(
    event_id: str,
    query_embedding: list[float],
    threshold: float = 0.60,
    limit: int = 50
) -> list[dict[str, Any]]:
    """
    Searches for closest faces in the given event using cosine similarity.
    Cosine similarity = 1 - cosine distance.
    Returns distinct photo_ids with max similarity.
    """
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        # We group by photo_id so each photo is returned at most once with its best face match score
        query = """
            SELECT 
                photo_id::text AS photo_id,
                MAX(1 - (embedding <=> $2)) AS similarity
            FROM face_embeddings
            WHERE event_id = $1::uuid
              AND 1 - (embedding <=> $2) >= $3
            GROUP BY photo_id
            ORDER BY similarity DESC
            LIMIT $4;
        """
        rows = await conn.fetch(query, event_id, query_embedding, threshold, limit)
        return [
            {
                "photoId": row["photo_id"],
                "similarity": float(row["similarity"])
            }
            for row in rows
        ]


async def delete_event_faces(event_id: str) -> int:
    """
    Purges biometric embeddings for a specific event (LGPD retention policy).
    """
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        result = await conn.execute(
            "DELETE FROM face_embeddings WHERE event_id = $1::uuid;",
            event_id
        )
        # result is e.g. "DELETE 15"
        try:
            return int(result.split()[-1])
        except (ValueError, IndexError):
            return 0
