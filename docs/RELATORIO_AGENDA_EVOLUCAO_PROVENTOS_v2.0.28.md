# APK VALORAE v2.0.28 — Agenda e Evolução de Proventos

## Escopo
Revisão da página **Agenda de Dividendos** e da página **Evolução de Proventos** para consumir corretamente os eventos do VALORAE Proxy/Investidor10.

## Problema encontrado
A agenda dependia de eventos remotos, mas a cadeia podia declarar vazio quando o Proxy não parseava o layout atual do Investidor10. Além disso, a evolução de proventos ainda misturava janela curta e projeções, enquanto a regra correta é mostrar pagamentos recebidos desde a criação da carteira.

## Correções aplicadas
- App atualizado para `versionName 2.0.28` e `versionCode 38`.
- Evolução de Proventos agora usa `portfolioAgeMonthsForInsights(firstTransactionTime)` em vez de janela curta fixa de 6 meses.
- `buildDividendEvolutionData` passa a montar a série desde o primeiro mês da carteira, limitada a 120 meses por segurança visual/performance.
- Evolução exibe apenas eventos recebidos/pagos (`isPaidDividendEvent`) com valor elegível pela carteira.
- Eventos futuros ficam fora da evolução histórica e continuam reservados para a Agenda.
- Filtro de elegibilidade ajustado: evento passado sem posição na data-com/pagamento é descartado, em vez de usar quantidade atual como fallback histórico.
- Eventos futuros continuam podendo usar quantidade atual para estimar valor previsto da agenda.

## Validação
- Revisão estática dos pontos de consumo em `ChartsScreen.kt`, `PortfolioViewModel.kt` e `B3NetworkService.kt`.
- Tentativa de `./gradlew assembleDebug` não concluiu porque o ambiente isolado não conseguiu baixar o Gradle em `services.gradle.org` (`UnknownHostException`). Não houve falha de compilação Kotlin reportada; a falha foi de rede do wrapper.

## Compatibilidade
Requer Proxy `v21.12.64` ou superior para máxima confiabilidade da agenda Investidor10. O APK mantém tolerância aos aliases antigos do contrato (`events`, `dividends`, `proventos`, `agendaEvents`, `upcomingEvents`, `historyEvents`).
