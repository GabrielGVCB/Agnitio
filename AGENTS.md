# AGENTS.md

Este arquivo orienta agentes de IA (Antigravity, Claude Code e outros) neste repositório. Leia-o inteiro antes de qualquer tarefa.

## Visão geral

Plataforma de fotos de eventos (corridas, casamentos, formaturas, eventos corporativos). O fotógrafo cria um evento e sobe milhares de fotos; o sistema processa as imagens em segundo plano e publica uma galeria. O participante encontra as próprias fotos enviando uma selfie (reconhecimento facial). Não existe busca por número de peito.

Projeto de portfólio: a qualidade da arquitetura, dos testes e da documentação importa tanto quanto as funcionalidades. Prefira soluções claras e bem justificadas a atalhos.

Nome do projeto: **Agnitio** (latim para "reconhecimento"). Repositório: `agnitio`.

## Stack

| Camada | Tecnologia |
|---|---|
| API e worker de imagens | Java 25 (LTS), Spring Boot 4.x, Maven multi-módulo |
| Serviço de visão computacional | Python 3.12+, FastAPI, InsightFace, pgvector |
| Frontend | Next.js, TypeScript, Tailwind CSS |
| Banco de dados | PostgreSQL + Flyway (photo-api); PostgreSQL + pgvector (vision-service) |
| Mensageria | RabbitMQ |
| Cache, sessões e progresso | Redis |
| Armazenamento de objetos | Cloudflare R2 (produção), MinIO (local); ambos via API S3 |
| Observabilidade | OpenTelemetry, Grafana, Loki, Prometheus, Tempo |
| Testes | JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit, pytest |
| Infra | Docker, Docker Compose, GitHub Actions |

Ao iniciar a Fase 1, confirme as versões estáveis mais recentes e fixe-as. Não adicione dependências novas sem explicar o motivo e pedir confirmação.

## Estrutura do monorepo

```
agnitio/
├── CLAUDE.md
├── README.md
├── docker-compose.yml
├── docs/
│   ├── adr/                  # Architecture Decision Records
│   ├── architecture.md
│   └── contracts/            # Contratos de mensagens (AsyncAPI / JSON Schema)
├── backend/                  # Maven multi-módulo (Java)
│   ├── pom.xml
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   ├── photo-api/            # bootstrap Spring Boot da API REST
│   └── image-worker/         # bootstrap Spring Boot do worker
├── vision-service/           # Python
└── frontend/                 # Next.js
```

## Serviços

**photo-api** — API REST. Autenticação, fotógrafos, eventos, busca pública de eventos (sem login, só eventos públicos; ADR 0005), URLs pré-assinadas de upload, galeria, busca por selfie (orquestra a chamada ao vision-service), progresso via SSE.

**image-worker** — Consome `photo.uploaded`. Gera thumbnail, preview com marca d'água e extrai EXIF. Salva as variantes no storage, atualiza o status da foto e publica `photo.processed`. Reutiliza os módulos `domain`, `application` e `infrastructure`.

**vision-service** — Consome `photo.processed`, detecta rostos, gera embeddings e os guarda no próprio banco (pgvector). Expõe HTTP interno para busca por selfie.

**frontend** — Duas áreas na mesma aplicação: painel do fotógrafo (autenticado) e galeria pública (renderizada no servidor para gerar prévias de link no WhatsApp e redes sociais).

Cada serviço é dono dos seus dados. O vision-service nunca acessa o banco da photo-api, e vice-versa.

## Clean Architecture (backend Java)

Dependências apontam sempre para dentro: `infrastructure → application → domain`.

- **domain**: entidades, value objects, regras de negócio e exceções de domínio. Java puro. **Proibido** importar Spring, JPA, Jackson, AWS SDK ou qualquer framework.
- **application**: casos de uso (um por classe, ex. `CreateEventUseCase`, `RequestUploadUrlsUseCase`, `SearchPhotosBySelfieUseCase`) e portas (interfaces) que eles exigem, ex. `PhotoStorage`, `EventPublisher`, `FaceSearchClient`, `PhotoRepository`. Sem dependência de framework.
- **infrastructure**: adaptadores que implementam as portas (R2/MinIO, RabbitMQ, Redis, JPA, cliente HTTP do vision-service) e adaptadores de entrada (controllers REST, listeners de fila).
- **photo-api / image-worker**: apenas configuração e montagem (beans, `@SpringBootApplication`).

Regras:

- Entidades JPA ficam na infrastructure e são mapeadas para entidades de domínio. Nunca exponha entidades JPA fora da infrastructure.
- Controllers recebem e devolvem DTOs próprios; nunca entidades de domínio.
- Testes ArchUnit garantem essas regras e devem quebrar o build se forem violadas. Não desative nem contorne esses testes.

## Modelo de domínio

- **Photographer**: conta autenticada; dono dos eventos.
- **Event**: pertence a um fotógrafo; tem `type` (`RACE`, `WEDDING`, `GRADUATION`, `CORPORATE`, `OTHER`), visibilidade (pública ou privada com código de acesso) e prazo de retenção de dados biométricos.
- **Photo**: pertence a um evento. Máquina de estados:

```
PENDING_UPLOAD → UPLOADED → PROCESSING → READY
                      ↘          ↘
                       FAILED ←───┘
```

  Transições inválidas lançam exceção de domínio. A lógica de transição fica na entidade, não nos casos de uso.
- **PhotoVariant**: `ORIGINAL`, `PREVIEW`, `THUMBNAIL`.

## Armazenamento (R2 / MinIO)

```
events/{eventId}/original/{photoId}.{ext}
events/{eventId}/preview/{photoId}.webp
events/{eventId}/thumb/{photoId}.webp
```

- O navegador sobe direto para o storage com URLs pré-assinadas. A photo-api nunca recebe os bytes das fotos.
- Originais são privados e só saem por URL pré-assinada de curta duração. Previews levam marca d'água.
- O bucket precisa de CORS configurado para o domínio do frontend.

## Mensageria (RabbitMQ)

Exchange `photos` do tipo topic. Routing keys:

| Routing key | Publicado por | Consumido por |
|---|---|---|
| `photo.uploaded` | photo-api | image-worker |
| `photo.processed` | image-worker | vision-service |
| `photo.faces.indexed` | vision-service | photo-api |

Regras obrigatórias:

- Cada consumidor tem fila própria, dead letter queue e retry com backoff via TTL.
- Toda mensagem carrega `eventId` (UUID único da mensagem), `correlationId`, `occurredAt` e `schemaVersion`.
- Consumidores são **idempotentes**: a mesma mensagem processada duas vezes não pode gerar efeito duplicado.
- Contratos documentados em `docs/contracts/`. Qualquer mudança de contrato atualiza a documentação e incrementa `schemaVersion`.

## Busca por selfie

- **Indexação é assíncrona** (via fila). **Busca é síncrona** (HTTP), porque o usuário está esperando.
- Fluxo: frontend envia selfie → photo-api valida consentimento e rate limit → chama vision-service → vision-service gera o embedding, busca por similaridade no pgvector filtrando pelo evento e devolve IDs de fotos → photo-api monta a resposta com URLs.
- A comunicação photo-api ↔ vision-service fica na rede interna do Docker e usa API key interna.

## LGPD (obrigatório)

Embeddings faciais são dados pessoais sensíveis.

- Consentimento explícito antes de qualquer busca por selfie; registre o aceite.
- O embedding da selfie é descartado logo após a consulta. Nunca persista a selfie nem seu embedding.
- Embeddings das fotos do evento são excluídos após o prazo de retenção configurado no evento (job agendado).
- Nunca registre em logs imagens, embeddings, tokens ou senhas.

## Autenticação

- Spring Security com JWT próprio.
- Access token de ~15 minutos, assinado com RS256.
- Refresh token com rotação a cada uso, armazenado no Redis (permite revogação e logout real), enviado em cookie `httpOnly`, `Secure`, `SameSite`.
- Senhas com Argon2 (ou BCrypt).
- Papéis: `ADMIN`, `PHOTOGRAPHER`. Participantes não têm conta.
- Eventos privados exigem código de acesso para a galeria.
- Rate limiting no Redis para login e busca por selfie.

## Redis

Usos previstos: refresh tokens, progresso de processamento por evento, cache de galerias, rate limiting. Toda chave tem prefixo por contexto (`auth:`, `progress:`, `cache:`, `ratelimit:`) e TTL definido.

## Frontend

- Upload paralelo com limite de concorrência, retry das falhas e URLs pré-assinadas pedidas em lote.
- Progresso do processamento via Server-Sent Events da photo-api.
- Galeria pública com renderização no servidor e metadados Open Graph.
- Fluxo de busca por selfie com tela de consentimento antes da câmera/upload.

## Observabilidade

- Logs estruturados em JSON em todos os serviços, com `correlationId` e `traceId`.
- OpenTelemetry em Java e Python; o trace de uma foto deve ser visível do upload até a indexação facial.
- Métricas no Prometheus, incluindo tamanho das filas, tempo de processamento por foto e taxa de falha.
- Dashboards do Grafana versionados no repositório.

## Testes

- **domain** e **application**: testes unitários sem Spring, rápidos.
- **infrastructure**: testes de integração com Testcontainers (Postgres, RabbitMQ, Redis, MinIO). Não use H2 nem mocks de broker no lugar dos serviços reais.
- **ArchUnit** para as regras de camadas.
- **vision-service**: pytest; testes de detecção usam imagens de fixture pequenas e sem pessoas reais identificáveis.
- Toda funcionalidade nova vem com testes. Rode os testes antes de considerar uma tarefa concluída.

## Convenções

- Código, nomes, mensagens de commit e de log em **inglês**. Documentação (`README.md`, `docs/`) em **português**.
- Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).
- Commits pequenos e focados.
- Decisões de arquitetura relevantes viram um ADR em `docs/adr/` (formato: contexto, decisão, consequências).
- Configuração via variáveis de ambiente; nada de segredos no repositório. Mantenha `.env.example` atualizado.
- Licença: PolyForm Noncommercial 1.0.0. Nenhum agente deve alterar o LICENSE ou adicionar outra licença sem pedido explícito.

## Comandos

Atualize esta seção conforme os comandos forem criados.

```bash
docker compose up -d                 # infraestrutura local (inclui Grafana :3001, Prometheus :9090, Tempo :3200, Loki :3100)
cd backend && ./mvnw verify          # build + testes Java
cd backend && ./mvnw verify -DskipITs   # rápido: só unitários + ArchUnit (sem Testcontainers)
scripts/run-backend.sh photo-api     # roda a API local com o .env carregado (idem: image-worker)
cd vision-service && pytest          # testes Python
cd frontend && npm run dev           # frontend local
```

## Roteiro

1. **Fundação**: monorepo, Docker Compose (Postgres, RabbitMQ, Redis, MinIO), módulos Maven, ArchUnit, CI no GitHub Actions.
2. **Auth e eventos**: cadastro/login do fotógrafo, CRUD de eventos, início do painel.
3. **Upload**: URLs pré-assinadas, upload paralelo no frontend, galeria pública básica.
4. **Processamento**: image-worker, variantes e marca d'água, progresso via SSE.
5. **Observabilidade**: OpenTelemetry, Grafana, Loki, Prometheus, Tempo.
6. **Reconhecimento facial**: vision-service, indexação com pgvector, busca por selfie com consentimento.

**Fase atual: 6 — Reconhecimento facial (Concluída).** Todas as fases do roteiro inicial foram concluídas com sucesso.

## Como trabalhar neste repositório

- Antes de implementar algo não trivial, apresente um plano curto e aguarde confirmação.
- Siga as regras de camadas mesmo quando um atalho parecer mais rápido.
- Se uma decisão deste arquivo parecer errada para o caso, aponte o problema e proponha alternativa em vez de ignorá-la.
- Ao concluir uma fase, atualize a "Fase atual" e a seção de comandos deste arquivo.
- Agentes NUNCA executam git commit nem git push; isso é responsabilidade exclusiva do desenvolvedor.
