# RELATÓRIO — Revisão final dos gráficos por abas · APK VALORAE v2.0.20

## Objetivo
Revisar o que foi feito nos gráficos do APK VALORAE e corrigir lacunas que impediam os gráficos canônicos do Investidor10 de aparecerem corretamente nas telas de Análise e no modal Detalhes do Ativo.

## Arquivos principais alterados
- `app/src/main/java/com/example/network/AssetChartModels.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/components/AssetCharts.kt`
- `app/src/main/java/com/example/ui/components/AssetProxySections.kt`
- `app/build.gradle.kts`

## Correções aplicadas
1. Separado `balanceSheet` de `equityEvolution` no modelo do APK.
2. O parser agora lê `financial.balanceSheet` e `financial.equityEvolution` separadamente quando o Proxy retornar `assetChartsCanonical`.
3. O gráfico **Evolução Patrimonial** agora é exibido explicitamente na aba **Finanças & Balanço**.
4. O gráfico **Balanço Patrimonial: Ativo/PL/Passivo** agora usa a série específica `balanceSheet`, exigindo Ativo, PL e Passivo reais.
5. O gráfico **Payout Histórico** foi removido da aba **Finanças & Balanço** e movido para **Proventos & Payout**.
6. Adicionada exibição de **Histórico de Indicadores Fundamentalistas** na aba **Indicadores**, usando `indicatorHistory`.
7. Corrigido erro estrutural duplicado em `AssetProxySections.kt` que podia impedir compilação (`shape` repetido no `Surface`).
8. Mantido o painel genérico fora das telas. Os gráficos ficam nas abas/categorias corretas.

## Organização final por categoria

### Desempenho & Índices
- Rentabilidade nominal vs real.
- Comparação com Índices.
- Comparação com commodity/setor quando houver.
- Comparação com outros FIIs quando houver.

### Finanças & Balanço
- Receitas e Lucros.
- Evolução: Lucro x Cotação.
- Evolução Patrimonial.
- Balanço Patrimonial: Ativo / PL / Passivo.
- Faturamento por segmento/região quando houver.
- Patrimônio e imóveis/ativos para FIIs.

### Proventos & Payout
- Dividendos/Proventos.
- Dividend Yield histórico.
- Distribuições de FIIs.
- Payout Histórico.
- Eventos de distribuição.

### Indicadores
- Indicadores atuais.
- Histórico de Indicadores Fundamentalistas.

## Regras mantidas
- O APK não cria gráficos artificiais.
- O APK não monta gráfico com série insuficiente.
- O APK não mostra JSON bruto.
- O APK respeita `assetChartsCanonical` e `assetChartsCoverage`.
- Se a série real não vier do Proxy, a UI mostra estado vazio amigável.

## Validação executada
Validação estática local:

```text
STATIC_APK_GRAFICOS_FINAL_V220_OK
```

Tentativa de compilação Gradle:

```text
./gradlew --offline :app:compileDebugKotlin
```

Resultado: não finalizou porque o Gradle Wrapper tentou baixar o Gradle em `services.gradle.org`, mas o ambiente estava sem DNS/rede externa. Log salvo em:

```text
docs/APK_BUILD_ATTEMPT_GRAFICOS_REVISAO_FINAL_v2.0.20.log
```

## Versão
- `versionName = "2.0.20"`
- `versionCode = 30`
