# Revisão da integração APK VALORAE × VALORAE Proxy e correção dos gráficos de faturamento

Data: 2026-05-30
Projeto revisado: `valorae-apk-gradle-validado-consolidado.zip`
Proxy alvo: VALORAE Proxy v21.12.51/v21.12.52, URL pública `https://servidor-valorae.vercel.app/`

## Resultado executivo

A integração principal do APK com o VALORAE Proxy está preservada e apontando para os contratos `/api/v1/...`.
A revisão encontrou um problema real nos gráficos **Faturamento por Região** e **Faturamento por Negócio**: o parser do APK só aceitava alguns formatos de quebra de receita, enquanto o Proxy/Investidor10 pode entregar esses blocos em formatos de gráfico diferentes, como Highcharts, Apex/Chart.js, objetos por ano, objetos simples ou campos dentro do contrato canônico.

Também havia um problema visual: quando o mapa existia, mas os pontos não tinham valores utilizáveis, o componente podia ficar em branco em vez de mostrar um estado vazio claro.

As duas causas foram corrigidas.

## Arquivos alterados

1. `app/src/main/java/com/example/network/B3NetworkService.kt`
2. `app/src/main/java/com/example/ui/components/AssetCharts.kt`
3. `app/src/test/java/com/example/B3NetworkServiceParserTest.kt`
4. `scripts/verify_valorae_proxy_integration.py`
5. `docs/RELATORIO_REVISAO_INTEGRACAO_GRAFICOS_FATURAMENTO.md`

## Correções aplicadas

### 1. Parser robusto para faturamento por região e por negócio

O APK agora aceita múltiplos formatos possíveis para `revenueGeography`, `revenueSegment`, `regioesReceita` e `negociosReceita`:

- `{ "2024": { "Brasil": { "value": 75 }, "Exterior": { "value": 25 } } }`
- `{ "2024": [{ "name": "Varejo", "percent": "60%" }] }`
- `{ "labels": ["Brasil", "Exterior"], "series": [80, 20] }`
- `{ "labels": ["Brasil", "Exterior"], "data": [80, 20] }`
- `{ "series": [{ "data": [{ "name": "Brasil", "y": 80 }] }] }`
- `{ "datasets": [{ "data": [80, 20] }], "labels": [...] }`
- `{ "Brasil": 80, "Exterior": 20 }`
- seções resumidas com `keyValues` e `text` quando o Proxy entregar seção textual do Investidor10.

Também foram adicionados caminhos extras de leitura do contrato canônico:

- `appPayload.charts.revenueGeography`
- `appPayload.charts.revenueSegment`
- `appPayload.charts.revenueByBusiness`
- `assetClassContract.groups.statements.fields.regioesReceita.value`
- `assetClassContract.groups.statements.fields.revenueGeography.value`
- `assetClassContract.groups.statements.fields.negociosReceita.value`
- `assetClassContract.groups.statements.fields.revenueSegment.value`

### 2. Proteção contra gráfico em branco

A tela `StockBusinessTab` agora seleciona o ano mais recente que possui pontos realmente válidos. Se o Proxy não entregar percentuais utilizáveis, a UI mostra um estado vazio explícito em vez de renderizar um card aparentemente quebrado.

Mensagens adicionadas:

- `O Proxy não retornou percentuais válidos de segmentos operacionais para este ativo.`
- `O Proxy não retornou percentuais válidos de geografia de receita para este ativo.`

### 3. Testes adicionados

Foram adicionados testes unitários cobrindo os formatos críticos:

- `testRevenueBreakdownParsesHighchartsAndApexShapes`
- `testRevenueBreakdownPreservesYearMappedProxyShapes`
- `testRevenueBreakdownParsesAppContractFieldValueShape`

Esses testes cobrem exatamente os gráficos de faturamento por região e faturamento por negócio.

### 4. Auditoria estática atualizada

O script `scripts/verify_valorae_proxy_integration.py` foi atualizado para validar os endpoints `/api/v1/...`, e não mais os endpoints legados.

Resultado local:

```text
Valorae Proxy integration audit OK
```

## Endpoints confirmados no código do APK

- `/api/v1/ready`
- `/api/v1/release/readiness`
- `/api/v1/source/status`
- `/api/server/metrics`
- `/api/v1/asset`
- `/api/v1/assets`
- `/api/v1/asset/history`
- `/api/v1/asset/dividends`
- `/api/v1/compare`
- `/api/v1/news`
- `/api/v1/market/indices`
- `/api/v1/market/ipca`
- `/api/v1/portfolio/analyze`
- `/api/v1/portfolio/history`
- `/api/v1/portfolio/next-dividends`
- `/api/v1/integration/manifest`

Rotas técnicas preservadas:

- `/api/observability`
- `/api/fields`
- `/api/openapi`

## Segurança e compatibilidade

Confirmado estaticamente:

- Não há chamadas diretas no app para `investidor10.com.br`, `statusinvest.com.br`, `query1.finance.yahoo.com` ou `news.google.com/rss`.
- O app permanece consumindo o VALORAE Proxy como backend central.
- O fallback direto Android segue desativado.
- A URL base continua configurável via `VALORAE_API_BASE_URL`, `VALORAE_PROXY_BASE_URL` e `VALORAE_PUBLIC_BASE_URL`.
- O host legado `valorae-proxy.vercel.app` é bloqueado pela sanitização local.

## Validação executada neste ambiente

Executado com sucesso:

```bash
python3 scripts/verify_valorae_proxy_integration.py
```

Resultado:

```text
Valorae Proxy integration audit OK
```

Tentativa de Gradle neste sandbox:

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
```

Resultado: não executou por falha externa de DNS ao tentar baixar a distribuição do Gradle:

```text
java.net.UnknownHostException: services.gradle.org
```

Isso é o mesmo tipo de falha ambiental já identificado anteriormente; não é erro de código do APK. Como o projeto já havia sido validado no Android Studio com Gradle nativo, a recomendação é rodar novamente os comandos abaixo no Studio após abrir este pacote atualizado.

## Comandos recomendados no Android Studio

```bash
./gradlew clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
./gradlew :app:lintDebug :app:check --stacktrace --info
```

## Como testar funcionalmente os gráficos corrigidos

1. Abrir o app com internet.
2. Confirmar em Configurações/Diagnóstico que `/api/v1/ready` está Online ou Parcial utilizável.
3. Abrir um ativo de ação, preferencialmente um que o Investidor10 entregue quebra de receita.
4. Entrar nos gráficos/aba de análise avançada.
5. Verificar:
   - `Faturamento por Negócio (%)`;
   - `Faturamento por Região (%)`.
6. Resultado esperado:
   - se o Proxy entregar `revenueSegment/negociosReceita`, o gráfico de negócio renderiza;
   - se o Proxy entregar `revenueGeography/regioesReceita`, o gráfico de região renderiza;
   - se o Proxy não entregar a seção para aquele ativo, aparece estado vazio claro, não card em branco.

## Observação importante

Nem todos os ativos possuem quebra de receita por região ou por negócio disponível publicamente. A correção garante que o APK consuma corretamente os formatos enviados pelo Proxy e não fique em branco. Quando a fonte não entregar esses blocos, o app deve exibir indisponibilidade de forma limpa.
