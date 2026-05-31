# VALORAE APK — Rankings em página própria estilo Investidor10 e correção da Home

## Objetivo

Atender à solicitação de transformar a página **Rankings** em uma área própria com vários containers/categorias, semelhante à experiência visual da página de rankings do Investidor10, e corrigir o resumo da página **Início**, onde o bloco **Maiores Baixas** podia aparecer sem o valor/preço do ativo.

## Correções e melhorias aplicadas

### 1. Página Rankings com containers diversificados

A tela `RankingsScreen.kt` foi reorganizada para exibir uma grade de containers com categorias de ranking. Cada container pode ser selecionado e abre a lista detalhada correspondente abaixo.

Categorias implementadas:

- Maiores Altas
- Maiores Baixas
- Score Valorae
- Dividend Yield
- Mais Baratas por P/VP
- Menores P/Ls
- Maiores ROEs
- Maiores ROICs
- Qualidade dos Dados
- Buy And Hold / Perfil Conservador
- Crescimento
- Perfil Dividendos
- Valor
- FIIs Renda
- FIIs Dividend Yield
- FIIs Mais Baratos
- Carteira Score
- Carteira Dividend Yield

A tela mantém aviso visual limpo quando uma categoria ainda não tem dados suficientes do Proxy, sem travar ou mostrar tela quebrada.

### 2. Rankings de mercado independentes da carteira

O `PortfolioViewModel` agora busca rankings de mercado mesmo sem carteira cadastrada:

- Ranking ao vivo de ações, para altas/baixas.
- Ranking fundamentalista de ações, para score, DY, P/VP, P/L, ROE, ROIC, qualidade e perfis.
- Ranking fundamentalista de FIIs, para renda e DY de fundos imobiliários.

Novos campos adicionados ao estado de analytics:

- `stockMarketRanking`
- `fiiMarketRanking`

### 3. Parser ampliado do Valorae Proxy

O parser de rankings em `B3NetworkService.kt` agora lê mais listas do contrato do Proxy:

- `score`
- `dividendYield`
- `pvp`
- `pl`
- `roe`
- `roic`
- `quality`
- perfis `conservador`, `crescimento`, `dividendos`, `valor`, `rendaFii`
- `altas`
- `baixas`

Também foram adicionados métodos:

- `fetchStockFundamentalRankings()`
- `fetchFiiFundamentalRankings()`

### 4. Correção do preço em Maiores Baixas na Home

O bloco **Movimentos do Dia** da página **Início** agora passa `cachedAssetData` para os cards de ranking.

Com isso, quando o ranking ao vivo retorna variação mas não retorna preço suficiente para uma baixa, a UI tenta preencher o preço usando o snapshot local/cacheado do ativo.

Também foram ampliados os campos aceitos pelo parser para preço e variação:

- `price`
- `lastPrice`
- `currentPrice`
- `cotacao`
- `cotacaoAtual`
- `preco`
- `precoAtual`
- `valorAtual`
- `last`
- `changePercent`
- `variationPercent`
- `dailyChangePercent`
- `variacaoPercentual`
- `percent`
- `percentage`
- `pct`
- `variacao`
- `change`

### 5. Regras preservadas

- O app continua consumindo o VALORAE Proxy como backend central.
- Não foi adicionado serviço pago.
- Não foi adicionado Firebase pago, Redis, KV, WebSocket pago ou banco externo.
- O app não faz scraping direto inseguro.
- Rankings não alteram proventos históricos, IPCA ou a linha do tempo real da carteira.
- A página Insights permanece separada da página Rankings.

## Arquivos alterados

- `app/src/main/java/com/example/ui/screens/RankingsScreen.kt`
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `scripts/verify_valorae_proxy_capabilities.py`

## Validações executadas

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

A tentativa de execução do Gradle neste sandbox voltou a falhar por DNS externo ao baixar a distribuição:

```text
java.net.UnknownHostException: services.gradle.org
```

A falha ocorre antes da compilação Kotlin e não indica erro de código do APK.

Comandos recomendados no Android Studio:

```bash
./gradlew clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
./gradlew :app:lintDebug :app:check --stacktrace --info
```
