#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
checks = [
    (root/'app/src/main/java/com/example/network/B3NetworkService.kt', '/api/v1/asset/quality', 'Raio-X: qualidade do ativo'),
    (root/'app/src/main/java/com/example/network/B3NetworkService.kt', '/api/v1/asset/action-plan', 'Raio-X: plano de ação'),
    (root/'app/src/main/java/com/example/network/B3NetworkService.kt', '/api/v1/asset/source-map', 'Raio-X: mapa de fontes'),
    (root/'app/src/main/java/com/example/network/B3NetworkService.kt', '/api/v1/fii/income', 'Central FII: renda'),
    (root/'app/src/main/java/com/example/network/B3NetworkService.kt', '/api/v1/fii/vacancy', 'Central FII: vacância'),
    (root/'app/src/main/java/com/example/network/B3NetworkService.kt', '/api/v1/fii/checklist', 'Central FII: checklist'),
    (root/'app/src/main/java/com/example/network/B3NetworkService.kt', '/api/v1/portfolio/rebalance', 'Carteira: rebalanceamento dedicado'),
    (root/'app/src/main/java/com/example/network/B3NetworkService.kt', '/api/v1/portfolio/risk', 'Carteira: risco dedicado'),
    (root/'app/src/main/java/com/example/network/B3NetworkService.kt', '/api/v1/portfolio/income', 'Carteira: renda dedicada'),
    (root/'app/src/main/java/com/example/network/B3NetworkService.kt', '/api/v1/watchlist/analyze', 'Radar / Watchlist'),
    (root/'app/src/main/java/com/example/network/B3NetworkService.kt', '/api/v1/engine/maturity', 'Diagnóstico: maturidade do motor'),
    (root/'app/src/main/java/com/example/network/B3NetworkService.kt', '/api/v1/cache/stats', 'Diagnóstico: cache stats'),
    (root/'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt', 'refreshProxyCapabilities', 'ViewModel expõe refresh de capacidades'),
    (root/'app/src/main/java/com/example/ui/screens/ProxyToolsScreen.kt', 'fun ProxyToolsScreen', 'Blocos técnicos preservados fora da barra inferior'),
    (root/'app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'Diagnóstico do VALORAE', 'Diagnóstico amigável mantido em Configurações'),
]
failed=[]
for path, needle, label in checks:
    text = path.read_text(encoding='utf-8') if path.exists() else ''
    if needle not in text:
        failed.append(f'{label}: ausente ({needle})')
    else:
        print(f'OK - {label}')
if failed:
    print('\n'.join(failed))
    raise SystemExit(1)
print('Valorae Proxy recommendations implementation audit OK')
