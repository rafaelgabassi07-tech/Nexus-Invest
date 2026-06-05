#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parents[1]
main = (root/'app/src/main/java/com/example/MainActivity.kt').read_text(encoding='utf-8')
settings = (root/'app/src/main/java/com/example/ui/screens/SettingsScreen.kt').read_text(encoding='utf-8')
build = (root/'app/build.gradle.kts').read_text(encoding='utf-8')
checks = {
    'Aba técnica Proxy+ removida da barra inferior': 'ProxyToolsScreen(' not in main and 'Proxy+' not in main and 'activePage == 5' not in main and 'activePage = 5' not in main,
    'Estados antigos da navegação são normalizados': 'activePage !in 0..4' in main and 'else -> DashboardScreen(' in main,
    'Diagnóstico do Proxy virou painel amigável': all(x in settings for x in ['Diagnóstico do VALORAE', 'Recebimento de dados', 'Cache e proteção contra perda', 'DETALHES TÉCNICOS', 'DiagnosticUserCard']),
    'Diagnóstico força atualização manual real': 'viewModel.refreshProxyHealth(force = true)' in settings,
    'Bloqueio biométrico valida disponibilidade no boot': all(x in main for x in ['canUseDeviceAuth', 'onDisableBiometric', 'setBiometricEnabled(false)', 'onAuthenticationFailed']),
    'Ativação de biometria exige autenticação prévia': all(x in settings for x in ['requestAuthenticationToEnable', 'onAuthenticationSucceeded', 'setBiometricEnabled(true)', 'setAllowedAuthenticators(authenticators)']),
    'Versão do app atualizada para a entrega': ('versionCode = 10' in build or 'versionCode = 12' in build) and ('versionName = "1.1.7"' in build or 'versionName = "1.1.9"' in build),
}
failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL'), '-', name)
if failed:
    raise SystemExit('Falhas na verificação v1.1.7/v1.1.9: ' + '; '.join(failed))
print('Valorae navigation, diagnostics and biometric audit OK')
