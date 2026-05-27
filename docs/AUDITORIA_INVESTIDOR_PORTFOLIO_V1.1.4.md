# Auditoria profunda do VALORAE Investidor/Portfolio v1.1.4

Esta revisão foi feita especificamente no projeto Android/Kotlin `Investidor-portifolio (2)-2`, com foco em garantir que as telas críticas recebam dados do Valorae Proxy oficial.

## Proxy configurado

- `VALORAE_PROXY_BASE_URL=https://servidor-valorae.vercel.app`
- `VALORAE_PROXY_CLIENT_ID=valorae-investidor-android`
- `VALORAE_DIRECT_FALLBACK_ENABLED=false`

## Telas revisadas

### Análise

A tela de análise de ativo agora usa dados retornados por `B3NetworkService.fetchAssetData` e histórico por `fetchHistoricalChart`. Foram adicionados gráficos derivados dos dados reais recebidos pelo Proxy, incluindo score visual e barras de indicadores como DY, ROE, ROIC, margem e payout quando disponíveis.

### Detalhes do ativo

A tela/modal de detalhes do ativo recebeu diagnóstico de completude dos campos recebidos pelo Proxy e reforço para não apresentar campos simulados como se fossem reais. O tratamento de FII foi ajustado para aceitar `FII`, `fii` e variações de caixa.

### Agenda de dividendos

A agenda agora prioriza `B3NetworkService.fetchNextDividends`, que consome `/api/portfolio/next-dividends` e entende o formato real do Proxy com `nextDividend`, `lastDividend`, `dataCom`, `dataPagamento`, `valor`, `valorPorCota` e estimativa por quantidade em carteira.

### Equilíbrio da carteira

A alocação por classe e setor agora lê os arrays reais do Proxy em `/api/portfolio/analyze`, especialmente `allocation.byType`, `allocation.bySector`, `key`, `percent` e `value`. A UI usa estes dados antes do cálculo local.

### Rentabilidade vs IPCA+

A tela agora prioriza `/api/market/ipca`, usando o parâmetro `last` e acumulando a série mensal real do Banco Central via Proxy. Se o Proxy não entregar IPCA, a tela usa uma estimativa local marcada como estimativa.

### Evolução de patrimônio

A evolução passa a consumir `/api/portfolio/history`, aceitando `series`, `totalValue`, `investedValue`, `unrealizedPnLPct` e variações compatíveis. Se o endpoint estiver indisponível, usa reconstrução local baseada nas transações.

## Endpoints integrados

- `GET /api/asset`
- `POST /api/assets`
- `GET /api/asset/history`
- `GET /api/news`
- `GET /api/market/indices`
- `GET /api/market/ipca`
- `POST /api/portfolio/analyze`
- `POST /api/portfolio/history`
- `POST/GET /api/portfolio/next-dividends`

## Correções técnicas principais

- Normalização de ranges: `1mo -> 1M`, `3mo -> 3M`, `6mo -> 6M`, `1y -> 1Y`, `5y -> 5Y`, `max/tudo -> MAX`.
- Payload de posições enriquecido com `investedValue`, `currentValue`, `averagePrice`, `currentPrice`, `type` e `quantity`.
- Parser de carteira ajustado ao contrato real do Proxy: `portfolioScore`, `risk`, `income`, `allocation`, `diagnostics`, `insights`.
- Parser de dividendos ajustado para `nextDividend` e `lastDividend`.
- Parser de histórico ajustado para `series` e `unrealizedPnLPct`.
- Auditoria automatizada ampliada em `scripts/verify_valorae_proxy_integration.py`.
- APK antigo removido do pacote para evitar instalação de build desatualizada.

## Validação local possível neste pacote

O projeto enviado não contém `gradlew`, então esta auditoria valida estrutura, integração, presença de endpoints, placeholders e parse Kotlin assistido com `kotlinc` em subconjuntos. A geração do APK final deve ser feita no Android Studio/AI Studio com o Gradle do projeto.
