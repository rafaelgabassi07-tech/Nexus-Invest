#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
network = (ROOT / 'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
dashboard = (ROOT / 'app/src/main/java/com/example/ui/screens/DashboardScreen.kt').read_text(encoding='utf-8')
vm = (ROOT / 'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text(encoding='utf-8')
gradle = (ROOT / 'app/build.gradle.kts').read_text(encoding='utf-8')
tests = (ROOT / 'app/src/test/java/com/example/B3NetworkServiceParserTest.kt').read_text(encoding='utf-8')
source_tree = '\n'.join(p.read_text(encoding='utf-8', errors='ignore') for p in (ROOT / 'app/src/main/java').rglob('*.kt'))

checks = [
    ('Botão Ver Ranking Completo continua removido do código principal', 'Ver Ranking Completo' not in source_tree and 'onOpenRankings' not in source_tree),
    ('Home continua com card compacto de altas/baixas', 'HomeMarketMoversPreview' in dashboard and 'ALTAS ${highs.size}' in dashboard and 'BAIXAS ${lows.size}' in dashboard),
    ('Parser aceita aliases camelCase/snake_case de altas', 'topGainers' in network and 'top_gainers' in network and 'maiores_altas' in network),
    ('Parser aceita aliases camelCase/snake_case de baixas', 'topLosers' in network and 'top_losers' in network and 'maiores_baixas' in network),
    ('Parser separa listas genéricas de movers', 'genericMovers' in network and 'marketRankingItemLooksHigh' in network and 'marketRankingItemLooksLow' in network),
    ('Parser normaliza direction/tipo/movement', 'normalizeMarketRankingDirection' in network and 'item.optAny("tipo")' in network and 'item.optAny("movement")' in network),
    ('Parser normaliza percentual sem símbolo %', 'normalizeRankingPercentDisplay' in network and 'percentual' in network and 'percent_change' in network),
    ('Preço não é mais usado como value de variação', 'item.optAny("preco")\n        )' not in network[network.find('val value = firstNumber('):network.find('val display = firstText')]),
    ('Enriquecimento não sobrescreve variação real com preço/cotação auxiliar indevida', 'valueLooksLikePercent' in network and 'A cotação auxiliar serve apenas como fallback' in network),
    ('Home mostra erro/tentar novamente após tentativa falha', 'rankingsAttempted' in dashboard and 'MarketMoversErrorCard' in dashboard and 'onRetry' in dashboard),
    ('Home força símbolo % se display legado vier numérico', 'val normalized = if (display.contains("%")) display else "$display%"' in dashboard),
    ('ViewModel mantém fallback sem skeleton infinito', 'marketRankingsAttempted = true' in vm and 'unavailableLiveMarketRanking' in vm),
    ('Versão do APK incrementada para 2.0.14', 'versionName = "2.0.14"' in gradle and 'versionCode = 24' in gradle),
    ('Teste cobre generic items em altas/baixas', 'testGenericMarketMoversAreSplitIntoHighsAndLows' in tests),
]

failed = [name for name, ok in checks if not ok]
if failed:
    print('STATIC_RANKINGS_REVIEW_V214_FAILED')
    for name in failed:
        print(f'FAIL - {name}')
    raise SystemExit(1)

print('STATIC_RANKINGS_REVIEW_V214_OK')
for name, _ in checks:
    print(f'OK - {name}')
