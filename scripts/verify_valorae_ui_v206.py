from pathlib import Path
root = Path(__file__).resolve().parents[1]
build = (root / 'app/build.gradle.kts').read_text(encoding='utf-8')
update = (root / 'update.json').read_text(encoding='utf-8')
b3 = (root / 'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
charts = (root / 'app/src/main/java/com/example/ui/components/AssetCharts.kt').read_text(encoding='utf-8')
analysis = (root / 'app/src/main/java/com/example/ui/screens/AnalysisScreen.kt').read_text(encoding='utf-8')
detail = (root / 'app/src/main/java/com/example/ui/components/AssetDetailModal.kt').read_text(encoding='utf-8')
all_src = '\n'.join([b3, charts, analysis, detail])
checks = {
    'Versão do app v2.0.6+ / code 16+': (('versionName = "2.0.6"' in build and 'versionCode = 16' in build) or ('versionName = "2.0.7"' in build and 'versionCode = 17' in build)) and (('\"versionName\": \"2.0.6\"' in update and '\"latestVersionCode\": 16' in update) or ('\"versionName\": \"2.0.7\"' in update and '\"latestVersionCode\": 17' in update)),
    'Painel mantém abas DRE e Negócios para ações': 'listOf("Análise", "Dividendos", "Comparação", "DRE", "Negócios")' in charts and 'StockDreTab(bundle)' in charts and 'StockBusinessTab(bundle)' in charts,
    'Faturamento por região lê revenueBreakdowns do appPayload/mobile': 'appPayload.charts.revenueBreakdowns.revenueGeography' in b3 and 'appMobileSnapshot.revenueBreakdowns.revenueByRegion' in b3,
    'Faturamento por negócio lê revenueBreakdowns do appPayload/mobile': 'appPayload.charts.revenueBreakdowns.revenueByBusiness' in b3 and 'appMobileSnapshot.revenueBreakdowns.revenueByBusiness' in b3,
    'Parser aceita aliases distribuicaoFaturamento': 'distribuicaoFaturamento.negocios' in b3 and 'distribuicaoFaturamento.regioes' in b3,
    'Parser mantém contratos antigos revenueSegment/revenueGeography': 'results.optAny("revenueSegment")' in b3 and 'results.optAny("revenueGeography")' in b3,
    'Sem simulação/fallback proibido de mercado': all(term not in all_src for term in ['P/VP Máximo Alvo', 'Benjamin Graham', 'Décio Bazin', 'Preço Teto', 'Cota do Infinito', 'DY / 12']),
    'Não usa preço médio como cotação atual nos gráficos': 'precoMedio' not in charts and 'averagePrice as currentPrice' not in all_src,
}
failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL') + ' - ' + name)
if failed:
    raise SystemExit('Falhas na auditoria UI v2.0.6+: ' + ', '.join(failed))
print('VALORAE UI v2.0.6+ revenue breakdown audit OK')
