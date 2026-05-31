# RELATÓRIO — Auditoria crítica de funcionamento: gráficos, Detalhes do Ativo e Insights

Projeto auditado: `investidor-portfolio (7).zip`  
Data da auditoria: 2026-05-27/28  
Proxy oficial: `https://servidor-valorae.vercel.app/api`

## 1. Diagnóstico geral

A página **Análise** continuava funcionando melhor que **Detalhes do Ativo** porque usa o estado central do `PortfolioViewModel` e os dados carregados pelo fluxo de busca/análise. Já **Detalhes do Ativo** ainda dependia demais de novas chamadas no modal, começava com `initialAssetData` nulo e não preservava fallback local quando o Proxy estava parcial, lento ou retornava um bundle ainda vazio.

Também havia regressões na camada de gráficos/Insights:

- `Detalhes do Ativo` não reaproveitava de forma robusta `AssetChartBundle.priceHistory`.
- O modal podia receber `chartHistory` genérico da última análise, possivelmente de outro ticker.
- A página **Insights** ainda usava muitos dados simulados/projetados mesmo quando `analytics.dividendEvents` estava disponível.
- O gráfico de **Payout Histórico** não lia corretamente arrays do Proxy no formato `{ value, year }` dentro de `payOutCompanyIndicators`.
- `mapProxyAsset()` só aceitava `root.normalized`, mas o Proxy também pode retornar `results.normalized`.
- O script de verificação estava preso a uma implementação antiga (`AssetChartBundlePanel`) e acusava falso negativo quando a tela usava abas diretas como `StockAnalysisTab`/`FiiGeneralTab`.
- O `gradle-wrapper.jar` presente no ZIP está corrompido, impedindo `./gradlew clean assembleDebug` neste ambiente.

## 2. Correções aplicadas

### 2.1 `AssetDetailModal.kt`

Correções aplicadas:

- Reintroduzido fallback local por ativo através de `AssetSummary.toFallbackB3AssetData()`.
- `assetData` agora inicia com `initialAssetData ?: fallbackAssetData`.
- O modal não abre mais vazio quando o Proxy ainda não respondeu.
- Dados remotos são mesclados com fallback local via `mergeWithFallback()`.
- `chartBundle.priceHistory` agora alimenta `localChartPoints` quando existir.
- `chartPoints` só é usado como fallback, evitando exibir histórico de outro ativo.
- `isLoadingChartBundle` não fica travado como loading quando não há bundle inicial mas há fallback funcional.
- Mantida inferência robusta de FII via tipo do ativo e ticker.

Resultado esperado:

- Ao tocar em PETR4, VALE3, MXRF11 ou outro ativo na carteira, **Detalhes do Ativo** abre com preço/custódia local imediatamente e atualiza com Proxy quando os dados chegarem.
- A tela não fica branca quando o Proxy retorna `PARTIAL`, `warnings` ou campos ausentes.

### 2.2 `DashboardScreen.kt`

Correções aplicadas:

- Ao clicar em um ativo, o Dashboard agora dispara `onLoadAssetChartBundle(ticker, range)` antes de abrir o modal.
- O modal recebe um `cachedBundle` específico do ticker.
- `chartPoints` passado para o modal agora vem de `cachedBundle.priceHistory`, não do histórico genérico da Análise.

Resultado esperado:

- **Detalhes do Ativo** passa a usar dados e gráficos do ativo correto.
- Reduz risco de PETR4 abrir com gráfico de MXRF11 ou de outro ativo analisado anteriormente.

### 2.3 `B3NetworkService.kt`

Correções aplicadas:

- `mapProxyAsset()` agora aceita `root.normalized` e `results.normalized`.
- `payoutHistorico` agora aceita:
  - array numérico;
  - array de objetos `{ value, year }`;
  - chaves alternativas `payout` e `items`.

Resultado esperado:

- Indicadores de Detalhes e Análise ficam mais resilientes aos formatos reais do Proxy.
- Gráfico de **Payout Histórico** passa a renderizar quando o Proxy retorna `payOutCompanyIndicators` como objetos.

### 2.4 `ChartsScreen.kt` — Insights

Correções aplicadas nas páginas:

#### Evolução de Proventos

- Criado `buildDividendEvolutionData()`.
- O gráfico agora prioriza `analytics.dividendEvents` vindos do Proxy.
- Quando não houver eventos do Proxy, usa fallback local baseado nos ativos da carteira.
- Agrupa eventos por mês e separa valores recebidos/projetados.

#### Rentabilidade vs IPCA+

- Mantido consumo de `analytics.portfolioHistory` e `analytics.ipcaSeries`.
- Mantido fallback transparente quando o Proxy não entrega histórico/IPCA.
- Ajustado cálculo seguro para valores NaN/infinitos.

#### Equilíbrio de Carteira

- Mantido uso de `analytics.analysis.allocationByClass` e `allocationBySector` quando disponíveis.
- Corrigido texto “Sectores” para “Setores”.
- Mantido fallback local por tipo de ativo e setor inferido.

#### Agenda de Dividendos

- Criado `buildDividendAgendaData()`.
- A prévia da agenda e a página detalhada agora priorizam `analytics.dividendEvents`.
- Ordenação passa a considerar data de pagamento/data-com quando disponível.
- Se não houver evento remoto, volta ao fallback local por `lastDividend * sharesCount`.

### 2.5 `scripts/verify_valorae_proxy_integration.py`

Correções aplicadas:

- Atualizado para aceitar as abas diretas de gráficos (`StockAnalysisTab`, `FiiGeneralTab`) em vez de exigir apenas `AssetChartBundlePanel`.
- Adicionados checks para helpers de Insights.
- Adicionados checks para fallback e merge em `AssetDetailModal`.

Resultado da verificação local:

```text
Valorae Proxy integration audit OK
```

## 3. Validação executada

### 3.1 Script de integração

Comando:

```bash
python3 scripts/verify_valorae_proxy_integration.py
```

Resultado:

```text
Valorae Proxy integration audit OK
```

### 3.2 Checagem estrutural simples

Arquivos checados:

- `AssetDetailModal.kt`
- `ChartsScreen.kt`
- `B3NetworkService.kt`
- `DashboardScreen.kt`

Resultado:

- Chaves `{}` balanceadas.
- Parênteses `()` balanceados.

### 3.3 Tentativa de build

Comando:

```bash
./gradlew clean assembleDebug
```

Resultado:

```text
Error: Invalid or corrupt jarfile /mnt/data/audit7/gradle/wrapper/gradle-wrapper.jar
```

Conclusão: não foi possível gerar APK neste ambiente porque o `gradle-wrapper.jar` que veio no ZIP está corrompido. Isso precisa ser regenerado no Android Studio/Gemini ou substituído por um wrapper Gradle válido antes de compilar.

## 4. Arquivos alterados

- `app/src/main/java/com/example/ui/components/AssetDetailModal.kt`
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `scripts/verify_valorae_proxy_integration.py`
- `docs/RELATORIO_AUDITORIA_INSIGHTS_DETALHES_GRAFICOS_VALORAE.md`

## 5. Critérios de aceite para o Gemini/Studio

O app só deve ser considerado corrigido quando:

1. O Gradle wrapper for regenerado/substituído e `./gradlew clean assembleDebug` executar.
2. APK debug novo for gerado.
3. PETR4 abrir em Análise e Detalhes com dados.
4. MXRF11 abrir em Análise e Detalhes com dados de FII.
5. Detalhes do Ativo não abrir vazio mesmo com Proxy parcial.
6. Gráficos de preço, dividendos, indicadores e payout renderizarem quando houver dados.
7. Insights mostrar Evolução de Proventos com eventos reais do Proxy quando disponíveis.
8. Rentabilidade vs IPCA+ usar `portfolioHistory` + `ipcaSeries` quando disponíveis.
9. Equilíbrio de Carteira usar alocação do Proxy com fallback local.
10. Agenda de Dividendos usar `analytics.dividendEvents` com fallback local.
11. Nenhuma tela ficar branca com `PARTIAL`, `warnings` ou campos ausentes.
12. Nenhuma referência ativa ao host antigo ser usada.

