# ADR 0003: Arquitetura de Observabilidade Distribuída (Prometheus, Grafana, Loki, Tempo e OpenTelemetry)

## Contexto
O ecossistema do Agnitio é composto por múltiplos serviços distribuídos operando de forma assíncrona e síncrona: a `photo-api` recebe uploads e requisições de clientes, publica eventos no RabbitMQ; o `image-worker` consome mensagens, gera variantes de imagens e persiste no S3/MinIO; o `vision-service` indexa faces com pgvector; e o frontend consome progresso via SSE e fotos via galeria pública.

Para garantir rastreabilidade de ponta a ponta, monitoramento de saúde, identificação de gargalos de desempenho e diagnóstico rápido de falhas sem violar a LGPD (sem expor fotos ou dados biométricos em logs), é indispensável uma estratégia de observabilidade baseada nos três pilares fundamentais:
1. **Métricas** (agregações numéricas para visibilidade contínua de latência, volume e erros);
2. **Logs** (registros estruturados para análise contextual com trace ID);
3. **Traces** (rastreamento distribuído unificado de ponta a ponta desde o upload até a indexação).

## Decisão

1. **Padrão OpenTelemetry & Micrometer**:
   - Adotamos o padrão OpenTelemetry com **Micrometer Tracing** no backend Java (`micrometer-tracing-bridge-otel`).
   - Propagação de contexto de tracing utilizando o padrão W3C Trace Context (`traceparent` e `tracestate`) tanto em chamadas HTTP quanto nos metadados de mensagens AMQP no RabbitMQ.
   - O `traceId` originado na confirmação de upload da foto na `photo-api` viaja dentro do cabeçalho da mensagem `photo.uploaded` e é continuado no `image-worker` durante o processamento da imagem, permitindo visualização de uma única árvore de execução (span tree).

2. **Métricas com Prometheus e Micrometer**:
   - Exposição de métricas no formato padrão do Prometheus via endpoint `/actuator/prometheus` no Spring Boot Actuator.
   - Métricas customizadas de negócio registradas através de `MeterRegistry`:
     - `photo.processing.duration`: Timer medindo a latência do processamento por foto com percentis.
     - `photo.processing.success.total`: Contador de fotos processadas com sucesso.
     - `photo.processing.failure.total`: Contador de fotos que falharam durante processamento.
     - `photo.upload.confirmed.total`: Contador de uploads confirmados.
   - Coleta de métricas do RabbitMQ (tamanho de filas, taxas de publicação e consumo via plugin `rabbitmq_prometheus`).
   - Servidor Prometheus local no Docker Compose configurado para scrape a cada 5 segundos dos serviços.

3. **Tracing com Grafana Tempo**:
   - Servidor Grafana Tempo no Docker Compose recebendo traces via protocolo OTLP (OpenTelemetry Protocol HTTP na porta 4318 ou gRPC na porta 4317).
   - O Spring Boot envia os spans de tracing diretamente para o endpoint OTLP do Tempo.

4. **Logs Estruturados em JSON com Grafana Loki & Promtail**:
   - Padronização de logs no formato JSON estruturado contendo campos contextuais obrigatórios injetados via MDC/Tracing:
     - `traceId`, `spanId`, `correlationId`, `service`, `level`, `timestamp`, `message` e stack traces normalizados.
   - Proibição estrita de logging de dados sensíveis ou biométricos (tokens, senhas, embeddings ou imagens) em conformidade com as regras de LGPD do projeto.
   - Grafana Loki no Docker Compose para indexação eficiente de logs baseada em labels.
   - Promtail coletando os logs estruturados dos containers Docker e ingerindo no Loki.

5. **Visualização Centralizada no Grafana com Provisioning Automatizado**:
   - Grafana rodando na porta `3001` (para evitar colisão com o frontend Next.js na porta `3000`).
   - Datasources pré-configurados automaticamente via arquivos YAML (`datasources.yml`) conectando Prometheus, Tempo e Loki.
   - Configuração de correlação *Trace-to-Logs* e *Logs-to-Trace*: ao inspecionar um trace no Tempo, o desenvolvedor pode clicar diretamente para ver os logs daquele `traceId` no Loki, e vice-versa.
   - Dashboard unificado versionado no repositório (`infra/grafana/dashboards/agnitio-overview.json`) apresentando métricas operacionais, filas e visualização do pipeline.

## Consequências

- **Positivas**:
  - Visibilidade completa de qualquer foto no pipeline distribuído através de um único `traceId` ou `correlationId`.
  - Diagnóstico imediato de gargalos de processamento de imagem ou enfileiramento no RabbitMQ.
  - Conformidade com LGPD mantida sem perda de contexto operacional.
  - Subida automática de toda a stack com `docker compose up -d` e dashboards pré-provisionados sem configurações manuais no navegador.

- **Mitigações**:
  - Leve overhead de CPU e rede na geração e exportação de traces/métricas, minimizado pela amostragem configurável e buffering assíncrono do OTLP exporter e logback.
