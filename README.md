# Agnitio

**Agnitio** (do latim *"reconhecimento"*) é uma plataforma de fotos de eventos (corridas, casamentos, formaturas, eventos corporativos). O fotógrafo cria um evento e sobe milhares de fotos; o sistema processa as imagens em segundo plano e publica uma galeria. O participante encontra as próprias fotos exclusivamente enviando uma selfie (reconhecimento facial ético com prova de vida e conformidade LGPD). Não existe busca por número de peito.

## Stack Tecnológica

- **API REST & Worker**: Java 25 (LTS), Spring Boot 4.x, Maven multi-módulo (Clean Architecture).
- **Visão Computacional**: Python 3.12+, FastAPI, InsightFace, pgvector.
- **Frontend**: Next.js, TypeScript, Tailwind CSS.
- **Bancos de Dados**: PostgreSQL 17 + Flyway (`photo-api`); PostgreSQL 17 + pgvector (`vision-service`).
- **Mensageria**: RabbitMQ 4.x (Exchange topic `photos`).
- **Cache & Rate Limiting**: Redis 8.x.
- **Armazenamento de Objetos**: Cloudflare R2 (produção) / MinIO (desenvolvimento local via S3 API).
- **Testes & Governança**: JUnit 5, AssertJ, Mockito, Testcontainers, ArchUnit.

## Estrutura do Monorepo

```
agnitio/
├── AGENTS.md                 # Regras e convenções para agentes de IA
├── README.md
├── docker-compose.yml        # Infraestrutura local completa
├── docs/                     # Arquitetura, ADRs e contratos assíncronos
├── backend/                  # Maven multi-módulo (Java 25 + Spring Boot 4.x)
│   ├── domain/               # Entidades de negócio puras (sem frameworks)
│   ├── application/          # Casos de uso e portas
│   ├── infrastructure/       # Adaptadores JPA, RabbitMQ, Redis, S3
│   ├── photo-api/            # Bootstrap da API REST
│   ├── image-worker/         # Bootstrap do Worker de processamento
│   └── architecture-tests/   # Validações arquiteturais estritas com ArchUnit
├── vision-service/           # Serviço Python de visão computacional
└── frontend/                 # Aplicação Next.js
```

## Comandos Rápidos

```bash
# Iniciar a infraestrutura local (Postgres, RabbitMQ, Redis, MinIO, Prometheus, Tempo, Loki, Grafana, Vision)
docker compose up -d

# Compilar e rodar todos os testes do backend (incluindo ArchUnit e Testcontainers)
cd backend && ./mvnw verify

# Rodar a API e o worker localmente (carrega o .env da raiz antes do spring-boot:run)
scripts/run-backend.sh photo-api
scripts/run-backend.sh image-worker

# Rodar testes do serviço de visão computacional
cd vision-service && pytest

# Rodar o frontend localmente
cd frontend && npm run dev
```

### Portais e Ferramentas Locais

- **Frontend**: [http://localhost:3000](http://localhost:3000)
- **API REST (photo-api)**: [http://localhost:8080](http://localhost:8080)
- **Serviço de Visão (vision-service)**: [http://localhost:8001](http://localhost:8001)
- **Grafana (Dashboards, Logs & Traces)**: [http://localhost:3001](http://localhost:3001) (admin/admin)
- **Prometheus (Métricas)**: [http://localhost:9090](http://localhost:9090)
- **MinIO Console (S3 Storage)**: [http://localhost:9001](http://localhost:9001) (minio_admin/minio_secret_key)
- **RabbitMQ Management**: [http://localhost:15673](http://localhost:15673) (agnitio_rabbit/rabbit_secret)

## Documentação Adicional

- [Arquitetura Geral](docs/architecture.md)
- [ADR 0001: Fundação e Clean Architecture](docs/adr/0001-foundation-and-architecture.md)
- [ADR 0002: Armazenamento S3 Local com MinIO e Alternativas](docs/adr/0002-s3-local-storage-minio.md)
- [ADR 0003: Arquitetura de Observabilidade Distribuída](docs/adr/0003-observability-stack.md)
- [ADR 0004: Reconhecimento Facial com pgvector e Conformidade LGPD](docs/adr/0004-facial-recognition-and-pgvector.md)
- [Contratos de Mensagens Assíncronas](docs/contracts/README.md)

## Licença

Este projeto está sob a [PolyForm Noncommercial License 1.0.0](LICENSE).
O código pode ser estudado, modificado e usado para fins pessoais e
educacionais. Uso comercial não é permitido.
