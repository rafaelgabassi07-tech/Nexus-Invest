# RELATÓRIO — Restauração de Carteira, Rankings e Compatibilidade Proxy v2.0.22

## Contexto
A revisão da versão v2.0.21 indicou regressões funcionais: a navegação inferior perdeu a aba de Ativos/Carteira, o card de rankings da Home deixou de ser acionado corretamente e parte das otimizações anteriores de Proxy/rankings foi removida do `PortfolioViewModel`.

## Correções aplicadas
- Restaurada a navegação inferior com a aba **Ativos** usando ícone de carteira.
- Restaurado o fluxo da página **Ativos**, que acomoda **Meus Ativos** e **Histórico de Compras**.
- Restaurado o card compacto de rankings na Home, sem botão “Ver Ranking Completo”.
- Restaurado o carregamento de rankings via `refreshLiveMarketRankings(force, full)`.
- Restaurado `portfolioAnalytics` com suporte a rankings de carteira e rankings de mercado.
- Preservadas as melhorias de gráficos canônicos do Investidor10 via `assetChartsCanonical`/`assetChartsCoverage`.
- Preservado envio de parâmetros completos ao VALORAE Proxy para carteira, IPCA, dividendos e gráficos.
- Mantida a política de não montar gráficos falsos quando não houver série real.

## Compatibilidade VALORAE Proxy conferida
O app permanece consumindo o Proxy com suporte a:

- `/api/v1/asset`
- `/api/v1/assets`
- `/api/v1/market/rankings`
- `/api/v1/portfolio/analyze`
- `/api/v1/portfolio/history`
- `/api/v1/market/ipca`
- `/api/v1/portfolio/dividends`
- `/api/v1/portfolio/next-dividends`
- `/api/v1/proxy/diagnostics`
- módulos de capabilities avançados quando disponíveis.

Parâmetros preservados/confirmados:

- `mode=complete`
- `complete=true` ou `complete=1`
- `includeHistory=true`
- `includeUpcoming=true`
- `includeBenchmark=true`
- `benchmark=IPCA`
- `source=home` para rankings de Home quando aplicado pelo serviço de rankings.
- `firstPurchaseAt` por posição quando disponível.

## Versionamento
- `versionName = 2.0.22`
- `versionCode = 32`

## Observação
A compilação Gradle completa pode depender de acesso a `services.gradle.org`. Caso o ambiente não tenha rede/DNS, abrir o projeto no Android Studio ou Gemini com Gradle configurado para compilar o APK final.

## Validação executada

Validação estática:

```text
STATIC_RESTORE_PROXY_COMPAT_V222
ALL_OK
```

Tentativa de build:

```text
./gradlew assembleDebug
```

Resultado: não foi possível concluir neste ambiente porque o Gradle Wrapper tentou baixar a distribuição em `services.gradle.org`, mas o DNS/rede externa não está disponível. O log completo foi salvo em:

```text
docs/APK_BUILD_ATTEMPT_RESTORE_PROXY_COMPAT_v2.0.22.log
```
