# Auditoria VALORAE Investidor/Portfolio x Valorae Proxy 21.5.13

Data: 2026-05-27
Proxy oficial validado: `https://servidor-valorae.vercel.app`
Base API: `https://servidor-valorae.vercel.app/api`

## Diagnóstico principal

O app estava com o caminho principal do Proxy divergente em arquivos de build/documentação. O `gradle.properties` apontava para `https://valorae-proxy.vercel.app`, enquanto o deploy real do usuário é `https://servidor-valorae.vercel.app`. Como o `app/build.gradle.kts` injeta `VALORAE_PROXY_BASE_URL` no `BuildConfig`, o APK poderia compilar chamando o host antigo, deixando páginas, cards, indicadores e gráficos sem dados reais.

## Correções aplicadas

1. **URL pública oficial e Client ID padronizados**
   - Configuração de `VALORAE_PROXY_BASE_URL` para `https://servidor-valorae.vercel.app`.
   - Configuração do Client ID estável `valorae-investidor-android` para observabilidade correta.
   - Atualizados em: `gradle.properties`, `.env.example`, `README.md`, `docs/VALORAE_PROXY_INTEGRATION.md`, `docs/AUDITORIA_INVESTIDOR_PORTFOLIO_V1.1.4.md`, `app/src/test/java/com/example/ExampleUnitTest.kt` e `scripts/verify_valorae_proxy_integration.py`.

2. **BuildConfig mais robusto**
   - `app/build.gradle.kts` agora lê configuração na ordem:
     1. `gradle.properties`
     2. variáveis de ambiente/Studio
     3. `.env`
     4. `.env.example`
     5. fallback fixo para `https://servidor-valorae.vercel.app`
   - Isso evita depender de um único local de configuração.

3. **Remoção de dependência frágil de `BuildConfig.VERCEL_BACKEND_URL` em runtime**
   - `B3NetworkService.kt` agora usa `BuildConfig.VALORAE_PROXY_BASE_URL` como fonte canônica.
   - A compatibilidade com `VERCEL_BACKEND_URL` fica no build script, antes de gerar o `BuildConfig`.

4. **Ativo individual com perfil mais completo**
   - `GET /api/asset` para busca/detalhe agora usa `profile=deep`, `view=full`, `mode=super`.
   - Motivo: perfil `portfolio` é otimizado para carteira/lote e pode evitar parsing pesado, o que prejudica detalhes fundamentalistas.

5. **Lote de ativos com fallback de contrato**
   - `POST /api/assets` continua sendo o caminho principal.
   - Se o POST falhar, o app tenta `GET /api/assets?tickers=...`, conforme endpoint público listado pelo Proxy.

6. **Parser de gráficos históricos reforçado**
   - `fetchHistoricalChart` agora aceita arrays em:
     - `points`
     - `series`
     - `history`
     - `prices`
     - `items`
     - `data.points`
     - `data.series`
     - `history.points`
     - `chart.series`
   - Também aceita campos de preço `close`, `adjClose`, `value`, `price`, `regularMarketPrice`.
   - Se vier sem data parseável, o gráfico ainda renderiza usando índice sequencial, evitando tela vazia.

7. **Parser de campos fundamentalistas reforçado**
   - `mapProxyAsset` agora considera `advancedMetrics` além de `indicadoresAvancados`.
   - Dividendos aceitam `historicoDividendos`, `dividends`, `dividendos.historico`, `dividendos.items` e `dividendos.events`.
   - Último provento aceita `valor`, `value`, `valorPorCota`, `valuePerShare`, `ultimoRendimento` e `lastDividend`.

8. **Histórico de carteira e IPCA mais tolerantes**
   - `fetchPortfolioHistory` e `fetchIpcaSeries` agora aceitam arrays nested em `data.points` e `data.series`.

9. **Script de auditoria atualizado**
   - `scripts/verify_valorae_proxy_integration.py` agora valida a URL pública atual `https://servidor-valorae.vercel.app`.

## Resultado da auditoria local

Comando executado:

```bash
python3 scripts/verify_valorae_proxy_integration.py
```

Resultado:

```text
Valorae Proxy integration audit OK
```

## Arquivos alterados

- `app/build.gradle.kts`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `gradle.properties`
- `.env.example`
- `README.md`
- `docs/VALORAE_PROXY_INTEGRATION.md`
- `docs/AUDITORIA_INVESTIDOR_PORTFOLIO_V1.1.4.md`
- `app/src/test/java/com/example/ExampleUnitTest.kt`
- `scripts/verify_valorae_proxy_integration.py`

## Observação importante

O APK que vinha dentro de `.build-outputs/app-debug.apk` pertence ao build anterior e não foi recompilado neste ambiente, porque o pacote não inclui `gradlew` e o ambiente atual não tem Gradle/Android SDK completos para gerar um APK confiável. Compile o ZIP corrigido pelo Android Studio para gerar o APK novo.
