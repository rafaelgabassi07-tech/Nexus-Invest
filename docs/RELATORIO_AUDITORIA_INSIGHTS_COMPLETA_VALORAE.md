# RELATÓRIO — Auditoria completa da página Insights e integração VALORAE Proxy

Data: 2026-05-30  
Projeto: APK VALORAE Investidor  
Base: `valorae-apk-integracao-revisada-graficos-faturamento.zip`

## Escopo revisado

Foi auditada a página **Insights** (`ChartsScreen.kt`) e suas subpáginas internas:

- **Proventos**
- **IPCA+**
- **Diversificação**
- **Agenda**

Também foram revisadas as integrações de suporte no `PortfolioViewModel.kt` e no `B3NetworkService.kt`, com foco em dados vindos do VALORAE Proxy, fallback local e respeito ao tempo real de existência da carteira.

## Problemas encontrados

### 1. Proventos antes da existência da carteira

A lógica anterior já evitava parte do fallback antes do primeiro mês da carteira, mas eventos reais do Proxy podiam ser usados sem recalcular se o usuário tinha posição na **data-com** ou na data de pagamento. Isso poderia exibir como recebido/projetado um provento anterior à compra do ativo.

### 2. Agenda misturando eventos não elegíveis

A Agenda priorizava eventos do Proxy, mas não aplicava filtro definitivo por quantidade possuída na data relevante. Em carteiras recentes, eventos antigos do ativo poderiam aparecer como se fossem da carteira.

### 3. Ranking “Proventos por Ativos” com multiplicador fixo

O gráfico de distribuição por ativo usava multiplicadores fixos, incluindo `Neste ano = 5`, sem considerar o mês atual e sem limitar ao início real da carteira.

### 4. IPCA comparando período maior que a carteira

O IPCA remoto podia vir em janela de 12 meses, enquanto a carteira podia existir há menos tempo. Isso distorcia o ganho real por comparar rentabilidade da carteira com inflação acumulada anterior à carteira.

### 5. Histórico local de carteira após vendas

O fallback local do histórico reduzia o valor investido com base no preço da venda. Para um histórico de carteira, o correto é reduzir o custo pelo custo médio carregado antes da venda, preservando consistência com a lógica de preço médio móvel já usada nos resumos.

## Correções implementadas

### `ChartsScreen.kt`

Foram adicionados helpers específicos para a página Insights:

- `eligibleDividendAmount(...)`
- `sharesOwnedAtInsightDate(...)`
- `eventEligibilityMillis(...)`
- `monthlyDividendEstimateForMonth(...)`
- `buildTopDividendAssetsForPeriod(...)`
- `periodStartMillis(...)`
- `monthsInSelectedPeriod(...)`

Com isso:

- Proventos passados agora são contabilizados apenas se o usuário tinha posição na data-com/pagamento.
- Proventos futuros usam a posição elegível quando há data definida.
- O gráfico de evolução de proventos não cria valores antes do mês inicial da carteira.
- O fallback mensal calcula quantidade no fim de cada mês, não a posição atual aplicada retroativamente.
- O ranking de pagadores usa eventos reais dentro do período selecionado quando disponíveis.
- O período `Neste ano` agora respeita janeiro do ano atual e o início real da carteira.
- A Agenda mostra eventos elegíveis e informa quantidade elegível.
- KPIs da Agenda usam soma de eventos elegíveis.

### `PortfolioViewModel.kt`

Foram adicionadas normalizações pós-Proxy:

- `normalizePortfolioHistoryForAge(...)`
- `normalizeIpcaForPortfolioAge(...)`
- `sanitizeDividendEventsForPortfolio(...)`
- `firstPortfolioPurchaseMillis(...)`
- `portfolioAgeMonths(...)`

Com isso:

- Histórico remoto do Proxy é cortado para começar no início real da carteira.
- IPCA é rebaseado para o período da carteira.
- Fallback de IPCA usa a idade real da carteira, não sempre 12 meses.
- Eventos de dividendos remotos são saneados antes de entrarem no estado da UI.
- Histórico local usa custo médio móvel após vendas parciais.

### `B3NetworkService.kt`

O payload de carteira enviado ao Proxy agora inclui metadado opcional:

- `firstPurchaseAt`
- `firstPurchaseAtSeconds`

Esse campo não quebra compatibilidade. Se o Proxy ignorar, o APK continua fazendo o ajuste localmente.

## Subpáginas revisadas

### Proventos

Status: **corrigida**

Valida agora:

- início real da carteira;
- quantidade elegível na data do evento;
- filtros FIIs/Ações;
- períodos 6, 12 e 24 meses;
- ranking por ativo usando eventos reais quando disponíveis;
- fallback local transparente quando não há eventos do Proxy.

### IPCA+

Status: **corrigida**

Valida agora:

- histórico remoto limitado ao período da carteira;
- IPCA rebaseado ao mesmo período;
- fallback local com meses proporcionais à existência da carteira;
- comparação mais justa de ganho real.

### Diversificação

Status: **revisada sem falha bloqueante**

Valida:

- alocação por classe;
- alocação por setor;
- concentração Top 5;
- detalhamento por ativo;
- simulador de rebalanceamento.

Observação: a classificação setorial local ainda é heurística quando o Proxy não entrega `allocationBySector`, mas o fallback está seguro e não quebra UI.

### Agenda

Status: **corrigida**

Valida agora:

- eventos futuros priorizados;
- eventos passados só aparecem se forem elegíveis;
- eventos anteriores à compra são removidos;
- valor mostrado é recalculado pela quantidade possuída na data-com/pagamento;
- fallback local só entra quando não há eventos do Proxy.

## Auditorias executadas

```bash
python3 scripts/verify_valorae_proxy_integration.py
```

Resultado:

```text
Valorae Proxy integration audit OK
```

```bash
python3 scripts/verify_valorae_insights_logic.py
```

Resultado:

```text
Valorae Insights logic audit OK
OK - Insights calcula quantidade elegível em dividendos
OK - Evolução de proventos recebe transações
OK - Agenda de dividendos filtra por elegibilidade
OK - Top pagadores respeita período e existência da carteira
OK - KPIs da agenda usam eventos elegíveis
OK - Histórico remoto é ajustado ao início da carteira
OK - IPCA é rebaseado ao período da carteira
OK - Proventos remotos são saneados por posição na data
OK - Histórico local usa custo médio móvel após vendas
OK - Payload de carteira envia firstPurchaseAt opcional
```

## Validação Gradle

Foi tentado executar:

```bash
./gradlew :app:compileDebugKotlin --stacktrace --info
```

Resultado no sandbox:

```text
java.net.UnknownHostException: services.gradle.org
```

O ambiente atual não possui `gradle` nativo instalado e o wrapper não conseguiu baixar a distribuição por DNS externo. Portanto, a validação Gradle final deve ser executada no Android Studio, onde a bateria anterior já havia sido bem-sucedida.

Comandos recomendados:

```bash
./gradlew clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
./gradlew :app:lintDebug :app:check --stacktrace --info
```

## Arquivos alterados

- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `scripts/verify_valorae_insights_logic.py`
- `docs/RELATORIO_AUDITORIA_INSIGHTS_COMPLETA_VALORAE.md`

## Resultado final

A página Insights foi revisada de ponta a ponta, com correções reais nas lógicas de:

- dividendos passados;
- dividendos futuros;
- idade real da carteira;
- elegibilidade por data-com;
- IPCA no mesmo período da carteira;
- histórico local após vendas;
- Agenda de eventos;
- ranking de proventos por ativo.

A integração com o VALORAE Proxy permanece centralizada, usando `/api/v1/...`, cache local e fallback seguro.
