# Prompt para Gemini 3.1 Pro — VALORAE Investidor / Portfolio

Use o ZIP corrigido anexado como base principal do projeto.

Leia o relatório dentro do projeto:

`docs/RELATORIO_AUDITORIA_INSIGHTS_DETALHES_GRAFICOS_VALORAE.md`

E leia também o log:

`BUILD_VALIDATION_LOG_VALORAE_INSIGHTS_DETALHES.txt`

## Objetivo

Validar, preservar e finalizar as correções aplicadas no app VALORAE Investidor/Portfolio, com foco em:

1. Gráficos gerais do app.
2. Página **Detalhes do Ativo**, que não recebia informações corretamente.
3. Página **Insights**.
4. Páginas de **Evolução de Proventos**, **Rentabilidade vs IPCA+**, **Equilíbrio de Carteira** e **Agenda de Dividendos**.
5. Correções de código e funcionamento.
6. Geração de APK debug novo.

## Proxy oficial obrigatório

Use somente:

`https://servidor-valorae.vercel.app/api`

Não use:

`https://valorae-proxy.vercel.app`

Não faça scraping direto no app Android.
Todas as informações externas devem vir pelo Valorae Proxy.

## Primeiro problema a corrigir no ambiente

O ZIP anterior contém `gradle/wrapper/gradle-wrapper.jar` corrompido.
Antes de compilar, regenere ou substitua o Gradle Wrapper por um wrapper válido.

Depois execute:

```bash
./gradlew clean assembleDebug
```

Se o wrapper não funcionar, use o Android Studio para regenerar o wrapper e compile novamente.

## Correções que já foram aplicadas e devem ser preservadas

### 1. `AssetDetailModal.kt`

Preserve as seguintes correções:

- `AssetSummary.toFallbackB3AssetData()`.
- `B3AssetData.hasUsefulProxyData()`.
- `B3AssetData.mergeWithFallback()`.
- `assetData` deve iniciar com `initialAssetData ?: fallbackAssetData`.
- O modal deve abrir com dados locais imediatamente.
- O modal deve atualizar com dados do Proxy sem ficar branco.
- `initialChartBundle.priceHistory` deve alimentar `localChartPoints`.
- `chartPoints` genérico só deve ser fallback, nunca fonte principal quando houver bundle específico.
- O modal deve aceitar PETR4, VALE3, MXRF11 e demais ativos.
- O modal não pode quebrar com status `PARTIAL`, `warnings` ou campos ausentes.

### 2. `DashboardScreen.kt`

Preserve as seguintes correções:

- Ao clicar em um ativo, chamar `onLoadAssetChartBundle(ticker, range)`.
- Passar `cachedBundle = assetChartBundles[tickerKey]` para o modal.
- Passar `chartPoints = cachedBundle?.priceHistory.orEmpty()`.
- Não passar histórico genérico de Análise para Detalhes se ele pode pertencer a outro ticker.

### 3. `B3NetworkService.kt`

Preserve as seguintes correções:

- `mapProxyAsset()` deve aceitar `root.normalized` e `results.normalized`.
- `payoutHistorico` deve aceitar arrays numéricos e arrays de objetos `{ value, year }`.
- Continuar aceitando `results.chartsFinanceiros`, `results.revenueGeography`, `results.revenueSegment`, `historicoDividendos`, `sections`, `financialSummary` e `ratiosChave`.
- Continuar aceitando múltiplos formatos de histórico:
  - `points`
  - `series`
  - `history`
  - `prices`
  - `items`
  - `data.points`
  - `data.series`
  - `data.history`
  - `data.prices`
  - `data.items`

### 4. `ChartsScreen.kt`

Preserve as seguintes correções:

- `buildDividendEvolutionData()`.
- `buildDividendAgendaData()`.
- `safeDividendAmount()`.
- `eventRelevantMillis()`.
- `eventMonthLabel()`.
- `isPaidDividendEvent()`.

A página Insights deve funcionar assim:

#### Evolução de Proventos

- Priorizar `analytics.dividendEvents` vindos do Proxy.
- Agrupar eventos por mês.
- Separar recebido x projetado.
- Usar fallback local baseado em carteira quando não houver eventos.

#### Rentabilidade vs IPCA+

- Priorizar `analytics.portfolioHistory`.
- Priorizar `analytics.ipcaSeries`.
- Calcular ganho real como carteira acumulada menos IPCA acumulado.
- Nunca quebrar se uma série vier vazia; usar fallback transparente.

#### Equilíbrio de Carteira

- Priorizar `analytics.analysis.allocationByClass` e `allocationBySector`.
- Usar fallback local por classe e setor quando o Proxy não entregar.
- Corrigir texto para “Setores”.

#### Agenda de Dividendos

- Priorizar `analytics.dividendEvents`.
- Ordenar por data de pagamento/data-com.
- Exibir valor recebido/projetado.
- Usar fallback local por `lastDividend * sharesCount` quando não houver evento remoto.

## Testes obrigatórios no app

Validar manualmente no emulador ou aparelho:

1. Abrir Dashboard.
2. Clicar em PETR4 na carteira.
3. Confirmar que **Detalhes do Ativo** abre com dados.
4. Confirmar que os gráficos de PETR4 aparecem quando disponíveis.
5. Abrir Análise e pesquisar PETR4.
6. Abrir Análise e pesquisar MXRF11.
7. Clicar em MXRF11 na carteira e confirmar detalhes de FII.
8. Abrir Insights.
9. Validar **Evolução de Proventos**.
10. Validar **Rentabilidade vs IPCA+**.
11. Validar **Equilíbrio de Carteira**.
12. Validar **Agenda de Dividendos**.
13. Confirmar que nenhuma tela fica branca.
14. Confirmar que nenhum host antigo está ativo.

## Headers obrigatórios do Proxy

Todas as chamadas devem manter:

```text
Accept: application/json
X-Valorae-Client-Id: valorae-investidor-android
X-Valorae-Client-Version: 21.5.13
X-Valorae-Environment: production
```

## Critérios de aceite

A entrega só estará correta quando:

- O app compilar sem erros.
- Um APK debug novo for gerado.
- `./gradlew clean assembleDebug` funcionar após corrigir o wrapper.
- Análise continuar funcionando.
- Detalhes do Ativo passar a receber dados corretamente.
- PETR4, VALE3 e MXRF11 funcionarem.
- Gráficos avançados funcionarem quando houver dados.
- Insights usar dados do Proxy com fallback local.
- Nenhuma tela ficar branca com dados parciais.
- O app continuar compatível com plano gratuito GitHub/Vercel.
