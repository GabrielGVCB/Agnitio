# Agnitio

**Agnitio** (do latim *"reconhecimento"*) é uma plataforma de fotos de eventos (corridas, casamentos, formaturas, eventos corporativos). O fotógrafo cria um evento e sobe milhares de fotos; o sistema processa as imagens em segundo plano e publica uma galeria. O participante encontra as próprias fotos enviando uma selfie (reconhecimento facial) e, em fase futura, pelo número de peito (OCR).

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
# Iniciar a infraestrutura local (Postgres, RabbitMQ, Redis, MinIO)
docker compose up -d

# Compilar e rodar todos os testes do backend (incluindo ArchUnit e Testcontainers)
cd backend && ./mvnw verify
```

## Documentação Adicional

- [Arquitetura Geral](docs/architecture.md)
- [ADR 0001: Fundação e Clean Architecture](docs/adr/0001-foundation-and-architecture.md)
- [ADR 0002: Armazenamento S3 Local com MinIO e Alternativas](docs/adr/0002-s3-local-storage-minio.md)
- [Contratos de Mensagens Assíncronas](docs/contracts/README.md)

## Licença

Este projeto está sob a [PolyForm Noncommercial License 1.0.0](LICENSE).
O código pode ser estudado, modificado e usado para fins pessoais e
educacionais. Uso comercial não é permitido.
