#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parents[1]
service = (root / 'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
vm = (root / 'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text(encoding='utf-8')
charts = (root / 'app/src/main/java/com/example/ui/screens/ChartsScreen.kt').read_text(encoding='utf-8')
rankings_screen = (root / 'app/src/main/java/com/example/ui/screens/RankingsScreen.kt').read_text(encoding='utf-8')
dashboard = (root / 'app/src/main/java/com/example/ui/screens/DashboardScreen.kt').read_text(encoding='utf-8')
main = (root / 'app/src/main/java/com/example/MainActivity.kt').read_text(encoding='utf-8')
required = {
    'B3NetworkService consome ranking de mercado': 'fun fetchMarketRankings(' in service and '"/api/v1/market/rankings"' in service,
    'B3NetworkService expõe ranking personalizado da carteira': 'fun fetchPortfolioRankings(' in service,
    'Ranking da carteira funciona com 1 ativo': 'if (tickers.isEmpty()) return null' in service and 'tickers.size < 2' not in service,
    'B3NetworkService expõe ranking ao vivo de ações': 'fun fetchLiveStockRankings(' in service,
    'Parser aceita rankings por critérios e perfis': 'parseMarketRankingSnapshot' in service and 'dividendYield' in service and 'conservador' in service and 'rendaFii' in service and 'roe' in service and 'quality' in service,
    'Portfolio analytics captura inteligência do Proxy': 'actionPlan = actionPlan' in service and 'positionRanking = positionRanking' in service and 'rebalanceActions = rebalanceActions' in service,
    'ViewModel carrega rankings junto dos Insights': 'fetchPortfolioRankings(positions)' in vm and 'fetchLiveStockRankings()' in vm,
    'Estado de Insights armazena rankings': 'portfolioRanking: MarketRankingSnapshot?' in vm and 'liveMarketRanking: MarketRankingSnapshot?' in vm,
    'Rankings possui página própria na barra inferior': 'RankingsScreen(' in main and 'activePage == 1' in main and 'Icons.Filled.Leaderboard' in main,
    'Rankings não contaminam proventos/IPCA históricos': 'não alteram proventos históricos' in rankings_screen and 'linha do tempo real da sua carteira' in rankings_screen,
    'UI renderiza ação/inteligência da carteira': 'ProxyActionPlanSection' in rankings_screen and 'RankingDetailsCard' in rankings_screen,
    'UI mostra categorias amplas de mercado quando altas/baixas não vêm': 'Score Valorae' in rankings_screen and 'stockMarketRanking' in rankings_screen and 'fiiMarketRanking' in rankings_screen,
    'Início exibe cards de altas/baixas do dia': 'HomeMarketMoversPreview' in dashboard and 'analytics.liveMarketRanking' in dashboard and 'assetData = cachedAssetData' in dashboard,
    'ViewModel carrega ranking ao vivo sem depender da carteira e sem bloquear abertura': 'refreshLiveMarketRankings(force = false)' in vm and 'delay(1200)' in vm,
    'Parser preserva preço e variação separados nos movers': 'priceDisplay: String' in service and 'changeDisplay: String' in service and 'changePercent' in service and 'precoAtual' in service,
    'Página de rankings tem containers estilo Investidor10': 'CATEGORIAS DE RANKING' in rankings_screen and 'RankingCategoryCard' in rankings_screen and 'Mais Baratas' in rankings_screen and 'Menores P/Ls' in rankings_screen,
    'Home usa cache local para preencher preço faltante em baixas': 'assetData = cachedAssetData' in dashboard and 'marketMoverPriceText(item, asset)' in rankings_screen,
    'Ranking ao vivo enriquece preço faltante pelo batch /api/v1/assets': 'enrichMarketMoverPrices' in service and 'fetchAssetsData(tickers, bypassCache = false)' in service and 'priceDisplay = item.priceDisplay.ifBlank' in service,
    'Rankings selecionam categoria carregada após resposta assíncrona': 'rememberSaveable { mutableStateOf("") }' in rankings_screen and 'selected.items.isEmpty() && categories.any { it.items.isNotEmpty() }' in rankings_screen,
}
failed = [name for name, ok in required.items() if not ok]
if failed:
    print('Valorae Proxy capabilities audit FAILED')
    for name in failed:
        print(f'- {name}')
    raise SystemExit(1)
print('Valorae Proxy capabilities audit OK')
for name in required:
    print(f'OK - {name}')
