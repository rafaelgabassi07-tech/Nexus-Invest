# Integração APK VALORAE ↔ VALORAE Proxy v21.12.52

> Base solicitada: VALORAE Proxy v21.12.51.  
> Arquivo recebido nesta rodada: `valorae-proxy-v21.12.52-news-reliability-upgrade.zip`.  
> A implementação usou a v21.12.52 por ser a versão anexada e mais recente, preservando os contratos `/api/v1/...` esperados para a v21.12.51.

## 1. Auditoria técnica do APK

### Tecnologia identificada

- Android nativo.
- Kotlin.
- Jetpack Compose.
- Room/SQLite para transações locais.
- OkHttp e parsing JSON manual via `org.json` na camada `B3NetworkService`.
- MVVM com `PortfolioViewModel`.
- Tema claro/escuro já existente em `ui/theme` e `ThemePreferences`.

### Arquitetura localizada

- Entrada do app: `app/src/main/java/com/example/MainActivity.kt`.
- Camada de rede principal: `app/src/main/java/com/example/network/B3NetworkService.kt`.
- Estado/regras de carteira: `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`.
- Banco local e transações: `app/src/main/java/com/example/data/`.
- Telas principais:
  - `DashboardScreen.kt`.
  - `PortfolioDetailScreen.kt`.
  - `ChartsScreen.kt`.
  - `NewsScreen.kt`.
  - `SettingsScreen.kt`.
- Detalhe de ativo: `ui/components/AssetDetailModal.kt`.

### Situação antes da integração

O app já possuía uma base boa para integração com o Proxy: cache em memória, chamadas em lote, gráficos, notícias, tema claro/escuro e fallback direto bloqueado por padrão. A principal lacuna era contratual: várias chamadas ainda usavam rotas legadas `/api/...`, a configuração de URL não expunha todos os nomes esperados e o tratamento de `PARTIAL` não preservava explicitamente o último snapshot bom em cache local persistente.

## 2. Auditoria técnica do Proxy recebido

### Versão enviada

- Pacote analisado: `valorae-proxy-v21.12.52-news-reliability-upgrade.zip`.
- O Proxy mantém router central em `routes/_router.js`.
- O prefixo `/api/v1/...` é aceito pelo router, que remove o prefixo de versão e despacha para as rotas internas.
- `lib/Valorae-engine.js` foi preservado e não foi desmembrado.

### Endpoints confirmados no router

- `/api/v1/ready`.
- `/api/v1/release/readiness`.
- `/api/v1/source/status`.
- `/api/server/metrics`.
- `/api/v1/asset`.
- `/api/v1/assets`.
- `/api/v1/news`.
- `/api/v1/asset/history`.
- `/api/v1/asset/dividends`.
- `/api/v1/market/indices`.
- `/api/v1/market/ipca`.
- `/api/v1/portfolio/analyze`.
- `/api/v1/portfolio/history`.
- `/api/v1/portfolio/next-dividends`.
- `/api/v1/integration/manifest`.
- `/api/v1/integration/sdk`.
- `/api/v1/integration/prompts`.

## 3. Implementação aplicada no APK

### Configuração de URL

Arquivos ajustados:

- `app/build.gradle.kts`.
- `gradle.properties`.
- `.env.example`.

Variáveis configuradas:

```properties
VALORAE_API_BASE_URL=https://servidor-valorae.vercel.app
VALORAE_PROXY_BASE_URL=https://servidor-valorae.vercel.app
VALORAE_PUBLIC_BASE_URL=https://servidor-valorae.vercel.app
```

A prioridade agora é:

1. `VALORAE_API_BASE_URL`.
2. `VALORAE_PROXY_BASE_URL`.
3. `VALORAE_PUBLIC_BASE_URL`.
4. `VERCEL_BACKEND_URL` legado.
5. fallback seguro para `https://servidor-valorae.vercel.app`.

### Camada Network/API

Arquivo principal alterado:

- `app/src/main/java/com/example/network/B3NetworkService.kt`.

Mudanças principais:

- Migração das chamadas centrais para `/api/v1/...`.
- Headers de integração adicionados:
  - `x-valorae-app`.
  - `x-valorae-client`.
  - `x-valorae-build`.
  - `x-valorae-platform`.
- Timeouts e perfis alinhados aos contextos de uso:
  - carteira/listas: `profile=portfolio&timeoutMs=800`.
  - detalhe de ativo: `profile=turbo&timeoutMs=800`.
  - fallback rápido: `profile=fast&timeoutMs=500`.
  - análise/gráficos profundos: `profile=max&complete=1` com fallback para `turbo` e `fast`.
- Métricas internas de tempo de resposta, erros recentes, respostas parciais e snapshots bons.

### Endpoints integrados

- Saúde/readiness:
  - `GET /api/v1/ready`.
  - `GET /api/v1/release/readiness`.
  - `GET /api/v1/source/status`.
  - `GET /api/server/metrics`.
- Ativo individual:
  - `GET /api/v1/asset?ticker=...&view=app&profile=turbo&timeoutMs=800`.
  - fallback: `profile=fast&timeoutMs=500`.
- Carteira/lista:
  - `POST /api/v1/assets`.
  - fallback `GET /api/v1/assets?tickers=...&view=app&profile=portfolio&timeoutMs=800`.
- Gráficos e profundidade:
  - `GET /api/v1/asset?profile=max&complete=1`.
  - `GET /api/v1/asset/history`.
- Dividendos:
  - `GET /api/v1/asset/dividends`.
  - `POST/GET /api/v1/portfolio/next-dividends`.
- Notícias:
  - `GET /api/v1/news`.
- Mercado/carteira auxiliares:
  - `GET /api/v1/market/indices`.
  - `GET /api/v1/market/ipca`.
  - `POST /api/v1/portfolio/analyze`.
  - `POST /api/v1/portfolio/history`.
- Integração:
  - `GET /api/v1/integration/manifest`.

## 4. Tratamento de `PARTIAL`

O app agora não trata `PARTIAL` como falha fatal. A resposta parcial é renderizada quando possui campos úteis, mas não apaga um snapshot bom anterior.

Campos adicionados ao modelo de ativo `B3AssetData`:

- `proxyStatus`.
- `isPartial`.
- `dataReliability`.
- `extractionCompleteness`.
- `partialDataGuidance`.
- `cacheStatus`.
- `handlerTotalMs`.
- `shouldKeepPreviousSnapshot`.
- `lastUpdatedAt`.
- `fromLocalSnapshot`.

Estratégia aplicada:

- Se a resposta é boa, o app salva um snapshot por ticker.
- Se a resposta vem parcial e pobre, o app recupera o último snapshot bom.
- Se o Proxy fica offline/lento, o app tenta usar cache local antes de falhar visualmente.
- Informações técnicas são mantidas em diagnóstico, não poluem a tela principal.

## 5. Cache local do APK

Além dos caches já existentes, foi adicionado snapshot local via `SharedPreferences` em `B3NetworkService`.

Comportamento:

- Mantém último snapshot bom por ticker.
- Preserva dados bons diante de resposta parcial ruim.
- Marca dados recuperados como `fromLocalSnapshot=true`.
- Permite diagnóstico de quantidade de snapshots/cache.

Esse cache é simples, gratuito, compatível com Android nativo e não adiciona dependências pagas.

## 6. Carteira e prewarm

A carteira continua preservando posições e transações locais. A busca de preços/dados usa batch por `/api/v1/assets`, evitando loop de chamadas individuais quando há múltiplos tickers.

O fluxo do `PortfolioViewModel` já possui debounce de atualização; a integração mantém esse comportamento e usa o batch para pré-carregar os tickers quando a carteira muda ou é aberta.

## 7. Saúde do Proxy e diagnóstico

Arquivos alterados:

- `MainActivity.kt`.
- `PortfolioViewModel.kt`.
- `SettingsScreen.kt`.

Implementado:

- Inicialização de `B3NetworkService` com `applicationContext`.
- Chamada de saúde no start do app.
- Chip discreto no topo com estados:
  - `Proxy`.
  - `Cache`.
  - `Parcial`.
  - `Offline`.
- Tela/seção `Configurações > Diagnóstico do Proxy` com:
  - URL do Proxy.
  - status de readiness.
  - última verificação.
  - status de fontes.
  - status de métricas.
  - cache em memória.
  - snapshots bons locais.
  - ativos atualizados.
  - respostas parciais.
  - erros recentes.
  - tempo médio de resposta.

## 8. Detalhe de ativo

Arquivo alterado:

- `ui/components/AssetDetailModal.kt`.

A tela de detalhe passou a considerar os metadados reais do Proxy:

- completude real (`extractionCompleteness`) quando disponível;
- aviso discreto para `PARTIAL`;
- indicação quando está usando cache local;
- renderização dos campos disponíveis sem quebrar a tela.

## 9. Segurança

- O app continua não expondo tokens sensíveis.
- A URL base valida HTTPS/HTTP utilizável e ignora placeholders inseguros.
- O app não aceita URL livre do usuário final para `/api/scrape`.
- O scraping direto permanece fora do fluxo normal do APK.
- Não foram adicionados serviços pagos, Firebase pago, Redis, KV, banco externo, WebSocket pago nem APIs pagas.

## 10. Validação executada

### Proxy

Comandos executados:

```bash
npm run check
npm run bench:news
```

Resultado:

- `npm run check`: sucesso, 275 arquivos JS verificados.
- `npm run bench:news`: sucesso.
- Benchmark de notícias indicou `NEWS_CACHE_HIT` em cache quente e semântica de notícia vazia preservando notícia anterior (`shouldKeepPreviousNews: true`).

### APK

Comando tentado:

```bash
./gradlew :app:compileDebugKotlin --no-daemon --stacktrace
```

Resultado do sandbox:

- Não foi possível concluir a compilação porque o Gradle Wrapper tentou baixar `gradle-9.3.1-bin.zip` em `services.gradle.org`, mas o sandbox não tinha resolução/acesso de rede para esse host.
- Erro bloqueador externo: `java.net.UnknownHostException: services.gradle.org`.
- A falha ocorreu antes da etapa de compilação Kotlin do app.

### Verificação estática local

Foi executada uma verificação estática de presença dos contratos críticos no código, incluindo:

- BuildConfig para as três URLs.
- `/api/v1/ready`.
- `/api/v1/asset`.
- `/api/v1/assets`.
- `/api/v1/source/status`.
- `/api/v1/release/readiness`.
- `/api/v1/integration/manifest`.
- cache de snapshot.
- tratamento de parcial.
- diagnóstico em Configurações.
- inicialização no start do app.

Todos os checks estáticos retornaram `OK`.

## 11. Como configurar no Android Studio

Edite `gradle.properties`, `.env` ou variáveis de ambiente do build:

```properties
VALORAE_API_BASE_URL=https://servidor-valorae.vercel.app
VALORAE_PROXY_BASE_URL=https://servidor-valorae.vercel.app
VALORAE_PUBLIC_BASE_URL=https://servidor-valorae.vercel.app
```

Depois rode:

```bash
./gradlew :app:assembleDebug
```

ou, no Android Studio:

1. Sync Gradle.
2. Build > Make Project.
3. Rodar no emulador/dispositivo.

## 12. Como testar no APK

1. Abrir o app com internet.
2. Verificar o chip do topo:
   - esperado: `Proxy` quando `/api/v1/ready` responde bem.
3. Ir em `Configurações > Diagnóstico do Proxy`.
4. Confirmar URL: `https://servidor-valorae.vercel.app`.
5. Confirmar status de fontes/readiness/métricas.
6. Adicionar ativos na carteira, por exemplo:
   - `PETR4`.
   - `VALE3`.
   - `GARE11`.
7. Confirmar que a carteira faz batch via `/api/v1/assets`.
8. Abrir detalhe de ativo e confirmar uso de `/api/v1/asset` com perfil `turbo`.
9. Simular conexão ruim/offline:
   - abrir um ativo já consultado antes;
   - confirmar uso do último snapshot bom/cache local.
10. Validar modo claro/escuro e telas pequenas/grandes.

## 13. Arquivos alterados

- `app/build.gradle.kts`.
- `.env.example`.
- `gradle.properties`.
- `app/src/main/java/com/example/network/B3NetworkService.kt`.
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`.
- `app/src/main/java/com/example/MainActivity.kt`.
- `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`.
- `app/src/main/java/com/example/ui/components/AssetDetailModal.kt`.

## 14. Pendências honestas

- Compilação Android precisa ser confirmada em ambiente com Gradle já disponível ou com internet para baixar a distribuição `gradle-9.3.1-bin.zip`.
- Após compilar, validar em dispositivo real os estados de rede lenta, offline e `PARTIAL` usando o Proxy publicado.
- A seção de diagnóstico foi adicionada de forma discreta em Configurações; se o design final do app tiver navegação própria para suporte/devtools, ela pode ser movida para essa área.
