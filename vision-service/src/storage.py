import io
import logging
import boto3
from botocore.client import Config
from src.config import settings

logger = logging.getLogger(__name__)

_s3_client = None


def get_s3_client():
    global _s3_client
    if _s3_client is None:
        _s3_client = boto3.client(
            "s3",
            endpoint_url=settings.S3_ENDPOINT,
            region_name=settings.S3_REGION,
            aws_access_key_id=settings.S3_ACCESS_KEY,
            aws_secret_access_key=settings.S3_SECRET_KEY,
            config=Config(s3={"addressing_style": "path"}),
        )
    return _s3_client


def download_photo(storage_key: str, bucket: str = settings.S3_BUCKET) -> bytes:
    """
    Downloads photo bytes from S3/MinIO.
    """
    s3 = get_s3_client()
    buffer = io.BytesIO()
    s3.download_fileobj(bucket, storage_key, buffer)
    buffer.seek(0)
    return buffer.read()
