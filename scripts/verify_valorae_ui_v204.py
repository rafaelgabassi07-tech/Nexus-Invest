#!/usr/bin/env python3
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
read = lambda p: (root / p).read_text(encoding='utf-8')
analysis = read('app/src/main/java/com/example/ui/screens/AnalysisScreen.kt')
detail = read('app/src/main/java/com/example/ui/components/AssetDetailModal.kt')
sections = read('app/src/main/java/com/example/ui/components/AssetProxySections.kt')
charts = read('app/src/main/java/com/example/ui/components/AssetCharts.kt')
viewmodel = read('app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt')
build = read('app/build.gradle.kts')
update = read('update.json') if (root / 'update.json').exists() else ''
all_ui = analysis + '\n' + detail + '\n' + sections

checks = {
    'Versão do app atualizada para 2.0.4 / code 14': 'versionName = "2.0.4"' in build and 'versionCode = 14' in build and '"versionName": "2.0.4"' in update and '"latestVersionCode": 14' in update,
    'Análise mantém somente três abas principais': 'val analysisTabs = listOf("Resumo & Gráficos", "Indicadores Gerais", "Perfil & Dados")' in analysis and 'mainAnalysisTabIdx == 3' not in analysis,
    'Análise usa pacote completo de gráficos do Proxy': 'AssetChartBundlePanel(' in analysis and 'Carregando pacote completo de gráficos do Proxy' in analysis,
    'Análise renderiza Indicadores Gerais via builder dinâmico': 'AssetProxyIndicatorSection(' in analysis and 'buildAssetProxyIndicatorFields' in sections,
    'Perfil & Dados centraliza perfil e notícias': 'AssetProxyProfileSection(' in analysis and 'ÚLTIMAS NOTÍCIAS DO ATIVO' in sections,
    'Modal usa o mesmo pacote de gráficos avançados': 'AssetChartBundlePanel(' in detail and 'currentRange = localChartRange' in detail,
    'Modal usa Indicadores Gerais dinâmicos': 'AssetProxyIndicatorSection(' in detail and 'Histórico de Indicadores Gerais' in detail,
    'Modal usa Perfil & Dados dinâmico': 'AssetProxyProfileSection(' in detail,
    'Sem card solto de Indicadores Fundamentalistas': 'Indicadores Fundamentalistas' not in all_ui and 'INDICADORES FUNDAMENTALISTAS' not in all_ui,
    'Sem P/VP máximo hardcoded': 'P/VP Máximo' not in all_ui and '1.00", "Parâmetro do mercado' not in all_ui,
    'Sem valuation local Graham/Bazin na análise/detalhes': not any(term in all_ui for term in ['Benjamin Graham', 'Décio Bazin', 'Preço Teto', 'ANÁLISE DE VALUATION', 'Conselho Valorae', 'CONSELHO VALORAE']),
    'Sem fallback local de preço médio como cotação': '?: avgPrice' not in viewmodel and 'price = currentPrice.takeIf { it > 0.0 } ?: averageCost' not in detail and 'realData?.price ?: asset.currentPrice' not in detail,
    'Campos ausentes são tratados como indisponíveis': 'Campos ausentes permanecem como indisponíveis' in sections and 'Gráficos indisponíveis' in analysis,
    'Ação e FII são tratados de forma adaptativa': 'if (isFii)' in sections and 'fiiPatrimonialInfo' in sections and 'revenueByBusiness' in charts and 'FiiPatrimonialTab' in charts,
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL'), '-', name)
if failed:
    raise SystemExit('Falhas na auditoria UI v2.0.4: ' + '; '.join(failed))
print('VALORAE UI v2.0.4 audit OK')
