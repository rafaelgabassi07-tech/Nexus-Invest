# RELATÓRIO — Auditoria de Comparação de Índices, Descrições e Performance

Data da auditoria: 2026-05-27/28  
Projeto: VALORAE Investidor / Portfolio  
Proxy oficial: `https://servidor-valorae.vercel.app/api`

## Resumo executivo

Foi realizada uma nova auditoria no código recebido, com foco nos pontos informados pelo usuário:

1. Gráfico **Comparação de Índices** não funcionando corretamente.
2. Gráficos vindos do Investidor10 aparecendo sem descrição.
3. Correções de desempenho/performance e redução de chamadas redundantes.
4. Continuidade das correções anteriores para **Detalhes do Ativo**, **Análise**, gráficos, FIIs, ações e estados parciais.

Foram encontradas falhas reais na implementação atual, principalmente na lógica de comparação de índices e no comportamento de filtros dos gráficos.

## Problemas encontrados

### 1. Risco de crash no filtro MAX da Comparação de Índices

Em `AssetCharts.kt`, os gráficos comparativos usavam lógica semelhante a:

```kotlin
val filterYears = filter.replace("A", "").toIntOrNull() ?: Int.MAX_VALUE
s.points.takeLast((filterYears * 12).coerceAtMost(s.points.size))
```

Quando o filtro era `MAX`, `filterYears` virava `Int.MAX_VALUE`. Multiplicar isso por `12` pode causar overflow de inteiro. Em Kotlin, isso pode gerar quantidade negativa e quebrar `takeLast()`.

### 2. Filtro temporal incorreto

O filtro assumia aproximadamente `12 pontos por ano`, mas as séries vindas de histórico de preço normalmente podem ser diárias, semanais ou mensais. Isso fazia o filtro cortar dados de forma incorreta.

### 3. Parser limitado para Comparação de Índices

O parser só procurava alguns caminhos:

- `sections.comparacaoIndices`
- `sections.rentabilidadeChart`

Mas o Proxy/Investidor10 pode entregar comparações em outros formatos, por exemplo:

- `results.comparacaoIndices`
- `results.rentabilidadeChart`
- `results.indexComparison`
- `results.indicesComparison`
- `results.rentabilidade`
- `root.comparison`
- `root.compare`
- `series[]`
- `items[]`
- arrays por nome: `ativo`, `ibov`, `ifix`, `cdi`, `ipca`

### 4. Ausência de fallback para `/api/compare`

Mesmo o Proxy anunciando suporte a `/api/compare`, o app não usava essa rota como fallback quando a página de ativo não trazia a comparação pronta.

### 5. Gráficos do Investidor10 sem descrição

Os cards de gráficos tinham somente título. Isso deixava a UI pobre e dificultava o entendimento de gráficos como:

- Comparação com Índices
- Retorno em Comparação com IFIX
- Rentabilidade Nominal vs Real
- Indicadores Fundamentalistas
- DRE: Receitas x Lucros
- Evolução Lucro x Cotação
- Balanço Patrimonial
- Payout Histórico
- Faturamento por Negócio
- Faturamento por Região
- Métricas Patrimoniais

### 6. Chamadas redundantes no Detalhes do Ativo

`AssetDetailModal.kt` disparava `onLoadChartBundle()` e também fazia chamada local para `fetchAssetChartBundle()` no mesmo efeito. Isso podia duplicar chamadas, piorar latência e aumentar o risco de estado visual inconsistente.

### 7. Gradle Wrapper inválido no ZIP recebido

O arquivo abaixo veio com tamanho zero:

```text
gradle/wrapper/gradle-wrapper.jar
```

Por isso o comando abaixo falha antes mesmo de iniciar o build:

```bash
./gradlew --version
```

Erro observado:

```text
Error: Invalid or corrupt jarfile gradle/wrapper/gradle-wrapper.jar
```

## Correções aplicadas

### B3NetworkService.kt

Foram adicionadas melhorias na camada de rede e parsing:

- `OkHttpClient` com `ConnectionPool`.
- Dispatcher com limites de requisições simultâneas.
- `callTimeout` para evitar chamadas presas.
- `retryOnConnectionFailure(true)`.
- Parser robusto para séries de comparação.
- Normalização de nomes de séries:
  - `IBOVESPA` → `IBOV`
  - `^BVSP` → `IBOV`
  - `IFIX_PROXY` → `IFIX`
  - `ativo`, `asset`, `ticker` → ticker do ativo
- Parser flexível para pontos de gráfico:
  - data: `label`, `dateLabel`, `date`, `data`, `month`, `period`, `x`
  - valor: `returnPercent`, `accumulatedPercent`, `variationPercent`, `percent`, `percentage`, `valuePercent`, `value`, `valor`, `close`, `price`, `preco`, `y`
- Suporte a múltiplas formas de resposta:
  - `series`
  - `items`
  - `datasets`
  - `comparisons`
  - `indices`
  - arrays nomeados por índice
- Fallback para `/api/compare`.
- Fallback por histórico de preço para montar retorno acumulado.
- Fallback para IPCA usando `/api/market/ipca`.
- Merge de séries, preferindo a série com maior quantidade de pontos.
- Cache de comparação por ticker/período/tipo.

### AssetCharts.kt

Foram corrigidos e melhorados:

- Remoção da lógica de filtro com risco de overflow.
- Novo helper `comparisonWindowLimit()`.
- Novo helper `filterComparisonSeries()`.
- Gráfico de comparação agora aceita séries diárias com janelas aproximadas:
  - 1A → até 260 pontos
  - 3A → até 780 pontos
  - 5A → até 1300 pontos
  - 10A → até 2600 pontos
  - MAX → todos os pontos
- `AssetIndexComparisonChart()` agora:
  - filtra pontos inválidos;
  - evita divisão por zero;
  - desenha linha de referência em 0%;
  - mostra último retorno percentual na legenda;
  - limita a 6 séries visíveis para preservar legibilidade;
  - trata séries vazias sem crash.
- `FilteredChartCard()` passou a aceitar descrição.
- `ChartCardContainer()` passou a aceitar descrição.
- Foi criado `defaultChartDescription()` para descrever automaticamente todos os principais gráficos do Investidor10/Proxy.

### AssetDetailModal.kt

Foi reduzida a duplicação de chamadas:

- Só chama `onLoadChartBundle()` quando o bundle ainda não existe.
- Só busca asset remoto quando os dados atuais não têm utilidade.
- Só busca histórico quando não há pontos locais.
- Só busca `AssetChartBundle` localmente quando ele ainda não foi recebido pelo estado central.

Isso melhora performance e reduz risco de estado inconsistente no detalhe.

### PortfolioViewModel.kt

Foi adicionado controle simples de carregamento em andamento:

- `loadingChartBundleKeys`
- Evita múltiplas cargas simultâneas do mesmo ticker/período.
- `isLoadingChartBundle` agora considera cargas ativas reais.

### Testes

Foi adicionado teste para parser de comparação de índices com múltiplos formatos, cobrindo:

- PETR4
- IBOVESPA normalizado para IBOV
- IPCA vindo como `data`/`x`/`y`

### Script de auditoria

`scripts/verify_valorae_proxy_integration.py` foi atualizado para validar:

- `defaultChartDescription`
- `filterComparisonSeries`
- `comparisonWindowLimit`
- `AssetIndexComparisonChart`
- `/api/compare`
- `fetchProxyComparisonSeries`
- `parseComparisonSeriesFromObject`
- `mergeComparisonSeries`
- ausência de filtros antigos com risco de overflow

## Validação executada neste ambiente

Executado com sucesso:

```bash
python3 scripts/verify_valorae_proxy_integration.py
```

Resultado:

```text
Valorae Proxy integration audit OK
```

Também foi feita validação estrutural de chaves/parênteses nos arquivos modificados:

- `B3NetworkService.kt`
- `AssetCharts.kt`
- `AssetDetailModal.kt`
- `PortfolioViewModel.kt`

Resultado: sem desequilíbrio estrutural detectado.

## Bloqueio de build/APK neste ambiente

Não foi possível gerar APK novo aqui porque o Gradle Wrapper recebido está inválido:

```text
gradle/wrapper/gradle-wrapper.jar = 0 bytes
```

Com isso, `./gradlew` falha antes de baixar ou executar Gradle.

O APK antigo foi removido do pacote corrigido para evitar instalar uma build desatualizada.

## Arquivos alterados

- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/components/AssetCharts.kt`
- `app/src/main/java/com/example/ui/components/AssetDetailModal.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/test/java/com/example/B3NetworkServiceParserTest.kt`
- `scripts/verify_valorae_proxy_integration.py`
- `docs/RELATORIO_AUDITORIA_COMPARACAO_INDICES_DESCRICOES_PERFORMANCE_VALORAE.md`

## Validações obrigatórias no Android Studio / Gemini

Antes de compilar, substituir/regenerar o Gradle Wrapper inválido.

Depois executar:

```bash
./gradlew clean assembleDebug
```

Validar no APK:

1. PETR4 → Análise.
2. PETR4 → Detalhes do Ativo.
3. MXRF11 → Análise.
4. MXRF11 → Detalhes do Ativo.
5. Comparação com Índices.
6. Retorno em Comparação com IFIX.
7. Troca dos filtros 1A, 3A, 5A e MAX.
8. Descrições aparecendo nos gráficos.
9. Gráficos do Investidor10 sem tela branca.
10. Estados parciais com mensagem amigável.
11. Performance ao abrir Detalhes do Ativo.
12. Ausência de chamadas duplicadas excessivas.
