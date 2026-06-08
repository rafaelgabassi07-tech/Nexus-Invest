package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.data.*
import com.example.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject

data class AssetSummary(
    val ticker: String,
    val type: String, // "ACAO" or "FII"
    val sharesCount: Double,
    val averageCost: Double,
    val totalInvested: Double,
    val currentPrice: Double,
    val totalCurrentValue: Double,
    val totalReturn: Double,
    val returnPercent: Double,
    val dailyChangePercent: Double,
    val dividendYield: Double,
    val lastDividend: Double,
    val nextEarningsDate: String
)

data class PortfolioSummary(
    val totalInvested: Double = 0.0,
    val totalCurrentValue: Double = 0.0,
    val totalReturn: Double = 0.0,
    val returnPercent: Double = 0.0,
    val sharesRatioStock: Double = 0.0, // stock ratio 0-1
    val sharesRatioFii: Double = 0.0,    // fii ratio 0-1
    val totalStocksInvested: Double = 0.0,
    val totalFiisInvested: Double = 0.0,
    val totalStocksCurrent: Double = 0.0,
    val totalFiisCurrent: Double = 0.0
)

data class PortfolioAnalyticsState(
    val isLoading: Boolean = false,
    val analysis: PortfolioProxyAnalysis? = null,
    val portfolioHistory: List<PortfolioHistoryPoint> = emptyList(),
    val ipcaSeries: List<IpcaPoint> = emptyList(),
    val dividendEvents: List<DividendEvent> = emptyList(),
    val portfolioRanking: MarketRankingSnapshot? = null,
    val liveMarketRanking: MarketRankingSnapshot? = null,
    val stockMarketRanking: MarketRankingSnapshot? = null,
    val fiiMarketRanking: MarketRankingSnapshot? = null,
    val marketRankingsAttempted: Boolean = false,
    val source: String = "Aguardando carteira",
    val lastUpdated: Long = 0L
)

data class ProxyHealthUiState(
    val status: String = "Verificando",
    val isOnline: Boolean = false,
    val isUsingCache: Boolean = false,
    val lastCheckedAt: Long = 0L,
    val diagnostics: ProxyDiagnosticsSummary? = null
)


data class ProxyCapabilitiesUiState(
    val isLoading: Boolean = false,
    val selectedTicker: String = "",
    val assetCapabilities: AssetProxyCapabilities? = null,
    val portfolioCapabilities: PortfolioProxyCapabilities? = null,
    val error: String = "",
    val lastUpdated: Long = 0L
)

data class DarfMonthSummary(
    val monthLabel: String, // "MM/YYYY"
    val monthIdx: Int,      // for sorting
    val year: Int,
    val stockSalesVolume: Double,
    val stockProfit: Double,
    val stockTax: Double,
    val fiiSalesVolume: Double,
    val fiiProfit: Double,
    val fiiTax: Double,
    val totalTax: Double
)

class PortfolioViewModel(private val repository: TransactionRepository) : ViewModel() {

    // List of transactions from Room db
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notifications: StateFlow<List<DbNotification>> = repository.allNotifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val changelogs: StateFlow<List<DbChangelog>> = repository.allChangelogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            setupInitialDataIfNeeded()
        }
    }

    private suspend fun setupInitialDataIfNeeded() {
        val currentNotes = repository.getAllNotificationsSync()
        if (currentNotes.isEmpty()) {
            val defaults = listOf(
                DbNotification(
                    title = "Bem-vindo ao VALORAE",
                    description = "Ficamos felizes em ter você aqui! Gerencie seus investimentos da B3 com precisão. Comece adicionando ou importando suas transações de forma automatizada na aba de configurações.",
                    category = "SISTEMA",
                    date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                    iconName = "WALLET",
                    isRead = false
                ),
                DbNotification(
                    title = "Motor de Cotações v2.4 Ativo",
                    description = "Upgrade massivo na busca offline de FIIs e análise de dividendos. Suas informações são protegidas e persistidas exclusivamente localmente.",
                    category = "SISTEMA",
                    date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                    iconName = "TRENDING",
                    isRead = false
                )
            )
            repository.insertAllNotifications(defaults)
        }

        val defaultChangelogs = listOf(
            DbChangelog(
                versionName = com.example.BuildConfig.VERSION_NAME,
                releaseNotes = "Motor Inteligente v2.4: Upgrade massivo no suporte de FIIs (Vacância, Cotistas e Liquidez).\nBusca Ultra: Implementação de sugestões inteligentes em tempo real na página de Análise.\nInteligência de Análise: Novo sistema de conselhos automáticos baseados em múltiplos indicadores.\nCentral de Ajuda: Lançamento de um guia integrado com dicas de uso e suporte técnico.\nDesign Moderno: Reorganização das configurações em clusters e polimento visual na Dashboard.\nCompartilhamento APK: Agora você pode convidar amigos enviando o próprio instalador do app.\nPágina de Notícias: Integração de feeds de notícias específicos para cada ativo monitorado.\nRegistro de Ativos: Interface de cadastro aprimorada com seletores premium e feedback tátil.\nCorreções Estáveis: Refatoração da importação de tabelas B3 e tratamento de erros de rede.",
                date = "Maio 2026"
            ),
            DbChangelog(
                versionName = "1.0.1",
                releaseNotes = "Lançamento inicial do VALORAE.\nEstrutura de banco de dados SQLite/Room integrado.\nSuporte a importação de planilhas do investidor B3.\nCálculo de preço médio ponderado ponderado automático.",
                date = "Abril 2026"
            )
        )
        repository.insertAllChangelogs(defaultChangelogs)
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAllNotificationsAsRead()
        }
    }

    fun markNotificationAsRead(notification: DbNotification) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNotification(notification.copy(isRead = true))
        }
    }

    fun deleteNotificationById(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotificationById(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllNotifications()
        }
    }

    fun addUpdateNotification(info: AppUpdateInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentNotifications = repository.getAllNotificationsSync()
            val alreadyNotified = currentNotifications.any { it.iconName == "UPDATE" && it.title.contains(info.versionName) }
            
            if (!alreadyNotified) {
                repository.insertNotification(
                    DbNotification(
                        title = "Atualização Disponível v${info.versionName}",
                        description = info.releaseNotes ?: "Nova versão de patch disponível para download nas configurações.",
                        category = "SISTEMA",
                        date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                        iconName = "UPDATE",
                        isRead = false
                    )
                )
            }
            
            val notes = info.releaseNotes ?: "Melhorias gerais e otimizações de performance."
            repository.insertChangelog(
                DbChangelog(
                    versionName = info.versionName,
                    releaseNotes = notes,
                    date = "Hoje"
                )
            )
        }
    }

    // Cached indicators from B3 Network
    private val _cachedAssetData = MutableStateFlow<Map<String, B3AssetData>>(emptyMap())
    val cachedAssetData: StateFlow<Map<String, B3AssetData>> = _cachedAssetData.asStateFlow()

    private val _isLoadingPrices = MutableStateFlow(false)
    val isLoadingPrices: StateFlow<Boolean> = _isLoadingPrices.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Global dashboard compiled summaries
    val assetSummaries: StateFlow<List<AssetSummary>> = combine(transactions, _cachedAssetData) { txs, cache ->
        if (txs.isEmpty()) return@combine emptyList<AssetSummary>()
        
        // Group by ticker
        val grouped = txs.groupBy { it.ticker.trim().uppercase() }
        
        grouped.map { (ticker, list) ->
            val liveInfo = cache[ticker]
            val declaredType = list.firstOrNull()?.type?.trim()?.uppercase() ?: "ACAO"
            val type = when {
                liveInfo?.isFii == true -> "FII"
                B3NetworkService.inferIsFii(ticker) -> "FII"
                declaredType == "FII" -> "FII"
                else -> "ACAO"
            }
            
            var currentShares = 0.0
            var remainingCostBasis = 0.0
            
            val sortedTxs = list.sortedBy { it.date }
            
            // Custo médio móvel: compras aumentam posição/custo; vendas baixam o custo
            // proporcional ao preço médio vigente. A lógica anterior usava média de todas
            // as compras históricas e podia distorcer rentabilidade após vendas parciais.
            for (tx in sortedTxs) {
                val qty = tx.quantity.takeIf { it.isFinite() && it > 0.0 } ?: continue
                val price = tx.purchasePrice.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                if (!tx.isSell) {
                    currentShares += qty
                    remainingCostBasis += qty * price
                } else if (currentShares > 0.0) {
                    val qtySold = qty.coerceAtMost(currentShares)
                    val avgBeforeSale = if (currentShares > 0.0) remainingCostBasis / currentShares else 0.0
                    currentShares -= qtySold
                    remainingCostBasis -= qtySold * avgBeforeSale
                    if (currentShares <= 0.0001) {
                        currentShares = 0.0
                        remainingCostBasis = 0.0
                    }
                }
            }
            
            if (currentShares < 0.0001) {
                currentShares = 0.0
                remainingCostBasis = 0.0
            }
            
            val avgPrice = if (currentShares > 0.0 && remainingCostBasis > 0.0) remainingCostBasis / currentShares else 0.0
            val totalCostBasis = remainingCostBasis.coerceAtLeast(0.0)

            val livePrice = liveInfo?.price?.takeIf { it.isFinite() && it > 0.0 } ?: avgPrice
            val liveDY = liveInfo?.dy?.takeIf { it.isFinite() } ?: 0.0
            val liveChange = liveInfo?.changePercent?.takeIf { it.isFinite() } ?: 0.0
            val lastDiv = liveInfo?.lastDividend?.takeIf { it.isFinite() } ?: 0.0
            val nextEarningsDate = liveInfo?.nextEarningsDate ?: ""

            val currentVal = (currentShares * (if (livePrice > 0.0) livePrice else avgPrice)).takeIf { it.isFinite() } ?: 0.0
            val retVal = currentVal - totalCostBasis
            val retPct = if (totalCostBasis > 0) (retVal / totalCostBasis) * 100.0 else 0.0

            AssetSummary(
                ticker = ticker,
                type = type,
                sharesCount = currentShares,
                averageCost = avgPrice,
                totalInvested = totalCostBasis,
                currentPrice = livePrice,
                totalCurrentValue = currentVal,
                totalReturn = retVal,
                returnPercent = retPct,
                dailyChangePercent = liveChange,
                dividendYield = liveDY,
                lastDividend = lastDiv,
                nextEarningsDate = nextEarningsDate
            )
        }.filter { it.sharesCount > 0 }.sortedByDescending { it.totalCurrentValue }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val darfSummaries: StateFlow<List<DarfMonthSummary>> = transactions.map { txs ->
        if (txs.isEmpty()) return@map emptyList<DarfMonthSummary>()
        
        val cal = java.util.Calendar.getInstance()
        val monthlyGroups = txs.filter { it.isSell }.groupBy {
            cal.timeInMillis = it.date
            val month = cal.get(java.util.Calendar.MONTH) + 1
            val year = cal.get(java.util.Calendar.YEAR)
            String.format("%02d/%d", month, year)
        }

        // To calculate profit accurately, we need avg cost at the moment of sale.
        // We cluster all transactions by ticker first.
        val tickerTxs = txs.groupBy { it.ticker.uppercase() }
        
        monthlyGroups.map { (monthLabel, sTxs) ->
            val parts = monthLabel.split("/")
            val monthIdx = parts[1].toInt() * 100 + parts[0].toInt()
            
            var sVolume = 0.0
            var sProfit = 0.0
            var fVolume = 0.0
            var fProfit = 0.0
            
            // For each sale in this month
            sTxs.forEach { sale ->
                val allTickerTxs = tickerTxs[sale.ticker] ?: emptyList()
                val sorted = allTickerTxs.sortedBy { it.date }
                
                // Calculate average cost UP TO this sale
                var sharesBefore = 0.0
                var costBasisBefore = 0.0
                for (tx in sorted) {
                    if (tx.id == sale.id) break
                    if (!tx.isSell) {
                        sharesBefore += tx.quantity
                        costBasisBefore += (tx.quantity * tx.purchasePrice)
                    } else {
                        if (sharesBefore > 0) {
                            val avg = costBasisBefore / sharesBefore
                            val qtySold = tx.quantity.coerceAtMost(sharesBefore).coerceAtLeast(0.0)
                            sharesBefore -= qtySold
                            costBasisBefore -= (qtySold * avg)
                        }
                    }
                }
                
                val qtySold = sale.quantity.coerceAtMost(sharesBefore.coerceAtLeast(0.0)).coerceAtLeast(0.0)
                if (qtySold <= 0.0) return@forEach
                val avgCost = if (sharesBefore > 0) costBasisBefore / sharesBefore else 0.0
                val profit = (sale.purchasePrice - avgCost) * qtySold
                val saleVolume = qtySold * sale.purchasePrice
                
                if (sale.type == "ACAO") {
                    sVolume += saleVolume
                    sProfit += profit
                } else {
                    fVolume += saleVolume
                    fProfit += profit
                }
            }
            
            // Brazil Tax Rules:
            // Stocks: 15% profit if volume > 20k.
            val sTax = if (sVolume > 20000.0 && sProfit > 0) sProfit * 0.15 else 0.0
            // FIIs: 20% on any profit
            val fTax = if (fProfit > 0) fProfit * 0.20 else 0.0
            
            DarfMonthSummary(
                monthLabel = monthLabel,
                monthIdx = monthIdx,
                year = parts[1].toInt(),
                stockSalesVolume = sVolume,
                stockProfit = sProfit,
                stockTax = sTax,
                fiiSalesVolume = fVolume,
                fiiProfit = fProfit,
                fiiTax = fTax,
                totalTax = sTax + fTax
            )
        }.sortedByDescending { it.monthIdx }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val portfolioSummary: StateFlow<PortfolioSummary> = assetSummaries.map { summaries ->
        if (summaries.isEmpty()) return@map PortfolioSummary()

        var totalInvested = 0.0
        var totalCurrent = 0.0
        var totalStocksSpent = 0.0
        var totalFiisSpent = 0.0
        var totalStocksCurrent = 0.0
        var totalFiisCurrent = 0.0

        summaries.forEach { s ->
            totalInvested += s.totalInvested
            totalCurrent += s.totalCurrentValue
            if (s.type == "ACAO") {
                totalStocksSpent += s.totalInvested
                totalStocksCurrent += s.totalCurrentValue
            } else {
                totalFiisSpent += s.totalInvested
                totalFiisCurrent += s.totalCurrentValue
            }
        }

        val totalRet = totalCurrent - totalInvested
        val retPct = if (totalInvested > 0) (totalRet / totalInvested) * 100.0 else 0.0
        val stocksRatio = if (totalCurrent > 0) totalStocksCurrent / totalCurrent else if (totalInvested > 0) totalStocksSpent / totalInvested else 0.0
        val fiisRatio = if (totalCurrent > 0) totalFiisCurrent / totalCurrent else if (totalInvested > 0) totalFiisSpent / totalInvested else 0.0

        PortfolioSummary(
            totalInvested = totalInvested,
            totalCurrentValue = totalCurrent,
            totalReturn = totalRet,
            returnPercent = retPct,
            sharesRatioStock = stocksRatio,
            sharesRatioFii = fiisRatio,
            totalStocksInvested = totalStocksSpent,
            totalFiisInvested = totalFiisSpent,
            totalStocksCurrent = totalStocksCurrent,
            totalFiisCurrent = totalFiisCurrent
        )
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioSummary())

    // News state
    private val _newsFeed = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsFeed: StateFlow<List<NewsItem>> = _newsFeed.asStateFlow()

    private val _isLoadingNews = MutableStateFlow(false)
    val isLoadingNews: StateFlow<Boolean> = _isLoadingNews.asStateFlow()

    // Selected / searched asset analyzer state
    val searchTickerInput = MutableStateFlow("")
    private val _searchQueryResult = MutableStateFlow<B3AssetData?>(null)
    val searchQueryResult: StateFlow<B3AssetData?> = _searchQueryResult.asStateFlow()

    private val _searchQueryHistory = MutableStateFlow<List<ChartPoint>>(emptyList())
    val searchQueryHistory: StateFlow<List<ChartPoint>> = _searchQueryHistory.asStateFlow()

    private val _searchQueryNews = MutableStateFlow<List<NewsItem>>(emptyList())
    val searchQueryNews: StateFlow<List<NewsItem>> = _searchQueryNews.asStateFlow()

    private val _isSearchingAsset = MutableStateFlow(false)
    val isSearchingAsset: StateFlow<Boolean> = _isSearchingAsset.asStateFlow()

    private val _searchQueryRange = MutableStateFlow("1y")
    val searchQueryRange: StateFlow<String> = _searchQueryRange.asStateFlow()

    private val _assetChartBundles = MutableStateFlow<Map<String, AssetChartBundle>>(emptyMap())
    val assetChartBundles: StateFlow<Map<String, AssetChartBundle>> = _assetChartBundles.asStateFlow()

    private val _isLoadingChartBundle = MutableStateFlow(false)
    val isLoadingChartBundle: StateFlow<Boolean> = _isLoadingChartBundle.asStateFlow()
    private val loadingChartBundleKeys = mutableSetOf<String>()

    private val _portfolioAnalytics = MutableStateFlow(PortfolioAnalyticsState())
    val portfolioAnalytics: StateFlow<PortfolioAnalyticsState> = _portfolioAnalytics.asStateFlow()

    private val _proxyHealth = MutableStateFlow(ProxyHealthUiState())
    val proxyHealth: StateFlow<ProxyHealthUiState> = _proxyHealth.asStateFlow()


    private val _proxyCapabilities = MutableStateFlow(ProxyCapabilitiesUiState())
    val proxyCapabilities: StateFlow<ProxyCapabilitiesUiState> = _proxyCapabilities.asStateFlow()

    private var newsJob: Job? = null
    private var proxyHealthJob: Job? = null
    private var marketRankingsJob: Job? = null
    private var proxyCapabilitiesJob: Job? = null
    private var searchAssetJob: Job? = null
    private var chartRangeJob: Job? = null
    private var lastNewsRefreshAt = 0L
    private var lastMarketRankingsRefreshAt = 0L
    private var lastProxyHealthRefreshAt = 0L
    private var lastPortfolioAnalyticsRefreshAt = 0L
    private var lastPortfolioAnalyticsSignature = ""
    private var lastPriceFetchSignature = ""
    private var lastPriceFetchAt = 0L
    private var inFlightPriceFetchSignature = ""
    private var proxyCapabilitiesRequestToken = 0L
    private var lastSearchTicker = ""
    private var lastSearchAt = 0L

    private companion object {
        const val PRICE_CACHE_SOFT_TTL_MS = 2 * 60 * 1000L
        const val NEWS_SOFT_TTL_MS = 10 * 60 * 1000L
        const val MARKET_RANKINGS_SOFT_TTL_MS = 5 * 60 * 1000L
        const val PROXY_HEALTH_SOFT_TTL_MS = 90 * 1000L
        const val PORTFOLIO_ANALYTICS_SOFT_TTL_MS = 2 * 60 * 1000L
        const val PROXY_CAPABILITIES_SOFT_TTL_MS = 30 * 60 * 1000L
        const val ASSET_SEARCH_SOFT_TTL_MS = 90 * 1000L
    }

    private fun isFresh(lastUpdatedAt: Long, ttlMs: Long): Boolean {
        return lastUpdatedAt > 0L && System.currentTimeMillis() - lastUpdatedAt < ttlMs
    }

    private fun unavailableLiveMarketRanking(reason: String = "Ranking temporariamente indisponível"): MarketRankingSnapshot {
        return MarketRankingSnapshot(
            type = "ACAO",
            source = "VALORAE Proxy",
            fallbackUsed = true,
            warnings = listOf(reason)
        )
    }

    private fun normalizeAssetRange(range: String): String {
        return when (range.trim().lowercase(java.util.Locale.ROOT)) {
            "1d", "d1" -> "1D"
            "5d", "d5" -> "5D"
            "1mo", "1m", "m1", "30d" -> "1M"
            "3mo", "3m", "m3" -> "3M"
            "6mo", "6m", "m6" -> "6M"
            "ytd", "ano" -> "YTD"
            "1y", "1a", "y1", "12m" -> "1Y"
            "3y", "3a", "y3" -> "3Y"
            "5y", "5a", "y5" -> "5Y"
            "10y", "10a", "y10" -> "10Y"
            "max", "tudo", "all" -> "MAX"
            else -> range.trim().uppercase(java.util.Locale.ROOT).ifBlank { "1Y" }
        }
    }

    init {
        // Automatically fetch prices for loaded tickers when room updates, but with debouncing
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            transactions
                .debounce(1500) // Avoid thrashing on multiple rapid imports
                .collect { list ->
                val distinctTickers = list.map { it.ticker.trim().uppercase() }.distinct()
                if (distinctTickers.isNotEmpty()) {
                    triggerBatchedPriceFetch(distinctTickers, showSecondaryLoading = false)
                }
            }
        }
        
        // Abertura leve: a UI não fica bloqueada por notícias/rankings/diagnósticos.
        // As chamadas remotas são escalonadas e respeitam TTL para evitar rajadas no Proxy/Vercel Free.
        refreshProxyHealth()
        viewModelScope.launch {
            delay(600)
            fetchGlobalNews(force = false)
        }
        viewModelScope.launch {
            delay(1200)
            refreshLiveMarketRankings(force = false)
        }

        // Dados avançados é carregado sob demanda ao abrir a página/ao tocar em Atualizar.
        // Evita disparar dezenas de endpoints avançados na abertura do app, preservando
        // fluidez, bateria e compatibilidade com Vercel Free.

        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            combine(assetSummaries, transactions) { summaries, txs -> summaries to txs }
                .debounce(1800)
                .collect { (summaries, txs) ->
                    if (summaries.isNotEmpty()) {
                        refreshPortfolioAnalytics(summaries, txs)
                    } else {
                        val current = _portfolioAnalytics.value
                        _portfolioAnalytics.value = PortfolioAnalyticsState(
                            liveMarketRanking = current.liveMarketRanking,
                            stockMarketRanking = current.stockMarketRanking,
                            fiiMarketRanking = current.fiiMarketRanking,
                            marketRankingsAttempted = current.marketRankingsAttempted,
                            source = if (current.liveMarketRanking != null || current.stockMarketRanking != null || current.fiiMarketRanking != null) "Rankings de mercado" else "Aguardando carteira",
                            lastUpdated = current.lastUpdated
                        )
                    }
                }
        }
    }

    fun insertTransaction(
        ticker: String, quantity: Double, purchasePrice: Double, type: String,
        broker: String = "", sector: String = "", date: Long? = null, notes: String = "",
        isSell: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitizedTicker = ticker.trim().uppercase()
            val tx = Transaction(
                ticker = sanitizedTicker,
                name = sanitizedTicker,
                quantity = quantity,
                purchasePrice = purchasePrice,
                type = type,
                isSell = isSell,
                broker = broker,
                sector = sector,
                date = date ?: System.currentTimeMillis(),
                notes = notes
            )
            repository.insert(tx)
            
            val action = if (isSell) "Venda" else "Compra"
            repository.insertNotification(
                DbNotification(
                    title = "$action de $sanitizedTicker Registrada",
                    description = "Transação de $action de ${String.format("%.2f", quantity)} cotas/ações de $sanitizedTicker a R$ ${String.format("%.2f", purchasePrice)} foi adicionada com sucesso ao portfólio.",
                    category = "TRANSAÇÃO",
                    date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                    iconName = "WALLET",
                    isRead = false
                )
            )
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(tx)
        }
    }

    fun updateTransaction(
        id: Int, ticker: String, quantity: Double, purchasePrice: Double, type: String,
        broker: String = "", sector: String = "", date: Long? = null, notes: String = "",
        isSell: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitizedTicker = ticker.trim().uppercase()
            val tx = Transaction(
                id = id,
                ticker = sanitizedTicker,
                name = sanitizedTicker,
                quantity = quantity,
                purchasePrice = purchasePrice,
                type = type,
                isSell = isSell,
                broker = broker,
                sector = sector,
                date = date ?: System.currentTimeMillis(),
                notes = notes
            )
            repository.insert(tx)
        }
    }


    private fun currentProxyPositions(): List<PortfolioProxyPosition> {
        val summaries = assetSummaries.value
        val txs = transactions.value
        val firstPurchaseByTicker = txs
            .filter { !it.isSell && it.quantity > 0.0 }
            .groupBy { it.ticker.trim().uppercase(java.util.Locale.ROOT) }
            .mapValues { (_, list) -> list.minOfOrNull { it.date } ?: 0L }
        return summaries.map { summary ->
            val key = summary.ticker.trim().uppercase(java.util.Locale.ROOT)
            PortfolioProxyPosition(
                ticker = summary.ticker,
                quantity = summary.sharesCount,
                averagePrice = summary.averageCost,
                type = summary.type,
                currentPrice = summary.currentPrice,
                totalInvested = summary.totalInvested,
                firstPurchaseAt = firstPurchaseByTicker[key] ?: 0L
            )
        }.filter { it.quantity > 0.0 }
    }

    fun refreshProxyCapabilities(ticker: String? = null, force: Boolean = true) {
        val selected = (ticker ?: searchTickerInput.value.ifBlank { assetSummaries.value.firstOrNull()?.ticker.orEmpty() })
            .trim()
            .uppercase(java.util.Locale.ROOT)
        val current = _proxyCapabilities.value
        if (proxyCapabilitiesJob?.isActive == true) {
            if (force || current.selectedTicker != selected) {
                proxyCapabilitiesJob?.cancel()
            } else {
                return
            }
        }
        if (!force && current.selectedTicker == selected && current.lastUpdated > 0L && isFresh(current.lastUpdated, PROXY_CAPABILITIES_SOFT_TTL_MS)) return
        if (!force && current.assetCapabilities != null && current.portfolioCapabilities != null && current.selectedTicker == selected) return
        val requestToken = System.currentTimeMillis()
        proxyCapabilitiesRequestToken = requestToken
        proxyCapabilitiesJob = viewModelScope.launch {
            _proxyCapabilities.value = current.copy(isLoading = true, error = "", selectedTicker = selected)
            val positions = currentProxyPositions()
            val watchlist = (positions.map { it.ticker } + listOf(selected)).filter { it.isNotBlank() }.distinct()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val assetDeferred = if (selected.isNotBlank()) {
                        async { B3NetworkService.fetchAssetProxyCapabilities(selected, positions.any { it.ticker.equals(selected, true) && it.type.equals("FII", true) }, bypassCache = force) }
                    } else null
                    val portfolioDeferred = async { B3NetworkService.fetchPortfolioProxyCapabilities(positions, watchlist, bypassCache = force) }
                    assetDeferred?.await() to portfolioDeferred.await()
                }
            }
            result.onSuccess { (assetCaps, portfolioCaps) ->
                if (requestToken == proxyCapabilitiesRequestToken) {
                    _proxyCapabilities.value = ProxyCapabilitiesUiState(
                        isLoading = false,
                        selectedTicker = selected,
                        assetCapabilities = assetCaps,
                        portfolioCapabilities = portfolioCaps,
                        error = if (assetCaps == null && portfolioCaps == null) "O serviço de dados não retornou módulos avançados para o contexto atual." else "",
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            }.onFailure { e ->
                if (requestToken == proxyCapabilitiesRequestToken) {
                    _proxyCapabilities.value = current.copy(
                        isLoading = false,
                        selectedTicker = selected,
                        error = e.message ?: "Falha ao consultar recursos avançados",
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    fun refreshProxyHealth(force: Boolean = false) {
        if (proxyHealthJob?.isActive == true) return
        if (!force && isFresh(lastProxyHealthRefreshAt, PROXY_HEALTH_SOFT_TTL_MS) && _proxyHealth.value.lastCheckedAt > 0L) return
        proxyHealthJob = viewModelScope.launch {
            val diagnostics = withContext(Dispatchers.IO) {
                withTimeoutOrNull(3_500) {
                    runCatching { B3NetworkService.fetchProxyDiagnosticsSummary() }.getOrNull()
                }
            }
            lastProxyHealthRefreshAt = System.currentTimeMillis()
            _proxyHealth.value = if (diagnostics != null) {
                ProxyHealthUiState(
                    status = diagnostics.state,
                    isOnline = diagnostics.ready,
                    isUsingCache = diagnostics.usingLocalCache,
                    lastCheckedAt = diagnostics.lastCheckedAt,
                    diagnostics = diagnostics
                )
            } else {
                ProxyHealthUiState(
                    status = if (_cachedAssetData.value.isNotEmpty()) "Usando cache local" else "Offline",
                    isOnline = false,
                    isUsingCache = _cachedAssetData.value.isNotEmpty(),
                    lastCheckedAt = System.currentTimeMillis(),
                    diagnostics = null
                )
            }
        }
    }

    fun forceRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val distinctTickers = transactions.value.map { it.ticker.trim().uppercase() }.distinct()
                if (distinctTickers.isNotEmpty()) {
                    triggerBatchedPriceFetch(distinctTickers, showSecondaryLoading = true)
                }
                refreshProxyHealth(force = true)
                fetchGlobalNews(force = true)
                if (assetSummaries.value.isNotEmpty()) {
                    refreshPortfolioAnalytics(assetSummaries.value, transactions.value, force = true)
                }
                refreshLiveMarketRankings(force = true, full = true)
                if (_proxyCapabilities.value.lastUpdated > 0L) {
                    refreshProxyCapabilities(force = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Erro ao atualizar dados do app", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshLiveMarketRankings(force: Boolean = false, full: Boolean = false) {
        val currentState = _portfolioAnalytics.value
        val hasEnoughData = if (full) {
            currentState.liveMarketRanking != null && currentState.stockMarketRanking != null && currentState.fiiMarketRanking != null
        } else {
            currentState.liveMarketRanking != null
        }
        if (!force && hasEnoughData && isFresh(lastMarketRankingsRefreshAt, MARKET_RANKINGS_SOFT_TTL_MS)) return
        if (marketRankingsJob?.isActive == true) {
            if (full) marketRankingsJob?.cancel() else return
        }

        _portfolioAnalytics.value = currentState.copy(isLoading = true)

        marketRankingsJob = viewModelScope.launch {
            try {
                val baseState = _portfolioAnalytics.value
                val (live, stock, fii) = withContext(Dispatchers.IO) {
                    coroutineScope {
                        // A Home sempre usa o modo completo do Proxy para maiores altas/baixas.
                        // O próprio serviço cai para modo leve caso o endpoint completo demore ou volte vazio.
                        val liveDeferred = async {
                            withTimeoutOrNull(if (full) 18_000 else 14_000) {
                                runCatching { B3NetworkService.fetchLiveStockRankings(complete = true) }.getOrNull()
                            }
                        }
                        if (full) {
                            val stockDeferred = async { withTimeoutOrNull(18_000) { runCatching { B3NetworkService.fetchStockFundamentalRankings(complete = true) }.getOrNull() } }
                            val fiiDeferred = async { withTimeoutOrNull(18_000) { runCatching { B3NetworkService.fetchFiiFundamentalRankings(complete = true) }.getOrNull() } }
                            Triple(liveDeferred.await(), stockDeferred.await(), fiiDeferred.await())
                        } else {
                            Triple(liveDeferred.await(), baseState.stockMarketRanking, baseState.fiiMarketRanking)
                        }
                    }
                }
                lastMarketRankingsRefreshAt = System.currentTimeMillis()
                val current = _portfolioAnalytics.value
                val resolvedLive = live
                    ?: current.liveMarketRanking
                    ?: unavailableLiveMarketRanking("Não foi possível carregar as maiores altas/baixas agora. Use tentar novamente.")
                _portfolioAnalytics.value = current.copy(
                    isLoading = false,
                    liveMarketRanking = resolvedLive,
                    stockMarketRanking = stock ?: current.stockMarketRanking,
                    fiiMarketRanking = fii ?: current.fiiMarketRanking,
                    marketRankingsAttempted = true,
                    source = if (current.analysis != null || current.portfolioRanking != null) current.source else "Rankings de mercado",
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Erro ao carregar rankings da Home", e)
                val current = _portfolioAnalytics.value
                _portfolioAnalytics.value = current.copy(
                    isLoading = false,
                    liveMarketRanking = current.liveMarketRanking ?: unavailableLiveMarketRanking("Falha temporária ao carregar rankings. Verifique a conexão e tente novamente."),
                    marketRankingsAttempted = true,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        }
    }

    private suspend fun triggerBatchedPriceFetch(tickers: List<String>, showSecondaryLoading: Boolean) {
        val normalizedTickers = tickers.map { it.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
        val signature = normalizedTickers.sorted().joinToString(",")
        val sameTickerSet = signature.isNotBlank() && signature == lastPriceFetchSignature
        val cacheStillFresh = sameTickerSet && isFresh(lastPriceFetchAt, PRICE_CACHE_SOFT_TTL_MS)
        if (!showSecondaryLoading && cacheStillFresh) return
        if (!showSecondaryLoading && signature.isNotBlank() && signature == inFlightPriceFetchSignature) return
        if (signature.isNotBlank()) inFlightPriceFetchSignature = signature
        if (showSecondaryLoading) _isLoadingPrices.value = true
        try {
            val updated = withContext(Dispatchers.IO) {
                val currentCache = _cachedAssetData.value
                val updatedMap = java.util.concurrent.ConcurrentHashMap<String, B3AssetData>(currentCache)
                val shouldRefreshExisting = showSecondaryLoading || (sameTickerSet && !cacheStillFresh)

                val tickersToFetch = normalizedTickers
                    .filter { shouldRefreshExisting || !currentCache.containsKey(it) }

                if (tickersToFetch.isNotEmpty()) {
                    val batch = runCatching {
                        B3NetworkService.fetchAssetsData(tickersToFetch, bypassCache = showSecondaryLoading || shouldRefreshExisting)
                    }.getOrDefault(emptyMap())
                    batch.forEach { (ticker, data) ->
                        updatedMap[ticker] = data
                    }
                }

                updatedMap.toMap()
            }
            if (signature.isNotBlank()) {
                lastPriceFetchSignature = signature
                lastPriceFetchAt = System.currentTimeMillis()
            }
            _cachedAssetData.value = updated
        } catch (e: Exception) {
            android.util.Log.e("PortfolioViewModel", "Erro ao atualizar cotações em lote", e)
        } finally {
            if (signature.isNotBlank() && inFlightPriceFetchSignature == signature) inFlightPriceFetchSignature = ""
            if (showSecondaryLoading) _isLoadingPrices.value = false
        }
    }

    fun fetchGlobalNews(force: Boolean = false) {
        if (!force && _newsFeed.value.isNotEmpty() && isFresh(lastNewsRefreshAt, NEWS_SOFT_TTL_MS)) return
        if (newsJob?.isActive == true) return
        newsJob = viewModelScope.launch {
            _isLoadingNews.value = true
            try {
                val news = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(5_500) {
                        runCatching { B3NetworkService.fetchNews("") }.getOrDefault(emptyList())
                    }.orEmpty()
                }
                _newsFeed.value = news
                lastNewsRefreshAt = System.currentTimeMillis()
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Erro ao carregar notícias", e)
                // Preserve o último bloco bom de notícias; uma atualização manual que falha não deve apagar a Home.
                if (_newsFeed.value.isEmpty()) _newsFeed.value = emptyList()
            } finally {
                _isLoadingNews.value = false
            }
        }
    }

    fun loadAssetChartBundle(ticker: String, range: String = "1Y") {
        val clean = ticker.trim().uppercase(java.util.Locale.ROOT)
        if (clean.isEmpty()) return
        val normalizedRange = normalizeAssetRange(range)
        val alreadyLoaded = _assetChartBundles.value[clean]
        if (alreadyLoaded != null && alreadyLoaded.range.equals(normalizedRange, ignoreCase = true)) return
        val loadingKey = "$clean:$normalizedRange"
        if (loadingChartBundleKeys.contains(loadingKey)) return
        viewModelScope.launch {
            loadingChartBundleKeys.add(loadingKey)
            _isLoadingChartBundle.value = true
            try {
                val bundle = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(12_000) {
                        B3NetworkService.fetchAssetChartBundle(clean, normalizedRange)
                    }
                }
                if (bundle != null) {
                    val current = _assetChartBundles.value.toMutableMap()
                    current[clean] = bundle
                    _assetChartBundles.value = current
                }
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Error loading asset chart bundle", e)
            } finally {
                loadingChartBundleKeys.remove(loadingKey)
                _isLoadingChartBundle.value = loadingChartBundleKeys.isNotEmpty()
            }
        }
    }

    fun searchAndAnalyzeAsset(ticker: String) {
        val clean = ticker.trim().uppercase()
        if (clean.isEmpty()) return

        val previousTicker = _searchQueryResult.value?.ticker?.uppercase(java.util.Locale.ROOT)
        if (previousTicker != null && previousTicker != clean) {
            _searchQueryResult.value = null
            _searchQueryHistory.value = emptyList()
            _searchQueryNews.value = emptyList()
        }

        if (clean == lastSearchTicker && isFresh(lastSearchAt, ASSET_SEARCH_SOFT_TTL_MS) && _searchQueryResult.value?.ticker.equals(clean, ignoreCase = true)) {
            loadAssetChartBundle(clean, _searchQueryRange.value)
            return
        }

        searchAssetJob?.cancel()
        searchAssetJob = viewModelScope.launch {
            _isSearchingAsset.value = true
            lastSearchTicker = clean
            try {
                val info = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(15_000) { runCatching { B3NetworkService.fetchAssetData(clean) }.getOrNull() }
                }
                if (!isActive || lastSearchTicker != clean) return@launch
                _searchQueryResult.value = info

                if (info != null) {
                    // Fetch historic chart point & dedicated news for selected asset in parallel, but with bounded waits.
                    val normalizedRange = _searchQueryRange.value.ifBlank { "1Y" }
                    val (history, news) = coroutineScope {
                        val historyDeferred = async(Dispatchers.IO) {
                            withTimeoutOrNull(7_000) { runCatching { B3NetworkService.fetchHistoricalChart(clean, normalizedRange) }.getOrDefault(emptyList()) }.orEmpty()
                        }
                        val newsDeferred = async(Dispatchers.IO) {
                            withTimeoutOrNull(5_000) { runCatching { B3NetworkService.fetchNews(clean) }.getOrDefault(emptyList()) }.orEmpty()
                        }
                        historyDeferred.await() to newsDeferred.await()
                    }
                    if (!isActive || lastSearchTicker != clean) return@launch
                    _searchQueryHistory.value = history
                    _searchQueryNews.value = news
                    loadAssetChartBundle(clean, normalizedRange)
                    lastSearchAt = System.currentTimeMillis()
                } else {
                    _searchQueryHistory.value = emptyList()
                    _searchQueryNews.value = emptyList()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Erro ao analisar ativo $clean", e)
                if (lastSearchTicker == clean) {
                    _searchQueryResult.value = null
                    _searchQueryHistory.value = emptyList()
                    _searchQueryNews.value = emptyList()
                }
            } finally {
                if (lastSearchTicker == clean) _isSearchingAsset.value = false
            }
        }
    }

    fun changeSearchChartRange(range: String) {
        val ticker = searchQueryResult.value?.ticker ?: return
        val normalized = normalizeAssetRange(range)
        _searchQueryRange.value = normalized

        chartRangeJob?.cancel()
        chartRangeJob = viewModelScope.launch {
            try {
                val history = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(7_000) { runCatching { B3NetworkService.fetchHistoricalChart(ticker, normalized) }.getOrDefault(emptyList()) }.orEmpty()
                }
                if (!isActive || !_searchQueryRange.value.equals(normalized, ignoreCase = true)) return@launch
                _searchQueryHistory.value = history
                loadAssetChartBundle(ticker, normalized)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Erro ao trocar período do gráfico de $ticker", e)
            }
        }
    }

    fun refreshPortfolioAnalytics(force: Boolean = true) {
        val summaries = assetSummaries.value
        val txs = transactions.value
        if (summaries.isEmpty()) {
            refreshLiveMarketRankings(force = force)
            return
        }
        viewModelScope.launch {
            refreshPortfolioAnalytics(summaries, txs, force = force)
        }
    }

    private suspend fun refreshPortfolioAnalytics(
        summaries: List<AssetSummary>,
        txs: List<Transaction>,
        force: Boolean = false
    ) {
        if (!force && _portfolioAnalytics.value.isLoading) return
        _portfolioAnalytics.value = _portfolioAnalytics.value.copy(isLoading = true)
        val firstPurchaseByTicker = txs
            .filter { !it.isSell && it.quantity > 0.0 }
            .groupBy { it.ticker.trim().uppercase(java.util.Locale.ROOT) }
            .mapValues { (_, list) -> list.minOfOrNull { it.date } ?: 0L }
        val positions = summaries.map { summary ->
            val key = summary.ticker.trim().uppercase(java.util.Locale.ROOT)
            PortfolioProxyPosition(
                ticker = summary.ticker,
                quantity = summary.sharesCount,
                averagePrice = summary.averageCost,
                type = summary.type,
                currentPrice = summary.currentPrice,
                totalInvested = summary.totalInvested,
                firstPurchaseAt = firstPurchaseByTicker[key] ?: 0L
            )
        }.filter { it.quantity > 0.0 }

        val analyticsSignature = positions.joinToString("|") { p ->
            "${p.ticker}:${p.quantity}:${p.averagePrice}:${p.currentPrice}:${p.totalInvested}:${p.firstPurchaseAt}"
        } + "#tx=${txs.size}#max=${txs.maxOfOrNull { it.date } ?: 0L}"
        if (!force && analyticsSignature == lastPortfolioAnalyticsSignature && isFresh(lastPortfolioAnalyticsRefreshAt, PORTFOLIO_ANALYTICS_SOFT_TTL_MS)) {
            _portfolioAnalytics.value = _portfolioAnalytics.value.copy(isLoading = false)
            return
        }
        lastPortfolioAnalyticsSignature = analyticsSignature
        lastPortfolioAnalyticsRefreshAt = System.currentTimeMillis()

        val result = try {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val analysisDeferred = async { withTimeoutOrNull(8_000) { runCatching { B3NetworkService.fetchPortfolioAnalysis(positions) }.getOrNull() } }
                    val historyDeferred = async { withTimeoutOrNull(8_000) { runCatching { B3NetworkService.fetchPortfolioHistory(positions, "1Y") }.getOrDefault(emptyList()) }.orEmpty() }
                    val ipcaDeferred = async { withTimeoutOrNull(4_500) { runCatching { B3NetworkService.fetchIpcaSeries(12) }.getOrDefault(emptyList()) }.orEmpty() }
                    val dividendsDeferred = async { withTimeoutOrNull(20_000) { runCatching { B3NetworkService.fetchNextDividends(positions) }.getOrDefault(emptyList()) }.orEmpty() }
                    val portfolioRankingDeferred = async { withTimeoutOrNull(6_000) { runCatching { B3NetworkService.fetchPortfolioRankings(positions) }.getOrNull() } }

                    val remoteAnalysis = analysisDeferred.await()
                    val remoteHistory = historyDeferred.await()
                    val remoteIpca = ipcaDeferred.await()
                    val remoteDividends = dividendsDeferred.await()
                    val remotePortfolioRanking = portfolioRankingDeferred.await()

                    val firstPortfolioMillis = firstPortfolioPurchaseMillis(txs)
                    val localHistory = buildLocalPortfolioHistory(txs, summaries)
                    val ageAdjustedHistory = normalizePortfolioHistoryForAge(remoteHistory, firstPortfolioMillis).ifEmpty { localHistory }
                    val ageMonths = portfolioAgeMonths(firstPortfolioMillis).coerceIn(1, 120)
                    val ageAdjustedIpca = normalizeIpcaForPortfolioAge(
                        points = remoteIpca,
                        firstPortfolioMillis = firstPortfolioMillis,
                        targetMonths = ageAdjustedHistory.size.coerceAtLeast(ageMonths)
                    ).ifEmpty { buildIpcaFallbackSeries(ageMonths) }
                    val eligibleDividends = sanitizeDividendEventsForPortfolio(remoteDividends, txs, summaries)
                    val currentMarketState = _portfolioAnalytics.value

                    PortfolioAnalyticsState(
                        isLoading = false,
                        analysis = remoteAnalysis ?: buildLocalPortfolioAnalysis(summaries),
                        portfolioHistory = ageAdjustedHistory,
                        ipcaSeries = ageAdjustedIpca,
                        dividendEvents = eligibleDividends,
                        portfolioRanking = remotePortfolioRanking ?: currentMarketState.portfolioRanking,
                        liveMarketRanking = currentMarketState.liveMarketRanking,
                        stockMarketRanking = currentMarketState.stockMarketRanking,
                        fiiMarketRanking = currentMarketState.fiiMarketRanking,
                        marketRankingsAttempted = currentMarketState.marketRankingsAttempted,
                        source = if (remoteAnalysis != null || remoteHistory.isNotEmpty() || remoteIpca.isNotEmpty() || remoteDividends.isNotEmpty() || remotePortfolioRanking != null) "Dados VALORAE + carteira local ajustada" else "Carteira local + indicadores disponíveis",
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PortfolioViewModel", "Erro ao atualizar analytics da carteira", e)
            val currentMarketState = _portfolioAnalytics.value
            PortfolioAnalyticsState(
                isLoading = false,
                analysis = buildLocalPortfolioAnalysis(summaries),
                portfolioHistory = buildLocalPortfolioHistory(txs, summaries),
                ipcaSeries = buildIpcaFallbackSeries(portfolioAgeMonths(firstPortfolioPurchaseMillis(txs))),
                dividendEvents = emptyList(),
                portfolioRanking = currentMarketState.portfolioRanking,
                liveMarketRanking = currentMarketState.liveMarketRanking,
                stockMarketRanking = currentMarketState.stockMarketRanking,
                fiiMarketRanking = currentMarketState.fiiMarketRanking,
                marketRankingsAttempted = currentMarketState.marketRankingsAttempted,
                source = "Carteira local + fallback seguro",
                lastUpdated = System.currentTimeMillis()
            )
        }
        _portfolioAnalytics.value = result
    }

    private fun firstPortfolioPurchaseMillis(txs: List<Transaction>): Long {
        return txs.filter { !it.isSell && it.quantity > 0.0 }.minOfOrNull { it.date }
            ?: txs.minOfOrNull { it.date }
            ?: System.currentTimeMillis()
    }

    private fun portfolioMonthStartMillis(firstMillis: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = firstMillis
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun portfolioAgeMonths(firstMillis: Long): Int {
        val start = java.util.Calendar.getInstance().apply { timeInMillis = portfolioMonthStartMillis(firstMillis) }
        val end = java.util.Calendar.getInstance()
        val months = (end.get(java.util.Calendar.YEAR) - start.get(java.util.Calendar.YEAR)) * 12 +
            (end.get(java.util.Calendar.MONTH) - start.get(java.util.Calendar.MONTH)) + 1
        return months.coerceAtLeast(1)
    }

    private fun pointMillisFromProxyTimestamp(timestamp: Long): Long {
        if (timestamp <= 0L) return 0L
        return if (timestamp > 10_000_000_000L) timestamp else timestamp * 1000L
    }

    private fun normalizePortfolioHistoryForAge(
        points: List<PortfolioHistoryPoint>,
        firstPortfolioMillis: Long
    ): List<PortfolioHistoryPoint> {
        if (points.isEmpty()) return emptyList()
        val start = portfolioMonthStartMillis(firstPortfolioMillis)
        return points
            .filter { pointMillisFromProxyTimestamp(it.timestamp) >= start }
            .sortedBy { pointMillisFromProxyTimestamp(it.timestamp) }
            .takeLast(60)
    }

    private fun normalizeIpcaForPortfolioAge(
        points: List<IpcaPoint>,
        firstPortfolioMillis: Long,
        targetMonths: Int
    ): List<IpcaPoint> {
        if (points.isEmpty()) return emptyList()
        val start = portfolioMonthStartMillis(firstPortfolioMillis)
        val filtered = points
            .filter { pointMillisFromProxyTimestamp(it.timestamp) >= start }
            .sortedBy { pointMillisFromProxyTimestamp(it.timestamp) }
            .takeLast(targetMonths.coerceIn(1, 120))
        if (filtered.isEmpty()) return emptyList()

        val hasMonthly = filtered.any { it.monthlyPercent != 0.0 }
        if (hasMonthly) {
            var accumulated = 0.0
            return filtered.map { point ->
                val monthly = point.monthlyPercent
                accumulated = (((1.0 + accumulated / 100.0) * (1.0 + monthly / 100.0)) - 1.0) * 100.0
                point.copy(accumulatedPercent = accumulated)
            }
        }

        val base = filtered.firstOrNull()?.accumulatedPercent ?: 0.0
        return filtered.map { point ->
            point.copy(accumulatedPercent = (point.accumulatedPercent - base).coerceAtLeast(0.0))
        }
    }

    private fun parsePortfolioDateMillis(value: String): Long {
        if (value.isBlank()) return 0L
        val raw = value.trim()
        raw.toLongOrNull()?.let { return if (it > 10_000_000_000L) it else it * 1000L }
        val normalized = raw
            .replace(Regex("\\s+"), " ")
            .replace("às", " ", ignoreCase = true)
            .trim()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy",
            "dd/MM/yy",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "MM/yyyy",
            "yyyy-MM"
        )
        for (pattern in patterns) {
            try {
                val locale = if (pattern.startsWith("yyyy")) java.util.Locale.US else java.util.Locale("pt", "BR")
                val sdf = java.text.SimpleDateFormat(pattern, locale)
                sdf.isLenient = false
                if (pattern.contains("'Z'") || pattern.contains("XXX")) {
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                return sdf.parse(normalized)?.time ?: 0L
            } catch (_: Exception) {}
        }
        return 0L
    }

    private fun startOfDayMillis(millis: Long): Long {
        if (millis <= 0L) return 0L
        return java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun endOfDayMillis(millis: Long): Long {
        if (millis <= 0L) return 0L
        return java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private fun sharesOwnedAt(txs: List<Transaction>, ticker: String, millis: Long): Double {
        val key = ticker.trim().uppercase(java.util.Locale.ROOT)
        if (key.isBlank()) return 0.0
        return txs
            .asSequence()
            .filter { it.ticker.trim().uppercase(java.util.Locale.ROOT) == key && it.date <= millis }
            .fold(0.0) { acc, tx -> if (tx.isSell) acc - tx.quantity else acc + tx.quantity }
            .coerceAtLeast(0.0)
    }

    private fun sanitizeDividendEventsForPortfolio(
        events: List<DividendEvent>,
        txs: List<Transaction>,
        summaries: List<AssetSummary>
    ): List<DividendEvent> {
        val currentQty = summaries.associateBy({ it.ticker.trim().uppercase(java.util.Locale.ROOT) }, { it.sharesCount })
        val todayStart = startOfDayMillis(System.currentTimeMillis())
        return events.mapNotNull { event ->
            val ticker = event.ticker.trim().uppercase(java.util.Locale.ROOT)
            if (ticker.isBlank()) return@mapNotNull null

            val comDateMillis = parsePortfolioDateMillis(event.dateCom).takeIf { it > 0L }
            val paymentDateMillis = parsePortfolioDateMillis(event.paymentDate).takeIf { it > 0L }
            val eligibilityMillis = comDateMillis ?: paymentDateMillis
            val relevantMillis = paymentDateMillis ?: comDateMillis
            val isFutureOrProvisioned = relevantMillis == null || relevantMillis >= todayStart

            val ownedAtEligibility = eligibilityMillis?.let { sharesOwnedAt(txs, ticker, endOfDayMillis(it)) } ?: 0.0
            val currentShares = currentQty[ticker] ?: 0.0
            val eligibleShares = when {
                ownedAtEligibility > 0.0001 -> ownedAtEligibility
                isFutureOrProvisioned && currentShares > 0.0001 -> currentShares
                isFutureOrProvisioned && event.quantity > 0.0001 -> event.quantity
                else -> 0.0
            }.coerceAtLeast(0.0)

            val hasRealEventMarker = event.dateCom.isNotBlank() || event.paymentDate.isNotBlank() || event.source.isNotBlank()
            if (eligibleShares <= 0.0001 && !isFutureOrProvisioned) return@mapNotNull null
            if (eligibleShares <= 0.0001 && !hasRealEventMarker) return@mapNotNull null

            val amountFromUnit = if (event.valuePerShare > 0.0 && eligibleShares > 0.0) event.valuePerShare * eligibleShares else 0.0
            val proratedAmount = if (amountFromUnit > 0.0) amountFromUnit
                else if (event.estimatedAmount > 0.0 && event.quantity > 0.0 && eligibleShares > 0.0) event.estimatedAmount * (eligibleShares / event.quantity)
                else if (isFutureOrProvisioned) event.estimatedAmount
                else 0.0
            if (proratedAmount <= 0.0 && event.valuePerShare <= 0.0 && !hasRealEventMarker) return@mapNotNull null

            event.copy(
                ticker = ticker,
                quantity = if (eligibleShares > 0.0) eligibleShares else event.quantity,
                estimatedAmount = proratedAmount.coerceAtLeast(0.0),
                status = event.status.ifBlank { if (isFutureOrProvisioned) "Previsto" else "Recebido" }
            )
        }.distinctBy { listOf(it.ticker.trim().uppercase(java.util.Locale.ROOT), it.dateCom, it.paymentDate, it.valuePerShare).joinToString("|") }
    }

    private fun buildLocalPortfolioAnalysis(summaries: List<AssetSummary>): PortfolioProxyAnalysis {
        val total = summaries.sumOf { it.totalCurrentValue }.coerceAtLeast(0.0)
        val totalInvested = summaries.sumOf { it.totalInvested }.coerceAtLeast(0.0)
        val top = summaries.maxByOrNull { it.totalCurrentValue }
        val topWeight = if (total > 0.0 && top != null) top.totalCurrentValue / total * 100.0 else 0.0
        val avgDy = if (total > 0.0) summaries.sumOf { it.totalCurrentValue * it.dividendYield } / total else 0.0
        val returnPct = if (totalInvested > 0.0) (total - totalInvested) / totalInvested * 100.0 else 0.0
        val classAlloc = summaries.groupBy { if (it.type.uppercase() == "FII") "FIIs" else "Ações" }
            .mapValues { (_, list) -> if (total > 0.0) list.sumOf { it.totalCurrentValue } / total * 100.0 else 0.0 }
            .toList()
            .filter { it.second > 0.0 }
        val sectorAlloc = summaries.groupBy { if (it.type.uppercase() == "FII") "Fundos Imobiliários" else "Ações B3" }
            .mapValues { (_, list) -> if (total > 0.0) list.sumOf { it.totalCurrentValue } / total * 100.0 else 0.0 }
            .toList()
            .filter { it.second > 0.0 }
        val concentrationPenalty = when {
            topWeight > 60.0 -> 22.0
            topWeight > 40.0 -> 12.0
            else -> 4.0
        }
        val score = (70.0 + returnPct.coerceIn(-20.0, 20.0) / 2.0 + avgDy.coerceIn(0.0, 12.0) - concentrationPenalty).coerceIn(0.0, 100.0)
        val warnings = mutableListOf<String>()
        if (topWeight > 40.0 && top != null) warnings.add("Concentração relevante em ${top.ticker}: ${String.format("%.1f", topWeight)}% da carteira.")
        if (avgDy <= 0.0) warnings.add("Dividend Yield médio ainda não disponível para a carteira.")
        return PortfolioProxyAnalysis(
            score = score,
            riskLabel = when {
                topWeight > 60.0 -> "Alto por concentração"
                topWeight > 40.0 -> "Moderado"
                else -> "Controlado"
            },
            diversificationLabel = when {
                summaries.size >= 10 && topWeight < 25.0 -> "Bem diversificada"
                summaries.size >= 5 && topWeight < 40.0 -> "Diversificação moderada"
                else -> "Concentração elevada"
            },
            concentrationPercent = topWeight,
            topHolding = top?.ticker ?: "",
            monthlyDividendEstimate = total * (avgDy / 100.0) / 12.0,
            annualDividendEstimate = total * (avgDy / 100.0),
            dataQuality = 75.0,
            allocationByClass = classAlloc,
            allocationBySector = sectorAlloc,
            warnings = warnings,
            source = "Calculado pela carteira local"
        )
    }

    private data class LocalPositionSnapshot(
        var shares: Double = 0.0,
        var costBasis: Double = 0.0
    )

    private fun buildLocalPortfolioHistory(txs: List<Transaction>, summaries: List<AssetSummary>): List<PortfolioHistoryPoint> {
        if (txs.isEmpty()) return emptyList()
        val first = txs.filter { !it.isSell && it.quantity > 0.0 }.minOfOrNull { it.date }
            ?: txs.minOfOrNull { it.date }
            ?: return emptyList()
        val now = System.currentTimeMillis()
        val start = java.util.Calendar.getInstance().apply {
            timeInMillis = first
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val end = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val currentPriceByTicker = summaries.associateBy({ it.ticker.uppercase() }, { it.currentPrice })
        val averagePriceByTicker = summaries.associateBy({ it.ticker.uppercase() }, { it.averageCost })
        val out = mutableListOf<PortfolioHistoryPoint>()
        val cursor = start.clone() as java.util.Calendar
        var guard = 0
        while (!cursor.after(end) && guard < 120) {
            val monthEnd = (cursor.clone() as java.util.Calendar).apply {
                set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                set(java.util.Calendar.HOUR_OF_DAY, 23)
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }.timeInMillis

            val positions = linkedMapOf<String, LocalPositionSnapshot>()
            txs.filter { it.date <= monthEnd }.sortedBy { it.date }.forEach { tx ->
                val key = tx.ticker.uppercase()
                val snapshot = positions.getOrPut(key) { LocalPositionSnapshot() }
                val qty = tx.quantity.takeIf { it.isFinite() && it > 0.0 } ?: return@forEach
                val price = tx.purchasePrice.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                if (!tx.isSell) {
                    snapshot.shares += qty
                    snapshot.costBasis += qty * price
                } else if (snapshot.shares > 0.0) {
                    val sold = qty.coerceAtMost(snapshot.shares)
                    val avgCost = if (snapshot.shares > 0.0) snapshot.costBasis / snapshot.shares else 0.0
                    snapshot.shares -= sold
                    snapshot.costBasis -= sold * avgCost
                    if (snapshot.shares <= 0.0001) {
                        snapshot.shares = 0.0
                        snapshot.costBasis = 0.0
                    }
                }
            }

            val invested = positions.values.sumOf { it.costBasis.coerceAtLeast(0.0) }
            val markedValue = positions.entries.sumOf { (ticker, pos) ->
                val price = currentPriceByTicker[ticker]
                    ?: averagePriceByTicker[ticker]
                    ?: if (pos.shares > 0.0) pos.costBasis / pos.shares else 0.0
                pos.shares.coerceAtLeast(0.0) * price
            }
            if (invested > 0.0 || markedValue > 0.0) {
                val ret = if (invested > 0.0) (markedValue - invested) / invested * 100.0 else 0.0
                out.add(
                    PortfolioHistoryPoint(
                        timestamp = cursor.timeInMillis / 1000L,
                        dateLabel = java.text.SimpleDateFormat("MM/yy", java.util.Locale.getDefault()).format(cursor.time),
                        totalValue = markedValue,
                        investedValue = invested,
                        returnPercent = ret,
                        source = "Carteira local"
                    )
                )
            }
            cursor.add(java.util.Calendar.MONTH, 1)
            guard++
        }
        return out.takeLast(60)
    }

    private fun buildIpcaFallbackSeries(months: Int): List<IpcaPoint> {
        // Fallback transparente: usado apenas quando o Proxy ainda não entrega IPCA.
        // Mantém a tela funcional sem apresentar a estimativa como dado oficial.
        val annualEstimate = 5.5
        val cal = java.util.Calendar.getInstance()
        return List(months.coerceIn(1, 60)) { index ->
            val monthOffset = months - index - 1
            val c = cal.clone() as java.util.Calendar
            c.add(java.util.Calendar.MONTH, -monthOffset)
            val accumulated = annualEstimate * ((index + 1).toDouble() / months.toDouble())
            IpcaPoint(
                timestamp = c.timeInMillis / 1000L,
                dateLabel = java.text.SimpleDateFormat("MM/yy", java.util.Locale.getDefault()).format(c.time),
                accumulatedPercent = accumulated,
                monthlyPercent = annualEstimate / 12.0,
                source = "Estimativa local transparente"
            )
        }
    }

    // --- DATA BACKUP & SHEET IMPORT UTILITIES ---

    fun exportTransactionsToJson(): String {
        val arr = JSONArray()
        transactions.value.forEach { tx ->
            arr.put(
                JSONObject()
                    .put("ticker", tx.ticker)
                    .put("name", tx.name)
                    .put("quantity", tx.quantity)
                    .put("purchasePrice", tx.purchasePrice)
                    .put("date", tx.date)
                    .put("type", tx.type)
                    .put("isSell", tx.isSell)
                    .put("broker", tx.broker)
                    .put("sector", tx.sector)
                    .put("notes", tx.notes)
            )
        }
        return arr.toString(2)
    }


    fun exportTransactionsToCsv(): String {
        val txs = transactions.value
        val sb = java.lang.StringBuilder()
        sb.append("Código;Nome;Quantidade;Preço Unitário;Tipo;Operação;Corretora;Setor;Data\n")
        txs.forEach { tx ->
            val opStr = if (tx.isSell) "Venda" else "Compra"
            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(tx.date))
            val cleanName = tx.name.replace(";", " ")
            val cleanBroker = tx.broker.replace(";", " ")
            val cleanSector = tx.sector.replace(";", " ")
            sb.append("${tx.ticker};$cleanName;${tx.quantity};${tx.purchasePrice};${tx.type};$opStr;$cleanBroker;$cleanSector;$dateStr\n")
        }
        return sb.toString()
    }

    private fun parseImportedNumber(value: Any?): Double {
        if (value == null || value == JSONObject.NULL) return 0.0
        if (value is Number) return value.toDouble().takeIf { it.isFinite() } ?: 0.0
        var raw = value.toString().trim()
        if (raw.isBlank() || raw == "--" || raw == "-") return 0.0
        raw = raw
            .replace("R$", "", ignoreCase = true)
            .replace("%", "")
            .replace("−", "-")
            .trim()
        var clean = raw.replace(Regex("[^0-9,.-]"), "")
        if (clean.isBlank() || clean == "-" || clean == "." || clean == ",") return 0.0
        val lastComma = clean.lastIndexOf(',')
        val lastDot = clean.lastIndexOf('.')
        clean = when {
            lastComma >= 0 && lastDot >= 0 && lastComma > lastDot -> clean.replace(".", "").replace(',', '.')
            lastComma >= 0 && lastDot >= 0 -> clean.replace(",", "")
            lastComma >= 0 && clean.indexOf(',') != lastComma -> clean.replace(",", "")
            lastComma >= 0 -> clean.replace(".", "").replace(',', '.')
            lastDot >= 0 && clean.indexOf('.') != lastDot -> clean.replace(".", "")
            else -> clean
        }
        return clean.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
    }

    private fun excelSerialDateToMillis(serial: Double): Long {
        if (!serial.isFinite() || serial < 20_000.0 || serial > 80_000.0) return 0L
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(java.util.Calendar.YEAR, 1899)
            set(java.util.Calendar.MONTH, java.util.Calendar.DECEMBER)
            set(java.util.Calendar.DAY_OF_MONTH, 30)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis + (serial * 86_400_000L).toLong()
    }

    private fun parseImportedDateMillis(value: Any?): Long {
        if (value == null || value == JSONObject.NULL) return 0L
        if (value is Number) {
            val n = value.toDouble()
            if (!n.isFinite() || n <= 0.0) return 0L
            if (n > 10_000_000_000L) return n.toLong()
            if (n > 1_000_000_000L) return (n * 1000L).toLong()
            excelSerialDateToMillis(n).takeIf { it > 0L }?.let { return it }
        }
        val raw = value.toString().trim()
        if (raw.isBlank()) return 0L
        raw.toLongOrNull()?.let { n ->
            return when {
                n in 20_000L..80_000L -> excelSerialDateToMillis(n.toDouble())
                n > 10_000_000_000L -> n
                n > 1_000_000_000L -> n * 1000L
                else -> 0L
            }
        }
        raw.replace(',', '.').toDoubleOrNull()?.let { excelSerialDateToMillis(it).takeIf { ts -> ts > 0L }?.let { ts -> return ts } }
        val normalized = raw.replace("às", " ", ignoreCase = true).replace(Regex("\\s+"), " ").trim()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy",
            "dd/MM/yy",
            "dd-MM-yyyy",
            "yyyy/MM/dd"
        )
        for (pattern in patterns) {
            try {
                val locale = if (pattern.startsWith("yyyy")) java.util.Locale.US else java.util.Locale("pt", "BR")
                val sdf = java.text.SimpleDateFormat(pattern, locale).apply {
                    isLenient = false
                    if (pattern.contains("'Z'") || pattern.contains("XXX")) timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(normalized)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return 0L
    }

    fun importTransactionsFromJson(json: String, clearExisting: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsedList = mutableListOf<Transaction>()
                val trimmed = json.trim()
                val arr = when {
                    trimmed.startsWith("[") -> JSONArray(trimmed)
                    trimmed.startsWith("{") -> {
                        val obj = JSONObject(trimmed)
                        obj.optJSONArray("transactions")
                            ?: obj.optJSONArray("items")
                            ?: obj.optJSONArray("movements")
                            ?: obj.optJSONArray("movimentacoes")
                            ?: JSONArray().put(obj)
                    }
                    else -> JSONArray()
                }

                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val ticker = listOf(
                        item.optString("ticker", ""),
                        item.optString("symbol", ""),
                        item.optString("ativo", ""),
                        item.optString("codigo", ""),
                        item.optString("código", "")
                    ).firstOrNull { it.isNotBlank() }?.trim()?.uppercase(java.util.Locale.ROOT)?.replace(".SA", "") ?: ""
                    if (ticker.isBlank()) continue
                    val quantity = parseImportedNumber(item.opt("quantity").takeUnless { it == null || it == JSONObject.NULL } ?: item.opt("quantidade") ?: item.opt("qtd") ?: item.opt("shares"))
                    if (quantity <= 0.0) continue
                    val purchasePrice = parseImportedNumber(item.opt("purchasePrice").takeUnless { it == null || it == JSONObject.NULL } ?: item.opt("price") ?: item.opt("preco") ?: item.opt("preço") ?: item.opt("valorUnitario") ?: item.opt("valor_unitario"))
                    val parsedDate = parseImportedDateMillis(item.opt("date").takeUnless { it == null || it == JSONObject.NULL } ?: item.opt("data") ?: item.opt("tradeDate") ?: item.opt("dateStr"))
                    val operationText = listOf("operation", "operacao", "operação", "movimentacao", "movimentação", "side", "tipoOperacao", "tipo")
                        .mapNotNull { key -> item.optString(key, "").takeIf { it.isNotBlank() } }
                        .joinToString(" ")
                        .lowercase(java.util.Locale.ROOT)
                    val isSell = item.optBoolean("isSell", false) || operationText.contains("venda") || operationText == "v" || operationText.contains("sell")
                    val type = item.optString("type", if (B3NetworkService.inferIsFii(ticker)) "FII" else "ACAO").uppercase(java.util.Locale.ROOT)
                    parsedList.add(
                        Transaction(
                            ticker = ticker,
                            name = item.optString("name", item.optString("nome", ticker)),
                            quantity = quantity,
                            purchasePrice = purchasePrice,
                            date = if (parsedDate > 0L) parsedDate else System.currentTimeMillis(),
                            type = if (type == "FII") "FII" else "ACAO",
                            isSell = isSell,
                            broker = item.optString("broker", item.optString("corretora", "")),
                            sector = item.optString("sector", item.optString("setor", "")),
                            notes = item.optString("notes", item.optString("observacoes", ""))
                        )
                    )
                }

                if (parsedList.isNotEmpty()) {
                    if (clearExisting) {
                        repository.deleteAllTransactions()
                    }
                    repository.insertAll(parsedList)
                    repository.insertNotification(
                        DbNotification(
                            title = "Backup Restaurado",
                            description = "Seu código de cópia de segurança foi carregado com sucesso, incluindo ${parsedList.size} movimentações.",
                            category = "SISTEMA",
                            date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                            iconName = "UPDATE",
                            isRead = false
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Erro ao importar backup JSON", e)
            }
        }
    }

    fun importFromB3Spreadsheet(text: String, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            var count = 0
            try {
                val lines = text.split("\n")
                val listToInsert = mutableListOf<Transaction>()
                
                var colTicker = -1
                var colQty = -1
                var colPrice = -1
                var colType = -1
                var colDate = -1
                var colBroker = -1

                for (line in lines) {
                    val cleanLine = line.trim()
                    if (cleanLine.isEmpty()) continue
                    
                    val parts = if (cleanLine.contains("\t")) {
                        cleanLine.split("\t")
                    } else if (cleanLine.contains(";")) {
                        cleanLine.split(";")
                    } else {
                        cleanLine.split(",")
                    }.map { it.trim() }

                    if (parts.size < 2) continue

                    // Header mapping identification
                    if (colTicker == -1) {
                        for ((idx, part) in parts.withIndex()) {
                            val lower = part.lowercase()
                            if (lower.contains("código") || lower.contains("codigo") || lower.contains("ticker") || lower.contains("ativo") || lower.contains("papel") || lower == "produto") {
                                colTicker = idx
                            } else if (lower.contains("quantidade") || lower.contains("qtd") || lower.contains("quant")) {
                                colQty = idx
                            } else if (lower.contains("preço") || lower.contains("preco") || lower.contains("valor unit") || lower.contains("p.u.")) {
                                colPrice = idx
                            } else if (lower.contains("movimentação") || lower.contains("movimentacao") || lower.contains("compra") || lower.contains("venda") || lower.contains("operação") || lower.contains("operacao") || lower.contains("tipo")) {
                                colType = idx
                            } else if (lower.contains("data") || lower.contains("negócio") || lower.contains("negocio")) {
                                colDate = idx
                            } else if (lower.contains("instituição") || lower.contains("instituicao") || lower.contains("corretora") || lower.contains("banco")) {
                                colBroker = idx
                            }
                        }
                        // If we found essential columns, we might have just processed the header
                        if (colTicker != -1 && (colQty != -1 || colPrice != -1)) {
                            // Check if this row is actually data or header
                            val firstPart = parts[colTicker].uppercase()
                            if (!firstPart.matches("[A-Z]{4}[0-9]{1,2}[F]?".toRegex())) {
                                continue 
                            }
                        }
                    }

                    try {
                        var ticker = ""
                        var qty = 0.0
                        var price = 0.0
                        var isSale = false
                        var dateMillis = System.currentTimeMillis()
                        var brokerName = "B3 Planilha"

                        val tickerRegex = "[A-Z]{4}[0-9]{1,2}[F]?".toRegex()

                        if (colTicker != -1 && colTicker < parts.size) {
                            val rawTicker = parts[colTicker].uppercase().replace(".SA", "")
                            val match = tickerRegex.find(rawTicker)
                            if (match != null) {
                                ticker = match.value
                            }
                        }
                        
                        if (colQty != -1 && colQty < parts.size) {
                            qty = parseImportedNumber(parts[colQty])
                        }
                        
                        if (colPrice != -1 && colPrice < parts.size) {
                            price = parseImportedNumber(parts[colPrice])
                        }
                        
                        if (colType != -1 && colType < parts.size) {
                            val typeStr = parts[colType].lowercase()
                            if (typeStr.contains("venda") || typeStr == "v") {
                                isSale = true
                            }
                        }
                        
                        if (colDate != -1 && colDate < parts.size) {
                            val parsedDate = parseImportedDateMillis(parts[colDate])
                            if (parsedDate > 0L) dateMillis = parsedDate
                        }
                        
                        if (colBroker != -1 && colBroker < parts.size) {
                            val bStr = parts[colBroker]
                            if (bStr.isNotBlank()) {
                                brokerName = bStr
                                if (brokerName.contains(" - ")) {
                                    brokerName = brokerName.substringAfter(" - ").trim()
                                }
                            }
                        }

                        // fallback scanning heuristics if ticker/qty/price missing
                        if (ticker.isEmpty()) {
                            for (p in parts) {
                                val upper = p.uppercase().replace(".SA", "")
                                val match = tickerRegex.find(upper)
                                if (match != null) {
                                    ticker = match.value
                                    break
                                }
                            }
                        }

                        if (qty == 0.0) {
                            for (p in parts) {
                                val d = parseImportedNumber(p)
                                if (d > 0.0 && d % 1.0 == 0.0 && d < 1_000_000.0 && parseImportedDateMillis(p) == 0L) {
                                    qty = d
                                    break
                                }
                            }
                        }

                        if (price == 0.0) {
                            for (p in parts) {
                                val d = parseImportedNumber(p)
                                if (d > 0.0 && d != qty && d < 1_000_000.0 && parseImportedDateMillis(p) == 0L) {
                                    price = d
                                }
                            }
                        }

                        if (ticker.isNotEmpty() && qty > 0.0) {
                            val assetType = if (B3NetworkService.inferIsFii(ticker)) "FII" else "ACAO"
                            listToInsert.add(
                                Transaction(
                                    ticker = ticker,
                                    name = ticker,
                                    quantity = qty,
                                    purchasePrice = price,
                                    type = assetType,
                                    isSell = isSale,
                                    broker = brokerName,
                                    sector = "Planilha",
                                    date = dateMillis
                                )
                            )
                            count++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (listToInsert.isNotEmpty()) {
                    repository.insertAll(listToInsert)
                    repository.insertNotification(
                        DbNotification(
                            title = "Planilha B3 Sincronizada",
                            description = "Seu portfólio foi alimentado automaticamente com $count novas transações extraídas de sua cópia da B3/Excel.",
                            category = "PLANILHA",
                            date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                            iconName = "FILE",
                            isRead = false
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    onComplete(count)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(0)
                }
            }
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllTransactions()
        }
    }
}

class PortfolioViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortfolioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PortfolioViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
