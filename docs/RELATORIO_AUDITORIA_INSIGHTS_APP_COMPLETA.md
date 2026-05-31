# Relatório — Nova auditoria minuciosa da página Insights e integração geral do APK VALORAE

Data: 2026-05-30  
Base analisada: `valorae-apk-nova-auditoria-minuciosa-corrigido.zip`  
Saída gerada: `valorae-apk-insights-app-auditoria-completa-corrigido.zip`

## 1. Escopo da auditoria

A auditoria focou novamente na página **Insights** e nas suas abas/subpáginas principais, verificando se gráficos, tabelas, KPIs e projeções respeitam a existência real da carteira, ou seja, a data da primeira compra, as quantidades efetivamente mantidas em cada período e a elegibilidade real para dividendos.

Também foram revisadas áreas complementares do aplicativo:

- integração com o VALORAE Proxy;
- rotas `/api/v1/...`;
- uso de cache e último snapshot bom;
- tratamento de dados parciais;
- validação de HTTPS;
- diagnóstico do Proxy;
- segurança contra scraping direto pelo APK;
- gráficos, barras e progressos com dados extremos ou parciais.

## 2. Arquivos alterados

- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `scripts/verify_valorae_insights_logic.py`
- `docs/RELATORIO_AUDITORIA_INSIGHTS_APP_COMPLETA.md`

## 3. Problemas encontrados e corrigidos

### 3.1 Ranking “Proventos por Ativos” superestimava fallback sem eventos reais

Quando o Proxy não entregava eventos de dividendos detalhados, o app ainda podia estimar proventos usando:

- quantidade atual do ativo;
- dividend yield atual;
- número total de meses do filtro.

Isso podia inflar o resultado de ativos comprados depois do início do período selecionado. Exemplo: um ativo comprado em novembro poderia aparecer como se tivesse gerado proventos desde janeiro.

Correção aplicada:

- criado cálculo mês a mês em `estimateDividendForAssetAcrossPeriod(...)`;
- para cada mês, o app calcula a quantidade realmente possuída no encerramento daquele mês usando `sharesOwnedAtInsightDate(...)`;
- o fallback agora respeita compras e vendas ao longo do tempo;
- o ranking de top pagadores não usa mais apenas a quantidade atual para todo o período.

### 3.2 Fallback visual de IPCA+ e carteira usava sempre 12 meses

Quando não havia histórico remoto de carteira ou IPCA, a UI criava uma série visual fixa de 12 meses. Isso ficava incorreto para uma carteira recém-criada, porque mostrava uma comparação anterior à existência da carteira.

Correção aplicada:

- criado `portfolioAgeMonthsForInsights(...)`;
- fallback de carteira e IPCA agora usa a idade real da carteira, limitada de 1 a 120 meses;
- gráficos não projetam histórico artificial antes da primeira transação.

### 3.3 Tabela de IPCA+ podia comparar séries desalinhadas

A tabela de IPCA+ podia usar tamanhos diferentes para série de carteira e série IPCA, calculando juros reais com pontos temporais desalinhados.

Correção aplicada:

- a série IPCA agora é reamostrada para o tamanho da série da carteira antes da tabela;
- o cálculo de juros reais usa `alignedIpcaTable`;
- a tabela agora prioriza o rótulo do histórico da carteira e só usa IPCA como fallback.

### 3.4 Parser de datas era estreito demais

Algumas datas vindas do Proxy ou de fontes financeiras podem chegar em ISO, BR curto, BR com hora, `yyyy/MM/dd`, timestamp em segundos ou milissegundos.

Correção aplicada em `ChartsScreen.kt` e `PortfolioViewModel.kt`:

- suporte a `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`;
- suporte a `yyyy-MM-dd'T'HH:mm:ss'Z'`;
- suporte a `yyyy-MM-dd'T'HH:mm:ssXXX`;
- suporte a `yyyy-MM-dd HH:mm:ss`;
- suporte a `dd/MM/yyyy HH:mm:ss`;
- suporte a `dd/MM/yyyy HH:mm`;
- suporte a `dd/MM/yyyy`;
- suporte a `dd/MM/yy`;
- suporte a `yyyy-MM-dd`;
- suporte a `yyyy/MM/dd`;
- suporte a `dd-MM-yyyy`;
- suporte a `MM/yyyy`;
- suporte a `yyyy-MM`;
- normalização de espaços e da expressão `às`.

### 3.5 Projeção de bola de neve podia usar preço médio inválido

Se algum ativo tivesse custo médio zero, inválido ou ausente, a simulação podia ficar distorcida.

Correção aplicada:

- preço médio agora usa `averageCost` válido;
- se não houver custo médio válido, usa `currentPrice` válido;
- se não houver nenhum preço válido, usa fallback seguro;
- progressos visuais foram limitados com `coerceIn(0f, 1f)`;
- dividend yield extremo foi limitado no cálculo de projeção.

## 4. Abas/subpáginas de Insights revisadas

### 4.1 Proventos

Status: corrigido e validado estaticamente.

A lógica agora respeita:

- data de início da carteira;
- quantidade elegível em cada evento;
- data-com e data de pagamento;
- eventos futuros somente se forem de hoje em diante;
- fallback mês a mês quando não há eventos detalhados;
- vendas que reduzem ou zeram a posição.

### 4.2 Agenda

Status: revisada e preservada.

Já havia sido corrigida para não reaproveitar eventos antigos como futuros. Nesta auditoria foi mantida a regra:

- evento antigo local não aparece na agenda futura;
- evento remoto é filtrado por elegibilidade;
- eventos sem posição na data correta não entram como recebíveis.

### 4.3 IPCA+

Status: corrigido e validado estaticamente.

A aba agora respeita:

- início real da carteira;
- IPCA rebaseado pelo ViewModel;
- fallback visual com idade real da carteira;
- tabela com séries alinhadas;
- cálculo de juros reais sem misturar períodos incompatíveis.

### 4.4 Diversificação

Status: revisada e preservada.

Foram mantidos os clamps de barras em `0..1`, evitando quebra visual com dados extremos, percentuais parciais ou inconsistência temporária de fonte.

### 4.5 Simulações e bola de neve

Status: corrigido.

Foram aplicadas proteções para:

- custo médio inválido;
- preço inválido;
- DY extremo;
- progresso visual fora de faixa;
- divisão por zero.

## 5. Revisão da integração geral com o Proxy

A auditoria confirmou que a integração principal continua usando os contratos novos:

- `/api/v1/asset`;
- `/api/v1/assets`;
- `/api/v1/asset/history`;
- `/api/v1/asset/dividends`;
- `/api/v1/news`;
- `/api/v1/market/indices`;
- `/api/v1/portfolio/analyze`;
- `/api/v1/portfolio/history`;
- `/api/v1/market/ipca`;
- `/api/v1/portfolio/next-dividends`;
- `/api/v1/ready`;
- `/api/v1/release/readiness`;
- `/api/v1/source/status`;
- `/api/v1/integration/manifest`;
- `/api/server/metrics`.

Também foi verificado:

- não há uso funcional das rotas legadas `/api/asset` ou `/api/assets` no app;
- a validação estática rejeita regressão para rotas antigas;
- a URL do Proxy exige HTTPS;
- scraping direto pelo APK permanece desabilitado;
- snapshot local persistente permanece ativo;
- `PARTIAL` continua sendo tratado sem falha fatal;
- diagnóstico do Proxy permanece disponível em Configurações.

## 6. Observação sobre sync externo opcional

O projeto ainda possui uma camada opcional de backup/sync externo. Ela não é dependência do VALORAE Proxy nem do funcionamento principal do APK, e só é habilitada quando configurada com URL HTTPS e credenciais válidas.

Como regra de segurança, ela já rejeita placeholder, HTTP, localhost e URLs inválidas. Se a exigência passar a ser “nenhum sync externo possível em hipótese alguma”, a próxima etapa deve remover ou ocultar totalmente essa funcionalidade opcional.

## 7. Validações executadas

### 7.1 Auditoria estática da integração Proxy

Comando:

```bash
python3 scripts/verify_valorae_proxy_integration.py
```

Resultado:

```text
Valorae Proxy integration audit OK
```

### 7.2 Auditoria estática da lógica de Insights

Comando:

```bash
python3 scripts/verify_valorae_insights_logic.py
```

Resultado:

```text
Valorae Insights logic audit OK
OK - Insights calcula quantidade elegível em dividendos
OK - Evolução de proventos recebe transações
OK - Agenda de dividendos filtra por elegibilidade
OK - Agenda não reaproveita eventos antigos quando não há futuro
OK - Agenda local remove datas-com antigas
OK - Barras de diversificação são clampadas em 0..1
OK - Top pagadores respeita período e existência da carteira
OK - Fallback de top pagadores soma mês a mês por quantidade histórica
OK - Parser de datas dos Insights aceita ISO e formato brasileiro curto
OK - KPIs da agenda usam eventos elegíveis
OK - Histórico remoto é ajustado ao início da carteira
OK - IPCA é rebaseado ao período da carteira
OK - Fallback visual de IPCA/Carteira respeita idade da carteira
OK - Tabela IPCA alinha séries antes de calcular juros reais
OK - Proventos remotos são saneados por posição na data
OK - Histórico local usa custo médio móvel após vendas
OK - Payload de carteira envia firstPurchaseAt opcional
```

### 7.3 Gradle no sandbox

Comando tentado:

```bash
./gradlew --version
```

Resultado:

```text
java.net.UnknownHostException: services.gradle.org
```

Este erro ocorreu antes da compilação do projeto e representa limitação de rede/DNS do sandbox ao tentar baixar a distribuição do Gradle. Não é evidência de erro no código. No Android Studio, onde o usuário já conseguiu usar Gradle nativo anteriormente, a validação final deve ser executada novamente.

## 8. Comandos recomendados no Android Studio

Execute na raiz do projeto:

```bash
./gradlew clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
./gradlew :app:lintDebug :app:check --stacktrace --info
```

Depois testar manualmente no app:

1. abrir o app com carteira vazia;
2. abrir o app com carteira recém-criada;
3. criar compra antiga e compra recente;
4. abrir Insights > Proventos;
5. abrir Insights > Agenda;
6. abrir Insights > IPCA+;
7. verificar se não aparecem dividendos anteriores à compra;
8. verificar se ativos comprados recentemente não são inflados no ranking;
9. testar resposta parcial do Proxy;
10. testar modo offline/cache.

## 9. Conclusão

A página Insights ficou mais consistente com a existência real da carteira. Os pontos mais sensíveis foram corrigidos: dividendos sem eventos reais, fallback visual de IPCA, tabela de juros reais, parser de datas e simulações com dados extremos.

A integração geral com o VALORAE Proxy permanece preservada, usando endpoints `/api/v1/...`, cache local, tratamento de `PARTIAL` e diagnóstico separado da interface principal.
