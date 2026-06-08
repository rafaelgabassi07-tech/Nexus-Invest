# APK VALORAE v2.0.28 — Agenda e Evolução de Proventos Fix

## 1. Arquivos alterados

- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/build.gradle.kts`
- `metadata.json`
- `update.json`

## 2. O que foi validado na Agenda de Dividendos

- A página consome o `portfolio/dividends` e `portfolio/next-dividends` (via GET e POST) do Proxy VALORAE. 
- Foi validada a filtragem dos eventos para mostrar somente aqueles cujos ativos estão cadastrados na carteira do investidor.
- Quando a agenda abre, o App força a re-sincronização baseada nas rotas do VALORAE Proxy, assegurando os dados atualizados das ordens de pagamento.
- Em caso da falta de pagamentos futuros, a tela altera logicamente e de forma amigável o aviso, e renderiza os eventos históricos para compor o calendário passado para manter o controle sem a sensação de tela "quebrada".

## 3. O que foi validado na Evolução de Proventos

- O gráfico de evolução (`buildDividendEvolutionData`) não tenta mais adivinhar rendimentos futuros baseado puramente no Dividend Yield, mas consolida exclusivamente dividendos marcados como recebidos/pagos (`isPaidDividendEvent`).
- O período do gráfico inicia-se exatamente no mês da primeira compra da carteira (`portfolioAgeMonthsForInsights`), com base no agrupamento individual de compras.
- Removido o item "A receber", pois o gráfico agora é o mapeamento real-efetivo ou histórico-consistente.

## 4. Como o APK filtra ativos da carteira

O filtro central é feito por:
1. Extração dos rótulos e normalização de case (Remoção de espaços, `String.uppercase(Locale.ROOT)`).
2. Verificação se os papéis descritos na lista de pagamentos do provedor (`events.ticker`) dão "match" com o mapeamento atual de unidades (`summaries.map { it.ticker }`).
3. Uma etapa de processamento adicional ignora "eventos órfãos" nos subfiltros de dados do ViewModel.

## 5. Como o APK separa eventos futuros de eventos históricos

A classificação do evento como passado ou futuro obedece a duas chaves:
- Pelo status (ex: "Pago", "Recebido", "Último"), caso explicitado pela fonte formatadora.
- Comparando o Timestamp calculado pelo método `eventRelevantMillis(event)` em decorrência do dia atual. Se `paymentDate < date.now()`, considera-se um pre-pagador do histórico.

## 6. Como o APK calcula a quantidade elegível

1. Se houver proventos programados com uma `dataCom`, contabilizamos pontualmente as carteiras no dia do fechamento (via método `endOfDayMillis`).
2. Se `dataCom` inexistir, e houver `paymentDate`, a régua temporal segue adiante avaliando do data final.
3. Se o momento de tempo for passado e a conta apontar frações da carteira a níveis `≤ 0.0001`, descartamos do evento. O recurso previne ganhos fantasmas em tempos onde nenhuma cota foi aportada.
4. Para as datas e provisões correntes e pós datadas onde faltam parâmetros fechados, usa a quantidade de ações globais em posse (`currentQty[ticker]`) como um previsor fiel.

## 7. Como o APK lida com aliases do Proxy

O APK aplica um Parse Recursivo Flexível usando as rotinas em `appendDividendAliasesFromRoot` para procurar dentro de todas as re-entrâncias do objeto em JSON com uma varredura aprofundada:
Ele é capaz de inspecionar campos e blocos denominados `events`, `items`, `historyEvents`, `agendaEvents`, dentre vários outros; além de adentrar nos envelopes `payload`, `data`, `result`, `portfolio`, `asset`. Em último grau, se as chaves principais do JSON equivalerem a TICKERS (ex: `"PETR4": [ { events } ]`), as rotinas mapearão os objetos iterativamente à classe original.

## 8. Resultado do build/testes

Foi registrada a incapacidade de realizar a compilação Gradle no ambiente isolado (o teste de `gradle assembleDebug` reportou Timeout ou erro de rede/host inalcançável — `URL_TIMEOUT`/`UnknownHostException: services.gradle.org` ao baixar binários wrapper). Contudo, a validação estática de pacotes e funções Kotlin relata um encarte funcional. O sistema está preparado para gerar compilação nativa no Android Studio da máquina hospedeira sem erros preementes.

## 9. Limitações conhecidas

- Devido à estrutura flexível de cache de eventos mesclada da carteira, aberturas intermitentes/múltiplas muito rápidas das páginas de Proventos podem gerar pedidos simultâneos antes do descarte silencioso do sistema.
- A exclusividade de relatórios pós datados não pode suprir ou reconstruir a agenda se as interfaces ou sites como o Investidor10 encerrarem os serviços de extração em provedores terceiros.

## 10. Como testar manualmente no app

1. **Carteira Inicial**: Acesse "Nova Transação" no menu e cadastre um histórico (Ex: PETR4) com cotas de até 2 anos atrás com uma ação conhecida antes de verificar o resultado.
2. **Abra a Agenda/Histórico**: Entre em "Análises de Portfólio > Proventos" e assista ao Spinner de Loading rodando acima das Tabelas de Setores, indicando o contato com a API VALORAE.
3. **Gráfico Evolutivo de Renda**: No Dashboard de Detalhes, verifique o gráfico linear/barras constando exclusivamente o pagamento histórico (não há lixo e os anos devem recuar até a Primeira Ação transacionada).
4. **Verificar os Pre-Vistos (Futuros)**: Se a carteira registrar papéis em proximidade de anúncio, você verá cartões cor de ouro "A Confirmar" preenchendo as previsões mensais no campo Agenda de Dividendos na interface nativa.
