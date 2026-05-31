# Relatório final — Integração APK VALORAE com VALORAE Proxy

## Resultado

A integração foi aplicada diretamente no projeto Android enviado. O APK agora usa os contratos `/api/v1/...` do VALORAE Proxy para carteira, detalhe de ativo, saúde, fontes, métricas, notícias e dados auxiliares, mantendo o Proxy como backend central e sem adicionar dependências pagas.

## Versão do Proxy usada

O pedido menciona v21.12.51, mas o arquivo recebido nesta rodada foi `valorae-proxy-v21.12.52-news-reliability-upgrade.zip`. Usei a v21.12.52 por ser a versão enviada e mais recente, mantendo compatibilidade com os contratos v21.12.51.

## Arquivos alterados no APK

- `app/build.gradle.kts`
- `.env.example`
- `gradle.properties`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`
- `app/src/main/java/com/example/ui/components/AssetDetailModal.kt`
- `docs/INTEGRACAO_VALORAE_PROXY_V21_12_52_APK.md`

## Endpoints integrados

- `/api/v1/ready`
- `/api/v1/release/readiness`
- `/api/v1/source/status`
- `/api/server/metrics`
- `/api/v1/asset`
- `/api/v1/assets`
- `/api/v1/news`
- `/api/v1/asset/history`
- `/api/v1/asset/dividends`
- `/api/v1/market/indices`
- `/api/v1/market/ipca`
- `/api/v1/portfolio/analyze`
- `/api/v1/portfolio/history`
- `/api/v1/portfolio/next-dividends`
- `/api/v1/integration/manifest`

## Telas conectadas

- App start/top bar: status discreto do Proxy.
- Carteira: batch por `/api/v1/assets` com `profile=portfolio&timeoutMs=800`.
- Detalhe de ativo: `/api/v1/asset` com `profile=turbo` e fallback rápido.
- Gráficos/dados profundos: `profile=max&complete=1` com fallback.
- Configurações: nova seção `Diagnóstico do Proxy`.

## Correções e melhorias aplicadas

- Configuração clara de URL por `VALORAE_API_BASE_URL`, `VALORAE_PROXY_BASE_URL` e `VALORAE_PUBLIC_BASE_URL`.
- Headers de integração adicionados.
- Tratamento de `PARTIAL` sem falha fatal.
- Último snapshot bom por ticker em cache local.
- Não sobrescreve snapshot bom com resposta parcial ruim.
- Fallback visual para cache local/offline.
- Diagnóstico com status, cache, parciais, erros e tempo médio.
- Migração de rotas principais para `/api/v1/...`.
- Mantida compatibilidade com Vercel Free e sem serviços pagos.

## Validação

Proxy:

- `npm run check`: sucesso, 275 JS files verificados.
- `npm run bench:news`: sucesso, cache quente com `NEWS_CACHE_HIT`.

APK:

- Verificação estática de integração: OK para BuildConfig, endpoints v1, snapshot cache, partial handling, health e diagnóstico.
- Build Android tentado com `./gradlew :app:compileDebugKotlin --no-daemon --stacktrace`, mas bloqueado pelo sandbox sem rede ao tentar baixar Gradle de `services.gradle.org`.
- Erro externo: `java.net.UnknownHostException: services.gradle.org`.

## Como configurar

Em `gradle.properties`, `.env` ou variáveis de ambiente:

```properties
VALORAE_API_BASE_URL=https://servidor-valorae.vercel.app
VALORAE_PROXY_BASE_URL=https://servidor-valorae.vercel.app
VALORAE_PUBLIC_BASE_URL=https://servidor-valorae.vercel.app
```

## Como testar

1. Abrir no Android Studio.
2. Fazer Sync Gradle.
3. Rodar `./gradlew :app:assembleDebug`.
4. Abrir o app e verificar o chip do Proxy.
5. Ir em Configurações > Diagnóstico do Proxy.
6. Adicionar `PETR4`, `VALE3` e `GARE11` na carteira.
7. Confirmar carregamento da carteira por batch.
8. Abrir detalhe de ativo.
9. Testar offline/cache após ter carregado um ativo pelo menos uma vez.

## Pendências

A única pendência técnica não resolvida no sandbox é confirmar a compilação Android em ambiente com Gradle disponível ou com internet para baixar a distribuição do wrapper. O Proxy foi validado com sucesso.
