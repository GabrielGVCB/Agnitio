import json
import logging
import threading
import time
from typing import Any
import pika
from src.config import settings
from src.db import close_db, insert_face_embeddings
from src.face_engine import get_face_engine
from src.schemas import PhotoFacesIndexedMessage, PhotoProcessedMessage
from src.storage import download_photo

logger = logging.getLogger(__name__)

_consumer_thread: threading.Thread | None = None
_stop_event = threading.Event()


def get_rabbitmq_connection() -> pika.BlockingConnection:
    credentials = pika.PlainCredentials(settings.RABBITMQ_USER, settings.RABBITMQ_PASSWORD)
    parameters = pika.ConnectionParameters(
        host=settings.RABBITMQ_HOST,
        port=settings.RABBITMQ_PORT,
        credentials=credentials,
        heartbeat=60,
        blocked_connection_timeout=300
    )
    return pika.BlockingConnection(parameters)


def setup_rabbitmq(channel: pika.adapters.blocking_connection.BlockingChannel) -> None:
    # Exchange topic 'photos'
    channel.exchange_declare(
        exchange=settings.RABBITMQ_EXCHANGE,
        exchange_type="topic",
        durable=True
    )

    # Dead Letter Queue
    dlq_name = f"{settings.RABBITMQ_QUEUE_PROCESSED}.dlq"
    channel.queue_declare(queue=dlq_name, durable=True)

    # Main Queue with DLQ arguments
    args = {
        "x-dead-letter-exchange": "",
        "x-dead-letter-routing-key": dlq_name,
    }
    channel.queue_declare(
        queue=settings.RABBITMQ_QUEUE_PROCESSED,
        durable=True,
        arguments=args
    )

    # Bind queue to exchange with routing key 'photo.processed'
    channel.queue_bind(
        exchange=settings.RABBITMQ_EXCHANGE,
        queue=settings.RABBITMQ_QUEUE_PROCESSED,
        routing_key=settings.RABBITMQ_ROUTING_KEY_PROCESSED
    )
    logger.info(
        f"RabbitMQ queue '{settings.RABBITMQ_QUEUE_PROCESSED}' bound to "
        f"'{settings.RABBITMQ_EXCHANGE}' with key '{settings.RABBITMQ_ROUTING_KEY_PROCESSED}'."
    )


def publish_faces_indexed(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    indexed_event: PhotoFacesIndexedMessage
) -> None:
    body = indexed_event.model_dump_json()
    channel.basic_publish(
        exchange=settings.RABBITMQ_EXCHANGE,
        routing_key=settings.RABBITMQ_ROUTING_KEY_INDEXED,
        body=body,
        properties=pika.BasicProperties(
            content_type="application/json",
            delivery_mode=2,  # make message persistent
            headers={
                "X-Correlation-ID": indexed_event.correlationId,
            }
        )
    )
    logger.info(
        f"Published photo.faces.indexed for photo {indexed_event.photoId} "
        f"({indexed_event.facesCount} faces)."
    )


async def handle_photo_processed_payload(payload_dict: dict[str, Any]) -> int:
    message = PhotoProcessedMessage(**payload_dict)
    key = message.previewStorageKey or message.originalStorageKey
    if not key:
        logger.warning(f"No storage key found in message for photo {message.photoId}")
        return 0

    logger.info(f"Downloading photo {message.photoId} from storage key: {key}")
    photo_bytes = download_photo(key)

    face_engine = get_face_engine()
    faces = face_engine.detect_and_extract_faces(photo_bytes)
    logger.info(f"Detected {len(faces)} faces for photo {message.photoId}")

    inserted = await insert_face_embeddings(message.eventId, message.photoId, faces)
    return inserted


def run_consumer_loop() -> None:
    import asyncio

    logger.info("Starting RabbitMQ consumer thread for vision-service...")
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

    while not _stop_event.is_set():
        connection = None
        try:
            connection = get_rabbitmq_connection()
            channel = connection.channel()
            setup_rabbitmq(channel)
            channel.basic_qos(prefetch_count=5)

            def on_message(ch, method, properties, body):
                try:
                    payload = json.loads(body.decode("utf-8"))
                    inserted = loop.run_until_complete(handle_photo_processed_payload(payload))
                    
                    # Publish indexed confirmation
                    corr_id = payload.get("correlationId", "")
                    indexed_msg = PhotoFacesIndexedMessage(
                        correlationId=corr_id,
                        photoId=payload["photoId"],
                        eventId=payload["eventId"],
                        facesCount=inserted
                    )
                    publish_faces_indexed(ch, indexed_msg)
                    ch.basic_ack(delivery_tag=method.delivery_tag)
                except Exception as e:
                    logger.error(f"Error processing RabbitMQ message: {e}", exc_info=True)
                    ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

            channel.basic_consume(
                queue=settings.RABBITMQ_QUEUE_PROCESSED,
                on_message_callback=on_message
            )
            logger.info("Vision-service RabbitMQ consumer is active and consuming.")

            while not _stop_event.is_set():
                connection.process_data_events(time_limit=1.0)

        except Exception as e:
            if not _stop_event.is_set():
                logger.warning(f"RabbitMQ connection lost, retrying in 5 seconds: {e}")
                time.sleep(5)
        finally:
            if connection and not connection.is_closed:
                try:
                    connection.close()
                except Exception:
                    pass

    loop.run_until_complete(close_db())
    loop.close()
    logger.info("RabbitMQ consumer thread terminated.")


def start_messaging() -> None:
    global _consumer_thread, _stop_event
    _stop_event.clear()
    _consumer_thread = threading.Thread(target=run_consumer_loop, daemon=True)
    _consumer_thread.start()


def stop_messaging() -> None:
    global _stop_event
    _stop_event.set()
