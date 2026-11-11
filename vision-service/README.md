# Vision Service (Python / FastAPI)

Serviço de visão computacional responsável por:
- Consumir eventos `photo.processed` do RabbitMQ.
- Detectar rostos e extrair embeddings vetoriais com InsightFace.
- Indexar vetores no PostgreSQL com `pgvector`.
- Expor endpoint HTTP REST síncrono interno para busca por selfie (`SearchPhotosBySelfie`).
- Na Fase 7, estender com detecção OCR de número de peito (PaddleOCR) para eventos esportivos (`RACE`).

Implementação prevista para a **Fase 6** do roadmap.
