# RELATÓRIO — Sincronização de Gráficos Investidor10 · APK VALORAE v2.0.16

## Objetivo
Atualizar o APK VALORAE para consumir os gráficos canônicos enviados pelo VALORAE Proxy v21.12.61, evitando gráficos incompletos, inventados ou divergentes do Investidor10.

## Arquivo principal alterado
- `app/src/main/java/com/example/network/B3NetworkService.kt`

Também foi sincronizada a cópia interna quando existente:
- `apk/app/src/main/java/com/example/network/B3NetworkService.kt`

## Mudanças aplicadas
- A chamada de bundle de gráficos agora solicita `charts=full`, `includeCharts=1`, `chartSource=investidor10`, `internalApis=1`, `mode=complete`.
- Timeout principal ampliado para 16000ms; fallback completo para 9000ms; fallback básico para 5000ms.
- Parser do APK agora prioriza `assetChartsCanonical`.
- Rentabilidade nominal e real agora são lidas de `assetChartsCanonical.profitability.nominal` e `.real`.
- Comparação com índices prioriza `assetChartsCanonical.indexComparison`.
- Receita x Lucro prioriza `assetChartsCanonical.financial.revenueProfit`.
- Lucro x Cotação prioriza `assetChartsCanonical.financial.profitVsQuote`.
- Balanço Patrimonial prioriza `assetChartsCanonical.financial.balanceSheet`.
- Payout Histórico prioriza `assetChartsCanonical.financial.payoutHistory`.

## Correções contra dados errados
- Removida criação de rentabilidade sintética a partir de histórico de preço.
- Removido fallback automático de `/api/v1/compare` dentro do bundle da tela de ativo.
- Removida criação sintética de comparação com índice usando apenas `Base` e `12M` dentro do fluxo de gráfico de ativo.

## Validações executadas
- `STATIC_VALORAE_ASSET_CHARTS_CANONICAL_V216_OK`.
- `STATIC_BALANCE_STRIPPED_OK` para arquivos Kotlin.
- Tentativa de Gradle registrada em `docs/APK_BUILD_ATTEMPT_GRAFICOS_INVESTIDOR10_v2.0.16.log`.

## Limitação
A compilação Gradle completa não foi finalizada neste ambiente porque o wrapper tentou baixar Gradle em `services.gradle.org`, mas o ambiente está sem DNS/rede externa.

## Versão
- `versionName = 2.0.16`
- `versionCode = 26`
