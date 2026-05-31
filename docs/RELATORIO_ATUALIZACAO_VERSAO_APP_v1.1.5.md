# Atualização de versão do APK VALORAE

## Versão do aplicativo

A versão real do aplicativo Android foi atualizada no Gradle:

- `versionCode`: `7` -> `8`
- `versionName`: `1.1.4` -> `1.1.5`

## Nome correto do pacote

O pacote foi renomeado seguindo a regra correta: usar a versão do **APP/APK** após as modificações, e não a versão do VALORAE Proxy.

Arquivo final:

```text
APK VALORAE v1.1.5.zip
```

## Observação

Esta atualização corrige a versão/nomeação do pacote final. A integração com o VALORAE Proxy, otimizações, Rankings, Home, Insights e Proxy+ permanecem preservados da última auditoria consolidada.

## Validação recomendada no Android Studio

```bash
./gradlew clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
./gradlew :app:lintDebug :app:check --stacktrace --info
```
