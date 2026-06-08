# Relatório — nova revisão dos rankings do APK VALORAE v2.0.14

## Objetivo
Revisar novamente o funcionamento dos rankings do APK VALORAE, mantendo removido o botão **Ver Ranking Completo** e reduzindo riscos de falha na Home, no parser e na integração com o VALORAE Proxy.

## Resultado
Foram aplicadas correções defensivas adicionais no suporte de rankings, principalmente para casos em que o Proxy retorne formatos alternativos ou listas genéricas de ativos em movimento.

## Arquivos principais alterados

- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- `app/src/test/java/com/example/B3NetworkServiceParserTest.kt`
- `app/build.gradle.kts`
- `scripts/verify_valorae_rankings_review_v214.py`

## Correções aplicadas

### 1. Botão de ranking completo continua removido
O código principal foi verificado para garantir que não existem mais:

- `Ver Ranking Completo`
- `onOpenRankings`

A Home mantém apenas o card compacto de rankings com alternância interna entre:

- `ALTAS`
- `BAIXAS`

### 2. Parser reforçado para listas genéricas
Antes, o APK dependia principalmente de arrays explícitos como `altas`, `baixas`, `topGainers` e `topLosers`.

Agora também aceita listas genéricas como:

- `items`
- `ranking`
- `rows`
- `result`
- `list`
- `ativos`
- `marketMovers`
- `market_movers`
- `movements`
- `movimentacoes`
- `dailyMovers`
- `daily_movers`

Quando esses arrays vierem misturados, o APK separa automaticamente altas e baixas usando:

- `direction`
- `tipo`
- `type`
- `category`
- `categoria`
- `movement`
- `movimento`
- `side`
- sinal de `changePercent`
- sinal textual em `changeDisplay` / `displayValue`

### 3. Percentual sem símbolo `%` normalizado
Se o Proxy retornar variação como `5,25` ou `-4,75` em campos de variação, o APK passa a normalizar para:

- `+5,25%`
- `-4,75%`

Isso evita exibição incompleta na Home.

### 4. Preço não é mais confundido com variação
Foi removido o uso de `preco` como fallback direto de `value` em item de ranking. Isso evita que o APK interprete preço, por exemplo `R$ 10,25`, como se fosse `+10,25%`.

### 5. Enriquecimento por cotação mais seguro
O enriquecimento com `assetData` agora continua preservando a variação real capturada do ranking. A cotação auxiliar só entra como fallback quando o ranking não trouxe percentual confiável.

### 6. Aliases ampliados
Além dos aliases anteriores, foram reforçados campos alternativos para:

- ticker/código;
- posição/rank;
- preço;
- variação percentual;
- volume;
- fonte;
- direção do movimento.

## Versionamento

- `versionName = "2.0.14"`
- `versionCode = 24`

## Validações executadas

```text
STATIC_RANKINGS_REVIEW_V214_OK
Valorae loading optimization audit OK
Valorae deep final audit OK
```

Também foi feita tentativa de `./gradlew test`, mas o ambiente não conseguiu baixar o Gradle por falta de acesso externo a `services.gradle.org`. O log foi salvo em:

```text
docs/APK_BUILD_ATTEMPT_RANKINGS_REVIEW_v2.0.14.log
```

## Como validar manualmente no app

1. Abrir o app VALORAE.
2. Confirmar que a tela inicial não possui botão **Ver Ranking Completo**.
3. Confirmar que o card de rankings aparece na Home.
4. Alternar entre `ALTAS` e `BAIXAS`.
5. Confirmar que os itens exibem:
   - ticker;
   - nome quando disponível;
   - preço quando disponível;
   - variação percentual com `%`;
   - seta correta `▲` para altas e `▼` para baixas.
6. Testar com internet lenta ou Proxy instável e confirmar que a Home não fica travada em skeleton infinito.

## Conclusão
O ranking da Home está mais tolerante a variações reais de contrato do Proxy e menos propenso a mau funcionamento por resposta parcial, lista genérica ou percentual sem formatação.
