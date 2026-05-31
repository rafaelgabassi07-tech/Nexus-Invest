# VALORAE APK — Auditoria do Valorae Proxy e Implementação de Rankings

## Base auditada

- APK base: `valorae-apk-insights-app-auditoria-completa-corrigido.zip`
- Proxy auditado: `valorae-proxy-v21.12.52-news-reliability-upgrade.zip`
- URL pública esperada: `https://servidor-valorae.vercel.app`
- Regra preservada: o APK continua consumindo o Proxy; não foi adicionado scraping direto nem serviço pago.

## O que o Proxy oferece além do que o APK já usava

A auditoria encontrou que o APK já consumia as rotas essenciais:

- `/api/v1/ready`
- `/api/v1/assets`
- `/api/v1/asset`
- `/api/v1/asset/history`
- `/api/v1/asset/dividends`
- `/api/v1/news`
- `/api/v1/market/indices`
- `/api/v1/market/ipca`
- `/api/v1/portfolio/analyze`
- `/api/v1/portfolio/history`
- `/api/v1/portfolio/next-dividends`
- `/api/v1/source/status`
- `/api/server/metrics`

Mas o Proxy também oferece blocos que ainda não estavam expostos na carteira:

### Mercado e comparação

- `/api/v1/market/rankings`
  - maiores altas/baixas via ranking controlado do Proxy;
  - ranking por score;
  - ranking por dividend yield;
  - ranking por P/VP/P/L;
  - perfis: dividendos, conservador, crescimento, valor e renda FII.
- `/api/v1/compare`
  - comparação de ativos por múltiplos critérios.

### Inteligência de carteira

O APK já chamava `/api/v1/portfolio/analyze`, mas ainda não expunha partes relevantes do payload:

- `intelligence.actionPlan`
- `intelligence.positionRanking`
- `intelligence.incomeCoverage`
- `intelligence.incomeStabilityScore`
- `intelligence.technologyReadiness`
- `intelligence.dataCompleteness`
- `rebalance.actions`
- `portfolioScore`
- `risk`
- `allocation`

### Rotas especializadas de carteira

Ainda disponíveis para próximas telas:

- `/api/v1/portfolio/summary`
- `/api/v1/portfolio/allocation`
- `/api/v1/portfolio/risk`
- `/api/v1/portfolio/income`
- `/api/v1/portfolio/events`
- `/api/v1/portfolio/dividends`
- `/api/v1/portfolio/rebalance`
- `/api/v1/portfolio/transactions`

### Rotas especializadas de ativo

Ainda disponíveis para enriquecer tela de detalhes e diagnóstico por ticker:

- `/api/v1/asset/coverage`
- `/api/v1/asset/quality`
- `/api/v1/asset/action-plan`
- `/api/v1/asset/indicators`
- `/api/v1/asset/fundamentals`
- `/api/v1/asset/valuation`
- `/api/v1/asset/profitability`
- `/api/v1/asset/debt`
- `/api/v1/asset/statements`
- `/api/v1/asset/peers`
- `/api/v1/asset/source-map`
- `/api/v1/asset/next-dividend`

### Rotas especializadas de FII

Disponíveis para uma futura aba específica de FIIs:

- `/api/v1/fii/profile`
- `/api/v1/fii/indicators`
- `/api/v1/fii/income`
- `/api/v1/fii/patrimonial`
- `/api/v1/fii/portfolio`
- `/api/v1/fii/vacancy`
- `/api/v1/fii/communications`
- `/api/v1/fii/checklist`

## Implementado nesta rodada

### 1. Modelos novos no APK

Adicionados ao `B3NetworkService.kt`:

- `MarketRankingItem`
- `MarketRankingSnapshot`
- `PortfolioProxyActionPlanItem`
- `PortfolioPositionRankingItem`
- `PortfolioRebalanceAction`

`PortfolioProxyAnalysis` foi ampliado para carregar:

- score de saúde;
- estabilidade de renda;
- prontidão tecnológica;
- cobertura de pagadores de renda;
- plano de ação;
- ranking interno de posições;
- ações de rebalanceamento.

### 2. Novas chamadas ao Proxy

Adicionadas funções:

- `fetchMarketRankings(...)`
- `fetchPortfolioRankings(...)`
- `fetchLiveStockRankings()`

Endpoint integrado:

```text
/api/v1/market/rankings
```

A carteira usa ranking personalizado por tickers atuais. O ranking ao vivo de mercado é usado como complemento, sem substituir dados de carteira.

### 3. ViewModel integrado

`PortfolioAnalyticsState` agora guarda:

- `portfolioRanking`
- `liveMarketRanking`

`refreshPortfolioAnalytics()` passa a carregar em paralelo:

- análise de carteira;
- histórico;
- IPCA;
- agenda de proventos;
- ranking da carteira;
- ranking ao vivo de mercado.

### 4. Página Insights ampliada

Adicionada nova subpágina:

```text
Insights > Rankings do Proxy
```

Ela mostra:

- ranking da carteira por score;
- ranking da carteira por dividend yield;
- ranking da carteira por perfil conservador;
- ranking de renda FII quando disponível;
- maiores altas do mercado;
- maiores baixas do mercado;
- plano de ação da inteligência do Proxy;
- ranking interno de posições.

### 5. Regra de existência da carteira preservada

Rankings são dados atuais/fundamentalistas. Eles **não** foram conectados aos cálculos de proventos passados, IPCA ou histórico patrimonial.

As regras já corrigidas continuam preservadas:

- dividendos só entram se havia posição elegível;
- histórico remoto é limitado ao início real da carteira;
- IPCA é rebaseado para o período da carteira;
- agenda futura não reaproveita eventos antigos;
- fallback de proventos soma mês a mês pela quantidade existente.

## Arquivos alterados

- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `scripts/verify_valorae_proxy_capabilities.py`
- `docs/RELATORIO_AUDITORIA_PROXY_CAPABILITIES_RANKINGS.md`

## Validação executada

```bash
python3 scripts/verify_valorae_proxy_capabilities.py
python3 scripts/verify_valorae_proxy_integration.py
python3 scripts/verify_valorae_insights_logic.py
```

Resultado:

```text
Valorae Proxy capabilities audit OK
Valorae Proxy integration audit OK
Valorae Insights logic audit OK
```

## Gradle

Tentativa executada no sandbox:

```bash
./gradlew :app:compileDebugKotlin --stacktrace --info
```

Resultado: falha externa de ambiente ao baixar a distribuição Gradle:

```text
java.net.UnknownHostException: services.gradle.org
```

Isso é o mesmo bloqueio de DNS/ambiente já observado antes. Rodar no Android Studio com Gradle disponível.

## Comandos recomendados no Android Studio

```bash
./gradlew clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
./gradlew :app:lintDebug :app:check --stacktrace --info
```

## Próximas integrações recomendadas

A próxima rodada de valor alto seria:

1. Tela `Qualidade do Ativo` usando `/api/v1/asset/quality`, `/coverage` e `/action-plan`.
2. Aba `Comparar Ativos` usando `/api/v1/compare`.
3. Aba específica de FIIs usando `/api/v1/fii/*`.
4. Tela `Rebalanceamento` usando `/api/v1/portfolio/rebalance` com metas por classe/ticker.
5. Diagnóstico por fonte/campo usando `/api/v1/asset/source-map`.

Nenhuma dessas pendências impede a carteira atual de funcionar; são ampliações de produto.
