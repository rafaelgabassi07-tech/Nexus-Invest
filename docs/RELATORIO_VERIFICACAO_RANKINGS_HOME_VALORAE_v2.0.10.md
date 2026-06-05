# Relatório — correção dos rankings da tela inicial do VALORAE v2.0.10

## Objetivo
Verificar se o ranking exibido na tela de início do APK VALORAE está funcionando corretamente para receber os dados do VALORAE Proxy, especialmente maiores altas e maiores baixas vindas do mecanismo atualizado de rankings.

## Problemas encontrados

1. O card `HomeMarketMoversPreview` tinha uma duplicação estrutural de `Column(` na lista dos itens de ranking, com risco de quebrar a compilação/renderização da Home.
2. O card recebia `assetData`, mas não usava esse cache para preencher preço e nome quando o ranking vinha incompleto.
3. A seta/semântica visual de altas e baixas dependia demais do sinal numérico vindo do Proxy. Se a fonte mandasse uma baixa como valor absoluto, a Home poderia exibir seta de alta.
4. O carregamento inicial de rankings não marcava `isLoading=true`, então a Home podia mostrar erro antes da primeira tentativa de rede.
5. O ranking vivo usado pela Home ainda podia usar captura leve. Agora a Home solicita o modo completo do Proxy e usa fallback leve automático se a fonte completa falhar.
6. O timeout global do OkHttp estava curto para o modo completo do Proxy.

## Arquivos alterados

- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/build.gradle.kts`
- `update.json`
- `scripts/verify_valorae_home_rankings_fix.py`
- `scripts/verify_valorae_loading_optimization.py`

## Correções aplicadas na Home

- Removida a duplicação de `Column(` no card de rankings.
- Adicionado uso real do cache `assetData` para preencher preço e nome do ativo.
- Adicionado `testTag("home_market_movers_card")` para facilitar auditoria visual/instrumentada.
- A aba selecionada agora se ajusta automaticamente quando só há altas ou só há baixas.
- As setas são definidas pela seção ativa:
  - `▲` para Maiores Altas;
  - `▼` para Maiores Baixas.
- A Home passa a mostrar skeleton antes da primeira resposta em vez de erro prematuro.
- A fonte do ranking é exibida de forma discreta no cabeçalho do card.
- O card não mostra JSON bruto e usa placeholder visual `—` quando preço/variação não vierem do Proxy.

## Correções aplicadas na busca de rankings

`PortfolioViewModel.refreshLiveMarketRankings()` agora:

- marca `isLoading=true` antes da chamada;
- busca o ranking vivo da Home com `fetchLiveStockRankings(complete = true)`;
- mantém fallback seguro em caso de falha;
- preserva o último ranking válido quando a nova chamada falha;
- desliga loading em erro controlado.

`B3NetworkService.fetchMarketRankings()` já usa o endpoint:

```text
/api/v1/market/rankings
```

com parâmetros de modo completo:

```text
mode=complete
complete=1
strict=1
limit=15
minRows=6
```

E mantém fallback leve:

```text
mode=auto
limit=15
minRows=3
```

## Timeouts ajustados

O cliente OkHttp foi ajustado para suportar ranking completo/lento:

- `connectTimeout`: 10s
- `readTimeout`: 22s
- `callTimeout`: 24s

## Parser de rankings

O parser foi reforçado para aceitar mais aliases compatíveis com Investidor10/AeroScrape/Proxy:

- `altas`, `highs`, `gainers`, `maioresAltas`, `topGainers`
- `baixas`, `lows`, `losers`, `maioresBaixas`, `topLosers`
- ticker/código por `ticker`, `codigo`, `symbol`, `code`, `papel`, `ativo`
- preço por `price`, `cotacao`, `preco`, `valor`, `valorUnitario` e objetos aninhados de cotação
- variação por `changePercent`, `variationPercent`, `variacaoPercentual`, `percentual`, `variacaoDia`, `dailyChange`, `dailyVariation`, `change_pct`, `change_percentage`

## Versionamento

- `versionName`: `2.0.10`
- `versionCode`: `20`

## Validação executada

Foi executada validação estática específica para a Home:

```text
STATIC_HOME_RANKINGS_FIX_OK
STATIC_KOTLIN_BALANCE_OK
Valorae loading optimization audit OK
```

O Gradle completo não foi executado neste ambiente porque o wrapper tenta baixar a distribuição em `services.gradle.org`, e o ambiente atual não possui acesso externo. A validação feita aqui foi estática e focada nos contratos de código da Home, ViewModel e Proxy client.
