#!/usr/bin/env python3
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
read = lambda p: (root / p).read_text(encoding='utf-8')
analysis = read('app/src/main/java/com/example/ui/screens/AnalysisScreen.kt')
detail = read('app/src/main/java/com/example/ui/components/AssetDetailModal.kt')
charts = read('app/src/main/java/com/example/ui/components/AssetCharts.kt')
build = read('app/build.gradle.kts')
update = read('update.json') if (root / 'update.json').exists() else ''

checks = {
    'Versão do app atualizada para 2.0.3 / code 13': 'versionName = "2.0.3"' in build and 'versionCode = 13' in build and '"versionName": "2.0.3"' in update and '"latestVersionCode": 13' in update,
    'Tela de análise remove aba separada de Notícias/Análise VALORAE': 'val analysisTabs = listOf("Resumo & Gráficos", "Indicadores Gerais", "Perfil & Dados")' in analysis and 'mainAnalysisTabIdx == 3' not in analysis and 'Análise Valorae' not in analysis,
    'Últimas notícias do ativo ficam em Perfil & Dados': 'if (mainAnalysisTabIdx == 2)' in analysis and 'ÚLTIMAS NOTÍCIAS DE ${asset.ticker}' in analysis and 'Notícias do ativo foram movidas para a aba Perfil & Dados' in analysis,
    'Indicadores da página ficam em Indicadores Gerais': 'if (mainAnalysisTabIdx == 1)' in analysis and 'text = "INDICADORES GERAIS"' in analysis and 'Histórico de Indicadores Gerais' in analysis,
    'Modal Detalhes tem abas separadas Indicadores Gerais e Perfil & Dados': 'val mainTabs = listOf("Resumo & Gráficos", "Indicadores Gerais", "Perfil & Dados", "Minha Custódia", "Transações")' in detail and 'if (mainTabIdx == 1)' in detail and 'if (mainTabIdx == 2)' in detail,
    'Modal Detalhes mantém indicadores gerais na aba correta': 'text = "INDICADORES GERAIS"' in detail and 'Histórico de Indicadores Gerais' in detail,
    'Painel de gráficos avançados não injeta indicadores fora da aba Indicadores Gerais': 'ChartCardContainer(title = "Indicadores Fundamentalistas")' not in charts and 'ChartCardContainer(title = "Indicadores Fundamentalistas do FII")' not in charts and 'Histórico de Indicadores Gerais' not in charts,
    'Strings antigas removidas do código Kotlin': not any(old in (analysis + detail + charts) for old in ['Análise Valorae', 'INDICADORES FUNDAMENTALISTAS', 'Indicadores Fundamentalistas']),
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL'), '-', name)
if failed:
    raise SystemExit('Falhas na auditoria UI v2.0.3: ' + '; '.join(failed))
print('VALORAE UI v2.0.3 audit OK')
