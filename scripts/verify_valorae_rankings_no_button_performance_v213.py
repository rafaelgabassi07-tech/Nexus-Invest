#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parents[1]
service = (root / 'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
vm = (root / 'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text(encoding='utf-8')
dash = (root / 'app/src/main/java/com/example/ui/screens/DashboardScreen.kt').read_text(encoding='utf-8')
main = (root / 'app/src/main/java/com/example/MainActivity.kt').read_text(encoding='utf-8')
gradle = (root / 'app/build.gradle.kts').read_text(encoding='utf-8')
source_tree = ''.join(p.read_text(encoding='utf-8', errors='ignore') for p in (root/'app/src/main').rglob('*') if p.is_file())

checks = [
    ('Versão 2.0.13 aplicada', 'versionName = "2.0.13"' in gradle and 'versionCode = 23' in gradle),
    ('Botão Ver Ranking Completo removido do APK', 'Ver Ranking Completo' not in source_tree and 'onOpenRankings' not in source_tree),
    ('Home mantém card compacto de altas/baixas', 'HomeMarketMoversPreview' in dash and 'ALTAS ${highs.size}' in dash and 'BAIXAS ${lows.size}' in dash),
    ('Home mantém retry sem botão de ranking completo', 'MarketMoversErrorCard(onRetry = onRetry)' in dash and 'Tentar Novamente' in dash),
    ('Home reduz recomposições nas listas do ranking', 'val highs = remember(ranking)' in dash and 'val lows = remember(ranking)' in dash),
    ('Home preserva estado amigável após falha', 'rankingsAttempted' in dash and 'MarketMoversSkeleton()' in dash and 'MarketMoversErrorCard' in dash),
    ('Parser aceita aliases Investidor10/AeroScrape de altas/baixas', all(x in service for x in ['"altas"', '"baixas"', '"topGainers"', '"top_gainers"', '"maioresAltas"', '"maiores_altas"', '"topLosers"', '"top_losers"', '"maioresBaixas"', '"maiores_baixas"', '"gainers"', '"losers"'])),
    ('Parser aceita campos alternativos essenciais', all(x in service for x in ['item.optAny("ticker")', 'item.optAny("codigo")', 'item.optAny("symbol")', 'item.optAny("papel")', 'item.optAny("preco")', 'item.optAny("cotacao")', 'item.optAny("percentual")', 'item.optAny("volume")'])),
    ('Modo completo e fallback leve do Proxy preservados', all(x in service for x in ['"mode" to "complete"', '"complete" to "1"', '"strict"', '"minRows" to "6"', '"mode" to "auto"', 'fallbackParsed'])),
    ('Enriquecimento preserva variação real do ranking', 'val safeChange = if (rankingChange != 0.0) rankingChange else auxiliaryChange' in service and 'A cotação auxiliar serve apenas como fallback' in service),
    ('Enriquecimento não transforma preço em variação', 'kotlin.math.abs(it) <= 100.0' in service),
    ('ViewModel evita skeleton infinito e mantém fallback', 'marketRankingsAttempted = true' in vm and 'unavailableLiveMarketRanking' in vm and 'copy(isLoading = false' in vm),
    ('ViewModel mantém timeouts delimitados', all(x in vm for x in ['withTimeoutOrNull(if (full) 18_000 else 14_000)', 'withTimeoutOrNull(18_000)', 'withTimeoutOrNull(5_500)', 'withTimeoutOrNull(12_000)'])),
    ('HTTP mantém concorrência controlada para responsividade', all(x in service for x in ['maxRequests = 8', 'maxRequestsPerHost = 4', '.readTimeout(22, TimeUnit.SECONDS)', '.callTimeout(24, TimeUnit.SECONDS)'])),
    ('Bottom navigation Insights continua sendo o caminho de rankings avançados', 'text = "Insights"' in main and 'viewModel.refreshLiveMarketRankings(force = false, full = true)' in main),
]

failed = False
for name, ok in checks:
    print(('OK' if ok else 'FAIL') + ' - ' + name)
    failed = failed or not ok

def strip_quoted(src: str) -> str:
    out = []
    i = 0
    n = len(src)
    while i < n:
        if src.startswith('"""', i):
            i += 3
            while i < n and not src.startswith('"""', i):
                i += 1
            i += 3
            out.append('S')
        elif src[i] == '"':
            i += 1
            esc = False
            while i < n:
                ch = src[i]
                if esc:
                    esc = False
                elif ch == '\\':
                    esc = True
                elif ch == '"':
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

for rel in [
    'app/src/main/java/com/example/network/B3NetworkService.kt',
    'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt',
    'app/src/main/java/com/example/ui/screens/DashboardScreen.kt',
    'app/src/main/java/com/example/MainActivity.kt',
]:
    text = strip_quoted((root / rel).read_text(encoding='utf-8'))
    ok = text.count('{') == text.count('}') and text.count('(') == text.count(')')
    print(('OK' if ok else 'FAIL') + f' - Balanceamento estrutural leve {rel}')
    failed = failed or not ok

if failed:
    raise SystemExit('STATIC_RANKINGS_NO_BUTTON_PERFORMANCE_V213_FAILED')
print('STATIC_RANKINGS_NO_BUTTON_PERFORMANCE_V213_OK')
