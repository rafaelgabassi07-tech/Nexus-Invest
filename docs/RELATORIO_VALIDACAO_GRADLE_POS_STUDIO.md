# RELATÓRIO DE VALIDAÇÃO GRADLE PÓS-STUDIO — VALORAE APK

## Situação recebida

O projeto corrigido informado pelo usuário passou no pipeline Gradle executado no Android Studio com Gradle nativo do sistema.

Correções confirmadas no projeto principal:

- `WarningOrange` declarado em `app/src/main/java/com/example/ui/theme/Color.kt`.
- Removido conflito de `Typography` causado por `ui/screens/Type.kt` duplicado.
- `parseLocaleFinancialNumber()` ajustado para lidar com múltiplos pontos de milhar sem vírgula decimal, como `1.453.148`.
- Endpoints principais migrados para `/api/v1/...` no projeto principal.

## Observação estrutural importante

O ZIP recebido continha duas cópias do projeto:

- uma cópia na raiz;
- outra cópia dentro de `apk/`.

A cópia dentro de `apk/` é a versão integrada e corrigida. Para evitar confusão no Android Studio, este pacote consolidado foi gerado usando o conteúdo de `apk/` como raiz do projeto.

## Resultado informado pelo usuário

- `:app:compileDebugKotlin`: passou após correções.
- `:app:compileDebugJavaWithJavac`: passou.
- `:app:processDebugManifest`: passou.
- `:app:mergeDebugResources`: passou.
- `:app:assembleDebug`: passou.
- `:app:testDebugUnitTest`: passou após correção do parser.
- `:app:lintDebug`: passou.
- `:app:check`: passou.

## Próximos testes recomendados no Android Studio

```bash
./gradlew clean :app:assembleDebug --stacktrace --info
./gradlew :app:testDebugUnitTest :app:lintDebug :app:check --stacktrace --info
```

## Testes funcionais recomendados no emulador/dispositivo

1. Abrir o app com internet ativa.
2. Confirmar status do Proxy via `/api/v1/ready`.
3. Abrir carteira e confirmar chamada batch via `/api/v1/assets`.
4. Abrir detalhe de `PETR4`, `VALE3` e um FII como `GARE11` via `/api/v1/asset`.
5. Desligar internet e confirmar uso de cache local/último snapshot bom.
6. Confirmar que resposta parcial não quebra a UI.
7. Abrir Configurações > Diagnóstico do Proxy.
8. Confirmar modo claro e modo escuro.
