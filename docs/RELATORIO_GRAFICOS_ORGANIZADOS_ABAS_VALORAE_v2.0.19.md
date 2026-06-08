# Relatório — Gráficos organizados por abas/categorias — VALORAE v2.0.19

## Objetivo

Remover a abordagem de painel único para gráficos do Investidor10 e acomodar cada gráfico em sua categoria correta nas telas de Análise e Detalhes do Ativo.

## Mudanças aplicadas

- Removida a chamada ao painel genérico `Investidor10CanonicalChartsSection`.
- Os gráficos canônicos continuam usando `assetChartsCanonical` e `assetChartsCoverage`, mas agora aparecem dentro das abas corretas.
- A tela de Análise agora separa as abas em:
  - Resumo;
  - Desempenho & Índices;
  - Finanças & Balanço;
  - Proventos & Payout;
  - Indicadores;
  - Perfil & Dados.
- O modal Detalhes do Ativo agora separa as abas em:
  - Resumo;
  - Desempenho & Índices;
  - Finanças & Balanço;
  - Proventos & Payout;
  - Indicadores;
  - Perfil & Dados;
  - Minha Custódia;
  - Transações.

## Onde cada gráfico deve aparecer

### Desempenho & Índices

- Rentabilidade nominal vs real.
- Comparação com Índices.
- Comparação com commodity/setor, quando existir.
- Comparação com outros FIIs, quando for FII.

### Finanças & Balanço

- Receitas e Lucros.
- Evolução: Lucro x Cotação.
- Evolução Patrimonial.
- Balanço Patrimonial: Ativo / PL / Passivo.
- Payout Histórico.
- Faturamento por segmento e região, quando existir.
- Patrimônio e ativos/imóveis, quando for FII.

### Proventos & Payout

- Dividendos.
- Dividend Yield histórico.
- Distribuições de FIIs.
- Eventos de distribuição.

### Indicadores

- Histórico de Indicadores Fundamentalistas.
- Indicadores gerais vindos do Proxy e do bundle oficial.

## Regras mantidas

- O APK não cria gráfico falso.
- O APK não usa fallback artificial para comparação com índices.
- O APK não cria rentabilidade real a partir da cotação.
- O APK só mostra Balanço Patrimonial quando houver Ativo, PL e Passivo reais.
- O APK só mostra Payout Histórico quando houver série histórica real.
- O usuário não vê JSON bruto.

## Versão

- versionName: 2.0.19
- versionCode: 29
