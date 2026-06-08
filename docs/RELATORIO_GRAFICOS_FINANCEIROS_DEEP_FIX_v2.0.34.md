# RELATÓRIO — APK VALORAE v2.0.34 — Gráficos Financeiros Deep Fix

## Objetivo

Corrigir os gráficos das páginas **Detalhes do Ativo**, **Desempenho & Índices** e **Finanças & Balanço**, garantindo que o APK consuma corretamente o contrato do Proxy v21.12.70 e desenhe os dados sem distorção visual.

## Problemas corrigidos

### 1. Lucro x Cotação

Problema: o gráfico desenhava lucro e cotação em um único eixo absoluto. Como lucro vem em milhões/bilhões e cotação vem em reais, a linha de lucro ficava sempre no topo e a cotação no chão.

Correção:

- As duas séries agora são indexadas na própria base `100`.
- Os valores reais continuam no tooltip.
- Foi adicionada interação por toque/arraste.
- O gráfico mostra linha guia e tooltip com:
  - período
  - cotação real
  - lucro real
  - índice de cada série

### 2. Evolução Patrimonial

Problema: o APK aceitava bem `Patrimônio Líquido`, mas perdia séries chamadas `Ativo` no singular.

Correção:

- Aliases ampliados em `B3NetworkService.kt`.
- `Ativo`, `ativo`, `total_assets`, `assetsTotal` agora entram como `totalAssets`.
- `Passivo`, `passivo`, `total_liabilities`, `liabilitiesTotal` agora entram como `totalLiabilities`.
- `Patrimônio`, `patrimonio_total`, `shareholdersEquity` entram como `netWorth`.

### 3. Balanço Patrimonial

Problema: objetos diretos como `{ ativo, patrimonio, passivo }` não eram totalmente reconhecidos.

Correção:

- Parser direto e parser de arrays nomeados aceitam os novos aliases.
- Gráfico Ativo/PL/Passivo passa a receber os três campos quando o Proxy entrega esses dados.

### 4. Faturamento por negócio e região

Problema: o Proxy podia entregar `assetChartsCanonical.revenueBreakdowns`, mas o APK procurava principalmente caminhos antigos.

Correção:

- O APK agora busca também:
  - `canonicalCharts.revenueGeography`
  - `canonicalCharts.revenueByRegion`
  - `canonicalCharts.revenueSegment`
  - `canonicalCharts.revenueByBusiness`
  - `canonicalCharts.revenueBreakdowns.geography`
  - `canonicalCharts.revenueBreakdowns.byRegion`
  - `canonicalCharts.revenueBreakdowns.business`
  - `canonicalCharts.revenueBreakdowns.byBusiness`

### 5. Payout Histórico

Problema: alguns retornos canônicos deixavam o payout somente em `assetChartsCanonical.financial.payoutHistory`.

Correção:

- O APK agora também consome:
  - `canonicalCharts.financial.payoutHistory`
  - `canonicalCharts.payoutHistory`
  - aliases antigos já existentes.

### 6. Detalhes do Ativo

A modal continua usando o ViewModel como fonte única para o bundle pesado, preservando a correção de performance da versão v2.0.32. Esta versão não reintroduz chamadas duplicadas.

## Arquivos alterados

- `app/build.gradle.kts`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/components/AssetCharts.kt`
- `metadata.json`
- `update.json`
- `index.html`
- `docs/RELATORIO_GRAFICOS_FINANCEIROS_DEEP_FIX_v2.0.34.md`

## Versionamento

- `versionName = 2.0.34`
- `versionCode = 44`

## Validação

Tentativa de build executada:

```bash
./gradlew --no-daemon assembleDebug
```

Resultado no ambiente isolado:

```text
UnknownHostException: services.gradle.org
```

A falha ocorreu antes da compilação porque o Gradle Wrapper não conseguiu baixar a distribuição do Gradle. Não foi possível confirmar erro Kotlin neste ambiente.

## Compatibilidade

Usar com:

- Proxy: `21.12.70-valorae-financial-charts-deep-fix`
- APK: `2.0.34`
