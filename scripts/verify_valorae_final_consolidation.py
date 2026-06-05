from pathlib import Path
root = Path(__file__).resolve().parents[1]
service = (root/'app/src/main/java/com/example/network/B3NetworkService.kt').read_text()
vm = (root/'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text()
rankings = (root/'app/src/main/java/com/example/ui/screens/RankingsScreen.kt').read_text()
proxy_tools = (root/'app/src/main/java/com/example/ui/screens/ProxyToolsScreen.kt').read_text()
main = (root/'app/src/main/java/com/example/MainActivity.kt').read_text()
checks = {
    'Dados avançados não disparam endpoints na abertura do app': 'refreshProxyCapabilities(force = false)' not in vm and 'Dados avançados é carregado sob demanda' in vm,
    'Parser de rankings aceita aliases de altas/baixas': all(x in service for x in ['"gainers"', '"losers"', '"maioresAltas"', '"maioresBaixas"', '"topGainers"', '"topLosers"']),
    'Parser de rankings aceita data/results/rankings': all(x in service for x in ['data?.optJSONObject("rankings")', 'results?.optJSONObject("rankings")', 'root.optArray("data.rankings.$key")']),
    'Home de movimentos usa preço de cache/ativo': 'marketMoverPriceText(item, asset)' in rankings and 'asset?.price' in rankings,
    'Rankings tem página própria na barra inferior': 'RankingsScreen(' in main and 'Icons.Filled.Leaderboard' in main,
    'Página técnica removida da barra inferior, mantendo diagnóstico em Configurações': 'ProxyToolsScreen(' not in main and 'Proxy+' not in main and 'activePage == 5' not in main,
    'Insights preserva lógica de existência da carteira': 'firstPurchaseAt' in service and 'eligibleDividendAmount' in (root/'app/src/main/java/com/example/ui/screens/ChartsScreen.kt').read_text() and 'eventEligibilityMillis' in (root/'app/src/main/java/com/example/ui/screens/ChartsScreen.kt').read_text(),
}
missing = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL'), '-', name)
if missing:
    raise SystemExit('Falhas na consolidação final: ' + '; '.join(missing))
print('Valorae final consolidation audit OK')
