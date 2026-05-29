# Auditoria profunda VALORAE Investidor/Portfolio — revisão 2026-05-27

## Escopo

Auditoria do projeto Android/Kotlin enviado em `investidor-portfolio (3).zip`, com foco em:

- divergência entre a página **Análise** funcionando e **Detalhes do ativo** sem informações;
- fluxo de dados do Valorae Proxy oficial;
- ações, FIIs, gráficos, indicadores, dividendos, carteira, observabilidade e build;
- compatibilidade com GitHub/Vercel free-only, sem banco externo, Redis, KV ou WebSocket obrigatório.

Proxy oficial mantido:

```text
https://servidor-valorae.vercel.app/api
```

## Diagnóstico principal

A falha mais importante não estava no Proxy, mas no app:

- A tela **Análise** recebe dados centralizados pelo `PortfolioViewModel` (`searchQueryResult`, `searchQueryHistory`, `assetChartBundles`).
- O modal **Detalhes do ativo** fazia novas chamadas próprias, isoladas, sem reaproveitar `cachedAssetData` e `assetChartBundles` já carregados.
- O modal também atualizava estados Compose (`mutableState`) dentro de `Dispatchers.IO`, o que pode gerar atualização inconsistente da UI.
- Se uma chamada falhasse, o modal poderia ficar com `assetData == null`, gráficos vazios ou estado de carregamento pouco útil, enquanto a página Análise continuava normal.

## Correções aplicadas

### 1. Dashboard agora entrega dados reais ao modal de Detalhes

Arquivo alterado:

```text
app/src/main/java/com/example/ui/screens/DashboardScreen.kt
```

Novos parâmetros adicionados:

```kotlin
cachedAssetData: Map<String, B3AssetData> = emptyMap()
assetChartBundles: Map<String, AssetChartBundle> = emptyMap()
isLoadingChartBundle: Boolean = false
onLoadAssetChartBundle: (String, String) -> Unit = { _, _ -> }
```

Ao abrir Detalhes, o Dashboard agora envia:

- `initialAssetData = cachedAssetData[ticker]`
- `initialChartBundle = assetChartBundles[ticker]`
- callback para carregar/atualizar bundle avançado do ativo.

Isso alinha **Detalhes do ativo** com a mesma fonte de estado que já fazia **Análise** funcionar.

### 2. MainActivity passou a coletar `cachedAssetData`

Arquivo alterado:

```text
app/src/main/java/com/example/MainActivity.kt
```

Foi adicionada a coleta:

```kotlin
val cachedAssetData by viewModel.cachedAssetData.collectAsStateWithLifecycle()
```

E esses dados agora são passados para `DashboardScreen`.

### 3. AssetDetailModal ficou resiliente e não bloqueia a tela inteira

Arquivo alterado:

```text
app/src/main/java/com/example/ui/components/AssetDetailModal.kt
```

Melhorias:

- recebe `initialAssetData` e `initialChartBundle` vindos do ViewModel;
- inicializa Detalhes com dados já carregados pela carteira/análise;
- continua tentando atualizar via Proxy em paralelo;
- não deixa mais a tela inteira bloqueada em loading;
- mostra banner discreto de atualização enquanto busca dados;
- se gráfico avançado falhar, mostra estado amigável em vez de sumir silenciosamente;
- infere FII também pelo ticker (`MXRF11`, `HGLG11`, etc.), não só pelo tipo salvo na transação;
- as mutações de estado Compose agora ocorrem depois do retorno de `withContext(Dispatchers.IO)`, no fluxo principal do `LaunchedEffect`.

### 4. Parser do Proxy melhorado para payloads reais

Arquivo alterado:

```text
app/src/main/java/com/example/network/B3NetworkService.kt
```

Melhorias:

- leitura de `results.financialSummary`;
- leitura de `results.financialSummary.ratiosChave`;
- fallback para `valorDeMercado`, `patrimonioLiquido` e `valorDeFirma` dentro de `financialSummary`;
- leitura de indicadores como `pl`, `pvp`, `roe`, `roic`, `roa`, `margemLiquida`, `evEbitda`, `evEbit`, `payout` também via `ratiosChave`;
- `parseAssetChartBundle` agora aceita `results.normalized` quando `root.normalized` não existir;
- removido `.put("profile", "portfolio")` duplicado no corpo de `/api/assets`.

### 5. Erro de sintaxe no Dashboard corrigido

Arquivo alterado:

```text
app/src/main/java/com/example/ui/screens/DashboardScreen.kt
```

Havia duplicidade no parâmetro `fiiValue` em `SegmentedAllocationBar`, o que pode quebrar a compilação:

```kotlin
fiiValue = ...
fiiValue = ...
```

Foi corrigido para apenas um `fiiValue`.

### 6. Script de verificação atualizado

Arquivo alterado:

```text
scripts/verify_valorae_proxy_integration.py
```

O script agora valida a implementação atual:

- `AssetChartBundlePanel` em Análise;
- `assetChartBundles` no fluxo de UI;
- `initialAssetData` e `onLoadChartBundle` em Detalhes;
- URL oficial do Proxy;
- headers e endpoints essenciais.

Resultado local:

```text
Valorae Proxy integration audit OK
```

## Limitações identificadas

Este ambiente não possui Gradle/Android SDK e o projeto não contém `gradlew`. Por isso:

- não foi possível recompilar o APK com segurança aqui;
- o APK antigo incluído em `.build-outputs/app-debug.apk` foi removido do ZIP corrigido para evitar instalação de uma build desatualizada;
- o Gemini/Android Studio deve sincronizar o Gradle, compilar e gerar um novo APK/AAB.

## Checklist para validação no Gemini/Android Studio

1. Sincronizar Gradle.
2. Build debug.
3. Abrir app.
4. Adicionar PETR4 como ação.
5. Abrir **Detalhes do ativo** pela carteira.
6. Confirmar que preço, DY, P/VP, ROE, ROIC, margens e gráfico histórico aparecem quando o Proxy retornar dados.
7. Ir em **Gráficos Avançados** dentro do modal.
8. Confirmar que o mesmo bundle usado em Análise aparece em Detalhes.
9. Buscar PETR4 na página **Análise**.
10. Buscar MXRF11 na página **Análise**.
11. Abrir MXRF11 em Detalhes pela carteira.
12. Confirmar FII com DY, P/VP, último rendimento, patrimônio, proventos e histórico quando disponíveis.
13. Confirmar que nenhum card deixa a tela branca quando o Proxy retorna warnings/campos ausentes.
14. Confirmar ausência de `https://valorae-proxy.vercel.app` como host ativo.

## Arquivos principais alterados

```text
app/src/main/java/com/example/MainActivity.kt
app/src/main/java/com/example/network/B3NetworkService.kt
app/src/main/java/com/example/ui/components/AssetDetailModal.kt
app/src/main/java/com/example/ui/screens/DashboardScreen.kt
scripts/verify_valorae_proxy_integration.py
```

## Resultado esperado após compilar

- A página **Análise** deve continuar funcionando.
- A página **Detalhes do ativo** deve receber o mesmo estado central do ViewModel.
- Detalhes não deve mais depender exclusivamente de chamadas novas e isoladas.
- Gráficos avançados devem aparecer em Detalhes quando já tiverem sido carregados ou quando a busca interna completar.
- Indicadores vindos do Proxy devem aparecer mesmo em payloads compactos que usam `financialSummary`.
- A UI deve ficar estável mesmo com payload `PARTIAL`, warnings ou blocos ausentes.
