from pathlib import Path
root = Path(__file__).resolve().parents[1]
main = (root/'app/src/main/java/com/example/MainActivity.kt').read_text(encoding='utf-8')
vm = (root/'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text(encoding='utf-8')
dash = (root/'app/src/main/java/com/example/ui/screens/DashboardScreen.kt').read_text(encoding='utf-8')
build = (root/'app/build.gradle.kts').read_text(encoding='utf-8')
strings = (root/'app/src/main/res/values/strings.xml').read_text(encoding='utf-8')
checks = [
    ('Tela de carregamento branded', 'BrandedLoadingScreen' in main and 'Preparando o Valorae' in main),
    ('Logo + nome Valorae reutilizável', 'ValoraeBrandLockup' in main and 'text = "Valorae"' in main),
    ('Logo preserva cores originais', 'tint = Color.Unspecified' in main),
    ('Desbloqueio biométrico com marca Valorae', '.setTitle("Valorae")' in main and 'ACESSO SEGURO' in main),
    ('Preferências carregam antes da Home/Lock', 'biometricEnabledState.value == null' in main),
    ('Keyboard ajusta sem sobrepor campos', 'SOFT_INPUT_ADJUST_RESIZE' in main),
    ('App label atualizado', '<string name="app_name">Valorae</string>' in strings),
    ('StateFlows evitam emissões iguais', vm.count('.distinctUntilChanged()') >= 3),
    ('Diagnóstico do Proxy tem timeout curto', 'withTimeoutOrNull(3_500)' in vm),
    ('Rankings completos têm timeout delimitado', 'withTimeoutOrNull(if (full) 18_000 else 14_000)' in vm),
    ('Notícias têm timeout delimitado', 'withTimeoutOrNull(5_500)' in vm),
    ('Bundles de gráficos têm timeout delimitado', 'withTimeoutOrNull(12_000)' in vm),
    ('Home usa chaves estáveis no LazyColumn', 'item(key = "portfolio_header")' in dash and 'item(key = "home_market_movers")' in dash),
    ('Home evita AnimatedVisibility redundante', 'visible = true' not in dash[:4500]),
    ('Versão v2.0.11', 'versionName = "2.0.11"' in build and 'versionCode = 21' in build),
]
failed = False
for name, ok in checks:
    print(('OK' if ok else 'FAIL') + ' - ' + name)
    failed = failed or not ok
if failed:
    raise SystemExit('STATIC_BRANDING_PERFORMANCE_CHECK_FAILED')
print('STATIC_BRANDING_PERFORMANCE_CHECK_OK')
