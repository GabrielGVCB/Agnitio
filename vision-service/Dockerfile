FROM python:3.12-slim

WORKDIR /app

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DEFAULT_TIMEOUT=100

# libgl1, libglib2.0-0 and libxcb1 are native libraries required by OpenCV ("opencv-python",
# pulled in by insightface); python:slim does not ship them.
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    build-essential \
    libgl1 \
    libglib2.0-0 \
    libxcb1 \
    && rm -rf /var/lib/apt/lists/*

COPY pyproject.toml .
# "[ml]" installs InsightFace + ONNX Runtime (+ OpenCV through insightface). Without it the service would only
# have the deterministic (fake) engine and selfie search could never match real faces.
RUN pip install --upgrade pip setuptools wheel && pip install ".[ml]"

COPY src/ ./src/

EXPOSE 8000

CMD ["uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "8000"]
