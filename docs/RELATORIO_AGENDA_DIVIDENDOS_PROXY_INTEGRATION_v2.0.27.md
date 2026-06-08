# Relatório — Correção profunda Agenda de Dividendos ↔ VALORAE Proxy — APK v2.0.27

## Diagnóstico

Após a refatoração da página de Proventos/Agenda, a integração ficou frágil em pontos importantes:

1. A página de Agenda dependia de `portfolioAnalytics.dividendEvents`, mas não forçava uma nova sincronização quando o usuário abria a página.
2. O APK montava a agenda principalmente a partir dos endpoints agregados de carteira. Quando esses endpoints retornavam estrutura diferente, a UI acabava indo para estado vazio.
3. O payload POST com `positions`, `firstPurchaseAt`, `includeHistory` e `includeUpcoming` já era montado no código, mas não era usado nas chamadas de dividendos. Isso reduzia a compatibilidade com o Proxy.
4. O parser aceitava muitos aliases, mas ainda podia falhar quando o Proxy retornava mapas por ticker ou wrappers mais profundos.
5. O card da Agenda na tela de Proventos filtrava apenas futuros. Se o Proxy retornasse histórico válido, o card podia parecer vazio.
6. O pacote recebido continha uma pasta duplicada `apk/` com versão antiga e APK vazio, que poderia fazer o Gemini/Android Studio abrir a versão errada.

## Arquivos alterados

- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/build.gradle.kts`
- `update.json`
- `metadata.json`

## Correções aplicadas no APK

### 1. Requisição ao Proxy

O APK agora tenta GET e POST nos endpoints agregados:

- `/api/v1/portfolio/next-dividends`
- `/api/v1/portfolio/dividends`

O POST envia `positions`, `tickers`, `mode=complete`, `complete=true`, `includeHistory=true`, `includeUpcoming=true` e `limit=250`.

### 2. Fallback por ativo

Quando o agregado não entrega eventos utilizáveis para todos os tickers, o APK reforça fallback por ativo:

- `/api/v1/asset/dividends`
- `/api/v1/asset/next-dividend`

As rotas por ativo agora também podem ser chamadas por GET e POST.

### 3. Parser recursivo

Foi adicionado parser defensivo para estruturas como:

- `data`
- `payload`
- `result`
- `response`
- `body`
- `portfolio`
- `asset`
- mapas por ticker como `PETR4: [...]` ou `HGLG11: { events: [...] }`
- `agendaEvents`
- `upcomingEvents`
- `historyEvents`
- `nextDividends`
- `futureEvents`
- `schedule`
- `calendar`
- `calendario`

### 4. Renderização da UI

Ao abrir as páginas `Agenda` ou `Proventos`, o app força uma sincronização da carteira com o VALORAE Proxy.

A Agenda agora:

- mostra carregamento enquanto consulta o Proxy;
- não declara vazio antes da tentativa de sincronização;
- exibe histórico quando não houver eventos futuros;
- mantém eventos reais mesmo sem valor estimado na carteira;
- mantém mensagem amigável quando não houver dados.

### 5. Limpeza de pacote

Foram removidos:

- pasta duplicada `apk/`;
- APKs antigos/vazios;
- diretórios de build antigos.

## Compatibilidade preservada

Confirmado no código:

- Home preservada;
- ranking compacto preservado;
- aba Ativos/Carteira preservada;
- Meus Ativos preservado;
- Histórico de Compras preservado;
- Nova Transação preservada;
- gráficos canônicos preservados;
- `assetChartsCanonical` preservado;
- `assetChartsCoverage` preservado;
- integração com `https://servidor-valorae.vercel.app` preservada.

## Versão

- `versionName = "2.0.27"`
- `versionCode = 37`

## Validação

Validação estática executada:

```text
STATIC_AGENDA_PROXY_INTEGRATION_v2.0.27
SUMMARY: OK
```

Tentativa de Gradle registrada em:

```text
docs/APK_BUILD_ATTEMPT_AGENDA_PROXY_INTEGRATION_v2.0.27.log
```

A compilação Gradle completa não pôde ser feita neste ambiente porque o wrapper tentou baixar `gradle-9.3.1-bin.zip` em `services.gradle.org` e o DNS/rede externa não estava disponível.
