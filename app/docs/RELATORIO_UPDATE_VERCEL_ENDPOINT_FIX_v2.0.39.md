# RELATÓRIO — VALORAE v2.0.39 — Vercel Update Manifest Endpoint Fix

## Objetivo

Corrigir o sistema de atualização para usar o Vercel como fonte oficial do manifesto de atualização, mantendo o APK publicado no GitHub Releases.

## Fluxo final

```text
App VALORAE
  -> consulta https://app-atualizacoes.vercel.app/update.json
  -> lê downloadUrl/apkUrl/apk_url
  -> baixa o APK publicado no GitHub Releases
  -> salva no cache do app
  -> abre o instalador Android via FileProvider
```

## Arquivos alterados

- `app/build.gradle.kts`
  - `versionCode` atualizado para `49`.
  - `versionName` atualizado para `2.0.39`.
  - adicionada constante `BuildConfig.VALORAE_UPDATE_MANIFEST_URL`.

- `app/src/main/java/com/example/MainActivity.kt`
  - troca da URL direta do GitHub Raw por `BuildConfig.VALORAE_UPDATE_MANIFEST_URL`.

- `app/src/main/java/com/example/ui/components/SystemUpdateCenterDialog.kt`
  - troca da URL direta do GitHub Raw por `BuildConfig.VALORAE_UPDATE_MANIFEST_URL`.

- `update.json` e `version.json`
  - alinhados para v2.0.39 / versionCode 49.

- `metadata.json`
  - alinhado para v2.0.39 / versionCode 49.

## Observação importante

O app Android agora aponta corretamente para o Vercel, mas o manifesto publicado no Vercel precisa conter a URL HTTPS real do APK final publicado no GitHub Releases.

Modelo esperado:

```json
{
  "latestVersionCode": 49,
  "versionName": "2.0.39",
  "downloadUrl": "https://github.com/rafaelgabassi07-tech/app-atualizacoes/releases/download/v2.0.39/APK_VALORAE_v2.0.39.apk"
}
```

## Build

Não foi executado build Gradle neste ambiente. O projeto deve ser compilado no Android Studio/Google AI Studio ou ambiente com acesso ao Gradle/dependências.
