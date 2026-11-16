import asyncio
import threading
from unittest.mock import AsyncMock, patch

from src import db


async def _fake_create_pool(*args, **kwargs):
    pool = AsyncMock()
    pool.loop = asyncio.get_running_loop()
    return pool


def test_each_event_loop_gets_its_own_pool():
    """The API loop and the consumer-thread loop must never share an asyncpg pool."""
    db._pools.clear()
    with patch("src.db.asyncpg.create_pool", side_effect=_fake_create_pool):
        main_pool = asyncio.run(db.get_db_pool())

        result = {}

        def consumer_thread():
            loop = asyncio.new_event_loop()
            try:
                result["pool"] = loop.run_until_complete(db.get_db_pool())
                result["same_again"] = loop.run_until_complete(db.get_db_pool())
            finally:
                loop.close()

        t = threading.Thread(target=consumer_thread)
        t.start()
        t.join()

    assert result["pool"] is not main_pool
    assert result["same_again"] is result["pool"]
    assert result["pool"].loop is not main_pool.loop
    db._pools.clear()
