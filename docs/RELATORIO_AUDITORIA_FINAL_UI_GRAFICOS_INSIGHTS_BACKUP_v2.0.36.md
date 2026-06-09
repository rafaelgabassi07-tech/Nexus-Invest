# VALORAE APK v2.0.36 — Auditoria final UI, gráficos, Insights, Notícias e Backup

## Escopo
Correção conservadora e ponta-a-ponta sobre os problemas informados pelo usuário:

- Ranking da Home poluído por `+`, `%` e variação textual.
- Detalhes do Ativo abrindo sem chamar/receber os bundles de gráficos que a página Análise já recebia.
- Filtros de Lucro x Cotação mostrando dados só em MAX.
- Evolução Patrimonial perdendo Ativo e preservando só Patrimônio Líquido.
- Balanço Patrimonial mostrando “balanço real indisponível”.
- Comparação com índices sem fallback quando o bundle do ativo vem incompleto.
- Rentabilidade Nominal vs Real desenhando barra real falsa em 0%.
- Insights começando tarde demais.
- Notícias globais vazias por conflito de contrato com o Proxy.
- Backup e Dados falhando ao abrir seletor de arquivo.

## Arquivos alterados

- `app/build.gradle.kts`
- `metadata.json`
- `update.json`
- `index.html`
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/ui/screens/AssetsScreen.kt`
- `app/src/main/java/com/example/ui/components/AssetDetailModal.kt`
- `app/src/main/java/com/example/ui/components/AssetCharts.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`

## Correções principais

### Home / Ranking
O ranking agora mostra `#1`, `#2`, `#3` no fim da linha, em vez de repetir variações com `+` e `%`. O preço continua visível, e o próprio título do card já indica se é maior alta ou maior baixa.

### Detalhes do Ativo
Ao abrir qualquer ativo pela aba Ativos, o app solicita o bundle de gráficos com horizonte `MAX`. A modal também pede `MAX` para o bundle canônico financeiro, mantendo o gráfico de preço local no intervalo selecionado. Isso evita o caso em que a página Análise recebia dados completos e Detalhes ficava com contrato curto/incompleto.

### Gráficos financeiros
O merge de bundles no `PortfolioViewModel` deixou de escolher apenas a lista “mais longa”. Agora ele mescla ponto a ponto por ano/período, preservando campos complementares como Ativo, PL, Passivo, lucro, cotação e payout.

### Lucro x Cotação
O filtro agora usa fallback para o conjunto completo quando o recorte de 3A/5A/10A fica com menos pontos do que o necessário. O gráfico só usa pontos que têm cotação e lucro no mesmo período.

### Evolução Patrimonial e Balanço
A UI mescla `equityEvolution` com `balanceSheet`. Quando há Ativo + PL, mas Passivo não vem explícito, o app calcula Passivo pela identidade patrimonial `Ativo - PL` para evitar esconder o gráfico inteiro quando o Investidor10/Proxy entrega campos separados.

### Comparação com Índices
Quando o bundle `/asset` não traz pelo menos duas séries comparativas, o app tenta o endpoint `/api/v1/compare` e histórico de IBOV/IFIX/IPCA como fallback.

### Rentabilidade Nominal vs Real
A barra de rentabilidade real não é mais desenhada como 0% quando a série real está ausente. O tooltip mostra “Real: indisponível” nesse caso, evitando interpretação errada.

### Insights
O debounce de pré-aquecimento de analytics da carteira foi reduzido de 1800ms para 700ms e a intenção do fluxo foi ajustada: agenda, rankings e análise começam a ser solicitados após abertura/primeiro ativo, com timeouts curtos, em vez de depender só da entrada manual na página.

### Backup e Dados
O seletor de arquivo agora tenta primeiro `ActivityResultContracts.OpenDocument()` com múltiplos MIME types e permissão persistível de leitura. Se falhar, usa fallback `GetContent("*/*")`. SAF não requer permissão ampla de armazenamento para selecionar documentos individuais.

## Validações executadas

```bash
./gradlew --no-daemon assembleDebug
```

Resultado: falhou antes da compilação por rede do Gradle Wrapper:

```text
UnknownHostException: services.gradle.org
```

Validação estática adicional executada via script Python:

```text
APK static audit OK: ranking, details MAX bundle, charts merge/filter, news comparison fallback and SAF picker present
```

## Observações
A correção evita mexer em lógica já funcional de carteira, transações, banco Room e cálculo de posição. O ajuste maior foi no fluxo Detalhes/Gráficos e na experiência de importação de backup.
