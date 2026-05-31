# RELATÓRIO — Verificação dos Rankings do VALORAE APK

Data: 2026-05-30
Projeto auditado: `valorae-apk-proxy-capabilities-rankings-corrigido.zip`

## Objetivo

Verificar se a implementação de rankings no APK está conectada corretamente ao VALORAE Proxy e se a tela Insights > Rankings renderiza os dados sem quebrar o restante das lógicas da carteira.

## Resultado

A implementação estava parcialmente correta, mas foram encontrados 2 pontos que poderiam fazer o usuário enxergar a seção de rankings como vazia em casos legítimos.

Status após correção: **OK em auditoria estática**.

## Problemas encontrados e corrigidos

### 1. Ranking da carteira não funcionava com apenas 1 ativo

Arquivo:

- `app/src/main/java/com/example/network/B3NetworkService.kt`

Antes, `fetchPortfolioRankings()` retornava `null` quando a carteira tinha menos de 2 tickers:

```kotlin
if (tickers.size < 2) return null
```

Isso impedia um usuário iniciante, com apenas um ativo na carteira, de ver o ativo ranqueado como `#1`.

Correção aplicada:

```kotlin
if (tickers.isEmpty()) return null
```

Agora o ranking da carteira funciona com 1 ou mais ativos.

---

### 2. Ranking de mercado podia cair em fallback sem preencher Maiores Altas/Baixas

Arquivos:

- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`

O endpoint `/api/v1/market/rankings` pode operar em dois modos principais:

1. ranking ao vivo do Investidor10, com `rankings.altas` e `rankings.baixas`;
2. fallback por comparação Valorae, com `rankings.score`, `rankings.dividendYield`, `rankings.pvp`, etc.

O app já parseava os dois formatos, mas a UI da subpágina Rankings priorizava apenas `highs/lows` para mercado. Se o Proxy caísse no fallback seguro, a seção “Mercado — Maiores Altas/Baixas” aparecia vazia mesmo havendo rankings por score ou DY.

Correção aplicada:

- Se vierem `highs/lows`, a UI mostra:
  - Mercado — Maiores Altas;
  - Mercado — Maiores Baixas.

- Se não vierem `highs/lows`, mas vierem rankings por comparação, a UI mostra:
  - Mercado — Score Valorae;
  - Mercado — Dividend Yield.

Também foi ajustado o preview da página para aceitar fallback de mercado por `score` e `dividendYield`.

## Fluxo validado

### Chamada de ranking da carteira

Método:

```kotlin
B3NetworkService.fetchPortfolioRankings(positions)
```

Endpoint usado:

```text
GET /api/v1/market/rankings?type=ACAO|FII&profile=portfolio&timeoutMs=1800&source=compare&tickers=...
```

Validação:

- aceita 1 ou mais ativos;
- limita tickers a 15;
- escolhe `FII` quando a maioria da carteira é FII;
- usa `source=compare`, evitando depender de ranking ao vivo para carteira;
- usa cache local curto.

### Chamada de ranking de mercado

Método:

```kotlin
B3NetworkService.fetchLiveStockRankings()
```

Endpoint usado:

```text
GET /api/v1/market/rankings?type=ACAO&profile=portfolio&timeoutMs=12000&source=auto
```

Validação:

- se o Investidor10 entregar altas/baixas, a UI mostra altas/baixas;
- se a fonte externa bloquear/falhar, o app mostra ranking de mercado por score/DY do fallback do Proxy;
- o app não faz scraping direto.

## Contrato parseado pelo APK

O parser aceita:

- `rankings.score`;
- `rankings.dividendYield`;
- `rankings.pvp`;
- `rankings.pl`;
- `rankings.altas`;
- `rankings.baixas`;
- `profiles.conservador`;
- `profiles.crescimento`;
- `profiles.rendaFii`;
- `ranking` raiz como fallback.

Campos aceitos por item:

- `rank`;
- `ticker` / `symbol` / `ativo`;
- `name` / `nome` / `company`;
- `value`;
- `score`;
- `profileScore`;
- `dividendYield`;
- `variacao`;
- `preco`;
- `grade` / `rating`;
- `direction`;
- `source`;
- `explanation` / `reason` / `message`.

## Garantia sobre existência da carteira

Os rankings são dados atuais/fundamentalistas e **não entram nos cálculos históricos**.

Foi preservado:

- proventos respeitando data real da carteira;
- IPCA limitado à existência da carteira;
- agenda usando elegibilidade por posição;
- histórico local com custo médio móvel;
- ranking sem contaminar dividendos passados ou futuros.

## Validações executadas

Comandos executados no sandbox:

```bash
python3 scripts/verify_valorae_proxy_capabilities.py
python3 scripts/verify_valorae_proxy_integration.py
python3 scripts/verify_valorae_insights_logic.py
```

Resultado:

```text
Valorae Proxy capabilities audit OK
Valorae Proxy integration audit OK
Valorae Insights logic audit OK
```

Novas garantias adicionadas ao script de auditoria:

- ranking da carteira funciona com 1 ativo;
- UI mostra fallback de mercado quando altas/baixas não vêm.

## Gradle

O Gradle não foi executado neste sandbox porque o wrapper voltou a falhar no download da distribuição por DNS:

```text
java.net.UnknownHostException: services.gradle.org
```

Validação recomendada no Android Studio:

```bash
./gradlew clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
./gradlew :app:lintDebug :app:check --stacktrace --info
```

## Conclusão

Os rankings agora estão tecnicamente integrados e protegidos contra os dois cenários que poderiam deixá-los aparentemente quebrados:

1. carteira com apenas 1 ativo;
2. ranking ao vivo indisponível com fallback por comparação retornado pelo Proxy.

Status final: **Rankings aprovados em auditoria estática e prontos para validação no Android Studio/dispositivo.**
