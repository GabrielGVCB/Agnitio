from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # Database (Postgres + pgvector)
    POSTGRES_VISION_HOST: str = "localhost"
    POSTGRES_VISION_PORT: int = 5433
    POSTGRES_VISION_DB: str = "agnitio_vision"
    POSTGRES_VISION_USER: str = "vision_user"
    POSTGRES_VISION_PASSWORD: str = "vision_secret"

    # RabbitMQ
    RABBITMQ_HOST: str = "localhost"
    RABBITMQ_PORT: int = 5673
    RABBITMQ_USER: str = "agnitio_rabbit"
    RABBITMQ_PASSWORD: str = "rabbit_secret"
    RABBITMQ_EXCHANGE: str = "photos"
    RABBITMQ_QUEUE_PROCESSED: str = "photos.vision.processed"
    RABBITMQ_ROUTING_KEY_PROCESSED: str = "photo.processed"
    RABBITMQ_ROUTING_KEY_INDEXED: str = "photo.faces.indexed"

    # Storage (S3 / MinIO)
    S3_ENDPOINT: str = "http://localhost:9000"
    S3_REGION: str = "us-east-1"
    S3_BUCKET: str = "agnitio-photos"
    S3_ACCESS_KEY: str = "minio_admin"
    S3_SECRET_KEY: str = "minio_secret_key"

    # Security
    INTERNAL_API_KEY: str = "local-dev-internal-api-key-32chars-min"

    # App
    PORT: int = 8000
    DEFAULT_SIMILARITY_THRESHOLD: float = 0.60
    DEFAULT_MAX_RESULTS: int = 50
    FACE_ENGINE_TYPE: str = "auto"  # "auto", "insightface", or "deterministic"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

    @property
    def database_url(self) -> str:
        return (
            f"postgresql://{self.POSTGRES_VISION_USER}:{self.POSTGRES_VISION_PASSWORD}"
            f"@{self.POSTGRES_VISION_HOST}:{self.POSTGRES_VISION_PORT}/{self.POSTGRES_VISION_DB}"
        )


settings = Settings()
