# VALORAE APK v2.0.42 — Segunda auditoria geral de boot, UI, cache e estados vazios

Data: 2026-06-09
Base auditada: APK VALORAE v2.0.41 gerado na rodada anterior

## Objetivo

Esta rodada foi feita para revisar o app além do fluxo Proxy → APK. O foco foi reduzir delays percebidos, impedir páginas vazias, evitar chamadas de rede no caminho crítico de renderização, separar estados de carregamento e endurecer a experiência de abertura do usuário.

## Diagnóstico aprofundado

A primeira rodada já havia introduzido cache-first/stale-first, snapshots de ativos e timeouts locais. Nesta segunda auditoria foram encontrados pontos que ainda podiam causar lentidão ou sensação de tela vazia:

1. O endpoint `/api/v1/mobile/bootstrap` havia sido criado no Proxy, mas o APK ainda não o consumia como primeira chamada compacta de abertura.
2. Notícias tinham cache em memória, mas não tinham snapshot persistido para sobreviver a fechamento/reabertura do app.
3. Rankings de mercado ainda podiam alterar `portfolioAnalytics.isLoading`, fazendo páginas de Agenda/Proventos/Insights parecerem consultar dados mesmo quando o bloco pesado era apenas ranking.
4. A Dashboard ainda mantinha código morto de logos remotos via Clearbit. Mesmo desativado por flag, era um risco de regressão e mantinha dependência externa desnecessária.
5. Existia um arquivo de teste em `src/main` (`Test.kt`) com chamada direta de URL; esse tipo de artefato não deve entrar em produção.
6. A checagem automática de atualização ainda podia competir com o boot se a rede estivesse lenta.
7. A agenda de dividendos dependia demais de resposta remota para ter algo útil a mostrar.

## Melhorias aplicadas

### 1. Consumo do mobile bootstrap no boot do APK

Foi adicionada a função `B3NetworkService.fetchMobileBootstrap(...)` e a rotina `warmMobileBootstrap(...)` no `PortfolioViewModel`.

Agora, após carregar a carteira local, o app tenta buscar em uma única chamada compacta:

- ativos da carteira;
- notícias gerais leves;
- sinalização de resposta parcial;
- cache/stale vindo do Proxy.

Se o bootstrap falhar, o app preserva snapshots locais e segue para revalidação em lote apenas dos tickers que ainda precisam de dados.

### 2. Snapshot persistente de notícias

Foram adicionadas rotinas para salvar e recuperar notícias em `SharedPreferences`:

- `saveNewsSnapshot(...)`
- `loadCachedNewsSnapshot(...)`
- `parseNewsItemsFromArray(...)`

Com isso, a tela de notícias pode abrir preenchida com o último feed conhecido antes da rede responder.

### 3. Separação de loading de rankings e analytics

`refreshLiveMarketRankings(...)` não força mais `portfolioAnalytics.isLoading = true` em refresh leve. Essa mudança evita que Agenda/Proventos/Insights exibam mensagens de carregamento indevidas quando apenas rankings estão sendo atualizados em background.

### 4. Fallback local de proventos

Foi adicionado `buildLocalDividendPreviewEvents(...)`, que cria uma prévia local com base no último provento conhecido de cada ativo. Quando o Proxy ainda não retorna agenda/histórico, a página não precisa ficar vazia: ela pode exibir uma estimativa local claramente marcada como tal.

### 5. Remoção definitiva de logos externos da Dashboard

O componente `AsyncCompanyLogo` foi simplificado para renderizar monograma local instantâneo. Foram removidos:

- `logo.clearbit.com`;
- `HttpURLConnection`;
- `java.net.URL` para logos;
- bitmap remoto;
- recomposição causada por download externo.

Essa é uma melhoria de UI/performance, não apenas de rede: listas e cards deixam de depender de chamadas externas durante composição.

### 6. Remoção de artefato de teste em produção

O arquivo `app/src/main/java/com/example/network/Test.kt` foi removido. Ele fazia chamada direta para URL externa e usava `main()` dentro de `src/main`, o que não é adequado para o APK final.

### 7. Checagem de atualização menos competitiva

`UpdateManager.checkForUpdate(...)` agora aplica timeout curto de 6 segundos quando a checagem é automática. A checagem manual continua completa. Além disso, a checagem automática permanece adiada no startup, para não competir com carteira, snapshots e bootstrap.

## Resultado esperado para o usuário

- A carteira aparece mais rápido com o último estado conhecido.
- Notícias e proventos têm mais chance de aparecer preenchidos mesmo com rede lenta.
- Agenda/Proventos não herdam loading indevido de rankings.
- Dashboard evita jank por logos remotos.
- Abertura do app tem menos concorrência entre update, carteira, rankings, notícias e analytics.
- O app passa a consumir o endpoint compacto de bootstrap do Proxy.

## Arquivos principais alterados

- `app/build.gradle.kts`
- `metadata.json`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/network/UpdateManager.kt`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- removido: `app/src/main/java/com/example/network/Test.kt`

## Validação executada neste ambiente

- Integridade do ZIP final: executada com `unzip -t`.
- Busca estática por Clearbit/URL direta na Dashboard: não há mais `logo.clearbit.com`, `HttpURLConnection` ou `java.net.URL` em telas principais.
- A build Android completa não pôde ser executada neste ambiente porque o Gradle Wrapper precisa baixar o Gradle em `services.gradle.org`, e o ambiente não possui resolução/acesso externo. Isso é limitação de ambiente, não uma falha confirmada do código.

## Próximos pontos recomendados

1. Medir em dispositivo real o tempo até o primeiro card de carteira renderizado.
2. Medir tempo até preenchimento de notícias e agenda com rede lenta.
3. Criar métrica interna de `bootCompleted`, `usingStaleData` e `bootstrap.partial` na tela de diagnóstico.
4. Evoluir logos para cache via Proxy apenas se forem realmente necessários.
5. Criar testes de regressão para impedir que novas telas voltem a disparar rede bloqueante na composição.
