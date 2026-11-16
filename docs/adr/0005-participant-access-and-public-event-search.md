# ADR 0005: Acesso do Participante sem Conta e Busca Pública de Eventos

## Contexto
A plataforma tem dois públicos com objetivos diferentes:

- **Fotógrafo**: cria eventos, sobe fotos e acompanha o processamento. Precisa de conta.
- **Participante**: só quer encontrar as próprias fotos de um evento em que esteve.

A primeira versão da interface misturava os dois. O botão "Criar Conta" aparecia para todos e sempre criava um fotógrafo, então quem só queria ver uma foto acabava virando fotógrafo. Além disso, a busca da página inicial abria a câmera direto, sem saber em qual evento procurar: ela tentava descobrir o evento pela listagem `GET /api/v1/events`, que é **restrita aos eventos do fotógrafo logado**. Para um visitante anônimo a lista vinha vazia e a busca falhava com "ID do evento não especificado". Não existia nenhuma forma pública de encontrar um evento.

## Decisão

1. **Participantes não têm conta.** Mantém a regra já prevista no `AGENTS.md`: menos atrito e menos dados pessoais armazenados (LGPD). O cadastro é exclusivo de fotógrafos, e a interface passa a deixar isso explícito ("Área do fotógrafo", "Crie sua conta de fotógrafo").

2. **Fluxo do participante em duas etapas:**
   1. **Encontrar o evento**: busca por nome ou cidade na página inicial.
   2. **Encontrar as fotos**: na galeria do evento (`/events/{id}`), envia a selfie com consentimento explícito.

   A busca por selfie só existe dentro da página de um evento, onde o identificador do evento é conhecido. A busca facial continua sempre filtrada por evento (ADR 0004).

3. **Endpoint público de busca de eventos**: `GET /api/v1/events/public?query=&page=&size=`, sem autenticação.
   - Retorna apenas eventos **`PUBLIC` e `ACTIVE`** (regra no domínio: `Event.isPubliclyListed()`).
   - Busca por nome ou cidade, sem diferenciar maiúsculas de minúsculas; os curingas do `LIKE` digitados pelo usuário são escapados.
   - Ordena por data do evento (mais recente primeiro, sem data por último) e depois por data de criação.
   - Paginação com `hasNext` em vez de total, evitando uma consulta `COUNT` extra; no máximo 20 itens por página.
   - Expõe somente `id`, `name`, `type`, `eventDate` e `city`. Nunca o ID do fotógrafo, o código de acesso ou a política de retenção.
   - Rate limit no Redis (`ratelimit:event-search:{ip}`, 60 requisições por minuto).

4. **Eventos privados não aparecem na busca.** São acessados apenas pelo link compartilhado pelo fotógrafo, com o código de acesso. Isso protege casamentos e eventos corporativos.

5. **Data e cidade no evento** (migration `V4`): campos opcionais para não invalidar eventos já existentes. Eles ajudam o participante a diferenciar eventos com nomes parecidos. Índice `idx_events_public_listing` apoia o filtro e a ordenação da busca.

## Consequências

### Positivas
- Quem só quer ver fotos não cria conta nem vira fotógrafo por engano.
- A busca da página inicial funciona para visitantes anônimos.
- Eventos privados ficam fora de qualquer listagem pública.
- O endpoint público expõe o mínimo de dados necessário.

### Negativas / trade-offs
- A busca usa `LIKE '%termo%'`, que não aproveita índice B-tree para o texto. É suficiente para o volume atual; se crescer, a evolução natural é `pg_trgm` (índice GIN) ou busca full-text.
- Sem conta, o participante não consegue salvar eventos ou fotos favoritas. Se isso virar requisito, será um papel novo (`PARTICIPANT`) e um novo ADR.
- O rate limit por IP pode afetar várias pessoas atrás do mesmo NAT (por exemplo, o Wi-Fi de um evento); o limite foi definido com folga por esse motivo.
