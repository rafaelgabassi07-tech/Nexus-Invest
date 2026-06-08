# RELATÓRIO — Performance de Gráficos e Index Web v2.0.32

## Objetivo

Reduzir o tempo percebido de montagem dos gráficos das abas **Desempenho & Índices** e **Finanças & Balanço** nas telas de Detalhes do Ativo/Análise, além de diminuir a espera inicial da página **Insights** quando o usuário abre o app ou adiciona o primeiro ativo.

## Causa encontrada

A modal `AssetDetailModal` disparava chamadas locais redundantes para:

- `B3NetworkService.fetchAssetData(ticker)`
- `B3NetworkService.fetchHistoricalChart(ticker, range)`
- `B3NetworkService.fetchAssetChartBundle(ticker, range)`

Essas chamadas ocorriam além da chamada oficial do `PortfolioViewModel.loadAssetChartBundle()`. Em rede lenta ou Vercel fria, isso criava chamadas duplicadas e encadeadas, com timeouts acumulados próximos de 1 minuto.

Além disso, `fetchAssetChartBundle()` buscava primeiro histórico e depois tentava `/api/v1/asset` em modo `max/complete`, com timeouts de 25s, 18s e 5s. A tela ficava aguardando o contrato completo antes de liberar a experiência.

## Correções aplicadas

### 1. Modal sem bundle duplicado

Arquivo:

```text
app/src/main/java/com/example/ui/components/AssetDetailModal.kt
```

A modal agora:

- Não chama mais `fetchAssetChartBundle()` diretamente.
- Usa o `ViewModel` como única fonte do bundle avançado.
- Carrega asset básico e histórico com timeouts curtos.
- Não mantém spinner infinito se o bundle avançado atrasar.
- Atualiza automaticamente quando o `ViewModel` entrega o bundle.
- Limita notícias do ativo a 2,5s para não travar a experiência.

### 2. Bundle de gráficos com contrato rápido

Arquivo:

```text
app/src/main/java/com/example/network/B3NetworkService.kt
```

`fetchAssetChartBundle()` agora chama o Proxy com:

```text
profile=chartfast
performance=chartfast
mode=charts-fast
charts=mobile
chartProfile=mobile-fast
includeNews=0
adaptiveCompletion=0
statusInvestComplement=0
timeoutMs=5500
valoraeScrapeTimeoutMs=4500
adaptiveCompletionTimeoutMs=1500
staleWhileRevalidate=1
staleIfError=1
```

O histórico de preço virou complemento e não bloqueia mais o contrato avançado.

### 3. Insights com orçamento menor

Arquivo:

```text
app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt
```

Reduzidos os orçamentos de chamadas concorrentes:

- Análise da carteira: 8s -> 6s.
- Histórico: 8s -> 6s.
- Agenda/proventos: 35s -> 9s.
- Ranking da carteira: 6s -> 4,5s.
- Bundle de ativo: 12s -> 6,5s.

Isso evita que a página Insights fique aguardando uma chamada longa de dividendos ou ranking antes de mostrar informações úteis.

### 4. Index.html atualizado

O arquivo `index.html` na raiz do APK foi substituído pelo arquivo enviado pelo usuário, identificado como versão web `2.0.32-desktop-apk-parity`.

### 5. Versionamento

- `versionName`: `2.0.32`
- `versionCode`: `42`

## Validação

Tentativa de build:

```bash
./gradlew --no-daemon assembleDebug
```

Resultado no ambiente isolado:

```text
UnknownHostException: services.gradle.org
```

A falha ocorreu antes da compilação do Kotlin, durante o download do Gradle Wrapper, por indisponibilidade de rede do ambiente.

## Resultado esperado

- Abrir Detalhes do Ativo não deve mais disparar chamadas duplicadas pesadas.
- As abas **Desempenho & Índices** e **Finanças & Balanço** devem sair do carregamento muito antes.
- Quando o Proxy estiver frio, a UI deve mostrar estado parcial/seguro em vez de aguardar quase 1 minuto.
- Quando o cache do Proxy estiver quente, os gráficos devem aparecer de forma bem mais rápida.
- A página Insights deve carregar informações locais/rápidas e não ficar bloqueada por agenda/ranking lento.
