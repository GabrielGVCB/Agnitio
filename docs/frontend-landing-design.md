# Especificação de Design — Landing Page (Camera HUD / Visor)

Este documento registra as diretrizes de design e regras de negócio aprovadas para a página inicial (landing page) do Agnitio.

---

## 1. Conceito

A página inteira imita o visor de uma câmera profissional (HUD — *Heads-Up Display*), em tons de azul escuro e ciano técnico.
- O topo é a tela do visor (viewport de captura).
- Rolando para baixo, cada seção tem uma animação conceitualmente ligada ao tema de fotografia (rolo de filme, foco facial, diafragma/obturador).
- Stack: Next.js (App Router), TypeScript, Tailwind CSS e componentes reutilizáveis.

---

## 2. Tokens de Design

| Token | Valor / Aplicação |
|---|---|
| **Fundo geral** | `#050d1c` (azul escuro profundo) |
| **Painéis / Cards** | `#0b1a33` e `#081530` |
| **Faixa clara / Destaque** | `#e8f4ff` |
| **Cor do HUD (destaque)** | `#5ec8ff` (variável CSS `--hud`) |
| **Texto principal** | `#e8f4ff` |
| **Texto secundário** | `#b9d3ee` |
| **Indicador REC / Alerta** | `#ff4040` |
| **Bordas / Cantos** | `rounded-[4px]` em botões; `rounded-[6px]` em cartões |
| **Tipografia (Google Fonts)** | **Chakra Petch**: títulos, dados técnicos e elementos HUD<br>**Barlow**: texto corrido e corpo de página |

---

## 3. Seções da Página (de cima para baixo)

### 3.1. Cabeçalho
- Logo `AGNITIO` com estética técnica.
- Links de navegação: *Eventos*, *Como funciona*, *Privacidade*, *Para fotógrafos*.
- Botão de ação: *Entrar* (leva ao login do fotógrafo).

### 3.2. Visor (Hero — ~820px de altura)
Moldura arredondada com overlays de visor de câmera:
- **Topo do visor**:
  - Ponto indicador `REC` vermelho piscando + cronômetro em contagem (`00:00:00`).
  - Badges técnicos: `16:9`, `4K`, `60 FPS`.
  - Barras de nível de áudio animadas e ícone de bateria.
- **Área central**:
  - Linhas guias da regra dos terços (grid 3x3 sutil).
  - Círculos de bokeh desfocados em movimento suave no fundo.
  - 4 cantos de foco (*focus brackets*) que "caçam" foco ao carregar e travam na cor `--hud`, exibindo o indicador `AF-C FOCO TRAVADO`.
  - Título de impacto: *"Você correu. A gente clicou."*
  - Campo de busca: input com *"Nome do evento ou cidade"* + botão *"Tirar selfie e buscar"*.
  - **Consentimento obrigatório (LGPD)**: checkbox com a frase:
    > *"Autorizo o uso da minha selfie só para esta busca. Ela é apagada logo depois."* (com link para o aviso de privacidade).
  - Ao disparar a busca: efeito de flash branco na tela inteira antes de inicializar o modal/câmera.
- **Rodapé do visor**:
  - Informações técnicas: `CARD 128GB`, `AF-C`, `±0.0`.
  - Régua de exposição interativa `3..2..1..0..1..2..3` com ponteiro oscilante.
  - Metadados de câmera: `WB AUTO`, `ISO AUTO`, `F2.8`, `1/2000s`.

### 3.3. "Eventos com fotos no ar"
- Carrossel infinito horizontal de cartões com estética de **rolo de filme** fotográfico (perforations laterais).
- Rolagem automática contínua que pausa no hover do mouse.
- Mostra nome do evento, data, cidade, total de fotos e botão para acessar a galeria do evento.

### 3.4. "Três passos até a sua foto"
Apresentação em cards com microanimações ativadas por rolagem:
1. **Escolha o evento**: encontre a corrida ou ocasião pelo nome ou data.
2. **Tire uma selfie**: biometria facial instantânea com prova de vida.
3. **Baixe em alta resolução**: fotos identificadas prontas para download ou compra.

### 3.5. "Só você vê as suas fotos" (Privacidade & LGPD)
- Mockup ilustrativo de celular com rosto escaneado: quadro de detecção pulsando e feixe de leitura (*scanning line*) percorrendo a face.
- Explicação clara sobre prova de vida (anti-spoofing).
- Garantia de que a selfie enviada e seus vetores derivados são descartados imediatamente após a busca (nunca persistidos).
- Aviso de necessidade de autorização de pais/responsáveis para menores de 18 anos.

### 3.6. "Fotógrafo, você clica. O sistema vende."
- Seção em faixa clara (`#e8f4ff` com tipografia escura) criando quebra visual intencional.
- Gráfico de histograma animado simulando fluxo de vendas e visualizações.
- Botão CTA: *"Quero vender minhas fotos"*, direcionando para o cadastro de fotógrafos.

### 3.7. Chamada Final
- Frase de fechamento: *"Sua próxima foto já foi clicada."*
- Gráfico SVG de diafragma/obturador fotográfico grande girando e fechando de acordo com o scroll da página.

### 3.8. Rodapé Estilo HUD
- Status técnico: `STBY`, `ISO 100`, `F2.8`, `BAT 98%`.
- Três colunas de links: *Atletas*, *Fotógrafos*, *Contato e Legal*.
- Aviso de direitos autorais e conformidade com a LGPD.

---

## 4. Regras de Negócio e Conformidade

1. **Sem busca por número de peito**: A plataforma utiliza **exclusivamente busca por Evento + Selfie** (reconhecimento facial). Não há OCR nem número de peito.
2. **Privacidade e LGPD (Dados Biométricos Sensíveis)**:
   - Exigência mandatória de consentimento ativo antes de liberar a captura de câmera/selfie.
   - Prova de vida na captura.
   - Descarte imediato da imagem da selfie e do embedding gerado para a consulta após o retorno da busca.
   - Fluxo de autorização de responsável legal para menores de 18 anos.
3. **Acessibilidade e Performance**:
   - Suporte estrito a `prefers-reduced-motion`: desativar animações de foco, bokeh, flash e transições contínuas quando o usuário preferir menos movimento.
   - Total responsividade: em telas pequenas (mobile), ocultar linhas guias e dados técnicos poluídos do visor, mantendo a busca e o foco centralizados e legíveis.
   - Animações acionadas via CSS scroll-driven animations ou `IntersectionObserver`.
