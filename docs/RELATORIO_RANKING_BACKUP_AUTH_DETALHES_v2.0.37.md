# RELATÓRIO — VALORAE APK v2.0.37

## Escopo
Correção dos problemas confirmados nas imagens enviadas pelo usuário:

1. Ranking da Home mostrando placeholders quebrados abaixo do ticker (`+, %`, `-, %`).
2. Backup e Dados falhando ao abrir o seletor de arquivos.
3. Autenticação não reativando de forma confiável ao retornar do segundo plano/Recentes.
4. Ranking da Home demorando demais para preencher.
5. Detalhes do Ativo usando cache local/baixo percentual de completude mesmo quando a Análise recebe dados ricos.

## Correções aplicadas

### Ranking da Home
- A variação percentual voltou a aparecer abaixo do ticker.
- Placeholders sem número, como `+, %`, `-, %`, `%` ou textos quebrados, são descartados.
- Se a variação não vier do Proxy, a linha inferior fica limpa em vez de poluir o ranking.
- O preço continua à direita e a posição `#1`, `#2`, etc. continua preservada.

Arquivos:
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`

### Ranking com menor espera na abertura
- A abertura do app agora faz chamada rápida de ranking ao invés de forçar captura completa.
- O pré-carregamento do ranking começa após 350 ms, não mais após 1200 ms.
- A chamada rápida usa timeout menor e não disputa a tela inicial com ranking fundamentalista completo.

Arquivo:
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`

### Backup e Dados / seletor de arquivos
- Substituído o fluxo frágil por `ACTION_OPEN_DOCUMENT` via `StartActivityForResult`.
- Adicionado `Intent.createChooser` para permitir escolher o app de arquivos/gerenciador.
- Adicionados flags `FLAG_GRANT_READ_URI_PERMISSION` e `FLAG_GRANT_PERSISTABLE_URI_PERMISSION`.
- Mantido fallback `GetContent`.
- Removido bug de leitura JSON duplicada.
- Adicionada permissão legada `READ_EXTERNAL_STORAGE` somente até Android 12L (`maxSdkVersion=32`).

Arquivos:
- `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`
- `app/src/main/AndroidManifest.xml`

### Autenticação ao retornar do segundo plano
- Além de `ON_STOP`, o app agora registra `ON_PAUSE` e revalida em `ON_RESUME`.
- Se o app perdeu foco e voltou após intervalo real, a carteira volta para bloqueio.
- Isso cobre casos em que alguns launchers/Recentes não disparam `ON_STOP` de forma previsível.

Arquivo:
- `app/src/main/java/com/example/MainActivity.kt`

### Detalhes do Ativo
- A tela não considera mais uma cotação simples em cache como “dados ricos suficientes”.
- Mesmo se houver preço local/cache, Detalhes do Ativo busca snapshot rico do Proxy.
- Timeout do snapshot rico subiu para 6,5 s.
- A tela continua usando fallback local para não abrir vazia, mas passa a tentar enriquecer dados do Proxy.

Arquivo:
- `app/src/main/java/com/example/ui/components/AssetDetailModal.kt`

## Validação estática

Resultado da auditoria local:

```text
ranking_change_line_restored: OK
ranking_placeholder_guard: OK
saf_uses_start_activity_result: OK
saf_has_legacy_permission_launcher: OK
saf_no_duplicate_json_read: OK
auth_locks_on_resume_after_background: OK
details_requires_rich_proxy_data: OK
home_ranking_fast_non_full: OK
manifest_legacy_storage_permission: OK
version_2_0_37: OK
```

## Build

Tentativa executada:

```bash
./gradlew --no-daemon assembleDebug
```

Resultado no ambiente isolado:

```text
UnknownHostException: services.gradle.org
```

A falha ocorreu antes da compilação Kotlin, ao baixar a distribuição Gradle. O build final precisa ser feito em ambiente com internet.

## Versão

- `versionName`: `2.0.37`
- `versionCode`: `47`
