from pathlib import Path
root = Path(__file__).resolve().parents[1]
service = (root/'app/src/main/java/com/example/network/B3NetworkService.kt').read_text()
vm = (root/'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text()
docs = (root/'docs/VALORAE_PROXY_INTEGRATION.md').read_text()
gradle = (root/'app/build.gradle.kts').read_text()
checks = {
    'Batch aceita objeto indexado por ticker sem perder ticker': 'fun acceptMapped(mapped: B3AssetData?, fallbackTicker: String? = null)' in service and 'acceptMapped(mapProxyAsset(item), key)' in service and 'val normalizedMapped = mapped.copy(ticker = cleanMappedTicker)' in service,
    'Batch não sobrescreve snapshot bom com parcial ruim': 'assetIsGoodSnapshot(normalizedMapped)' in service and 'loadBestSnapshot(cleanMappedTicker)?.copy' in service and 'Resposta parcial em lote; usando último snapshot bom.' in service,
    'Analytics preserva ranking da carteira quando endpoint remoto falha': 'portfolioRanking = remotePortfolioRanking ?: currentMarketState.portfolioRanking' in vm,
    'Analytics preserva rankings de mercado em fallback local': 'portfolioRanking = currentMarketState.portfolioRanking' in vm and 'liveMarketRanking = currentMarketState.liveMarketRanking' in vm and 'stockMarketRanking = currentMarketState.stockMarketRanking' in vm and 'fiiMarketRanking = currentMarketState.fiiMarketRanking' in vm,
    'Documentação oficial usa contratos /api/v1': '/api/v1/asset' in docs and '/api/v1/assets' in docs and '/api/v1/portfolio/analyze' in docs,
    'Documentação não mantém rotas legadas principais': 'GET /api/asset`' not in docs and 'POST /api/assets`' not in docs and 'GET /api/news`' not in docs,
    'Build exige HTTPS na URL do Proxy': '!value.startsWith("https://")' in gradle and 'safeValoraeProxyUrl' in gradle,
    'Fallback direto permanece bloqueado por política': 'VALORAE_DIRECT_FALLBACK_ENABLED' in gradle and 'false' in gradle and 'private fun directFallbackEnabled(): Boolean' in service and 'return false' in service,
}
missing = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL'), '-', name)
if missing:
    raise SystemExit('Falhas na auditoria final profunda: ' + '; '.join(missing))
print('Valorae deep final audit OK')
