# PROMPT PARA GEMINI 3.1 PRO — VALORAE INVESTIDOR

Use o ZIP corrigido anexado como projeto principal.

Leia o relatório:

`docs/RELATORIO_AUDITORIA_COMPARACAO_INDICES_DESCRICOES_PERFORMANCE_VALORAE.md`

## Objetivo

Revisar, preservar e validar as correções aplicadas para:

1. Corrigir o gráfico **Comparação de Índices**.
2. Corrigir o gráfico **Retorno em Comparação com IFIX**.
3. Adicionar descrições úteis em todos os gráficos vindos do Investidor10/Valorae Proxy.
4. Melhorar desempenho, cache e redução de chamadas redundantes.
5. Garantir que Detalhes do Ativo, Análise e Insights continuem funcionando.

## Proxy obrigatório

Use somente:

`https://servidor-valorae.vercel.app/api`

Não use:

`https://valorae-proxy.vercel.app`

Não faça scraping direto no app Android.
Todas as informações externas devem vir pelo Valorae Proxy.

## Correções que devem ser preservadas

### B3NetworkService.kt

Preservar:

- uso de `/api/compare` como fallback;
- `fetchProxyComparisonSeries`;
- `parseComparisonSeriesFromObject`;
- `mergeComparisonSeries`;
- `returnSeriesFromPriceHistory`;
- parser flexível para séries e pontos;
- normalização `IBOVESPA → IBOV`;
- fallback IPCA via `/api/market/ipca`;
- cache por ticker/período/tipo;
- `OkHttpClient` com connection pool, timeout e retry.

### AssetCharts.kt

Preservar:

- `defaultChartDescription`;
- `comparisonWindowLimit`;
- `filterComparisonSeries`;
- descrições em `FilteredChartCard`;
- descrições em `ChartCardContainer`;
- `AssetIndexComparisonChart` sem divisão por zero;
- filtro `MAX` sem overflow;
- legenda mostrando último retorno de cada série;
- linha de referência em 0%.

### AssetDetailModal.kt

Preservar a otimização:

- não duplicar `fetchAssetChartBundle` quando o bundle já veio do estado central;
- só buscar histórico quando não houver pontos locais;
- só buscar dados de ativo quando o fallback local não tiver informação útil.

### PortfolioViewModel.kt

Preservar:

- `loadingChartBundleKeys`;
- prevenção de múltiplas cargas simultâneas do mesmo ticker/período.

## Corrigir o Gradle Wrapper antes do build

O ZIP anterior recebido tinha:

```text
gradle/wrapper/gradle-wrapper.jar = 0 bytes
```

Antes de compilar, substitua/regere o Gradle Wrapper pelo Android Studio ou por um wrapper válido.

Depois execute:

```bash
./gradlew clean assembleDebug
```

## Validação obrigatória no APK

Testar:

- PETR4 em Análise;
- PETR4 em Detalhes do Ativo;
- MXRF11 em Análise;
- MXRF11 em Detalhes do Ativo;
- Comparação com Índices;
- Retorno em Comparação com IFIX;
- filtros 1A, 3A, 5A e MAX;
- descrições dos gráficos;
- dividendos/proventos;
- indicadores fundamentalistas;
- Insights;
- Evolução de proventos;
- Rentabilidade vs IPCA+;
- Equilíbrio de Carteira;
- Agenda de dividendos;
- estados vazios sem tela branca;
- performance ao abrir e trocar abas.

## Critérios de aceite

A entrega só está correta se:

1. O app compilar sem erros.
2. Um APK debug novo for gerado.
3. A Comparação de Índices renderizar quando o Proxy entregar séries ou quando o fallback conseguir montar retorno histórico.
4. O filtro MAX não quebrar.
5. Todos os gráficos tiverem descrição.
6. Detalhes do Ativo receber dados corretamente.
7. Análise continuar funcionando.
8. Insights continuar funcionando.
9. Nenhuma tela ficar branca com payload parcial.
10. Não houver host antigo ativo.
11. O app continuar compatível com plano gratuito.
