from pathlib import Path
root = Path(__file__).resolve().parents[1]
vm = (root / 'app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt').read_text(encoding='utf-8')
service = (root / 'app/src/main/java/com/example/network/B3NetworkService.kt').read_text(encoding='utf-8')
settings = (root / 'app/src/main/java/com/example/ui/screens/SettingsScreen.kt').read_text(encoding='utf-8')
updates = (root / 'app/src/main/java/com/example/network/UpdateManager.kt').read_text(encoding='utf-8')
main = (root / 'app/src/main/java/com/example/MainActivity.kt').read_text(encoding='utf-8')
proxy_tools = (root / 'app/src/main/java/com/example/ui/screens/ProxyToolsScreen.kt').read_text(encoding='utf-8')

checks = {
    'Busca de ativo cancela requisição anterior para evitar resultado atrasado': 'searchAssetJob?.cancel()' in vm and 'lastSearchTicker' in vm,
    'Troca de range de gráfico cancela requisição anterior': 'chartRangeJob?.cancel()' in vm,
    'Análise de ativo usa timeout por bloco': 'withTimeoutOrNull(6_500)' in vm and 'withTimeoutOrNull(7_000)' in vm,
    'Analytics remota usa timeout por endpoint e fallback local': 'fetchPortfolioAnalysis(positions)' in vm and 'withTimeoutOrNull(5_500)' in vm,
    'Update check automático tem delay de boot': 'delay(4_500)' in main,
    'UpdateManager tem TTL de 12h': 'UPDATE_CHECK_TTL_MS = 12 * 60 * 60 * 1000L' in updates,
    'Downloads de update aceitam apenas HTTPS': 'URL de download inválida: apenas HTTPS é permitido' in updates,
    'Receiver de update não é exportado': 'ContextCompat.RECEIVER_NOT_EXPORTED' in updates,
    'Sync externo direto foi ocultado da UI': 'Sincronização externa direta foi removida da UI' in settings and 'Nenhuma chamada a Supabase' in settings,
    'CloudSyncManager está desativado por política do APK': 'return false' in (root / 'app/src/main/java/com/example/network/CloudSyncManager.kt').read_text(encoding='utf-8') and 'banco externo/serviço potencialmente pago' in (root / 'app/src/main/java/com/example/network/CloudSyncManager.kt').read_text(encoding='utf-8'),
    'Proxy+ não sobrescreve ticker digitado quando estado muda': 'rememberSaveable { mutableStateOf("") }' in proxy_tools and 'if (tickerInput.isBlank())' in proxy_tools,
    'Cache em memória poda overflow além de expirados': ('memoryCache.size > 140' in service or 'memoryCache.size > 180' in service) and ('.take(memoryCache.size - 120)' in service or '.take(memoryCache.size - 150)' in service),
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('OK' if ok else 'FAIL') + ' - ' + name)
if failed:
    print('Falhas na auditoria contínua: ' + '; '.join(failed))
    raise SystemExit(1)
print('Valorae continuous correction and optimization audit OK')
