# Contratos de Mensagens Assíncronas (RabbitMQ)

O Agnitio utiliza um Exchange do tipo **topic** chamado `photos`.
Todas as mensagens trafegadas no broker seguem uma estrutura padronizada com envelope de rastreabilidade.

## Envelope Padrão de Mensagem

Toda mensagem JSON publicada deve conter os seguintes metadados de cabeçalho:

```json
{
  "messageId": "c83b8a41-2e65-4f40-84df-f53856b3e8c7",
  "correlationId": "50c82fb6-ecb8-4389-a292-6d2c4bf2a647",
  "occurredAt": "2026-11-11T13:30:00.000Z",
  "schemaVersion": 1,
  "payload": {}
}
```

## Routing Keys e Tópicos

| Routing Key | Publicador | Consumidor | Descrição do Payload |
|---|---|---|---|
| `photo.uploaded` | `photo-api` | `image-worker` | Foto bruta enviada ao storage, pronta para variantes e EXIF. |
| `photo.processed` | `image-worker` | `vision-service` | Variantes (thumb/preview) geradas; pronta para visão computacional. |
| `photo.faces.indexed` | `vision-service` | `photo-api` | Rostos detectados e indexados com sucesso no pgvector. |

### Detalhamento dos Contratos (Schema v1)

#### `photo.uploaded`
```json
{
  "messageId": "c83b8a41-2e65-4f40-84df-f53856b3e8c7",
  "correlationId": "50c82fb6-ecb8-4389-a292-6d2c4bf2a647",
  "occurredAt": "2026-11-11T13:30:00.000Z",
  "schemaVersion": 1,
  "photoId": "a57bbda0-3bd8-47bc-a567-27b9c9f4d1c9",
  "eventId": "18e69efc-f1d2-43bb-a5a4-569614db2540",
  "storageKey": "events/18e69efc-f1d2-43bb-a5a4-569614db2540/original/a57bbda0-3bd8-47bc-a567-27b9c9f4d1c9.jpg",
  "originalFileName": "runner_finish.jpg"
}
```

#### `photo.processed`
```json
{
  "messageId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "correlationId": "50c82fb6-ecb8-4389-a292-6d2c4bf2a647",
  "occurredAt": "2026-11-11T13:30:05.000Z",
  "schemaVersion": 1,
  "photoId": "a57bbda0-3bd8-47bc-a567-27b9c9f4d1c9",
  "eventId": "18e69efc-f1d2-43bb-a5a4-569614db2540",
  "originalStorageKey": "events/18e69efc-f1d2-43bb-a5a4-569614db2540/original/a57bbda0-3bd8-47bc-a567-27b9c9f4d1c9.jpg",
  "previewStorageKey": "events/18e69efc-f1d2-43bb-a5a4-569614db2540/preview/a57bbda0-3bd8-47bc-a567-27b9c9f4d1c9.webp",
  "thumbnailStorageKey": "events/18e69efc-f1d2-43bb-a5a4-569614db2540/thumb/a57bbda0-3bd8-47bc-a567-27b9c9f4d1c9.webp",
  "width": 3840,
  "height": 2160
}
```

#### `photo.faces.indexed`
```json
{
  "messageId": "7f8b9c0d-1e2f-4a5b-8c9d-0e1f2a3b4c5d",
  "correlationId": "50c82fb6-ecb8-4389-a292-6d2c4bf2a647",
  "occurredAt": "2026-11-11T13:30:10.000Z",
  "schemaVersion": 1,
  "photoId": "a57bbda0-3bd8-47bc-a567-27b9c9f4d1c9",
  "eventId": "18e69efc-f1d2-43bb-a5a4-569614db2540",
  "facesCount": 3
}
```

## Regras de Consumo

1. **Idempotência Obrigatória**: Todo consumidor deve verificar se a operação já foi concluída antes de reprocessar.
2. **Dead Letter Queue (DLQ)**: Cada fila possui uma DLQ correspondente (`*.dlq`) e estratégia de retry com backoff via TTL.
3. **Evolução de Esquema**: Qualquer alteração em contratos de payload exige atualização da documentação e incremento do `schemaVersion`.
