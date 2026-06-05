# Relatório de Verificação de Suporte a Rankings - VALORAE APK v2.0.9

## 1. Visão Geral

Este documento detalha o suporte completo, robusto e retrocompatível implementado na versão **2.0.9** (versionCode `19`) do APK VALORAE para consumir com segurança os rankings da B3 e do Investidor10, entregues pelo proxy atualizado do VALORAE (`https://servidor-valorae.vercel.app/`).

O foco principal do aprimoramento foi garantir resiliência máxima na rede, integrando detecção flexível de envelopes e aliases (sinônimos) para atributos e caminhos, suportando tanto formatos novos (modo "complete" lento enriquecido do Investidor10) quanto legados ou compactos.

---

## 2. Endpoints e Parâmetros

As funções de ranking consomem o endpoint `/api/v1/market/rankings` no Proxy VALORAE.

### Modo Completo (Prioridade #1)
Ao iniciar o fetch, a aplicação envia os seguintes parâmetros recomendados por padrão para obter o conjunto enriquecido do Investidor10:
- `mode=complete`
- `complete=1`
- `strict=1`
- `limit=15`
- `minRows=6`

### Modo Leve / Fallback (Prioridade #2)
Se a chamada completa falhar, o SDK retrocede transparentemente utilizando parâmetros leves:
- `mode=auto`
- `limit=15`
- `minRows=3`

---

## 3. Tempos Limite (Timeouts) Customizados

Foram definidos timeouts específicos ajustados de acordo com a latência esperada de cada modalidade, prevenindo travamentos de interface (ANRs):
- **Modo Completo ao Vivo (Live):** `14000ms` (14 segundos)
- **Modo Completo Fundamentalista (Histórico):** `18000ms` (18 segundos)
- **Modo Leve ao Vivo (Live Fallback):** `9000ms` (9 segundos)
- **Modo Leve Fundamentalista (Histórico Fallback):** `6000ms` (6 segundos)

---

## 4. Estratégia de Fallback Automático e Resiliência

A resiliência é implementada diretamente em `fetchMarketRankings` estruturada em três camadas:
1. **Chamada Completa:** Tenta obter os dados do modo completo com timeout configurado.
2. **Fallback por Erro de Rede ou Timeout:** Caso ocorra exceção (5xx, timeout ou falha de conectividade), realiza uma segunda chamada usando parâmetros de modo leve.
3. **Fallback por Resposta Vazia:** Se a primeira chamada retornar um snapshot válido sintaticamente mas vazio (sem itens de alta/baixa), executa automaticamente uma chamada no formato leve.

---

## 5. Mapeamento Flexível (Aliases e Envelopes)

### Envelopes de Objeto Raiz Suportados
Os rankings podem vir embrulhados em qualquer uma das estruturas de campo a seguir na raiz do JSON de resposta:
- `data.rankings`
- `payload.rankings`
- `result.rankings`
- `results.rankings`
- `marketRankings`
- Ou diretamente no root da resposta (`rankings.altas`, `rankings.baixas`, etc.)

### Aliases de Listas de Ranking (Altas/Baixas)
A normalização interna no modelo `MarketRankingSnapshot` mapeia dinamicamente os nomes dos arrays:
- **Maiores Altas (Highs):** `altas`, `highs`, `gainers`, `maioresAltas`, `topGainers`, `up`, `alta`
- **Maiores Baixas (Lows):** `baixas`, `lows`, `losers`, `maioresBaixas`, `topLosers`, `down`, `baixa`

### Aliases de Atributos de Itens Individuais
Dada a grande variedade de campos retornados por fontes externas (B3 tradicional, statusinvest, Investidor10), o método `marketRankingItemFromObject` normaliza dezenas de aliases e propriedades opcionais:

| Propriedade Interna | Tipo no Kotlin | Aliases Mapeados no JSON |
| :--- | :--- | :--- |
| **ticker** | `String` | `ticker`, `codigo`, `symbol`, `ativo` |
| **name** | `String` | `name`, `nome`, `company`, `companyName` |
| **price** | `Double` | `price`, `lastPrice`, `currentPrice`, `cotacao`, `preco`, etc. |
| **changePercent**| `Double` | `changePercent`, `variationPercent`, `percentual`, `percent`, `variacao`, `change` |
| **volume** | `Double` | `volume`, `vol`, `financialVolume` |
| **setor** | `String` | `setor`, `sector`, `industrialSector` |
| **segmento** | `String` | `segmento`, `segment`, `subSector` |
| **url** | `String` | `url`, `link`, `sourceUrl` |
| **source** | `String` | `source` (Fallback: "Serviço de dados VALORAE") |

---

## 6. Interface de Usuário Moderna (Material Design 3 Cards)

A visualização de rankings dentro do `ChartsScreen.kt` foi reestruturada como um **Card Material Design 3** limpo e moderno (`RankingCompactRow`):
- **Aparência Elevada:** Renderizado sob um container com cantos arredondados (`RoundedCornerShape(12.dp)`), fundo contrastante (`DarkSurfaceElevated`) e uma borda tonal sutil.
- **Campos Completos:**
  - Código/Ticker ao lado do Ranking Rank (ex: `#1 VALE3`)
  - Nome completo da empresa imediatamente abaixo do ticker.
  - Variação percentual envolta em um distintivo tonal colorido (verde escuro para alta, vermelho escuro para baixa) de forte destaque visual.
  - Preço formatado em reais correspondente (R$) posicionado abaixo da variação.
  - Divisor interno separando o rodapé informando a Categorização do Ranking (`Tipo: Alta/Baixa`) e a Fonte oficial de dados (`Fonte: Investidor10`), conferindo veracidade e clareza.

---

## 7. Validação e Testes Unitários

Para garantir integridade eterna e evitar regressões, os seguintes testes unitários em `B3NetworkServiceParserTest.kt` validam esta interoperabilidade de dados:
- **`testRankingsV211259AliasesAndCompleteFields`**: Valida a decodificação dos campos vindos de `investidor10-live-complete` (campos como `nome`, `preco`, `variacao` mapeados para `highs` e `lows`).
- **`testRankingsPayloadEnvelopeAndAlternativeFieldNames`**: Valida a resposta encapsulada em `payload.rankings` lendo campos de atributos específicos como `codigo`, `companyName`, `percentual`, `volume`, `setor`, `segmento`, `url` e `source`.

Ambos os cenários são compilados e verificados com sucesso nas execuções de testes unitários locais.

---
*Relatório emitido pela Equipe de Arquitetura e Engenharia Android VALORAE.*
