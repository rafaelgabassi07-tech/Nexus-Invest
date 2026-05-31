from pathlib import Path
import zipfile
import sys
root = Path(__file__).resolve().parents[1]
service = root / 'app/src/main/java/com/example/network/B3NetworkService.kt'
viewmodel = root / 'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt'
required_service = [
    'VALORAE_PROXY_BASE_URL',
    'X-Valorae-Client-Id',
    'X-Valorae-Client-Version',
    'X-Valorae-Environment',
    'fun fetchAssetsData(',
    '"/api/v1/asset"',
    '"/api/v1/assets"',
    '"/api/v1/asset/history"',
    '"/api/v1/asset/dividends"',
    '"/api/v1/news"',
    '"/api/v1/market/indices"',
    '"/api/v1/portfolio/analyze"',
    '"/api/v1/portfolio/history"',
    '"/api/v1/market/ipca"',
    '"/api/v1/portfolio/next-dividends"',
    '"/api/v1/ready"',
    '"/api/v1/release/readiness"',
    '"/api/v1/source/status"',
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

for needle in ['buildDividendEvolutionData', 'buildDividendAgendaData', 'analytics.dividendEvents']:
    if needle not in charts:
        missing.append(f'ChartsScreen sem helper/consumo de Insights: {needle}')

analysis_screen = (root / 'app/src/main/java/com/example/ui/screens/AnalysisScreen.kt').read_text(encoding='utf-8')
for needle in ['HistoricalPriceLineChart', 'assetChartBundles']:
    if needle not in analysis_screen:
        missing.append(f'AnalysisScreen sem integração de gráficos: {needle}')
if 'AssetChartBundlePanel' not in analysis_screen and 'StockAnalysisTab' not in analysis_screen and 'FiiGeneralTab' not in analysis_screen:
    missing.append('AnalysisScreen sem painel/abas de gráficos avançados do AssetChartBundle')

asset_detail = (root / 'app/src/main/java/com/example/ui/components/AssetDetailModal.kt').read_text(encoding='utf-8')
for needle in ['DADOS RECEBIDOS PELO PROXY', 'realData.source', 'HistoricalPriceLineChart', 'initialAssetData', 'onLoadChartBundle', 'toFallbackB3AssetData', 'mergeWithFallback']:
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

asset_charts = (root / 'app/src/main/java/com/example/ui/components/AssetCharts.kt').read_text(encoding='utf-8')
for needle in ['defaultChartDescription', 'filterComparisonSeries', 'comparisonWindowLimit', 'AssetIndexComparisonChart']:
    if needle not in asset_charts:
        missing.append(f'AssetCharts sem melhoria de descrição/comparação: {needle}')
for risky in ['filterYears * 12', 'takeLast(filterYears * 12)']:
    if risky in asset_charts:
        missing.append(f'AssetCharts ainda contém filtro arriscado com overflow: {risky}')
for needle in ['"/api/v1/compare"', 'fetchProxyComparisonSeries', 'parseComparisonSeriesFromObject', 'mergeComparisonSeries']:
    if needle not in text:
        missing.append(f'B3NetworkService sem fallback robusto de comparação: {needle}')


wrapper_warnings = []
wrapper = root / 'gradle/wrapper/gradle-wrapper.jar'
if not wrapper.exists() or wrapper.stat().st_size <= 0:
    wrapper_warnings.append('Gradle Wrapper ausente ou zerado: gradle/wrapper/gradle-wrapper.jar')
elif not zipfile.is_zipfile(wrapper):
    wrapper_warnings.append('Gradle Wrapper inválido/corrompido: gradle/wrapper/gradle-wrapper.jar não é um JAR/ZIP válido')
else:
    try:
        with zipfile.ZipFile(wrapper) as zf:
            names = set(zf.namelist())
            required_entries = [
                'org/gradle/wrapper/GradleWrapperMain.class',
                'org/gradle/wrapper/WrapperExecutor.class',
            ]
            for entry in required_entries:
                if entry not in names:
                    wrapper_warnings.append(f'Gradle Wrapper inválido: entrada ausente {entry}')
    except Exception as exc:
        wrapper_warnings.append(f'Gradle Wrapper inválido/corrompido: {exc}')

# O Wrapper corrompido impede build local, mas não invalida os checks de código.
# Android Studio/Gemini deve regenerá-lo antes de compilar.


# Garantia de que o app Android não contém mais fallback direto para Yahoo/Google News.
for forbidden in ['query1.finance.yahoo.com', 'news.google.com/rss', 'statusinvest.com.br', 'investidor10.com.br/acoes', 'investidor10.com.br/fiis']:
    if forbidden in text:
        missing.append(f'B3NetworkService contém chamada direta externa proibida: {forbidden}')
for needle in ['return false', 'Fallback direto desativado', 'Histórico direto desativado', 'Notícias diretas desativadas']:
    if needle not in text:
        missing.append(f'B3NetworkService não bloqueia fallback direto: {needle}')
if 'valorae-proxy.vercel.app' in text and '!lower.contains("valorae-proxy.vercel.app")' not in text:
    missing.append('B3NetworkService pode aceitar host antigo valorae-proxy.vercel.app')

# Testes não devem mascarar contratos antigos nem bater em endpoint legado.
unit_test = (root / 'app/src/test/java/com/example/ExampleUnitTest.kt').read_text(encoding='utf-8')
if '/api/asset?ticker=' in unit_test or '/api/assets' in unit_test:
    missing.append('ExampleUnitTest ainda referencia rotas legadas /api/asset ou /api/assets')
if '/api/v1/asset?ticker=' not in unit_test:
    missing.append('ExampleUnitTest sem cobertura explícita do contrato /api/v1/asset')

# O APK deve aceitar apenas HTTPS para o Proxy em runtime e build time.
build_gradle = (root / 'app/build.gradle.kts').read_text(encoding='utf-8')
if '!value.startsWith("https://")' not in build_gradle:
    missing.append('app/build.gradle.kts ainda permite URL não HTTPS para o Proxy')
if 'if (!value.startsWith("https://")) return false' not in text:
    missing.append('B3NetworkService ainda permite URL não HTTPS para o Proxy')

cloud_sync = (root / 'app/src/main/java/com/example/network/CloudSyncManager.kt').read_text(encoding='utf-8')
settings_screen = (root / 'app/src/main/java/com/example/ui/screens/SettingsScreen.kt').read_text(encoding='utf-8')
if 'return false' not in cloud_sync and 'cleanUrl.startsWith("https://")' not in cloud_sync:
    missing.append('CloudSyncManager não valida HTTPS quando sincronização externa opcional estiver configurada')
if 'Sincronização externa direta foi removida da UI' not in settings_screen or 'Nenhuma chamada a Supabase' not in settings_screen:
    missing.append('SettingsScreen ainda expõe sincronização externa direta em vez de backup local seguro')

charts_core = (root / 'app/src/main/java/com/example/ui/components/Charts.kt').read_text(encoding='utf-8')
for needle in ['resampleFloatSeries', 'cleanPortfolioValues', 'alignedIpcaValues']:
    if needle not in charts_core:
        missing.append(f'Charts.kt sem alinhamento seguro de séries IPCA/carteira: {needle}')

versions = (root / 'gradle/libs.versions.toml').read_text(encoding='utf-8')
if 'kotlin = "2.2.10"' in versions and 'googleDevtoolsKsp = "2.2.10-2.0.2"' not in versions:
    missing.append('libs.versions.toml com KSP incompatível com Kotlin 2.2.10')
for needle in ['runCatching { B3NetworkService.fetchAssetData(clean) }', 'JSONObject()\n                    .put("ticker", tx.ticker)']:
    if needle not in vm:
        missing.append(f'PortfolioViewModel sem hardening recente: {needle}')
if 'if (lastSearchTicker == clean) _isSearchingAsset.value = false' not in vm and 'finally {\n                _isSearchingAsset.value = false' not in vm:
    missing.append('PortfolioViewModel sem finalização segura do loading de análise')



# Continuação 5: custo médio móvel, range explícito de bundle, IPCA composto e sanitização de build.
for needle in ['remainingCostBasis', 'avgBeforeSale', 'remainingCostBasis -= qtySold * avgBeforeSale']:
    if needle not in vm:
        missing.append(f'PortfolioViewModel sem custo médio móvel seguro: {needle}')
for needle in ['fun safeValoraeProxyUrl', '!value.startsWith("https://")', 'lower.contains("valorae-proxy.vercel.app")', 'lower.contains("10.0.2.2")']:
    if needle not in (root / 'app/build.gradle.kts').read_text(encoding='utf-8'):
        missing.append(f'app/build.gradle.kts sem sanitização contra host legado: {needle}')
for needle in ['positionsCacheSignature', 'portfolio_history_${normalizedRange}_${positionsCacheSignature(positions)}', 'next_dividends_${positionsCacheSignature(positions)}']:
    if needle not in text:
        missing.append(f'B3NetworkService sem cache por assinatura completa de posições: {needle}')
for needle in ['val range: String = "1Y"', 'copy(range = normalizedRange)']:
    source_text = (root / 'app/src/main/java/com/example/network/AssetChartModels.kt').read_text(encoding='utf-8') + text
    if needle not in source_text:
        missing.append(f'AssetChartBundle sem range rastreável: {needle}')
if '((1.0 + accumulated / 100.0) * (1.0 + monthly / 100.0))' not in text:
    missing.append('B3NetworkService ainda soma IPCA linearmente em vez de compor o acumulado')
if 'alreadyLoaded.range.equals(normalizedRange' not in vm:
    missing.append('PortfolioViewModel não evita recarga de bundle já carregado no mesmo range')


# Continuação 6: todos os gráficos avançados devem aparecer em Análise e Detalhes,
# com seletor de período, downsampling e sanitização de dados antes de desenhar Canvas.
if 'AssetChartBundlePanel(' not in analysis_screen:
    missing.append('AnalysisScreen ainda mostra apenas uma aba de gráficos em vez do painel completo AssetChartBundlePanel')
if 'AssetChartBundlePanel(' not in asset_detail:
    missing.append('AssetDetailModal ainda mostra apenas uma aba de gráficos em vez do painel completo AssetChartBundlePanel')
for needle in ['BundleRangeSelector', 'downsampleComparisonPoints', 'normalizeBreakdownPoints', 'sanitizeFinancialPoints']:
    if needle not in asset_charts:
        missing.append(f'AssetCharts sem hardening/performance da continuação 6: {needle}')
for needle in ['val items = points.filter { it.value.isFinite() && it.secondaryValue.isFinite() }', 'val range = (maxVal - minVal).takeIf { it.isFinite() && it > 0.0001f } ?: 1f']:
    if needle not in asset_charts:
        missing.append(f'AssetCharts ainda pode quebrar com NaN/range zero: {needle}')


# Continuação 7: FIIs precisam usar normalized/financialSummary/indicadores para alimentar cards e gráficos.
for needle in ['fun normalizedOrElse', 'infoFiiForIndicators', 'valorPatrimonialIndicadores', 'Yield 12M', 'Indicadores Fundamentalistas do FII']:
    joined = text + '\n' + asset_charts
    if needle not in joined:
        missing.append(f'Continuação 7 ausente: {needle}')
for needle in ['field.optAny("value")', 'normalized.optAny(key)']:
    if needle not in text:
        missing.append(f'normalizedValue não aceita formato direto/FinancialField: {needle}')
parser_test = (root / 'app/src/test/java/com/example/B3NetworkServiceParserTest.kt').read_text(encoding='utf-8')
for needle in ['testFiiNormalizedFieldsBecomeIndicatorCards', 'Patrimônio Líquido', 'fiiDistribution12m']:
    if needle not in parser_test:
        missing.append(f'Testes sem cobertura FII normalized: {needle}')



# Continuação 8: gráficos/FIIs devem pedir payload completo e aceitar blocos normalizados/dividendos em objeto.
for needle in ['"profile" to "max"', '"complete" to "1"', 'addIndicatorWithDisplay("Preço Atual"', 'normalizedDisplay', 'addIndicatorsFromObject(normalized)', 'dividendSection', 'ultimos12Meses', 'last12Months', 'fiiDistribution12m.isEmpty() && dividendMonthly.isNotEmpty()']:
    if needle not in text:
        missing.append(f'Continuação 8 ausente em B3NetworkService: {needle}')
if '"lean" to "1"' in text:
    missing.append('fetchAssetChartBundle ainda usa lean=1 e pode remover blocos de gráficos/FIIs')
for needle in ['testFiiDividendObjectBuildsMonthlyAndYieldCharts', 'testGenericNormalizedIndicatorsAreNotDropped']:
    if needle not in parser_test:
        missing.append(f'Testes sem cobertura da continuação 8: {needle}')



# Continuação 9: chegada de dados fundamentalistas/FIIs reforçada.
for needle in ['mergedObject(', 'canonicalKey(', 'indicadoresFundamentalistas.comComparativos', 'keyRatios', 'fundamentalistIndicators', 'Vacância Física', 'hasExplicitValue', 'keepZeroIndicator']:
    if needle not in text:
        missing.append(f'Continuação 9 ausente em B3NetworkService: {needle}')
for needle in ['Completa o grid com indicadores vindos do bundle avançado', 'chartBundle?.indicatorCards']:
    if needle not in asset_detail:
        missing.append(f'AssetDetailModal não mescla indicadores do bundle avançado: {needle}')
for needle in ['testMergedRootAndResultsNormalizedForFii', 'testFundamentalistIndicatorAlternativeShapesAreParsed']:
    if needle not in parser_test:
        missing.append(f'Testes sem cobertura da continuação 9: {needle}')

for needle in ['legacyAppCompat', 'officialAppContractVersion', '21.12.54-total-apk-proxy-contract', 'testProxyV211254LegacyCompatContractIsParsed']:
    joined_contract = text + '\n' + parser_test
    if needle not in joined_contract:
        missing.append(f'Contrato APK/Proxy v21.12.54 sem cobertura: {needle}')


for needle in ['testRevenueBreakdownParsesHighchartsAndApexShapes', 'testRevenueBreakdownPreservesYearMappedProxyShapes', 'testRevenueBreakdownParsesAppContractFieldValueShape']:
    if needle not in parser_test:
        missing.append(f'Testes sem cobertura de faturamento região/negócio: {needle}')
for needle in ['parseBreakdownPointsFromObject', 'shouldParseObjectAsSingleBreakdown', 'hasUsableBreakdownMap']:
    if needle not in text:
        missing.append(f'Parser de faturamento região/negócio sem hardening: {needle}')

if wrapper_warnings:
    print("WARNINGS:", *wrapper_warnings, sep="\n- ")

if missing:
    print('\n'.join(missing))
    raise SystemExit(1)
print('Valorae Proxy integration audit OK')
