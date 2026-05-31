# VALORAE Investidor / Portfolio — Continuação 3 da Auditoria

## Base usada

Esta rodada foi feita em cima do pacote corrigido anterior:

`investidor-portfolio-continuacao-2-hardening-performance-corrigido.zip`

As correções anteriores foram preservadas. Esta etapa focou em problemas remanescentes de recebimento de informações, normalização de séries, classificação de ativos, dividendos e estabilidade de dados exibidos nas páginas do app.

## Status do Proxy

Proxy oficial obrigatório:

`https://servidor-valorae.vercel.app/api`

O app deve continuar usando somente esse Proxy para dados externos. O app Android não deve fazer scraping direto nem acessar Yahoo, Google News, StatusInvest ou Investidor10 diretamente.

## Problemas encontrados nesta rodada

### 1. Comparação de Índices podia misturar preço bruto com retorno percentual

Algumas respostas do Proxy ou de blocos vindos do Investidor10 podem entregar séries em escala bruta, por exemplo:

- PETR4: preço 40 → 44
- IBOV: índice 120000 → 126000

A UI de Comparação de Índices precisa comparar retorno percentual, não valores absolutos. Quando séries brutas eram usadas diretamente, o gráfico ficava distorcido, ilegível ou parecia incorreto.

### 2. `/api/assets` podia retornar objeto indexado por ticker

A busca em lote já tratava arrays, mas nem todos os formatos possíveis de resposta foram cobertos. Agora também são aceitos objetos indexados por ticker em caminhos como:

- `assets`
- `items`
- `results`
- `data.assets`
- `data.results`

Isso aumenta a chance de a carteira, Detalhes do Ativo, Insights e Análise aproveitarem dados mesmo quando a estrutura do JSON muda levemente.

### 3. Dividendos/proventos ainda dependiam demais do bundle principal

Quando o pacote avançado de gráficos não trazia dividendos, o app podia exibir áreas vazias mesmo com o Proxy expondo uma rota específica de dividendos.

Foi adicionado fallback dedicado para:

`/api/asset/dividends?ticker=PETR4`

O parser aceita variações como:

- `events`
- `items`
- `dividends`
- `dividendos`
- `historicoDividendos`
- `proventos`
- `income`
- `data.events`
- `data.items`
- `data.dividends`
- `data.dividendos`
- `data.proventos`

### 4. Evolução de proventos precisava de séries derivadas

Quando o Proxy entrega eventos de dividendos, mas não entrega séries prontas para gráfico, o app agora deriva:

- evolução anual de dividendos;
- histórico anual de Dividend Yield;
- evolução mensal de dividendos.

Isso melhora Detalhes do Ativo, Insights e gráficos de proventos sem depender de um único formato fixo do JSON.

### 5. Classificação de FIIs precisava considerar dados do Proxy e ticker

A carteira podia carregar um ativo como `ACAO` por dado local antigo mesmo quando o ticker era FII, por exemplo `MXRF11`. Agora a classificação usa:

1. `liveInfo.isFii` vindo do Proxy;
2. inferência pelo ticker;
3. tipo declarado localmente;
4. fallback para ação.

Isso melhora:

- Equilíbrio de Carteira;
- segmentação Ações/FIIs;
- Detalhes do Ativo;
- Insights;
- filtros e cards específicos de FIIs.

### 6. Histórico de carteira usava limite pouco adaptável

A chamada de histórico de carteira passou a usar limite por range, evitando truncar períodos maiores como `5A`, `10A` e `MAX`.

### 7. Headers e identificação do app

O `User-Agent` e `X-Valorae-Client-Version` passaram a usar `BuildConfig.VERSION_NAME`, evitando versão fixa/hardcoded em produção.

## Arquivos alterados

### `app/src/main/java/com/example/network/B3NetworkService.kt`

Principais mudanças:

- adicionada normalização percentual de séries da Comparação de Índices;
- melhorado merge de séries comparativas;
- ampliado parser de `/api/assets` para objetos por ticker;
- adicionado fallback de dividendos por `/api/asset/dividends`;
- adicionada geração derivada de dividendos anuais, mensais e DY histórico;
- melhorado limite de histórico por range em `/api/portfolio/history`;
- atualizados headers para usar `BuildConfig.VERSION_NAME`.

### `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`

Principais mudanças:

- classificação de FIIs usando dados ao vivo do Proxy;
- inferência de tipo pelo ticker;
- remoção de redundância local em `assetSummaries`.

### `app/src/test/java/com/example/B3NetworkServiceParserTest.kt`

Principais mudanças:

- adicionado teste para garantir que Comparação de Índices normalize séries brutas para retorno percentual.

### `README.md`

Principais mudanças:

- documentada a Continuação 3;
- reforçada a regra de Proxy único;
- documentado o bloqueio do Gradle Wrapper corrompido.

## Validação estática executada

Resultado dos checks locais:

```text
host_antigo_bloqueado_runtime: OK
fallback_direto_false: OK
sem_chamadas_diretas_marketdata: OK
comparacao_normaliza_retorno: OK
dividendos_endpoint_fallback: OK
assets_objeto_por_ticker: OK
historico_portfolio_limit_range: OK
classificacao_fii_por_proxy: OK
teste_comparacao_raw: OK
```

## Bloqueio de build/APK

Ainda não foi possível gerar APK neste ambiente porque o arquivo abaixo está corrompido:

```text
gradle/wrapper/gradle-wrapper.jar
```

A tentativa de validação indica erro no JAR do Wrapper. Portanto, o Studio/Gemini deve substituir ou regenerar o Gradle Wrapper antes de executar:

```bash
./gradlew clean assembleDebug
```

## Critérios de aceite para o Studio/Gemini

A entrega só deve ser considerada concluída quando:

1. O Gradle Wrapper for substituído/regenerado.
2. O app compilar sem erros.
3. Um APK debug novo for gerado.
4. PETR4 carregar em Análise e Detalhes do Ativo.
5. MXRF11 carregar em Análise e Detalhes do Ativo.
6. Comparação de Índices mostrar retornos percentuais coerentes.
7. Dividendos/proventos aparecerem quando disponíveis pelo Proxy.
8. Evolução de proventos funcionar com dados diretos ou derivados.
9. Rentabilidade vs IPCA+ continuar estável.
10. Equilíbrio de Carteira classificar FIIs corretamente.
11. Agenda de Dividendos continuar priorizando eventos futuros.
12. Nenhuma tela ficar branca com payload parcial, warnings ou campos ausentes.
13. Nenhuma chamada direta a fontes externas permanecer no app Android.
14. O host antigo não ser usado.

## Observação final

Não foi entregue APK novo porque entregar um APK antigo seria incorreto. O ZIP gerado contém o código corrigido; a compilação deve ser feita no Android Studio/Gemini após corrigir o Gradle Wrapper.
