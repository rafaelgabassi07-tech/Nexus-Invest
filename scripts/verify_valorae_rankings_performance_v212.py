#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parents[1]
service = (root / 'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
vm = (root / 'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text(encoding='utf-8')
dash = (root / 'app/src/main/java/com/example/ui/screens/DashboardScreen.kt').read_text(encoding='utf-8')
main = (root / 'app/src/main/java/com/example/MainActivity.kt').read_text(encoding='utf-8')
gradle = (root / 'app/build.gradle.kts').read_text(encoding='utf-8')
checks = [
    ('Versão 2.0.12 aplicada', 'versionName = "2.0.12"' in gradle and 'versionCode = 22' in gradle),
    ('Parser aceita aliases camelCase e snake_case de altas/baixas', all(x in service for x in ['"topGainers"', '"top_gainers"', '"maioresAltas"', '"maiores_altas"', '"topLosers"', '"top_losers"', '"maioresBaixas"', '"maiores_baixas"'])),
    ('Parser aceita campos alternativos de ticker/preço/variação/fonte', all(x in service for x in ['item.optAny("codigo")', 'item.optAny("symbol")', 'item.optAny("papel")', 'item.optAny("preco")', 'item.optAny("cotacao")', 'item.optAny("percentual")', 'item.optAny("volume")', 'item.optAny("source")'])),
    ('Modo completo usa contrato Proxy adequado', all(x in service for x in ['"mode" to "complete"', '"complete" to "1"', '"strict"', '"minRows" to "6"', '"limit" to "15"'])),
    ('Fallback leve de rankings preservado', all(x in service for x in ['"mode" to "auto"', '"minRows" to "3"', 'fallbackParsed'])),
    ('Enriquecimento não zera variação real do ranking', 'val rankingChange = item.changePercent.takeIf' in service and 'asset.changePercent.takeIf { it != 0.0 && it.isFinite() }' in service),
    ('Home não fica em skeleton infinito após falha', 'marketRankingsAttempted' in vm and 'rankingsAttempted' in dash and 'unavailableLiveMarketRanking' in vm),
    ('Home mostra skeleton antes da primeira tentativa', '(!rankingsAttempted && ranking == null)' in dash and 'MarketMoversSkeleton()' in dash),
    ('Home tem retry conectado a busca completa', 'onRefreshRankings = { viewModel.refreshLiveMarketRankings(force = true, full = true) }' in main),
    ('Abrir ranking completo força atualização ampla', 'onOpenRankings = {' in main and 'viewModel.refreshLiveMarketRankings(force = true, full = true)' in main),
    ('Aba Insights solicita rankings completos sem bloquear', 'viewModel.refreshLiveMarketRankings(force = false, full = true)' in main),
    ('Timeouts de performance delimitados no ViewModel', all(x in vm for x in ['withTimeoutOrNull(if (full) 18_000 else 14_000)', 'withTimeoutOrNull(18_000)', 'withTimeoutOrNull(5_500)', 'withTimeoutOrNull(12_000)'])),
    ('OkHttp limitado para responsividade', all(x in service for x in ['maxRequests = 8', 'maxRequestsPerHost = 4', '.readTimeout(22, TimeUnit.SECONDS)', '.callTimeout(24, TimeUnit.SECONDS)'])),
    ('LazyColumn principal usa chaves estáveis', all(x in dash for x in ['item(key = "portfolio_header")', 'item(key = "home_market_movers")'])),
    ('Redução de recomposições preservada', vm.count('.distinctUntilChanged()') >= 3),
]
failed = False
for name, ok in checks:
    print(('OK' if ok else 'FAIL') + ' - ' + name)
    failed = failed or not ok
# lightweight structural checks: remove quoted text first so JSON examples and string templates do not create false positives.
def strip_quoted(src: str) -> str:
    out = []
    i = 0
    n = len(src)
    while i < n:
        if src.startswith('\"\"\"', i):
            i += 3
            while i < n and not src.startswith('\"\"\"', i):
                i += 1
            i += 3
            out.append('S')
        elif src[i] == '\"':
            i += 1
            esc = False
            while i < n:
                ch = src[i]
                if esc:
                    esc = False
                elif ch == '\\':
                    esc = True
                elif ch == '\"':
                    i += 1
                    break
                i += 1
            out.append('S')
        elif src.startswith('//', i):
            while i < n and src[i] != '\n':
                i += 1
            out.append('\n')
        elif src.startswith('/*', i):
            i += 2
            while i < n and not src.startswith('*/', i):
                i += 1
            i += 2
            out.append(' ')
        else:
            out.append(src[i])
            i += 1
    return ''.join(out)

for rel in ['app/src/main/java/com/example/network/B3NetworkService.kt', 'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt', 'app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'app/src/main/java/com/example/MainActivity.kt']:
    text = strip_quoted((root / rel).read_text(encoding='utf-8'))
    ok = text.count('{') == text.count('}') and text.count('(') == text.count(')')
    print(('OK' if ok else 'FAIL') + f' - Balanceamento estrutural leve {rel}')
    failed = failed or not ok
if failed:
    raise SystemExit('STATIC_RANKINGS_PERFORMANCE_V212_FAILED')
print('STATIC_RANKINGS_PERFORMANCE_V212_OK')
