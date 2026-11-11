# ADR 0001: Arquitetura Base, Monorepo e Fundação Tecnológica

## Contexto
O projeto Agnitio é uma plataforma para processamento, gerenciamento e busca de fotos de eventos (corridas, casamentos, formaturas). O sistema precisa processar grandes volumes de imagens em segundo plano, oferecer busca facial síncrona por selfie (e futuramente OCR de número de peito), manter galerias públicas de alta performance e assegurar conformidade estrita com a LGPD para dados biométricos.

Como projeto de portfólio de alto nível, a clareza da arquitetura, a testabilidade, a isolação de domínios e a reprodutibilidade de infraestrutura são requisitos de primeira classe.

## Decisão

1. **Estrutura de Monorepo**:
   - Organização em monorepo contendo:
     - `backend/`: Maven multi-módulo em Java 25 (LTS) e Spring Boot 4.x.
     - `vision-service/`: Microsserviço Python com FastAPI e bibliotecas de visão computacional.
     - `frontend/`: Aplicação Next.js (TypeScript/Tailwind CSS) contendo painel administrativo e galeria pública SSR.
     - `docs/`: Documentação de arquitetura, ADRs e contratos assíncronos.

2. **Clean Architecture no Backend**:
   - As dependências apontam estritamente para dentro: `infrastructure -> application -> domain`.
   - `domain`: Java puro, sem nenhuma dependência de frameworks (Spring, JPA, Jackson, etc.). Contém entidades, regras de negócio e exceções de domínio.
   - `application`: Casos de uso (um por classe) e interfaces de portas (`ports.input`, `ports.output`). Sem dependências de frameworks.
   - `infrastructure`: Adaptadores técnicos (Spring Data JPA, PostgreSQL, Spring AMQP/RabbitMQ, Redis, AWS SDK S3 para MinIO/R2).
   - `photo-api` e `image-worker`: Módulos de bootstrap Spring Boot responsáveis por montagem e configuração dos contextos de execução.

3. **Governança Arquitetural Automatizada (ArchUnit)**:
   - Adotamos um módulo dedicado `architecture-tests` com ArchUnit executado a cada build para quebrar a compilação caso qualquer regra de camadas, dependência proibida ou vazamento de entidades de infraestrutura ocorra.

4. **Isolamento de Dados e Serviços**:
   - `photo-api` e `vision-service` possuem bancos de dados PostgreSQL completamente separados no ambiente local (`postgres-photo` na porta 5432 e `postgres-vision` na porta 5433). Nenhum serviço acessa as tabelas do outro diretamente.

## Consequências

- **Positivas**:
  - Núcleo de regras de negócio desacoplado de infraestrutura e frameworks, facilitando evolução e manutenção.
  - Testes de domínio e aplicação ultrarrápidos sem necessidade de contexto Spring.
  - Impossibilidade de vazamento de camadas garantida por testes de arquitetura automáticos no CI.
  - Separação clara de responsabilidades entre microsserviços e seus respectivos bancos de dados.

- **Mitigações**:
  - Exige mapeamento explícito entre modelos de persistência JPA, entidades de domínio e DTOs de API, mitigado pela previsibilidade e segurança de dados sensíveis.
