# Relatório — APK VALORAE v2.0.29 — Agenda e Evolução de Proventos End-to-End Fix

## Diagnóstico real

A tela podia ficar vazia ou inconsistente por combinação de problemas no conjunto Proxy + APK. No APK, o principal ajuste necessário era separar definitivamente projeções futuras de histórico recebido.

## Arquivos alterados

- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/build.gradle.kts`
- `metadata.json`
- `docs/RELATORIO_AGENDA_PROVENTOS_END_TO_END_FIX_v2.0.29.md`

## Agenda de Dividendos

A agenda continua podendo usar quantidade atual da carteira como estimativa para eventos futuros/provisionados. Isso permite mostrar próximos pagamentos para ativos atualmente na carteira.

## Evolução de Proventos

A evolução histórica agora evita o erro de usar quantidade atual para eventos passados. Para eventos recebidos:

1. Usa posição na data-com quando disponível.
2. Se não houver data-com, usa posição na data de pagamento.
3. Se não houver posição histórica elegível, não conta como recebido.
4. Eventos futuros não entram mais como projeção dentro da evolução histórica.

## Ajustes aplicados

- `sanitizeDividendEventsForPortfolio()` separa evento futuro/provisionado de evento histórico.
- `eligibleDividendAmount()` recebeu parâmetro `allowProjectionFallback`.
- `buildDividendEvolutionData()` passou a considerar somente eventos pagos/recebidos.
- Ranking de proventos por período passou a usar cálculo histórico estrito.

## Versionamento

- `versionCode = 39`
- `versionName = "2.0.29"`

## Validação

Foi tentado executar:

```bash
./gradlew --no-daemon assembleDebug
```

O ambiente isolado não conseguiu baixar o Gradle:

```text
UnknownHostException: services.gradle.org
```

A falha é de rede do wrapper Gradle, não uma evidência de erro Kotlin.
