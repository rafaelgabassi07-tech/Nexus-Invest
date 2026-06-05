#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SERVICE = ROOT / 'app/src/main/java/com/example/network/B3NetworkService.kt'
BUILD = ROOT / 'app/build.gradle.kts'

def read(path: Path) -> str:
    if not path.exists():
        raise SystemExit(f'FAIL: arquivo ausente: {path}')
    return path.read_text(encoding='utf-8')

service = read(SERVICE)
build = read(BUILD)
checks = []

def check(name, ok):
    checks.append((name, bool(ok)))

check('versionName 2.0.8', 'versionName = "2.0.8"' in build)
check('versionCode 18', 'versionCode = 18' in build)
check('JSONObject.optAny navega paths aninhados com ponto', "key.contains('.')" in service and 'node.has(part)' in service and 'is JSONArray ->' in service)
check('optObject delega para optAny path-aware', 'private fun JSONObject.optObject(path: String)' in service and 'return optAny(path) as? JSONObject' in service)
check('optArray delega para optAny path-aware', 'private fun JSONObject.optArray(path: String)' in service and 'return optAny(path) as? JSONArray' in service)
check('helper filterChartSeriesByKeywords existe', 'private fun filterChartSeriesByKeywords' in service)
check('parser financeiro preserva identidade de séries', 'parseFinancialSeriesPoints' in service and 'financialFieldFromLabel(seriesLabel)' in service)
check('parser lucro x cotação combina séries separadas', 'parseProfitVsQuotePointsFromChartSeriesArray' in service)
check('parser rentabilidade nominal vs real por chartSeries', 'parseProfitabilityReturnsFromChartSeries' in service)
check('payout histórico aceita séries nomeadas', 'payoutChartSeries' in service and 'contains("payout")' in service)
check('DRE usa chart series e contratos alternativos', 'dreChartSeries' in service and 'receitasLucros' in service and 'revenueProfit' in service)
check('Lucro x cotação usa chart series e aliases', 'profitQuoteChartSeries' in service and 'lucroCotacao' in service and 'profitVsQuote' in service)
check('Balanço patrimonial usa chart series e aliases', 'equityChartSeries' in service and 'balanceSheet' in service and 'balancoPatrimonial' in service)
check('Faturamento região lê aliases nested appPayload/appMobileSnapshot', 'appPayload.charts.revenueBreakdowns.geography' in service and 'appPayload.charts.revenueBreakdowns.byRegion' in service and 'appMobileSnapshot.revenueBreakdowns.regions' in service)
check('Faturamento negócio lê aliases nested appPayload/appMobileSnapshot', 'appPayload.charts.revenueBreakdowns.business' in service and 'appPayload.charts.revenueBreakdowns.byBusiness' in service and 'appMobileSnapshot.revenueBreakdowns.segments' in service)
check('Contrato amplo lê appPayload', 'appPayload' in service)
check('Contrato amplo lê appMobileSnapshot', 'appMobileSnapshot' in service)
check('Contrato amplo lê assetClassContract/groups/statements', 'assetClassContract' in service and 'groups.statements' in service)
check('Sem DY/12 simulado', 'DY / 12' not in service and 'dy / 12' not in service and 'dividendYield / 12' not in service and 'dividend_yield / 12' not in service)
check('Sem Cota do Infinito', 'Cota do Infinito' not in service)
check('Sem valuation Graham/Bazin local', 'Graham' not in service and 'Bazin' not in service)
check('Sem preço teto local', 'preço teto' not in service.lower() and 'preco teto' not in service.lower())

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(('OK  ' if ok else 'FAIL') + ' - ' + name)
if failed:
    raise SystemExit('\nVALORAE UI v2.0.8 graph audit FAILED:\n- ' + '\n- '.join(failed))
print('\nVALORAE UI v2.0.8 graph audit OK')
