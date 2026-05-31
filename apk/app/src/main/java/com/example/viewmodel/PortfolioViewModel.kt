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

            val currentVal = (currentShares * livePrice).takeIf { it.isFinite() } ?: 0.0
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
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                            sharesBefore -= tx.quantity
                            costBasisBefore -= (tx.quantity * avg)
                        }
                    }
                }
                
                val avgCost = if (sharesBefore > 0) costBasisBefore / sharesBefore else 0.0
                val profit = (sale.purchasePrice - avgCost) * sale.quantity
                
                if (sale.type == "ACAO") {
                    sVolume += (sale.quantity * sale.purchasePrice)
                    sProfit += profit
                } else {
                    fVolume += (sale.quantity * sale.purchasePrice)
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
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioSummary())

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
        
        // Verificação discreta de saúde do Proxy na abertura do app.
        refreshProxyHealth()

        // Initial news feed load
        fetchGlobalNews()

        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            combine(assetSummaries, transactions) { summaries, txs -> summaries to txs }
                .debounce(1800)
                .collect { (summaries, txs) ->
                    if (summaries.isNotEmpty()) {
                        refreshPortfolioAnalytics(summaries, txs)
                    } else {
                        _portfolioAnalytics.value = PortfolioAnalyticsState(source = "Aguardando carteira")
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

    fun refreshProxyHealth() {
        viewModelScope.launch {
            val diagnostics = withContext(Dispatchers.IO) {
                runCatching { B3NetworkService.fetchProxyDiagnosticsSummary() }.getOrNull()
            }
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
                refreshProxyHealth()
                fetchGlobalNews()
                if (assetSummaries.value.isNotEmpty()) {
                    refreshPortfolioAnalytics(assetSummaries.value, transactions.value, force = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Erro ao atualizar dados do app", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun triggerBatchedPriceFetch(tickers: List<String>, showSecondaryLoading: Boolean) {
        if (showSecondaryLoading) _isLoadingPrices.value = true
        try {
            val updated = withContext(Dispatchers.IO) {
                val currentCache = _cachedAssetData.value
                val updatedMap = java.util.concurrent.ConcurrentHashMap<String, B3AssetData>(currentCache)

                val tickersToFetch = tickers
                    .map { it.trim().uppercase() }
                    .distinct()
                    .filter { showSecondaryLoading || !currentCache.containsKey(it) }

                if (tickersToFetch.isNotEmpty()) {
                    val batch = runCatching {
                        B3NetworkService.fetchAssetsData(tickersToFetch, bypassCache = showSecondaryLoading)
                    }.getOrDefault(emptyMap())
                    batch.forEach { (ticker, data) ->
                        updatedMap[ticker] = data
                    }
                }

                updatedMap.toMap()
            }
            _cachedAssetData.value = updated
        } catch (e: Exception) {
            android.util.Log.e("PortfolioViewModel", "Erro ao atualizar cotações em lote", e)
        } finally {
            if (showSecondaryLoading) _isLoadingPrices.value = false
        }
    }

    fun fetchGlobalNews() {
        viewModelScope.launch {
            _isLoadingNews.value = true
            try {
                val news = withContext(Dispatchers.IO) {
                    // If user has tickers, fetch customized news for foremost picker or generic news
                    val userTicker = transactions.value.firstOrNull()?.ticker
                    runCatching { B3NetworkService.fetchNews(userTicker ?: "") }.getOrDefault(emptyList())
                }
                _newsFeed.value = news
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Erro ao carregar notícias", e)
                _newsFeed.value = emptyList()
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
                    B3NetworkService.fetchAssetChartBundle(clean, normalizedRange)
                }
                val current = _assetChartBundles.value.toMutableMap()
                current[clean] = bundle
                _assetChartBundles.value = current
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

        viewModelScope.launch {
            _isSearchingAsset.value = true
            try {
                val info = withContext(Dispatchers.IO) {
                    runCatching { B3NetworkService.fetchAssetData(clean) }.getOrNull()
                }
                _searchQueryResult.value = info

                if (info != null) {
                    // Fetch historic chart point & dedicated news for selected asset in parallel on Dispatchers.IO
                    val (history, news) = coroutineScope {
                        val historyDeferred = async(Dispatchers.IO) {
                            runCatching { B3NetworkService.fetchHistoricalChart(clean, "1y") }.getOrDefault(emptyList())
                        }
                        val newsDeferred = async(Dispatchers.IO) {
                            runCatching { B3NetworkService.fetchNews(clean) }.getOrDefault(emptyList())
                        }
                        historyDeferred.await() to newsDeferred.await()
                    }
                    _searchQueryHistory.value = history
                    _searchQueryNews.value = news
                    loadAssetChartBundle(clean, "1Y")
                } else {
                    _searchQueryHistory.value = emptyList()
                    _searchQueryNews.value = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Erro ao analisar ativo $clean", e)
                _searchQueryResult.value = null
                _searchQueryHistory.value = emptyList()
                _searchQueryNews.value = emptyList()
            } finally {
                _isSearchingAsset.value = false
            }
        }
    }

    fun changeSearchChartRange(range: String) {
        val ticker = searchQueryResult.value?.ticker ?: return
        _searchQueryRange.value = normalizeAssetRange(range)

        viewModelScope.launch {
            try {
                val history = withContext(Dispatchers.IO) {
                    runCatching { B3NetworkService.fetchHistoricalChart(ticker, normalizeAssetRange(range)) }.getOrDefault(emptyList())
                }
                _searchQueryHistory.value = history
                loadAssetChartBundle(ticker, normalizeAssetRange(range))
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Erro ao trocar período do gráfico de $ticker", e)
            }
        }
    }

    fun refreshPortfolioAnalytics(force: Boolean = true) {
        val summaries = assetSummaries.value
        val txs = transactions.value
        if (summaries.isEmpty()) {
            _portfolioAnalytics.value = PortfolioAnalyticsState(source = "Aguardando carteira")
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
        val positions = summaries.map { summary ->
            PortfolioProxyPosition(
                ticker = summary.ticker,
                quantity = summary.sharesCount,
                averagePrice = summary.averageCost,
                type = summary.type,
                currentPrice = summary.currentPrice,
                totalInvested = summary.totalInvested
            )
        }.filter { it.quantity > 0.0 }

        val result = try {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val analysisDeferred = async { runCatching { B3NetworkService.fetchPortfolioAnalysis(positions) }.getOrNull() }
                    val historyDeferred = async { runCatching { B3NetworkService.fetchPortfolioHistory(positions, "1Y") }.getOrDefault(emptyList()) }
                    val ipcaDeferred = async { runCatching { B3NetworkService.fetchIpcaSeries(12) }.getOrDefault(emptyList()) }
                    val dividendsDeferred = async { runCatching { B3NetworkService.fetchNextDividends(positions) }.getOrDefault(emptyList()) }

                    val remoteAnalysis = analysisDeferred.await()
                    val remoteHistory = historyDeferred.await()
                    val remoteIpca = ipcaDeferred.await()
                    val remoteDividends = dividendsDeferred.await()

                    PortfolioAnalyticsState(
                        isLoading = false,
                        analysis = remoteAnalysis ?: buildLocalPortfolioAnalysis(summaries),
                        portfolioHistory = remoteHistory.ifEmpty { buildLocalPortfolioHistory(txs, summaries) },
                        ipcaSeries = remoteIpca.ifEmpty { buildIpcaFallbackSeries(12) },
                        dividendEvents = remoteDividends.ifEmpty { buildLocalDividendEvents(summaries) },
                        source = if (remoteAnalysis != null || remoteHistory.isNotEmpty() || remoteIpca.isNotEmpty() || remoteDividends.isNotEmpty()) "Valorae Proxy + carteira local" else "Carteira local + indicadores disponíveis",
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PortfolioViewModel", "Erro ao atualizar analytics da carteira", e)
            PortfolioAnalyticsState(
                isLoading = false,
                analysis = buildLocalPortfolioAnalysis(summaries),
                portfolioHistory = buildLocalPortfolioHistory(txs, summaries),
                ipcaSeries = buildIpcaFallbackSeries(12),
                dividendEvents = buildLocalDividendEvents(summaries),
                source = "Carteira local + fallback seguro",
                lastUpdated = System.currentTimeMillis()
            )
        }
        _portfolioAnalytics.value = result
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

    private fun buildLocalDividendEvents(summaries: List<AssetSummary>): List<DividendEvent> {
        return summaries.mapNotNull { summary ->
            val estimated = summary.lastDividend * summary.sharesCount
            if (estimated <= 0.0 && summary.nextEarningsDate.isBlank()) return@mapNotNull null
            DividendEvent(
                ticker = summary.ticker,
                dateCom = summary.nextEarningsDate,
                paymentDate = "",
                valuePerShare = summary.lastDividend,
                quantity = summary.sharesCount,
                estimatedAmount = estimated.coerceAtLeast(0.0),
                status = if (summary.nextEarningsDate.isBlank()) "Estimado sem data confirmada" else "Data COM informada",
                source = "Carteira local"
            )
        }.sortedByDescending { it.estimatedAmount }
    }

    private fun buildLocalPortfolioHistory(txs: List<Transaction>, summaries: List<AssetSummary>): List<PortfolioHistoryPoint> {
        if (txs.isEmpty()) return emptyList()
        val first = txs.minOfOrNull { it.date } ?: return emptyList()
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
        val out = mutableListOf<PortfolioHistoryPoint>()
        val cursor = start.clone() as java.util.Calendar
        var guard = 0
        while (!cursor.after(end) && guard < 60) {
            val monthEnd = (cursor.clone() as java.util.Calendar).apply {
                set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                set(java.util.Calendar.HOUR_OF_DAY, 23)
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
            }.timeInMillis
            var invested = 0.0
            val quantities = linkedMapOf<String, Double>()
            txs.filter { it.date <= monthEnd }.sortedBy { it.date }.forEach { tx ->
                val key = tx.ticker.uppercase()
                if (tx.isSell) {
                    quantities[key] = (quantities[key] ?: 0.0) - tx.quantity
                    invested -= tx.quantity * tx.purchasePrice
                } else {
                    quantities[key] = (quantities[key] ?: 0.0) + tx.quantity
                    invested += tx.quantity * tx.purchasePrice
                }
            }
            invested = invested.coerceAtLeast(0.0)
            val markedValue = quantities.entries.sumOf { (ticker, qty) ->
                val price = currentPriceByTicker[ticker] ?: summaries.firstOrNull { it.ticker.uppercase() == ticker }?.averageCost ?: 0.0
                qty.coerceAtLeast(0.0) * price
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
        return out.takeLast(24)
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

    fun importTransactionsFromJson(json: String, clearExisting: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsedList = mutableListOf<Transaction>()
                val trimmed = json.trim()
                val arr = when {
                    trimmed.startsWith("[") -> JSONArray(trimmed)
                    trimmed.startsWith("{") -> {
                        val obj = JSONObject(trimmed)
                        obj.optJSONArray("transactions") ?: obj.optJSONArray("items") ?: JSONArray().put(obj)
                    }
                    else -> JSONArray()
                }

                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val ticker = item.optString("ticker", "").trim().uppercase()
                    if (ticker.isBlank()) continue
                    val quantity = item.optDouble("quantity", Double.NaN).takeIf { it.isFinite() } ?: continue
                    val purchasePrice = item.optDouble("purchasePrice", item.optDouble("price", 0.0)).takeIf { it.isFinite() } ?: 0.0
                    val date = item.optLong("date", System.currentTimeMillis())
                    val type = item.optString("type", if (B3NetworkService.inferIsFii(ticker)) "FII" else "ACAO")
                    parsedList.add(
                        Transaction(
                            ticker = ticker,
                            name = item.optString("name", ticker),
                            quantity = quantity,
                            purchasePrice = purchasePrice,
                            date = date,
                            type = type,
                            isSell = item.optBoolean("isSell", false),
                            broker = item.optString("broker", ""),
                            sector = item.optString("sector", ""),
                            notes = item.optString("notes", "")
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
                            val str = parts[colQty]
                            qty = if (str.contains(",")) str.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0 else str.toDoubleOrNull() ?: 0.0
                        }
                        
                        if (colPrice != -1 && colPrice < parts.size) {
                            val str = parts[colPrice]
                            price = if (str.contains(",")) str.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0 else str.toDoubleOrNull() ?: 0.0
                        }
                        
                        if (colType != -1 && colType < parts.size) {
                            val typeStr = parts[colType].lowercase()
                            if (typeStr.contains("venda") || typeStr == "v") {
                                isSale = true
                            }
                        }
                        
                        if (colDate != -1 && colDate < parts.size) {
                            val dateStr = parts[colDate]
                            try {
                                val formats = listOf("dd/MM/yyyy", "yyyy-MM-dd", "dd/MM/yy")
                                for (fmt in formats) {
                                    try {
                                        val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault())
                                        val parsed = sdf.parse(dateStr)
                                        if (parsed != null) {
                                            dateMillis = parsed.time
                                            break
                                        }
                                    } catch (e: Exception) {}
                                }
                            } catch (e: Exception) {}
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
                                val d = if (p.contains(",")) p.replace(".", "").replace(",", ".").toDoubleOrNull() else p.toDoubleOrNull()
                                if (d != null && d > 0.0 && d % 1.0 == 0.0 && d < 1000000) {
                                    qty = d
                                    break
                                }
                            }
                        }

                        if (price == 0.0) {
                            for (p in parts) {
                                val d = if (p.contains(",")) p.replace(".", "").replace(",", ".").toDoubleOrNull() else p.toDoubleOrNull()
                                if (d != null && d > 0.0 && d != qty) {
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
