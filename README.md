# VALORAE Investidor/Portfolio

Aplicativo Android de gestão de carteira, análise de ativos, gráficos e notícias, configurado para consumir dados através do **Valorae Proxy**.

## Configuração obrigatória do Proxy

Antes de compilar/publicar, configure o arquivo `.env` ou os Secrets do Studio:

```env
VALORAE_PROXY_BASE_URL=https://servidor-valorae.vercel.app
VALORAE_PROXY_CLIENT_ID=valorae-investidor-android
VALORAE_DIRECT_FALLBACK_ENABLED=false
```

`VERCEL_BACKEND_URL` continua aceito por compatibilidade, mas a variável recomendada é `VALORAE_PROXY_BASE_URL`.

## Como os dados chegam ao app

O app usa `B3NetworkService` como camada única de rede para ativos, gráficos e notícias:

- `POST /api/assets` para atualizar carteira em lote.
- `GET /api/asset` para análise/detalhe de ativo.
- `GET /api/asset/history` para gráficos históricos.
- `GET /api/news` para notícias por ativo.
- `GET /api/market/indices` para ticker de mercado.
- `POST /api/portfolio/analyze` para análise consolidada, equilíbrio, renda e risco.
- `POST /api/portfolio/history` para evolução de patrimônio.
- `GET /api/market/ipca` para Rentabilidade vs IPCA+.
- `POST/GET /api/portfolio/next-dividends` para agenda de dividendos.

Os dados recebidos do Proxy são normalizados para `B3AssetData`, `ChartPoint`, `NewsItem`, `PortfolioProxyAnalysis`, `PortfolioHistoryPoint`, `IpcaPoint` e `DividendEvent`, alimentando as páginas, cards e gráficos do app.

## Fallback direto

Por padrão, o fallback direto está desligado:

```env
VALORAE_DIRECT_FALLBACK_ENABLED=false
```

Mantenha assim para garantir que o Valorae Proxy seja a fonte oficial. Defina `true` apenas como contingência manual durante manutenção do Proxy.

## Build local

1. Abra o projeto no Android Studio.
2. Configure `.env` com o endereço real do Valorae Proxy.
3. Sincronize o Gradle.
4. Rode em emulador ou dispositivo físico.

Mais detalhes: `docs/VALORAE_PROXY_INTEGRATION.md`.
