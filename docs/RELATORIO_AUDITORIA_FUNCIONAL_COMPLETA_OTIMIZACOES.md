## Auditoria funcional completa VALORAE APK — carregamento e correções

Data: 2026-05-30

### Escopo revisado
- Início/Home: resumo da carteira, cards de maiores altas/baixas, abertura progressiva e chamadas não bloqueantes.
- Rankings: página própria na barra inferior, categorias estilo Investidor10, fallback Score/DY/PVP/PL/ROE/ROIC/FIIs e enriquecimento de preço.
- Análise: busca de ativo, bundle de gráficos avançados, notícias e histórico.
- Insights: Proventos, IPCA+, Diversificação e Agenda respeitando tempo real de existência da carteira.
- Notícias: rota do Proxy com TTL e sem fallback direto externo.
- Proxy+: Raio-X, FIIs, carteira avançada, radar e diagnóstico avançado sob demanda.
- Configurações: diagnóstico do Proxy, tema, segurança e ferramentas locais de importação/exportação.

### Correções e otimizações aplicadas nesta rodada
1. **Gráficos avançados sob demanda por aba**
   - Antes, `AssetChartBundlePanel` renderizava todos os blocos de gráficos de uma vez.
   - Agora usa abas internas e renderiza somente a aba selecionada.
   - Reduz recomposição, uso de CPU/GPU e tempo de abertura da tela de análise/detalhe.

2. **Abertura mais leve dos rankings**
   - A Home agora preserva a abertura leve buscando apenas o ranking ao vivo necessário para Maiores Altas/Baixas.
   - A página Rankings, ao ser aberta, solicita `full=true` para carregar os rankings completos.
   - Se uma chamada leve estiver em andamento e o usuário abrir Rankings, ela é cancelada e substituída pela carga completa.

3. **Atualização real de cotações em cache expirado**
   - Corrigido ponto em que o batch de preços podia não atualizar ativos já presentes no cache mesmo após TTL expirar.
   - Agora, quando o conjunto de tickers é o mesmo mas o cache venceu, o app busca novamente em lote via `/api/v1/assets`.

4. **Auditoria estática funcional adicionada**
   - Novo script: `scripts/verify_valorae_full_app_functionality.py`.
   - Cobre navegação, Home, Rankings, Análise, Insights, Proxy+, Notícias, Diagnóstico, segurança HTTPS e bloqueio de scraping direto.

### Validações executadas
```text
Valorae Proxy integration audit OK
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
Valorae Proxy capabilities audit OK
OK - B3NetworkService consome ranking de mercado
OK - B3NetworkService expõe ranking personalizado da carteira
OK - Ranking da carteira funciona com 1 ativo
OK - B3NetworkService expõe ranking ao vivo de ações
OK - Parser aceita rankings por critérios e perfis
OK - Portfolio analytics captura inteligência do Proxy
OK - ViewModel carrega rankings junto dos Insights
OK - Estado de Insights armazena rankings
OK - Rankings possui página própria na barra inferior
OK - Rankings não contaminam proventos/IPCA históricos
OK - UI renderiza ação/inteligência da carteira
OK - UI mostra categorias amplas de mercado quando altas/baixas não vêm
OK - Início exibe cards de altas/baixas do dia
OK - ViewModel carrega ranking ao vivo sem depender da carteira e sem bloquear abertura
OK - Parser preserva preço e variação separados nos movers
OK - Página de rankings tem containers estilo Investidor10
OK - Home usa cache local para preencher preço faltante em baixas
OK - Ranking ao vivo enriquece preço faltante pelo batch /api/v1/assets
OK - Rankings selecionam categoria carregada após resposta assíncrona
OK - Raio-X: qualidade do ativo
OK - Raio-X: plano de ação
OK - Raio-X: mapa de fontes
OK - Central FII: renda
OK - Central FII: vacância
OK - Central FII: checklist
OK - Carteira: rebalanceamento dedicado
OK - Carteira: risco dedicado
OK - Carteira: renda dedicada
OK - Radar / Watchlist
OK - Diagnóstico: maturidade do motor
OK - Diagnóstico: cache stats
OK - ViewModel expõe refresh de capacidades
OK - UI Proxy+ criada
OK - Proxy+ conectado na navegação
Valorae Proxy recommendations implementation audit OK
OK - Proxy+ não dispara endpoints avançados na abertura do app
OK - Proxy+ carrega sob demanda ao abrir a tela
OK - Parser de rankings aceita aliases de altas/baixas
OK - Parser de rankings aceita data/results/rankings
OK - Home de movimentos usa preço de cache/ativo
OK - Rankings tem página própria na barra inferior
OK - Proxy+ tem página própria na barra inferior
OK - Insights preserva lógica de existência da carteira
Valorae final consolidation audit OK
Valorae loading optimization audit OK
OK - Abertura escalona notícias
OK - Abertura escalona rankings
OK - TTL para notícias
OK - TTL para rankings de mercado
OK - TTL para saúde do Proxy
OK - TTL para cotações em lote
OK - Insights não baixa rankings globais a cada atualização da carteira
OK - Proxy+ não roda no refresh geral quando nunca aberto
OK - Proxy+ tem trava contra chamadas concorrentes
OK - OkHttp com concorrência moderada
OK - Rankings ao vivo com timeout curto
OK - Batch de ativos não vira dezenas de chamadas individuais
OK - Diagnóstico do Proxy tem cache
OK - Proxy+ limita blocos avançados por abertura
OK - Navegação possui Início, Rankings, Análise, Insights, Notícias e Proxy+
OK - App tem INTERNET no Manifest
OK - URL do Proxy é HTTPS/configurável e tem fallback oficial
OK - Home exibe movimentos do dia usando ranking ao vivo
OK - Rankings abre página completa com full=true, mas abertura do app usa apenas ranking ao vivo
OK - Rankings full cancela chamada leve em andamento para não ficar incompleto
OK - Cotações em lote atualizam cache expirado, não apenas tickers ausentes
OK - Gráficos avançados são renderizados sob demanda por aba
OK - Análise de ativo carrega bundle avançado e notícias sem bloquear UI
OK - Insights respeita existência da carteira para dividendos/IPCA
OK - Proxy+ é página própria e carrega sob demanda
OK - Notícias usam Proxy e TTL
OK - Diagnóstico usa readiness/status/métricas/cache
OK - Sem scraping direto no app Android
OK - Configurações preserva diagnóstico, tema e ferramentas locais
Valorae full app functionality and loading audit OK
```

### Resultado
- Auditoria da integração Proxy: OK.
- Auditoria de Insights por existência da carteira: OK.
- Auditoria de rankings/capacidades do Proxy: OK.
- Auditoria de Proxy+ avançado: OK.
- Auditoria de otimização de carregamento: OK.
- Auditoria funcional completa de páginas: OK.

### Gradle
Foi feita nova tentativa de build com o wrapper, mas o sandbox ainda não consegue resolver `services.gradle.org`:
```text
Baixando Gradle: https://services.gradle.org/distributions/gradle-9.3.1-bin.zip
Exception in thread "main" java.net.UnknownHostException: services.gradle.org
	at java.base/sun.nio.ch.NioSocketImpl.connect(NioSocketImpl.java:567)
	at java.base/java.net.SocksSocketImpl.connect(SocksSocketImpl.java:327)
	at java.base/java.net.Socket.connect(Socket.java:751)
	at java.base/sun.security.ssl.SSLSocketImpl.connect(SSLSocketImpl.java:304)
	at java.base/sun.security.ssl.BaseSSLSocketImpl.connect(BaseSSLSocketImpl.java:181)
	at java.base/sun.net.NetworkClient.doConnect(NetworkClient.java:183)
	at java.base/sun.net.www.http.HttpClient.openServer(HttpClient.java:531)
	at java.base/sun.net.www.http.HttpClient.openServer(HttpClient.java:636)
	at java.base/sun.net.www.protocol.https.HttpsClient.<init>(HttpsClient.java:264)
	at java.base/sun.net.www.protocol.https.HttpsClient.New(HttpsClient.java:377)
	at java.base/sun.net.www.protocol.https.AbstractDelegateHttpsURLConnection.getNewHttpClient(AbstractDelegateHttpsURLConnection.java:193)
	at java.base/sun.net.www.protocol.http.HttpURLConnection.plainConnect0(HttpURLConnection.java:1257)
	at java.base/sun.net.www.protocol.http.HttpURLConnection.plainConnect(HttpURLConnection.java:1143)
	at java.base/sun.net.www.protocol.https.AbstractDelegateHttpsURLConnection.connect(AbstractDelegateHttpsURLConnection.java:179)
	at java.base/sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1705)
	at java.base/sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1629)
	at java.base/sun.net.www.protocol.https.HttpsURLConnectionImpl.getInputStream(HttpsURLConnectionImpl.java:223)
	at java.base/java.net.URL.openStream(URL.java:1325)
	at org.gradle.wrapper.GradleWrapperMain.main(GradleWrapperMain.java:44)
```

No Android Studio, validar com:
```bash
./gradlew clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
./gradlew :app:lintDebug :app:check --stacktrace --info
```

### Arquivos principais alterados
- `app/src/main/java/com/example/ui/components/AssetCharts.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/ui/screens/RankingsScreen.kt`
- `scripts/verify_valorae_full_app_functionality.py`
- `docs/APK_BUILD_ATTEMPT_FULL_APP_AUDIT_LOG.txt`
