# VALORAE APK — Rankings em Página Própria e Cards de Altas/Baixas na Home

## Objetivo

Atender à solicitação de transformar a função de rankings em uma página própria do aplicativo e adicionar, na página inicial, cards de **Maiores Altas** e **Maiores Baixas do dia** inspirados nas referências visuais enviadas.

## Implementações aplicadas

### 1. Página própria de Rankings

Foi criada a tela:

- `app/src/main/java/com/example/ui/screens/RankingsScreen.kt`

A nova página contém:

- cabeçalho próprio `Rankings`;
- botão de atualização manual;
- bloco explicativo sobre o uso dos rankings;
- cards grandes de **Maiores Altas** e **Maiores Baixas**;
- rankings da carteira por Score Valorae, Dividend Yield, Perfil Conservador e Renda FII;
- ranking de mercado por Score/DY quando o Proxy não retornar altas/baixas ao vivo;
- avisos do Proxy;
- plano de ação e inteligência da carteira quando disponíveis.

### 2. Ícone na barra inferior

A barra inferior agora possui a nova entrada:

- `Rankings`, com ícone `Leaderboard`.

O fluxo ficou:

1. Início
2. Rankings
3. Análise
4. Insights
5. Notícias

Arquivo alterado:

- `app/src/main/java/com/example/MainActivity.kt`

### 3. Cards de Maiores Altas/Baixas na página Início

A página inicial agora pode exibir o bloco **Movimentos do Dia**, contendo:

- card de **Maiores Altas**;
- card de **Maiores Baixas**;
- ticker;
- badge visual do ticker;
- preço, quando o Proxy enviar;
- variação, quando o Proxy enviar;
- atalho para a página de Rankings.

Arquivo alterado:

- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`

Componente criado e reutilizado:

- `HomeMarketMoversPreview()` em `RankingsScreen.kt`.

### 4. Rankings de mercado independentes da existência da carteira

Antes, rankings de mercado eram carregados junto do pacote de Insights da carteira. Isso podia impedir Maiores Altas/Baixas na Home quando a carteira ainda estivesse vazia.

Agora o ViewModel carrega ranking ao vivo de mercado na abertura do app, sem depender de posições cadastradas.

Arquivo alterado:

- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`

Função adicionada:

- `refreshLiveMarketRankings(force: Boolean = false)`

### 5. Parser preservando preço e variação separadamente

O modelo `MarketRankingItem` foi ampliado para armazenar separadamente:

- `price`;
- `priceDisplay`;
- `changePercent`;
- `changeDisplay`.

Isso permite montar os cards de altas/baixas com layout mais próximo ao exemplo enviado, sem misturar preço e percentual em um único campo.

Arquivo alterado:

- `app/src/main/java/com/example/network/B3NetworkService.kt`

## Regras preservadas

- O APK continua consumindo o VALORAE Proxy como backend central.
- Não foi adicionado serviço pago.
- Não foi adicionado Firebase, Redis, KV, WebSocket pago ou banco externo.
- O app continua sem scraping direto inseguro.
- Rankings são dados atuais/fundamentalistas e não alteram cálculos históricos da carteira.
- Proventos, IPCA, histórico e agenda continuam respeitando a existência real da carteira.

## Validações estáticas executadas

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

## Validação Gradle

Foi tentado executar:

```bash
./gradlew :app:compileDebugKotlin --stacktrace --info
```

Mas o sandbox voltou a falhar por DNS externo ao baixar o Gradle Wrapper:

```text
java.net.UnknownHostException: services.gradle.org
```

A falha ocorreu antes da compilação do código Kotlin, portanto deve ser tratada como problema de ambiente neste sandbox. Recomenda-se validar no Android Studio com:

```bash
./gradlew clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
./gradlew :app:lintDebug :app:check --stacktrace --info
```

## Arquivos alterados

- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/ui/screens/RankingsScreen.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `scripts/verify_valorae_proxy_capabilities.py`
- `docs/RELATORIO_RANKINGS_PAGINA_PROPRIA_HOME_CARDS.md`

## Observação funcional

Os cards de Maiores Altas/Baixas aparecem somente quando o Proxy retornar `highs`/`lows`. Se a fonte ao vivo bloquear ou o Proxy retornar fallback por comparação, a página própria de Rankings mostra os rankings alternativos de Score Valorae e Dividend Yield em vez de deixar a tela vazia.
