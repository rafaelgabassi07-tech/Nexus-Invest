# Integração VALORAE Investidor/Portfolio com VALORAE Proxy

Esta versão do app VALORAE consome o **VALORAE Proxy v21.12.52** como canal oficial de dados financeiros. O APK não deve fazer scraping direto nem depender de serviços pagos.

## Variáveis de ambiente suportadas

Configure no arquivo `.env`, no `local.properties` ou no ambiente de build quando necessário:

```env
VALORAE_API_BASE_URL=https://servidor-valorae.vercel.app
VALORAE_PROXY_BASE_URL=https://servidor-valorae.vercel.app
VALORAE_PUBLIC_BASE_URL=https://servidor-valorae.vercel.app
VALORAE_PROXY_CLIENT_ID=valorae-investidor-android
VALORAE_DIRECT_FALLBACK_ENABLED=false
```

As URLs precisam usar **HTTPS**. URLs `http://`, `localhost`, placeholders e hosts legados de teste são rejeitados pelo build/runtime.

## Fluxo de dados

```text
VALORAE Android App
  -> B3NetworkService
  -> VALORAE Proxy HTTPS
  -> Valorae-engine.js / cache / normalizadores / fallback controlado
  -> JSON canônico v1
  -> ViewModel
  -> Home, Carteira, Rankings, Insights, Notícias, Proxy+, Diagnóstico e Detalhes
```

## Endpoints principais consumidos pelo app

### Saúde, readiness e diagnóstico

- `GET /api/v1/ready`
- `GET /api/v1/release/readiness`
- `GET /api/v1/source/status`
- `GET /api/server/metrics`
- `GET /api/v1/cache/stats`
- `GET /api/v1/deploy/status`
- `GET /api/v1/health`
- `GET /api/v1/schema`
- `GET /api/v1/engine/maturity`
- `GET /api/v1/engine/performance`

### Ativos e carteira

- `GET /api/v1/asset?view=app&profile=fast|turbo|max`
- `POST/GET /api/v1/assets?view=app&profile=portfolio&timeoutMs=800`
- `GET /api/v1/asset/history`
- `GET /api/v1/asset/dividends`
- `GET /api/v1/asset/next-dividend`
- `POST /api/v1/portfolio/analyze`
- `POST /api/v1/portfolio/history`
- `POST/GET /api/v1/portfolio/next-dividends`
- `POST /api/v1/portfolio/rebalance`
- `POST /api/v1/portfolio/allocation`
- `POST /api/v1/portfolio/risk`
- `POST /api/v1/portfolio/income`
- `POST /api/v1/portfolio/dividends`
- `POST /api/v1/portfolio/events`
- `POST /api/v1/portfolio/summary`
- `POST /api/v1/portfolio/transactions`

### Mercado, notícias, rankings e comparações

- `GET /api/v1/news`
- `GET /api/v1/market/indices`
- `GET /api/v1/market/ipca`
- `GET /api/v1/market/rankings`
- `GET/POST /api/v1/compare`
- `POST /api/v1/watchlist/analyze`

### Análise avançada do ativo

- `GET /api/v1/asset/quality`
- `GET /api/v1/asset/coverage`
- `GET /api/v1/asset/action-plan`
- `GET /api/v1/asset/source-map`
- `GET /api/v1/asset/fundamentals`
- `GET /api/v1/asset/profile`
- `GET /api/v1/asset/valuation`
- `GET /api/v1/asset/profitability`
- `GET /api/v1/asset/debt`
- `GET /api/v1/asset/statements`
- `GET /api/v1/asset/peers`
- `GET /api/v1/asset/indicators`

### Central de FIIs

- `GET /api/v1/fii/profile`
- `GET /api/v1/fii/income`
- `GET /api/v1/fii/patrimonial`
- `GET /api/v1/fii/portfolio`
- `GET /api/v1/fii/vacancy`
- `GET /api/v1/fii/communications`
- `GET /api/v1/fii/checklist`
- `GET /api/v1/fii/indicators`

## Perfis usados pelo APK

- Home, cards e listas: `profile=fast` ou ranking leve.
- Carteira: `profile=portfolio&timeoutMs=800` via `/api/v1/assets` em lote.
- Detalhe de ativo: `profile=turbo`.
- Análise profunda: `profile=max&complete=1` quando necessário.
- Proxy+: módulos avançados carregados sob demanda para não pesar a abertura do app.

## Tratamento de `PARTIAL`

Resposta `PARTIAL` não é erro fatal. O app deve:

- renderizar campos disponíveis;
- manter último snapshot bom por ticker;
- não sobrescrever cache bom com payload parcial ruim;
- mostrar aviso discreto quando necessário;
- usar diagnóstico técnico apenas em Configurações/Proxy+;
- preservar rankings e blocos de mercado existentes quando uma chamada de analytics falhar.

## Headers enviados

```text
x-valorae-app: VALORAE
x-valorae-client: valorae-investidor-android
x-valorae-build: <versão do app>
x-valorae-platform: android
X-Valorae-Client-Id: valorae-investidor-android
X-Valorae-Client-Version: 21.5.13
X-Valorae-Environment: production
Accept: application/json
User-Agent: VALORAE-Investidor-Portfolio/1.1.4 Android
```

## Segurança

`VALORAE_DIRECT_FALLBACK_ENABLED=false` deve permanecer fixo. O app Android não deve consultar Yahoo, Google News, StatusInvest ou Investidor10 diretamente. Todo dado externo deve passar pelo VALORAE Proxy oficial para manter cache, normalização, observabilidade, controle de headers e contrato JSON estável.

O endpoint `/api/scrape` não é exposto ao usuário final no APK.

## Privacidade

Para atualizar ativos, o app envia tickers e posições agregadas quando necessário. O histórico detalhado de transações permanece local; o Proxy recebe apenas os dados mínimos para análise de carteira, dividendos, risco, renda e rebalanceamento.
