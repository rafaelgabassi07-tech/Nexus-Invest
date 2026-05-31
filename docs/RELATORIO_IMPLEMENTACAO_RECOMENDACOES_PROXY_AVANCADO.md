# RELATÓRIO — Implementação das recomendações avançadas do VALORAE Proxy no APK

## Objetivo

Implementar no APK VALORAE os principais recursos que o VALORAE Proxy já oferecia e que ainda não estavam expostos de forma completa na carteira de investimentos.

A implementação preserva as regras do projeto:

- sem Firebase pago;
- sem Redis, KV, WebSocket pago ou banco externo pago;
- sem scraping direto no APK;
- Proxy continua sendo backend central;
- chamadas usando HTTPS e contratos `/api/v1/...`;
- dados técnicos ficam concentrados em tela própria, sem poluir Home/Insights.

## Nova área adicionada

Foi criada a página **Proxy+** na barra inferior do aplicativo.

Essa página centraliza módulos avançados do Proxy:

1. Raio-X do ativo;
2. análise fundamentalista avançada;
3. central avançada de FIIs;
4. rebalanceamento, risco e renda da carteira;
5. Radar / Watchlist;
6. diagnóstico avançado do Proxy.

## Endpoints novos integrados

### Raio-X do ativo

- `/api/v1/asset/quality`
- `/api/v1/asset/coverage`
- `/api/v1/asset/action-plan`
- `/api/v1/asset/source-map`

### Análise fundamentalista avançada

- `/api/v1/asset/profile`
- `/api/v1/asset/fundamentals`
- `/api/v1/asset/valuation`
- `/api/v1/asset/profitability`
- `/api/v1/asset/debt`
- `/api/v1/asset/statements`
- `/api/v1/asset/peers`
- `/api/v1/asset/indicators`
- `/api/v1/asset/next-dividend`

### Central avançada de FIIs

- `/api/v1/fii/profile`
- `/api/v1/fii/income`
- `/api/v1/fii/patrimonial`
- `/api/v1/fii/portfolio`
- `/api/v1/fii/vacancy`
- `/api/v1/fii/communications`
- `/api/v1/fii/checklist`
- `/api/v1/fii/indicators`

### Carteira avançada

- `/api/v1/portfolio/rebalance`
- `/api/v1/portfolio/allocation`
- `/api/v1/portfolio/risk`
- `/api/v1/portfolio/income`
- `/api/v1/portfolio/dividends`
- `/api/v1/portfolio/events`
- `/api/v1/portfolio/summary`
- `/api/v1/portfolio/transactions`

### Radar / Watchlist

- `/api/v1/watchlist/analyze`

### Diagnóstico avançado

- `/api/v1/engine/maturity`
- `/api/v1/engine/performance`
- `/api/v1/cache/stats`
- `/api/v1/deploy/status`
- `/api/v1/health`
- `/api/v1/personal/readiness`
- `/api/v1/schema`

## Arquivos alterados

- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/ui/screens/ProxyToolsScreen.kt`
- `app/src/main/java/com/example/MainActivity.kt`
- `scripts/verify_valorae_proxy_recommendations.py`

## O que foi adicionado na camada de rede

Novos modelos genéricos:

- `ProxyCapabilityRow`
- `ProxyCapabilitySection`
- `AssetProxyCapabilities`
- `PortfolioProxyCapabilities`

Novos métodos públicos:

- `fetchAssetProxyCapabilities(ticker, isFiiHint)`
- `fetchPortfolioProxyCapabilities(positions, watchlistTickers)`

Esses métodos não assumem um formato único de payload. Eles aceitam respostas em objetos, arrays, `summary`, `quality`, `coverage`, `actions`, `events`, `items`, `warnings` e variações comuns do Proxy.

## O que foi adicionado no ViewModel

Novo estado:

- `ProxyCapabilitiesUiState`

Novo fluxo:

- `proxyCapabilities`

Nova ação:

- `refreshProxyCapabilities(ticker, force)`

A consulta usa:

- ticker selecionado;
- ticker pesquisado;
- primeiro ativo da carteira, quando aplicável;
- posições atuais da carteira com quantidade, preço médio, tipo, preço atual, total investido e primeira compra.

## Tela Proxy+

A tela permite:

- escolher um ticker para análise;
- consultar o Proxy;
- abrir o ativo na tela de análise;
- visualizar cards por módulo;
- expandir seções com muitos campos;
- ver endpoint usado em cada bloco;
- ver status, score, avisos e campos principais retornados pelo Proxy.

## Validações executadas

Foram executadas validações estáticas:

```bash
python3 scripts/verify_valorae_proxy_recommendations.py
python3 scripts/verify_valorae_proxy_capabilities.py
python3 scripts/verify_valorae_proxy_integration.py
python3 scripts/verify_valorae_insights_logic.py
```

Resultado esperado:

```text
Valorae Proxy recommendations implementation audit OK
Valorae Proxy capabilities audit OK
Valorae Proxy integration audit OK
Valorae Insights logic audit OK
```

## Limitação do ambiente

O Gradle não foi executado neste sandbox porque o wrapper voltou a falhar ao baixar a distribuição em `services.gradle.org` por `UnknownHostException`.

Validar no Android Studio com:

```bash
./gradlew clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
./gradlew :app:lintDebug :app:check --stacktrace --info
```

## Observações importantes

Alguns endpoints avançados podem retornar vazio ou HTTP 404 caso ainda não estejam habilitados no deploy público. O APK trata isso como ausência de módulo, não como falha fatal.

A tela **Proxy+** foi desenhada para absorver novos campos do Proxy sem exigir nova versão do APK a cada pequeno ajuste de payload.
