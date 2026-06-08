# Relatório de auditoria de regressões — VALORAE APK v2.0.23

## Objetivo

Revisar possíveis regressões introduzidas durante as alterações das versões recentes, especialmente após a integração da lógica inspirada no Vesto e a restauração da Carteira/Rankings/Proxy.

## Regressões encontradas

1. **Pacote duplicado `apk/` dentro do ZIP**
   - Havia uma cópia antiga do projeto dentro da pasta `apk/`.
   - Essa cópia interna apontava para versão `2.0.19`, enquanto a raiz correta estava em `2.0.22`.
   - Risco: Gemini/Android Studio abrir a pasta errada e compilar versão antiga.
   - Correção: pasta duplicada removida do pacote final.

2. **APK vazio de 0 bytes**
   - Dentro da cópia duplicada havia `apk/.build-outputs/app-debug.apk` com 0 bytes.
   - Risco: usuário confundir arquivo vazio com APK instalável.
   - Correção: arquivo removido. O pacote final é projeto-fonte Android e deve ser compilado.

3. **`update.json` desatualizado**
   - O manifesto de atualização local ainda indicava `versionName = 2.0.19` e `latestVersionCode = 29`.
   - Risco: central de atualização exibir informações antigas.
   - Correção: atualizado para `versionName = 2.0.23` e `latestVersionCode = 33`.

## Pontos verificados e preservados

- Aba inferior **Ativos** continua presente.
- Página de carteira/ativos continua acomodando **Meus Ativos** e **Histórico de Compras**.
- Fluxo de **Nova Transação**, edição e exclusão continua disponível via `AssetsScreen` e `AddTransactionDialog`.
- Card compacto de rankings na Home continua presente com **ALTAS / BAIXAS**.
- Botão **Ver Ranking Completo** continua removido.
- Compatibilidade com VALORAE Proxy mantida:
  - `mode=complete`
  - `complete=true/1`
  - `includeHistory=true`
  - `includeUpcoming=true`
  - `includeBenchmark=true`
  - `benchmark=IPCA`
  - `firstPurchaseAt`
- Gráficos canônicos mantidos:
  - `assetChartsCanonical`
  - `assetChartsCoverage`
  - `financial.balanceSheet`
  - `financial.equityEvolution`
  - `financial.payoutHistory`
  - `financial.profitVsQuote`

## Versão final

- `versionName = 2.0.23`
- `versionCode = 33`

## Observação de build

A compilação Gradle completa não foi executada neste ambiente porque depende de acesso externo ao Gradle/Android tooling. O pacote final foi limpo para abrir corretamente no Android Studio/Gemini.
