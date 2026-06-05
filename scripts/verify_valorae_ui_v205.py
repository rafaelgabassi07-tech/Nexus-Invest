#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path):
    return (root / path).read_text(encoding='utf-8')

b3 = read('app/src/main/java/com/example/network/B3NetworkService.kt')
charts = read('app/src/main/java/com/example/ui/components/AssetCharts.kt')
analysis = read('app/src/main/java/com/example/ui/screens/AnalysisScreen.kt')
detail = read('app/src/main/java/com/example/ui/components/AssetDetailModal.kt')
build = read('app/build.gradle.kts')
update = read('update.json') if (root / 'update.json').exists() else ''

checks = {
    'Versão do app atualizada para 2.0.5 / code 15': 'versionName = "2.0.5"' in build and 'versionCode = 15' in build and '"versionName": "2.0.5"' in update and '"latestVersionCode": 15' in update,
    'Painel avançado mantém abas DRE e Negócios para ações': 'listOf("Análise", "Dividendos", "Comparação", "DRE", "Negócios")' in charts and 'StockDreTab(bundle)' in charts and 'StockBusinessTab(bundle)' in charts,
    'Página Análise usa AssetChartBundlePanel': 'AssetChartBundlePanel(' in analysis,
    'Modal Detalhes usa AssetChartBundlePanel': 'AssetChartBundlePanel(' in detail,
    'DRE aceita labels/datasets/series': 'parseFinancialChartWithLabels' in b3 and 'optJSONArray("datasets")' in b3 and 'optJSONArray("series")' in b3,
    'DRE aceita arrays nomeados e objetos por período': 'parseFinancialNamedArrays' in b3 and 'parseFinancialStatementPointsFromObject(value, key)' in b3,
    'DRE lê appPayload/appMobileSnapshot/assetClassContract': 'appPayload.charts.receitasLucros' in b3 and 'appMobileSnapshot.charts.revenueProfit' in b3 and 'assetClassContract.groups.statements.fields.receitasLucros.value' in b3,
    'Lucro x Cotação aceita labels/datasets e arrays quote/profit': 'parseFirstProfitVsQuotePoints' in b3 and 'parseProfitVsQuotePointsFromObject' in b3 and 'optJSONArray("quotes")' in b3 and 'optJSONArray("profits")' in b3,
    'Evolução patrimonial aceita balance sheet em múltiplos contratos': 'appPayload.charts.evolucaoPatrimonio' in b3 and 'balanceSheet' in b3 and 'totalAssets' in b3 and 'totalLiabilities' in b3,
    'Negócios aceita objeto/array/string e contratos revenueByBusiness': 'parseFirstBreakdownMap' in b3 and 'parseBreakdownMapFromAny' in b3 and 'revenueByBusiness' in b3 and 'appPayload.charts.negociosReceita' in b3,
    'Sem fallback local proibido de preço médio como cotação': '?: avgPrice' not in b3 and 'price = currentPrice.takeIf { it > 0.0 } ?: averageCost' not in detail,
    'Sem simulação de valuation/local para DRE ou Negócios': all(term not in (analysis + detail + charts + b3) for term in ['P/VP Máximo Alvo', 'Benjamin Graham', 'Décio Bazin', 'Preço Teto', 'Cota do Infinito', 'DY / 12']),
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL'), '-', name)
if failed:
    raise SystemExit('Falhas na auditoria UI v2.0.5: ' + '; '.join(failed))
print('VALORAE UI v2.0.5 DRE/Negócios audit OK')
