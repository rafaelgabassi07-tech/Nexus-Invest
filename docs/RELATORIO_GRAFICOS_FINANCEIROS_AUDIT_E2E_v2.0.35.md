# APK VALORAE v2.0.35 — Auditoria E2E dos Gráficos Financeiros

## Problema auditado
Os gráficos das abas **Desempenho & Índices**, **Finanças & Balanço** e **Detalhes do Ativo** continuavam vazios/parciais.

## Causas encontradas
1. O APK chamava somente o contrato rápido `chartfast`, com timeout curto, e aceitava que esse retorno parcial fosse o estado final.
2. O parser do APK escolhia a primeira fonte financeira não vazia, mesmo quando era incompleta.
3. O gráfico **Lucro x Cotação** aceitava pontos com apenas lucro ou apenas cotação, causando linhas grudadas no teto/chão.
4. Campos de Ativo/PL/Passivo vinham em aliases diferentes ou em fontes separadas.

## Correções aplicadas
- `B3NetworkService.fetchAssetChartBundle()` ganhou parâmetro `deepFinancial`.
- Primeiro carregamento continua rápido com `chartfast`.
- Se o bundle vier incompleto, o `PortfolioViewModel` dispara uma segunda carga silenciosa `chartdeep`, sem prender a UI.
- A segunda carga mescla campos mais ricos no bundle já renderizado.
- Parser financeiro do APK passou a mesclar fontes e preservar séries complementares.
- **Lucro x Cotação** agora exige pontos com cotação e lucro no mesmo período para desenhar a comparação.
- O cache separa bundles `fast` e `deep` para evitar que uma resposta incompleta substitua dados completos.

## Validação
Tentativa executada:

```bash
./gradlew --no-daemon assembleDebug
```

Resultado no ambiente isolado:

```text
UnknownHostException: services.gradle.org
```

A falha ocorreu antes da compilação por bloqueio de rede do Gradle Wrapper.

## Versão
- `versionName`: `2.0.35`
- `versionCode`: `45`
