from pathlib import Path
root = Path(__file__).resolve().parents[1]
checks = []
for rel in ['apk/app/src/main/java/com/example/network/B3NetworkService.kt', 'app/src/main/java/com/example/network/B3NetworkService.kt']:
    p = root / rel
    if p.exists():
        s = p.read_text()
        checks.extend([
            (f'{rel}: consome assetChartsCoverage', 'assetChartsCoverage' in s and 'requiredMissing' in s),
            (f'{rel}: consome canonical FII', 'val canonicalFii' in s and 'fii.distribution12m' in s),
            (f'{rel}: distribuições 12m canônicas', 'appendFiiDistribution12mFromAny' in s and 'addFiiDistribution12mPoint' in s),
            (f'{rel}: commodity canonical', 'canonicalCharts?.optAny("commodityComparison")' in s),
            (f'{rel}: não sobrescreve parsing por fallback pobre', 'assetChartsCanonical' in s and 'no-synthetic' not in s.lower()),
        ])
builds = []
for rel in ['apk/app/build.gradle.kts', 'app/build.gradle.kts']:
    p = root / rel
    if p.exists(): builds.append(p.read_text())
checks.append(('versionName 2.0.17/versionCode 27', any('versionName = "2.0.17"' in b and 'versionCode = 27' in b for b in builds)))
failed = [name for name, ok in checks if not ok]
if failed:
    print('FAILED')
    for f in failed:
        print('-', f)
    raise SystemExit(1)
print('STATIC_VALORAE_COMPLETE_I10_CHARTS_V217_OK')
