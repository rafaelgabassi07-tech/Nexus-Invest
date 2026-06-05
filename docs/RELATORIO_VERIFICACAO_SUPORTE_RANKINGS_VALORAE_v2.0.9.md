# Verificação do suporte de rankings no APK VALORAE v2.0.9

## Objetivo
Verificar se o APK VALORAE está preparado para receber os rankings do VALORAE Proxy v21.12.59, incluindo o novo mecanismo próprio de rankings do Investidor10.

## Resultado
O suporte estrutural do APK estava presente, mas a chamada remota ainda não aproveitava o modo completo do Proxy. O parser já aceitava os aliases principais, porém a requisição ao endpoint `/api/v1/market/rankings` usava timeout curto para ranking ao vivo e não enviava `mode=complete`, `complete=1`, `strict=1`, `limit` e `minRows`.

## Correções aplicadas

### 1. Parser confirmado
O `B3NetworkService.parseMarketRankingSnapshot` aceita:

- `rankings.altas`
- `rankings.baixas`
- `rankings.highs`
- `rankings.lows`
- `rankings.gainers`
- `rankings.losers`
- `rankings.maioresAltas`
- `rankings.maioresBaixas`
- `rankings.topGainers`
- `rankings.topLosers`

Também aceita campos de linha como:

- `ticker`, `symbol`, `ativo`
- `name`, `nome`, `company`
- `price`, `preco`, `precoAtual`, `cotacaoAtual`
- `priceDisplay`, `precoFormatado`, `cotacaoFormatada`
- `changePercent`, `change`, `variacao`, `variacaoPercentual`
- `changeDisplay`, `variacaoFormatada`

### 2. Chamadas ao Proxy atualizadas
A função `fetchMarketRankings` agora aceita parâmetros internos:

- `complete`
- `strict`

Quando `complete=true`, o APK envia ao Proxy:

```text
mode=complete
complete=1
limit=15
minRows=6
profile=deep
```

Quando o ranking ao vivo é aberto em modo completo, também envia:

```text
strict=1
timeoutMs=14000
```

### 3. Timeouts corrigidos
Antes, ranking ao vivo usava timeout de aproximadamente 2,5 segundos, o que era agressivo demais para capturar páginas dedicadas do Investidor10.

Agora:

- ranking ao vivo leve: `timeoutMs=9000`
- ranking ao vivo completo: `timeoutMs=14000`
- ranking comparativo completo: `timeoutMs=18000`
- ranking comparativo leve: `timeoutMs=6000`

### 4. Modo completo conectado ao ViewModel
`PortfolioViewModel.refreshLiveMarketRankings(full = true)` agora chama:

- `fetchLiveStockRankings(complete = true)`
- `fetchStockFundamentalRankings(complete = true)`
- `fetchFiiFundamentalRankings(complete = true)`

Assim, quando a tela de rankings/atualização completa for usada, o APK passa a pedir dados completos em vez de depender apenas do ranking rápido.

### 5. Ranking da carteira também mais completo
`fetchPortfolioRankings` agora usa `complete=true`, permitindo que o ranking da carteira receba análise mais profunda do Proxy.

## Teste regressivo adicionado
Foi adicionado teste em `B3NetworkServiceParserTest.kt` para validar o contrato v21.12.59 com:

- `topGainers`
- `maioresBaixas`
- `preco`
- `precoFormatado`
- `variacao`
- `changeDisplay`

## Limitação da validação local
A execução de `./gradlew testDebugUnitTest` não pôde ser concluída neste ambiente porque o Gradle Wrapper tentou baixar a distribuição do Gradle em `services.gradle.org`, mas o ambiente local não tem acesso externo à internet.

Erro observado:

```text
java.net.UnknownHostException: services.gradle.org
```

A validação possível aqui foi feita por inspeção estática dos fontes e consistência do contrato APK ↔ Proxy.

## Conclusão
Após a correção, o APK VALORAE está com suporte correto para receber os rankings do VALORAE Proxy v21.12.59, incluindo rankings ao vivo do Investidor10 em modo completo, aliases Android e fallback por comparação quando a fonte ao vivo estiver indisponível.
