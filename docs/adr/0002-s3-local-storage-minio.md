# ADR 0002: Armazenamento de Objetos S3 para Desenvolvimento Local (MinIO) e Alternativas

## Contexto
Em ambiente de produção, o Agnitio utiliza o Cloudflare R2 para armazenamento de fotos originais, previews com marca d'água e miniaturas. A comunicação com o storage é feita integralmente através da API padrão S3 (AWS SDK v2).

Para desenvolvimento local e execução de testes em ambiente conteinerizado (Docker Compose / Testcontainers), é indispensável um servidor de armazenamento de objetos compatível com a API S3, de modo que a aplicação não dependa de credenciais de nuvem externas ou conectividade com a internet.

Historicamente, o **MinIO** foi o padrão de facto para esse propósito. Contudo, a organização MinIO alterou sua política de distribuição: após a transição para AGPLv3, removeu seus repositórios comunitários públicos (`minio/minio`, `minio/mc`) do Docker Hub e restringiu imagens históricas no Quay.io sob autenticação fechada (`401 UNAUTHORIZED`), direcionando o projeto para modelos comerciais corporativos.

## Decisão

1. **Uso de Imagem Comunitária Acessível (`bitnamilegacy/minio:latest`)**:
   - Para garantir que qualquer desenvolvedor consiga rodar `docker compose up -d` sem falhas de autenticação (`pull access denied`), adotamos a distribuição comunitária mantida no Docker Hub (`bitnamilegacy/minio:latest`), que agrupa o servidor MinIO e o utilitário de linha de comando `mc`.
   - O container inclui healthcheck nativo na porta `9000` (`/minio/health/live`).
   - Um serviço auxiliar (`minio-init`) aguarda o MinIO ficar saudável, cria o bucket padrão `agnitio-photos` de forma idempotente e configura políticas públicas de download/CORS para a galeria.

2. **Isolamento de Acoplamento**:
   - O código do backend (módulo `infrastructure`) comunica-se exclusivamente através da API padronizada AWS S3 (via endpoint configurável: URL do MinIO local ou URL do Cloudflare R2 em produção).
   - Nenhuma biblioteca ou SDK proprietário do MinIO é importado no código-fonte da aplicação.

3. **Alternativas Avaliadas**:
   - **SeaweedFS**:
     - *Prós*: Open Source (Apache 2.0), altamente performático, suporte nativo robusto à API S3, leve e excelente para escalabilidade horizontal em cluster.
     - *Contras*: Configuração local de buckets, volumes e mestres ligeiramente mais complexa para desenvolvedores novatos em comparação com um único comando MinIO.
     - *Status*: Candidato principal para substituição caso surja qualquer incompatibilidade futura com o AWS SDK Java v2.
   - **Garage (GarageHQ)**:
     - *Prós*: Open Source (AGPLv3), escrito em Rust, projetado especificamente para armazenamento distribuído leve e auto-hospedado com compatibilidade S3.
     - *Contras*: Ecossistema mais jovem e menos difundido em setups locais corporativos tradicionais de testes.
     - *Status*: Monitorado como alternativa viável para ambientes descentralizados.

## Consequências

- **Positivas**:
  - Paridade funcional total com Cloudflare R2 para uploads com URLs pré-assinadas e download de variantes no fluxo local.
  - Setup reproduzível e sem custos ou bloqueios de autenticação para os desenvolvedores.
  - Testes rápidos e confiáveis sem dependência de internet.

- **Mitigações**:
  - Como a imagem comunitária congela uma versão estável de desenvolvimento, monitoramos a compatibilidade contínua do AWS SDK Java v2. Caso surjam requisitos mais modernos, a migração para o SeaweedFS poderá ser realizada sem qualquer impacto no código do domínio ou de casos de uso da aplicação.
