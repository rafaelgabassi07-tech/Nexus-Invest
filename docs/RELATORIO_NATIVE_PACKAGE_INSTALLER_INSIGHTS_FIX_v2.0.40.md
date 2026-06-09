# VALORAE v2.0.40 — Native Package Installer Flow + Insights Refresh Fix

Data: 2026-06-08

## Objetivo

Evoluir o sistema de atualização para um fluxo com aparência mais nativa no Android e corrigir a página de Insights, que podia permanecer sem recalcular dados mesmo quando a carteira local já possuía ativos.

## Atualização in-app

### O que mudou

- Mantido o modelo gratuito: Vercel como manifesto e GitHub Releases como hospedagem do APK.
- Mantido endpoint principal: `https://app-atualizacoes.vercel.app/update.json`.
- Adicionado suporte a fallback: `/api/update`, `/update.json` e `/version.json`.
- Reduzido TTL de checagem automática para 3 horas, sem afetar verificação manual forçada.
- Substituído o fluxo primário de abertura direta por `PackageInstaller.Session`.
- Mantido fallback via `FileProvider` caso a sessão nativa falhe no dispositivo.
- Preservada a restrição segura do Android: a instalação ainda depende da confirmação do usuário.

### Validações adicionadas antes da instalação

- O APK precisa existir e ter tamanho válido.
- O arquivo precisa começar com assinatura ZIP/APK (`PK`).
- O pacote do APK precisa ser o mesmo pacote instalado no VALORAE.
- O `versionCode` do APK precisa ser maior que o app instalado.
- O `versionCode` do APK precisa conferir com o manifesto.
- Se `sha256`/`sha_256` for informado no manifesto, o hash local precisa bater.
- Se `fileSizeBytes`/`file_size_bytes` for informado no manifesto, o tamanho local precisa bater.

### Arquivos alterados

- `app/src/main/java/com/example/network/UpdateManager.kt`
- `app/src/main/java/com/example/network/UpdateApiService.kt`
- `app/src/main/java/com/example/network/UpdateInstallReceiver.kt`
- `app/src/main/java/com/example/ui/components/SystemUpdateCenterDialog.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `update.json`
- `version.json`
- `metadata.json`

## Insights

### Causa provável encontrada

A aba Insights dependia principalmente do pré-aquecimento assíncrono do ViewModel e, ao tocar na aba, o app forçava somente os rankings de mercado. Em cenários de carteira recém-importada/adicionada, os ativos já apareciam no Dashboard, mas os cards de Insights podiam continuar exibindo estado antigo, ranking de mercado ou aparência de “aguardando carteira”.

### Correções aplicadas

- O clique na aba Insights agora chama `refreshPortfolioAnalytics(force = true)` antes de abrir a página.
- `ChartsScreen` ganhou uma assinatura local da carteira para disparar atualização quando a aba é composta e quando os ativos/transações mudam.
- O `PortfolioViewModel` agora aplica atualização otimista/local imediatamente: análise local, histórico local e IPCA fallback aparecem antes das chamadas remotas terminarem.
- As chamadas ao Proxy continuam acontecendo em seguida para enriquecer histórico, rankings, proventos e agenda, sem deixar a UI visualmente parada.

### Arquivos alterados

- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`

## Vercel

O projeto Vercel complementar foi atualizado para v2.0.40 com:

- `update.json`
- `version.json`
- `api/update.js`
- `README.md`

## Observações para publicação

Após compilar o APK final e publicar no GitHub Releases:

1. Confirme que a tag é `v2.0.40`.
2. Confirme que o arquivo se chama `APK_VALORAE_v2.0.40.apk` ou ajuste as URLs no Vercel.
3. Preencha `fileSize` e `fileSizeBytes`.
4. Gere o SHA-256 do APK e preencha `sha256` e `sha_256`.
5. Faça deploy do projeto `app-atualizacoes` no Vercel.

