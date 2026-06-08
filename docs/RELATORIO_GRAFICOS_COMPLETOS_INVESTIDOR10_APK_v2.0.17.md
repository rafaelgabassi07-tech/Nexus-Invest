# RELATÓRIO — Gráficos completos Investidor10 · APK VALORAE v2.0.17

## Objetivo

Sincronizar o APK VALORAE com o novo contrato completo de gráficos do VALORAE Proxy v21.12.62, evitando gráficos pobres, artificiais ou desconectados do Investidor10.

## Arquivos alterados

- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `apk/app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/build.gradle.kts`
- `apk/app/build.gradle.kts`
- `update.json`
- `apk/update.json`

## Melhorias aplicadas no parser

- O APK passa a consumir `assetChartsCanonical` como fonte prioritária.
- O APK passa a ler `assetChartsCoverage` para detectar gráficos visíveis no Investidor10 que ainda não vieram completos do Proxy.
- O APK adiciona aviso amigável quando o Proxy sinalizar gráfico visível sem série completa.
- O APK passa a consumir `fii.distribution12m` diretamente do contrato canônico.
- O APK mantém fallback para `sections.distribuicoes12m`, `results.distribuicoes12m` e `data.distribuicoes12m`.
- O APK passa a consumir `commodityComparison` canônico antes de qualquer fallback legado.
- O APK usa dados auxiliares apenas como fallback, sem substituir séries reais do Investidor10.

## Blocos suportados

### Ações

- Rentabilidade nominal.
- Rentabilidade real.
- Comparação com índices.
- Comparação com commodity/setor.
- Receitas e Lucros.
- Lucro x Cotação.
- Evolução do Patrimônio.
- Balanço Patrimonial.
- Payout histórico.
- Dividendos/proventos.
- Informações da empresa.

### FIIs

- Rentabilidade nominal.
- Rentabilidade real.
- Histórico de indicadores fundamentalistas.
- Comparação com índices.
- Comparação com outros FIIs, quando disponível.
- Distribuições nos últimos 12 meses.
- Dividend Yield histórico.
- Histórico de dividendos/distribuições.
- Informações do fundo.
- Lista/distribuição de imóveis/ativos, quando disponível.

## Política de dados

- O APK não deve fabricar gráficos quando o Investidor10 não entregar série suficiente.
- O APK deve mostrar estado vazio/aviso amigável em vez de gráfico enganoso.
- O Proxy é a fonte canônica; o APK apenas normaliza e renderiza.

## Validação estática

```bash
python3 scripts/verify_valorae_complete_i10_charts_v217.py
```

Resultado:

```text
STATIC_VALORAE_COMPLETE_I10_CHARTS_V217_OK
```

## Tentativa de build

A tentativa de Gradle foi registrada em:

```text
docs/APK_BUILD_ATTEMPT_GRAFICOS_COMPLETOS_INVESTIDOR10_v2.0.17.log
```

Ela não finalizou porque o ambiente não conseguiu resolver `services.gradle.org` para baixar o Gradle.

## Versão final

- `versionName = 2.0.17`
- `versionCode = 27`
