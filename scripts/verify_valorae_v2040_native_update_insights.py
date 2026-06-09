#!/usr/bin/env python3
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
checks = []

def read(path):
    return (root / path).read_text(encoding='utf-8')

def ok(name, condition):
    checks.append((name, bool(condition)))

build = read('app/build.gradle.kts')
manager = read('app/src/main/java/com/example/network/UpdateManager.kt')
api = read('app/src/main/java/com/example/network/UpdateApiService.kt')
receiver = read('app/src/main/java/com/example/network/UpdateInstallReceiver.kt')
manifest = read('app/src/main/AndroidManifest.xml')
main = read('app/src/main/java/com/example/MainActivity.kt')
charts = read('app/src/main/java/com/example/ui/screens/ChartsScreen.kt')
vm = read('app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt')
dialog = read('app/src/main/java/com/example/ui/components/SystemUpdateCenterDialog.kt')
update_json = json.loads(read('update.json'))
version_json = json.loads(read('version.json'))

ok('versionName 2.0.40', 'versionName = "2.0.40"' in build)
ok('versionCode 50', 'versionCode = 50' in build)
ok('Vercel endpoint', 'https://app-atualizacoes.vercel.app/update.json' in build)
ok('PackageInstaller.Session', 'PackageInstaller.SessionParams' in manager and '.commit(' in manager)
ok('FileProvider fallback', 'launchFileProviderInstaller' in manager and 'FileProvider.getUriForFile' in manager)
ok('SHA-256 validation', 'normalizedSha256' in api and 'calculateSha256' in manager)
ok('APK package metadata validation', 'getPackageArchiveInfo' in manager and 'archiveInfo.packageName' in manager)
ok('UpdateInstallReceiver registered', 'UpdateInstallReceiver' in manifest and 'UpdateInstallReceiver' in receiver)
ok('Manifest fallback api/update', 'api/update' in manager and 'version.json' in manager)
ok('Insights click forces portfolio analytics', 'refreshPortfolioAnalytics(force = true)' in main)
ok('ChartsScreen portfolio signature refresh', 'insightsPortfolioSignature' in charts and 'refreshPortfolioAnalytics(force = needsHardRefresh)' in charts)
ok('Optimistic local analytics state', 'Carteira local recalculada; sincronizando VALORAE Proxy' in vm)
ok('Native installer UI states', 'NativeInstallStageCard' in dialog and 'NativeInstallStarted' in dialog)
ok('update.json v2.0.40', update_json.get('latestVersionCode') == 50 and update_json.get('versionName') == '2.0.40')
ok('version.json v2.0.40', version_json.get('version_code') == 50 and version_json.get('latest_version') == '2.0.40')

failed = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(f"{'OK' if passed else 'FAIL'} - {name}")
if failed:
    raise SystemExit(f"Falharam {len(failed)} verificações: {failed}")
