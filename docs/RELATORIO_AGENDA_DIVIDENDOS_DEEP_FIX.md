# RELATÓRIO — Correção profunda da Agenda de Dividendos VALORAE v2.0.26

## Diagnóstico

A agenda não montava informações mesmo quando o VALORAE Proxy fornecia dados por causa de uma combinação de falhas no APK:

1. O carregamento da carteira dependia quase exclusivamente dos endpoints agregados `/api/v1/portfolio/next-dividends` e `/api/v1/portfolio/dividends`.
2. Se esses endpoints retornassem estrutura diferente, `data` como array, `payload`, `result` ou aliases novos, o parser podia não coletar os eventos.
3. Não havia fallback profundo por ativo para `/api/v1/asset/dividends` e `/api/v1/asset/next-dividend` quando o agregado não vinha no formato esperado.
4. A UI filtrava a agenda com uma regra muito restritiva, focada em elegibilidade/valor estimado, e podia esconder evento real do Proxy quando a posição do usuário não permitia cálculo do valor estimado.
5. Eventos reais com datas/fonte, mas sem valor por ação/cota normalizado, podiam ser descartados antes da renderização.
6. Se não houvesse pagamento futuro, mas houvesse histórico real, a tela priorizava estado vazio em vez de mostrar o histórico disponível.

## Arquivos alterados

- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/build.gradle.kts`
- `update.json`

## Correções no parser/rede

- Adicionado suporte robusto a wrappers do Proxy:
  - `data`
  - `payload`
  - `result`
  - `response`
  - `body`
  - `portfolio`
  - `asset`
- Adicionado suporte quando `data`, `payload` ou `result` vierem como array direto.
- Adicionado coletor de aliases de dividendos:
  - `events`
  - `items`
  - `rows`
  - `dividends`
  - `dividendos`
  - `proventos`
  - `historico`
  - `history`
  - `agenda`
  - `agendaEvents`
  - `upcomingEvents`
  - `historyEvents`
  - `nextDividends`
  - `futureEvents`
  - `schedule`
  - `calendar`
  - `calendario`
- Adicionado fallback por ativo:
  - `/api/v1/asset/dividends`
  - `/api/v1/asset/next-dividend`
- Mantidos os endpoints agregados:
  - `/api/v1/portfolio/next-dividends`
  - `/api/v1/portfolio/dividends`

## Correções de campos

O APK agora aceita mais aliases de valor:

- `valueFormatted`
- `valorFormatado`
- `valorPorCotaFormatado`
- `valorPorAcaoFormatado`
- `cashAmount`
- `dividend`

Também mantém suporte a datas curtas e completas, já contempladas por `dd/MM/yy` e `dd/MM/yyyy`.

## Correções na UI

- Criada lógica específica `agendaDividendEvents`, menos restritiva que a elegibilidade de carteira.
- Eventos reais do Proxy aparecem mesmo quando:
  - o usuário não tinha posição elegível;
  - o valor estimado ficou zerado;
  - o evento veio com data/fonte, mas sem valor normalizado.
- Quando não há evento futuro, mas há histórico real, a tela mostra:
  - “Nenhum pagamento futuro encontrado. Exibindo histórico disponível do VALORAE Proxy.”
- A tabela de eventos não descarta mais eventos reais só por não ter valor estimado.
- Quando não há valor por cota, mostra “Valor/cota a confirmar” em vez de `R$ 0,0000/cota`.

## Compatibilidade preservada

Foram preservados:

- Home;
- card compacto de rankings;
- alternância ALTAS/BAIXAS;
- aba Ativos/Carteira;
- Meus Ativos;
- Histórico de Compras;
- Nova Transação;
- gráficos canônicos;
- `assetChartsCanonical`;
- `assetChartsCoverage`;
- compatibilidade com VALORAE Proxy.

## Parâmetros mantidos nas chamadas

- `mode=complete`
- `complete=1`
- `includeHistory=1`
- `includeUpcoming=1`
- `limit=250`

## Versionamento

- `versionName = "2.0.25"`
- `versionCode = 35`

## Limitação

A compilação Gradle completa não foi executada neste ambiente porque o Gradle depende de acesso externo a `services.gradle.org`. A validação aplicada foi estática e estrutural.
