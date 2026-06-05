# RELATÓRIO TÉCNICO — VALORAE v2.0.5

## Correção urgente: gráficos DRE e Negócios

Versão atualizada:

- `versionCode = 15`
- `versionName = 2.0.5`

## Problema identificado

Os gráficos **DRE** e **Negócios** podiam aparecer vazios mesmo quando o VALORAE Proxy entregava dados, porque o app estava aceitando poucos formatos de resposta.

A implementação anterior esperava principalmente arrays diretos como:

- `chartsFinanceiros.receitasLucros` como `JSONArray`
- `chartsFinanceiros.evolucaoPatrimonio` como `JSONArray`
- `lucroCotacao` como objeto em formato específico
- `revenueSegment` / `negociosReceita` como `JSONObject` simples

Isso era frágil, porque o Proxy/Investidor10 pode entregar gráficos em vários formatos reais, por exemplo:

- `labels + datasets`
- `categories + series`
- `years + arrays nomeados`
- `data/items/points/values`
- objetos por ano, como `{ "2024": {...}, "2023": {...} }`
- blocos dentro de `appPayload.charts`
- blocos dentro de `appMobileSnapshot.charts`
- blocos dentro de `assetClassContract.groups.statements.fields.*.value`

## Arquivo principal alterado

```text
app/src/main/java/com/example/network/B3NetworkService.kt
```

## Correções implementadas

### 1. Parser robusto para DRE

Adicionados parsers para aceitar:

- `labels + datasets`
- `labels + series`
- `categories + series`
- `years + arrays nomeados`
- arrays de objetos por período
- objetos por ano
- wrappers `data`, `chart`, `payload`, `result`, `results`, `response`
- contratos `incomeStatement`, `dre`, `receitasLucros`, `revenueProfit`

Agora o app consegue montar `FinancialStatementPoint` a partir de campos como:

- `netRevenue`
- `net_revenue`
- `revenue`
- `receitaLiquida`
- `receita`
- `faturamento`
- `netProfit`
- `net_profit`
- `profit`
- `lucroLiquido`
- `lucro`
- `grossProfit`
- `lucroBruto`
- `cost`
- `custo`
- `ebitda`
- `ebit`

### 2. Parser robusto para Lucro x Cotação

Antes o app aceitava basicamente `quotes + profits` em formato específico.

Agora aceita:

- `labels + datasets`
- `labels + series`
- `quotes + profits`
- `cotacoes + lucros`
- arrays de objetos com preço/cotação e lucro
- wrappers `data`, `chart`, `payload`, `result`, `results`
- contratos `appPayload.charts.lucroCotacao`
- contratos `appMobileSnapshot.charts.profitVsQuote`
- contrato `assetClassContract.groups.statements.fields.lucroCotacao.value`

### 3. Parser robusto para evolução patrimonial

Agora o gráfico de patrimônio/ativo/passivo aceita:

- `evolucaoPatrimonio`
- `equityEvolution`
- `balancoPatrimonial`
- `balanceSheet`
- `totalAssets`
- `totalLiabilities`
- `netWorth`
- `patrimonioLiquido`
- `ativos`
- `passivos`

Fontes aceitas:

- `chartsFinanceiros`
- `sections.demonstrativos`
- `appPayload.charts`
- `appMobileSnapshot.charts`
- `assetClassContract.groups.statements.fields.*.value`

### 4. Parser robusto para Negócios

O gráfico **Faturamento por Negócio (%)** agora aceita:

- `negociosReceita`
- `segmentosReceita`
- `revenueSegment`
- `revenueByBusiness`
- `appPayload.charts.revenueSegment`
- `appPayload.charts.revenueByBusiness`
- `appPayload.charts.negociosReceita`
- `appMobileSnapshot.charts.revenueSegment`
- `appMobileSnapshot.charts.revenueByBusiness`
- `assetClassContract.groups.statements.fields.negociosReceita.value`
- `assetClassContract.groups.statements.fields.revenueSegment.value`
- `assetClassContract.groups.statements.fields.revenueByBusiness.value`

Também foi adicionada leitura para dados em:

- objeto simples;
- array;
- string resumida;
- `labels + data`;
- `labels + datasets`;
- `series + data`;
- objetos por ano.

### 5. Parser robusto para Regiões

O gráfico **Faturamento por Região (%)** também foi reforçado para aceitar:

- `regioesReceita`
- `geografiaReceita`
- `revenueGeography`
- `appPayload.charts.revenueGeography`
- `appPayload.charts.regioesReceita`
- `appMobileSnapshot.charts.revenueGeography`
- `assetClassContract.groups.statements.fields.regioesReceita.value`
- `assetClassContract.groups.statements.fields.revenueGeography.value`

## Funções novas adicionadas

Foram adicionadas funções privadas no `B3NetworkService.kt`, incluindo:

- `parseFirstFinancialStatementPoints`
- `parseFinancialStatementPointsFromAny`
- `parseFinancialStatementPointsFromArray`
- `parseFinancialStatementPointsFromObject`
- `parseFinancialStatementPointFromObject`
- `parseFinancialChartWithLabels`
- `parseFinancialNamedArrays`
- `parseFirstProfitVsQuotePoints`
- `parseProfitVsQuotePointsFromAny`
- `parseProfitVsQuotePointsFromArray`
- `parseProfitVsQuotePointsFromObject`
- `parseFirstBreakdownMap`
- `parseBreakdownMapFromAny`
- `financialFieldFromLabel`
- `applyFinancialField`
- `mergeFinancialStatementPoints`
- `mergeFinancialPoint`
- `extractYearFromLabel`
- `extractQuarterFromLabel`

## UI preservada

O componente abaixo foi preservado:

```text
AssetChartBundlePanel
```

As abas de ação continuam existindo:

- `Análise`
- `Dividendos`
- `Comparação`
- `DRE`
- `Negócios`

As funções preservadas:

- `StockDreTab`
- `StockBusinessTab`
- `AssetRevenueProfitChart`
- `AssetProfitVsQuoteChart`
- `AssetEquityEvolutionChart`
- `AssetBreakdownDonutChart`

## Política Proxy-only preservada

Nenhum gráfico foi simulado.

A correção apenas amplia a capacidade do app de ler os formatos reais retornados pelo Proxy.

Se o Proxy não retornar DRE ou Negócios, a UI continua mostrando estado vazio/indisponível.

Não foram adicionados:

- mock de DRE;
- mock de negócios;
- dados hardcoded;
- gráfico falso;
- projeção local;
- valuation local;
- fallback por preço médio.

## Auditoria criada

Novo script:

```text
scripts/verify_valorae_ui_v205.py
```

Ele valida:

- versão `2.0.5` e `versionCode 15`;
- presença das abas `DRE` e `Negócios`;
- uso de `AssetChartBundlePanel` na página Análise;
- uso de `AssetChartBundlePanel` no modal Detalhes;
- parser DRE com `labels/datasets/series`;
- parser DRE com arrays nomeados e objetos por período;
- leitura de `appPayload`, `appMobileSnapshot` e `assetClassContract`;
- parser de Lucro x Cotação;
- parser de Evolução Patrimonial;
- parser de Negócios com `revenueByBusiness` e `negociosReceita`;
- ausência de simulações e fallbacks proibidos.

Resultado:

```text
VALORAE UI v2.0.5 DRE/Negócios audit OK
```

## Validações executadas

### Auditoria UI v2.0.5

```bash
python3 scripts/verify_valorae_ui_v205.py
```

Resultado:

```text
OK - Versão do app atualizada para 2.0.5 / code 15
OK - Painel avançado mantém abas DRE e Negócios para ações
OK - Página Análise usa AssetChartBundlePanel
OK - Modal Detalhes usa AssetChartBundlePanel
OK - DRE aceita labels/datasets/series
OK - DRE aceita arrays nomeados e objetos por período
OK - DRE lê appPayload/appMobileSnapshot/assetClassContract
OK - Lucro x Cotação aceita labels/datasets e arrays quote/profit
OK - Evolução patrimonial aceita balance sheet em múltiplos contratos
OK - Negócios aceita objeto/array/string e contratos revenueByBusiness
OK - Sem fallback local proibido de preço médio como cotação
OK - Sem simulação de valuation/local para DRE ou Negócios
VALORAE UI v2.0.5 DRE/Negócios audit OK
```

### XML Android

```text
XML OK: 12 arquivos válidos
```

### Gradle

O Gradle foi testado, mas continua bloqueado pelo ambiente sem DNS externo:

```text
UnknownHostException: services.gradle.org
```

Portanto, não foi afirmado que o APK compilou localmente.

## Observação importante

Essa correção não cria dados. Ela torna o app mais compatível com os contratos reais do Proxy. Se mesmo após essa versão algum gráfico continuar vazio, isso indica que o Proxy não retornou aquele bloco para o ativo/período consultado, ou retornou em um formato ainda não mapeado. Nesse caso, o próximo diagnóstico deve coletar o JSON real do endpoint do ativo específico e comparar com os parsers adicionados.
