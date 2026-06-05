from pathlib import Path
import re, sys
root = Path(__file__).resolve().parents[1]
vm = (root/'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text()
net = (root/'app/src/main/java/com/example/network/B3NetworkService.kt').read_text()
dash = (root/'app/src/main/java/com/example/ui/screens/DashboardScreen.kt').read_text()
build = (root/'app/build.gradle.kts').read_text()
readme = (root/'README.md').read_text()
update = (root/'update.json').read_text()
checks = {
    'App versionName atualizado para 1.1.8': 'versionName = "1.1.9"' in build,
    'App versionCode atualizado para 11': 'versionCode = 12' in build,
    'Manifesto local update.json acompanha versão 1.1.8': '"versionName": "1.1.9"' in update and '"latestVersionCode": 12' in update,
    'Busca de ativo limpa dados antigos ao trocar ticker': 'previousTicker != null && previousTicker != clean' in vm and '_searchQueryResult.value = null' in vm,
    'Notícias preservam último bloco bom em falha': 'Preserve o último bloco bom de notícias' in vm and 'if (force || _newsFeed.value.isEmpty())' not in vm,
    'Importação JSON aceita números e datas flexíveis': 'parseImportedNumber(value: Any?)' in vm and 'parseImportedDateMillis(value: Any?)' in vm and 'movimentacoes' in vm,
    'Planilha B3 usa parser locale-aware': 'qty = parseImportedNumber(parts[colQty])' in vm and 'price = parseImportedNumber(parts[colPrice])' in vm,
    'Planilha B3 aceita data serial do Excel': 'excelSerialDateToMillis' in vm and 'parseImportedDateMillis(parts[colDate])' in vm,
    'DARF limita venda à posição existente': 'val qtySold = sale.quantity.coerceAtMost(sharesBefore.coerceAtLeast(0.0))' in vm,
    'Tipo FII no formulário usa inferência central': 'B3NetworkService.inferIsFii(newTicker)' in dash,
    'Notícias do Proxy aceitam aliases comuns': 'root?.optJSONArray("articles")' in net and 'data.news' in net and 'headline' in net,
    'Índices do Proxy aceitam aliases comuns': 'root.optArray("data.indices")' in net and 'benchmarks' in net and 'variationPct' in net,
    'Rankings aceitam quote aninhado': 'val quote = firstObject(item.optJSONObject("quote")' in net and 'regularMarketPrice' in net,
    'Datas flexíveis aceitam ISO, BR e Excel': 'dd/MM/yyyy HH:mm' in net and 'yyyy/MM/dd' in net and '20_000L..80_000L' in net,
    'README documenta contratos /api/v1': '/api/v1/assets' in readme and '/api/v1/asset`' in readme and 'POST /api/assets`' not in readme and 'GET /api/asset`' not in readme,
}
missing = [name for name, ok in checks.items() if not ok]
if missing:
    print('Valorae deep logic/pages audit FAILED')
    for item in missing:
        print(f'- {item}')
    sys.exit(1)
print('Valorae deep logic/pages audit OK')
