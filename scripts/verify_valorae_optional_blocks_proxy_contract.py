#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parents[1]
service = (root / 'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
checks = {
    'GET/POST não descartam payload apenas por root error': 'json?.has("error") == true' not in service,
    'APK diferencia erro fatal de bloco opcional': 'private fun isFatalProxyPayload' in service and 'optionalBlock' in service,
    'News/history opcionais continuam disponíveis para fallback': 'endpoint", "").lowercase(Locale.ROOT) in setOf("asset-history", "news")' in service,
    'Mensagem de erro ainda é registrada quando fatal': 'proxyPayloadMessage' in service and 'payload fatal' in service,
}
failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL') + ' - ' + name)
if failed:
    raise SystemExit('Valorae optional-block proxy contract audit FAILED: ' + '; '.join(failed))
print('Valorae optional-block proxy contract audit OK')
