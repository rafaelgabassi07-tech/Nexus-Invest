#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parents[1]
service = (root / 'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
vm = (root / 'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text(encoding='utf-8')
checks = {
    'Cache permite TTL longo para dados corporativos lentos': '24 * 60' in service and 'stableEndpointTtlMinutes' in service,
    'Fundamentos/valuation/rentabilidade/dívida/demonstrativos usam TTL estável': '/asset/fundamentals' in service and '/asset/valuation' in service and '/asset/profitability' in service and '/asset/debt' in service and '/asset/statements' in service and '12 * 60' in service,
    'FIIs avançados usam TTL estável': 'e.contains("/fii/") -> 12 * 60' in service,
    'Proxy+ usa cache por endpoint e payload': 'endpointCacheKey("cap_get"' in service and 'endpointCacheKey("cap_post"' in service,
    'Proxy+ reaproveita bloco estável quando endpoint lento falha': 'allowExpired = true' in service and 'Endpoint lento/indisponível agora' in service,
    'Atualização manual do Proxy+ ignora cache quando necessário': 'bypassCache: Boolean = false' in service and 'bypassCache = force' in vm,
    'Proxy+ cancela requisição antiga ao trocar ticker ou forçar refresh': 'proxyCapabilitiesJob?.cancel()' in vm and 'proxyCapabilitiesRequestToken' in vm,
    'Busca de preços em lote evita duplicidade em andamento': 'inFlightPriceFetchSignature' in vm,
    'Histórico e bundle gráfico usam TTL por range': 'rangeCacheTtlMinutes(normalizedRange)' in service and 'fun rangeCacheTtlMinutes' in service,
    'Cache ainda tem poda de overflow': 'memoryCache.size > 180' in service and '.take(memoryCache.size - 150)' in service,
}
failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL') + ' - ' + name)
if failed:
    raise SystemExit('Valorae slow data/performance audit FAILED: ' + '; '.join(failed))
print('Valorae slow data and performance optimization audit OK')
