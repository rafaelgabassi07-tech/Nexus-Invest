# Integração VALORAE Investidor/Portfolio com Valorae Proxy

Esta versão do app VALORAE foi ajustada para consumir os dados financeiros através do **Valorae Proxy** como canal oficial de dados.

## Variáveis necessárias

Configure no arquivo `.env` ou no painel Secrets/Environment do Studio:

```env
VALORAE_PROXY_BASE_URL=https://servidor-valorae.vercel.app
VALORAE_PROXY_CLIENT_ID=valorae-investidor-android
VALORAE_DIRECT_FALLBACK_ENABLED=false
```

`VERCEL_BACKEND_URL` continua aceito por compatibilidade, mas `VALORAE_PROXY_BASE_URL` tem prioridade.

## Fluxo de dados

```text
VALORAE Android App
  -> B3NetworkService
  -> Valorae Proxy HTTPS
  -> Valorae-engine.js / scraping / normalizadores
  -> JSON estável
  -> ViewModel
  -> páginas, cards, gráficos, detalhes de ativos e notícias
```

## Endpoints consumidos pelo app

- `GET /api/asset`: dados completos do ativo para tela de análise, detalhes, cards e indicadores.
- `POST /api/assets`: busca em lote para carteira e dashboard.
- `GET /api/asset/history`: série histórica para gráficos.
- `GET /api/news`: notícias por ticker.
- `GET /api/market/indices`: ticker de mercado/índices usados na faixa superior.
- `POST /api/portfolio/analyze`: análise consolidada da carteira, score, risco, renda, alocação e insights.
- `POST /api/portfolio/history`: série consolidada para Evolução de Patrimônio.
- `GET /api/market/ipca`: IPCA/BCB para Rentabilidade vs IPCA+.
- `POST/GET /api/portfolio/next-dividends`: agenda de dividendos por posição/ticker.

## Headers enviados

O app envia os seguintes headers para facilitar observabilidade no painel do Proxy:

```text
X-Valorae-Client-Id: valorae-investidor-android
X-Valorae-Client-Version: 21.5.13
X-Valorae-Environment: production
X-Valorae-App: VALORAE
X-Valorae-Consumer: investidor-portfolio
Accept: application/json
User-Agent: VALORAE-Investidor-Portfolio/1.1.4 Android
```

Com isso, o painel de observabilidade do Valorae Proxy consegue identificar o app consumidor, medir latência, volume, status, endpoints chamados, payloads e erros.

## Fallback direto desativado

`VALORAE_DIRECT_FALLBACK_ENABLED=false` deve permanecer fixo. O app Android não deve consultar Yahoo, Google News, StatusInvest ou Investidor10 diretamente para dados de mercado. Todo dado externo deve passar pelo Valorae Proxy oficial para manter cache, normalização, observabilidade, controle de headers e contrato JSON estável.

As rotinas legadas diretas foram neutralizadas no código e retornam vazio/nulo mesmo se alguém tentar habilitar a flag por engano.

## Privacidade

Para atualização simples de ativos, o app envia apenas tickers. Para análise consolidada, evolução, equilíbrio e agenda de dividendos, o app envia ao Proxy somente posições agregadas necessárias (`ticker`, quantidade, preço médio, tipo e valores calculados), não o histórico completo de transações. O histórico detalhado e os lançamentos continuam no banco local do app, salvo fluxos explícitos de sincronização/configuração externa já existentes no projeto.
