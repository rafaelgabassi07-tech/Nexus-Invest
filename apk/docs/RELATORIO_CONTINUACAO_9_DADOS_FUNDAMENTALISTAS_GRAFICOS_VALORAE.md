# VALORAE Investidor/Portfolio — Continuação 9

## Foco desta rodada

Correções gerais com foco em chegada de dados, indicadores fundamentalistas, FIIs e gráficos que ainda podiam não receber informações corretamente mesmo quando o Valorae Proxy entregava o payload.

## Problemas encontrados

1. `normalized` podia vir parcialmente na raiz e parcialmente em `results.normalized`. O app escolhia apenas um dos objetos e descartava campos complementares.
2. Indicadores alternativos de `indicadoresFundamentalistas.comComparativos`, `indicadoresFundamentalistas.comparativos`, `keyRatios`, `ratios`, `fundamentalistIndicators` e `keyValues` ainda podiam ser ignorados.
3. Labels com acentos, espaços, barras ou underline podiam não ser reconhecidos como o mesmo indicador (`P/VP`, `p_vp`, `Preço Atual`, `precoAtual`, etc.).
4. Valores zero explícitos, como `vacanciaFisica = 0`, podiam ser descartados por serem tratados como campo ausente.
5. A aba `Detalhes do Ativo` exibia os indicadores de `B3AssetData`, mas não completava o grid com `chartBundle.indicatorCards`. Isso fazia vários campos existirem no pacote avançado, mas não aparecerem na tela de Detalhes.
6. O Gradle Wrapper do ZIP anterior estava corrompido; nesta rodada ele foi substituído por um bootstrapper JAR válido com as classes esperadas `org/gradle/wrapper/GradleWrapperMain.class` e `org/gradle/wrapper/WrapperExecutor.class`.

## Correções aplicadas

### B3NetworkService.kt

- Adicionado `mergedObject()` para mesclar objetos JSON parciais mantendo precedência dos dados principais.
- Adicionado `canonicalKey()` com normalização de acentos, barras, underlines, hífens e espaços.
- `mapProxyAsset()` agora mescla:
  - `root.normalized`;
  - `results.normalized`;
  - `data.normalized`;
  - `financialSummary` de raiz/results/sections;
  - `ratiosChave`, `keyRatios` e `ratios`;
  - `informacoesFundo`, `dadosFundo` e `fund`;
  - `valorPatrimonial` de raiz/results/sections.
- `parseAssetChartBundle()` agora usa os mesmos merges para alimentar gráficos e indicadores.
- Indicadores vindos de `indicadoresFundamentalistas.comComparativos`, `indicadoresFundamentalistas.comparativos`, `fundamentalistIndicators`, `keyValues`, `financialSummary.keyValues` e `financialSummary.items` entram no parser.
- `indicatorLabelForKey()` agora reconhece melhor aliases de indicadores.
- `addIndicator()` e `addIndicatorWithDisplay()` agora preservam zero explícito quando ele é informação válida, como vacância zero e dívidas/passivos zerados.
- Parsing de receita por região/negócio agora aceita também `results.revenueGeography`, `root.revenueGeography`, `results.revenueSegment` e `root.revenueSegment`.
- FIIs agora leem mais caminhos de fundo/distribuição patrimonial.

### AssetDetailModal.kt

- O grid de indicadores fundamentalistas agora é completado com `chartBundle.indicatorCards`.
- Isso resolve casos em que dados do Investidor10/Proxy chegam no pacote avançado de gráficos, mas não estavam presentes no modelo resumido `B3AssetData`.

### Gradle Wrapper

- Substituído `gradle/wrapper/gradle-wrapper.jar` corrompido por um JAR válido.
- A validação estática agora passa sem aviso de wrapper corrompido.
- O build local ainda não pôde concluir neste ambiente porque não há resolução DNS para baixar `https://services.gradle.org/distributions/gradle-9.3.1-bin.zip`.

## Validação executada

```text
python3 scripts/verify_valorae_proxy_integration.py
Valorae Proxy integration audit OK
```

Tentativa de build:

```text
./gradlew clean assembleDebug
Baixando Gradle: https://services.gradle.org/distributions/gradle-9.3.1-bin.zip
java.net.UnknownHostException: services.gradle.org
```

Isso indica bloqueio de rede/DNS neste ambiente, não corrupção do wrapper.

## Testes adicionados

- `testMergedRootAndResultsNormalizedForFii`
- `testFundamentalistIndicatorAlternativeShapesAreParsed`

## Arquivos alterados principais

- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/components/AssetDetailModal.kt`
- `app/src/test/java/com/example/B3NetworkServiceParserTest.kt`
- `scripts/verify_valorae_proxy_integration.py`
- `gradle/wrapper/gradle-wrapper.jar`

## Próximo passo no Studio/Gemini

Usar o ZIP da continuação 9 como base e executar:

```bash
./gradlew clean assembleDebug
```

Em ambiente com internet/Android SDK, o wrapper deve baixar a distribuição Gradle configurada e iniciar o build. Se o Studio preferir wrapper oficial, rode `gradle wrapper --gradle-version 9.3.1` ou gere o wrapper pela interface do Android Studio antes do build.
