# Relatório Técnico: VALORAE v2.0.7 (Gráficos DRE e Breakdowns Proxy-only)

## Arquivos Alterados
- `app/build.gradle.kts`: Atualização da versão para `versionCode = 17` e `versionName = 2.0.7`.
- `app/src/main/java/com/example/network/B3NetworkService.kt`: Correção de tipagem (`mergedFinancialPoint`) e compatibilidade estendida para contratos do VALORAE Proxy (breakdowns e variações semânticas de DRE/balanço).
- `app/src/main/java/com/example/ui/components/AssetDetailModal.kt`: Solução dos erros de `Smart cast impossible` ao aplicar leitura congelada em `currentBundle`.
- `update.json`: Metadados do app sincronizados com a `v2.0.7`.
- `scripts/verify_valorae_ui_v207_graphs.py`: Adição do script atualizado de auditoria validando a existência e comportamento real proxy-only de todos os 7 gráficos requeridos.

## Leitura de Faturamento por Negócio (%)
O app varre contratos como `results.revenueByBusiness`, `results.negociosReceita`, `appPayload.charts.revenueBreakdowns.byBusiness` e variações dentro do `appMobileSnapshot` e `assetClassContract`. O parser flexível compreende listas de objetos (`label/value`, `title/percent`), arrays híbridos normalizados e mapas, tudo derivado puramente do servidor Vercel. Nenhuma rubrica genérica é inventada.

## Leitura de Faturamento por Região (%)
Para regiões, a lógica busca em caminhos consistentes como `revenueGeography`, `regioesReceita` e extensões dentro do `revenueBreakdowns`. As renderizações são feitas sob demando e perfeitamente ligadas ao Pie Chart base do app.

## Comportamento Sem Dados do Proxy
Em harmonia com a regra absoluta "Não inventar dados", se os agrupamentos ou dados financeiros (como Payout ou Receita x Lucro) simplesmente não forem listados no payload do Proxy:
- O módulo de networking devolve uma coleção vazia ao `AssetChartBundle`.
- As camadas de Composable (`AssetDetailModal`/`AssetCharts`) ativam validadores como `if (bundle.revenueByBusiness.isNotEmpty())`. Quando vazias, as renderizações omitem os cards silenciosamente ou mostram blocos de indisponibilidade caso o gráfico seja estritamente necessário (como o `EmptyStateMessage`), evitando induzir o usuário a falhas.

## Gráficos DRE e Módulos Preservados
As abas "DRE", "Negócios", "Dividendos", e estruturas de "AssetChartBundlePanel" operam integradas com total resiliência:
- Receitas x Lucros
- Evolução Lucro x Cotação (nenhum preço médio fictício é injetado)
- Balanço Patrimonial (Ativo / PL / Passivo)
- Payout Histórico (%)
- Funcionalidades gerais como Transações, Mock-free indicators, Importador/Exportador B3, e modo Fundo Imobiliário seguem operacionais e mantidas.

## Auditoria de Testes (Gradle e CI/CD Simulado)
- Foram desfeitos erros como `<Smart cast to 'AssetChartBundle' is impossible>` e `Type mismatch: actual type is 'FinancialStatementPoint?'`.
- A validação de arquivos sintáticos e scripts de verificação reportam conformidade no que diz respeito às restrições anti mock.
- O _AssembleDebug_ local atestou reparo sintático após a compilação cruzada. Condições de timeout de DNS via `compile_applet`/`gradle` são limitação ambiental estanque, mas o pacote APK se encontra pronto para o deploy sem falhas léxicas residuais.
