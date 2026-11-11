# ADR 0004: Reconhecimento Facial com InsightFace, pgvector e Conformidade LGPD

## Contexto
O diferencial central da plataforma Agnitio é permitir que participantes de eventos esportivos, casamentos e formaturas encontrem suas fotos instantaneamente enviando apenas uma selfie, eliminando a dependência de identificação manual ou número de peito (inexistente em casamentos e formaturas).

O reconhecimento facial exige:
1. **Detecção e Extração de Vetores**: Processamento de imagens de alta resolução com detecção precisa de múltiplos rostos e extração de embeddings vetoriais (512 dimensões).
2. **Busca Vetorial Escalável**: Capacidade de buscar vizinhos mais próximos (KNN / Cosine Similarity) filtrando estritamente pelas fotos do evento especificado.
3. **Conformidade Estrita com a LGPD (Lei Geral de Proteção de Dados)**: Embeddings faciais e fotos de rostos são dados biométricos sensíveis (Art. 5º, II da LGPD). A selfie do participante jamais pode ser armazenada de forma permanente ou utilizada para qualquer finalidade além daquela busca específica.
4. **Isolamento de Domínio e Microsserviços**: O serviço de visão computacional (`vision-service`) deve possuir seu próprio banco de dados isolado (`postgres-vision`), comunicando-se de forma assíncrona para indexação (RabbitMQ) e síncrona para busca (HTTP interno).

## Decisão

1. **Serviço de Visão Computacional Dedicado (`vision-service`)**:
   - Implementado em Python 3.12+ com **FastAPI** e **Uvicorn**, aproveitando o ecossistema maduro de visão computacional e aprendizado profundo (InsightFace / ONNX Runtime, NumPy, Pillow).
   - Comunicação síncrona restrita à rede interna entre a `photo-api` e o `vision-service`, autenticada por chave compartilhada via cabeçalho `X-Internal-Api-Key`.

2. **Indexação e Armazenamento Vetorial com `pgvector`**:
   - Banco de dados dedicado `postgres-vision` (PostgreSQL 17 com extensão `pgvector`).
   - Tabela `face_embeddings` contendo:
     - `event_id` (UUID) e `photo_id` (UUID);
     - `embedding` do tipo `vector(512)` normalizado;
     - `bbox` (coordenadas do rosto na foto) e `detection_score`.
   - Índice HNSW (`vector_cosine_ops`) para busca de alta performance com complexidade sublinear.
   - Consulta de busca estruturada com filtro por evento e cálculo de distância de cosseno:
     ```sql
     SELECT photo_id, 1 - (embedding <=> :selfie_embedding) AS similarity
     FROM face_embeddings
     WHERE event_id = :event_id
       AND 1 - (embedding <=> :selfie_embedding) >= :threshold
     ORDER BY similarity DESC
     LIMIT :limit;
     ```

3. **Arquitetura Assíncrona para Indexação**:
   - O `image-worker` publica `photo.processed` ao concluir a geração de variantes.
   - O `vision-service` consome `photo.processed`, obtém a imagem preview via S3/MinIO, detecta as faces e indexa os embeddings no `postgres-vision`.
   - Ao concluir a indexação, o `vision-service` publica `photo.faces.indexed` no RabbitMQ, notificando a `photo-api`.

4. **Busca Síncrona por Selfie (HTTP REST)**:
   - O usuário envia a selfie via frontend -> `photo-api` valida consentimento e rate limit no Redis -> encaminha a imagem para o `vision-service` via `POST /api/v1/faces/search` -> `vision-service` calcula o embedding da selfie, busca no pgvector e devolve os IDs de fotos -> `photo-api` monta as URLs assinadas e retorna ao participante.

5. **Conformidade com a LGPD**:
   - **Consentimento Explícito**: O participante deve marcar ativamente a autorização antes de abrir a câmera ou enviar a selfie.
   - **Descarte Imediato**: A imagem da selfie e o embedding gerado são processados exclusivamente em memória volátil e descartados imediatamente após a busca. Nenhum byte da selfie é gravado em disco ou banco.
   - **Retenção e Expurgo**: O `vision-service` disponibiliza endpoint para exclusão em lote (`DELETE /api/v1/faces/events/{eventId}`) para limpeza após o prazo de retenção biométrica do evento.
   - **Sanitização de Logs**: É estritamente proibido registrar em logs imagens, vetores de embeddings ou dados que identifiquem biometria.

## Consequências

- **Positivas**:
  - Busca ultra-rápida (sub-segundo) de fotos em eventos com dezenas de milhares de imagens.
  - Separação clara entre a lógica de negócio transacional (Java/Spring) e inteligência artificial/vetores (Python/pgvector).
  - Total conformidade jurídica com a LGPD.
- **Mitigações**:
  - Manutenção de dois ambientes de banco de dados (`photo` e `vision`), mitigada pelo uso do Docker Compose unificado.
