# RELATÓRIO — Agenda de Dividendos no APK VALORAE v2.0.24

## Objetivo
Corrigir o modal Agenda de Dividendos para consumir corretamente eventos futuros e históricos entregues pelo VALORAE Proxy.

## Causa raiz encontrada
1. O APK não lia todos os aliases retornados pelo Proxy, especialmente `historico`, `history`, `agendaEvents`, `upcomingEvents` e `historyEvents`.
2. O fallback GET para carteira não solicitava `includeUpcoming=1` em todos os fluxos.
3. A UI escondia eventos reais quando o cálculo de elegibilidade do usuário retornava R$ 0,00, mesmo quando havia valor por ação/cota vindo do Proxy.

## Arquivos alterados
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/build.gradle.kts`
- `update.json`
- `docs/APK_BUILD_ATTEMPT_AGENDA_DIVIDENDOS_v2.0.24.log`

## Correções de consumo do Proxy
O APK agora consome:
- `events`
- `items`
- `dividends`
- `dividendos`
- `proventos`
- `historico`
- `history`
- `agenda`
- `agendaEvents`
- `upcomingEvents`
- `historyEvents`
- equivalentes em `data.*`

## Parâmetros reforçados
As chamadas de carteira/dividendos agora solicitam:
- `mode=complete`
- `complete=1`
- `includeHistory=1`
- `includeUpcoming=1`
- `limit=250`

## Correção visual
O modal Agenda de Dividendos não descarta mais evento real somente porque o usuário não está elegível ou porque a estimativa calculada ficou R$ 0,00. Nesses casos, o evento continua visível com o valor por ação/cota e fonte do Proxy.

## Estado vazio
Mensagem ajustada para indicar quando o Proxy não trouxe eventos confirmados:

`Sem eventos confirmados pelo VALORAE Proxy para os ativos da carteira.`

## Compatibilidade preservada
Mantidos:
- Home e card de rankings;
- aba Ativos/Carteira;
- Meus Ativos;
- Histórico de Compras;
- Nova Transação;
- gráficos canônicos;
- compatibilidade com `assetChartsCanonical` e `assetChartsCoverage`.

## Validação estática
Executado check estático manual:

```text
STATIC_APK_AGENDA_DIVIDENDOS_V224
OK versionName 2.0.24
OK versionCode 34
OK B3NetworkService agendaEvents
OK B3NetworkService upcomingEvents
OK B3NetworkService historyEvents
OK B3NetworkService historico
OK B3NetworkService includeUpcoming
OK B3NetworkService includeHistory
OK ChartsScreen DividendEventsList
OK sem botão Ver Ranking Completo
OK sem apk vazio
OK sem pasta apk duplicada
```

## Gradle
A compilação Gradle completa não foi concluída neste ambiente porque o wrapper tentou baixar o Gradle em `services.gradle.org` e o ambiente não resolveu DNS. O log está em:

`docs/APK_BUILD_ATTEMPT_AGENDA_DIVIDENDOS_v2.0.24.log`

## Versão final
- `versionName = "2.0.24"`
- `versionCode = 34`
