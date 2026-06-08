# RELATÓRIO — Gráficos reais visíveis em Análise e Detalhes do Ativo — VALORAE v2.0.18

## Problema corrigido

A versão anterior tinha parsing/contrato para `assetChartsCanonical`, mas os gráficos canônicos não estavam suficientemente destacados nas telas usadas pelo usuário: página de **Análise** e modal **Detalhes do ativo**. Isso causava a percepção correta de que os gráficos novos não tinham sido implementados de forma visível.

## Arquivos alterados

- `app/src/main/java/com/example/ui/components/AssetCharts.kt`
- `app/src/main/java/com/example/network/AssetChartModels.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/build.gradle.kts`
- equivalentes dentro de `apk/app/...`

## O que foi implementado no APK

### 1. Painel canônico visível

Foi criado um painel explícito no `AssetChartBundlePanel` chamado **Gráficos reais do Investidor10**. Ele aparece no fluxo já usado pelas telas de Análise e Detalhes do ativo.

Esse painel renderiza diretamente os gráficos principais:

- Rentabilidade nominal vs real;
- Comparação com Índices;
- Comparação com commodity/setor quando existir;
- Receitas e Lucros;
- Evolução: Lucro x Cotação;
- Balanço Patrimonial: Ativo / PL / Passivo;
- Payout Histórico;
- Distribuições e Dividend Yield para FIIs.

### 2. Balanço Patrimonial corrigido

O gráfico de Balanço Patrimonial agora exige e mostra as três séries:

- Ativo;
- Patrimônio Líquido;
- Passivo.

Antes, a visualização usava basicamente Ativo/PL e não deixava o Passivo evidente.

### 3. Rentabilidade nominal vs real reforçada

O gráfico agora aceita períodos vindos de qualquer uma das séries. Se o Proxy entregar nominal e real em conjuntos diferentes, o APK alinha os períodos e exibe as duas barras corretamente, sem sumir quando uma série chega primeiro.

### 4. Payout Histórico enriquecido

O payout deixou de ser uma linha simples e passou a ser exibido como gráfico de barras percentual, com média, ano/período e tooltip.

### 5. Cobertura do Proxy respeitada

O modelo `AssetChartBundle` agora carrega:

- `coverageCaptured`;
- `coverageMissing`;
- `coverageNotApplicable`.

Se o Proxy informar que um gráfico está ausente/incompleto, o APK mostra aviso amigável em vez de criar gráfico sintético.

### 6. Timeouts ampliados para gráficos completos

As chamadas de gráficos completos agora usam timeout maior:

- modo completo: 25000 ms;
- fallback intermediário: 18000 ms.

## Fallbacks proibidos mantidos fora

O APK continua proibido de:

- criar rentabilidade real a partir da cotação;
- criar comparação com índices artificial;
- criar payout histórico a partir de payout atual isolado;
- montar balanço com zeros;
- misturar gráficos de ações e FIIs.

## Versionamento

- `versionName = "2.0.18"`
- `versionCode = 28`

## Validação estática

- Conferido uso de `assetChartsCanonical` e `assetChartsCoverage`.
- Conferida presença do painel canônico em `AssetChartBundlePanel`.
- Conferida renderização explícita dos gráficos solicitados.
- Conferido balanço de chaves Kotlin nos arquivos alterados.

## Observação

A compilação Gradle completa não pôde finalizar porque o ambiente tentou baixar o Gradle em `services.gradle.org`, sem resolução de DNS/rede externa.
