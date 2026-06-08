# APK VALORAE v2.0.30 — Agenda Futura e Evolução Retroativa de Proventos

## Objetivo

Corrigir a lógica das telas:

- Agenda de Dividendos;
- Evolução de Proventos.

A correção acompanha o Proxy v21.12.67, que passa a buscar meses futuros e meses passados no Investidor10.

## Causa raiz

O app consumia a agenda como se o Proxy sempre trouxesse todos os períodos. Na prática, o Proxy antigo consultava somente o mês corrente do Investidor10. Além disso, o APK fazia chamadas redundantes para endpoints diferentes, o que ficaria pesado com varredura mensal.

## Arquivos alterados

- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/build.gradle.kts`
- `metadata.json`

## Correções implementadas

1. `fetchNextDividends()` agora envia ao Proxy:
   - `futureMonths = 24`;
   - `monthsForward = 24`;
   - `historyMonths` calculado desde a primeira compra da carteira, com limite de 72 meses;
   - `monthsBack` equivalente;
   - `startDate` com a data da primeira compra;
   - `includeHistory = true`;
   - `includeUpcoming = true`.

2. O APK passa a usar chamada principal consolidada:
   - primeiro `/api/v1/portfolio/dividends` via POST;
   - fallback para `/api/v1/portfolio/next-dividends` somente se a resposta principal vier vazia.

3. Isso evita até quatro varreduras mensais completas no Proxy.

4. A Agenda de Dividendos mostra:
   - pagamentos futuros;
   - Data Com;
   - data de pagamento;
   - JCP;
   - Dividendos;
   - amortização/redução de capital quando vierem no contrato;
   - eventos anunciados/provisionados.

5. A Evolução de Proventos não usa mais quantidade atual para inventar histórico passado.

6. Para evento histórico, o APK só conta se houver posição elegível na data-com ou data de pagamento.

7. Eventos passados sem posição histórica são descartados para evitar valor falso.

8. Eventos futuros continuam podendo usar quantidade atual como projeção.

## Status na UI

A tela agora diferencia melhor:

- `Confirmado`: evento com data de pagamento explícita;
- `Anunciado`: evento com Data Com/valor, mas sem pagamento confirmado;
- `JCP`: quando o tipo vem como JSCP/JCP;
- `Inelegível`: quando o usuário não tinha posição elegível.

## Versionamento

```text
versionName = 2.0.30
versionCode = 40
```

## Validação

Tentativa de build:

```bash
./gradlew --no-daemon assembleDebug
```

Resultado no ambiente isolado:

```text
UnknownHostException: services.gradle.org
```

A falha foi de rede ao tentar baixar a distribuição do Gradle, não uma falha Kotlin apontada pelo compilador.

## Observações de uso

Para funcionar corretamente:

1. Publique primeiro o Proxy v21.12.67 na Vercel.
2. Depois gere o APK v2.0.30.
3. Limpe cache/dados do app se ele continuar exibindo dados antigos.
4. Teste com uma carteira contendo ações e FIIs que tenham eventos em meses diferentes.

## Versão esperada do Proxy

```text
VALORAE Proxy v21.12.67
```
