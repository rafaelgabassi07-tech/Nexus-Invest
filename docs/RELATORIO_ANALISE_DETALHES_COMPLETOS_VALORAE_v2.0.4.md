# VALORAE v2.0.4 — Correção profunda da página Análise e Detalhes do Ativo

Data: 2026-06-04  
Projeto base: `APK_VALORAE_v2.0.3_UI_INDICADORES_ANALISE_FIX.zip`  
Versão final: `versionName = 2.0.4`, `versionCode = 14`

## Objetivo

Corrigir a exibição incompleta e incorreta das páginas:

- **Análise**
- **Detalhes do Ativo**

A regra preservada foi: **todas as informações externas devem vir do VALORAE Proxy**. O app não deve inventar indicadores, não deve criar gráficos simulados e não deve usar preço médio como cotação.

## Problemas encontrados

1. A página **Análise** não renderizava o pacote completo de gráficos disponível no `AssetChartBundle`.
2. Muitos indicadores eram montados manualmente com lista fixa, o que deixava dados retornados pelo Proxy sem aparecer.
3. Havia campo hardcoded em FII: **“P/VP Máximo Alvo = 1.00”**. Isso foi removido porque não vinha do Proxy.
4. O modal **Detalhes do Ativo** ainda tinha lógica local de valuation/alerta, como Graham, Bazin e Conselho VALORAE. Isso foi removido da tela porque parecia dado oficial, mas era cálculo local.
5. O modal ainda podia usar valores locais como fallback de mercado.
6. O resumo da carteira ainda podia usar preço médio como cotação quando não havia preço real. Isso foi corrigido.
7. A aba **Perfil & Dados** estava incompleta e não centralizava todos os dados de perfil, dados cadastrais, patrimoniais e notícias.

## Arquivos alterados

### Criado

- `app/src/main/java/com/example/ui/components/AssetProxySections.kt`

Novo componente central para renderização correta e dinâmica de:

- Indicadores Gerais;
- Perfil & Dados;
- qualidade/completude dos dados recebidos;
- notícias do ativo;
- campos adaptativos para Ação e FII.

### Alterados

- `app/src/main/java/com/example/ui/screens/AnalysisScreen.kt`
- `app/src/main/java/com/example/ui/components/AssetDetailModal.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/build.gradle.kts`
- `update.json`
- `scripts/verify_valorae_ui_v204.py`

## Nova estrutura da página Análise

A página **Análise** mantém somente três abas principais:

1. **Resumo & Gráficos**
2. **Indicadores Gerais**
3. **Perfil & Dados**

### Resumo & Gráficos

Agora usa o pacote completo do Proxy por meio de:

```kotlin
AssetChartBundlePanel(...)
```

Isso permite exibir todos os gráficos existentes no bundle, sem escolher manualmente apenas alguns.

Para **ações**, o painel cobre, quando o Proxy retornar dados:

- gráfico histórico de preço;
- rentabilidade nominal vs real;
- comparação com índices;
- comparação com commodities, quando existir;
- proventos por ano;
- histórico de Dividend Yield;
- sazonalidade mensal de proventos;
- eventos de dividendos/proventos;
- DRE: receitas x lucros;
- evolução lucro x cotação;
- balanço patrimonial: ativos, PL e passivos;
- payout histórico;
- faturamento por negócio;
- faturamento por região.

Para **FIIs**, o painel cobre, quando o Proxy retornar dados:

- gráfico histórico de preço;
- rentabilidade do FII;
- distribuições de 12 meses;
- rendimentos pagos por ano;
- histórico de Dividend Yield;
- sazonalidade mensal;
- eventos de proventos;
- métricas patrimoniais;
- distribuição física dos ativos;
- comparação com IFIX/CDI/IPCA quando disponível;
- métricas comparativas com segmento.

Se o Proxy não retornar o bundle gráfico, a tela mostra indisponibilidade controlada. Nenhum gráfico é inventado.

### Indicadores Gerais

A lista agora é dinâmica e construída por:

```kotlin
buildAssetProxyIndicatorFields(assetData, bundle, isFii)
```

Ela combina:

- `B3AssetData` normalizado;
- `AssetChartBundle.indicatorCards`;
- campos específicos de ação;
- campos específicos de FII.

Foram removidos campos hardcoded que pareciam dados reais, principalmente:

- `P/VP Máximo Alvo = 1.00`;
- parâmetros locais de mercado não vindos do Proxy.

### Perfil & Dados

A aba agora usa:

```kotlin
AssetProxyProfileSection(...)
```

Ela exibe:

- status e completude dos dados recebidos;
- fonte do snapshot;
- ticker;
- nome;
- classe do ativo;
- CNPJ;
- setor/subsetor/segmento;
- ano de fundação;
- estreia na bolsa;
- funcionários;
- total de papéis/cotas;
- dados específicos de FII quando existirem;
- descrição/perfil operacional;
- últimas notícias do ativo.

As notícias continuam dentro de **Perfil & Dados**. Se não houver notícia no Proxy, o app mostra estado vazio.

## Nova estrutura do modal Detalhes do Ativo

O modal mantém:

1. **Resumo & Gráficos**
2. **Indicadores Gerais**
3. **Perfil & Dados**
4. **Minha Custódia**
5. **Transações**

### Resumo & Gráficos

O modal também usa o pacote completo:

```kotlin
AssetChartBundlePanel(...)
```

Assim, ele fica alinhado com a página Análise e não perde gráficos.

### Indicadores Gerais

O modal usa o mesmo componente dinâmico da página:

```kotlin
AssetProxyIndicatorSection(...)
```

Também mantém:

- histórico de indicadores gerais;
- histórico de Dividend Yield;
- comparativo de segmento para FII, quando disponível.

### Perfil & Dados

O modal usa:

```kotlin
AssetProxyProfileSection(...)
```

Assim, os dados cadastrais, perfil operacional, status de completude e dados patrimoniais ficam centralizados e adaptativos.

## Correções de dados incorretos

### Removido do modal

- Graham local;
- Bazin local;
- “Preço Teto” local;
- “Conselho VALORAE” local;
- alerta qualitativo calculado localmente.

Motivo: esses blocos pareciam informação oficial do Proxy, mas eram derivados locais. Para seguir sua regra, foram removidos. Se futuramente o Proxy retornar esses blocos como contrato oficial, o app poderá renderizá-los de forma explícita com fonte do Proxy.

### Removido fallback de preço médio

No `PortfolioViewModel`, o app não usa mais `avgPrice` como cotação quando o Proxy não retorna preço.

Antes:

```kotlin
livePrice = liveInfo?.price ?: avgPrice
```

Agora:

```kotlin
livePrice = liveInfo?.price ?: 0.0
```

Isso evita que preço médio de compra pareça cotação atual.

### Modal sem fallback local de mercado

O modal não usa mais `asset.currentPrice` como substituto automático para `realData.price`. Quando a cotação do Proxy não existe, aparece estado de indisponibilidade.

## Arquitetura da correção

A correção centraliza a lógica em `AssetProxySections.kt` para evitar duplicação e divergência entre página e modal.

Componentes principais:

- `AssetProxyIndicatorSection`
- `AssetProxyProfileSection`
- `buildAssetProxyIndicatorFields`
- `buildAssetProxyProfileFields`
- `AssetDataQualityCard`
- `AssetNewsSection`

Benefícios:

- mesma regra para Análise e Detalhes;
- menos risco de divergência visual;
- renderização adaptativa por tipo de ativo;
- inclusão automática de novos indicadores vindos do Proxy;
- campos ausentes ficam indisponíveis, sem simulação.

## Validações executadas

### Auditoria UI v2.0.4

Comando:

```bash
python3 scripts/verify_valorae_ui_v204.py
```

Resultado:

```text
OK - Versão do app atualizada para 2.0.4 / code 14
OK - Análise mantém somente três abas principais
OK - Análise usa pacote completo de gráficos do Proxy
OK - Análise renderiza Indicadores Gerais via builder dinâmico
OK - Perfil & Dados centraliza perfil e notícias
OK - Modal usa o mesmo pacote de gráficos avançados
OK - Modal usa Indicadores Gerais dinâmicos
OK - Modal usa Perfil & Dados dinâmico
OK - Sem card solto de Indicadores Fundamentalistas
OK - Sem P/VP máximo hardcoded
OK - Sem valuation local Graham/Bazin na análise/detalhes
OK - Sem fallback local de preço médio como cotação
OK - Campos ausentes são tratados como indisponíveis
OK - Ação e FII são tratados de forma adaptativa
VALORAE UI v2.0.4 audit OK
```

### XML Android

Resultado:

```text
XML OK
```

### Gradle

Comando executado:

```bash
./gradlew --version --no-daemon
```

Resultado real:

```text
UnknownHostException: services.gradle.org
```

O build Android não foi finalizado porque o ambiente não resolve DNS externo para baixar o Gradle. Não foi afirmado que o APK compilou.

## Critério de aceitação aplicado

A versão v2.0.4 é considerada corrigida no código-fonte se:

- a aba Análise mantém somente três abas principais;
- a página Análise usa o pacote completo de gráficos do Proxy;
- Detalhes do Ativo usa o mesmo pacote completo;
- Indicadores Gerais não tem campo hardcoded;
- Perfil & Dados centraliza perfil, dados e notícias;
- Ação e FII se adaptam ao tipo do ativo;
- ausência de dado vira estado vazio/indisponível;
- preço médio não é usado como cotação;
- não há simulação local de gráficos ou valuation.

## Observação importante

A correção foi feita para **renderizar tudo que o VALORAE Proxy fornecer**. Se algum gráfico ou indicador ainda não aparecer no app depois do build, a causa provável será uma destas:

1. o Proxy não retornou aquele campo para o ativo específico;
2. o campo veio em formato ainda não mapeado pelo parser;
3. a tela recebeu `AssetChartBundle` vazio por timeout/rede;
4. o endpoint público retornou parcial/cache incompleto.

Nesses casos, o próximo passo correto é capturar o JSON real do Proxy para o ticker afetado e ampliar o parser sem inventar dados.
