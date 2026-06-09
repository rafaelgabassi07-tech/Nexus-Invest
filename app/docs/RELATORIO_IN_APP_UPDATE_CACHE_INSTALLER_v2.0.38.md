# RELATÓRIO — In-App Update Cache Installer v2.0.38

## Objetivo
Implementar o fluxo de atualização por GitHub/Vercel sem salvar o APK na pasta pública Downloads do usuário.

## Correções aplicadas

- `UpdateManager.kt` não usa mais `DownloadManager.setDestinationInExternalPublicDir`.
- Download do APK agora usa OkHttp e grava em `Context.getExternalCacheDir()/valorae_updates/`.
- Fallback para `Context.getCacheDir()/valorae_updates/` quando `externalCacheDir` não estiver disponível.
- Instalação usa `FileProvider.getUriForFile(...)` e `Intent.ACTION_VIEW` com MIME `application/vnd.android.package-archive`.
- Fluxo verifica `PackageManager.canRequestPackageInstalls()` no Android O+.
- Se a permissão de instalação não estiver ativa, abre `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` para o pacote do app.
- `file_paths.xml` foi restringido para cache externo/interno do app, removendo exposição insegura de root path.
- Rotina `cleanupOldApks()` apaga APKs antigos em toda inicialização do `UpdateManager`.
- `AppUpdateInfo` agora aceita contrato antigo e novo:
  - `latestVersionCode`, `versionCode`, `versionName`, `downloadUrl`, `apkUrl`
  - `latest_version`, `version_code`, `apk_url`
- UI da Central de Atualizações mostra progresso real e não simula velocidade.

## Limite do Android
O app pode baixar o APK silenciosamente para o cache, mas não pode instalar silenciosamente em aparelho comum. O instalador do Android sempre exige consentimento do usuário, salvo cenários especiais de app de sistema, device owner/MDM ou loja privilegiada.

## Arquivos alterados

- `app/src/main/java/com/example/network/UpdateManager.kt`
- `app/src/main/java/com/example/network/UpdateApiService.kt`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/main/java/com/example/ui/components/SystemUpdateCenterDialog.kt`
- `app/build.gradle.kts`
- `metadata.json`
- `update.json`
- `version.json`

## Validação esperada

1. Abrir app.
2. Checar atualização via URL Vercel/GitHub.
3. Aceitar atualização.
4. Confirmar que nenhum APK é salvo em Downloads.
5. Confirmar que o arquivo fica em cache do app.
6. Confirmar que o instalador Android abre via `content://` do FileProvider.
7. Se a permissão de fonte desconhecida não estiver ativa, confirmar que o app abre a tela de autorização do Android.
