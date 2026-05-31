#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parents[1]
main = (root/'app/src/main/java/com/example/MainActivity.kt').read_text(encoding='utf-8')
vm = (root/'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text(encoding='utf-8')
service = (root/'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
asset_charts = (root/'app/src/main/java/com/example/ui/components/AssetCharts.kt').read_text(encoding='utf-8')
rankings = (root/'app/src/main/java/com/example/ui/screens/RankingsScreen.kt').read_text(encoding='utf-8')
dashboard = (root/'app/src/main/java/com/example/ui/screens/DashboardScreen.kt').read_text(encoding='utf-8')
proxy_tools = (root/'app/src/main/java/com/example/ui/screens/ProxyToolsScreen.kt').read_text(encoding='utf-8')
charts = (root/'app/src/main/java/com/example/ui/screens/ChartsScreen.kt').read_text(encoding='utf-8')
settings = (root/'app/src/main/java/com/example/ui/screens/SettingsScreen.kt').read_text(encoding='utf-8')
analysis = (root/'app/src/main/java/com/example/ui/screens/AnalysisScreen.kt').read_text(encoding='utf-8')
news = (root/'app/src/main/java/com/example/ui/screens/NewsScreen.kt').read_text(encoding='utf-8')
manifest = (root/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
build = (root/'app/build.gradle.kts').read_text(encoding='utf-8')

checks = {
    'Navegação possui Início, Rankings, Análise, Insights e Notícias sem aba técnica Proxy+': all(x in main for x in ['DashboardScreen(', 'RankingsScreen(', 'AnalysisScreen(', 'ChartsScreen(', 'NewsScreen(']) and 'ProxyToolsScreen(' not in main and 'Proxy+' not in main,
    'App tem INTERNET no Manifest': 'android.permission.INTERNET' in manifest,
    'URL do Proxy é HTTPS/configurável e tem fallback oficial': all(x in build for x in ['VALORAE_API_BASE_URL', 'VALORAE_PROXY_BASE_URL', 'VALORAE_PUBLIC_BASE_URL', 'https://servidor-valorae.vercel.app', '!value.startsWith("https://")']),
    'Home exibe movimentos do dia usando ranking ao vivo': 'HomeMarketMoversPreview' in dashboard and 'analytics.liveMarketRanking' in dashboard,
    'Rankings abre página completa com full=true, mas abertura do app usa apenas ranking ao vivo': 'refreshLiveMarketRankings(force = false, full = true)' in rankings and 'refreshLiveMarketRankings(force = false)' in vm and 'fun refreshLiveMarketRankings(force: Boolean = false, full: Boolean = false)' in vm,
    'Rankings full cancela chamada leve em andamento para não ficar incompleto': 'if (full) marketRankingsJob?.cancel() else return' in vm,
    'Cotações em lote atualizam cache expirado, não apenas tickers ausentes': 'shouldRefreshExisting' in vm and 'sameTickerSet && !cacheStillFresh' in vm,
    'Gráficos avançados são renderizados sob demanda por aba': 'ScrollableTabRow(' in asset_charts and 'AnimatedContent(' in asset_charts and 'StockAnalysisTab(bundle)' in asset_charts and 'FiiGeneralTab(bundle)' in asset_charts,
    'Análise de ativo carrega bundle avançado e notícias sem bloquear UI': 'loadAssetChartBundle(clean, normalizedRange)' in vm and 'withTimeoutOrNull' in vm and 'async(Dispatchers.IO)' in vm and 'AssetChartBundlePanel(' in analysis,
    'Insights respeita existência da carteira para dividendos/IPCA': 'eligibleDividendAmount' in charts and 'sharesOwnedAtInsightDate' in charts and 'portfolioAgeMonthsForInsights' in charts and 'normalizePortfolioHistoryForAge' in vm,
    'Ferramentas técnicas Proxy+ foram removidas da navegação inferior': 'ProxyToolsScreen(' not in main and 'activePage == 5' not in main and 'activePage !in 0..4' in main,
    'Notícias usam Proxy e TTL': '/api/v1/news' in service and 'NEWS_SOFT_TTL_MS' in vm and 'fetchGlobalNews(force = false)' in vm,
    'Diagnóstico usa readiness/status/métricas/cache': all(x in service for x in ['/api/v1/ready', '/api/v1/source/status', '/api/server/metrics', 'proxy_diagnostics_summary']),
    'Sem scraping direto no app Android': all(x not in service for x in ['query1.finance.yahoo.com', 'news.google.com/rss', 'investidor10.com.br/acoes', 'statusinvest.com.br']) and 'return false' in service and 'Fallback direto desativado' in service,
    'Configurações preserva diagnóstico, tema e ferramentas locais': 'Diagnóstico do Proxy' in settings and 'ThemePreferences' in settings and 'Exportar' in settings and 'importar' in settings,
}
failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL'), '-', name)
if failed:
    raise SystemExit('Falhas na auditoria funcional completa: ' + '; '.join(failed))
print('Valorae full app functionality and loading audit OK')
