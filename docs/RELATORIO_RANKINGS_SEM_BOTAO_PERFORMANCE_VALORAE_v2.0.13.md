# RELATÓRIO — VALORAE APK v2.0.13

## Objetivo
Remover totalmente o botão **"Ver Ranking Completo"** da tela inicial e executar nova auditoria defensiva dos rankings, desempenho, performance e responsividade do APK VALORAE.

## Resultado
O botão foi removido da Home. O card de rankings permanece compacto, com alternância interna entre **Maiores Altas** e **Maiores Baixas**, sem CTA extra para ranking completo.

O caminho para análises/rankings avançados permanece pela navegação inferior em **Insights**, evitando duplicidade visual na Home.

## Arquivos alterados

- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/build.gradle.kts`
- `update.json`
- `scripts/verify_valorae_rankings_no_button_performance_v213.py`
- `docs/STATIC_RANKINGS_NO_BUTTON_PERFORMANCE_V213.log`
- `docs/STATIC_DEEP_FINAL_AUDIT_V213.log`
- `docs/STATIC_LOADING_OPTIMIZATION_V213.log`
- `docs/STATIC_KOTLIN_SOURCE_BALANCE_V213.log`
- `docs/APK_BUILD_ATTEMPT_RANKINGS_NO_BUTTON_PERFORMANCE_v2.0.13.log`

## Correções aplicadas

### 1. Remoção do botão da Home
Removido o bloco clicável que exibia:

- `Ver Ranking Completo`
- seta de navegação
- callback `onOpenRankings`

Também foram removidas as passagens desse callback entre `DashboardScreen`, `HomeMarketMoversPreview` e `MainActivity`.

### 2. Ranking da Home mantido sem mau funcionamento
O card da Home continua exibindo:

- Maiores Altas
- Maiores Baixas
- preço/cotação quando disponível
- variação percentual real
- nome do ativo quando disponível
- estado de carregamento
- estado de erro com botão **Tentar Novamente**

### 3. Correção defensiva no enriquecimento de preços
O enriquecimento por cotação auxiliar agora preserva primeiro a variação que veio do ranking do Proxy/Investidor10.

Antes havia risco de uma cotação auxiliar sobrescrever a variação real do ranking. Agora:

1. usa a variação do ranking quando existir;
2. usa cotação auxiliar somente se o ranking não trouxer percentual;
3. evita tratar preço como variação usando limite de segurança para percentuais.

### 4. Melhoria de responsividade no card
As listas de altas e baixas passaram a ser memorizadas com `remember(ranking)`, reduzindo alocações durante recomposições da Home.

### 5. Estados defensivos preservados
Foram mantidos:

- skeleton antes da primeira tentativa;
- erro amigável após falha;
- retry conectado a busca completa;
- fallback leve do Proxy se modo completo falhar;
- timeouts delimitados;
- concorrência HTTP controlada.

## Contrato de rankings revisado
O APK continua aceitando aliases como:

- `rankings.altas`
- `rankings.baixas`
- `altas`
- `baixas`
- `highs`
- `lows`
- `gainers`
- `losers`
- `topGainers`
- `topLosers`
- `top_gainers`
- `top_losers`
- `maioresAltas`
- `maioresBaixas`
- `maiores_altas`
- `maiores_baixas`

## Performance e responsividade revisadas

- Mantidos timeouts delimitados para rankings, notícias, gráficos e diagnóstico.
- Mantida concorrência OkHttp moderada.
- Mantido carregamento escalonado no boot.
- Mantido cache/TTL para evitar rajadas no Proxy.
- Reduzidas recomposições no ranking da Home.
- Removido CTA desnecessário da Home, reduzindo UI e caminho de navegação duplicado.

## Validações executadas

```text
STATIC_RANKINGS_NO_BUTTON_PERFORMANCE_V213_OK
Valorae deep final audit OK
Valorae loading optimization audit OK
STATIC_KOTLIN_SOURCE_BALANCE_V213_OK
```

## Limitação de build
A tentativa de compilação Gradle completa não pôde finalizar neste ambiente porque o wrapper tentou baixar Gradle de `services.gradle.org`, mas o ambiente não possui acesso externo. O log foi salvo em:

`docs/APK_BUILD_ATTEMPT_RANKINGS_NO_BUTTON_PERFORMANCE_v2.0.13.log`

## Versão final

- `versionName = "2.0.13"`
- `versionCode = 23`
