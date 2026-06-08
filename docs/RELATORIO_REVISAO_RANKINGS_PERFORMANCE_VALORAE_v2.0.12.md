# Relatório — revisão final de rankings, desempenho e responsividade do VALORAE v2.0.12

## Objetivo
Revisar novamente o funcionamento dos rankings do APK VALORAE, especialmente o card da tela inicial/Home, e reforçar melhorias de desempenho, performance e responsividade em toda a aplicação.

## Pontos auditados

1. Consumo do ranking da Home.
2. Parser de aliases vindos do VALORAE Proxy/Investidor10/AeroScrape.
3. Chamada em modo completo do Proxy.
4. Fallback leve quando o modo completo falhar.
5. Estado visual de carregamento, erro e retry.
6. Preservação da variação real de altas/baixas.
7. Timeouts, concorrência HTTP, TTLs e fluxo de inicialização.
8. Abertura da área de rankings/Insights a partir da Home.

## Problemas encontrados nesta nova revisão

### 1. Possível skeleton infinito na Home
Se a primeira tentativa de buscar rankings falhasse ou expirasse sem exceção fatal, `liveMarketRanking` poderia continuar nulo e `isLoading=false`. A Home podia continuar exibindo skeleton como se ainda estivesse carregando, sem mostrar botão de tentar novamente.

### 2. Possível sobrescrita da variação real por cotação parcial
O enriquecimento local buscava dados de cotação dos ativos de ranking. Quando a cotação auxiliar vinha parcial ou com `changePercent = 0.0`, ela podia sobrescrever a variação real do ranking do Investidor10/Proxy, degradando a informação exibida na Home.

### 3. Aliases em snake_case ainda podiam não ser capturados
O parser já aceitava `topGainers`, `topLosers`, `maioresAltas` e `maioresBaixas`, mas esta revisão ampliou suporte para variações como `top_gainers`, `top_losers`, `maiores_altas` e `maiores_baixas`.

### 4. Abrir ranking completo não forçava atualização ampla
O botão “Ver Ranking Completo” mudava para a área de Insights, mas agora também força atualização ampla dos rankings antes de abrir a página.

## Correções aplicadas

### `B3NetworkService.kt`

- Ampliados aliases do parser:
  - `altas`, `alta`, `highs`, `high`, `gainers`, `gain`, `maioresAltas`, `maiores_altas`, `topGainers`, `top_gainers`, `topHighs`, `top_highs`, `up`, `ups`, `winners`.
  - `baixas`, `baixa`, `lows`, `low`, `losers`, `loss`, `maioresBaixas`, `maiores_baixas`, `topLosers`, `top_losers`, `topLows`, `top_lows`, `down`, `downs`, `worst`.
- Preservada a variação real do ranking quando a cotação auxiliar vier parcial/zerada.
- Mantido modo completo com:
  - `mode=complete`
  - `complete=1`
  - `strict=1`
  - `limit=15`
  - `minRows=6`
- Mantido fallback leve com:
  - `mode=auto`
  - `limit=15`
  - `minRows=3`

### `PortfolioViewModel.kt`

- Adicionado controle `marketRankingsAttempted` para distinguir:
  - primeira tentativa ainda não realizada;
  - carregamento em andamento;
  - falha real após tentativa.
- Criado snapshot seguro de indisponibilidade para evitar estado nulo permanente.
- Quando a busca falha, a Home agora pode mostrar mensagem amigável e botão de retry.
- Mantida proteção de timeout:
  - Home/ranking vivo: `14.000ms`.
  - Ranking completo/Insights: `18.000ms`.

### `DashboardScreen.kt`

- `HomeMarketMoversPreview` agora recebe `rankingsAttempted`.
- Antes da primeira tentativa, mostra skeleton.
- Depois de falha real, mostra card de erro com “Tentar Novamente”.
- Mantém setas forçadas por aba:
  - Altas: `▲`.
  - Baixas: `▼`.
- Continua usando `assetData` local para completar preço/nome sem sobrescrever a variação real.

### `MainActivity.kt`

- Botão “Ver Ranking Completo” agora chama:
  - `refreshLiveMarketRankings(force = true, full = true)`.
- Aba Insights agora solicita ranking completo sem bloquear:
  - `refreshLiveMarketRankings(force = false, full = true)`.

## Revisão de desempenho, performance e responsividade

Mantidas e verificadas as melhorias anteriores:

- Inicialização escalonada: notícias e rankings não bloqueiam o primeiro frame.
- `distinctUntilChanged()` em fluxos derivados importantes.
- `LazyColumn` principal com chaves estáveis.
- `OkHttp` com concorrência moderada:
  - `maxRequests = 8`.
  - `maxRequestsPerHost = 4`.
- Timeouts delimitados para evitar travar a UI.
- Cache/TTL para evitar chamadas repetidas ao Proxy/Vercel.
- Sem scraping direto no APK.
- Sem JSON bruto na interface.
- Sem simular/inventar dados financeiros quando o Proxy não entrega a informação.

## Arquivos alterados

- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/build.gradle.kts`
- `update.json`
- `scripts/verify_valorae_rankings_performance_v212.py`
- `docs/RELATORIO_REVISAO_RANKINGS_PERFORMANCE_VALORAE_v2.0.12.md`

## Versionamento

- `versionName = "2.0.12"`
- `versionCode = 22`

## Validação executada

Validação estática criada e executada:

```text
STATIC_RANKINGS_PERFORMANCE_V212_OK
```

Itens validados:

- Versão aplicada.
- Aliases de ranking camelCase e snake_case.
- Campos alternativos de ticker, preço, variação, volume, setor e fonte.
- Modo completo e fallback leve do Proxy.
- Home sem skeleton infinito.
- Retry conectado ao modo completo.
- Abertura do ranking completo força atualização ampla.
- Timeouts e concorrência preservados.
- Chaves estáveis no `LazyColumn`.
- Redução de recomposições preservada.

## Limitação da validação

A compilação Gradle completa não pôde ser executada neste ambiente porque o wrapper tentou baixar:

```text
https://services.gradle.org/distributions/gradle-9.3.1-bin.zip
```

O ambiente não possui acesso externo, resultando em `UnknownHostException`. O log foi salvo em:

```text
docs/APK_BUILD_ATTEMPT_RANKINGS_PERFORMANCE_v2.0.12.log
```

## Resultado

A revisão encontrou riscos reais e aplicou correções defensivas. O ranking da Home agora está mais seguro contra:

- resposta parcial;
- timeout;
- primeira falha de rede;
- aliases alternativos do Proxy;
- cotação auxiliar incompleta;
- estado visual preso em carregamento.

