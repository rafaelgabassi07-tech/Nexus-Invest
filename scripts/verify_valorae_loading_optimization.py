#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parents[1]
vm = (root / 'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text(encoding='utf-8')
service = (root / 'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
checks = {
    'Abertura escalona notícias': 'delay(600)' in vm and 'fetchGlobalNews(force = false)' in vm,
    'Abertura escalona rankings': 'delay(1200)' in vm and 'refreshLiveMarketRankings(force = false)' in vm,
    'TTL para notícias': 'NEWS_SOFT_TTL_MS' in vm and 'lastNewsRefreshAt' in vm,
    'TTL para rankings de mercado': 'MARKET_RANKINGS_SOFT_TTL_MS' in vm and 'lastMarketRankingsRefreshAt' in vm,
    'TTL para saúde do Proxy': 'PROXY_HEALTH_SOFT_TTL_MS' in vm and 'proxyHealthJob?.isActive' in vm,
    'TTL para cotações em lote': 'PRICE_CACHE_SOFT_TTL_MS' in vm and 'lastPriceFetchSignature' in vm,
    'Insights não baixa rankings globais a cada atualização da carteira': 'val liveMarketRankingDeferred' not in vm and 'val stockMarketRankingDeferred' not in vm and 'val fiiMarketRankingDeferred' not in vm,
    'Proxy+ não roda no refresh geral quando nunca aberto': 'if (_proxyCapabilities.value.lastUpdated > 0L)' in vm,
    'Proxy+ tem trava contra chamadas concorrentes': 'proxyCapabilitiesJob?.isActive' in vm,
    'OkHttp com concorrência moderada': 'maxRequests = 8' in service and 'maxRequestsPerHost = 4' in service,
    'Rankings ao vivo com timeout curto': '"timeoutMs" to if (live) "2500" else "1800"' in service,
    'Batch de ativos não vira dezenas de chamadas individuais': 'stillMissing.take(if (bypassCache) 8 else 4)' in service,
    'Diagnóstico do Proxy tem cache': 'proxy_diagnostics_summary' in service and 'putInCache("proxy_diagnostics_summary"' in service,
    'Proxy+ limita blocos avançados por abertura': 'PROXY_PLUS_ASSET_ADVANCED_LIMIT' in service and 'PROXY_PLUS_PORTFOLIO_LIMIT' in service and 'PROXY_PLUS_DIAGNOSTIC_LIMIT' in service,
}
failed = [name for name, ok in checks.items() if not ok]
if failed:
    print('Valorae loading optimization audit FAILED')
    for name in failed:
        print(f'- {name}')
    raise SystemExit(1)
print('Valorae loading optimization audit OK')
for name in checks:
    print(f'OK - {name}')
