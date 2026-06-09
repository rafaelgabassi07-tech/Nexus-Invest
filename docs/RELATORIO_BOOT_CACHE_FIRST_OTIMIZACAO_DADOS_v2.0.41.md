# VALORAE APK v2.0.41 — Boot cache-first e otimização de dados

## Objetivo
Reduzir delay percebido na abertura do app e evitar que o usuário veja páginas ou informações vazias enquanto o Proxy ainda está aquecendo dados.

## Melhorias aplicadas

1. **Boot stale-first/cache-first no PortfolioViewModel**
   - A carteira local é renderizada primeiro com dados da Room.
   - Antes da rede, o app carrega snapshots persistidos pelo `B3NetworkService`.
   - As cotações ao vivo passam a revalidar os snapshots, não bloquear a UI.

2. **Timeout local para cotações em lote**
   - A atualização automática de preços agora usa deadline curto no boot.
   - Atualização manual mantém prazo maior, mas preserva dados locais se exceder o limite.

3. **Estado de boot global**
   - Adicionado `AppBootState` para acompanhar etapas: carteira local, snapshots, preços ao vivo, rankings, notícias e analytics.
   - O app pode evoluir para indicadores visuais mais precisos sem mudar novamente a arquitetura.

4. **Analytics local antes do Proxy**
   - Mantida a análise local otimista antes das chamadas remotas.
   - Reduzidos budgets de rede para analytics, histórico, IPCA, dividendos e rankings quando a chamada não é manual.

5. **Proventos/Agenda menos bloqueantes**
   - As chamadas para dividendos enviam `timeoutMs`, `agendaTimeoutMs` e `routeDeadlineMs` para o Proxy.
   - Isso evita que uma agenda pesada segure a montagem dos cards locais.

6. **Notícias sem tela vazia**
   - A tela de notícias deixou de exibir apenas um spinner central em tela cheia.
   - Agora mantém cabeçalho, busca e categorias visíveis, com indicador linear e mensagem de sincronização.

7. **Logos externos fora do caminho crítico**
   - O carregamento direto via Clearbit foi desativado no boot.
   - O app passa a usar monograma local instantâneo, evitando dezenas de requisições externas durante composição da Dashboard.

8. **Remoção de force refresh automático em navegação**
   - A abertura de Insights/Agenda/Proventos não força mais reload pesado por padrão.
   - O refresh manual continua disponível no fluxo global.

## Validação
- Tentativa de `./gradlew :app:assembleDebug --no-daemon` não pôde ser concluída porque o ambiente não tem acesso ao domínio `services.gradle.org` para baixar o Gradle Wrapper.
- Foram executadas verificações estáticas locais e revisão dos arquivos alterados.

## Arquivos principais alterados
- `app/build.gradle.kts`
- `metadata.json`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/screens/NewsScreen.kt`
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/src/main/java/com/example/MainActivity.kt`
