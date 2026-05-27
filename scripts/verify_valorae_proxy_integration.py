from pathlib import Path
root = Path(__file__).resolve().parents[1]
service = root / 'app/src/main/java/com/example/network/B3NetworkService.kt'
viewmodel = root / 'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt'
required_service = [
    'VALORAE_PROXY_BASE_URL',
    'X-Valorae-Client-Id',
    'X-Valorae-Client-Version',
    'X-Valorae-Environment',
    'fun fetchAssetsData(',
    '"/api/asset"',
    '"/api/assets"',
    '"/api/asset/history"',
    '"/api/news"',
    '"/api/market/indices"',
    '"/api/portfolio/analyze"',
    '"/api/portfolio/history"',
    '"/api/market/ipca"',
    '"/api/portfolio/next-dividends"',
    '"/api/health"',
    '"/api/ready"',
    '"/api/observability"',
    '"/api/fields"',
    '"/api/openapi"',
    'source = "Valorae Proxy"',
]
missing = []
text = service.read_text(encoding='utf-8')
for needle in required_service:
    if needle not in text:
        missing.append(f'B3NetworkService sem: {needle}')
vm = viewmodel.read_text(encoding='utf-8')
if 'B3NetworkService.fetchAssetsData(tickersToFetch' not in vm:
    missing.append('PortfolioViewModel não usa batch via Valorae Proxy')
for needle in ['portfolioAnalytics', 'B3NetworkService.fetchPortfolioAnalysis', 'B3NetworkService.fetchPortfolioHistory', 'B3NetworkService.fetchIpcaSeries', 'B3NetworkService.fetchNextDividends']:
    if needle not in vm:
        missing.append(f'PortfolioViewModel sem: {needle}')
for env in ['.env.example', '.env']:
    env_file = root / env
    if env_file.exists():
        e = env_file.read_text(encoding='utf-8')
        for key in ['VALORAE_PROXY_BASE_URL', 'VALORAE_PROXY_CLIENT_ID', 'VALORAE_DIRECT_FALLBACK_ENABLED']:
            if key not in e:
                missing.append(f'{env} sem {key}')

# Valores reais obrigatórios do deploy oficial.
for cfg in [root / 'gradle.properties', root / '.env.example', root / '.env']:
    if not cfg.exists():
        continue
    txt = cfg.read_text(encoding='utf-8')
    if 'VALORAE_PROXY_BASE_URL=https://servidor-valorae.vercel.app' not in txt:
        missing.append(f'{cfg.name} sem URL oficial do Valorae Proxy')
    if 'VALORAE_PROXY_CLIENT_ID=valorae-investidor-android' not in txt:
        missing.append(f'{cfg.name} sem client id oficial')
    if 'VALORAE_DIRECT_FALLBACK_ENABLED=false' not in txt:
        missing.append(f'{cfg.name} sem fallback direto desativado')

# Contratos de payload reais do Proxy usados pelas telas críticas.
for needle in ['portfolioScore', 'monthlyIncomeEstimated', 'byType', 'bySector', 'unrealizedPnLPct', 'nextDividend', 'dataPagamento']:
    if needle not in text:
        missing.append(f'B3NetworkService não trata campo real do Proxy: {needle}')

charts = (root / 'app/src/main/java/com/example/ui/screens/ChartsScreen.kt').read_text(encoding='utf-8')
for needle in ['analytics.portfolioHistory', 'analytics.ipcaSeries', 'analytics.dividendEvents', 'analytics.analysis']:
    if needle not in charts:
        missing.append(f'ChartsScreen não consome {needle}')

analysis_screen = (root / 'app/src/main/java/com/example/ui/screens/AnalysisScreen.kt').read_text(encoding='utf-8')
for needle in ['AssetVisualProxyCharts', 'HistoricalPriceLineChart', 'Gráficos derivados dos dados recebidos pelo Valorae Proxy']:
    if needle not in analysis_screen:
        missing.append(f'AnalysisScreen sem {needle}')

asset_detail = (root / 'app/src/main/java/com/example/ui/components/AssetDetailModal.kt').read_text(encoding='utf-8')
for needle in ['DADOS RECEBIDOS PELO PROXY', 'realData.source', 'HistoricalPriceLineChart']:
    if needle not in asset_detail:
        missing.append(f'AssetDetailModal sem {needle}')

manifest = (root / 'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
if '@xml/data_extraction_rules' in manifest and not (root / 'app/src/main/res/xml/data_extraction_rules.xml').exists():
    missing.append('AndroidManifest referencia @xml/data_extraction_rules, mas o arquivo não existe')

for cfg in [root / 'gradle.properties', root / '.env.example', root / '.env']:
    if cfg.exists():
        content = cfg.read_text(encoding='utf-8')
        if 'VALORAE_PROXY_BASE_URL=https://servidor-valorae.vercel.app' not in content:
            missing.append(f'{cfg.name} sem URL pública atual do Valorae Proxy')

if missing:
    print('\n'.join(missing))
    raise SystemExit(1)
print('Valorae Proxy integration audit OK')
