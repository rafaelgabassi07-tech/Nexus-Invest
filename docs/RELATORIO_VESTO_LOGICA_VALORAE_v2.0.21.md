# Relatório — Lógica inspirada no Vesto aplicada ao APK VALORAE v2.0.21

## Base usada

Projeto base: `APK_VALORAE_GRAFICOS_REVISAO_FINAL_v2.0.20.zip`.

Referência analisada: APK Vesto enviado pelo usuário. O Vesto é um APK baseado em assets web/Capacitor; a lógica útil foi extraída dos assets públicos, principalmente do fluxo de proventos, IPCA, patrimônio, alocação e agenda.

## Objetivo

Aprimorar as páginas do VALORAE sem quebrar o app e mantendo compatibilidade com o VALORAE Proxy.

Páginas focadas:

- Evolução de proventos;
- Rentabilidade vs IPCA+;
- Equilíbrio de Carteira;
- Agenda de dividendos.

## Arquivos alterados

- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/build.gradle.kts`
- `update.json`

## Melhorias na página Evolução de proventos

Foi adicionada uma camada de lógica inspirada no Vesto:

- separação entre valores recebidos e valores a receber;
- cálculo de total recebido no período;
- cálculo de total projetado;
- identificação do melhor mês;
- cálculo de YoC de 12 meses;
- leitura por mês com fonte dos dados;
- uso dos eventos do Proxy quando disponíveis;
- fallback local transparente somente quando o Proxy não entregar eventos.

O app não altera a compatibilidade existente: ele continua usando os gráficos atuais, mas agora adiciona leitura operacional mais rica e defensiva.

## Melhorias na página Rentabilidade vs IPCA+

Foi adicionada lógica de alinhamento entre a curva da carteira e o IPCA:

- prioriza histórico de carteira e IPCA vindos do Proxy;
- alinha por `dateLabel` quando possível;
- quando as séries têm tamanhos diferentes, usa reamostragem controlada para comparação visual;
- calcula ganho real por período;
- mostra melhor e pior janela;
- informa se os dados vieram do Proxy ou de fallback local.

## Melhorias na página Equilíbrio de Carteira

Foi adicionada leitura de equilíbrio baseada em:

- peso por classe;
- peso por ativo;
- concentração do maior ativo;
- concentração top 5;
- desvio em relação a alvo de referência;
- uso de `rebalanceActions` do VALORAE Proxy quando disponível;
- diagnóstico local quando o Proxy não entregar plano explícito.

A lógica evita forçar rebalanceamento artificial. Quando o Proxy não entregar ações de rebalanceamento, o app apenas informa diagnóstico de concentração.

## Melhorias na Agenda de dividendos

A agenda passou a ter leitura operacional mais clara:

- eventos ordenados por data relevante;
- separação de evento pago/recebido e previsto;
- exibição de data-com e data de pagamento;
- valor estimado total por ativo;
- fonte do dado;
- fallback local baseado na carteira somente quando não houver agenda remota.

## Compatibilidade com o VALORAE Proxy

Foram mantidos os endpoints atuais, mas o app agora solicita dados mais completos quando o Proxy suportar:

- `mode=complete`;
- `complete=true`;
- `includeHistory=true`;
- `includeUpcoming=true`;
- `includeBenchmark=true`;
- `benchmark=IPCA`.

Também foi corrigido o envio de `firstPurchaseAt` por ticker no payload de posições, permitindo que o Proxy limite análises ao período real da carteira do usuário.

## O que foi aproveitado do Vesto

- Separação visual entre proventos recebidos e a receber;
- análise de YoC;
- melhor mês de proventos;
- agenda ordenada e operacional;
- lógica de quantidade elegível por data;
- visão de concentração da carteira;
- leitura de rentabilidade real contra IPCA.

## Cuidados para não quebrar o VALORAE

- Não removi telas existentes;
- não troquei os gráficos principais por componentes incompatíveis;
- não alterei o contrato principal com o Proxy;
- os novos parâmetros enviados ao Proxy são adicionais e opcionais;
- os fallbacks locais continuam transparentes;
- nenhum dado financeiro é inventado como se fosse oficial.

## Versionamento

- `versionName = 2.0.21`
- `versionCode = 31`

## Validação

Validação estática:

```text
STATIC_VESTO_LOGICA_V221_OK
```

A tentativa de compilação Gradle não finalizou porque o ambiente não conseguiu baixar a distribuição do Gradle em `services.gradle.org`. O log está salvo em:

```text
docs/APK_BUILD_ATTEMPT_VESTO_LOGICA_v2.0.21.log
```
