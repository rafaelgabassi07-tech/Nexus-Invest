package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.data.*
import com.example.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val lastDividend: Double
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

class PortfolioViewModel(private val repository: TransactionRepository) : ViewModel() {

    // List of transactions from Room db
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
            val type = list.firstOrNull()?.type ?: "ACAO"
            val totalShares = list.sumOf { it.quantity }
            val totalSpent = list.sumOf { it.quantity * it.purchasePrice }
            val avgPrice = if (totalShares > 0) totalSpent / totalShares else 0.0

            val liveInfo = cache[ticker]
            val livePrice = liveInfo?.price ?: avgPrice
            val liveDY = liveInfo?.dy ?: 0.0
            val liveChange = liveInfo?.changePercent ?: 0.0
            val lastDiv = liveInfo?.lastDividend ?: 0.0

            val currentVal = totalShares * livePrice
            val retVal = currentVal - totalSpent
            val retPct = if (totalSpent > 0) (retVal / totalSpent) * 100.0 else 0.0

            AssetSummary(
                ticker = ticker,
                type = type,
                sharesCount = totalShares,
                averageCost = avgPrice,
                totalInvested = totalSpent,
                currentPrice = livePrice,
                totalCurrentValue = currentVal,
                totalReturn = retVal,
                returnPercent = retPct,
                dailyChangePercent = liveChange,
                dividendYield = liveDY,
                lastDividend = lastDiv
            )
        }.sortedByDescending { it.totalCurrentValue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        val stocksRatio = if (totalInvested > 0) totalStocksSpent / totalInvested else 0.0
        val fiisRatio = if (totalInvested > 0) totalFiisSpent / totalInvested else 0.0

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioSummary())

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

    // AI advisor report status
    private val _aiReport = MutableStateFlow<String?>(null)
    val aiReport: StateFlow<String?> = _aiReport.asStateFlow()

    private val _isLoadingAiReport = MutableStateFlow(false)
    val isLoadingAiReport: StateFlow<Boolean> = _isLoadingAiReport.asStateFlow()

    init {
        // Automatically fetch prices for loaded tickers when room updates
        viewModelScope.launch {
            transactions.collect { list ->
                val distinctTickers = list.map { it.ticker.trim().uppercase() }.distinct()
                if (distinctTickers.isNotEmpty()) {
                    triggerBatchedPriceFetch(distinctTickers, showSecondaryLoading = false)
                }
            }
        }
        
        // Initial news feed load
        fetchGlobalNews()
    }

    fun insertTransaction(ticker: String, quantity: Double, purchasePrice: Double, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitizedTicker = ticker.trim().uppercase()
            val tx = Transaction(
                ticker = sanitizedTicker,
                name = sanitizedTicker,
                quantity = quantity,
                purchasePrice = purchasePrice,
                type = type
            )
            repository.insert(tx)
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(tx)
        }
    }

    fun forceRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val distinctTickers = transactions.value.map { it.ticker.trim().uppercase() }.distinct()
            if (distinctTickers.isNotEmpty()) {
                triggerBatchedPriceFetch(distinctTickers, showSecondaryLoading = true)
            }
            fetchGlobalNews()
            _isRefreshing.value = false
        }
    }

    private suspend fun triggerBatchedPriceFetch(tickers: List<String>, showSecondaryLoading: Boolean) {
        if (showSecondaryLoading) _isLoadingPrices.value = true
        withContext(Dispatchers.IO) {
            val updatedMap = _cachedAssetData.value.toMutableMap()
            tickers.forEach { ticker ->
                val data = B3NetworkService.fetchAssetData(ticker)
                if (data != null) {
                    updatedMap[ticker] = data
                }
            }
            _cachedAssetData.value = updatedMap
        }
        _isLoadingPrices.value = false
    }

    fun fetchGlobalNews() {
        viewModelScope.launch {
            _isLoadingNews.value = true
            val news = withContext(Dispatchers.IO) {
                // If user has tickers, fetch customized news for foremost picker or generic news
                val userTicker = transactions.value.firstOrNull()?.ticker
                B3NetworkService.fetchNews(userTicker ?: "")
            }
            _newsFeed.value = news
            _isLoadingNews.value = false
        }
    }

    fun searchAndAnalyzeAsset(ticker: String) {
        val clean = ticker.trim().uppercase()
        if (clean.isEmpty()) return

        viewModelScope.launch {
            _isSearchingAsset.value = true
            _aiReport.value = null // clear old AI analyses
            
            val info = withContext(Dispatchers.IO) {
                B3NetworkService.fetchAssetData(clean)
            }
            _searchQueryResult.value = info

            if (info != null) {
                // Fetch historic chart point & dedicated news for selected asset
                val history = withContext(Dispatchers.IO) {
                    B3NetworkService.fetchHistoricalChart(clean, "1y")
                }
                val dedicatedNews = withContext(Dispatchers.IO) {
                    B3NetworkService.fetchNews(clean)
                }
                _searchQueryHistory.value = history
                _searchQueryNews.value = dedicatedNews
            }
            
            _isSearchingAsset.value = false
        }
    }

    fun generateAiAnalysisForSearchedAsset() {
        val activeRes = searchQueryResult.value ?: return
        viewModelScope.launch {
            _isLoadingAiReport.value = true
            
            val typeStr = if (activeRes.isFii) "Fundo Imobiliário" else "Ação"
            val prompt = """
            Você é um analista experiente no portal brasileiro Investidor 10. Emita um relatório analítico para o ativo:
            Ticker: ${activeRes.ticker}
            Tipo: $typeStr
            Preço Atual: R$ ${String.format("%.2f", activeRes.price)}
            Variação do Dia: ${String.format("%.2f", activeRes.changePercent)}%
            Dividend Yield Anual: ${String.format("%.2f", activeRes.dy)}%
            Relação P/L (Preço/Lucro): ${String.format("%.2f", activeRes.pl)} (ignorar se for FII)
            Relação P/VP (Preço/Valor Patrimonial): ${String.format("%.2f", activeRes.pvp)}
            Valor Patrimonial por Ação/Cota (VPA): R${'$'} ${String.format("%.2f", activeRes.vpa)}
            Último Rendimento Individual Estimado: R${'$'} ${String.format("%.2f", activeRes.lastDividend)}
            Retorno sobre Patrimônio (ROE): ${String.format("%.2f", activeRes.roe)}% (ignorar se for FII)
            Margem Líquida: ${String.format("%.2f", activeRes.margins)}% (ignorar se for FII)

            Divida seu relatório em:
            1. **Visão da Empresa/Fundo**: O que este ativo representa.
            2. **Análise de Indicadores**: Avaliação de DY, P/VP, P/L e sua robustez sob critérios de Value Investing.
            3. **Conselho do Consultor**: Uma recomendação educacional estruturada em português.
            Mantenha o tom profissional, estruturado e otimista. Use markdown completo com listas e títulos claros.
            """.trimIndent()

            val response = GeminiService.generateAnalysis(prompt)
            _aiReport.value = response
            _isLoadingAiReport.value = false
        }
    }

    fun makeAiAdvisorPortfolioReport() {
        val summaries = assetSummaries.value
        val summaryModel = portfolioSummary.value
        if (summaries.isEmpty()) return

        viewModelScope.launch {
            _isLoadingAiReport.value = true
            
            val assetsText = summaries.joinToString("\n") { s ->
                "- ${s.ticker} (${s.type}): Comprado ${s.sharesCount} cotas a Preço Médio R$ ${String.format("%.2f", s.averageCost)}. Preço atual R$ ${String.format("%.2f", s.currentPrice)}. Retorno total: R$ ${String.format("%.2f", s.totalReturn)} (${String.format("%.2f", s.returnPercent)}%). DY: ${String.format("%.2f", s.dividendYield)}%"
            }

            val prompt = """
            Você é o Orientador de Investimentos AI do site Investidor 10.
            Escreva um relatório detalhado e educativo sobre a carteira do usuário abaixo:
            
            **DADOS GERAIS DA CARTEIRA**:
            - Total Investido: R${'$'} ${String.format("%.2f", summaryModel.totalInvested)}
            - Valor Atual de Mercado: R${'$'} ${String.format("%.2f", summaryModel.totalCurrentValue)}
            - Retorno Total Absoluto: R${'$'} ${String.format("%.2f", summaryModel.totalReturn)} (${String.format("%.2f", summaryModel.returnPercent)}%)
            - Alocação em Ações (Relação): ${String.format("%.1f", summaryModel.sharesRatioStock * 100)}%
            - Alocação em Fundos Imobiliários (Relação): ${String.format("%.1f", summaryModel.sharesRatioFii * 100)}%

            **LISTA DE ATIVOS NO PORTFÓLIO**:
            $assetsText

            Analise e responda detalhadamente:
            1. **Metodologia de Alocação**: Se a proporção de Ações vs FIIs está equilibrada para um perfil moderador que busca dividendos recorrentes.
            2. **Retorno Geral e Rentabilidade**: Se o retorno atual é satisfatório frente à média do Ibovespa e IFIX de longo prazo.
            3. **Recomendações Práticas (Próximos Aportes)**: Diga quais ativos ou setores merecem novos aportes ou reequilíbrio, aplicando regras padrão de diversificação do Investidor 10.
            
            Retorne um painel analítico com formatação markdown rica em português.
            """.trimIndent()

            val response = GeminiService.generateAnalysis(prompt)
            _aiReport.value = response
            _isLoadingAiReport.value = false
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
