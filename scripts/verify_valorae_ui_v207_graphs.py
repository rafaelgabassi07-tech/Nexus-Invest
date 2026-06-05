from pathlib import Path

root = Path(__file__).resolve().parents[1]
charts = (root / 'app/src/main/java/com/example/ui/components/AssetCharts.kt').read_text(encoding='utf-8')
service = (root / 'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
build = (root / 'app/build.gradle.kts').read_text(encoding='utf-8')
all_src = '\n'.join(p.read_text(encoding='utf-8', errors='ignore') for p in (root / 'app/src/main/java').rglob('*.kt'))

checks = {
    'Versão do app atualizada para 2.0.7 / code 17': 'versionCode = 17' in build and 'versionName = "2.0.7"' in build,
    'AssetChartBundle expõe todos os 7 gráficos solicitados': all(token in charts for token in [
        'DRE: Receitas x Lucros', 'Evolução Lucro x Cotação', 'Balanço Patrimonial: Ativo/PL/Passivo',
        'Payout Histórico (%)', 'Faturamento por Negócio (%)', 'Faturamento por Região (%)', 'Rentabilidade Nominal vs Real'
    ]),
    'DRE Receitas x Lucros usa bundle.revenueProfit e parser multiformato': 'bundle.revenueProfit' in charts and 'parseFirstFinancialStatementPoints' in service and 'appPayload.charts.receitasLucros' in service and 'assetClassContract.groups.statements.fields.revenueProfit.value' in service,
    'Evolução lucro x cotação usa bundle.profitVsQuote e aceita aliases': 'bundle.profitVsQuote' in charts and 'parseFirstProfitVsQuotePoints' in service and 'appPayload.charts.profitVsQuote' in service and 'assetClassContract.groups.statements.fields.quoteProfit.value' in service,
    'Balanço patrimonial usa bundle.equityEvolution e aceita Ativo/PL/Passivo': 'bundle.equityEvolution' in charts and 'AssetEquityEvolutionChart' in charts and 'totalAssets' in service and 'totalLiabilities' in service and 'netWorth' in service and 'assetClassContract.groups.statements.fields.balanceSheet.value' in service,
    'Payout Histórico usa somente histórico recebido do Proxy/contrato': 'bundle.payoutHistory' in charts and 'appendPayoutHistoryFromAny' in service and 'appPayload.charts.payoutHistorico' in service and 'assetClassContract.groups.statements.fields.payoutHistory.value' in service,
    'Faturamento por negócio lê caminhos v21.12.58 e aliases antigos': 'bundle.revenueByBusiness' in charts and 'appPayload.charts.revenueBreakdowns.revenueByBusiness' in service and 'appMobileSnapshot.revenueBreakdowns.revenueByBusiness' in service and 'distribuicaoFaturamento.negocios' in service,
    'Faturamento por região lê caminhos v21.12.58 e aliases antigos': 'bundle.revenueByRegion' in charts and 'appPayload.charts.revenueBreakdowns.revenueGeography' in service and 'appMobileSnapshot.revenueBreakdowns.revenueByRegion' in service and 'distribuicaoFaturamento.regioes' in service,
    'Rentabilidade nominal vs real lê appPayload/appMobileSnapshot/assetClassContract': 'bundle.profitability' in charts and 'bundle.realProfitability' in charts and 'appPayload.charts.rentabilidade' in service and 'appMobileSnapshot.charts.profitability' in service and 'assetClassContract.groups.performance.fields.rentabilidade.value' in service,
    'Não há rótulo de estimativa para variação 12M': 'label = "Est. 12M"' not in service,
    'Sem gráficos simulados/fallback proibido para mercado': all(term not in all_src for term in ['P/VP Máximo Alvo', 'Preço Teto', 'Cota do Infinito', 'DY / 12', 'mockChart', 'fakeChart']),
    'Preço médio não é usado como cotação atual nos gráficos': 'precoMedio' not in charts and 'averagePrice as currentPrice' not in all_src,
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL') + ' - ' + name)
if failed:
    raise SystemExit('Falhas na auditoria UI v2.0.7 graphs: ' + '; '.join(failed))
print('VALORAE UI v2.0.7 requested charts audit OK')
