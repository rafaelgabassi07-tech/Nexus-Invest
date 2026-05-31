#!/usr/bin/env python3
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
network = (root/'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
main = (root/'app/src/main/java/com/example/MainActivity.kt').read_text(encoding='utf-8')
settings = (root/'app/src/main/java/com/example/ui/screens/SettingsScreen.kt').read_text(encoding='utf-8')
build = (root/'app/build.gradle.kts').read_text(encoding='utf-8')

endpoint_literals = sorted(set(re.findall(r'"(/api/[^"]+)"', network + main + settings)))
expected = {
    '/api/fields', '/api/observability', '/api/openapi', '/api/server/metrics',
    '/api/v1/asset', '/api/v1/asset/action-plan', '/api/v1/asset/coverage', '/api/v1/asset/debt',
    '/api/v1/asset/dividends', '/api/v1/asset/fundamentals', '/api/v1/asset/history', '/api/v1/asset/indicators',
    '/api/v1/asset/next-dividend', '/api/v1/asset/peers', '/api/v1/asset/profile', '/api/v1/asset/profitability',
    '/api/v1/asset/quality', '/api/v1/asset/source-map', '/api/v1/asset/statements', '/api/v1/asset/valuation',
    '/api/v1/assets', '/api/v1/cache/stats', '/api/v1/compare', '/api/v1/deploy/status',
    '/api/v1/engine/maturity', '/api/v1/engine/performance', '/api/v1/fii/checklist', '/api/v1/fii/communications',
    '/api/v1/fii/income', '/api/v1/fii/indicators', '/api/v1/fii/patrimonial', '/api/v1/fii/portfolio',
    '/api/v1/fii/profile', '/api/v1/fii/vacancy', '/api/v1/health', '/api/v1/integration/manifest',
    '/api/v1/market/indices', '/api/v1/market/ipca', '/api/v1/market/rankings', '/api/v1/news',
    '/api/v1/personal/readiness', '/api/v1/portfolio/allocation', '/api/v1/portfolio/analyze', '/api/v1/portfolio/dividends',
    '/api/v1/portfolio/events', '/api/v1/portfolio/history', '/api/v1/portfolio/income', '/api/v1/portfolio/next-dividends',
    '/api/v1/portfolio/rebalance', '/api/v1/portfolio/risk', '/api/v1/portfolio/summary', '/api/v1/portfolio/transactions',
    '/api/v1/ready', '/api/v1/release/readiness', '/api/v1/schema', '/api/v1/source/status', '/api/v1/watchlist/analyze',
}
missing = sorted(expected - set(endpoint_literals))
unexpected = sorted(set(endpoint_literals) - expected)
checks = {
    '57 endpoints esperados permanecem declarados no APK': len(endpoint_literals) == 57 and not missing and not unexpected,
    'Parser de ativo lê raízes oficiais e compatibilidade legada': all(x in network for x in ['appPayload', 'appMobileSnapshot', 'legacyAppCompat', 'normalized', 'results']),
    'Parser de ativo lê contratos de classe e cobertura': all(x in network for x in ['assetClassContract', 'assetIndicatorCoverage', 'contractFieldsObject', 'coverageFieldsObject']),
    'Blocos opcionais não derrubam cache/fallback': all(x in network for x in ['isFatalProxyPayload', 'optionalBlock', 'asset-history', 'news', 'return false']),
    'Headers de integração Android são enviados ao Proxy': all(x in network for x in ['x-valorae-app', 'x-valorae-build', 'x-valorae-platform', 'X-Valorae-Client-Id']),
    'APK continua sem scraping direto': 'return false' in network and 'directFallbackEnabled' in network,
    'Aba Proxy+ continua fora da navegação inferior': 'ProxyToolsScreen(' not in main and 'activePage == 5' not in main and 'activePage = 5' not in main,
    'Diagnóstico amigável permanece em Configurações': all(x in settings for x in ['Diagnóstico do VALORAE', 'Recebimento de dados', 'Cache e proteção contra perda', 'viewModel.refreshProxyHealth(force = true)']),
    'Biometria mantém validação e fallback seguro': all(x in main + settings for x in ['setAllowedAuthenticators(authenticators)', 'canUseDeviceAuth', 'onDisableBiometric', 'setBiometricEnabled(false)', 'requestAuthenticationToEnable']),
    'Versão do app está em v1.1.8': 'versionCode = 11' in build and 'versionName = "1.1.8"' in build,
}
failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL'), '-', name)
if failed:
    if missing: print('Missing endpoints:', missing)
    if unexpected: print('Unexpected endpoints:', unexpected)
    raise SystemExit('Falhas na verificação v1.1.8: ' + '; '.join(failed))
print('Valorae full proxy receivers audit v1.1.8 OK')
