# RELATÓRIO — Gráficos de Ativos End-to-End v2.0.31

## Objetivo
Corrigir e melhorar os gráficos das páginas **Detalhes do Ativo** e **Análise**, especialmente as abas:

- **Desempenho & Índices**
- **Finanças & Balanço**
- **Proventos & Payout**
- **Indicadores**
- **Perfil & Dados**

O APK v2.0.31 foi ajustado para consumir corretamente o contrato do **VALORAE Proxy v21.12.68**.

## Problemas tratados

1. Gráficos não montavam mesmo quando o Proxy retornava dados em aliases diferentes.
2. Alguns gráficos só aceitavam um formato específico de JSON.
3. A agregação de proventos falhava quando datas vinham em ISO `yyyy-MM-dd`.
4. Aba de Finanças escondia o gráfico de balanço quando uma das séries faltava.
5. As abas de Detalhes e Análise não estavam reaproveitando o mesmo contrato com a mesma robustez.
6. O app não deixava claro quando o dado era parcial ou quando o Investidor10 não entregava pontos suficientes.

## Arquivos principais alterados

- `app/src/main/java/com/example/network/AssetChartModels.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/components/AssetCharts.kt`
- `app/src/main/java/com/example/ui/components/AssetDetailModal.kt`
- `app/src/main/java/com/example/ui/screens/AnalysisScreen.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/build.gradle.kts`
- `metadata.json`
- `docs/RELATORIO_GRAFICOS_ATIVOS_END_TO_END_v2.0.31.md`

## Aba Desempenho & Índices

Agora a aba separa corretamente:

1. **Rentabilidade nominal vs real**
   - 1 mês
   - 3 meses
   - 1 ano
   - 2 anos
   - 5 anos
   - 10 anos

2. **Comparação com índices**
   - ativo
   - IBOV/IFIX quando aplicável
   - CDI/IPCA quando o Proxy entregar
   - séries equivalentes vindas do contrato canônico

3. **Comparação com commodity**
   - Exemplo: Brent para ativos relacionados a petróleo, quando existir no Investidor10/Proxy.

O app não usa mais painel genérico nessa aba. Ele tenta renderizar gráficos específicos e mostra estado parcial quando o Proxy não entrega série suficiente.

## Aba Finanças & Balanço

Agora a aba consome:

- `revenueProfit`
- `receitasLucros`
- `profitVsQuote`
- `lucroCotacao`
- `equityEvolution`
- `evolucaoPatrimonio`
- `balanceSheet`
- `balancoPatrimonial`
- `payoutHistory`
- `payoutHistorico`

Gráficos ajustados:

1. **DRE: Receitas x Lucros**
2. **Evolução Lucro x Cotação**
3. **Evolução Patrimonial**
4. **Balanço Patrimonial: Ativo / PL / Passivo**
5. **Payout Histórico**
6. **Faturamento por Negócio**
7. **Faturamento por Região**

O gráfico de balanço agora monta quando existem pelo menos duas séries reais entre:

- Ativo Total
- Patrimônio Líquido
- Passivo Total

Isso evita esconder o gráfico inteiro quando apenas uma linha não veio no payload.

## Aba Proventos & Payout

Além das páginas Agenda/Evolução, o detalhe individual do ativo agora também aceita histórico por ativo via:

- `assetChartsCanonical.dividendHistory`
- `company.dividendHistory`
- `fii.dividendHistory`
- `dividendHistory`
- `historicoDividendos`
- `dividendos.historico`
- `sections.dividendos`

Correção importante:

- Datas ISO `yyyy-MM-dd` agora entram na agregação anual/mensal.
- Datas brasileiras `dd/MM/yyyy` continuam aceitas.
- Valores com várias casas decimais são preservados.

## Aba Indicadores

A aba passa a usar:

- indicadores atuais do ativo;
- histórico de indicadores quando houver pelo menos dois pontos reais;
- cards informativos quando não existir série histórica suficiente.

## FII

Para FIIs, o app passa a aproveitar melhor:

- Distribuições nos últimos 12 meses;
- DY histórico;
- dividendos mensais/anuais;
- métricas patrimoniais;
- lista de imóveis;
- distribuição física por estado/segmento;
- comparação com IFIX/segmento quando disponível.

## Chamadas de rede

A chamada principal de bundle ficou mais completa:

```text
/api/v1/asset?ticker=XXXX&view=app&profile=max&mode=complete&complete=1&charts=full&includeCharts=1&chartSource=investidor10&internalApis=1
```

Há fallbacks mais leves quando a principal falha.

## Validação

Tentativa de build:

```bash
./gradlew --no-daemon assembleDebug
```

Resultado no ambiente isolado:

```text
UnknownHostException: services.gradle.org
```

A falha foi de rede do Gradle Wrapper. Não houve erro Kotlin confirmado pelo compilador neste ambiente.

## Versão

```text
versionName = 2.0.31
```

## Observação importante
O APK não fabrica gráficos. Se o Proxy informar que a seção existe no Investidor10, mas não trouxe pontos reais suficientes, o app mostra estado parcial/indisponível. Isso evita dados falsos.
