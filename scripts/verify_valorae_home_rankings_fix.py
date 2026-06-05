from pathlib import Path
root = Path(__file__).resolve().parents[1]
dashboard = (root / 'app/src/main/java/com/example/ui/screens/DashboardScreen.kt').read_text(encoding='utf-8')
vm = (root / 'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text(encoding='utf-8')
service = (root / 'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
gradle = (root / 'app/build.gradle.kts').read_text(encoding='utf-8')
checks = {
    'Home sem Column duplicado no card de rankings': 'Column(\n            Column(' not in dashboard,
    'Home usa cache assetData para preço/nome': 'homeMarketMoverAsset(assetData, item.ticker)' in dashboard and 'asset?.price' in dashboard and 'asset?.name' in dashboard,
    'Home força seta por aba alta/baixa': 'val arrow = if (isPositive) "▲" else "▼"' in dashboard,
    'Home mostra fonte do ranking': 'sourceLabel = ranking.source' in dashboard,
    'Home tem testTag para auditoria visual': 'testTag("home_market_movers_card")' in dashboard,
    'Home mostra skeleton antes da primeira resposta': 'if (isLoading || ranking == null)' in dashboard,
    'ViewModel marca carregamento dos rankings': 'currentState.copy(isLoading = true)' in vm,
    'ViewModel busca ranking vivo em modo completo': 'fetchLiveStockRankings(complete = true)' in vm,
    'ViewModel protege falha e desliga loading': 'Erro ao carregar rankings da Home' in vm and 'copy(isLoading = false)' in vm,
    'OkHttp suporta ranking completo lento': '.readTimeout(22, TimeUnit.SECONDS)' in service and '.callTimeout(24, TimeUnit.SECONDS)' in service,
    'Parser aceita aliases Investidor10/AeroScrape': all(x in service for x in ['"topGainers"', '"topLosers"', '"maioresAltas"', '"maioresBaixas"', '"gainers"', '"losers"']),
    'Parser aceita ticker por code/papel': 'item.optAny("code")' in service and 'item.optAny("papel")' in service,
    'Versão atualizada': 'versionCode = 20' in gradle and 'versionName = "2.0.10"' in gradle,
}
failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL') + ' - ' + name)
if failed:
    raise SystemExit('STATIC_HOME_RANKINGS_FIX_FAILED: ' + ', '.join(failed))
print('STATIC_HOME_RANKINGS_FIX_OK')
