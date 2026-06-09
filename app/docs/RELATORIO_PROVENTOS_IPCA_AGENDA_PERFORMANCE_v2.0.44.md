# VALORAE APK v2.0.44 — Proventos, IPCA+ e Agenda com carteira grande

## Escopo

Esta revisão corrige gargalos e estados vazios percebidos nas telas:

- Evolução de proventos;
- Rentabilidade vs IPCA+;
- Agenda de dividendos.

Também reduz o tempo de espera quando a carteira possui muitos ativos.

## Causa raiz encontrada

A rota de proventos do Proxy trabalha com limite operacional de lote. Quando a carteira tinha muitos ativos, o APK podia acionar fallback por ativo após falha/timeout da chamada consolidada. Esse fallback fazia múltiplas requisições sequenciais por ticker, especialmente em dividendos, histórico e próximos pagamentos. Com callTimeout alto de rede, isso podia se acumular e parecer travamento de vários minutos.

Além disso, a UI dependia demais do retorno remoto para proventos/IPCA/histórico, deixando cards com aparência vazia enquanto o Proxy montava os dados.

## Alterações aplicadas

### 1. Lote móvel balanceado para proventos

O APK agora monta um lote remoto de até 30 posições, balanceando ações e FIIs:

- até 15 ações;
- até 15 FIIs;
- preenchimento restante pelos ativos de maior peso na carteira.

Isso impede que uma carteira com muitos FIIs esconda ações na agenda, e vice-versa.

### 2. Fim do fallback sequencial por ativo em carteiras grandes

O APK não percorre mais dezenas de ativos chamando endpoints individuais de dividendos quando a carteira é grande. O fallback por ativo ficou limitado a carteiras pequenas e em modo rápido.

### 3. Timeouts reais por requisição crítica

Foram aplicados call timeouts explícitos nas rotas críticas:

- `/api/v1/portfolio/dividends`;
- `/api/v1/portfolio/next-dividends`;
- `/api/v1/portfolio/history`;
- `/api/v1/portfolio/analyze`;
- `/api/v1/market/ipca`;
- `/api/v1/asset/dividends` em fallback rápido.

Isso evita depender apenas do timeout global do OkHttp.

### 4. Analytics local antes do Proxy

As telas de insights passam a receber estado local imediato com:

- histórico local sintético da carteira;
- fallback transparente de IPCA;
- preview local de proventos;
- loading visual desativado para não travar a navegação.

### 5. Evolução de proventos sem gráfico vazio

Quando os eventos reais ainda não chegaram, a evolução de proventos usa a estimativa mensal local como série projetada recente, mantendo o gráfico vivo e deixando claro que não é recebido/confirmado.

### 6. Agenda inclui pagos recentes

A agenda agora aceita eventos pagos dos últimos 18 meses, além dos eventos futuros/previstos. Isso corrige o cenário em que proventos pagos por ações ficavam escondidos enquanto FIIs apareciam.

## Resultado esperado

- Abertura das páginas sem espera de vários minutos.
- Cards de IPCA+, Proventos e Agenda renderizando com fallback local.
- Proventos de ações e FIIs preservados no mesmo fluxo.
- Menos chamadas remotas e menos chance de timeout em carteiras grandes.

## Observação de build

A estrutura e os contratos do app foram preservados. A validação completa do Gradle pode depender de acesso ao wrapper remoto em `services.gradle.org` no ambiente de build.
