package com.example.network

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ConnectionPool
import okhttp3.Dispatcher as OkHttpDispatcher
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

data class B3AssetData(
    val ticker: String,
    val name: String = "",
    val price: Double = 0.0,
    val changePercent: Double = 0.0,
    val dy: Double = 0.0, // Dividend Yield %
    val pl: Double = 0.0, // P/L
    val pvp: Double = 0.0, // P/VP
    val vpa: Double = 0.0, // Valor Patrimonial por Ação
    val lpa: Double = 0.0, // Lucro por Ação
    val marketCap: Double = 0.0,
    val roe: Double = 0.0, // %
    val margins: Double = 0.0, // Margem Liquida %
    val lastDividend: Double = 0.0,
    val dailyLiquidity: Double = 0.0,
    val high52: Double = 0.0,
    val low52: Double = 0.0,
    val forwardPE: Double = 0.0,
    val priceToSales: Double = 0.0, // PSR
    val nextEarningsDate: String = "",
    val isFii: Boolean = false,
    val source: String = "Serviço de dados VALORAE",
    val payout: Double = 0.0,         // Payout %
    val cagrRevenue5y: Double = 0.0,  // CAGR Receitas 5 anos %
    
    // Novas métricas listadas no Investidor10
    val grossMargin: Double = 0.0,     // Margem Bruta %
    val ebitMargin: Double = 0.0,      // Margem Ebit %
    val ebitdaMargin: Double = 0.0,    // Margem Ebtida %
    val evEbitda: Double = 0.0,        // EV/Ebitda
    val evEbit: Double = 0.0,          // EV/Ebit
    val priceEbitda: Double = 0.0,     // P/Ebitda
    val priceEbit: Double = 0.0,       // P/Ebit
    val priceAsset: Double = 0.0,      // P/Ativo
    val priceCapGiro: Double = 0.0,    // P/Cap.Giro
    val priceAtivoCircLiq: Double = 0.0, // P/Ativo Circ. Liq.
    val giroAtivos: Double = 0.0,      // Giro Ativos
    val roic: Double = 0.0,            // ROIC
    val roa: Double = 0.0,             // ROA
    val divLiqPatrimonio: Double = 0.0, // Dívida Líquida / Patrimônio
    val debtEbitda: Double = 0.0,      // Dívida Líquida / Ebitda (para ações)
    val divLiqEbit: Double = 0.0,      // Dívida Líquida / Ebit
    val divBrutaPatrimonio: Double = 0.0, // Dívida Bruta / Patrimônio
    val patrimonioAtivos: Double = 0.0, // Patrimônio / Ativos
    val passivosAtivos: Double = 0.0,   // Passivos / Ativos
    val liquidezCorrente: Double = 0.0, // Liquidez Corrente

    // Histórico / Dados da Empresa / Informações Iniciais
    val cnpj: String = "",
    val listSegment: String = "",       // Segmento de Listagem
    val foundationYear: String = "",    // Ano de fundação
    val listingYear: String = "",       // Ano de estreia na bolsa
    val employeesCount: String = "",    // Número de funcionários
    val firmValue: Double = 0.0,        // Valor de firma
    val netWorth: Double = 0.0,         // Patrimônio Líquido
    val totalPapers: String = "",       // Nº total de papeis
    val totalAssets: Double = 0.0,      // Ativos (Total)
    val currentAssets: Double = 0.0,    // Ativo Circulante
    val grossDebt: Double = 0.0,        // Dívida Bruta
    val netDebt: Double = 0.0,          // Dívida Líquida
    val availability: Double = 0.0,     // Disponibilidade
    val freeFloat: Double = 0.0,        // Free Float
    val tagAlong: Double = 0.0,         // Tag Along

    val fiiVacancy: Double = 0.0,     // Vacância % (para FIIs)
    val fiiPropertyCount: Int = 0,    // Quantidade de Imóveis
    val fiiSegment: String = "",       // Segmento do FII (ex: Papel, Tijolo, Logística)
    val assetDescription: String = "",// Descrição operacional do ativo
    val subSector: String = "",       // Subsetor de Atuação
    
    // Novas propriedades FII e Ação Guia Scraping Investidor10
    val cagrProfit5y: Double = 0.0,   // CAGR Lucros 5 anos %
    val fiiTotalHolders: String = "", // Número de Cotistas (FII)
    val fiiIssuedShares: String = "", // Cotas Emitidas (FII)
    val fiiAdminFee: String = "",     // Taxa de Administração (FII)
    val fiiFundType: String = "",     // Tipo de Fundo (FII)
    val fiiMandate: String = "",      // Mandato (FII)
    val fiiTargetAudience: String = "",// Público-alvo (FII)
    val fiiManagementType: String = "",// Tipo de Gestão (FII)
    val fiiDuration: String = "",     // Prazo de Duração (FII)
    val magicNumber: Double = 0.0,    // Magic Number (FII)

    // Metadados do contrato serviço de dados VALORAE v1 usados para cache, diagnóstico e renderização segura.
    val proxyStatus: String = "OK",
    val isPartial: Boolean = false,
    val dataReliability: String = "",
    val extractionCompleteness: Double = 0.0,
    val partialDataGuidance: String = "",
    val cacheStatus: String = "",
    val handlerTotalMs: Long = 0L,
    val shouldKeepPreviousSnapshot: Boolean = false,
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val fromLocalSnapshot: Boolean = false
)

data class ProxyDiagnosticsSummary(
    val baseUrl: String,
    val state: String = "Desconhecido",
    val ready: Boolean = false,
    val usingLocalCache: Boolean = false,
    val lastCheckedAt: Long = 0L,
    val sourceStatus: String = "Não consultado",
    val cacheEntries: Int = 0,
    val bestSnapshotEntries: Int = 0,
    val updatedAssets: Int = 0,
    val partialResponses: Int = 0,
    val recentErrors: List<String> = emptyList(),
    val averageResponseMs: Long = 0L,
    val metricsStatus: String = "Não consultado"
)

data class NewsItem(
    val title: String,
    val link: String,
    val pubDate: String = "",
    val source: String = "",
    val description: String = "",
    val timestamp: Long = 0L
)

data class ChartPoint(
    val timestamp: Long,
    val dateLabel: String,
    val close: Double
)

data class PortfolioProxyPosition(
    val ticker: String,
    val quantity: Double,
    val averagePrice: Double,
    val type: String = "ACAO",
    val currentPrice: Double = 0.0,
    val totalInvested: Double = 0.0,
    // Momento da primeira compra conhecida no APK. Enviado ao Proxy como
    // metadado opcional para evitar análises que extrapolem a existência real
    // da carteira. Se o backend ignorar o campo, o APK ainda aplica o filtro local.
    val firstPurchaseAt: Long = 0L
)

data class PortfolioHistoryPoint(
    val timestamp: Long,
    val dateLabel: String,
    val totalValue: Double,
    val investedValue: Double = 0.0,
    val returnPercent: Double = 0.0,
    val source: String = "Serviço de dados VALORAE"
)

data class IpcaPoint(
    val timestamp: Long,
    val dateLabel: String,
    val accumulatedPercent: Double,
    val monthlyPercent: Double = 0.0,
    val source: String = "Serviço de dados VALORAE"
)

data class DividendEvent(
    val ticker: String,
    val dateCom: String = "",
    val paymentDate: String = "",
    val valuePerShare: Double = 0.0,
    val quantity: Double = 0.0,
    val estimatedAmount: Double = 0.0,
    val status: String = "Previsto",
    val source: String = "Serviço de dados VALORAE"
)

data class PortfolioProxyActionPlanItem(
    val priority: String = "info",
    val code: String = "",
    val message: String = ""
)

data class PortfolioPositionRankingItem(
    val rank: Int = 0,
    val ticker: String = "",
    val score: Double = 0.0,
    val grade: String = "",
    val weightPercent: Double = 0.0,
    val monthlyIncomeEstimated: Double = 0.0,
    val reason: String = ""
)

data class PortfolioRebalanceAction(
    val scope: String = "",
    val ticker: String = "",
    val type: String = "",
    val action: String = "HOLD",
    val currentPercent: Double = 0.0,
    val targetPercent: Double = 0.0,
    val deltaValue: Double = 0.0,
    val estimatedQuantity: Double = 0.0
)

data class MarketRankingItem(
    val rank: Int = 0,
    val ticker: String = "",
    val name: String = "",
    val value: Double = 0.0,
    val displayValue: String = "",
    val price: Double = 0.0,
    val priceDisplay: String = "",
    val changePercent: Double = 0.0,
    val changeDisplay: String = "",
    val grade: String = "",
    val direction: String = "",
    val source: String = "Serviço de dados VALORAE",
    val explanation: String = "",
    val volume: Double = 0.0,
    val setor: String = "",
    val segmento: String = "",
    val url: String = ""
)

data class MarketRankingSnapshot(
    val type: String = "ACAO",
    val source: String = "Serviço de dados VALORAE",
    val fallbackUsed: Boolean = false,
    val score: List<MarketRankingItem> = emptyList(),
    val dividendYield: List<MarketRankingItem> = emptyList(),
    val pvp: List<MarketRankingItem> = emptyList(),
    val pl: List<MarketRankingItem> = emptyList(),
    val roe: List<MarketRankingItem> = emptyList(),
    val roic: List<MarketRankingItem> = emptyList(),
    val quality: List<MarketRankingItem> = emptyList(),
    val value: List<MarketRankingItem> = emptyList(),
    val conservative: List<MarketRankingItem> = emptyList(),
    val growth: List<MarketRankingItem> = emptyList(),
    val dividendsProfile: List<MarketRankingItem> = emptyList(),
    val valueProfile: List<MarketRankingItem> = emptyList(),
    val incomeFii: List<MarketRankingItem> = emptyList(),
    val highs: List<MarketRankingItem> = emptyList(),
    val lows: List<MarketRankingItem> = emptyList(),
    val warnings: List<String> = emptyList()
)



data class ProxyCapabilityRow(
    val label: String,
    val value: String = "",
    val detail: String = "",
    val score: Double = 0.0,
    val status: String = ""
)

data class ProxyCapabilitySection(
    val title: String,
    val subtitle: String = "",
    val endpoint: String = "",
    val status: String = "",
    val rows: List<ProxyCapabilityRow> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class AssetProxyCapabilities(
    val ticker: String,
    val isFii: Boolean = false,
    val qualitySections: List<ProxyCapabilitySection> = emptyList(),
    val advancedSections: List<ProxyCapabilitySection> = emptyList(),
    val fiiSections: List<ProxyCapabilitySection> = emptyList(),
    val source: String = "Serviço de dados VALORAE",
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

data class PortfolioProxyCapabilities(
    val sections: List<ProxyCapabilitySection> = emptyList(),
    val radarSections: List<ProxyCapabilitySection> = emptyList(),
    val diagnosticsSections: List<ProxyCapabilitySection> = emptyList(),
    val source: String = "Serviço de dados VALORAE",
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
data class PortfolioProxyAnalysis(
    val score: Double = 0.0,
    val riskLabel: String = "Não medido",
    val diversificationLabel: String = "Não medido",
    val concentrationPercent: Double = 0.0,
    val topHolding: String = "",
    val monthlyDividendEstimate: Double = 0.0,
    val annualDividendEstimate: Double = 0.0,
    val dataQuality: Double = 0.0,
    val healthScore: Double = 0.0,
    val incomeStabilityScore: Double = 0.0,
    val technologyReadinessScore: Double = 0.0,
    val incomePayerPercent: Double = 0.0,
    val allocationByClass: List<Pair<String, Double>> = emptyList(),
    val allocationBySector: List<Pair<String, Double>> = emptyList(),
    val actionPlan: List<PortfolioProxyActionPlanItem> = emptyList(),
    val positionRanking: List<PortfolioPositionRankingItem> = emptyList(),
    val rebalanceActions: List<PortfolioRebalanceAction> = emptyList(),
    val warnings: List<String> = emptyList(),
    val source: String = "Serviço de dados VALORAE"
)

object B3NetworkService {
    private val httpDispatcher = OkHttpDispatcher().apply {
        // Limita rajadas simultâneas para preservar fluidez no app e reduzir pressão no Vercel Free.
        maxRequests = 8
        maxRequestsPerHost = 4
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(22, TimeUnit.SECONDS)
        .callTimeout(24, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .dispatcher(httpDispatcher)
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .build()

    private class CacheEntry<T>(val data: T, val expiresAt: Long, val createdAt: Long = System.currentTimeMillis())
    private val memoryCache = ConcurrentHashMap<String, CacheEntry<Any>>()
    private val bestAssetSnapshots = ConcurrentHashMap<String, B3AssetData>()
    private val partialResponseTickers = ConcurrentHashMap<String, Long>()
    private val responseTimeSamples = Collections.synchronizedList(mutableListOf<Long>())
    private val recentProxyErrors = Collections.synchronizedList(mutableListOf<String>())
    private val lastUpdatedTickers = ConcurrentHashMap<String, Long>()
    @Volatile private var appContext: Context? = null
    @Volatile private var lastReadyAt: Long = 0L
    @Volatile private var lastReadyState: String = "Desconhecido"

    private const val PROXY_PLUS_ASSET_ADVANCED_LIMIT = 5
    private const val PROXY_PLUS_FII_LIMIT = 4
    private const val PROXY_PLUS_PORTFOLIO_LIMIT = 5
    private const val PROXY_PLUS_DIAGNOSTIC_LIMIT = 4

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun proxyBaseUrl(): String = configuredProxyBaseUrl()

    private fun <T> getFromCache(key: String, allowExpired: Boolean = false): T? {
        val entry = memoryCache[key] ?: return null
        if (!allowExpired && System.currentTimeMillis() > entry.expiresAt) {
            memoryCache.remove(key)
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return entry.data as? T
    }

    private fun putInCache(key: String, data: Any, ttlMinutes: Int = 5) {
        // Dados de empresa/fundamentos/FIIs mudam devagar e não devem ser rebaixados
        // como cotação. O limite máximo maior permite cache de 6h-24h para módulos
        // estáveis, enquanto cotações/rankings continuam usando TTL curto nos callers.
        val safeTtlMinutes = ttlMinutes.coerceIn(1, 24 * 60)
        val expiresAt = System.currentTimeMillis() + (safeTtlMinutes * 60 * 1000L)
        memoryCache[key] = CacheEntry(data, expiresAt)
        
        // Optimize memory consumption by pruning expired and least-useful entries when cache is growing.
        if (memoryCache.size > 120) {
            val now = System.currentTimeMillis()
            memoryCache.entries.removeIf { it.value.expiresAt < now }
        }
        if (memoryCache.size > 180) {
            val overflow = memoryCache.entries
                .sortedWith(compareBy<Map.Entry<String, CacheEntry<Any>>> { it.value.expiresAt }.thenBy { it.value.createdAt })
                .take(memoryCache.size - 150)
                .map { it.key }
            overflow.forEach { memoryCache.remove(it) }
        }
    }

    private fun stableEndpointTtlMinutes(endpoint: String): Int {
        val e = endpoint.lowercase(Locale.ROOT)
        return when {
            // Informações corporativas e fundamentalistas são atualizadas por empresas em janelas longas.
            // Mantê-las por mais tempo reduz travamentos, chamadas repetidas e pressão no Proxy/Vercel Free.
            e.contains("/asset/profile") || e.contains("/asset/fundamentals") ||
                e.contains("/asset/valuation") || e.contains("/asset/profitability") ||
                e.contains("/asset/debt") || e.contains("/asset/statements") ||
                e.contains("/asset/peers") || e.contains("/asset/indicators") ||
                e.contains("/asset/source-map") || e.contains("/asset/coverage") ||
                e.contains("/asset/quality") || e.contains("/asset/action-plan") -> 12 * 60
            e.contains("/fii/") -> 12 * 60
            e.contains("/watchlist/") || e.contains("/portfolio/rebalance") || e.contains("/portfolio/allocation") ||
                e.contains("/portfolio/risk") || e.contains("/portfolio/summary") || e.contains("/portfolio/income") -> 60
            e.contains("/portfolio/events") || e.contains("/portfolio/dividends") || e.contains("/asset/next-dividend") -> 30
            e.contains("/engine/") || e.contains("/cache/") || e.contains("/deploy/") || e.contains("/schema") || e.endsWith("/health") -> 10
            else -> 30
        }
    }

    private fun rangeCacheTtlMinutes(range: String): Int {
        return when (normalizeProxyRange(range)) {
            "1D", "5D" -> 5
            "1M", "3M", "6M" -> 20
            "YTD", "1Y" -> 60
            "3Y", "5Y", "10Y", "MAX" -> 6 * 60
            else -> 30
        }
    }

    private fun cacheKeySafe(value: String): String {
        return value.replace(Regex("[^A-Za-z0-9_]+"), "_").trim('_').take(180)
    }

    private fun endpointCacheKey(prefix: String, endpoint: String, params: Map<String, String?> = emptyMap(), payload: String = ""): String {
        val paramsKey = params.entries
            .filter { !it.value.isNullOrBlank() }
            .sortedBy { it.key }
            .joinToString("&") { "${it.key}=${it.value}" }
        val payloadKey = if (payload.isBlank()) "" else "_${payload.hashCode()}"
        return cacheKeySafe("${prefix}_${endpoint}_${paramsKey}${payloadKey}")
    }

    private fun recordResponseTime(ms: Long) {
        if (ms <= 0L) return
        synchronized(responseTimeSamples) {
            responseTimeSamples.add(ms)
            while (responseTimeSamples.size > 40) responseTimeSamples.removeAt(0)
        }
    }

    private fun recordProxyError(message: String) {
        val clean = message.take(180)
        synchronized(recentProxyErrors) {
            recentProxyErrors.add("${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())} • $clean")
            while (recentProxyErrors.size > 8) recentProxyErrors.removeAt(0)
        }
    }

    private fun assetHasUsefulData(asset: B3AssetData?): Boolean {
        if (asset == null) return false
        return asset.price > 0.0 || asset.dy > 0.0 || asset.pvp > 0.0 ||
            asset.pl > 0.0 || asset.assetDescription.isNotBlank() || asset.name.isNotBlank()
    }

    private fun assetIsGoodSnapshot(asset: B3AssetData): Boolean {
        if (!assetHasUsefulData(asset)) return false
        if (asset.shouldKeepPreviousSnapshot) return false
        if (asset.isPartial && asset.price <= 0.0 && asset.dy <= 0.0 && asset.pvp <= 0.0 && asset.pl <= 0.0) return false
        return true
    }

    private fun assetSnapshotKey(ticker: String) = "asset_snapshot_${ticker.trim().uppercase(Locale.ROOT)}"

    private fun saveBestSnapshot(asset: B3AssetData) {
        val clean = asset.ticker.trim().uppercase(Locale.ROOT)
        if (clean.isBlank() || !assetIsGoodSnapshot(asset)) return
        val normalized = asset.copy(ticker = clean, fromLocalSnapshot = false, lastUpdatedAt = System.currentTimeMillis())
        bestAssetSnapshots[clean] = normalized
        lastUpdatedTickers[clean] = System.currentTimeMillis()
        runCatching {
            appContext?.getSharedPreferences("valorae_proxy_snapshots", Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(assetSnapshotKey(clean), assetToSnapshotJson(normalized).toString())
                ?.apply()
        }
    }

    private fun loadBestSnapshot(ticker: String): B3AssetData? {
        val clean = ticker.trim().uppercase(Locale.ROOT)
        bestAssetSnapshots[clean]?.let { return it.copy(fromLocalSnapshot = true) }
        val raw = runCatching {
            appContext?.getSharedPreferences("valorae_proxy_snapshots", Context.MODE_PRIVATE)
                ?.getString(assetSnapshotKey(clean), null)
        }.getOrNull() ?: return null
        return runCatching { snapshotJsonToAsset(JSONObject(raw)) }
            .getOrNull()
            ?.also { bestAssetSnapshots[clean] = it.copy(fromLocalSnapshot = false) }
            ?.copy(fromLocalSnapshot = true)
    }

    private fun assetToSnapshotJson(asset: B3AssetData): JSONObject = JSONObject()
        .put("ticker", asset.ticker)
        .put("name", asset.name)
        .put("price", asset.price)
        .put("changePercent", asset.changePercent)
        .put("dy", asset.dy)
        .put("pl", asset.pl)
        .put("pvp", asset.pvp)
        .put("vpa", asset.vpa)
        .put("lpa", asset.lpa)
        .put("marketCap", asset.marketCap)
        .put("roe", asset.roe)
        .put("margins", asset.margins)
        .put("lastDividend", asset.lastDividend)
        .put("dailyLiquidity", asset.dailyLiquidity)
        .put("high52", asset.high52)
        .put("low52", asset.low52)
        .put("source", asset.source)
        .put("isFii", asset.isFii)
        .put("payout", asset.payout)
        .put("cagrRevenue5y", asset.cagrRevenue5y)
        .put("grossMargin", asset.grossMargin)
        .put("ebitMargin", asset.ebitMargin)
        .put("ebitdaMargin", asset.ebitdaMargin)
        .put("evEbitda", asset.evEbitda)
        .put("evEbit", asset.evEbit)
        .put("roic", asset.roic)
        .put("roa", asset.roa)
        .put("debtEbitda", asset.debtEbitda)
        .put("liquidezCorrente", asset.liquidezCorrente)
        .put("fiiVacancy", asset.fiiVacancy)
        .put("fiiSegment", asset.fiiSegment)
        .put("assetDescription", asset.assetDescription)
        .put("subSector", asset.subSector)
        .put("magicNumber", asset.magicNumber)
        .put("proxyStatus", asset.proxyStatus)
        .put("dataReliability", asset.dataReliability)
        .put("extractionCompleteness", asset.extractionCompleteness)
        .put("cacheStatus", asset.cacheStatus)
        .put("lastUpdatedAt", asset.lastUpdatedAt)

    private fun snapshotJsonToAsset(json: JSONObject): B3AssetData = B3AssetData(
        ticker = json.optString("ticker"),
        name = json.optString("name"),
        price = json.optDouble("price", 0.0),
        changePercent = json.optDouble("changePercent", 0.0),
        dy = json.optDouble("dy", 0.0),
        pl = json.optDouble("pl", 0.0),
        pvp = json.optDouble("pvp", 0.0),
        vpa = json.optDouble("vpa", 0.0),
        lpa = json.optDouble("lpa", 0.0),
        marketCap = json.optDouble("marketCap", 0.0),
        roe = json.optDouble("roe", 0.0),
        margins = json.optDouble("margins", 0.0),
        lastDividend = json.optDouble("lastDividend", 0.0),
        dailyLiquidity = json.optDouble("dailyLiquidity", 0.0),
        high52 = json.optDouble("high52", 0.0),
        low52 = json.optDouble("low52", 0.0),
        source = json.optString("source", "Snapshot local VALORAE"),
        isFii = json.optBoolean("isFii", false),
        payout = json.optDouble("payout", 0.0),
        cagrRevenue5y = json.optDouble("cagrRevenue5y", 0.0),
        grossMargin = json.optDouble("grossMargin", 0.0),
        ebitMargin = json.optDouble("ebitMargin", 0.0),
        ebitdaMargin = json.optDouble("ebitdaMargin", 0.0),
        evEbitda = json.optDouble("evEbitda", 0.0),
        evEbit = json.optDouble("evEbit", 0.0),
        roic = json.optDouble("roic", 0.0),
        roa = json.optDouble("roa", 0.0),
        debtEbitda = json.optDouble("debtEbitda", 0.0),
        liquidezCorrente = json.optDouble("liquidezCorrente", 0.0),
        fiiVacancy = json.optDouble("fiiVacancy", 0.0),
        fiiSegment = json.optString("fiiSegment", ""),
        assetDescription = json.optString("assetDescription", ""),
        subSector = json.optString("subSector", ""),
        magicNumber = json.optDouble("magicNumber", 0.0),
        proxyStatus = json.optString("proxyStatus", "SNAPSHOT"),
        dataReliability = json.optString("dataReliability", "snapshot-local"),
        extractionCompleteness = json.optDouble("extractionCompleteness", 0.0),
        cacheStatus = json.optString("cacheStatus", "local"),
        lastUpdatedAt = json.optLong("lastUpdatedAt", 0L),
        fromLocalSnapshot = true
    )

    private val ETFS_CONHECIDOS = setOf(
        "BOVA11", "IVVB11", "SMAL11", "DIVO11", "FIND11", "MATB11", "GOVE11", "XFIX11",
        "GOLD11", "SPXI11", "HASH11", "BOVB11", "BOVS11", "BRAP11", "BRRJ11", "BRAX11",
        "XINA11", "EURP11", "FIXA11", "TCHE11", "ECOO11", "ACWI11", "NASD11",
        "USTK11", "NSDQ11", "DEFI11", "ESGE11", "SUST11", "AGRI11", "IFRA11",
        "BDIV11", "BLKB11", "BNDX11", "BOVV11", "BRCO11", "CSMO11", "VALE11", "QUAL11",
        "REIT11", "TRET11", "WRLD11", "XBOV11", "PIBB11", "SMAC11", "MOAT11", "PORD11"
    )

    fun inferIsFii(ticker: String): Boolean {
        val clean = ticker.trim().uppercase()
        if (clean.endsWith("11") && !ETFS_CONHECIDOS.contains(clean)) return true
        if (clean.endsWith("12") || clean.endsWith("13")) return true
        return false
    }

    private const val PROXY_CLIENT_ID_DEFAULT = "valorae-investidor-android"

    private fun envString(valueProvider: () -> String): String = try {
        valueProvider().trim()
    } catch (_: Throwable) {
        ""
    }

    private fun configuredProxyBaseUrl(): String {
        val candidates = listOf(
            envString { BuildConfig.VALORAE_API_BASE_URL },
            envString { BuildConfig.VALORAE_PROXY_BASE_URL },
            envString { BuildConfig.VALORAE_PUBLIC_BASE_URL }
        )
        val selected = candidates.firstOrNull { isUsableProxyUrl(it) }?.trim()?.trimEnd('/')
        return if (!selected.isNullOrBlank()) selected else "https://servidor-valorae.vercel.app"
    }

    private fun isUsableProxyUrl(raw: String): Boolean {
        val value = raw.trim().trimEnd('/')
        if (value.isBlank()) return false
        if (!value.startsWith("https://")) return false
        val lower = value.lowercase(Locale.ROOT)
        return !lower.contains("your-backend") &&
            !lower.contains("seu-dominio") &&
            !lower.contains("seu-valorae") &&
            !lower.contains("your-valorae") &&
            !lower.contains("example.com") &&
            !lower.contains("localhost") &&
            !lower.contains("10.0.2.2") &&
            !lower.contains("127.0.0.1") &&
            // Host legado que causava telas vazias. Mesmo que venha do Studio/env,
            // deve ser ignorado para preservar o contrato atual do Serviço de dados VALORAE.
            !lower.contains("valorae-proxy.vercel.app")
    }

    private fun directFallbackEnabled(): Boolean {
        // Regra do app: não fazer scraping/chamadas diretas como fallback no Android.
        // Todo dado externo deve vir pelo Serviço de dados VALORAE oficial para manter CORS,
        // observabilidade, cache, normalização e contrato de campos estáveis.
        return false
    }

    private fun proxyClientId(): String {
        return envString { BuildConfig.VALORAE_PROXY_CLIENT_ID }.ifBlank { PROXY_CLIENT_ID_DEFAULT }
    }

    private fun urlEncode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    private fun proxyUrl(path: String, params: Map<String, String?> = emptyMap()): String? {
        val base = configuredProxyBaseUrl()
        if (base.isBlank()) return null
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        val query = params.entries
            .filter { !it.value.isNullOrBlank() }
            .joinToString("&") { "${urlEncode(it.key)}=${urlEncode(it.value ?: "")}" }
        return if (query.isBlank()) "$base$cleanPath" else "$base$cleanPath?$query"
    }

    private fun normalizeProxyRange(range: String): String {
        return when (range.trim().lowercase(Locale.ROOT)) {
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
            else -> range.trim().uppercase(Locale.ROOT).ifBlank { "1Y" }
        }
    }

    private fun historyLimitForRange(normalizedRange: String): String {
        val limit = when (normalizedRange.uppercase(Locale.ROOT)) {
            "1D" -> 390
            "5D" -> 500
            "1M", "3M", "6M", "YTD", "1Y" -> 370
            "3Y" -> 900
            "5Y" -> 1500
            "10Y" -> 3000
            "MAX" -> 5000
            else -> 370
        }
        return limit.toString()
    }

    private fun positionArray(positions: List<PortfolioProxyPosition>): JSONArray {
        val arr = JSONArray()
        positions.forEach { p ->
            if (p.ticker.isBlank() || p.quantity <= 0.0) return@forEach
            arr.put(
                JSONObject()
                    .put("ticker", p.ticker.trim().uppercase(Locale.ROOT))
                    .put("quantity", p.quantity)
                    .put("averagePrice", p.averagePrice)
                    .put("type", p.type)
                    .put("currentPrice", p.currentPrice)
                    .put("investedValue", p.totalInvested)
                    .put("totalInvested", p.totalInvested)
                    .put("firstPurchaseAt", p.firstPurchaseAt)
                    .put("firstPurchaseAtSeconds", if (p.firstPurchaseAt > 0L) p.firstPurchaseAt / 1000L else 0L)
                    .put("currentValue", if (p.currentPrice > 0.0) p.currentPrice * p.quantity else 0.0)
            )
        }
        return arr
    }

    private fun firstPairList(vararg arrays: JSONArray?): List<Pair<String, Double>> {
        val arr = arrays.firstOrNull { it != null && it.length() > 0 } ?: return emptyList()
        val out = mutableListOf<Pair<String, Double>>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val label = firstText(
                item.optAny("label"),
                item.optAny("name"),
                item.optAny("key"),
                item.optAny("class"),
                item.optAny("type"),
                item.optAny("sector"),
                item.optAny("segment")
            )
            val value = firstNumber(
                item.optAny("percent"),
                item.optAny("percentage"),
                item.optAny("weight"),
                item.optAny("share"),
                item.optAny("value")
            )
            if (label.isNotBlank() && value > 0.0) out.add(label to value)
        }
        return out
    }

    private fun proxyRequest(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "VALORAE-Investidor-Portfolio/${BuildConfig.VERSION_NAME} Android")
            .addHeader("x-valorae-app", "VALORAE Investidor")
            .addHeader("x-valorae-client", proxyClientId())
            .addHeader("x-valorae-build", "${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}")
            .addHeader("x-valorae-platform", "android")
            .addHeader("X-Valorae-Client-Id", proxyClientId())
            .addHeader("X-Valorae-Client-Version", BuildConfig.VERSION_NAME.ifBlank { "21.12.52" })
            .addHeader("X-Valorae-Environment", "production")
    }

    /**
     * O Proxy também usa campos `error`/`warning` em blocos opcionais (notícias, histórico,
     * rankings e métricas) para explicar indisponibilidade temporária de fonte externa.
     * Antes o APK descartava o JSON inteiro só por existir `error`, impedindo cache/fallback.
     * Agora somente status explicitamente fatal derruba a resposta; blocos opcionais continuam
     * disponíveis para que cada parser decida renderizar vazio, cache local ou último snapshot bom.
     */
    private fun isFatalProxyPayload(json: JSONObject?): Boolean {
        if (json == null) return false
        val status = json.optString("status", "")
        if (status.equals("ERROR", ignoreCase = true) || status.equals("FATAL", ignoreCase = true)) return true
        val okPresent = json.has("ok") && !json.isNull("ok")
        val okValue = if (okPresent) json.optBoolean("ok", true) else true
        val optionalBlock = json.optObject("appPolicy")?.optBoolean("optionalBlock", false) == true ||
            json.optObject("reliability")?.optBoolean("optionalBlock", false) == true ||
            json.optString("endpoint", "").lowercase(Locale.ROOT) in setOf("asset-history", "news")
        if (!okValue && optionalBlock) return false
        return !okValue && json.has("error") && !optionalBlock
    }

    private fun proxyPayloadMessage(json: JSONObject?): String {
        return firstText(
            json?.optAny("message"),
            json?.optAny("error"),
            json?.optAny("warning"),
            json?.optAny("code"),
            "payload error"
        )
    }

    private fun getProxyJson(path: String, params: Map<String, String?> = emptyMap()): JSONObject? {
        val url = proxyUrl(path, params) ?: return null
        val startedAt = System.currentTimeMillis()
        return try {
            client.newCall(proxyRequest(url).get().build()).execute().use { response ->
                recordResponseTime(System.currentTimeMillis() - startedAt)
                val responseString = response.body?.string().orEmpty()
                if (responseString.isBlank()) {
                    val msg = "GET $path vazio (HTTP ${response.code})"
                    Log.w("B3NetworkService", "Serviço de dados VALORAE response body is empty: $msg")
                    recordProxyError(msg)
                    return null
                }
                val json = try { 
                    JSONObject(responseString) 
                } catch (e: Exception) { 
                    try {
                        val arr = JSONArray(responseString)
                        JSONObject().put("payload", arr)
                    } catch (e2: Exception) {
                        null
                    }
                }
                if (!response.isSuccessful) {
                    val errMsg = json?.optString("message") ?: json?.optString("error") ?: "HTTP ${response.code}"
                    Log.w("B3NetworkService", "Serviço de dados VALORAE GET failed: $path, errorMsg: $errMsg")
                    recordProxyError("GET $path • $errMsg")
                    return null
                }
                if (isFatalProxyPayload(json)) {
                    val errMsg = proxyPayloadMessage(json)
                    Log.w("B3NetworkService", "Serviço de dados VALORAE GET payload fatal: $path, errorMsg: $errMsg")
                    recordProxyError("GET $path • $errMsg")
                    return null
                }
                json
            }
        } catch (e: Exception) {
            Log.w("B3NetworkService", "Serviço de dados VALORAE GET exception: $path", e)
            recordProxyError("GET $path • ${e.message ?: e.javaClass.simpleName}")
            null
        }
    }

    private fun postProxyJson(path: String, payload: JSONObject): JSONObject? {
        val url = proxyUrl(path) ?: return null
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val startedAt = System.currentTimeMillis()
        return try {
            client.newCall(
                proxyRequest(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
            ).execute().use { response ->
                recordResponseTime(System.currentTimeMillis() - startedAt)
                val raw = response.body?.string().orEmpty()
                if (raw.isBlank()) {
                    val msg = "POST $path vazio (HTTP ${response.code})"
                    Log.w("B3NetworkService", "Serviço de dados VALORAE POST response body is empty: $msg")
                    recordProxyError(msg)
                    return null
                }
                val json = try { 
                    JSONObject(raw) 
                } catch (e: Exception) { 
                    try {
                        val arr = JSONArray(raw)
                        JSONObject().put("payload", arr)
                    } catch (e2: Exception) {
                        null
                    }
                }
                if (!response.isSuccessful) {
                    val errMsg = json?.optString("message") ?: json?.optString("error") ?: "HTTP ${response.code}"
                    Log.w("B3NetworkService", "Serviço de dados VALORAE POST failed: $path, errorMsg: $errMsg")
                    recordProxyError("POST $path • $errMsg")
                    return null
                }
                if (isFatalProxyPayload(json)) {
                    val errMsg = proxyPayloadMessage(json)
                    Log.w("B3NetworkService", "Serviço de dados VALORAE POST payload fatal: $path, errorMsg: $errMsg")
                    recordProxyError("POST $path • $errMsg")
                    return null
                }
                json
            }
        } catch (e: Exception) {
            Log.w("B3NetworkService", "Serviço de dados VALORAE POST exception: $path", e)
            recordProxyError("POST $path • ${e.message ?: e.javaClass.simpleName}")
            null
        }
    }

    fun unwrapValoraePayload(json: JSONObject?): JSONObject? {
        if (json == null) return null
        return json.optJSONObject("data") ?: json.optJSONObject("payload") ?: json.optJSONObject("result") ?: json
    }

    private fun dividendPayloadCandidates(json: JSONObject?): List<JSONObject> {
        if (json == null) return emptyList()
        val out = mutableListOf<JSONObject>()
        fun addObject(obj: JSONObject?) {
            if (obj == null || obj.length() == 0) return
            val signature = obj.toString()
            if (out.none { it.toString() == signature }) out.add(obj)
        }
        fun addArrayAsObject(alias: String, arr: JSONArray?) {
            if (arr == null || arr.length() == 0) return
            addObject(JSONObject().put(alias, arr))
        }

        addObject(json)
        val wrapperKeys = listOf("data", "payload", "result", "response", "body", "portfolio", "asset")
        wrapperKeys.forEach { key ->
            addObject(json.optJSONObject(key))
            addArrayAsObject("items", json.optJSONArray(key))
        }

        var index = 0
        while (index < out.size && index < 32) {
            val obj = out[index++]
            wrapperKeys.forEach { key ->
                addObject(obj.optJSONObject(key))
                addArrayAsObject("items", obj.optJSONArray(key))
            }
        }
        return out
    }

    private fun JSONArray.optJsonObjectOrNull(index: Int): JSONObject? = try {
        optJSONObject(index)
    } catch (_: Exception) { null }

    private fun JSONObject.optObject(path: String): JSONObject? {
        if (has(path) && !isNull(path)) return optJSONObject(path)
        return optAny(path) as? JSONObject
    }

    private fun JSONObject.optArray(path: String): JSONArray? {
        if (has(path) && !isNull(path)) return optJSONArray(path)
        return optAny(path) as? JSONArray
    }

    private fun JSONObject.optAny(key: String): Any? {
        if (has(key) && !isNull(key)) return opt(key)
        if (!key.contains('.')) return null
        var current: Any? = this
        for (part in key.split('.').filter { it.isNotBlank() }) {
            current = when (val node = current) {
                is JSONObject -> if (node.has(part) && !node.isNull(part)) node.opt(part) else return null
                is JSONArray -> {
                    val index = part.toIntOrNull() ?: return null
                    if (index in 0 until node.length()) node.opt(index) else return null
                }
                else -> return null
            }
            if (current == JSONObject.NULL) return null
        }
        return current
    }

    private fun normalizedValue(normalized: JSONObject?, key: String): Any? {
        if (normalized == null || !normalized.has(key) || normalized.isNull(key)) return null
        val field = normalized.optJSONObject(key)
        return if (field != null) {
            field.optAny("value")
                ?: field.optAny("display")
                ?: field.optAny("valor")
                ?: field.optAny("raw")
        } else {
            normalized.optAny(key)
        }
    }

    private fun normalizedDisplay(normalized: JSONObject?, key: String): String {
        if (normalized == null || !normalized.has(key) || normalized.isNull(key)) return ""
        val field = normalized.optJSONObject(key)
        return if (field != null) {
            firstText(field.optAny("display"), field.optAny("value"), field.optAny("valor"), field.optAny("raw"))
        } else {
            firstText(normalized.optAny(key))
        }
    }

    private fun firstText(vararg values: Any?): String {
        for (value in values) {
            val text = value?.toString()?.trim().orEmpty()
            if (text.isNotBlank() && text != "null" && text != "--" && text != "-") return text
        }
        return ""
    }

    private fun firstNumber(vararg values: Any?): Double {
        for (value in values) {
            val n = when (value) {
                is JSONObject -> parseLocaleFinancialNumber(
                    value.optAny("value")
                        ?: value.optAny("valor")
                        ?: value.optAny("display")
                        ?: value.optAny("raw")
                )
                else -> parseLocaleFinancialNumber(value)
            }
            if (n != 0.0) return n
        }
        return 0.0
    }

    internal fun parseLocaleFinancialNumber(value: Any?): Double {
        if (value == null) return 0.0
        if (value is Number) {
            val d = value.toDouble()
            return if (d.isFinite()) d else 0.0
        }
        var raw = value.toString().trim()
        if (raw.isBlank() || raw == "--" || raw == "-" || raw.equals("null", ignoreCase = true)) return 0.0
        raw = raw.replace("−", "-")
        val upper = raw.uppercase(Locale.ROOT)
        val multiplier = when {
            upper.contains("TRILH") || Regex("\\bT\\b").containsMatchIn(upper) -> 1_000_000_000_000.0
            upper.contains("BILH") || Regex("\\bB\\b").containsMatchIn(upper) -> 1_000_000_000.0
            upper.contains("MILH") || Regex("\\bM\\b").containsMatchIn(upper) -> 1_000_000.0
            Regex("\\bK\\b").containsMatchIn(upper) -> 1_000.0
            else -> 1.0
        }
        var s = upper
            .replace("R$", "")
            .replace("US$", "")
            .replace("BRL", "")
            .replace("USD", "")
            .replace("%", "")
            .replace("TRILHÕES", "")
            .replace("TRILHÃO", "")
            .replace("BILHÕES", "")
            .replace("BILHÃO", "")
            .replace("MILHÕES", "")
            .replace("MILHÃO", "")
            .replace(Regex("[^0-9,.-]"), "")
        if (s.isBlank() || s == "-" || s == "." || s == ",") return 0.0
        val lastComma = s.lastIndexOf(',')
        val lastDot = s.lastIndexOf('.')
        s = when {
            lastComma >= 0 && lastDot >= 0 && lastComma > lastDot -> s.replace(".", "").replace(',', '.')
            lastComma >= 0 && lastDot >= 0 -> s.replace(",", "")
            lastComma >= 0 && s.indexOf(',') != lastComma -> s.replace(",", "")
            lastComma >= 0 -> s.replace('.', '\u0000').replace(',', '.').replace("\u0000", "")
            lastDot >= 0 && s.indexOf('.') != lastDot -> s.replace(".", "")
            else -> s
        }
        return (s.toDoubleOrNull() ?: 0.0) * multiplier
    }

    private fun firstArray(vararg arrays: JSONArray?): JSONArray? {
        return arrays.firstOrNull { it != null && it.length() > 0 }
    }

    private fun firstObject(vararg objects: JSONObject?): JSONObject? {
        return objects.firstOrNull { it != null && it.length() > 0 }
    }

    private fun filterChartSeriesByKeywords(series: JSONArray?, vararg keywords: String): JSONArray? {
        if (series == null || series.length() == 0) return null
        val out = JSONArray()
        val normalizedKeywords = keywords.map { canonicalKey(it) }.filter { it.isNotBlank() }
        for (i in 0 until series.length()) {
            val item = series.optJSONObject(i) ?: continue
            val haystack = canonicalKey(
                listOf(
                    item.optAny("key"), item.optAny("name"), item.optAny("label"), item.optAny("title"),
                    item.optAny("sourcePath"), item.optAny("sourceFormat")
                ).joinToString(" ") { firstText(it) }
            )
            if (normalizedKeywords.any { haystack.contains(it) }) out.put(item)
        }
        return if (out.length() > 0) out else null
    }

    private fun parseProfitabilityReturnsFromChartSeries(series: JSONArray?): Pair<List<AssetPeriodReturn>, List<AssetPeriodReturn>> {
        val nominal = mutableListOf<AssetPeriodReturn>()
        val real = mutableListOf<AssetPeriodReturn>()
        if (series == null || series.length() == 0) return nominal to real
        for (sIdx in 0 until series.length()) {
            val item = series.optJSONObject(sIdx) ?: continue
            val rawName = firstText(item.optAny("key"), item.optAny("name"), item.optAny("label"), item.optAny("title"))
            val name = canonicalKey(rawName)
            val isReturnSeries = name.contains("rentabilidade") || name.contains("return") || name.contains("performance") || name.contains("variacao") || name.contains("ipca")
            if (!isReturnSeries) continue
            val kind = if (name.contains("real") || name.contains("ipca") || name.contains("inflacao") || name.contains("inflação")) "real" else "nominal"
            val points = firstArray(item.optJSONArray("points"), item.optJSONArray("data"), item.optJSONArray("values"), item.optJSONArray("items")) ?: continue
            for (i in 0 until points.length()) {
                val point = points.opt(i)
                val label = when (point) {
                    is JSONObject -> firstText(point.optAny("label"), point.optAny("period"), point.optAny("periodo"), point.optAny("date"), point.optAny("x"), "P${i + 1}")
                    is JSONArray -> firstText(point.opt(0), "P${i + 1}")
                    else -> "P${i + 1}"
                }
                val value = when (point) {
                    is JSONObject -> firstNumber(point.optAny("valuePercent"), point.optAny("percent"), point.optAny("percentage"), point.optAny("y"), point.optAny("value"), point.optAny("valor"))
                    is JSONArray -> firstNumber(if (point.length() > 1) point.opt(1) else point.opt(0))
                    else -> firstNumber(point)
                }
                if (label.isNotBlank() && value != 0.0 && value.isFinite()) {
                    val ret = AssetPeriodReturn(period = label, valuePercent = value, label = label, kind = kind)
                    if (kind == "real") real.add(ret) else nominal.add(ret)
                }
            }
        }
        return nominal to real
    }


    private fun humanizeKey(key: String): String {
        val spaced = key
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
        return spaced.split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ") { part ->
            part.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    private fun formatCapabilityValue(value: Any?): String {
        if (value == null) return ""
        return when (value) {
            is JSONObject -> firstText(
                value.optAny("display"),
                value.optAny("label"),
                value.optAny("value"),
                value.optAny("score"),
                value.optAny("status"),
                value.optAny("message")
            )
            is JSONArray -> if (value.length() > 0) "${value.length()} itens" else ""
            is Number -> {
                val d = value.toDouble()
                if (!d.isFinite()) "" else if (kotlin.math.abs(d) >= 1000.0 || d % 1.0 == 0.0) String.format(Locale("pt", "BR"), "%,.0f", d) else String.format(Locale("pt", "BR"), "%.2f", d)
            }
            is Boolean -> if (value) "Sim" else "Não"
            else -> value.toString().trim().take(160)
        }
    }

    private fun capabilityRowsFromJsonObject(obj: JSONObject?, limit: Int = 10): List<ProxyCapabilityRow> {
        if (obj == null) return emptyList()
        val priority = listOf(
            "score", "value", "grade", "status", "label", "rating", "percent", "percentage", "completeness",
            "coverage", "quality", "risk", "recommendation", "action", "message", "summary", "description",
            "dy", "dividendYield", "pvp", "pl", "roe", "roic", "vacancy", "vacancia", "income", "monthlyIncomeEstimated"
        )
        val rows = mutableListOf<ProxyCapabilityRow>()
        fun addKey(key: String) {
            if (rows.size >= limit || !obj.has(key) || obj.isNull(key)) return
            val raw = obj.opt(key)
            if (raw is JSONObject && raw.length() > 0) {
                val v = formatCapabilityValue(raw)
                val detail = firstText(raw.optAny("message"), raw.optAny("description"), raw.optAny("hint"), raw.optAny("source"))
                val score = firstNumber(raw.optAny("score"), raw.optAny("value"), raw.optAny("percent"), raw.optAny("percentage"))
                if (v.isNotBlank() || detail.isNotBlank()) rows.add(ProxyCapabilityRow(humanizeKey(key), v, detail, score, firstText(raw.optAny("status"), raw.optAny("grade"))))
            } else if (raw is JSONArray) {
                if (raw.length() > 0) rows.add(ProxyCapabilityRow(humanizeKey(key), "${raw.length()} itens"))
            } else {
                val v = formatCapabilityValue(raw)
                if (v.isNotBlank() && v != "{}" && v != "[]") rows.add(ProxyCapabilityRow(humanizeKey(key), v))
            }
        }
        priority.forEach(::addKey)
        val keys = obj.keys().asSequence().toList().sorted()
        for (key in keys) {
            if (rows.size >= limit) break
            if (key in priority) continue
            if (key.equals("data", true) || key.equals("results", true) || key.equals("normalized", true)) continue
            addKey(key)
        }
        return rows.distinctBy { it.label.lowercase(Locale.ROOT) to it.value }.take(limit)
    }

    private fun capabilityRowsFromArray(arr: JSONArray?, limit: Int = 8): List<ProxyCapabilityRow> {
        if (arr == null) return emptyList()
        val out = mutableListOf<ProxyCapabilityRow>()
        for (i in 0 until arr.length().coerceAtMost(limit)) {
            val raw = arr.opt(i)
            if (raw is JSONObject) {
                val label = firstText(
                    raw.optAny("ticker"), raw.optAny("symbol"), raw.optAny("title"), raw.optAny("name"),
                    raw.optAny("label"), raw.optAny("code"), raw.optAny("field"), raw.optAny("type"), "Item ${i + 1}"
                )
                val value = firstText(
                    raw.optAny("displayValue"), raw.optAny("valueDisplay"), raw.optAny("value"), raw.optAny("score"),
                    raw.optAny("percent"), raw.optAny("status"), raw.optAny("action"), raw.optAny("recommendation")
                )
                val detail = firstText(raw.optAny("message"), raw.optAny("reason"), raw.optAny("description"), raw.optAny("detail"), raw.optAny("source"))
                out.add(ProxyCapabilityRow(label = label, value = value, detail = detail, score = firstNumber(raw.optAny("score"), raw.optAny("value"), raw.optAny("percent")), status = firstText(raw.optAny("status"), raw.optAny("grade"))))
            } else {
                val text = firstText(raw)
                if (text.isNotBlank()) out.add(ProxyCapabilityRow("Item ${i + 1}", text))
            }
        }
        return out.filter { it.label.isNotBlank() || it.value.isNotBlank() || it.detail.isNotBlank() }
    }

    private fun warningStringsFromJson(obj: JSONObject?): List<String> {
        if (obj == null) return emptyList()
        val arr = firstArray(obj.optJSONArray("warnings"), obj.optJSONArray("alerts"), obj.optJSONArray("insights"), obj.optJSONArray("errors"))
            ?: return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until arr.length().coerceAtMost(6)) {
            val raw = arr.opt(i)
            val item = raw as? JSONObject
            val text = firstText(item?.optAny("message"), item?.optAny("text"), item?.optAny("description"), item?.optAny("error"), raw)
            if (text.isNotBlank()) out.add(text)
        }
        return out
    }

    private fun parseCapabilitySection(title: String, endpoint: String, json: JSONObject?, subtitle: String = ""): ProxyCapabilitySection? {
        val root = unwrapValoraePayload(json) ?: return null
        val main = firstObject(
            root.optJSONObject("summary"),
            root.optJSONObject("quality"),
            root.optJSONObject("score"),
            root.optJSONObject("analysis"),
            root.optJSONObject("data"),
            root.optJSONObject("result"),
            root.optJSONObject("results"),
            root.optJSONObject("coverage"),
            root.optJSONObject("status"),
            root
        ) ?: root
        val rows = mutableListOf<ProxyCapabilityRow>()
        rows.addAll(capabilityRowsFromJsonObject(main, 8))
        val arrayCandidates = listOf(
            root.optJSONArray("items"), root.optJSONArray("ranking"), root.optJSONArray("actions"), root.optJSONArray("events"),
            root.optJSONArray("dividends"), root.optJSONArray("fields"), root.optJSONArray("sources"), root.optJSONArray("peers"),
            root.optJSONArray("statements"), root.optJSONArray("communications"), root.optJSONArray("checklist"),
            root.optObject("actionPlan")?.optJSONArray("items"), root.optObject("rebalance")?.optJSONArray("actions"),
            root.optObject("sourceMap")?.optJSONArray("fields")
        ).firstOrNull { it != null && it.length() > 0 }
        rows.addAll(capabilityRowsFromArray(arrayCandidates, 8))
        val warnings = warningStringsFromJson(root)
        val state = firstText(root.optAny("status"), root.optAny("state"), main.optAny("status"), if (rows.isNotEmpty()) "OK" else "Sem dados")
        if (rows.isEmpty() && warnings.isEmpty()) return null
        return ProxyCapabilitySection(
            title = title,
            subtitle = subtitle.ifBlank { firstText(root.optAny("description"), root.optAny("message"), root.optAny("summary")) },
            endpoint = endpoint,
            status = state,
            rows = rows.distinctBy { it.label to it.value to it.detail }.take(14),
            warnings = warnings
        )
    }

    private fun getCapabilitySection(
        title: String,
        endpoint: String,
        params: Map<String, String?> = emptyMap(),
        subtitle: String = "",
        bypassCache: Boolean = false
    ): ProxyCapabilitySection? {
        val cacheKey = endpointCacheKey("cap_get", endpoint, params)
        if (!bypassCache) getFromCache<ProxyCapabilitySection>(cacheKey)?.let { return it }
        val parsed = parseCapabilitySection(title, endpoint, getProxyJson(endpoint, params), subtitle)
        if (parsed != null) {
            putInCache(cacheKey, parsed, stableEndpointTtlMinutes(endpoint))
            return parsed
        }
        return getFromCache<ProxyCapabilitySection>(cacheKey, allowExpired = true)?.let { stale ->
            stale.copy(
                status = "Cache local",
                warnings = (listOf("Endpoint lento/indisponível agora; exibindo último bloco estável salvo em memória.") + stale.warnings).distinct().take(8)
            )
        }
    }

    private fun postCapabilitySection(
        title: String,
        endpoint: String,
        payload: JSONObject,
        fallbackParams: Map<String, String?> = emptyMap(),
        subtitle: String = "",
        bypassCache: Boolean = false
    ): ProxyCapabilitySection? {
        val cacheKey = endpointCacheKey("cap_post", endpoint, fallbackParams, payload.toString())
        if (!bypassCache) getFromCache<ProxyCapabilitySection>(cacheKey)?.let { return it }
        val parsed = parseCapabilitySection(title, endpoint, postProxyJson(endpoint, payload), subtitle)
            ?: parseCapabilitySection(title, endpoint, getProxyJson(endpoint, fallbackParams), subtitle)
        if (parsed != null) {
            putInCache(cacheKey, parsed, stableEndpointTtlMinutes(endpoint))
            return parsed
        }
        return getFromCache<ProxyCapabilitySection>(cacheKey, allowExpired = true)?.let { stale ->
            stale.copy(
                status = "Cache local",
                warnings = (listOf("Endpoint lento/indisponível agora; exibindo último bloco estável salvo em memória.") + stale.warnings).distinct().take(8)
            )
        }
    }

    /**
     * Combina objetos JSON parciais preservando a precedência dos primeiros argumentos.
     * O Proxy pode devolver parte dos campos em root.normalized e parte em results.normalized;
     * usar apenas o primeiro objeto fazia muitos indicadores/gráficos desaparecerem na UI.
     */
    private fun mergedObject(vararg objects: JSONObject?): JSONObject? {
        val out = JSONObject()
        var hasAny = false
        for (obj in objects.reversed()) {
            if (obj == null || obj.length() == 0) continue
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (!obj.isNull(key)) {
                    out.put(key, obj.opt(key))
                    hasAny = true
                }
            }
        }
        return if (hasAny) out else null
    }

    private fun canonicalKey(raw: String): String {
        return Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "")
    }

    private fun JSONArray.toJsonObjectList(): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        for (i in 0 until length()) {
            optJSONObject(i)?.let { out.add(it) }
        }
        return out
    }

    private fun keyValuesArray(section: JSONObject?): JSONArray? {
        return firstArray(section?.optJSONArray("keyValues"), section?.optJSONArray("items"), section?.optJSONArray("values"))
    }

    private fun textSection(section: JSONObject?): String {
        return firstText(section?.optAny("text"), section?.optAny("rawText"), section?.optAny("content"))
    }

    private fun parsePercentSeriesFromText(text: String, allowedNames: Set<String> = emptySet()): List<AssetComparisonSeries> {
        if (text.isBlank()) return emptyList()
        val knownNames = listOf(
            "PETRÓLEO BRENT" to "BRENT",
            "BRENT" to "BRENT",
            "OURO" to "OURO",
            "MILHO" to "MILHO",
            "CAFÉ" to "CAFÉ",
            "COBRE" to "COBRE",
            "IBOV" to "IBOV",
            "IBOVESPA" to "IBOV",
            "IFIX" to "IFIX",
            "CDI" to "CDI",
            "IPCA" to "IPCA"
        )
        val normalizedText = text.uppercase(Locale.ROOT)
        val out = mutableListOf<AssetComparisonSeries>()
        for ((needle, label) in knownNames) {
            if (allowedNames.isNotEmpty() && !allowedNames.contains(label)) continue
            val idx = normalizedText.indexOf(needle)
            if (idx < 0) continue
            val tail = normalizedText.substring(idx, kotlin.math.min(normalizedText.length, idx + 80))
            val match = Regex("(-?\\d{1,3}(?:[.,]\\d{1,2})?)\\s*%").find(tail)
            val value = firstNumber(match?.groupValues?.getOrNull(1))
            if (value != 0.0) {
                out.add(AssetComparisonSeries(label, listOf(AssetComparisonPoint("Base", 0.0), AssetComparisonPoint("12M", value))))
            }
        }
        return out.distinctBy { it.name }
    }

    private fun sanitizeSeriesName(raw: String, ticker: String): String {
        val clean = raw.trim().uppercase(Locale.ROOT)
            .replace("IBOVESPA", "IBOV")
            .replace("^BVSP", "IBOV")
            .replace("IFIX_PROXY", "IFIX")
        return when {
            clean.isBlank() || clean == "ATIVO" || clean == "TICKER" || clean == "ASSET" || clean == "COTA" || clean == "AÇÃO" -> ticker.uppercase(Locale.ROOT)
            clean.contains("IBOV") -> "IBOV"
            clean.contains("IFIX") -> "IFIX"
            clean.contains("CDI") -> "CDI"
            clean.contains("IPCA") -> "IPCA"
            clean.contains("BRENT") -> "BRENT"
            else -> clean.take(24)
        }
    }

    private fun comparisonPointFromJson(point: JSONObject?, fallbackLabel: String): AssetComparisonPoint? {
        if (point == null) return null
        val label = firstText(
            point.optAny("label"),
            point.optAny("dateLabel"),
            point.optAny("date"),
            point.optAny("data"),
            point.optAny("month"),
            point.optAny("period"),
            point.optAny("x"),
            fallbackLabel
        )
        val value = firstNumber(
            point.optAny("returnPercent"),
            point.optAny("accumulatedPercent"),
            point.optAny("accumulated"),
            point.optAny("variationPercent"),
            point.optAny("percent"),
            point.optAny("percentage"),
            point.optAny("valuePercent"),
            point.optAny("value"),
            point.optAny("valor"),
            point.optAny("close"),
            point.optAny("price"),
            point.optAny("preco"),
            point.optAny("y")
        )
        val ts = parseFlexibleDateMillis(firstText(point.optAny("timestamp"), point.optAny("time"), point.optAny("date"), point.optAny("data"), point.optAny("x")))
        return if (label.isNotBlank() || ts > 0L || value != 0.0) {
            AssetComparisonPoint(
                label = if (label.isNotBlank()) label else fallbackLabel,
                value = value,
                dateMillis = ts
            )
        } else null
    }

    private fun comparisonSeriesFromArray(name: String, arr: JSONArray?, ticker: String): AssetComparisonSeries? {
        if (arr == null || arr.length() == 0) return null
        val points = mutableListOf<AssetComparisonPoint>()
        for (i in 0 until arr.length()) {
            when (val raw = arr.opt(i)) {
                is JSONObject -> comparisonPointFromJson(raw, "P${i + 1}")?.let { points.add(it) }
                is Number -> points.add(AssetComparisonPoint("P${i + 1}", raw.toDouble()))
                is String -> {
                    val n = firstNumber(raw)
                    if (n != 0.0) points.add(AssetComparisonPoint("P${i + 1}", n))
                }
            }
        }
        val cleanPoints = points.filter { it.value.isFinite() }
        return if (cleanPoints.size >= 2) AssetComparisonSeries(sanitizeSeriesName(name, ticker), cleanPoints) else null
    }

    private fun parseComparisonSeriesFromObject(source: JSONObject?, ticker: String): List<AssetComparisonSeries> {
        if (source == null || source.length() == 0) return emptyList()
        val out = linkedMapOf<String, AssetComparisonSeries>()

        fun add(series: AssetComparisonSeries?) {
            if (series == null || series.points.size < 2) return
            val key = sanitizeSeriesName(series.name, ticker)
            val cleaned = series.copy(name = key, points = series.points.filter { it.value.isFinite() })
            if (cleaned.points.size >= 2 && (out[key]?.points?.size ?: 0) < cleaned.points.size) out[key] = cleaned
        }

        val containers = listOfNotNull(
            source,
            source.optJSONObject("data"),
            source.optJSONObject("results"),
            source.optJSONObject("comparison"),
            source.optJSONObject("comparacao"),
            source.optJSONObject("comparacaoIndices"),
            source.optJSONObject("indexComparison"),
            source.optJSONObject("indicesComparison"),
            source.optJSONObject("rentabilidade"),
            source.optJSONObject("rentabilidadeChart"),
            source.optJSONObject("chart"),
            source.optJSONObject("charts")
        )

        for (obj in containers) {
            val namedKeys = listOf(
                "ativo" to ticker,
                "asset" to ticker,
                "ticker" to ticker,
                ticker.lowercase(Locale.ROOT) to ticker,
                ticker.uppercase(Locale.ROOT) to ticker,
                "ibov" to "IBOV",
                "IBOV" to "IBOV",
                "ibovespa" to "IBOV",
                "ifix" to "IFIX",
                "IFIX" to "IFIX",
                "cdi" to "CDI",
                "CDI" to "CDI",
                "ipca" to "IPCA",
                "IPCA" to "IPCA"
            )
            for ((key, name) in namedKeys) add(comparisonSeriesFromArray(name, obj.optJSONArray(key), ticker))

            val seriesArr = firstArray(obj.optJSONArray("series"), obj.optJSONArray("items"), obj.optJSONArray("datasets"), obj.optJSONArray("comparisons"), obj.optJSONArray("indices"))
            if (seriesArr != null) {
                for (i in 0 until seriesArr.length()) {
                    val item = seriesArr.optJSONObject(i) ?: continue
                    val name = firstText(
                        item.optAny("name"),
                        item.optAny("label"),
                        item.optAny("ticker"),
                        item.optAny("symbol"),
                        item.optAny("indice"),
                        item.optAny("index"),
                        "S${i + 1}"
                    )
                    val arr = firstArray(
                        item.optJSONArray("points"),
                        item.optJSONArray("data"),
                        item.optJSONArray("series"),
                        item.optJSONArray("history"),
                        item.optJSONArray("prices"),
                        item.optJSONArray("values"),
                        item.optJSONArray("items")
                    )
                    add(comparisonSeriesFromArray(name, arr, ticker))
                }
            }

            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = obj.optJSONArray(key)
                if (arr != null) add(comparisonSeriesFromArray(key, arr, ticker))
            }
        }

        if (out.isEmpty()) {
            parsePercentSeriesFromText(textSection(source), setOf("IBOV", "IFIX", "CDI", "IPCA")).forEach { add(it) }
        }
        return out.values.mapNotNull { normalizeComparisonSeries(it, ticker) }
    }

    private fun parseComparisonSeriesFromAny(source: Any?, ticker: String): List<AssetComparisonSeries> {
        return when (source) {
            is JSONObject -> parseComparisonSeriesFromObject(source, ticker)
            is JSONArray -> parseComparisonSeriesFromObject(JSONObject().put("series", source), ticker)
            else -> emptyList()
        }
    }

    private fun returnSeriesFromPriceHistory(name: String, priceHistory: List<ChartPoint>): AssetComparisonSeries? {
        val sorted = priceHistory.filter { it.close > 0.0 && it.close.isFinite() }.sortedBy { it.timestamp }
        if (sorted.size < 2) return null
        val base = sorted.first().close.takeIf { it > 0.0 } ?: return null
        val points = sorted.map { point ->
            val pct = ((point.close / base) - 1.0) * 100.0
            AssetComparisonPoint(
                label = point.dateLabel.ifBlank { "P" },
                value = pct,
                dateMillis = if (point.timestamp > 10_000_000_000L) point.timestamp else point.timestamp * 1000L
            )
        }
        return AssetComparisonSeries(name, points)
    }

    private fun normalizeComparisonSeries(series: AssetComparisonSeries, ticker: String): AssetComparisonSeries? {
        val name = sanitizeSeriesName(series.name, ticker)
        val points = series.points
            .filter { it.value.isFinite() }
            .sortedWith(compareBy<AssetComparisonPoint> { if (it.dateMillis > 0L) it.dateMillis else Long.MAX_VALUE }.thenBy { it.label })
        if (points.size < 2) return null

        val first = points.first().value
        val looksAlreadyReturnPercent = kotlin.math.abs(first) <= 0.0001 || points.any { kotlin.math.abs(it.value) <= 0.0001 }
        val normalizedPoints = if (looksAlreadyReturnPercent || first == 0.0) {
            points
        } else {
            // Alguns formatos do /api/compare ou de seções do Investidor10 entregam preço/nível bruto
            // (ex.: PETR4=42, IBOV=130000). A UI de Comparação precisa retorno percentual
            // normalizado no ponto inicial, não escala bruta de preço/índice.
            points.map { point ->
                point.copy(value = ((point.value / first) - 1.0) * 100.0)
            }
        }
        val cleaned = normalizedPoints.filter { it.value.isFinite() }
        return if (cleaned.size >= 2) AssetComparisonSeries(name, cleaned) else null
    }

    private fun mergeComparisonSeries(primary: List<AssetComparisonSeries>, fallback: List<AssetComparisonSeries>, ticker: String): List<AssetComparisonSeries> {
        val out = linkedMapOf<String, AssetComparisonSeries>()
        fun put(s: AssetComparisonSeries) {
            val normalized = normalizeComparisonSeries(s, ticker) ?: return
            val name = normalized.name
            if ((out[name]?.points?.size ?: 0) < normalized.points.size) out[name] = normalized
        }
        primary.forEach(::put)
        fallback.forEach(::put)
        return out.values.toList()
    }

    private fun fetchProxyComparisonSeries(ticker: String, isFii: Boolean, range: String, assetHistory: List<ChartPoint>): List<AssetComparisonSeries> {
        val normalizedRange = normalizeProxyRange(range)
        val cacheKey = "index_compare_${ticker.uppercase(Locale.ROOT)}_${normalizedRange}_${if (isFii) "FII" else "ACAO"}"
        getFromCache<List<AssetComparisonSeries>>(cacheKey)?.let { return it }

        val indexList = if (isFii) listOf("IFIX", "CDI", "IPCA") else listOf("IBOV", "IFIX", "CDI", "IPCA")
        val compareRoot = unwrapValoraePayload(
            getProxyJson(
                "/api/v1/compare",
                mapOf(
                    "tickers" to (listOf(ticker) + indexList).joinToString(","),
                    "range" to normalizedRange,
                    "view" to "standard",
                    "profile" to "fast"
                )
            )
        )
        val fromCompare = parseComparisonSeriesFromObject(compareRoot, ticker)
        val built = mutableListOf<AssetComparisonSeries>()
        returnSeriesFromPriceHistory(ticker, assetHistory)?.let { built.add(it) }

        if (fromCompare.count { it.points.size >= 2 } < 2) {
            val indexHistoryTargets = if (isFii) listOf("IFIX") else listOf("IBOV", "IFIX")
            for (idx in indexHistoryTargets) {
                val points = runCatching { fetchHistoricalChart(idx, normalizedRange) }.getOrDefault(emptyList())
                returnSeriesFromPriceHistory(idx, points)?.let { built.add(it) }
            }
        }

        if (fromCompare.none { it.name.equals("IPCA", true) }) {
            val months = when (normalizedRange.uppercase(Locale.ROOT)) {
                "1D", "5D", "1M" -> 1
                "3M" -> 3
                "6M" -> 6
                "YTD", "1Y" -> 12
                "3Y" -> 36
                "5Y" -> 60
                "10Y" -> 120
                "MAX" -> 180
                else -> 60
            }
            val ipca = fetchIpcaSeries(months)
            if (ipca.size >= 2) {
                built.add(AssetComparisonSeries("IPCA", ipca.map { AssetComparisonPoint(it.dateLabel, it.accumulatedPercent, dateMillis = it.timestamp * 1000L) }))
            }
        }

        val merged = mergeComparisonSeries(fromCompare, built, ticker)
        if (merged.isNotEmpty()) putInCache(cacheKey, merged, 10)
        return merged
    }

    private fun parseBreakdownMap(source: JSONObject?): MutableMap<String, List<AssetBreakdownPoint>> {
        val out = mutableMapOf<String, List<AssetBreakdownPoint>>()
        if (source == null || source.length() == 0) return out

        // 1) Formatos diretos de bibliotecas de gráfico usados pelo Investidor10/Proxy:
        // Apex/Chart.js: { labels: [...], series/data: [...] }
        // Highcharts: { series: [{ data: [{ name, y }, ...] }] }
        // Objeto simples: { "Brasil": 95, "Exterior": 5 }
        if (shouldParseObjectAsSingleBreakdown(source)) {
            parseBreakdownPointsFromAny(source, "Atual").takeIf { it.isNotEmpty() }?.let { direct ->
                out[detectBreakdownYear(source, "Atual")] = direct
                return out
            }
        }

        // 2) Formato por ano: { "2024": [..] } ou { "2024": { "Brasil": { value: .. } } }
        val years = source.keys()
        while (years.hasNext()) {
            val yr = years.next()
            val raw = source.optAny(yr) ?: continue
            val parsed = parseBreakdownPointsFromAny(raw, yr)
            if (parsed.isNotEmpty()) out[yr] = parsed
        }
        return out
    }

    private fun parseBreakdownPointsFromAny(source: Any?, year: String = "Atual"): List<AssetBreakdownPoint> {
        return when (source) {
            is JSONArray -> parseBreakdownPointsFromArray(source, year)
            is JSONObject -> parseBreakdownPointsFromObject(source, year)
            is String -> parseBreakdownPointsFromText(source, year)
            else -> emptyList()
        }.let { sanitizeBreakdownPoints(it, year) }
    }

    private fun shouldParseObjectAsSingleBreakdown(obj: JSONObject): Boolean {
        val directKeys = setOf(
            "labels", "categories", "names", "series", "data", "datasets", "values", "percentages", "shares",
            "keyValues", "text", "rawText", "content", "chart", "pie", "payload", "breakdown", "revenue", "faturamento", "result"
        )
        val keys = mutableListOf<String>()
        val iterator = obj.keys()
        while (iterator.hasNext()) keys.add(iterator.next())
        if (keys.any { it in directKeys }) return true
        if (keys.isEmpty()) return false
        val yearLikeCount = keys.count { isBreakdownYearKey(it) }
        if (yearLikeCount > 0 && yearLikeCount == keys.size) return false
        return true
    }

    private fun isBreakdownYearKey(key: String): Boolean {
        val clean = key.trim()
        val year = clean.toIntOrNull()
        return year != null && year in 1990..2100
    }


    private fun parseBreakdownPointsFromArray(arr: JSONArray, year: String): List<AssetBreakdownPoint> {
        if (arr.length() == 0) return emptyList()
        val out = mutableListOf<AssetBreakdownPoint>()
        for (i in 0 until arr.length()) {
            val item = arr.opt(i)
            when (item) {
                is JSONObject -> {
                    // Highcharts pie: { name, y } / { label, value } / { region, percent }
                    val nestedData = firstArray(item.optJSONArray("data"), item.optJSONArray("points"), item.optJSONArray("items"), item.optJSONArray("values"))
                    if (nestedData != null && item.optJSONObject("data") == null) {
                        val seriesName = firstText(item.optAny("name"), item.optAny("label"), item.optAny("title"))
                        val nested = parseBreakdownPointsFromArray(nestedData, year)
                        if (nested.isNotEmpty() && nested.any { it.name != seriesName }) {
                            out.addAll(nested)
                            continue
                        }
                    }
                    val name = firstText(
                        item.optAny("name"), item.optAny("label"), item.optAny("title"),
                        item.optAny("region"), item.optAny("regiao"), item.optAny("geography"), item.optAny("geo"),
                        item.optAny("business"), item.optAny("negocio"), item.optAny("segment"), item.optAny("segmento"),
                        item.optAny("category"), item.optAny("categoria")
                    )
                    val value = firstNumber(
                        item.optAny("valuePercent"), item.optAny("percent"), item.optAny("percentage"), item.optAny("share"),
                        item.optAny("y"), item.optAny("value"), item.optAny("valor"), item.optAny("amount"), item.optAny("total")
                    )
                    val display = firstText(item.optAny("displayValue"), item.optAny("display"), item.optAny("revenue"), item.optAny("faturamento"), item.optAny("formatted"))
                    if (name.isNotBlank() && value != 0.0) out.add(AssetBreakdownPoint(name, value, display, year))
                }
                is Number -> out.add(AssetBreakdownPoint("Item ${i + 1}", item.toDouble(), year = year))
                is String -> {
                    val value = firstNumber(item)
                    if (value != 0.0) out.add(AssetBreakdownPoint("Item ${i + 1}", value, item, year))
                }
            }
        }
        return out
    }

    private fun parseBreakdownPointsFromObject(obj: JSONObject, year: String): List<AssetBreakdownPoint> {
        val out = mutableListOf<AssetBreakdownPoint>()

        // Sections resumidas pelo engine: { text, keyValues: [{label,value}] }
        keyValuesArray(obj)?.let { kv ->
            for (item in kv.toJsonObjectList()) {
                val name = firstText(item.optAny("label"), item.optAny("name"), item.optAny("key"), item.optAny("title"))
                val rawValue = item.optAny("value") ?: item.optAny("valor") ?: item.optAny("percent") ?: item.optAny("percentage")
                val value = firstNumber(rawValue)
                if (name.isNotBlank() && value != 0.0) out.add(AssetBreakdownPoint(name, value, firstText(rawValue), year))
            }
            if (out.isNotEmpty()) return out
        }

        // Apex/Chart.js: labels/categories + series/data/values
        val labels = firstArray(obj.optJSONArray("labels"), obj.optJSONArray("categories"), obj.optJSONArray("names"))
        val values = firstArray(obj.optJSONArray("series"), obj.optJSONArray("data"), obj.optJSONArray("values"), obj.optJSONArray("percentages"), obj.optJSONArray("shares"))
        if (labels != null && values != null) {
            val flatValues = flattenBreakdownNumericArray(values)
            val len = minOf(labels.length(), flatValues.size)
            for (i in 0 until len) {
                val name = firstText(labels.opt(i))
                val value = flatValues[i]
                if (name.isNotBlank() && value != 0.0) out.add(AssetBreakdownPoint(name, value, year = year))
            }
            if (out.isNotEmpty()) return out
        }

        // Chart.js: { datasets: [{ data: [...] }], labels: [...] }
        val datasets = obj.optJSONArray("datasets")
        if (labels != null && datasets != null && datasets.length() > 0) {
            val firstDataset = datasets.optJSONObject(0)
            val data = firstDataset?.optJSONArray("data")
            if (data != null) {
                val flatValues = flattenBreakdownNumericArray(data)
                val len = minOf(labels.length(), flatValues.size)
                for (i in 0 until len) {
                    val name = firstText(labels.opt(i))
                    val value = flatValues[i]
                    if (name.isNotBlank() && value != 0.0) out.add(AssetBreakdownPoint(name, value, year = year))
                }
                if (out.isNotEmpty()) return out
            }
        }

        // Highcharts/Apex series com objetos: { series: [{ data: [{ name, y }] }] } ou [{ name, data:[95] }]
        val seriesArr = obj.optJSONArray("series")
        if (seriesArr != null) {
            for (seriesIndex in 0 until seriesArr.length()) {
                val item = seriesArr.opt(seriesIndex)
                when (item) {
                    is JSONObject -> {
                        val data = firstArray(item.optJSONArray("data"), item.optJSONArray("points"), item.optJSONArray("values"))
                        val parsed = parseBreakdownPointsFromArray(data ?: JSONArray(), year)
                        if (parsed.isNotEmpty()) {
                            out.addAll(parsed)
                        } else {
                            val seriesName = firstText(item.optAny("name"), item.optAny("label"))
                            val value = firstNumber(item.optAny("y"), item.optAny("value"), item.optAny("percent"), item.optAny("percentage"), item.optAny("share"))
                                .takeIf { it != 0.0 }
                                ?: firstNumber(data?.opt(0))
                            if (seriesName.isNotBlank() && value != 0.0) out.add(AssetBreakdownPoint(seriesName, value, year = year))
                        }
                    }
                    is Number -> out.add(AssetBreakdownPoint("Série ${seriesIndex + 1}", item.toDouble(), year = year))
                }
            }
            if (out.isNotEmpty()) return out
        }

        // Wrapper comum: { chart: {...} }, { pie: {...} }, { payload: {...} }
        val wrapper = firstObject(
            obj.optJSONObject("chart"), obj.optJSONObject("pie"), obj.optJSONObject("payload"), obj.optJSONObject("breakdown"),
            obj.optJSONObject("revenue"), obj.optJSONObject("faturamento"), obj.optJSONObject("result")
        )
        if (wrapper != null && wrapper !== obj) {
            val parsed = parseBreakdownPointsFromObject(wrapper, year)
            if (parsed.isNotEmpty()) return parsed
        }

        // Texto resumido da seção do Investidor10.
        val textPoints = parseBreakdownPointsFromText(textSection(obj), year)
        if (textPoints.isNotEmpty()) return textPoints

        // Objeto simples: { "Brasil": 95, "Exterior": 5 } ou { "Brasil": { value: 95 } }
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key in setOf("labels", "categories", "names", "series", "data", "datasets", "values", "keyValues", "text", "rawText", "content", "value", "valor", "percent", "percentage", "share", "y", "amount", "total", "displayValue", "display", "revenue", "faturamento")) continue
            val valueAny = obj.optAny(key) ?: continue
            when (valueAny) {
                is JSONObject -> {
                    val nestedParsed = parseBreakdownPointsFromObject(valueAny, key)
                    if (nestedParsed.isNotEmpty() && nestedParsed.any { it.name != key }) {
                        out.addAll(nestedParsed)
                    } else {
                        val value = firstNumber(
                            valueAny.optAny("valuePercent"), valueAny.optAny("percent"), valueAny.optAny("percentage"), valueAny.optAny("share"),
                            valueAny.optAny("y"), valueAny.optAny("value"), valueAny.optAny("valor"), valueAny.optAny("amount"), valueAny.optAny("total")
                        )
                        val display = firstText(valueAny.optAny("displayValue"), valueAny.optAny("display"), valueAny.optAny("revenue"), valueAny.optAny("faturamento"))
                        if (key.isNotBlank() && value != 0.0) out.add(AssetBreakdownPoint(key, value, display, year))
                    }
                }
                is JSONArray -> {
                    val parsed = parseBreakdownPointsFromArray(valueAny, key)
                    if (parsed.isNotEmpty()) out.addAll(parsed)
                }
                else -> {
                    val value = firstNumber(valueAny)
                    if (key.isNotBlank() && value != 0.0) out.add(AssetBreakdownPoint(key, value, firstText(valueAny), year))
                }
            }
        }
        return out
    }

    private fun flattenBreakdownNumericArray(arr: JSONArray): List<Double> {
        val out = mutableListOf<Double>()
        for (i in 0 until arr.length()) {
            when (val item = arr.opt(i)) {
                is Number -> out.add(item.toDouble())
                is String -> firstNumber(item).takeIf { it != 0.0 }?.let(out::add)
                is JSONObject -> {
                    val nested = firstArray(item.optJSONArray("data"), item.optJSONArray("values"), item.optJSONArray("points"))
                    if (nested != null) out.addAll(flattenBreakdownNumericArray(nested))
                    else firstNumber(item.optAny("y"), item.optAny("value"), item.optAny("valor"), item.optAny("percent"), item.optAny("percentage"), item.optAny("share"))
                        .takeIf { it != 0.0 }?.let(out::add)
                }
                is JSONArray -> out.addAll(flattenBreakdownNumericArray(item))
            }
        }
        return out
    }

    private fun parseBreakdownPointsFromText(text: String, year: String): List<AssetBreakdownPoint> {
        if (text.isBlank()) return emptyList()
        val out = mutableListOf<AssetBreakdownPoint>()
        val patterns = listOf(
            Regex("([A-Za-zÀ-ÿ0-9 .&/()ºª-]{2,60})\\s*[:\\-]\\s*([+-]?[0-9]{1,3}(?:[.,][0-9]{1,2})?\\s*%|R\\$\\s*[0-9.,]+(?:\\s*(?:Bilhões|Bilhão|Milhões|Milhão|Trilhões|Trilhão))?)", RegexOption.IGNORE_CASE),
            Regex("([A-Za-zÀ-ÿ0-9 .&/()ºª-]{2,60})\\s+([+-]?[0-9]{1,3}(?:[.,][0-9]{1,2})?\\s*%)", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            for (m in pattern.findAll(text)) {
                val name = m.groupValues.getOrNull(1)?.trim().orEmpty()
                    .replace(Regex("^(e|de|do|da|dos|das)\\s+", RegexOption.IGNORE_CASE), "")
                val raw = m.groupValues.getOrNull(2)?.trim().orEmpty()
                val value = firstNumber(raw)
                if (name.length >= 2 && value != 0.0 && out.none { it.name.equals(name, true) }) {
                    out.add(AssetBreakdownPoint(name.take(42), value, raw, year))
                }
                if (out.size >= 12) return out
            }
            if (out.isNotEmpty()) return out
        }
        return out
    }

    private fun sanitizeBreakdownPoints(points: List<AssetBreakdownPoint>, fallbackYear: String): List<AssetBreakdownPoint> {
        val merged = linkedMapOf<String, AssetBreakdownPoint>()
        for (pt in points) {
            val name = pt.name.trim().replace(Regex("\\s+"), " ")
            val value = pt.valuePercent
            if (name.isBlank() || !value.isFinite() || value == 0.0) continue
            val key = canonicalKey(name)
            if (key.isBlank() || key in setOf("labels", "series", "data", "datasets", "values", "keyvalues", "text")) continue
            val safeValue = kotlin.math.abs(value)
            merged[key] = pt.copy(name = name.take(48), valuePercent = safeValue, year = pt.year.ifBlank { fallbackYear })
        }
        return merged.values.sortedByDescending { it.valuePercent }.take(12)
    }

    private fun detectBreakdownYear(source: JSONObject, fallback: String): String {
        return firstText(source.optAny("year"), source.optAny("ano"), source.optAny("period"), source.optAny("periodo"), fallback)
    }

    private fun hasUsableBreakdownMap(map: Map<String, List<AssetBreakdownPoint>>): Boolean {
        return map.values.flatten().any { it.name.isNotBlank() && it.valuePercent.isFinite() && it.valuePercent > 0.0 }
    }


    private fun parseFirstBreakdownMap(vararg sources: Any?): MutableMap<String, List<AssetBreakdownPoint>> {
        for (source in sources) {
            val parsed = parseBreakdownMapFromAny(source)
            if (hasUsableBreakdownMap(parsed)) return parsed
        }
        return mutableMapOf()
    }

    private fun parseBreakdownMapFromAny(source: Any?): MutableMap<String, List<AssetBreakdownPoint>> {
        val out = mutableMapOf<String, List<AssetBreakdownPoint>>()
        when (source) {
            is JSONObject -> out.putAll(parseBreakdownMap(source))
            is JSONArray -> {
                val points = parseBreakdownPointsFromAny(source, "Atual")
                if (points.isNotEmpty()) out["Atual"] = points
            }
            is String -> {
                val points = parseBreakdownPointsFromAny(source, "Atual")
                if (points.isNotEmpty()) out["Atual"] = points
            }
        }
        return out
    }

    private fun parseFirstFinancialStatementPoints(vararg sources: Any?): List<FinancialStatementPoint> {
        for (source in sources) {
            val parsed = parseFinancialStatementPointsFromAny(source)
            if (parsed.isNotEmpty()) return parsed
        }
        return emptyList()
    }

    private fun parseFinancialStatementPointsFromAny(source: Any?, fallbackYear: String = ""): List<FinancialStatementPoint> {
        return when (source) {
            is JSONArray -> parseFinancialStatementPointsFromArray(source, fallbackYear)
            is JSONObject -> parseFinancialStatementPointsFromObject(source, fallbackYear)
            is String -> parseFinancialStatementPointsFromText(source, fallbackYear)
            else -> emptyList()
        }.let { mergeFinancialStatementPoints(it) }
    }

    private fun parseFinancialStatementPointsFromArray(arr: JSONArray, fallbackYear: String = ""): List<FinancialStatementPoint> {
        val out = mutableListOf<FinancialStatementPoint>()
        for (i in 0 until arr.length()) {
            when (val item = arr.opt(i)) {
                is JSONObject -> {
                    val nested = firstArray(item.optJSONArray("data"), item.optJSONArray("items"), item.optJSONArray("points"), item.optJSONArray("values"))
                    val seriesLabel = firstText(item.optAny("key"), item.optAny("name"), item.optAny("label"), item.optAny("title"), item.optAny("metric"))
                    val seriesField = financialFieldFromLabel(seriesLabel)
                    val direct = parseFinancialStatementPointFromObject(item, fallbackYear.ifBlank { "P${i + 1}" })
                    if (direct != null) out.add(direct)
                    if (nested != null && nested.length() > 0) {
                        if (seriesField.isNotBlank()) {
                            out.addAll(parseFinancialSeriesPoints(nested, seriesField, fallbackYear))
                        } else {
                            out.addAll(parseFinancialStatementPointsFromArray(nested, fallbackYear))
                        }
                    }
                    val nestedObj = firstObject(item.optJSONObject("data"), item.optJSONObject("chart"), item.optJSONObject("payload"), item.optJSONObject("result"))
                    if (nestedObj != null) out.addAll(parseFinancialStatementPointsFromObject(nestedObj, fallbackYear))
                }
                is JSONArray -> out.addAll(parseFinancialStatementPointsFromArray(item, fallbackYear))
            }
        }
        return out
    }

    private fun parseFinancialSeriesPoints(arr: JSONArray, field: String, fallbackYear: String = ""): List<FinancialStatementPoint> {
        val out = mutableListOf<FinancialStatementPoint>()
        for (i in 0 until arr.length()) {
            val item = arr.opt(i)
            val label = when (item) {
                is JSONObject -> firstText(item.optAny("label"), item.optAny("year"), item.optAny("ano"), item.optAny("date"), item.optAny("period"), item.optAny("x"), fallbackYear, "P${i + 1}")
                is JSONArray -> firstText(item.opt(0), fallbackYear, "P${i + 1}")
                else -> firstText(fallbackYear, "P${i + 1}")
            }
            val value = when (item) {
                is JSONObject -> firstNumber(item.optAny("y"), item.optAny("value"), item.optAny("valor"), item.optAny("amount"), item.optAny("total"), item.optAny("close"))
                is JSONArray -> firstNumber(if (item.length() > 1) item.opt(1) else item.opt(0))
                else -> firstNumber(item)
            }
            if (label.isNotBlank() && value != 0.0 && value.isFinite()) {
                val year = firstText(extractYearFromLabel(label), label)
                val base = FinancialStatementPoint(label = label, year = year, quarter = extractQuarterFromLabel(label))
                out.add(applyFinancialField(base, field, value))
            }
        }
        return out
    }

    private fun parseFinancialStatementPointsFromObject(obj: JSONObject, fallbackYear: String = ""): List<FinancialStatementPoint> {
        val out = mutableListOf<FinancialStatementPoint>()

        val direct = parseFinancialStatementPointFromObject(obj, fallbackYear)
        if (direct != null) out.add(direct)

        out.addAll(parseFinancialChartWithLabels(obj))
        out.addAll(parseFinancialNamedArrays(obj))

        val wrappers = listOf("data", "chart", "payload", "result", "results", "response", "incomeStatement", "dre", "receitasLucros", "revenueProfit", "balanceSheet", "balancoPatrimonial")
        for (key in wrappers) {
            val wrappedObj = obj.optJSONObject(key)
            if (wrappedObj != null && wrappedObj.length() > 0) out.addAll(parseFinancialStatementPointsFromObject(wrappedObj, fallbackYear))
            val wrappedArr = obj.optJSONArray(key)
            if (wrappedArr != null && wrappedArr.length() > 0) out.addAll(parseFinancialStatementPointsFromArray(wrappedArr, fallbackYear))
        }

        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key in wrappers || key in setOf("labels", "categories", "years", "series", "datasets", "data", "values", "items", "points")) continue
            val value = obj.optAny(key) ?: continue
            if (isBreakdownYearKey(key) || key.matches(Regex("^(?:[1-4]T|T[1-4]|Q[1-4])[/ -]?[0-9]{2,4}$", RegexOption.IGNORE_CASE))) {
                when (value) {
                    is JSONObject -> out.addAll(parseFinancialStatementPointsFromObject(value, key))
                    is JSONArray -> out.addAll(parseFinancialStatementPointsFromArray(value, key))
                }
            }
        }
        return out
    }

    private fun parseFinancialStatementPointFromObject(obj: JSONObject, fallbackYear: String = ""): FinancialStatementPoint? {
        val rawLabel = firstText(
            obj.optAny("label"), obj.optAny("period"), obj.optAny("periodo"), obj.optAny("date"), obj.optAny("data"),
            obj.optAny("year"), obj.optAny("ano"), fallbackYear
        )
        val year = firstText(obj.optAny("year"), obj.optAny("ano"), extractYearFromLabel(rawLabel), extractYearFromLabel(fallbackYear), fallbackYear.takeIf { isBreakdownYearKey(it) })
        val quarter = firstText(obj.optAny("quarter"), obj.optAny("trimestre"), extractQuarterFromLabel(rawLabel))
        val label = when {
            quarter.isNotBlank() && year.isNotBlank() -> "$quarter/$year"
            rawLabel.isNotBlank() -> rawLabel
            year.isNotBlank() -> year
            else -> ""
        }
        val netRevenue = firstNumber(
            obj.optAny("net_revenue"), obj.optAny("netRevenue"), obj.optAny("revenue"), obj.optAny("revenues"),
            obj.optAny("receitaLiquida"), obj.optAny("receita_liquida"), obj.optAny("receita"), obj.optAny("faturamento"), obj.optAny("sales")
        )
        val netProfit = firstNumber(
            obj.optAny("net_profit"), obj.optAny("netProfit"), obj.optAny("profit"), obj.optAny("profits"),
            obj.optAny("lucroLiquido"), obj.optAny("lucro_liquido"), obj.optAny("lucro"), obj.optAny("earnings")
        )
        val costVal = firstNumber(obj.optAny("cost"), obj.optAny("costs"), obj.optAny("custo"), obj.optAny("custos"), obj.optAny("cpv"))
        val grossProfit = firstNumber(obj.optAny("gross_profit"), obj.optAny("grossProfit"), obj.optAny("lucroBruto"), obj.optAny("lucro_bruto"))
        val ebitda = firstNumber(obj.optAny("ebitda"), obj.optAny("EBITDA"))
        val ebit = firstNumber(obj.optAny("ebit"), obj.optAny("EBIT"))
        val netWorth = firstNumber(obj.optAny("net_worth"), obj.optAny("netWorth"), obj.optAny("patrimonioLiquido"), obj.optAny("patrimonio_liquido"), obj.optAny("pl"), obj.optAny("equity"))
        val totalAssets = firstNumber(obj.optAny("balance_total_assets"), obj.optAny("totalAssets"), obj.optAny("ativos"), obj.optAny("assets"), obj.optAny("ativoTotal"), obj.optAny("totalAtivos"))
        val totalLiabilities = firstNumber(obj.optAny("balance_total_liabilities"), obj.optAny("totalLiabilities"), obj.optAny("passivos"), obj.optAny("liabilities"), obj.optAny("passivoTotal"), obj.optAny("totalPassivos"))
        val hasValues = listOf(netRevenue, netProfit, costVal, grossProfit, ebitda, ebit, netWorth, totalAssets, totalLiabilities).any { it != 0.0 && it.isFinite() }
        if (!hasValues || label.isBlank()) return null
        return FinancialStatementPoint(
            label = label,
            year = year.ifBlank { label },
            quarter = quarter,
            netRevenue = netRevenue,
            cost = costVal,
            grossProfit = grossProfit,
            ebitda = ebitda,
            ebit = ebit,
            netProfit = netProfit,
            netWorth = netWorth,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities
        )
    }

    private fun parseFinancialChartWithLabels(obj: JSONObject): List<FinancialStatementPoint> {
        val labels = firstArray(obj.optJSONArray("labels"), obj.optJSONArray("categories"), obj.optJSONArray("years"), obj.optJSONArray("periods"), obj.optJSONArray("periodos"))
            ?: return emptyList()
        val series = firstArray(obj.optJSONArray("datasets"), obj.optJSONArray("series"))
        val pointsByLabel = linkedMapOf<String, FinancialStatementPoint>()
        fun basePointAt(index: Int): FinancialStatementPoint {
            val label = firstText(labels.opt(index), "P${index + 1}")
            val year = firstText(extractYearFromLabel(label), label)
            val quarter = extractQuarterFromLabel(label)
            return pointsByLabel.getOrPut(label) { FinancialStatementPoint(label = label, year = year, quarter = quarter) }
        }
        fun applyValue(index: Int, field: String, value: Double) {
            if (value == 0.0 || !value.isFinite()) return
            val current = basePointAt(index)
            pointsByLabel[current.label] = applyFinancialField(current, field, value)
        }
        if (series != null) {
            for (sIdx in 0 until series.length()) {
                val item = series.opt(sIdx)
                when (item) {
                    is JSONObject -> {
                        val name = firstText(item.optAny("label"), item.optAny("name"), item.optAny("title"), item.optAny("field"), item.optAny("metric"))
                        val field = financialFieldFromLabel(name)
                        val data = firstArray(item.optJSONArray("data"), item.optJSONArray("values"), item.optJSONArray("points"))
                        if (data != null && field.isNotBlank()) {
                            val flat = flattenFinancialNumericArray(data)
                            val len = minOf(labels.length(), flat.size)
                            for (i in 0 until len) applyValue(i, field, flat[i])
                        } else if (data != null) {
                            for (i in 0 until data.length()) {
                                val objPoint = data.optJSONObject(i) ?: continue
                                parseFinancialStatementPointFromObject(objPoint, firstText(labels.opt(i)))?.let { parsed ->
                                    val merged = mergeFinancialPoint(pointsByLabel[parsed.label], parsed)
                                    if (merged != null) {
                                        pointsByLabel[parsed.label] = merged
                                    }
                                }
                            }
                        }
                    }
                    is JSONArray -> {
                        val flat = flattenFinancialNumericArray(item)
                        val field = when (sIdx) {
                            0 -> "netRevenue"
                            1 -> "netProfit"
                            2 -> "ebitda"
                            else -> ""
                        }
                        if (field.isNotBlank()) {
                            val len = minOf(labels.length(), flat.size)
                            for (i in 0 until len) applyValue(i, field, flat[i])
                        }
                    }
                }
            }
        }
        val directValues = firstArray(obj.optJSONArray("data"), obj.optJSONArray("values"))
        if (pointsByLabel.isEmpty() && directValues != null) {
            val field = financialFieldFromLabel(firstText(obj.optAny("name"), obj.optAny("label"), obj.optAny("title"), obj.optAny("metric"))).ifBlank { "netRevenue" }
            val flat = flattenFinancialNumericArray(directValues)
            val len = minOf(labels.length(), flat.size)
            for (i in 0 until len) applyValue(i, field, flat[i])
        }
        return pointsByLabel.values.toList()
    }

    private fun parseFinancialNamedArrays(obj: JSONObject): List<FinancialStatementPoint> {
        val labels = firstArray(obj.optJSONArray("labels"), obj.optJSONArray("categories"), obj.optJSONArray("years"), obj.optJSONArray("periods"), obj.optJSONArray("periodos"))
            ?: return emptyList()
        val fieldCandidates = linkedMapOf(
            "netRevenue" to listOf("netRevenue", "net_revenue", "revenue", "revenues", "receitaLiquida", "receita", "faturamento"),
            "netProfit" to listOf("netProfit", "net_profit", "profit", "profits", "lucroLiquido", "lucro"),
            "grossProfit" to listOf("grossProfit", "gross_profit", "lucroBruto"),
            "cost" to listOf("cost", "costs", "custo", "custos"),
            "ebitda" to listOf("ebitda", "EBITDA"),
            "ebit" to listOf("ebit", "EBIT"),
            "netWorth" to listOf("netWorth", "net_worth", "patrimonioLiquido", "patrimonio_liquido", "pl", "equity"),
            "totalAssets" to listOf("totalAssets", "balance_total_assets", "ativos", "assets", "ativoTotal", "totalAtivos"),
            "totalLiabilities" to listOf("totalLiabilities", "balance_total_liabilities", "passivos", "liabilities", "passivoTotal", "totalPassivos")
        )
        val pointsByLabel = linkedMapOf<String, FinancialStatementPoint>()
        for ((field, keys) in fieldCandidates) {
            val arr = keys.firstNotNullOfOrNull { obj.optJSONArray(it) } ?: continue
            val flat = flattenFinancialNumericArray(arr)
            val len = minOf(labels.length(), flat.size)
            for (i in 0 until len) {
                val label = firstText(labels.opt(i), "P${i + 1}")
                val year = firstText(extractYearFromLabel(label), label)
                val base = pointsByLabel.getOrPut(label) { FinancialStatementPoint(label = label, year = year, quarter = extractQuarterFromLabel(label)) }
                pointsByLabel[label] = applyFinancialField(base, field, flat[i])
            }
        }
        return pointsByLabel.values.toList()
    }

    private fun parseFinancialStatementPointsFromText(text: String, fallbackYear: String = ""): List<FinancialStatementPoint> {
        if (text.isBlank()) return emptyList()
        val years = Regex("(20[0-9]{2}|19[9][0-9])").findAll(text).map { it.value }.distinct().toList()
        if (years.isEmpty()) return emptyList()
        val out = mutableListOf<FinancialStatementPoint>()
        for (year in years) {
            val window = text.substring(kotlin.math.max(0, text.indexOf(year) - 240), kotlin.math.min(text.length, text.indexOf(year) + 520))
            val revenue = Regex("(?:Receita(?: Líquida)?|Faturamento)\\s*(?:$year)?\\s*(R\\$\\s*[0-9.,]+(?:\\s*(?:Bilhões|Bilhão|Milhões|Milhão))?|[0-9.,]+)", RegexOption.IGNORE_CASE).find(window)?.groupValues?.getOrNull(1)
            val profit = Regex("(?:Lucro(?: Líquido)?)\\s*(?:$year)?\\s*(R\\$\\s*[0-9.,]+(?:\\s*(?:Bilhões|Bilhão|Milhões|Milhão))?|[0-9.,]+)", RegexOption.IGNORE_CASE).find(window)?.groupValues?.getOrNull(1)
            val rev = firstNumber(revenue)
            val prof = firstNumber(profit)
            if (rev != 0.0 || prof != 0.0) out.add(FinancialStatementPoint(label = year, year = year, netRevenue = rev, netProfit = prof))
        }
        return out
    }

    private fun parseFirstProfitVsQuotePoints(vararg sources: Any?): List<AssetComparisonPoint> {
        for (source in sources) {
            val parsed = parseProfitVsQuotePointsFromAny(source)
            if (parsed.isNotEmpty()) return parsed
        }
        return emptyList()
    }

    private fun parseProfitVsQuotePointsFromAny(source: Any?): List<AssetComparisonPoint> {
        return when (source) {
            is JSONObject -> parseProfitVsQuotePointsFromObject(source)
            is JSONArray -> parseProfitVsQuotePointsFromArray(source)
            else -> emptyList()
        }.filter { (it.value != 0.0 || it.secondaryValue != 0.0) && it.label.isNotBlank() }
    }

    private fun parseProfitVsQuotePointsFromArray(arr: JSONArray): List<AssetComparisonPoint> {
        parseProfitVsQuotePointsFromChartSeriesArray(arr).takeIf { it.isNotEmpty() }?.let { return it }
        val out = mutableListOf<AssetComparisonPoint>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val label = firstText(obj.optAny("label"), obj.optAny("year"), obj.optAny("ano"), obj.optAny("date"), obj.optAny("period"), obj.optAny("x"), "P${i + 1}")
            val quote = firstNumber(obj.optAny("quotation"), obj.optAny("quote"), obj.optAny("cotacao"), obj.optAny("price"), obj.optAny("preco"), obj.optAny("value"))
            val profit = firstNumber(obj.optAny("net_profit"), obj.optAny("netProfit"), obj.optAny("profit"), obj.optAny("lucro"), obj.optAny("lucroLiquido"), obj.optAny("secondaryValue"))
            if (quote != 0.0 || profit != 0.0) out.add(AssetComparisonPoint(label, quote, profit))
        }
        return out
    }

    private fun parseProfitVsQuotePointsFromChartSeriesArray(arr: JSONArray): List<AssetComparisonPoint> {
        val byLabel = linkedMapOf<String, AssetComparisonPoint>()
        for (sIdx in 0 until arr.length()) {
            val seriesObj = arr.optJSONObject(sIdx) ?: continue
            val seriesName = canonicalKey(firstText(seriesObj.optAny("key"), seriesObj.optAny("name"), seriesObj.optAny("label"), seriesObj.optAny("title")))
            val field = when {
                seriesName.contains("cotacao") || seriesName.contains("quote") || seriesName.contains("price") || seriesName.contains("preco") -> "quote"
                seriesName.contains("lucro") || seriesName.contains("profit") || seriesName.contains("earnings") -> "profit"
                else -> ""
            }
            if (field.isBlank()) continue
            val points = firstArray(seriesObj.optJSONArray("points"), seriesObj.optJSONArray("data"), seriesObj.optJSONArray("values"), seriesObj.optJSONArray("items")) ?: continue
            for (i in 0 until points.length()) {
                val item = points.opt(i)
                val label = when (item) {
                    is JSONObject -> firstText(item.optAny("label"), item.optAny("year"), item.optAny("ano"), item.optAny("date"), item.optAny("period"), item.optAny("x"), "P${i + 1}")
                    is JSONArray -> firstText(item.opt(0), "P${i + 1}")
                    else -> "P${i + 1}"
                }
                val value = when (item) {
                    is JSONObject -> firstNumber(item.optAny("y"), item.optAny("value"), item.optAny("valor"), item.optAny("amount"), item.optAny("total"), item.optAny("close"))
                    is JSONArray -> firstNumber(if (item.length() > 1) item.opt(1) else item.opt(0))
                    else -> firstNumber(item)
                }
                if (label.isBlank() || value == 0.0 || !value.isFinite()) continue
                val current = byLabel[label] ?: AssetComparisonPoint(label, 0.0, 0.0)
                byLabel[label] = if (field == "quote") current.copy(value = value) else current.copy(secondaryValue = value)
            }
        }
        return byLabel.values.filter { it.value != 0.0 || it.secondaryValue != 0.0 }
    }

    private fun parseProfitVsQuotePointsFromObject(obj: JSONObject): List<AssetComparisonPoint> {
        val out = mutableListOf<AssetComparisonPoint>()
        val labels = firstArray(obj.optJSONArray("labels"), obj.optJSONArray("categories"), obj.optJSONArray("years"), obj.optJSONArray("periods"))
        val series = firstArray(obj.optJSONArray("datasets"), obj.optJSONArray("series"))
        if (labels != null && series != null) {
            val byLabel = linkedMapOf<String, AssetComparisonPoint>()
            for (sIdx in 0 until series.length()) {
                val item = series.optJSONObject(sIdx) ?: continue
                val name = canonicalKey(firstText(item.optAny("label"), item.optAny("name"), item.optAny("title")))
                val data = firstArray(item.optJSONArray("data"), item.optJSONArray("values"), item.optJSONArray("points")) ?: continue
                val flat = flattenFinancialNumericArray(data)
                val len = minOf(labels.length(), flat.size)
                for (i in 0 until len) {
                    val label = firstText(labels.opt(i), "P${i + 1}")
                    val current = byLabel[label] ?: AssetComparisonPoint(label, 0.0, 0.0)
                    byLabel[label] = when {
                        name.contains("cotacao") || name.contains("quote") || name.contains("price") || name.contains("preco") -> current.copy(value = flat[i])
                        name.contains("lucro") || name.contains("profit") || name.contains("earnings") -> current.copy(secondaryValue = flat[i])
                        sIdx == 0 -> current.copy(value = flat[i])
                        else -> current.copy(secondaryValue = flat[i])
                    }
                }
            }
            if (byLabel.isNotEmpty()) return byLabel.values.toList()
        }

        val quotesArr = firstArray(obj.optJSONArray("quotes"), obj.optJSONArray("cotacoes"), obj.optJSONArray("cotações"), obj.optJSONArray("prices"), obj.optJSONArray("precos"))
        val profitsArr = firstArray(obj.optJSONArray("profits"), obj.optJSONArray("lucros"), obj.optJSONArray("netProfit"), obj.optJSONArray("earnings"))
        if (quotesArr != null && profitsArr != null) {
            val len = kotlin.math.min(quotesArr.length(), profitsArr.length())
            for (i in 0 until len) {
                val qObj = quotesArr.optJSONObject(i)
                val pObj = profitsArr.optJSONObject(i)
                val label = firstText(qObj?.optAny("label"), qObj?.optAny("year"), qObj?.optAny("ano"), pObj?.optAny("label"), pObj?.optAny("year"), labels?.opt(i), "P${i + 1}")
                val quote = firstNumber(qObj?.optAny("quotation"), qObj?.optAny("quote"), qObj?.optAny("cotacao"), qObj?.optAny("price"), qObj?.optAny("value"), quotesArr.opt(i))
                val profit = firstNumber(pObj?.optAny("net_profit"), pObj?.optAny("netProfit"), pObj?.optAny("profit"), pObj?.optAny("lucro"), pObj?.optAny("value"), profitsArr.opt(i))
                if (quote != 0.0 || profit != 0.0) out.add(AssetComparisonPoint(label, quote, profit))
            }
            if (out.isNotEmpty()) return out
        }

        val directArr = firstArray(obj.optJSONArray("items"), obj.optJSONArray("data"), obj.optJSONArray("points"), obj.optJSONArray("values"))
        if (directArr != null) {
            val parsed = parseProfitVsQuotePointsFromArray(directArr)
            if (parsed.isNotEmpty()) return parsed
        }
        val wrapper = firstObject(obj.optJSONObject("data"), obj.optJSONObject("chart"), obj.optJSONObject("payload"), obj.optJSONObject("result"), obj.optJSONObject("results"))
        if (wrapper != null && wrapper !== obj) {
            val parsed = parseProfitVsQuotePointsFromObject(wrapper)
            if (parsed.isNotEmpty()) return parsed
        }
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val child = obj.optJSONObject(key) ?: continue
            val quote = firstNumber(child.optAny("quotation"), child.optAny("quote"), child.optAny("cotacao"), child.optAny("price"), child.optAny("preco"), child.optAny("value"))
            val profit = firstNumber(child.optAny("net_profit"), child.optAny("netProfit"), child.optAny("profit"), child.optAny("lucro"), child.optAny("lucroLiquido"), child.optAny("secondaryValue"))
            if ((quote != 0.0 || profit != 0.0) && key.isNotBlank()) out.add(AssetComparisonPoint(key, quote, profit))
        }
        return out
    }

    private fun flattenFinancialNumericArray(arr: JSONArray): List<Double> {
        val out = mutableListOf<Double>()
        for (i in 0 until arr.length()) {
            when (val item = arr.opt(i)) {
                is Number -> out.add(item.toDouble())
                is String -> firstNumber(item).takeIf { it != 0.0 }?.let(out::add)
                is JSONObject -> {
                    val nested = firstArray(item.optJSONArray("data"), item.optJSONArray("values"), item.optJSONArray("points"))
                    if (nested != null) out.addAll(flattenFinancialNumericArray(nested))
                    else firstNumber(item.optAny("value"), item.optAny("valor"), item.optAny("y"), item.optAny("amount"), item.optAny("total"))
                        .takeIf { it != 0.0 }?.let(out::add)
                }
                is JSONArray -> out.addAll(flattenFinancialNumericArray(item))
            }
        }
        return out
    }

    private fun financialFieldFromLabel(label: String): String {
        val key = canonicalKey(label)
        return when {
            key.contains("receitaliquida") || key == "receita" || key.contains("revenue") || key.contains("faturamento") || key.contains("sales") -> "netRevenue"
            key.contains("lucroliquido") || key == "lucro" || key.contains("netprofit") || key.contains("profit") || key.contains("earnings") -> "netProfit"
            key.contains("lucrobruto") || key.contains("grossprofit") -> "grossProfit"
            key.contains("custo") || key.contains("cost") || key.contains("cpv") -> "cost"
            key.contains("ebitda") -> "ebitda"
            key.contains("ebit") -> "ebit"
            key.contains("patrimonioliquido") || key == "pl" || key.contains("networth") || key.contains("equity") -> "netWorth"
            key.contains("ativototal") || key.contains("totalativos") || key == "ativos" || key.contains("totalassets") || key.contains("assets") -> "totalAssets"
            key.contains("passivototal") || key.contains("totalpassivos") || key == "passivos" || key.contains("totalliabilities") || key.contains("liabilities") -> "totalLiabilities"
            else -> ""
        }
    }

    private fun applyFinancialField(point: FinancialStatementPoint, field: String, value: Double): FinancialStatementPoint {
        return when (field) {
            "netRevenue" -> point.copy(netRevenue = value)
            "netProfit" -> point.copy(netProfit = value)
            "grossProfit" -> point.copy(grossProfit = value)
            "cost" -> point.copy(cost = value)
            "ebitda" -> point.copy(ebitda = value)
            "ebit" -> point.copy(ebit = value)
            "netWorth" -> point.copy(netWorth = value)
            "totalAssets" -> point.copy(totalAssets = value)
            "totalLiabilities" -> point.copy(totalLiabilities = value)
            else -> point
        }
    }

    private fun mergeFinancialStatementPoints(points: List<FinancialStatementPoint>): List<FinancialStatementPoint> {
        val byKey = linkedMapOf<String, FinancialStatementPoint>()
        for (pt in points) {
            if (pt.label.isBlank()) continue
            val key = canonicalKey(pt.label).ifBlank { canonicalKey(pt.year) }
            val merged = mergeFinancialPoint(byKey[key], pt)
            if (merged != null) byKey[key] = merged
        }
        return byKey.values.filter { p ->
            listOf(p.netRevenue, p.netProfit, p.cost, p.grossProfit, p.ebitda, p.ebit, p.netWorth, p.totalAssets, p.totalLiabilities).any { it != 0.0 && it.isFinite() }
        }
    }

    private fun mergeFinancialPoint(existing: FinancialStatementPoint?, incoming: FinancialStatementPoint): FinancialStatementPoint? {
        if (existing == null) return incoming
        fun nz(a: Double, b: Double): Double = if (a != 0.0) a else b
        return existing.copy(
            label = existing.label.ifBlank { incoming.label },
            year = existing.year.ifBlank { incoming.year },
            quarter = existing.quarter.ifBlank { incoming.quarter },
            netRevenue = nz(existing.netRevenue, incoming.netRevenue),
            cost = nz(existing.cost, incoming.cost),
            grossProfit = nz(existing.grossProfit, incoming.grossProfit),
            ebitda = nz(existing.ebitda, incoming.ebitda),
            ebit = nz(existing.ebit, incoming.ebit),
            netProfit = nz(existing.netProfit, incoming.netProfit),
            netWorth = nz(existing.netWorth, incoming.netWorth),
            totalAssets = nz(existing.totalAssets, incoming.totalAssets),
            totalLiabilities = nz(existing.totalLiabilities, incoming.totalLiabilities)
        )
    }

    private fun extractYearFromLabel(label: String): String {
        return Regex("(20[0-9]{2}|19[9][0-9])").find(label)?.value.orEmpty()
    }

    private fun extractQuarterFromLabel(label: String): String {
        return Regex("\\b([1-4]T|T[1-4]|Q[1-4])\\b", RegexOption.IGNORE_CASE).find(label)?.value?.uppercase(Locale.ROOT).orEmpty()
    }

    private fun appendDividendEventsFromArray(ticker: String, arr: JSONArray?, out: MutableList<DividendEvent>, defaultStatus: String = "") {
        if (arr == null) return
        for (ev in arr.toJsonObjectList()) {
            val eventTicker = firstText(
                ev.optAny("ticker"), ev.optAny("symbol"), ev.optAny("codigo"), ev.optAny("code"), ev.optAny("asset"), ev.optAny("ativo"), ticker
            ).uppercase(Locale.ROOT)
            if (eventTicker.isBlank()) continue
            val type = firstText(
                ev.optAny("tipo"), ev.optAny("type"), ev.optAny("eventType"), ev.optAny("tipoEvento"),
                ev.optAny("kind"), ev.optAny("proventoTipo"), ev.optAny("provento"), defaultStatus, "Dividendo"
            )
            val datacom = normalizeDisplayDate(firstText(
                ev.optAny("dataCom"), ev.optAny("data_com"), ev.optAny("dateCom"), ev.optAny("comDate"),
                ev.optAny("date_with"), ev.optAny("dataBase"), ev.optAny("exDate"), ev.optAny("recordDate"), ev.optAny("baseDate")
            ))
            val payment = normalizeDisplayDate(firstText(
                ev.optAny("dataPagamento"), ev.optAny("paymentDate"), ev.optAny("payDate"), ev.optAny("pagamento"),
                ev.optAny("date_payment"), ev.optAny("data_pagamento"), ev.optAny("dataPagamentoPrevista"),
                ev.optAny("dataPagto"), ev.optAny("pgto"), ev.optAny("date"), ev.optAny("data")
            ))
            val value = firstNumber(
                ev.optAny("valor"), ev.optAny("value"), ev.optAny("amount"), ev.optAny("valuePerShare"),
                ev.optAny("valorPorCota"), ev.optAny("valorProvento"), ev.optAny("valorPorAcao"), ev.optAny("valorFormatado"),
                ev.optAny("valueFormatted"), ev.optAny("valorPorCotaFormatado"), ev.optAny("valorPorAcaoFormatado"),
                ev.optAny("rendimento"), ev.optAny("ultimoRendimento"), ev.optAny("cashAmount"), ev.optAny("dividend")
            )
            val quantity = firstNumber(ev.optAny("quantity"), ev.optAny("quantidade"), ev.optAny("shares"), ev.optAny("cotas"))
            val estimated = firstNumber(ev.optAny("estimatedAmount"), ev.optAny("valorEstimado"), ev.optAny("total"), ev.optAny("totalAmount"))
                .takeIf { it > 0.0 } ?: if (quantity > 0.0 && value > 0.0) quantity * value else 0.0
            val source = firstText(ev.optAny("source"), ev.optAny("fonte"), ev.optAny("sourceUrl"), ev.optAny("url"), "VALORAE / Investidor10")
            if (value > 0.0 || estimated > 0.0 || datacom.isNotBlank() || payment.isNotBlank()) {
                out.add(
                    DividendEvent(
                        ticker = eventTicker,
                        dateCom = datacom,
                        paymentDate = payment,
                        valuePerShare = value,
                        quantity = quantity,
                        estimatedAmount = estimated,
                        status = type.ifBlank { "Provento" },
                        source = source
                    )
                )
            }
        }
    }

    private fun isLikelyTickerKey(key: String): Boolean {
        return Regex("^[A-Z]{3,6}\\d{1,2}[A-Z]?$", RegexOption.IGNORE_CASE).matches(key.trim())
    }

    private fun dividendAliasStatus(alias: String, defaultStatus: String = ""): String {
        val a = alias.lowercase(Locale.ROOT)
        return when {
            a.contains("upcoming") || a.contains("agenda") || a.contains("future") || a.contains("next") || a.contains("calendar") || a.contains("calendario") || a.contains("schedule") -> "Previsto"
            a.contains("history") || a.contains("historico") || a.contains("paid") || a.contains("receb") || a.contains("last") || a.contains("ultimo") -> "Recebido"
            else -> defaultStatus
        }
    }

    private fun isLikelyDividendContainerKey(key: String): Boolean {
        val a = key.lowercase(Locale.ROOT)
        return a in setOf(
            "events", "event", "items", "rows", "assets", "result", "results", "data", "payload", "response", "body", "portfolio", "asset",
            "dividends", "dividend", "dividendos", "proventos", "income", "agenda", "agendaevents", "upcoming", "upcomingevents",
            "nextdividend", "nextdividends", "nextdividendevents", "future", "futureevents", "schedule", "calendar", "calendario",
            "historico", "historicodividendos", "history", "historyevents", "paidevents", "lastdividend", "ultimo"
        ) || a.contains("dividend") || a.contains("dividendo") || a.contains("provento") || a.contains("agenda") || a.contains("upcoming") || a.contains("history") || a.contains("historico")
    }

    private fun appendDividendEventsFromMappedObject(
        defaultTicker: String,
        root: JSONObject,
        out: MutableList<DividendEvent>,
        defaultStatus: String = "",
        depth: Int = 0
    ) {
        if (depth > 4) return
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val raw = root.opt(key) ?: continue
            val keyTicker = key.trim().uppercase(Locale.ROOT).takeIf { isLikelyTickerKey(it) }
            val childTicker = keyTicker ?: defaultTicker
            val status = dividendAliasStatus(key, defaultStatus)
            when (raw) {
                is JSONArray -> {
                    if (keyTicker != null || isLikelyDividendContainerKey(key)) {
                        appendDividendEventsFromArray(childTicker, raw, out, status)
                    }
                }
                is JSONObject -> {
                    if (keyTicker != null || isLikelyDividendContainerKey(key)) {
                        appendDividendEventsFromArray(childTicker, JSONArray().put(raw), out, status)
                    }
                    if (keyTicker != null || isLikelyDividendContainerKey(key) || depth < 2) {
                        appendDividendAliasesFromRoot(childTicker, raw, out, status)
                    }
                }
            }
        }
    }


    private fun appendDividendAliasesFromRoot(defaultTicker: String, root: JSONObject, out: MutableList<DividendEvent>, defaultStatus: String = "") {
        val aliases = listOf(
            "events", "items", "rows", "result", "results", "assets",
            "dividends", "dividendos", "historicoDividendos", "historico", "history",
            "agenda", "agendaEvents", "upcomingEvents", "nextDividends", "nextDividendEvents",
            "futureEvents", "future", "proventos", "income", "schedule", "calendar", "calendario",
            "historyEvents", "paidEvents",
            "data", "payload", "response", "body", "portfolio", "asset",
            "data.events", "data.items", "data.rows", "data.dividends", "data.dividendos",
            "data.historico", "data.history", "data.agenda", "data.agendaEvents", "data.upcomingEvents",
            "data.historyEvents", "data.proventos", "data.nextDividends", "data.futureEvents", "data.schedule", "data.calendar", "data.calendario",
            "payload.events", "payload.items", "payload.dividends", "payload.dividendos", "payload.proventos", "payload.historico", "payload.history", "payload.agenda",
            "payload.agendaEvents", "payload.upcomingEvents", "payload.historyEvents", "payload.nextDividends", "payload.futureEvents",
            "result.events", "result.items", "result.dividends", "result.dividendos", "result.proventos", "result.historico", "result.history", "result.agenda",
            "result.agendaEvents", "result.upcomingEvents", "result.historyEvents", "result.nextDividends", "result.futureEvents",
            "response.events", "response.items", "response.agendaEvents", "response.upcomingEvents", "response.historyEvents",
            "body.events", "body.items", "body.agendaEvents", "body.upcomingEvents", "body.historyEvents",
            "portfolio.events", "portfolio.items", "portfolio.agendaEvents", "portfolio.upcomingEvents", "portfolio.historyEvents",
            "asset.events", "asset.items", "asset.agendaEvents", "asset.upcomingEvents", "asset.historyEvents"
        )
        aliases.forEach { alias ->
            val status = dividendAliasStatus(alias, defaultStatus)
            appendDividendEventsFromArray(defaultTicker, root.optArray(alias), out, status)
        }

        val singleObjects = listOf("event", "item", "dividend", "nextDividend", "upcoming", "lastDividend", "ultimo", "data.event", "payload.event", "result.event")
        singleObjects.forEach { key ->
            root.optObject(key)?.let { appendDividendEventsFromArray(defaultTicker, JSONArray().put(it), out, dividendAliasStatus(key, defaultStatus)) }
        }

        appendDividendEventsFromMappedObject(defaultTicker, root, out, defaultStatus)
    }

    private fun fetchAssetDividendEvents(ticker: String): List<DividendEvent> {
        val clean = ticker.trim().uppercase(Locale.ROOT)
        if (clean.isBlank()) return emptyList()
        val cacheKey = "asset_dividend_events_$clean"
        getFromCache<List<DividendEvent>>(cacheKey)?.let { return it }
        val assetType = if (inferIsFii(clean)) "FII" else "ACAO"
        val params = mapOf(
            "ticker" to clean,
            "type" to assetType,
            "assetType" to assetType,
            "limit" to "250",
            "mode" to "complete",
            "complete" to "1",
            "includeHistory" to "1",
            "includeUpcoming" to "1"
        )
        val payload = JSONObject()
            .put("ticker", clean)
            .put("type", assetType)
            .put("assetType", assetType)
            .put("limit", 500)
            .put("mode", "complete")
            .put("complete", true)
            .put("includeHistory", true)
            .put("includeUpcoming", true)
        val jsons = listOfNotNull(
            getProxyJson("/api/v1/asset/dividends", params),
            postProxyJson("/api/v1/asset/dividends", payload),
            getProxyJson("/api/v1/asset/next-dividend", params),
            postProxyJson("/api/v1/asset/next-dividend", payload)
        )
        val out = mutableListOf<DividendEvent>()
        jsons.flatMap { dividendPayloadCandidates(it) }.forEach { root ->
            appendDividendAliasesFromRoot(clean, root, out)
            // Some endpoints return a direct event object instead of an array.
            appendDividendEventsFromArray(clean, JSONArray().put(root), out)
        }
        val distinct = out
            .filter { it.ticker.isNotBlank() && (it.valuePerShare > 0.0 || it.estimatedAmount > 0.0 || it.dateCom.isNotBlank() || it.paymentDate.isNotBlank()) }
            .distinctBy { listOf(it.ticker, it.dateCom, it.paymentDate, String.format(Locale.ROOT, "%.6f", it.valuePerShare), it.status).joinToString("|") }
            .sortedWith(compareByDescending<DividendEvent> { parseFlexibleDateMillis(it.paymentDate.ifBlank { it.dateCom }) }.thenBy { it.ticker })
        if (distinct.isNotEmpty()) putInCache(cacheKey, distinct, 20)
        return distinct
    }

    private fun buildDividendYearly(events: List<DividendEvent>, currentPrice: Double): Pair<List<AssetIndicatorPoint>, List<AssetIndicatorPoint>> {
        val byYear = linkedMapOf<String, Double>()
        for (event in events) {
            val ts = parseFlexibleDateMillis(event.paymentDate.ifBlank { event.dateCom })
            val year = if (ts > 0L) SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(ts)) else event.dateCom.substringAfterLast("/", "")
            val value = event.valuePerShare.takeIf { it > 0.0 } ?: continue
            if (year.length == 4 && year.all { it.isDigit() }) byYear[year] = (byYear[year] ?: 0.0) + value
        }
        val yearly = mutableListOf<AssetIndicatorPoint>()
        val dy = mutableListOf<AssetIndicatorPoint>()
        byYear.toSortedMap().forEach { (year, total) ->
            yearly.add(AssetIndicatorPoint(label = "Anual", value = total, display = String.format(Locale.ROOT, "R$ %.4f", total), year = year))
            if (currentPrice > 0.0) {
                val percent = total / currentPrice * 100.0
                dy.add(AssetIndicatorPoint(label = "DY %", value = percent, display = String.format(Locale.ROOT, "%.2f%%", percent), unit = "%", year = year))
            }
        }
        return yearly to dy
    }

    private fun buildDividendMonthly(events: List<DividendEvent>, limit: Int = 24): List<AssetIndicatorPoint> {
        val byMonth = linkedMapOf<String, Double>()
        for (event in events) {
            val ts = parseFlexibleDateMillis(event.paymentDate.ifBlank { event.dateCom })
            val label = if (ts > 0L) SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date(ts)) else continue
            val value = event.valuePerShare.takeIf { it > 0.0 } ?: continue
            byMonth[label] = (byMonth[label] ?: 0.0) + value
        }
        return byMonth.toSortedMap(compareBy<String> { label ->
            val parts = label.split("/")
            if (parts.size == 2) "${parts[1]}-${parts[0]}" else label
        }).entries.toList().takeLast(limit).map { (period, total) ->
            AssetIndicatorPoint(label = "Mensal", value = total, display = String.format(Locale.ROOT, "R$ %.4f", total), period = period)
        }
    }


    private fun hasUsableJsonValue(value: Any?): Boolean {
        return when (value) {
            null -> false
            JSONObject.NULL -> false
            is String -> value.trim().isNotBlank() && !value.equals("null", ignoreCase = true) && value != "--" && value != "-"
            is JSONObject -> value.length() > 0
            is JSONArray -> value.length() > 0
            else -> true
        }
    }

    private fun canonicalMetricAliases(rawKey: String): List<String> {
        val raw = rawKey.trim()
        val key = canonicalKey(raw)
        val canonical = when (key) {
            "price", "currentprice", "lastprice", "cotacao", "valoratual" -> "precoAtual"
            "daychange", "changeday", "variacaodia", "variationday", "variationdaily", "variationpct" -> "variacaoDay"
            "variation12m", "variacao12m", "changeyear", "yearchange", "variation1year" -> "variacao12m"
            "dy", "dividendyield", "dividendyield12m", "yield", "yield12m" -> "dividendYield"
            "averagedy5y", "dyavg5y", "dymedio", "dymedio5a", "dividendyieldlast5years" -> "dyMedio5a"
            "lastdividend", "lastincome", "ultimorendimento", "ultimodividendo", "ultimoprovento" -> "ultimoRendimento"
            "dividends12m", "income12m", "proventos12m", "totaldividendos12m" -> "totalDividendos12m"
            "bookvaluepershare", "vpcota", "valorpatrimonial", "valorpatrimonialcota" -> "valorPatrimonialCota"
            "marketcap", "marketvalue", "valordemercado" -> "valorDeMercado"
            "enterprisevalue", "firmvalue", "valordefirma" -> "valorDeFirma"
            "networth", "equity", "patrimonio", "patrimonioliquido" -> "patrimonioLiquido"
            "assets", "totalassets", "ativos", "totalativos" -> "totalAtivos"
            "currentassets", "ativocirculante" -> "ativoCirculante"
            "grossdebt", "dividabruta" -> "dividaBruta"
            "netdebt", "dividaliquida" -> "dividaLiquida"
            "cash", "availability", "disponibilidade" -> "disponibilidade"
            "dailyliquidity", "liquidezdiaria", "liquidezmediadiaria" -> "liquidezMediaDiaria"
            "physicalvacancy", "vacancy", "vacancia", "vacanciafisica" -> "vacanciaFisica"
            "netdebtequity", "netdebtnetworth", "dividaliquidapatrimonio" -> "dividaLiquidaPatrimonio"
            "netdebtebitda", "dividaliquidaebitda" -> "dividaLiquidaEbitda"
            "netdebtebit", "dividaliquidaebit" -> "dividaLiquidaEbit"
            "grossdebtequity", "grossdebtnetworth", "dividabrutapatrimonio" -> "dividaBrutaPatrimonio"
            "currentliquidity", "liquidezcorrente" -> "liquidezCorrente"
            "assetturnover", "activeturns", "giroativos" -> "giroAtivos"
            "netmargin", "margemliquida" -> "margemLiquida"
            "grossmargin", "margembruta" -> "margemBruta"
            "ebitmargin", "margemebit" -> "margemEbit"
            "ebitdamargin", "margemebitda" -> "margemEbitda"
            "cagrrevenue5y", "growthnetrevenuelast5years", "cagrreceitas5a" -> "cagrReceitas5a"
            "cagrprofit5y", "growthnetprofitlast5years", "cagrlucros5a" -> "cagrLucros5a"
            "companyname", "nomeempresa", "nome" -> "nome"
            "sector", "setor" -> "setor"
            "subsector", "subsetor", "subsetoratuacao" -> "subsetor"
            "segment", "segmento", "segmentofii" -> "segmento"
            "listingsegment", "segmentolistagem" -> "segmentoListagem"
            "freefloat" -> "freeFloat"
            "tagalong" -> "tagAlong"
            "holders", "cotistas", "numerocotistas" -> "numeroCotistas"
            "issuedshares", "cotasemitidas", "quantidadecotas" -> "cotasEmitidas"
            "adminfee", "taxaadministracao", "taxadeadministracao" -> "taxaAdministracao"
            else -> raw
        }
        return listOf(canonical, raw).filter { it.isNotBlank() }.distinct()
    }

    private fun putWithAliases(target: JSONObject, rawKey: String, rawValue: Any?) {
        if (!hasUsableJsonValue(rawValue)) return
        for (key in canonicalMetricAliases(rawKey)) {
            if (key.isNotBlank() && (!target.has(key) || target.isNull(key))) target.put(key, rawValue)
        }
    }

    private fun coverageFieldsObject(coverage: JSONObject?): JSONObject? {
        if (coverage == null || coverage.length() == 0) return null
        val out = JSONObject()
        val groups = coverage.optJSONArray("groups") ?: return null
        for (i in 0 until groups.length()) {
            val group = groups.optJSONObject(i) ?: continue
            val fields = group.optJSONArray("fields") ?: continue
            for (j in 0 until fields.length()) {
                val field = fields.optJSONObject(j) ?: continue
                val rawKey = firstText(field.optAny("key"), field.optAny("name"), field.optAny("label"))
                if (rawKey.isBlank()) continue
                val value = if (field.has("value") && !field.isNull("value")) field.optAny("value") else field
                putWithAliases(out, rawKey, value)
            }
        }
        return if (out.length() > 0) out else null
    }

    private fun contractFieldsObject(contract: JSONObject?): JSONObject? {
        if (contract == null || contract.length() == 0) return null
        val out = JSONObject()
        val groups = contract.optJSONObject("groups") ?: return null
        val groupKeys = groups.keys()
        while (groupKeys.hasNext()) {
            val groupKey = groupKeys.next()
            val fields = groups.optJSONObject(groupKey)?.optJSONObject("fields") ?: continue
            val fieldKeys = fields.keys()
            while (fieldKeys.hasNext()) {
                val rawKey = fieldKeys.next()
                val obj = fields.optJSONObject(rawKey)
                if (obj != null && obj.has("value") && !obj.isNull("value")) {
                    putWithAliases(out, rawKey, obj.opt("value"))
                } else {
                    val value = fields.opt(rawKey)
                    putWithAliases(out, rawKey, value)
                }
            }
        }
        return if (out.length() > 0) out else null
    }

    private fun appQuoteAsCotacao(appQuote: JSONObject?): JSONObject? {
        if (appQuote == null || appQuote.length() == 0) return null
        val out = JSONObject()
        putWithAliases(out, "precoAtual", appQuote.optAny("price") ?: appQuote.optAny("priceDisplay"))
        putWithAliases(out, "variacaoDay", appQuote.optAny("dayChange") ?: appQuote.optAny("dayChangeDisplay"))
        putWithAliases(out, "dividendYield", appQuote.optAny("dividendYield") ?: appQuote.optAny("dividendYieldDisplay"))
        putWithAliases(out, "nome", appQuote.optAny("name"))
        return if (out.length() > 0) out else null
    }

    private fun mapProxyAsset(payload: JSONObject?): B3AssetData? {
        val root = unwrapValoraePayload(payload) ?: return null
        val ticker = firstText(root.optAny("ticker"), root.optString("symbol", "")).uppercase(Locale.ROOT)
        if (ticker.isBlank()) return null
        val directResults = root.optJSONObject("results")
        val legacyAppCompat = firstObject(root.optJSONObject("legacyAppCompat"), directResults?.optJSONObject("legacyAppCompat"), root.optObject("data.legacyAppCompat"))
        val results = directResults ?: legacyAppCompat?.optJSONObject("results") ?: root
        val appPayload = firstObject(root.optJSONObject("appPayload"), results.optJSONObject("appPayload"), legacyAppCompat?.optJSONObject("appPayload"), root.optObject("data.appPayload"), results.optObject("data.appPayload"))
        val appSnapshot = firstObject(root.optJSONObject("appMobileSnapshot"), results.optJSONObject("appMobileSnapshot"), legacyAppCompat?.optJSONObject("appMobileSnapshot"), root.optObject("data.appMobileSnapshot"), results.optObject("data.appMobileSnapshot"))
        val appQuote = firstObject(appPayload?.optJSONObject("quote"), appSnapshot?.optJSONObject("quote"))
        val contractFields = contractFieldsObject(firstObject(root.optJSONObject("assetClassContract"), results.optJSONObject("assetClassContract"), legacyAppCompat?.optJSONObject("assetClassContract"), root.optObject("data.assetClassContract")))
        val coverageFields = coverageFieldsObject(firstObject(root.optJSONObject("assetIndicatorCoverage"), results.optJSONObject("assetIndicatorCoverage"), legacyAppCompat?.optJSONObject("assetIndicatorCoverage"), root.optObject("data.assetIndicatorCoverage")))
        val officialMetricFields = mergedObject(
            appPayload?.optObject("metrics.canonical"),
            appSnapshot?.optJSONObject("metrics"),
            coverageFields,
            contractFields
        )
        val sections = firstObject(results.optJSONObject("sections"), root.optJSONObject("sections"))
        val normalized = mergedObject(
            root.optJSONObject("normalized"),
            results.optJSONObject("normalized"),
            legacyAppCompat?.optJSONObject("normalized"),
            root.optObject("data.normalized"),
            results.optObject("data.normalized"),
            officialMetricFields
        )
        val cotacao = mergedObject(results.optJSONObject("cotacao"), root.optJSONObject("cotacao"), appQuoteAsCotacao(appQuote))
        val indicadores = mergedObject(
            results.optJSONObject("indicadores"),
            sections?.optJSONObject("indicadores"),
            results.optObject("indicadoresFundamentalistas.semComparativos"),
            results.optObject("indicadoresFundamentalistas.comComparativos"),
            results.optObject("indicadoresFundamentalistas.comparativos"),
            root.optJSONObject("indicadores"),
            officialMetricFields
        )
        val financialSummary = mergedObject(
            root.optJSONObject("financialSummary"),
            results.optJSONObject("financialSummary"),
            sections?.optJSONObject("financialSummary"),
            root.optObject("data.financialSummary")
        )
        val ratiosChave = mergedObject(
            financialSummary?.optJSONObject("ratiosChave"),
            financialSummary?.optJSONObject("keyRatios"),
            financialSummary?.optJSONObject("ratios"),
            results.optJSONObject("ratiosChave"),
            results.optJSONObject("keyRatios")
        )
        val indicadoresAvancados = mergedObject(
            sections?.optJSONObject("indicadoresAvancados"),
            results.optJSONObject("indicadoresAvancados"),
            results.optJSONObject("advancedMetrics"),
            root.optJSONObject("indicadoresAvancados"),
            root.optJSONObject("advancedMetrics")
        )
        val dadosEmpresa = firstObject(
            results.optJSONObject("dadosEmpresa"),
            results.optObject("sections.empresa.dados"),
            sections?.optObject("empresa.dados"),
            root.optJSONObject("dadosEmpresa")
        )
        val infoEmpresa = firstObject(results.optJSONObject("informacoesEmpresa"), sections?.optJSONObject("informacoesEmpresa"), root.optJSONObject("informacoesEmpresa"))
        val infoFundo = mergedObject(
            results.optJSONObject("informacoesFundo"),
            sections?.optJSONObject("informacoesFundo"),
            results.optJSONObject("dadosFundo"),
            sections?.optJSONObject("dadosFundo"),
            results.optJSONObject("fund"),
            sections?.optJSONObject("fund"),
            root.optJSONObject("informacoesFundo")
        )
        val dividendos = results.optJSONObject("dividendos")
            ?: results.optJSONObject("dividends")
            ?: results.optJSONObject("proventos")
            ?: results.optJSONObject("income")
            ?: results.optJSONObject("earnings")
            ?: appPayload?.optJSONObject("dividends")
            ?: appSnapshot?.optJSONObject("dividends")
            ?: root.optObject("data.dividendos")
            ?: root.optObject("data.dividends")
            ?: sections?.optJSONObject("dividendos")
            ?: sections?.optJSONObject("dividends")
            ?: sections?.optJSONObject("proventos")
        val history = firstArray(
            results.optJSONArray("historicoDividendos"),
            results.optJSONArray("dividends"),
            results.optJSONArray("historicoProventos"),
            results.optJSONArray("proventos"),
            dividendos?.optJSONArray("historico"),
            dividendos?.optJSONArray("items"),
            dividendos?.optJSONArray("events"),
            appPayload?.optArray("dividends.history"),
            appSnapshot?.optArray("dividends.recentHistory"),
            root.optArray("data.dividendos.historico"),
            root.optArray("data.dividends.history")
        )
        val firstDividend = history?.optJsonObjectOrNull(0)
        val type = firstText(root.optAny("type"), appPayload?.optAny("type"), appSnapshot?.optAny("type"), results.optAny("tipo"))
        val isFii = type.equals("FII", ignoreCase = true) || inferIsFii(ticker)
        val price = firstNumber(
            normalizedValue(normalized, "precoAtual"),
            normalizedValue(normalized, "price"),
            appQuote?.optAny("price"),
            appQuote?.optAny("priceDisplay"),
            cotacao?.optAny("precoAtual"),
            cotacao?.optAny("price"),
            results.optAny("precoAtual"),
            root.optAny("precoAtual")
        )
        val dy = firstNumber(
            normalizedValue(normalized, "dividendYield"),
            normalizedValue(normalized, "dy"),
            appQuote?.optAny("dividendYield"),
            appQuote?.optAny("dividendYieldDisplay"),
            indicadores?.optAny("dividendYield"),
            ratiosChave?.optAny("dividendYield"),
            dividendos?.optAny("dividendYield"),
            dividendos?.optAny("dy"),
            dividendos?.optAny("dyDisplay"),
            results.optAny("dividendYield"),
            results.optAny("yield12m"),
            indicadoresAvancados?.optAny("dividend_yield_last_12_months"),
            indicadoresAvancados?.optAny("dividend_yield")
        )
        val lastDividend = firstNumber(
            firstDividend?.optAny("valor"),
            firstDividend?.optAny("value"),
            firstDividend?.optAny("valorPorCota"),
            firstDividend?.optAny("valuePerShare"),
            dividendos?.optAny("lastIncome"),
            dividendos?.optAny("lastIncomeDisplay"),
            results.optAny("ultimoRendimento"),
            results.optAny("lastDividend"),
            indicadores?.optAny("ultimoRendimento")
        )
        val name = firstText(
            appQuote?.optAny("name"),
            normalizedDisplay(normalized, "nome"),
            normalizedDisplay(normalized, "companyName"),
            results.optAny("nome"),
            dadosEmpresa?.optAny("nomeCompleto"),
            infoFundo?.optAny("nome"),
            root.optAny("name"),
            ticker
        )
        val description = firstText(
            results.optAny("sobre"),
            results.optAny("descricao"),
            root.optAny("assetDescription"),
            normalizedDisplay(normalized, "descricao")
        )
        val segment = firstText(
            infoFundo?.optAny("segmentoFii"),
            infoFundo?.optAny("segmento"),
            results.optAny("segmentoFii"),
            normalizedDisplay(normalized, "segmento"),
            normalizedDisplay(normalized, "segmentoFii"),
            infoEmpresa?.optAny("segmento"),
            infoEmpresa?.optAny("setor")
        )
        val companySector = firstText(
            normalizedDisplay(normalized, "setor"),
            normalizedDisplay(normalized, "subsetor"),
            infoEmpresa?.optAny("setor"),
            infoEmpresa?.optAny("subsetor"),
            results.optAny("subSetor"),
            results.optAny("subsetor")
        )

        val valorPatrimonialObj = firstObject(results.optJSONObject("valorPatrimonial"), sections?.optJSONObject("valorPatrimonial"), root.optJSONObject("valorPatrimonial"))
        val vpaRaw = if (valorPatrimonialObj != null) {
            valorPatrimonialObj.optAny("valorPatrimonial") ?: valorPatrimonialObj.optAny("valorPatrimonialRaw")
        } else {
            results.optAny("valorPatrimonial")
        }

        val patrimonioLiquidoRaw = if (valorPatrimonialObj != null) {
            valorPatrimonialObj.optAny("patrimonioLiquidoRaw") ?: valorPatrimonialObj.optAny("patrimonioLiquido")
        } else {
            results.optAny("patrimonioLiquido") ?: financialSummary?.optAny("patrimonioLiquido")
        }

        val valorDeMercadoObj = results.optJSONObject("valorDeMercado") ?: results.optJSONObject("valorMercado")
        val marketCapRaw = if (valorDeMercadoObj != null) {
            valorDeMercadoObj.optAny("valorDeMercadoRaw") ?: valorDeMercadoObj.optAny("valorDeMercado")
        } else {
            results.optAny("valorDeMercado") ?: results.optAny("valorMercado") ?: financialSummary?.optAny("valorDeMercado")
        }

        val vpaVal = firstNumber(
            normalizedValue(normalized, "valorPatrimonialCota"),
            vpaRaw,
            indicadores?.optAny("vpa"),
            results.optAny("vpa"),
            indicadoresAvancados?.optAny("vpa")
        )

        val netWorthVal = firstNumber(
            infoEmpresa?.optAny("patrimonioLiquido"),
            patrimonioLiquidoRaw,
            results.optAny("patrimonioLiquido"),
            normalizedValue(normalized, "patrimonioLiquido"),
            indicadoresAvancados?.optAny("balance_net_worth")
        ).let { valOrScaled ->
            if (isFii && valOrScaled > 0.0 && valOrScaled < 100000.0) valOrScaled * 1_000_000_000.0 else valOrScaled
        }

        val marketCapVal = firstNumber(
            normalizedValue(normalized, "valorDeMercado"),
            marketCapRaw,
            infoEmpresa?.optAny("valorDeMercado"),
            results.optAny("valorDeMercado"),
            indicadoresAvancados?.optAny("market_value")
        ).let { valOrScaled ->
            if (isFii && valOrScaled > 0.0 && valOrScaled < 100000.0) valOrScaled * 1_000_000_000.0 else valOrScaled
        }

        val totalAssetsVal = firstNumber(
            infoEmpresa?.optAny("ativos"),
            infoEmpresa?.optAny("totalAtivos"),
            results.optAny("ativos"),
            results.optAny("totalAtivos"),
            indicadoresAvancados?.optAny("balance_total_assets")
        ).let { valOrScaled ->
            if (isFii && valOrScaled > 0.0 && valOrScaled < 100000.0) valOrScaled * 1_000_000_000.0 else valOrScaled
        }

        val dailyLiquidityRaw = firstNumber(
            normalizedValue(normalized, "liquidezMediaDiaria"),
            infoEmpresa?.optAny("liquidezMediaDiaria"),
            results.optAny("liquidezMediaDiaria"),
            results.optAny("liquidezDiaria"),
            indicadoresAvancados?.optAny("liquidez_media_diaria"),
            indicadoresAvancados?.optAny("liquidezMediaDiaria")
        )
        val dailyLiquidityVal = if (isFii && dailyLiquidityRaw > 0.0 && dailyLiquidityRaw < 100000.0) dailyLiquidityRaw * 1_000_000.0 else dailyLiquidityRaw

        val proxyStatus = firstText(root.optAny("status"), results.optAny("status"), "OK").uppercase(Locale.ROOT)
        val guidanceObj = firstObject(
            root.optJSONObject("partialDataGuidance"),
            results.optJSONObject("partialDataGuidance"),
            root.optObject("appSyncEnvelope.decision"),
            results.optObject("appSyncEnvelope.decision"),
            root.optJSONObject("assetActionPlan"),
            results.optJSONObject("assetActionPlan")
        )
        val extractionCompletenessValue = firstNumber(
            root.optAny("extractionCompleteness"),
            results.optAny("extractionCompleteness"),
            root.optObject("extractionCompleteness")?.optAny("score"),
            root.optObject("extractionCompleteness")?.optAny("percent"),
            root.optObject("extractionCompleteness")?.optAny("completenessPercent"),
            results.optObject("extractionCompleteness")?.optAny("score"),
            results.optObject("extractionCompleteness")?.optAny("percent"),
            results.optObject("extractionCompleteness")?.optAny("completenessPercent"),
            root.optObject("appRenderContract")?.optAny("score"),
            root.optObject("appMobileSnapshot")?.optAny("completenessPercent")
        )
        val dataReliabilityText = firstText(
            root.optObject("dataReliability")?.optAny("level"),
            root.optObject("dataReliability")?.optAny("status"),
            root.optObject("dataReliability")?.optAny("label"),
            root.optAny("dataReliability"),
            results.optObject("dataReliability")?.optAny("level"),
            results.optObject("dataReliability")?.optAny("status"),
            results.optAny("dataReliability")
        )
        val cacheStatusText = firstText(
            root.optObject("cacheStatus")?.optAny("status"),
            root.optObject("cache")?.optAny("status"),
            root.optAny("cacheStatus"),
            results.optAny("cacheStatus")
        )
        val shouldKeepPrevious = guidanceObj?.optBoolean("shouldKeepPreviousSnapshot", false) == true ||
            guidanceObj?.optBoolean("keepPreviousSnapshot", false) == true ||
            firstText(guidanceObj?.optAny("action"), guidanceObj?.optAny("renderHint"))
                .lowercase(Locale.ROOT).contains("keep_previous")
        val isPartialResponse = proxyStatus.equals("PARTIAL", ignoreCase = true) ||
            root.optBoolean("partial", false) || results.optBoolean("partial", false) || shouldKeepPrevious
        if (isPartialResponse) partialResponseTickers[ticker] = System.currentTimeMillis()

        return B3AssetData(
            ticker = ticker,
            name = name,
            price = price,
            changePercent = firstNumber(normalizedValue(normalized, "variacaoDay"), appQuote?.optAny("dayChange"), appQuote?.optAny("dayChangeDisplay"), cotacao?.optAny("variacaoDay"), results.optAny("variacaoDay")),
            dy = dy,
            pl = if (isFii) 0.0 else firstNumber(normalizedValue(normalized, "pl"), indicadores?.optAny("pl"), ratiosChave?.optAny("pl"), results.optAny("pl"), indicadoresAvancados?.optAny("p_l")),
            pvp = firstNumber(normalizedValue(normalized, "pvp"), indicadores?.optAny("pvp"), ratiosChave?.optAny("pvp"), results.optAny("pvp"), indicadoresAvancados?.optAny("p_vp")),
            vpa = vpaVal,
            lpa = if (isFii) 0.0 else firstNumber(indicadores?.optAny("lpa"), results.optAny("lpa"), indicadoresAvancados?.optAny("lpa")),
            marketCap = marketCapVal,
            roe = firstNumber(normalizedValue(normalized, "roe"), indicadores?.optAny("roe"), ratiosChave?.optAny("roe"), results.optAny("roe"), indicadoresAvancados?.optAny("roe")),
            margins = firstNumber(normalizedValue(normalized, "margemLiquida"), indicadores?.optAny("margemLiquida"), ratiosChave?.optAny("margemLiquida"), results.optAny("margemLiquida"), indicadoresAvancados?.optAny("net_margin")),
            lastDividend = lastDividend,
            dailyLiquidity = dailyLiquidityVal,
            high52 = firstNumber(cotacao?.optAny("max52Semanas"), cotacao?.optAny("high52"), results.optAny("max52Semanas"), results.optAny("high52"), results.optAny("fiftyTwoWeekHigh")),
            low52 = firstNumber(cotacao?.optAny("min52Semanas"), cotacao?.optAny("low52"), results.optAny("min52Semanas"), results.optAny("low52"), results.optAny("fiftyTwoWeekLow")),
            forwardPE = firstNumber(indicadores?.optAny("forwardPE"), results.optAny("forwardPE")),
            priceToSales = firstNumber(indicadores?.optAny("psr"), results.optAny("psr"), normalizedValue(normalized, "psr"), indicadoresAvancados?.optAny("psr")),
            nextEarningsDate = firstText(firstDividend?.optAny("dataCom"), results.optAny("dataCom")),
            isFii = isFii,
            source = "Serviço de dados VALORAE",
            payout = firstNumber(normalizedValue(normalized, "payout"), indicadores?.optAny("payout"), ratiosChave?.optAny("payout"), results.optAny("payout"), indicadoresAvancados?.optAny("payout")),
            cagrRevenue5y = firstNumber(normalizedValue(normalized, "cagrReceitas5a"), indicadores?.optAny("cagrReceitas5a"), results.optAny("cagrReceitas5a"), indicadoresAvancados?.optAny("growth_net_revenue_last_5_years")),
            grossMargin = firstNumber(normalizedValue(normalized, "margemBruta"), indicadores?.optAny("margemBruta"), results.optAny("margemBruta"), indicadoresAvancados?.optAny("gross_margin")),
            ebitMargin = firstNumber(normalizedValue(normalized, "margemEbit"), indicadores?.optAny("margemEbit"), results.optAny("margemEbit"), indicadoresAvancados?.optAny("ebit_margin")),
            ebitdaMargin = firstNumber(normalizedValue(normalized, "margemEbitda"), indicadores?.optAny("margemEbitda"), results.optAny("margemEbitda"), indicadoresAvancados?.optAny("ebitda_margin")),
            evEbitda = firstNumber(normalizedValue(normalized, "evEbitda"), indicadores?.optAny("evEbitda"), ratiosChave?.optAny("evEbitda"), results.optAny("evEbitda"), indicadoresAvancados?.optAny("ev_ebitda")),
            evEbit = firstNumber(normalizedValue(normalized, "evEbit"), indicadores?.optAny("evEbit"), ratiosChave?.optAny("evEbit"), results.optAny("evEbit"), indicadoresAvancados?.optAny("ev_ebit")),
            priceEbitda = firstNumber(normalizedValue(normalized, "pEbitda"), indicadores?.optAny("pEbitda"), results.optAny("pEbitda"), indicadores?.optAny("priceEbitda"), indicadoresAvancados?.optAny("p_ebitda")),
            priceEbit = firstNumber(normalizedValue(normalized, "pEbit"), indicadores?.optAny("pEbit"), results.optAny("pEbit"), indicadores?.optAny("priceEbit"), indicadoresAvancados?.optAny("p_ebit")),
            priceAsset = firstNumber(normalizedValue(normalized, "pAtivo"), indicadores?.optAny("pAtivo"), results.optAny("pAtivo"), indicadoresAvancados?.optAny("p_assets")),
            priceCapGiro = firstNumber(normalizedValue(normalized, "pCapGiro"), indicadores?.optAny("pCapGiro"), results.optAny("pCapGiro"), indicadoresAvancados?.optAny("p_working_capital")),
            priceAtivoCircLiq = firstNumber(normalizedValue(normalized, "pAtivoCircLiq"), indicadores?.optAny("pAtivoCircLiq"), results.optAny("pAtivoCircLiq"), indicadoresAvancados?.optAny("p_asset_current_net")),
            giroAtivos = firstNumber(normalizedValue(normalized, "giroAtivos"), indicadores?.optAny("giroAtivos"), results.optAny("giroAtivos"), indicadoresAvancados?.optAny("active_turns")),
            roic = firstNumber(normalizedValue(normalized, "roic"), indicadores?.optAny("roic"), ratiosChave?.optAny("roic"), results.optAny("roic"), indicadoresAvancados?.optAny("roic")),
            roa = firstNumber(normalizedValue(normalized, "roa"), indicadores?.optAny("roa"), ratiosChave?.optAny("roa"), results.optAny("roa"), indicadoresAvancados?.optAny("roa")),
            divLiqPatrimonio = firstNumber(normalizedValue(normalized, "dividaLiquidaPatrimonio"), indicadores?.optAny("dividaLiquidaPatrimonio"), results.optAny("dividaLiquidaPatrimonio"), indicadoresAvancados?.optAny("net_debt_net_worth")),
            debtEbitda = firstNumber(normalizedValue(normalized, "dividaLiquidaEbitda"), indicadores?.optAny("dividaLiquidaEbitda"), results.optAny("dividaLiquidaEbitda"), indicadoresAvancados?.optAny("net_debt_ebitda")),
            divLiqEbit = firstNumber(normalizedValue(normalized, "dividaLiquidaEbit"), indicadores?.optAny("dividaLiquidaEbit"), results.optAny("dividaLiquidaEbit"), indicadoresAvancados?.optAny("net_debt_ebit")),
            divBrutaPatrimonio = firstNumber(normalizedValue(normalized, "dividaBrutaPatrimonio"), indicadores?.optAny("dividaBrutaPatrimonio"), results.optAny("dividaBrutaPatrimonio"), indicadoresAvancados?.optAny("gross_debt_net_worth")),
            patrimonioAtivos = firstNumber(normalizedValue(normalized, "patrimonioAtivos"), indicadores?.optAny("patrimonioAtivos"), results.optAny("patrimonioAtivos"), indicadoresAvancados?.optAny("net_worth_assets")),
            passivosAtivos = firstNumber(normalizedValue(normalized, "passivosAtivos"), indicadores?.optAny("passivosAtivos"), results.optAny("passivosAtivos"), indicadoresAvancados?.optAny("liabilities_assets")),
            liquidezCorrente = firstNumber(normalizedValue(normalized, "liquidezCorrente"), indicadores?.optAny("liquidezCorrente"), results.optAny("liquidezCorrente"), indicadoresAvancados?.optAny("current_liquidity")),
            cnpj = firstText(dadosEmpresa?.optAny("cnpj"), infoFundo?.optAny("cnpj"), results.optAny("cnpj")),
            listSegment = firstText(dadosEmpresa?.optAny("segmentoListagem"), results.optAny("segmentoListagem")),
            foundationYear = firstText(dadosEmpresa?.optAny("anoFundacao"), results.optAny("anoFundacao")),
            listingYear = firstText(dadosEmpresa?.optAny("anoEstreiaB3"), results.optAny("anoEstreiaB3")),
            employeesCount = firstText(dadosEmpresa?.optAny("numeroFuncionarios"), results.optAny("numeroFuncionarios")),
            firmValue = firstNumber(infoEmpresa?.optAny("valorDeFirma"), financialSummary?.optAny("valorDeFirma"), results.optAny("valorDeFirma"), indicadoresAvancados?.optAny("enterprise_value")),
            netWorth = netWorthVal,
            totalPapers = firstText(infoEmpresa?.optAny("totalPapeis"), results.optAny("totalPapeis"), results.optAny("cotasEmitidas")),
            totalAssets = totalAssetsVal,
            currentAssets = firstNumber(infoEmpresa?.optAny("ativoCirculante"), results.optAny("ativoCirculante"), indicadoresAvancados?.optAny("balance_current_assets")),
            grossDebt = firstNumber(infoEmpresa?.optAny("dividaBruta"), results.optAny("dividaBruta"), indicadoresAvancados?.optAny("balance_gross_debt")),
            netDebt = firstNumber(infoEmpresa?.optAny("dividaLiquida"), results.optAny("dividaLiquida"), indicadoresAvancados?.optAny("balance_net_debt")),
            availability = firstNumber(infoEmpresa?.optAny("disponibilidade"), results.optAny("disponibilidade"), indicadoresAvancados?.optAny("balance_availability")),
            freeFloat = firstNumber(infoEmpresa?.optAny("freeFloat"), results.optAny("freeFloat"), indicadoresAvancados?.optAny("free_float")),
            tagAlong = firstNumber(infoEmpresa?.optAny("tagAlong"), results.optAny("tagAlong"), indicadoresAvancados?.optAny("tag_along")),
            fiiVacancy = firstNumber(normalizedValue(normalized, "vacanciaFisica"), infoFundo?.optAny("vacanciaFisica"), results.optAny("vacanciaFisica")),
            fiiPropertyCount = firstNumber(results.optAny("numeroImoveis"), infoFundo?.optAny("numeroImoveis"), sections?.optJSONArray("listaImoveis")?.length()).toInt(),
            fiiSegment = if (isFii) segment else "",
            assetDescription = description,
            subSector = if (isFii) "" else companySector.ifBlank { segment },
            cagrProfit5y = firstNumber(normalizedValue(normalized, "cagrLucros5a"), indicadores?.optAny("cagrLucros5a"), results.optAny("cagrLucros5a"), indicadoresAvancados?.optAny("growth_net_profit_last_5_years")),
            fiiTotalHolders = firstText(infoFundo?.optAny("numeroCotistas"), infoFundo?.optAny("cotistas"), results.optAny("numeroCotistas"), results.optAny("cotistas")),
            fiiIssuedShares = firstText(infoFundo?.optAny("cotasEmitidas"), infoFundo?.optAny("quantidadeCotas"), results.optAny("cotasEmitidas"), results.optAny("totalPapeis"), results.optAny("quantidadeCotas")),
            fiiAdminFee = firstText(infoFundo?.optAny("taxaAdministracao"), infoFundo?.optAny("taxaDeAdministracao"), results.optAny("taxaAdministracao"), results.optAny("taxaDeAdministracao")),
            fiiFundType = firstText(infoFundo?.optAny("tipoFundo"), infoFundo?.optAny("tipo"), results.optAny("tipoFundo"), results.optAny("tipoFii"), segment),
            fiiMandate = firstText(infoFundo?.optAny("mandato"), results.optAny("mandato"), results.optAny("classificacao")),
            fiiTargetAudience = firstText(infoFundo?.optAny("publicoAlvo"), infoFundo?.optAny("publico"), results.optAny("publicoAlvo")),
            fiiManagementType = firstText(infoFundo?.optAny("tipoGestao"), infoFundo?.optAny("gestao"), results.optAny("tipoGestao"), results.optAny("gestao")),
            fiiDuration = firstText(infoFundo?.optAny("prazoDuracao"), infoFundo?.optAny("prazo"), results.optAny("prazoDuracao"), results.optAny("prazo")),
            magicNumber = if (isFii && lastDividend > 0.0 && price > 0.0) kotlin.math.ceil(price / lastDividend) else 0.0,
            proxyStatus = proxyStatus,
            isPartial = isPartialResponse,
            dataReliability = dataReliabilityText,
            extractionCompleteness = extractionCompletenessValue,
            partialDataGuidance = firstText(
                guidanceObj?.optAny("message"),
                guidanceObj?.optAny("summary"),
                guidanceObj?.optAny("renderHint"),
                guidanceObj?.optAny("action"),
                root.optAny("partialDataGuidance"),
                results.optAny("partialDataGuidance")
            ),
            cacheStatus = cacheStatusText,
            handlerTotalMs = firstNumber(root.optAny("handlerTotalMs"), root.optObject("metrics")?.optAny("handlerTotalMs"), results.optAny("handlerTotalMs")).toLong(),
            shouldKeepPreviousSnapshot = shouldKeepPrevious
        )
    }

    private fun fetchAssetDataFromProxy(ticker: String, bypassCache: Boolean = false): B3AssetData? {
        val clean = ticker.trim().uppercase(Locale.ROOT)
        if (clean.startsWith("^") || clean.contains("=X")) return fetchMarketIndexFromProxy(clean)
        val json = getProxyJson(
            "/api/v1/asset",
            mapOf(
                "ticker" to clean,
                "view" to "app",
                "profile" to "max",
                "timeoutMs" to "3000",
                "complete" to "1",
                "adaptiveCompletion" to "1",
                "statusInvestComplement" to "1",
                "includeNews" to "0",
                "nocache" to if (bypassCache) "1" else null
            )
        ) ?: getProxyJson(
            "/api/v1/asset",
            mapOf(
                "ticker" to clean,
                "view" to "app",
                "profile" to "turbo",
                "timeoutMs" to "1800",
                "complete" to "1",
                "nocache" to if (bypassCache) "1" else null
            )
        ) ?: return null
        return mapProxyAsset(json)
    }

    private fun fetchMarketIndexFromProxy(symbol: String): B3AssetData? {
        val json = getProxyJson("/api/v1/market/indices") ?: return null
        val root = unwrapValoraePayload(json) ?: return null
        val rows = firstArray(
            root.optJSONArray("indices"),
            root.optJSONArray("items"),
            root.optJSONArray("benchmarks"),
            root.optJSONArray("results"),
            root.optArray("data.indices"),
            root.optArray("data.items"),
            root.optArray("results.indices"),
            root.optArray("market.indices")
        ) ?: return null
        val desired = when (symbol.uppercase(Locale.ROOT)) {
            "^BVSP", "IBOV", "IBOVESPA" -> "IBOV"
            "^IFIX", "IFIX" -> "IFIX_PROXY"
            "USDBRL=X", "DOLAR", "USD" -> "USDBRL=X"
            else -> symbol.uppercase(Locale.ROOT)
        }
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            val name = firstText(row.optAny("name"), row.optAny("label"), row.optAny("nome"), row.optAny("ticker"), row.optAny("symbol"))
            val sym = firstText(row.optAny("symbol"), row.optAny("ticker"), row.optAny("code"), row.optAny("codigo"), row.optAny("key"))
            val aliases = listOf(name, sym, row.optString("id", ""), row.optString("slug", "")).map { it.uppercase(Locale.ROOT) }
            if (aliases.none { it == desired || it == symbol.uppercase(Locale.ROOT) || (desired == "IFIX_PROXY" && it == "IFIX") }) continue
            val price = firstNumber(row.optAny("price"), row.optAny("value"), row.optAny("close"), row.optAny("last"), row.optAny("cotacao"), row.optAny("valor"))
            if (price <= 0.0) return null
            return B3AssetData(
                ticker = when (desired) { "IBOV" -> "IBOV"; "IFIX_PROXY" -> "IFIX"; else -> desired },
                name = name.ifBlank { desired },
                price = price,
                changePercent = firstNumber(row.optAny("variationPct"), row.optAny("changePercent"), row.optAny("percent"), row.optAny("pct"), row.optAny("variacao")),
                source = "Serviço de dados VALORAE",
                isFii = false
            )
        }
        return null
    }

    fun fetchAssetData(ticker: String, bypassCache: Boolean = false): B3AssetData? {
        val clean = ticker.trim().uppercase(Locale.ROOT)
        val cacheKey = "asset_data_proxy_$clean"
        val cached = if (!bypassCache) getFromCache<B3AssetData>(cacheKey) else null
        if (cached != null) return cached

        val fromProxy = fetchAssetDataFromProxy(clean, bypassCache)
        if (fromProxy != null && assetHasUsefulData(fromProxy)) {
            if (assetIsGoodSnapshot(fromProxy)) {
                putInCache(cacheKey, fromProxy, 5)
                saveBestSnapshot(fromProxy)
                return fromProxy
            }
            val best = loadBestSnapshot(clean)
            if (best != null) {
                putInCache(cacheKey, best, 3)
                return best.copy(
                    proxyStatus = fromProxy.proxyStatus.ifBlank { "PARTIAL" },
                    isPartial = true,
                    partialDataGuidance = fromProxy.partialDataGuidance.ifBlank { "Resposta parcial recebida; usando último snapshot bom." },
                    shouldKeepPreviousSnapshot = true,
                    fromLocalSnapshot = true
                )
            }
            putInCache(cacheKey, fromProxy, 2)
            return fromProxy
        }

        val best = loadBestSnapshot(clean)
        if (best != null) {
            putInCache(cacheKey, best, 3)
            return best.copy(
                proxyStatus = "CACHE_LOCAL",
                isPartial = true,
                partialDataGuidance = "Serviço de dados indisponível ou sem campos úteis; usando último snapshot bom local.",
                shouldKeepPreviousSnapshot = true,
                fromLocalSnapshot = true
            )
        }

        if (directFallbackEnabled()) {
            val direct = fetchAssetDataDirect(clean, bypassCache)
            if (direct != null) {
                putInCache(cacheKey, direct.copy(source = "Fallback direto"), 3)
                return direct.copy(source = "Fallback direto")
            }
        }
        return fromProxy
    }

    fun fetchAssetsData(tickers: List<String>, bypassCache: Boolean = false): Map<String, B3AssetData> {
        val cleanTickers = tickers.map { it.trim().uppercase(Locale.ROOT) }.filter { it.isNotBlank() }.distinct()
        if (cleanTickers.isEmpty()) return emptyMap()
        val out = linkedMapOf<String, B3AssetData>()
        val toFetch = mutableListOf<String>()
        for (ticker in cleanTickers) {
            val cacheKey = "asset_data_proxy_$ticker"
            val cached = if (!bypassCache) getFromCache<B3AssetData>(cacheKey) else null
            if (cached != null) out[ticker] = cached else toFetch.add(ticker)
        }
        if (toFetch.isNotEmpty()) {
            val body = JSONObject()
                .put("tickers", JSONArray(toFetch))
                .put("view", "app")
                .put("profile", "portfolio")
                .put("timeoutMs", 2200)
                .put("complete", true)
                .put("adaptiveCompletion", true)
                .put("statusInvestComplement", true)
                .put("maxConcurrency", 3)
                .put("includeNews", false)
                .put("nocache", bypassCache)
            val json = postProxyJson("/api/v1/assets", body)
                ?: getProxyJson(
                    "/api/v1/assets",
                    mapOf(
                        "tickers" to toFetch.joinToString(","),
                        "view" to "app",
                        "profile" to "portfolio",
                        "timeoutMs" to "2200",
                        "complete" to "1",
                        "adaptiveCompletion" to "1",
                        "statusInvestComplement" to "1",
                        "maxConcurrency" to "3",
                        "includeNews" to "0",
                        "nocache" to if (bypassCache) "1" else null
                    )
                )
                ?: getProxyJson(
                    "/api/v1/assets",
                    mapOf(
                        "tickers" to toFetch.joinToString(","),
                        "view" to "app",
                        "profile" to "turbo",
                        "timeoutMs" to "1500",
                        "complete" to "1",
                        "includeNews" to "0",
                        "nocache" to if (bypassCache) "1" else null
                    )
                )
            val root = unwrapValoraePayload(json)
            val assets = firstArray(
                root?.optJSONArray("assets"),
                root?.optJSONArray("items"),
                root?.optJSONArray("results"),
                root?.optArray("data.assets")
            )
            fun acceptMapped(mapped: B3AssetData?, fallbackTicker: String? = null) {
                val tickerCandidate = mapped?.ticker?.takeIf { it.isNotBlank() } ?: fallbackTicker
                if (mapped != null && !tickerCandidate.isNullOrBlank()) {
                    val cleanMappedTicker = tickerCandidate.trim().uppercase(Locale.ROOT)
                    val normalizedMapped = mapped.copy(ticker = cleanMappedTicker)
                    val safeMapped = if (assetIsGoodSnapshot(normalizedMapped)) {
                        saveBestSnapshot(normalizedMapped)
                        normalizedMapped
                    } else {
                        loadBestSnapshot(cleanMappedTicker)?.copy(
                            proxyStatus = normalizedMapped.proxyStatus.ifBlank { "PARTIAL" },
                            isPartial = true,
                            partialDataGuidance = normalizedMapped.partialDataGuidance.ifBlank { "Resposta parcial em lote; usando último snapshot bom." },
                            shouldKeepPreviousSnapshot = true,
                            fromLocalSnapshot = true
                        ) ?: normalizedMapped
                    }
                    out[cleanMappedTicker] = safeMapped
                    putInCache("asset_data_proxy_$cleanMappedTicker", safeMapped, 5)
                }
            }
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    acceptMapped(mapProxyAsset(assets.optJSONObject(i)))
                }
            }
            // Algumas versões do endpoint em lote podem devolver objeto indexado por ticker,
            // em vez de array: { results: { PETR4: {...}, MXRF11: {...} } }.
            // Sem este fallback a carteira fica com ativos ausentes mesmo quando o Proxy respondeu.
            val assetObjects = listOfNotNull(
                root?.optJSONObject("assets"),
                root?.optJSONObject("items"),
                root?.optJSONObject("results"),
                root?.optObject("data.assets"),
                root?.optObject("data.results")
            )
            for (obj in assetObjects) {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val item = obj.optJSONObject(key) ?: continue
                    acceptMapped(mapProxyAsset(item), key)
                }
            }
        }
        val missing = cleanTickers.filter { !out.containsKey(it) }
        if (missing.isNotEmpty()) {
            for (ticker in missing) {
                loadBestSnapshot(ticker)?.let { out[ticker] = it }
            }
            // Evita que uma resposta parcial do batch vire dezenas de chamadas individuais.
            // O restante continua aparecendo com snapshot local quando disponível.
            val stillMissing = cleanTickers.filter { !out.containsKey(it) }
            for (ticker in stillMissing.take(if (bypassCache) 8 else 4)) {
                fetchAssetData(ticker, bypassCache)?.let { out[ticker] = it }
            }
        }
        return out
    }

    private fun parseIsoDateMillis(value: String): Long {
        if (value.isBlank()) return 0L
        val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd")
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(value)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return 0L
    }

    fun fetchHistoricalChart(ticker: String, range: String = "1y"): List<ChartPoint> {
        val normalizedRange = normalizeProxyRange(range)
        val cacheKey = "history_proxy_${ticker.trim().uppercase(Locale.ROOT)}_$normalizedRange"
        getFromCache<List<ChartPoint>>(cacheKey)?.let { return it }
        val json = getProxyJson(
            "/api/v1/asset/history",
            mapOf("ticker" to ticker.trim().uppercase(Locale.ROOT), "range" to normalizedRange, "limit" to historyLimitForRange(normalizedRange))
        )
        val root = unwrapValoraePayload(json)
        val historyObj = root?.optJSONObject("history") ?: root?.optJSONObject("chart") ?: root?.optJSONObject("prices")
        val points = firstArray(
            root?.optJSONArray("points"),
            root?.optJSONArray("series"),
            root?.optJSONArray("history"),
            root?.optJSONArray("prices"),
            root?.optJSONArray("items"),
            root?.optArray("data.points"),
            root?.optArray("data.series"),
            root?.optArray("data.history"),
            root?.optArray("data.prices"),
            root?.optArray("data.items"),
            historyObj?.optJSONArray("points"),
            historyObj?.optJSONArray("series"),
            historyObj?.optJSONArray("result"),
            historyObj?.optJSONArray("items")
        )
        if (points != null && points.length() > 0) {
            val sdf = SimpleDateFormat(if (normalizedRange == "1D" || normalizedRange == "5D") "HH:mm" else "dd/MM", Locale.getDefault())
            val list = mutableListOf<ChartPoint>()
            for (i in 0 until points.length()) {
                val p = points.optJSONObject(i) ?: continue
                val close = firstNumber(
                    p.optAny("close"),
                    p.optAny("adjClose"),
                    p.optAny("value"),
                    p.optAny("valor"),
                    p.optAny("price"),
                    p.optAny("preco"),
                    p.optAny("regularMarketPrice"),
                    p.optAny("y")
                )
                if (close <= 0.0) continue
                val ts = parseFlexibleDateMillis(firstText(p.optAny("date"), p.optAny("data"), p.optAny("timestamp"), p.optAny("time"), p.optAny("datetime"), p.optAny("x")))
                val label = if (ts > 0L) sdf.format(Date(ts)) else firstText(p.optAny("label"), p.optAny("dateLabel"), "P${i + 1}")
                list.add(ChartPoint(if (ts > 0L) ts / 1000L else i.toLong(), label, close))
            }
            if (list.isNotEmpty()) {
                putInCache(cacheKey, list, rangeCacheTtlMinutes(normalizedRange))
                return list
            }
        }
        return if (directFallbackEnabled()) fetchHistoricalChartDirect(ticker, range) else emptyList()
    }

    fun fetchNews(ticker: String = ""): List<NewsItem> {
        val cacheKey = "news_proxy_${ticker.trim().uppercase(Locale.ROOT).ifBlank { "GERAL" }}"
        getFromCache<List<NewsItem>>(cacheKey)?.let { return it }
        val queryTicker = ticker.trim().uppercase(Locale.ROOT)
        val params = mutableMapOf<String, String?>("limit" to "40")
        if (queryTicker.isNotBlank()) params["ticker"] = queryTicker
        val json = getProxyJson("/api/v1/news", params)
        val root = unwrapValoraePayload(json)
        val dataObj = firstObject(root?.optJSONObject("data"), root?.optJSONObject("results"), root?.optJSONObject("news"), root?.optJSONObject("payload"))
        val items = firstArray(
            root?.optJSONArray("items"),
            root?.optJSONArray("news"),
            root?.optJSONArray("articles"),
            root?.optJSONArray("results"),
            root?.optArray("data.items"),
            root?.optArray("data.news"),
            root?.optArray("data.articles"),
            root?.optArray("results.items"),
            root?.optArray("results.news"),
            dataObj?.optJSONArray("items"),
            dataObj?.optJSONArray("news"),
            dataObj?.optJSONArray("articles")
        )
        if (items != null && items.length() > 0) {
            val out = mutableListOf<NewsItem>()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val sourceObj = item.optJSONObject("source")
                val pubRaw = firstText(
                    item.optAny("pubDate"),
                    item.optAny("date"),
                    item.optAny("publishedAt"),
                    item.optAny("published"),
                    item.optAny("createdAt"),
                    item.optAny("timestamp")
                )
                val ts = parseFlexibleDateMillis(pubRaw).takeIf { it > 0L } ?: parseIsoDateMillis(pubRaw)
                val formatted = if (ts > 0L) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts)) else pubRaw
                out.add(
                    NewsItem(
                        title = firstText(item.optAny("title"), item.optAny("headline"), item.optAny("name")),
                        link = firstText(item.optAny("link"), item.optAny("url"), item.optAny("href")),
                        pubDate = formatted,
                        source = firstText(item.optAny("sourceName"), sourceObj?.optAny("name"), item.optAny("provider"), root?.optAny("source")),
                        description = firstText(item.optAny("snippet"), item.optAny("summary"), item.optAny("description"), item.optAny("text")),
                        timestamp = ts
                    )
                )
            }
            val sorted = out.filter { it.title.isNotBlank() && it.link.isNotBlank() }.sortedByDescending { it.timestamp }
            if (sorted.isNotEmpty()) {
                putInCache(cacheKey, sorted, 5)
                return sorted
            }
        }
        return if (directFallbackEnabled()) fetchNewsDirect(ticker) else emptyList()
    }

    private fun positionsCacheSignature(positions: List<PortfolioProxyPosition>): String {
        return positions
            .map { p ->
                listOf(
                    p.ticker.trim().uppercase(Locale.ROOT),
                    String.format(Locale.ROOT, "%.4f", p.quantity),
                    String.format(Locale.ROOT, "%.4f", p.averagePrice),
                    p.type.trim().uppercase(Locale.ROOT),
                    String.format(Locale.ROOT, "%.4f", p.currentPrice),
                    String.format(Locale.ROOT, "%.2f", p.totalInvested),
                    p.firstPurchaseAt.toString()
                ).joinToString(":")
            }
            .sorted()
            .joinToString("|")
    }

    private fun parseActionPlanItems(array: JSONArray?): List<PortfolioProxyActionPlanItem> {
        if (array == null) return emptyList()
        val out = mutableListOf<PortfolioProxyActionPlanItem>()
        for (i in 0 until array.length()) {
            val raw = array.opt(i)
            val obj = raw as? JSONObject
            val message = firstText(obj?.optAny("message"), obj?.optAny("text"), obj?.optAny("description"), raw)
            if (message.isBlank()) continue
            out.add(
                PortfolioProxyActionPlanItem(
                    priority = firstText(obj?.optAny("priority"), obj?.optAny("level"), "info"),
                    code = firstText(obj?.optAny("code"), obj?.optAny("id")),
                    message = message
                )
            )
        }
        return out.take(8)
    }

    private fun parsePortfolioPositionRanking(array: JSONArray?): List<PortfolioPositionRankingItem> {
        if (array == null) return emptyList()
        val out = mutableListOf<PortfolioPositionRankingItem>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val ticker = firstText(item.optAny("ticker"), item.optAny("symbol"), item.optAny("ativo")).uppercase(Locale.ROOT)
            if (ticker.isBlank()) continue
            val reasons = item.optJSONArray("reasons") ?: item.optJSONArray("strengths") ?: item.optJSONArray("motivos")
            val reasonText = if (reasons != null && reasons.length() > 0) {
                (0 until reasons.length()).mapNotNull { idx -> firstText(reasons.opt(idx)).takeIf { it.isNotBlank() } }.take(2).joinToString(" · ")
            } else firstText(item.optAny("reason"), item.optAny("message"), item.optAny("explanation"))
            out.add(
                PortfolioPositionRankingItem(
                    rank = firstNumber(item.optAny("rank"), i + 1).toInt().takeIf { it > 0 } ?: (i + 1),
                    ticker = ticker,
                    score = firstNumber(item.optAny("score"), item.optAny("profileScore"), item.optAny("value")),
                    grade = firstText(item.optAny("grade"), item.optAny("rating")),
                    weightPercent = firstNumber(item.optAny("weightPercent"), item.optAny("percent"), item.optAny("peso")),
                    monthlyIncomeEstimated = firstNumber(item.optAny("monthlyIncomeEstimated"), item.optAny("monthlyIncome"), item.optAny("rendaMensal")),
                    reason = reasonText
                )
            )
        }
        return out.sortedBy { it.rank }.take(12)
    }

    private fun parseRebalanceActions(array: JSONArray?): List<PortfolioRebalanceAction> {
        if (array == null) return emptyList()
        val out = mutableListOf<PortfolioRebalanceAction>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val action = firstText(item.optAny("action"), item.optAny("acao"), "HOLD").uppercase(Locale.ROOT)
            val ticker = firstText(item.optAny("ticker"), item.optAny("symbol"), item.optAny("ativo")).uppercase(Locale.ROOT)
            val type = firstText(item.optAny("type"), item.optAny("class"), item.optAny("classe"))
            if (ticker.isBlank() && type.isBlank()) continue
            out.add(
                PortfolioRebalanceAction(
                    scope = firstText(item.optAny("scope"), item.optAny("escopo")),
                    ticker = ticker,
                    type = type,
                    action = action,
                    currentPercent = firstNumber(item.optAny("currentPercent"), item.optAny("current"), item.optAny("percentAtual")),
                    targetPercent = firstNumber(item.optAny("targetPercent"), item.optAny("target"), item.optAny("percentAlvo")),
                    deltaValue = firstNumber(item.optAny("deltaValue"), item.optAny("delta"), item.optAny("gapValue")),
                    estimatedQuantity = firstNumber(item.optAny("estimatedQuantity"), item.optAny("quantity"), item.optAny("quantidade"))
                )
            )
        }
        return out.sortedByDescending { kotlin.math.abs(it.deltaValue) }.take(12)
    }

    private fun normalizeMarketRankingDirection(raw: String): String {
        val text = raw.trim().lowercase(Locale.ROOT)
        return when {
            text.contains("alta") || text.contains("high") || text.contains("gain") || text.contains("winner") || text == "up" || text == "ups" || text.contains("positivo") -> "alta"
            text.contains("baixa") || text.contains("low") || text.contains("loss") || text.contains("loser") || text.contains("worst") || text == "down" || text == "downs" || text.contains("queda") || text.contains("negativo") -> "baixa"
            else -> raw.trim()
        }
    }

    private fun normalizeRankingPercentDisplay(raw: String, numeric: Double): String {
        val text = raw.trim()
        if (text.isBlank()) return ""
        if (text.contains("%")) return text
        val parsed = parseLocaleFinancialNumber(text)
        val value = when {
            parsed != 0.0 && kotlin.math.abs(parsed) <= 100.0 -> parsed
            numeric != 0.0 && kotlin.math.abs(numeric) <= 100.0 -> numeric
            else -> 0.0
        }
        return if (value != 0.0) {
            val prefix = if (value > 0.0) "+" else ""
            String.format(Locale("pt", "BR"), "%s%.2f%%", prefix, value)
        } else {
            text
        }
    }

    private fun marketRankingItemLooksHigh(item: MarketRankingItem): Boolean {
        val direction = normalizeMarketRankingDirection(item.direction).lowercase(Locale.ROOT)
        if (direction == "alta") return true
        if (direction == "baixa") return false
        val display = "${item.changeDisplay} ${item.displayValue}".trim()
        if (display.contains("▲") || display.startsWith("+")) return true
        if (display.contains("▼") || display.startsWith("-")) return false
        return item.changePercent > 0.0 && item.changePercent.isFinite()
    }

    private fun marketRankingItemLooksLow(item: MarketRankingItem): Boolean {
        val direction = normalizeMarketRankingDirection(item.direction).lowercase(Locale.ROOT)
        if (direction == "baixa") return true
        if (direction == "alta") return false
        val display = "${item.changeDisplay} ${item.displayValue}".trim()
        if (display.contains("▼") || display.startsWith("-")) return true
        if (display.contains("▲") || display.startsWith("+")) return false
        return item.changePercent < 0.0 && item.changePercent.isFinite()
    }

    private fun normalizeSplitMarketRankingItems(items: List<MarketRankingItem>, direction: String): List<MarketRankingItem> {
        val normalizedDirection = normalizeMarketRankingDirection(direction)
        return items
            .distinctBy { it.ticker.trim().uppercase(Locale.ROOT) }
            .sortedBy { it.rank.takeIf { rank -> rank > 0 } ?: Int.MAX_VALUE }
            .take(15)
            .mapIndexed { index, item ->
                item.copy(
                    rank = index + 1,
                    direction = item.direction.ifBlank { normalizedDirection }
                )
            }
    }

    private fun marketRankingItemFromObject(item: JSONObject?, index: Int, directionFallback: String = ""): MarketRankingItem? {
        if (item == null) return null
        val ticker = firstText(
            item.optAny("ticker"),
            item.optAny("codigo"),
            item.optAny("symbol"),
            item.optAny("code"),
            item.optAny("papel"),
            item.optAny("ativo"),
            item.optAny("asset"),
            item.optAny("assetCode"),
            item.optAny("stock"),
            item.optAny("acao")
        ).uppercase(Locale.ROOT)
        if (ticker.isBlank()) return null
        val explanationObj = item.optJSONObject("explanation")
        val strengths = explanationObj?.optJSONArray("strengths")
        val weaknesses = explanationObj?.optJSONArray("weaknesses")
        val explanation = when {
            strengths != null && strengths.length() > 0 -> firstText(strengths.opt(0))
            weaknesses != null && weaknesses.length() > 0 -> firstText(weaknesses.opt(0))
            else -> firstText(item.optAny("explanation"), item.optAny("reason"), item.optAny("message"))
        }
        val quote = firstObject(item.optJSONObject("quote"), item.optJSONObject("cotacao"), item.optJSONObject("marketData"), item.optJSONObject("priceInfo"))
        val price = firstNumber(
            item.optAny("price"),
            item.optAny("lastPrice"),
            item.optAny("currentPrice"),
            item.optAny("cotacao"),
            item.optAny("cotacaoAtual"),
            item.optAny("preco"),
            item.optAny("precoAtual"),
            item.optAny("valorAtual"),
            item.optAny("last"),
            item.optAny("last_price"),
            item.optAny("current_price"),
            item.optAny("close"),
            item.optAny("regularMarketPrice"),
            item.optAny("valor"),
            item.optAny("valorUnitario"),
            quote?.optAny("price"),
            quote?.optAny("lastPrice"),
            quote?.optAny("currentPrice"),
            quote?.optAny("cotacao"),
            quote?.optAny("preco"),
            quote?.optAny("valor"),
            quote?.optAny("regularMarketPrice")
        )
        val changePercent = firstNumber(
            item.optAny("changePercent"),
            item.optAny("variationPercent"),
            item.optAny("dailyChangePercent"),
            item.optAny("variacaoPercentual"),
            item.optAny("percentual"),
            item.optAny("percent"),
            item.optAny("percentage"),
            item.optAny("pct"),
            item.optAny("variacao"),
            item.optAny("change"),
            item.optAny("regularMarketChangePercent"),
            item.optAny("variacaoDia"),
            item.optAny("dailyChange"),
            item.optAny("dailyVariation"),
            item.optAny("change_pct"),
            item.optAny("change_percentage"),
            item.optAny("changePct"),
            item.optAny("pctChange"),
            item.optAny("percent_change"),
            item.optAny("variation"),
            item.optAny("variation_percent"),
            item.optAny("variacao_pct"),
            item.optAny("percentualDia"),
            item.optAny("rentabilidade"),
            quote?.optAny("changePercent"),
            quote?.optAny("variationPercent"),
            quote?.optAny("variacao"),
            quote?.optAny("regularMarketChangePercent")
        )
        val priceDisplay = firstText(
            item.optAny("priceDisplay"),
            item.optAny("currentPriceDisplay"),
            item.optAny("lastPriceDisplay"),
            item.optAny("precoFormatado"),
            item.optAny("cotacaoFormatada"),
            item.optAny("cotacaoAtual"),
            item.optAny("precoAtual"),
            item.optAny("preco"),
            item.optAny("valorAtual"),
            quote?.optAny("priceDisplay"),
            quote?.optAny("currentPriceDisplay"),
            quote?.optAny("precoFormatado"),
            quote?.optAny("cotacaoFormatada")
        )
        val rawChangeDisplay = firstText(
            item.optAny("changeDisplay"),
            item.optAny("variacaoFormatada"),
            item.optAny("variationDisplay"),
            item.optAny("variacaoPercentual"),
            item.optAny("percentDisplay"),
            item.optAny("percentual"),
            item.optAny("percent"),
            item.optAny("changePct"),
            item.optAny("pctChange"),
            item.optAny("percent_change"),
            item.optAny("variacaoDia"),
            item.optAny("dailyChange"),
            item.optAny("variacao"),
            quote?.optAny("changeDisplay"),
            quote?.optAny("variacaoFormatada"),
            quote?.optAny("variationDisplay"),
            quote?.optAny("variacaoDia"),
            quote?.optAny("dailyChange")
        )
        val changeDisplay = normalizeRankingPercentDisplay(rawChangeDisplay, changePercent)
        val value = firstNumber(
            item.optAny("value"),
            item.optAny("valorRanking"),
            item.optAny("metricValue"),
            item.optAny("rankingValue"),
            item.optAny("score"),
            item.optAny("profileScore"),
            item.optAny("dividendYield"),
            item.optAny("yield"),
            item.optAny("variacaoDia"),
            item.optAny("dailyChange"),
            item.optAny("variacao")
        )
        val display = firstText(item.optAny("displayValue"), item.optAny("variacaoFormatada"), item.optAny("changeDisplay"), item.optAny("percentDisplay"), item.optAny("percentual"), item.optAny("percent"), item.optAny("variacao"), item.optAny("preco"), item.optAny("value"), item.optAny("score"), item.optAny("profileScore"))
        val volume = firstNumber(item.optAny("volume"), item.optAny("vol"), item.optAny("financialVolume"), item.optAny("volumeFinanceiro"), item.optAny("volume_financeiro"), item.optAny("turnover")).takeIf { it.isFinite() } ?: 0.0
        val setor = firstText(item.optAny("setor"), item.optAny("sector"), item.optAny("industrialSector"))
        val segmento = firstText(item.optAny("segmento"), item.optAny("segment"), item.optAny("subSector"))
        val url = firstText(item.optAny("url"), item.optAny("link"), item.optAny("sourceUrl"))

        return MarketRankingItem(
            rank = firstNumber(item.optAny("rank"), item.optAny("position"), item.optAny("posicao"), item.optAny("ordem"), item.optAny("order"), item.optAny("index"), index + 1).toInt().takeIf { it > 0 } ?: (index + 1),
            ticker = ticker,
            name = firstText(item.optAny("name"), item.optAny("nome"), item.optAny("company"), item.optAny("companyName"), item.optAny("empresa"), item.optAny("razaoSocial"), item.optAny("description"), item.optAny("descricao"))
                .replace(Regex("\\s*\\([+-]?[0-9.,\\s]*%?\\)"), "")
                .replace("(+,%)", "")
                .replace("()", "")
                .trim(),
            value = value,
            displayValue = display,
            price = price,
            priceDisplay = priceDisplay,
            changePercent = changePercent,
            changeDisplay = changeDisplay,
            grade = firstText(item.optAny("grade"), item.optAny("rating")),
            direction = normalizeMarketRankingDirection(firstText(item.optAny("direction"), item.optAny("tipo"), item.optAny("type"), item.optAny("category"), item.optAny("categoria"), item.optAny("movement"), item.optAny("movimento"), item.optAny("side"), directionFallback)),
            source = firstText(item.optAny("source"), item.optAny("fonte"), item.optAny("provider"), "Serviço de dados VALORAE"),
            explanation = explanation,
            volume = volume,
            setor = setor,
            segmento = segmento,
            url = url
        )
    }

    private fun parseMarketRankingList(array: JSONArray?, directionFallback: String = ""): List<MarketRankingItem> {
        if (array == null) return emptyList()
        val out = mutableListOf<MarketRankingItem>()
        for (i in 0 until array.length()) {
            val raw = array.opt(i)
            val obj = when (raw) {
                is JSONObject -> raw
                is String -> JSONObject().put("ticker", raw)
                else -> null
            }
            marketRankingItemFromObject(obj, i, directionFallback)?.let { out.add(it) }
        }
        return out
            .distinctBy { it.ticker.trim().uppercase(Locale.ROOT) }
            .sortedBy { it.rank }
            .take(15)
    }

    private fun parseProfileRanking(obj: JSONObject?, key: String): List<MarketRankingItem> {
        return parseMarketRankingList(obj?.optJSONArray(key))
    }

    fun parseMarketRankingSnapshot(root: JSONObject?, fallbackType: String): MarketRankingSnapshot? {
        if (root == null) return null
        val data = root.optJSONObject("data")
        val payload = root.optJSONObject("payload")
        val result = root.optJSONObject("result")
        val results = root.optJSONObject("results")
        val rankings = firstObject(
            root.optJSONObject("rankings"),
            data?.optJSONObject("rankings"),
            payload?.optJSONObject("rankings"),
            result?.optJSONObject("rankings"),
            results?.optJSONObject("rankings"),
            root.optJSONObject("marketRankings"),
            data?.optJSONObject("marketRankings"),
            payload?.optJSONObject("marketRankings"),
            result?.optJSONObject("marketRankings"),
            results?.optJSONObject("marketRankings")
        )
        val profiles = firstObject(
            root.optJSONObject("profiles"),
            data?.optJSONObject("profiles"),
            payload?.optJSONObject("profiles"),
            result?.optJSONObject("profiles"),
            results?.optJSONObject("profiles"),
            rankings?.optJSONObject("profiles")
        )
        val warnings = mutableListOf<String>()
        val inputErrors = firstArray(
            root.optJSONArray("inputErrors"),
            root.optJSONArray("errors"),
            data?.optJSONArray("errors"),
            payload?.optJSONArray("errors"),
            result?.optJSONArray("errors"),
            results?.optJSONArray("errors")
        )
        if (inputErrors != null) {
            for (i in 0 until inputErrors.length()) {
                val raw = inputErrors.opt(i)
                val obj = raw as? JSONObject
                val msg = firstText(obj?.optAny("error"), obj?.optAny("message"), raw)
                if (msg.isNotBlank()) warnings.add(msg)
            }
        }
        firstText(
            root.optAny("warning"),
            root.optAny("error"),
            data?.optAny("warning"),
            payload?.optAny("warning"),
            result?.optAny("warning"),
            results?.optAny("warning")
        ).takeIf { it.isNotBlank() }?.let { warnings.add(it) }

        fun rankingArray(vararg keys: String): JSONArray? {
            for (key in keys) {
                val arr = firstArray(
                    rankings?.optJSONArray(key),
                    root.optJSONArray(key),
                    data?.optJSONArray(key),
                    payload?.optJSONArray(key),
                    result?.optJSONArray(key),
                    results?.optJSONArray(key),
                    root.optArray("rankings.$key"),
                    root.optArray("data.rankings.$key"),
                    root.optArray("payload.rankings.$key"),
                    root.optArray("result.rankings.$key"),
                    root.optArray("results.rankings.$key")
                )
                if (arr != null && arr.length() > 0) return arr
            }
            return null
        }

        fun profileArray(vararg keys: String): JSONArray? {
            for (key in keys) {
                val arr = firstArray(
                    profiles?.optJSONArray(key),
                    rankings?.optArray("profiles.$key"),
                    root.optArray("profiles.$key"),
                    data?.optArray("profiles.$key"),
                    payload?.optArray("profiles.$key"),
                    result?.optArray("profiles.$key"),
                    results?.optArray("profiles.$key")
                )
                if (arr != null && arr.length() > 0) return arr
            }
            return null
        }

        val scoreList = parseMarketRankingList(rankingArray("score", "scores", "scoreValorae", "valoraeScore", "ranking", "items"))
        val dyList = parseMarketRankingList(rankingArray("dividendYield", "dividend_yield", "dy", "dividendos", "yield"))
        val pvpList = parseMarketRankingList(rankingArray("pvp", "p_vp", "priceToBook", "price_to_book", "maisBaratasPvp", "mais_baratas_pvp", "baratasPvp"))
        val plList = parseMarketRankingList(rankingArray("pl", "p_l", "priceEarnings", "price_earnings", "menoresPl", "menoresPL", "menores_pl"))
        val roeList = parseMarketRankingList(rankingArray("roe", "maioresRoe", "maioresROE", "maiores_roe"))
        val roicList = parseMarketRankingList(rankingArray("roic", "maioresRoic", "maioresROIC", "maiores_roic"))
        val qualityList = parseMarketRankingList(rankingArray("quality", "qualidade", "dataQuality", "data_quality", "coverage"))
        val valueList = parseMarketRankingList(rankingArray("value", "valor", "baratas", "cheap", "graham", "bazin")).ifEmpty { pvpList.ifEmpty { plList } }
        val conservative = parseMarketRankingList(profileArray("conservador", "conservative", "buyAndHold", "buy_hold"))
        val growth = parseMarketRankingList(profileArray("crescimento", "growth", "crescimentoLucro", "crescimentoReceita"))
        val dividendsProfile = parseMarketRankingList(profileArray("dividendos", "dividends", "renda", "income"))
        val valueProfile = parseMarketRankingList(profileArray("valor", "value", "baratas"))
        val fiiIncome = parseMarketRankingList(profileArray("rendaFii", "incomeFii", "fiiRenda", "rendaFIIs", "renda_fii", "income_fii"))
        val genericMovers = parseMarketRankingList(
            rankingArray(
                "marketMovers", "market_movers", "movements", "movimentacoes", "movers", "dailyMovers", "daily_movers",
                "ranking", "items", "rows", "result", "list", "ativos"
            )
        )
        val explicitHighs = parseMarketRankingList(
            rankingArray(
                "altas", "alta", "highs", "high", "gainers", "gain", "maioresAltas", "maiores_altas",
                "topGainers", "top_gainers", "topHighs", "top_highs", "up", "ups", "winners", "best"
            ),
            "alta"
        )
        val explicitLows = parseMarketRankingList(
            rankingArray(
                "baixas", "baixa", "lows", "low", "losers", "loss", "maioresBaixas", "maiores_baixas",
                "topLosers", "top_losers", "topLows", "top_lows", "down", "downs", "worst", "fallers"
            ),
            "baixa"
        )
        val highs = explicitHighs.ifEmpty {
            normalizeSplitMarketRankingItems(genericMovers.filter { marketRankingItemLooksHigh(it) }, "alta")
        }
        val lows = explicitLows.ifEmpty {
            normalizeSplitMarketRankingItems(genericMovers.filter { marketRankingItemLooksLow(it) }, "baixa")
        }
        val hasAny = listOf(scoreList, dyList, pvpList, plList, roeList, roicList, qualityList, valueList, conservative, growth, dividendsProfile, valueProfile, fiiIncome, highs, lows).any { it.isNotEmpty() }
        if (!hasAny && warnings.isEmpty()) return null
        return MarketRankingSnapshot(
            type = firstText(root.optAny("type"), data?.optAny("type"), payload?.optAny("type"), result?.optAny("type"), results?.optAny("type"), fallbackType).uppercase(Locale.ROOT),
            source = firstText(root.optAny("rankingSource"), root.optAny("source"), data?.optAny("rankingSource"), data?.optAny("source"), payload?.optAny("rankingSource"), payload?.optAny("source"), result?.optAny("rankingSource"), result?.optAny("source"), results?.optAny("rankingSource"), results?.optAny("source"), "Serviço de dados VALORAE"),
            fallbackUsed = root.optBoolean("fallbackUsed", data?.optBoolean("fallbackUsed", payload?.optBoolean("fallbackUsed", result?.optBoolean("fallbackUsed", results?.optBoolean("fallbackUsed", false) ?: false) ?: false) ?: false) ?: false),
            score = scoreList,
            dividendYield = dyList,
            pvp = pvpList,
            pl = plList,
            roe = roeList,
            roic = roicList,
            quality = qualityList,
            value = valueList,
            conservative = conservative,
            growth = growth,
            dividendsProfile = dividendsProfile,
            valueProfile = valueProfile,
            incomeFii = fiiIncome,
            highs = highs,
            lows = lows,
            warnings = warnings.distinct().take(8)
        )
    }

    private fun formatCurrencyPtBr(value: Double): String {
        return if (value > 0.0 && value.isFinite()) "R$ ${String.format(Locale("pt", "BR"), "%.2f", value)}" else ""
    }

    private fun enrichMarketMoverPrices(snapshot: MarketRankingSnapshot): MarketRankingSnapshot {
        val movers = (snapshot.highs + snapshot.lows)
            .filter { item -> item.ticker.isNotBlank() }
        if (movers.isEmpty()) return snapshot

        val tickers = movers.map { it.ticker.trim().uppercase(Locale.ROOT) }.distinct().take(30)
        val assets = runCatching { fetchAssetsData(tickers, bypassCache = false) }.getOrElse { emptyMap() }
        if (assets.isEmpty()) return snapshot

        fun enrich(items: List<MarketRankingItem>): List<MarketRankingItem> = items.map { item ->
            val asset = assets[item.ticker.trim().uppercase(Locale.ROOT)] ?: return@map item
            val safePrice = asset.price.takeIf { it > 0.0 && it.isFinite() }
                ?: item.price.takeIf { it > 0.0 && it.isFinite() }
                ?: 0.0
            val valueLooksLikePercent = item.displayValue.contains("%") || item.changeDisplay.contains("%")
            val rankingChange = item.changePercent.takeIf { it != 0.0 && it.isFinite() }
                ?: item.value.takeIf { valueLooksLikePercent && it != 0.0 && it.isFinite() && kotlin.math.abs(it) <= 100.0 }
                ?: 0.0
            val auxiliaryChange = asset.changePercent.takeIf { it != 0.0 && it.isFinite() } ?: 0.0
            // Rankings de altas/baixas devem preservar a variação capturada do ranking.
            // A cotação auxiliar serve apenas como fallback quando o ranking veio sem percentual.
            val safeChange = if (rankingChange != 0.0) rankingChange else auxiliaryChange
            val originalDisplay = item.changeDisplay.ifBlank {
                if (item.displayValue.contains("%")) item.displayValue else ""
            }
            val formattedDailyChange = originalDisplay.ifBlank {
                if (safeChange != 0.0) {
                    val prefix = if (safeChange > 0.0) "+" else ""
                    String.format(Locale("pt", "BR"), "%s%.2f%%", prefix, safeChange)
                } else {
                    ""
                }
            }
            
            item.copy(
                price = safePrice,
                priceDisplay = item.priceDisplay.ifBlank { formatCurrencyPtBr(safePrice) },
                changePercent = safeChange,
                changeDisplay = formattedDailyChange,
                value = if (rankingChange != 0.0) item.value else if (safeChange != 0.0) safeChange else item.value,
                displayValue = item.displayValue.ifBlank { formattedDailyChange }
            )
        }

        return snapshot.copy(
            highs = enrich(snapshot.highs),
            lows = enrich(snapshot.lows)
        )
    }

    fun fetchPortfolioAnalysis(positions: List<PortfolioProxyPosition>): PortfolioProxyAnalysis? {
        if (positions.isEmpty()) return null
        val cacheKey = "portfolio_analysis_${positionsCacheSignature(positions)}"
        getFromCache<PortfolioProxyAnalysis>(cacheKey)?.let { return it }
        val payload = JSONObject()
            .put("positions", positionArray(positions))
            .put("mode", "complete")
            .put("complete", true)
            .put("view", "standard")
            .put("includeAssets", false)
            .put("include", JSONArray(listOf("allocation", "risk", "income", "events", "dividends", "ipca", "rebalance", "quality", "warnings", "intelligence")))
        val json = postProxyJson("/api/v1/portfolio/analyze", payload) ?: return null
        val root = unwrapValoraePayload(json) ?: return null
        val summary = root.optJSONObject("summary") ?: root
        val portfolioScore = root.optJSONObject("portfolioScore")
        val allocation = root.optJSONObject("allocation") ?: summary.optJSONObject("allocation")
        val income = root.optJSONObject("income") ?: summary.optJSONObject("income")
        val quality = root.optJSONObject("quality") ?: summary.optJSONObject("quality") ?: root.optJSONObject("intelligence")?.optJSONObject("dataCompleteness")
        val risks = root.optJSONObject("risk") ?: summary.optJSONObject("risk")
        val concentration = risks?.optJSONObject("concentration")
        val diversification = risks?.optJSONObject("diversification")
        val diagnostics = root.optJSONObject("diagnostics")
        val intelligence = root.optJSONObject("intelligence")
        val healthScoreObj = intelligence?.optJSONObject("healthScore")
        val incomeStabilityObj = intelligence?.optJSONObject("incomeStabilityScore")
        val incomeCoverageObj = intelligence?.optJSONObject("incomeCoverage") ?: intelligence?.optJSONObject("dividendCoverage")
        val technologyReadinessObj = intelligence?.optJSONObject("technologyReadiness")
        val dataCompletenessObj = intelligence?.optJSONObject("dataCompleteness") ?: quality
        val actionPlan = parseActionPlanItems(firstArray(intelligence?.optJSONArray("actionPlan"), root.optJSONArray("actionPlan")))
        val positionRanking = parsePortfolioPositionRanking(firstArray(
            intelligence?.optJSONObject("positionRanking")?.optJSONArray("items"),
            intelligence?.optJSONObject("positionRanking")?.optJSONArray("ranking"),
            intelligence?.optJSONArray("positionRanking"),
            root.optJSONArray("positionRanking")
        ))
        val rebalanceActions = parseRebalanceActions(firstArray(
            root.optJSONObject("rebalance")?.optJSONArray("actions"),
            intelligence?.optJSONObject("rebalanceRoadmap")?.optJSONArray("actions"),
            intelligence?.optJSONObject("rebalanceRoadmap")?.optJSONArray("buys")
        ))
        val insightsArray = firstArray(root.optJSONArray("insights"), root.optJSONArray("warnings"), summary.optJSONArray("warnings"), diagnostics?.optJSONArray("warnings"))
        val warnings = mutableListOf<String>()
        if (insightsArray != null) {
            for (i in 0 until insightsArray.length()) {
                val raw = insightsArray.opt(i)
                val obj = raw as? JSONObject
                val warning = firstText(obj?.optAny("message"), obj?.optAny("text"), obj?.optAny("description"), raw)
                if (warning.isNotBlank()) warnings.add(warning)
            }
        }
        val byTicker = allocation?.optJSONArray("byTicker")
        val topHolding = firstText(
            concentration?.optJSONArray("topAssets")?.optJSONObject(0)?.optAny("ticker"),
            byTicker?.optJSONObject(0)?.optAny("ticker"),
            byTicker?.optJSONObject(0)?.optAny("key"),
            summary.optAny("topHolding")
        )
        val classCount = firstNumber(diversification?.optAny("assetClasses"))
        val sectorCount = firstNumber(diversification?.optAny("sectors"))
        val diversificationLabel = firstText(
            summary.optAny("diversificationLabel"),
            allocation?.optAny("label"),
            if (classCount > 0 || sectorCount > 0) "${classCount.toInt()} classes · ${sectorCount.toInt()} setores" else null,
            "Não medido"
        )
        val riskLabel = firstText(
            risks?.optAny("label"),
            risks?.optAny("riskLabel"),
            risks?.optAny("grade")?.let { "Grau $it" },
            summary.optAny("riskLabel"),
            "Não medido"
        )
        val result = PortfolioProxyAnalysis(
            score = firstNumber(portfolioScore?.optAny("value"), portfolioScore?.optAny("score"), summary.optAny("score"), quality?.optAny("score")),
            riskLabel = riskLabel,
            diversificationLabel = diversificationLabel,
            concentrationPercent = firstNumber(concentration?.optAny("top1Percent"), summary.optAny("concentrationPercent"), allocation?.optAny("concentrationPercent"), allocation?.optAny("top5Percent")),
            topHolding = topHolding,
            monthlyDividendEstimate = firstNumber(income?.optAny("monthlyIncomeEstimated"), income?.optAny("monthly"), income?.optAny("monthlyEstimate"), summary.optAny("monthlyDividendEstimate")),
            annualDividendEstimate = firstNumber(income?.optAny("annualIncomeEstimated"), income?.optAny("annual"), income?.optAny("annualEstimate"), summary.optAny("annualDividendEstimate")),
            dataQuality = firstNumber(dataCompletenessObj?.optAny("score"), dataCompletenessObj?.optAny("percent"), dataCompletenessObj?.optAny("completeness"), quality?.optAny("score"), summary.optAny("averageQualityScore"), summary.optAny("dataQuality")),
            healthScore = firstNumber(healthScoreObj?.optAny("score"), healthScoreObj?.optAny("value"), root.optJSONObject("portfolioScore")?.optAny("value")),
            incomeStabilityScore = firstNumber(incomeStabilityObj?.optAny("score"), incomeStabilityObj?.optAny("value")),
            technologyReadinessScore = firstNumber(technologyReadinessObj?.optAny("score"), technologyReadinessObj?.optAny("value")),
            incomePayerPercent = firstNumber(incomeCoverageObj?.optAny("incomePayerPercent"), incomeCoverageObj?.optAny("payersPercent"), incomeCoverageObj?.optAny("percent")),
            allocationByClass = firstPairList(
                allocation?.optJSONArray("byType"),
                allocation?.optJSONArray("byClass"),
                allocation?.optJSONArray("classes"),
                root.optJSONArray("allocationByClass")
            ),
            allocationBySector = firstPairList(
                allocation?.optJSONArray("bySector"),
                allocation?.optJSONArray("sectors"),
                root.optJSONArray("allocationBySector")
            ),
            actionPlan = actionPlan,
            positionRanking = positionRanking,
            rebalanceActions = rebalanceActions,
            warnings = (warnings + actionPlan.map { it.message }).distinct().take(8),
            source = "Serviço de dados VALORAE"
        )
        putInCache(cacheKey, result, 5)
        return result
    }

    fun fetchMarketRankings(tickers: List<String> = emptyList(), type: String = "ACAO", live: Boolean = false, complete: Boolean = true, strict: Boolean = true): MarketRankingSnapshot? {
        val cleanTickers = tickers
            .map { it.trim().uppercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(15)
        val normalizedType = type.trim().uppercase(Locale.ROOT).ifBlank { "ACAO" }
        val captureKey = if (complete) "complete" else "auto"
        val cacheKey = "market_rankings_${normalizedType}_${if (live) "live" else cleanTickers.joinToString(",")}_${captureKey}_${if (strict) "strict" else "flex"}" 
        getFromCache<MarketRankingSnapshot>(cacheKey)?.let { return it }

        var json: JSONObject? = null
        if (complete) {
            val params = mutableMapOf<String, String?>(
                "type" to normalizedType,
                "profile" to "deep",
                "timeoutMs" to when {
                    live -> "14000"
                    else -> "18000"
                },
                "source" to if (live && cleanTickers.isEmpty()) "home" else "compare",
                "mode" to "complete",
                "limit" to if (live && cleanTickers.isEmpty()) "6" else "15",
                "minRows" to if (live && cleanTickers.isEmpty()) "6" else "6",
                "complete" to "1"
            )
            if (strict) params["strict"] = "1"
            if (cleanTickers.isNotEmpty()) params["tickers"] = cleanTickers.joinToString(",")
            json = runCatching { getProxyJson("/api/v1/market/rankings", params) }.getOrNull()
        }

        if (json == null) {
            val params = mutableMapOf<String, String?>(
                "type" to normalizedType,
                "profile" to "portfolio",
                "timeoutMs" to when {
                    live -> "9000"
                    else -> "6000"
                },
                "source" to if (live && cleanTickers.isEmpty()) "home" else "compare",
                "mode" to "auto",
                "limit" to if (live && cleanTickers.isEmpty()) "6" else "15",
                "minRows" to if (live && cleanTickers.isEmpty()) "6" else "3"
            )
            if (cleanTickers.isNotEmpty()) params["tickers"] = cleanTickers.joinToString(",")
            json = runCatching { getProxyJson("/api/v1/market/rankings", params) }.getOrNull()
        }

        var parsed = json?.let { parseMarketRankingSnapshot(unwrapValoraePayload(it), normalizedType) }

        if ((parsed == null || (parsed.highs.isEmpty() && parsed.lows.isEmpty() && parsed.score.isEmpty())) && complete) {
            val params = mutableMapOf<String, String?>(
                "type" to normalizedType,
                "profile" to "portfolio",
                "timeoutMs" to "6000",
                "source" to if (live && cleanTickers.isEmpty()) "home" else "compare",
                "mode" to "auto",
                "limit" to if (live && cleanTickers.isEmpty()) "6" else "15",
                "minRows" to if (live && cleanTickers.isEmpty()) "6" else "3"
            )
            if (cleanTickers.isNotEmpty()) params["tickers"] = cleanTickers.joinToString(",")
            val fallbackJson = runCatching { getProxyJson("/api/v1/market/rankings", params) }.getOrNull()
            if (fallbackJson != null) {
                val fallbackParsed = parseMarketRankingSnapshot(unwrapValoraePayload(fallbackJson), normalizedType)
                if (fallbackParsed != null && (fallbackParsed.highs.isNotEmpty() || fallbackParsed.lows.isNotEmpty() || fallbackParsed.score.isNotEmpty())) {
                    parsed = fallbackParsed
                }
            }
        }

        val finalParsed = parsed ?: return null
        val snapshot = if (live) enrichMarketMoverPrices(finalParsed) else finalParsed
        putInCache(cacheKey, snapshot, if (live) 5 else if (complete) 10 else 15)
        return snapshot
    }

    fun fetchPortfolioRankings(positions: List<PortfolioProxyPosition>): MarketRankingSnapshot? {
        val tickers = positions.map { it.ticker }.filter { it.isNotBlank() }.distinct().take(15)
        if (tickers.isEmpty()) return null
        val fiiCount = positions.count { it.type.equals("FII", true) || inferIsFii(it.ticker) }
        val type = if (fiiCount > positions.size / 2) "FII" else "ACAO"
        return fetchMarketRankings(tickers = tickers, type = type, live = false, complete = true, strict = true)
    }

    fun fetchLiveStockRankings(complete: Boolean = true): MarketRankingSnapshot? {
        return fetchMarketRankings(
            tickers = emptyList(),
            type = "ACAO",
            live = true,
            complete = complete,
            strict = complete
        )
    }

    fun fetchStockFundamentalRankings(complete: Boolean = true): MarketRankingSnapshot? {
        return fetchMarketRankings(tickers = emptyList(), type = "ACAO", live = false, complete = complete, strict = complete)
    }

    fun fetchFiiFundamentalRankings(complete: Boolean = true): MarketRankingSnapshot? {
        return fetchMarketRankings(tickers = emptyList(), type = "FII", live = false, complete = complete, strict = complete)
    }

    fun fetchPortfolioHistory(positions: List<PortfolioProxyPosition>, range: String = "1Y"): List<PortfolioHistoryPoint> {
        if (positions.isEmpty()) return emptyList()
        val normalizedRange = normalizeProxyRange(range)
        val cacheKey = "portfolio_history_${normalizedRange}_${positionsCacheSignature(positions)}"
        getFromCache<List<PortfolioHistoryPoint>>(cacheKey)?.let { return it }
        val payload = JSONObject()
            .put("positions", positionArray(positions))
            .put("range", normalizedRange)
            .put("mode", "complete")
            .put("complete", true)
            .put("includeDividends", true)
            .put("includeBenchmark", true)
            .put("benchmark", "IPCA")
            .put("limit", historyLimitForRange(normalizedRange).toIntOrNull() ?: 370)
        val json = postProxyJson("/api/v1/portfolio/history", payload) ?: return emptyList()
        val root = unwrapValoraePayload(json) ?: return emptyList()
        val points = firstArray(
            root.optJSONArray("points"),
            root.optJSONArray("history"),
            root.optJSONArray("series"),
            root.optJSONArray("items"),
            root.optArray("data.points"),
            root.optArray("data.series")
        ) ?: return emptyList()
        val out = mutableListOf<PortfolioHistoryPoint>()
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        for (i in 0 until points.length()) {
            val p = points.optJSONObject(i) ?: continue
            val total = firstNumber(p.optAny("totalValue"), p.optAny("value"), p.optAny("portfolioValue"), p.optAny("close"))
            val invested = firstNumber(p.optAny("investedValue"), p.optAny("invested"), p.optAny("costBasis"))
            val explicitReturn = firstNumber(
                p.optAny("returnPercent"),
                p.optAny("returnPct"),
                p.optAny("variationPct"),
                p.optAny("unrealizedPnLPct"),
                p.optAny("unrealizedPnLPercent"),
                p.optAny("periodVariationPct")
            )
            val computedReturn = if (invested > 0.0 && total > 0.0) ((total - invested) / invested) * 100.0 else 0.0
            val ret = if (explicitReturn != 0.0) explicitReturn else computedReturn
            val ts = parseFlexibleDateMillis(firstText(p.optAny("date"), p.optAny("time"), p.optAny("timestamp")))
            if (total <= 0.0 && invested <= 0.0) continue
            out.add(
                PortfolioHistoryPoint(
                    timestamp = if (ts > 0L) ts / 1000L else i.toLong(),
                    dateLabel = if (ts > 0L) sdf.format(Date(ts)) else firstText(p.optAny("label"), "P${i + 1}"),
                    totalValue = total,
                    investedValue = invested,
                    returnPercent = ret,
                    source = firstText(root.optAny("source"), "Serviço de dados VALORAE")
                )
            )
        }
        if (out.isNotEmpty()) putInCache(cacheKey, out, 3)
        return out
    }

    fun fetchIpcaSeries(months: Int = 12): List<IpcaPoint> {
        val safeMonths = months.coerceIn(1, 120)
        val cacheKey = "ipca_series_$safeMonths"
        getFromCache<List<IpcaPoint>>(cacheKey)?.let { return it }
        val json = getProxyJson("/api/v1/market/ipca", mapOf(
            "last" to safeMonths.toString(),
            "months" to safeMonths.toString(),
            "range" to "${safeMonths}M",
            "mode" to "complete",
            "complete" to "1"
        )) ?: return emptyList()
        val root = unwrapValoraePayload(json) ?: return emptyList()
        val points = firstArray(
            root.optJSONArray("points"),
            root.optJSONArray("series"),
            root.optJSONArray("items"),
            root.optArray("data.points"),
            root.optArray("data.series")
        )
        val out = mutableListOf<IpcaPoint>()
        if (points != null) {
            var accumulated = 0.0
            for (i in 0 until points.length()) {
                val p = points.optJSONObject(i) ?: continue
                val monthly = firstNumber(p.optAny("monthlyPercent"), p.optAny("value"), p.optAny("ipca"), p.optAny("rate"))
                val explicitAccumulated = firstNumber(p.optAny("accumulatedPercent"), p.optAny("accumulated"), p.optAny("cumulative"))
                // IPCA acumulado é composto, não uma soma linear simples.
                // Mantém o valor explícito do Proxy quando disponível.
                accumulated = if (explicitAccumulated != 0.0) {
                    explicitAccumulated
                } else {
                    (((1.0 + accumulated / 100.0) * (1.0 + monthly / 100.0)) - 1.0) * 100.0
                }
                val ts = parseFlexibleDateMillis(firstText(p.optAny("date"), p.optAny("month"), p.optAny("time")))
                out.add(
                    IpcaPoint(
                        timestamp = if (ts > 0L) ts / 1000L else i.toLong(),
                        dateLabel = if (ts > 0L) SimpleDateFormat("MM/yy", Locale.getDefault()).format(Date(ts)) else firstText(p.optAny("label"), "M${i + 1}"),
                        accumulatedPercent = accumulated,
                        monthlyPercent = monthly,
                        source = "Serviço de dados VALORAE"
                    )
                )
            }
        } else {
            val annual = firstNumber(root.optAny("annual"), root.optAny("annualPercent"), root.optAny("ipca12m"), root.optAny("value"))
            if (annual > 0.0) {
                val cal = Calendar.getInstance()
                for (i in safeMonths - 1 downTo 0) {
                    val c = cal.clone() as Calendar
                    c.add(Calendar.MONTH, -i)
                    val accumulated = annual * ((safeMonths - i).toDouble() / safeMonths.toDouble())
                    out.add(IpcaPoint(c.timeInMillis / 1000L, SimpleDateFormat("MM/yy", Locale.getDefault()).format(c.time), accumulated, annual / 12.0))
                }
            }
        }
        if (out.isNotEmpty()) putInCache(cacheKey, out, 60)
        return out
    }

    fun fetchNextDividends(positions: List<PortfolioProxyPosition>): List<DividendEvent> {
        if (positions.isEmpty()) return emptyList()
        val cacheKey = "next_dividends_${positionsCacheSignature(positions)}"
        getFromCache<List<DividendEvent>>(cacheKey)?.let { return it }
        val tickers = positions.map { it.ticker.trim().uppercase(Locale.ROOT) }.filter { it.isNotBlank() }.distinct()
        val payload = JSONObject()
            .put("positions", positionArray(positions))
            .put("tickers", JSONArray(tickers))
            .put("mode", "complete")
            .put("complete", true)
            .put("includeHistory", true)
            .put("includeUpcoming", true)
            .put("limit", 500)
        val roots = mutableListOf<JSONObject>()
        val firstPortfolioMillis = positions.mapNotNull { it.firstPurchaseAt.takeIf { ts -> ts > 0L } }.minOrNull() ?: 0L
        val historyMonths = if (firstPortfolioMillis > 0L) {
            val start = Calendar.getInstance().apply { timeInMillis = firstPortfolioMillis }
            val now = Calendar.getInstance()
            ((now.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 + (now.get(Calendar.MONTH) - start.get(Calendar.MONTH)) + 2).coerceIn(12, 72)
        } else 36
        val startDate = if (firstPortfolioMillis > 0L) SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(firstPortfolioMillis)) else ""
        val params = mapOf(
            "tickers" to tickers.joinToString(","),
            "limit" to "500",
            "mode" to "complete",
            "complete" to "1",
            "includeUpcoming" to "1",
            "includeHistory" to "1",
            "futureMonths" to "24",
            "historyMonths" to historyMonths.toString(),
            "monthsForward" to "24",
            "monthsBack" to historyMonths.toString(),
            "startDate" to startDate,
            "agendaConcurrency" to "4"
        )
        payload
            .put("futureMonths", 24)
            .put("historyMonths", historyMonths)
            .put("monthsForward", 24)
            .put("monthsBack", historyMonths)
            .put("startDate", startDate)
            .put("agendaConcurrency", 4)
        // Chamada principal consolidada: a versão nova do Proxy já devolve agenda futura e histórico
        // no mesmo contrato. Evita até quatro varreduras mensais completas no Investidor10.
        val primaryJson = postProxyJson("/api/v1/portfolio/dividends", payload)
            ?: getProxyJson("/api/v1/portfolio/dividends", params)
        listOfNotNull(primaryJson)
            .flatMap { dividendPayloadCandidates(it) }
            .forEach { candidate -> if (roots.none { it.toString() == candidate.toString() }) roots.add(candidate) }

        // Fallback leve para compatibilidade com proxies antigos ou respostas vazias.
        if (roots.isEmpty()) {
            val fallbackJson = postProxyJson("/api/v1/portfolio/next-dividends", payload)
                ?: getProxyJson("/api/v1/portfolio/next-dividends", params)
            listOfNotNull(fallbackJson)
                .flatMap { dividendPayloadCandidates(it) }
                .forEach { candidate -> if (roots.none { it.toString() == candidate.toString() }) roots.add(candidate) }
        }

        val quantityByTicker = positions.associateBy({ it.ticker.trim().uppercase(Locale.ROOT) }, { it.quantity })
        val out = mutableListOf<DividendEvent>()

        fun appendEvent(raw: JSONObject?, container: JSONObject?, defaultTicker: String = "", defaultStatus: String = "") {
            val item = raw ?: return
            val ticker = firstText(
                item.optAny("ticker"), item.optAny("symbol"), item.optAny("codigo"), item.optAny("code"), item.optAny("asset"), item.optAny("ativo"),
                container?.optAny("ticker"), container?.optAny("symbol"), defaultTicker
            ).uppercase(Locale.ROOT)
            if (ticker.isBlank()) return
            val q = firstNumber(
                item.optAny("quantity"), item.optAny("quantidade"), item.optAny("shares"), item.optAny("cotas"),
                container?.optAny("quantity"), container?.optAny("quantidade"), quantityByTicker[ticker]
            )
            val value = firstNumber(
                item.optAny("valuePerShare"), item.optAny("valorPorCota"), item.optAny("valorPorAcao"),
                item.optAny("valor"), item.optAny("value"), item.optAny("amount"), item.optAny("valorProvento"), item.optAny("dividend"),
                item.optAny("valorFormatado"), item.optAny("valueFormatted"), item.optAny("valorPorCotaFormatado"), item.optAny("valorPorAcaoFormatado"),
                item.optAny("rendimento"), item.optAny("provento"), item.optAny("cashAmount")
            )
            val estimated = firstNumber(
                item.optAny("estimatedAmount"), item.optAny("valorEstimado"), item.optAny("total"), item.optAny("totalAmount"),
                item.optAny("grossAmount"), item.optAny("amountTotal"), if (q > 0.0 && value > 0.0) q * value else 0.0
            )
            val dateCom = normalizeDisplayDate(firstText(item.optAny("dateCom"), item.optAny("comDate"), item.optAny("dataCom"), item.optAny("data_com"), item.optAny("exDate"), item.optAny("recordDate"), item.optAny("dataBase"), item.optAny("baseDate")))
            val paymentDate = normalizeDisplayDate(firstText(
                item.optAny("paymentDate"), item.optAny("payDate"), item.optAny("dataPagamento"), item.optAny("data_pagamento"),
                item.optAny("dataPagamentoPrevista"), item.optAny("dataPagto"), item.optAny("pagamento"), item.optAny("pgto"), item.optAny("date"), item.optAny("data")
            ))
            if (dateCom.isBlank() && paymentDate.isBlank() && value <= 0.0 && estimated <= 0.0) return
            out.add(
                DividendEvent(
                    ticker = ticker,
                    dateCom = dateCom,
                    paymentDate = paymentDate,
                    valuePerShare = value,
                    quantity = q,
                    estimatedAmount = estimated,
                    status = firstText(item.optAny("status"), item.optAny("type"), item.optAny("tipo"), item.optAny("eventType"), item.optAny("tipoEvento"), item.optAny("kind"), item.optAny("proventoTipo"), defaultStatus, "Provento"),
                    source = firstText(item.optAny("source"), item.optAny("fonte"), item.optAny("sourceUrl"), item.optAny("url"), container?.optAny("source"), "Serviço de dados VALORAE")
                )
            )
        }

        fun appendArray(arr: JSONArray?, container: JSONObject?, defaultTicker: String = "", defaultStatus: String = "") {
            if (arr == null) return
            for (i in 0 until arr.length()) {
                val pt = arr.optJSONObject(i) ?: continue
                val t = firstText(pt.optAny("ticker"), pt.optAny("symbol"), pt.optAny("codigo"), defaultTicker)
                appendDividendAliasesFromRoot(t, pt, out, defaultStatus)
                appendEvent(pt, container, t, defaultStatus)
            }
        }

        roots.forEach { root ->
            appendDividendAliasesFromRoot("", root, out)
            appendArray(root.optJSONArray("events"), root)
            appendArray(root.optJSONArray("items"), root)
            appendArray(root.optJSONArray("agenda"), root, defaultStatus = "Previsto")
            appendArray(root.optJSONArray("agendaEvents"), root, defaultStatus = "Previsto")
            appendArray(root.optJSONArray("dividends"), root)
            appendArray(root.optJSONArray("dividendos"), root)
            appendArray(root.optJSONArray("proventos"), root)
            appendArray(root.optJSONArray("historico"), root, defaultStatus = "Recebido")
            appendArray(root.optJSONArray("history"), root, defaultStatus = "Recebido")
            appendArray(root.optJSONArray("upcomingEvents"), root, defaultStatus = "Previsto")
            appendArray(root.optJSONArray("historyEvents"), root, defaultStatus = "Recebido")
            appendArray(root.optArray("data.events"), root)
            appendArray(root.optArray("data.items"), root)
            appendArray(root.optArray("data.agenda"), root, defaultStatus = "Previsto")
            appendArray(root.optArray("data.agendaEvents"), root, defaultStatus = "Previsto")
            appendArray(root.optArray("data.upcomingEvents"), root, defaultStatus = "Previsto")
            appendArray(root.optArray("data.historyEvents"), root, defaultStatus = "Recebido")
            appendArray(root.optArray("data.dividends"), root)
            appendArray(root.optArray("data.dividendos"), root)
            appendArray(root.optArray("data.proventos"), root)
            appendArray(root.optArray("data.historico"), root, defaultStatus = "Recebido")
            appendArray(root.optArray("data.history"), root, defaultStatus = "Recebido")
            appendArray(root.optArray("payload.events"), root)
            appendArray(root.optArray("payload.items"), root)
            appendArray(root.optArray("payload.agendaEvents"), root, defaultStatus = "Previsto")
            appendArray(root.optArray("payload.upcomingEvents"), root, defaultStatus = "Previsto")
            appendArray(root.optArray("payload.historyEvents"), root, defaultStatus = "Recebido")
            appendArray(root.optArray("result.events"), root)
            appendArray(root.optArray("result.items"), root)
            appendArray(root.optArray("result.agendaEvents"), root, defaultStatus = "Previsto")
            appendArray(root.optArray("result.upcomingEvents"), root, defaultStatus = "Previsto")
            appendArray(root.optArray("result.historyEvents"), root, defaultStatus = "Recebido")

            val items = firstArray(
                root.optJSONArray("items"), root.optJSONArray("assets"), root.optJSONArray("rows"), root.optJSONArray("result"), root.optArray("data.items")
            )
            if (items != null) {
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val ticker = firstText(item.optAny("ticker"), item.optAny("symbol"), item.optAny("codigo")).uppercase(Locale.ROOT)
                    appendEvent(item.optJSONObject("nextDividend"), item, ticker, "Previsto")
                    appendEvent(item.optJSONObject("upcoming"), item, ticker, "Previsto")
                    appendEvent(item.optJSONObject("lastDividend"), item, ticker, "Recebido")
                    appendEvent(item.optJSONObject("ultimo"), item, ticker, "Recebido")
                    appendArray(item.optJSONArray("events"), item, ticker)
                    appendArray(item.optJSONArray("items"), item, ticker)
                    appendArray(item.optJSONArray("agenda"), item, ticker, "Previsto")
                    appendArray(item.optJSONArray("agendaEvents"), item, ticker, "Previsto")
                    appendArray(item.optJSONArray("dividends"), item, ticker)
                    appendArray(item.optJSONArray("dividendos"), item, ticker)
                    appendArray(item.optJSONArray("proventos"), item, ticker)
                    appendArray(item.optJSONArray("historico"), item, ticker, "Recebido")
                    appendArray(item.optJSONArray("history"), item, ticker, "Recebido")
                    appendArray(item.optJSONArray("upcomingEvents"), item, ticker, "Previsto")
                    appendArray(item.optJSONArray("historyEvents"), item, ticker, "Recebido")
                    if (item.optJSONObject("nextDividend") == null && item.optJSONObject("lastDividend") == null && item.optJSONObject("ultimo") == null && item.optJSONArray("historico") == null) {
                        appendEvent(item, root, ticker)
                    }
                }
            }
        }

        val parsedTickers = out.map { it.ticker.trim().uppercase(Locale.ROOT) }.filter { it.isNotBlank() }.toSet()
        val needsAssetFallback = out.isEmpty() || tickers.any { it !in parsedTickers }
        if (needsAssetFallback) {
            tickers.filter { it !in parsedTickers || out.none { ev -> ev.ticker.equals(it, ignoreCase = true) && (ev.paymentDate.isNotBlank() || ev.dateCom.isNotBlank()) } }
                .forEach { ticker -> out.addAll(fetchAssetDividendEvents(ticker)) }
        }

        val sorted = out
            .filter { it.ticker.isNotBlank() && (it.valuePerShare > 0.0 || it.estimatedAmount > 0.0 || it.dateCom.isNotBlank() || it.paymentDate.isNotBlank()) }
            .distinctBy { listOf(it.ticker, it.dateCom, it.paymentDate, String.format(Locale.ROOT, "%.6f", it.valuePerShare), it.status).joinToString("|") }
            .sortedWith(compareBy<DividendEvent> {
                parseFlexibleDateMillis(it.paymentDate.ifBlank { it.dateCom }).let { ts -> if (ts > 0L) ts else Long.MAX_VALUE }
            }.thenBy { it.ticker })
        if (sorted.isNotEmpty()) putInCache(cacheKey, sorted, 10)
        return sorted
    }


    fun fetchAssetProxyCapabilities(ticker: String, isFiiHint: Boolean = false, bypassCache: Boolean = false): AssetProxyCapabilities? {
        val clean = ticker.trim().uppercase(Locale.ROOT)
        if (clean.isBlank()) return null
        val isFii = isFiiHint || inferIsFii(clean)
        val cacheKey = "asset_proxy_capabilities_${clean}_${if (isFii) "FII" else "ACAO"}"
        if (!bypassCache) getFromCache<AssetProxyCapabilities>(cacheKey)?.let { return it }
        val baseParams = mapOf(
            "ticker" to clean,
            "view" to "app",
            "profile" to "turbo",
            "timeoutMs" to "1200"
        )
        val qualityEndpoints = listOf(
            Triple("Qualidade Valorae", "/api/v1/asset/quality", "Score, confiabilidade e indicação de renderização segura."),
            Triple("Cobertura dos Dados", "/api/v1/asset/coverage", "Campos disponíveis, ausentes e completude por bloco."),
            Triple("Plano de Ação", "/api/v1/asset/action-plan", "Orientações do VALORAE para análise e próximos passos."),
            Triple("Mapa de Fontes", "/api/v1/asset/source-map", "Fonte usada por campo: Investidor10, StatusInvest, Yahoo, cache ou fallback.")
        )
        val advancedEndpoints = listOf(
            Triple("Perfil do Ativo", "/api/v1/asset/profile", "Dados cadastrais, setor, segmento e descrição operacional."),
            Triple("Fundamentos", "/api/v1/asset/fundamentals", "Indicadores fundamentalistas consolidados."),
            Triple("Valuation", "/api/v1/asset/valuation", "Múltiplos de preço, valor e atratividade."),
            Triple("Rentabilidade", "/api/v1/asset/profitability", "ROE, ROIC, margens e eficiência."),
            Triple("Endividamento", "/api/v1/asset/debt", "Dívida, liquidez e alavancagem."),
            Triple("Demonstrativos", "/api/v1/asset/statements", "Receita, lucro, balanço e evolução contábil."),
            Triple("Pares", "/api/v1/asset/peers", "Comparação com ativos semelhantes."),
            Triple("Indicadores", "/api/v1/asset/indicators", "Painel bruto de indicadores do serviço de dados."),
            Triple("Próximo Dividendo", "/api/v1/asset/next-dividend", "Evento de provento previsto ou último detectado.")
        )
        val fiiEndpoints = if (isFii) listOf(
            Triple("Perfil do FII", "/api/v1/fii/profile", "Tipo de fundo, mandato, gestão e público-alvo."),
            Triple("Renda do FII", "/api/v1/fii/income", "Rendimentos, estabilidade e cobertura de renda."),
            Triple("Patrimonial", "/api/v1/fii/patrimonial", "Patrimônio, valor patrimonial e relação P/VP."),
            Triple("Carteira do FII", "/api/v1/fii/portfolio", "Composição, imóveis, CRIs ou ativos do fundo."),
            Triple("Vacância", "/api/v1/fii/vacancy", "Vacância física/financeira quando disponível."),
            Triple("Comunicados", "/api/v1/fii/communications", "Comunicados e fatos relevantes do fundo."),
            Triple("Checklist FII", "/api/v1/fii/checklist", "Itens de qualidade específicos de fundos imobiliários."),
            Triple("Indicadores FII", "/api/v1/fii/indicators", "Métricas específicas para FIIs.")
        ) else emptyList()

        val quality = qualityEndpoints.mapNotNull { (title, endpoint, subtitle) -> getCapabilitySection(title, endpoint, baseParams, subtitle, bypassCache) }
        val advanced = advancedEndpoints.take(PROXY_PLUS_ASSET_ADVANCED_LIMIT)
            .mapNotNull { (title, endpoint, subtitle) -> getCapabilitySection(title, endpoint, baseParams, subtitle, bypassCache) }
        val fii = fiiEndpoints.take(PROXY_PLUS_FII_LIMIT)
            .mapNotNull { (title, endpoint, subtitle) -> getCapabilitySection(title, endpoint, baseParams, subtitle, bypassCache) }
        val result = AssetProxyCapabilities(clean, isFii, quality, advanced, fii)
        if (quality.isNotEmpty() || advanced.isNotEmpty() || fii.isNotEmpty()) putInCache(cacheKey, result, 6 * 60)
        return result
    }

    fun fetchPortfolioProxyCapabilities(positions: List<PortfolioProxyPosition>, watchlistTickers: List<String> = emptyList(), bypassCache: Boolean = false): PortfolioProxyCapabilities? {
        val cleanPositions = positions.filter { it.ticker.isNotBlank() && it.quantity > 0.0 }
        val tickers = (cleanPositions.map { it.ticker } + watchlistTickers)
            .map { it.trim().uppercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(20)
        val cacheKey = "portfolio_proxy_capabilities_${positionsCacheSignature(cleanPositions)}_${tickers.joinToString("_")}"
        if (!bypassCache) getFromCache<PortfolioProxyCapabilities>(cacheKey)?.let { return it }
        val payload = JSONObject()
            .put("positions", positionArray(cleanPositions))
            .put("tickers", JSONArray(tickers))
            .put("view", "app")
            .put("profile", "portfolio")
            .put("timeoutMs", 1800)
            .put("limit", 50)
        val fallbackParams = mapOf(
            "tickers" to tickers.joinToString(","),
            "view" to "app",
            "profile" to "portfolio",
            "timeoutMs" to "1800"
        )
        val portfolioEndpoints = listOf(
            Triple("Rebalanceamento", "/api/v1/portfolio/rebalance", "Comprar, reduzir, manter, pesos atuais e alvos."),
            Triple("Alocação", "/api/v1/portfolio/allocation", "Distribuição por classe, setor, ticker e concentração."),
            Triple("Risco", "/api/v1/portfolio/risk", "Concentração, diversificação e alertas."),
            Triple("Renda", "/api/v1/portfolio/income", "Estimativa de renda mensal/anual e estabilidade."),
            Triple("Dividendos", "/api/v1/portfolio/dividends", "Proventos agregados pelo serviço de dados."),
            Triple("Eventos", "/api/v1/portfolio/events", "Agenda e próximos eventos relevantes."),
            Triple("Resumo", "/api/v1/portfolio/summary", "Visão consolidada do serviço de dados."),
            Triple("Transações", "/api/v1/portfolio/transactions", "Validação e leitura canônica das posições.")
        )
        val portfolioSections = if (cleanPositions.isNotEmpty()) {
            portfolioEndpoints.take(PROXY_PLUS_PORTFOLIO_LIMIT)
                .mapNotNull { (title, endpoint, subtitle) -> postCapabilitySection(title, endpoint, payload, fallbackParams, subtitle, bypassCache) }
        } else emptyList()

        val radarSections = if (tickers.isNotEmpty()) {
            listOfNotNull(postCapabilitySection("Radar / Watchlist", "/api/v1/watchlist/analyze", payload, fallbackParams, "Oportunidades acompanhadas fora ou dentro da carteira.", bypassCache))
        } else emptyList()

        val diagnosticsEndpoints = listOf(
            Triple("Maturidade do Motor", "/api/v1/engine/maturity", "Aderência, cobertura e prontidão do motor."),
            Triple("Performance do Motor", "/api/v1/engine/performance", "Tempo, gargalos e estabilidade."),
            Triple("Cache de dados", "/api/v1/cache/stats", "Entradas, hits e saúde do cache."),
            Triple("Status de Deploy", "/api/v1/deploy/status", "Status da publicação e ambiente."),
            Triple("Saúde Geral", "/api/v1/health", "Health check técnico ampliado."),
            Triple("Readiness Pessoal", "/api/v1/personal/readiness", "Prontidão de recursos personalizados."),
            Triple("Schema", "/api/v1/schema", "Contrato atual de dados.")
        )
        val diagnostics = diagnosticsEndpoints.take(PROXY_PLUS_DIAGNOSTIC_LIMIT)
            .mapNotNull { (title, endpoint, subtitle) -> getCapabilitySection(title, endpoint, emptyMap(), subtitle, bypassCache) }
        val result = PortfolioProxyCapabilities(portfolioSections, radarSections, diagnostics)
        if (portfolioSections.isNotEmpty() || radarSections.isNotEmpty() || diagnostics.isNotEmpty()) putInCache(cacheKey, result, 60)
        return result
    }

    fun checkHealth(): JSONObject? = checkReady()

    fun checkReady(): JSONObject? {
        val json = getProxyJson("/api/v1/ready")
        lastReadyAt = System.currentTimeMillis()
        lastReadyState = when {
            json?.optBoolean("ready", false) == true -> "Online"
            json?.optString("status", "").equals("READY", true) -> "Online"
            json != null -> "Parcial"
            else -> if (bestAssetSnapshots.isNotEmpty()) "Usando cache local" else "Offline"
        }
        return json
    }

    fun fetchReleaseReadiness(): JSONObject? = getProxyJson("/api/v1/release/readiness")

    fun fetchSourceStatus(): JSONObject? = getProxyJson("/api/v1/source/status")

    fun fetchServerMetrics(): JSONObject? = getProxyJson("/api/server/metrics")

    fun fetchIntegrationManifest(): JSONObject? = getProxyJson("/api/v1/integration/manifest")

    fun fetchObservability(minutes: Int = 60): JSONObject? {
        return getProxyJson("/api/observability", mapOf("minutes" to minutes.toString()))
    }

    fun fetchFields(): JSONObject? {
        return getProxyJson("/api/fields")
    }

    fun fetchOpenApi(): JSONObject? {
        return getProxyJson("/api/openapi")
    }

    fun fetchProxyDiagnosticsSummary(): ProxyDiagnosticsSummary {
        getFromCache<ProxyDiagnosticsSummary>("proxy_diagnostics_summary")?.let { cached ->
            return cached.copy(
                cacheEntries = memoryCache.size,
                bestSnapshotEntries = bestAssetSnapshots.size,
                updatedAssets = lastUpdatedTickers.size,
                partialResponses = partialResponseTickers.size
            )
        }
        val readyJson = checkReady()
        val sourceJson = fetchSourceStatus()
        val metricsJson = fetchServerMetrics()
        val ready = readyJson?.optBoolean("ready", false) == true || readyJson?.optString("status", "").equals("READY", true)
        val sourceState = firstText(
            sourceJson?.optString("status"),
            sourceJson?.optObject("summary")?.optAny("status"),
            sourceJson?.optObject("sources")?.optAny("status"),
            if (sourceJson != null) "consultado" else "indisponível"
        )
        val metricsState = firstText(
            metricsJson?.optString("status"),
            metricsJson?.optObject("metrics")?.optAny("status"),
            if (metricsJson != null) "consultado" else "indisponível"
        )
        val averageMs = synchronized(responseTimeSamples) {
            if (responseTimeSamples.isEmpty()) 0L else responseTimeSamples.average().toLong()
        }
        val errors = synchronized(recentProxyErrors) { recentProxyErrors.toList().takeLast(6) }
        val now = System.currentTimeMillis()
        val summary = ProxyDiagnosticsSummary(
            baseUrl = configuredProxyBaseUrl(),
            state = when {
                ready -> "Online"
                readyJson != null -> "Parcial"
                bestAssetSnapshots.isNotEmpty() -> "Usando cache local"
                else -> "Offline"
            },
            ready = ready,
            usingLocalCache = !ready && bestAssetSnapshots.isNotEmpty(),
            lastCheckedAt = now,
            sourceStatus = sourceState,
            cacheEntries = memoryCache.size,
            bestSnapshotEntries = bestAssetSnapshots.size,
            updatedAssets = lastUpdatedTickers.size,
            partialResponses = partialResponseTickers.size,
            recentErrors = errors,
            averageResponseMs = averageMs,
            metricsStatus = metricsState
        )
        putInCache("proxy_diagnostics_summary", summary, 2)
        return summary
    }

    fun fetchAssetChartBundle(ticker: String, range: String = "1Y", bypassCache: Boolean = false): AssetChartBundle {
        val clean = ticker.trim().uppercase(Locale.ROOT)
        val normalizedRange = normalizeProxyRange(range)
        val cacheKey = "asset_chart_bundle_${clean}_$normalizedRange"
        if (!bypassCache) {
            getFromCache<AssetChartBundle>(cacheKey)?.let { return it }
        }

        val priceHistory = fetchHistoricalChart(clean, normalizedRange)

        var json = getProxyJson(
            "/api/v1/asset",
            mapOf(
                "ticker" to clean,
                "view" to "app",
                "profile" to "max",
                "mode" to "complete",
                "complete" to "1",
                "strict" to "1",
                "charts" to "full",
                "includeCharts" to "1",
                "chartSource" to "investidor10",
                "internalApis" to "1",
                "adaptiveCompletion" to "1",
                "statusInvestComplement" to "1",
                "timeoutMs" to "25000",
                "includeNews" to "0",
                "nocache" to if (bypassCache) "1" else null
            )
        )
        if (json == null) {
            json = getProxyJson(
                "/api/v1/asset",
                mapOf(
                    "ticker" to clean,
                    "view" to "app",
                    "profile" to "turbo",
                    "mode" to "complete",
                    "complete" to "1",
                    "charts" to "full",
                    "includeCharts" to "1",
                    "chartSource" to "investidor10",
                    "internalApis" to "1",
                    "timeoutMs" to "18000",
                    "includeNews" to "0",
                    "nocache" to if (bypassCache) "1" else null
                )
            )
        }
        if (json == null) {
            json = getProxyJson(
                "/api/v1/asset",
                mapOf(
                    "ticker" to clean,
                    "view" to "app",
                    "profile" to "fast",
                    "charts" to "basic",
                    "includeCharts" to "1",
                    "timeoutMs" to "5000",
                    "nocache" to if (bypassCache) "1" else null
                )
            )
        }

        val root = unwrapValoraePayload(json) ?: JSONObject()
        val mappedAsset = mapProxyAsset(root)
        val isFii = mappedAsset?.isFii ?: inferIsFii(clean)

        var bundle = parseAssetChartBundle(clean, isFii, root, priceHistory).copy(range = normalizedRange)
        if (bundle.dividendEvents.isEmpty()) {
            val dividendFallback = fetchAssetDividendEvents(clean)
            if (dividendFallback.isNotEmpty()) {
                val currentPrice = mappedAsset?.price?.takeIf { it > 0.0 }
                    ?: firstNumber(root.optObject("results.cotacao")?.optAny("precoAtual"), root.optObject("results")?.optAny("precoAtual"))
                val (yearly, dyHistory) = buildDividendYearly(dividendFallback, currentPrice)
                val monthly = buildDividendMonthly(dividendFallback)
                bundle = bundle.copy(
                    dividendEvents = dividendFallback,
                    dividendYearly = yearly.ifEmpty { bundle.dividendYearly },
                    dividendMonthly = monthly.ifEmpty { bundle.dividendMonthly },
                    dividendYieldHistory = dyHistory.ifEmpty { bundle.dividendYieldHistory }
                )
            }
        }
        // Gráficos de rentabilidade e comparação devem vir do Investidor10 via Proxy.
        // Não fabricamos mais série nominal a partir do histórico de preço nem misturamos /compare,
        // pois isso gerava divergência visual em relação aos gráficos da página do ativo no Investidor10.
        putInCache(cacheKey, bundle, rangeCacheTtlMinutes(normalizedRange))
        return bundle
    }

    internal fun parseAssetChartBundle(ticker: String, isFii: Boolean, root: JSONObject, priceHistory: List<ChartPoint>): AssetChartBundle {
        val directResults = root.optJSONObject("results")
        val legacyAppCompat = firstObject(root.optJSONObject("legacyAppCompat"), directResults?.optJSONObject("legacyAppCompat"), root.optObject("data.legacyAppCompat"))
        val results = directResults ?: legacyAppCompat?.optJSONObject("results") ?: root
        val appPayload = firstObject(root.optJSONObject("appPayload"), results.optJSONObject("appPayload"), legacyAppCompat?.optJSONObject("appPayload"), root.optObject("data.appPayload"), results.optObject("data.appPayload"))
        val appSnapshot = firstObject(root.optJSONObject("appMobileSnapshot"), results.optJSONObject("appMobileSnapshot"), legacyAppCompat?.optJSONObject("appMobileSnapshot"), root.optObject("data.appMobileSnapshot"), results.optObject("data.appMobileSnapshot"))
        val chartContractFields = contractFieldsObject(firstObject(root.optJSONObject("assetClassContract"), results.optJSONObject("assetClassContract"), legacyAppCompat?.optJSONObject("assetClassContract"), root.optObject("data.assetClassContract")))
        val chartCoverageFields = coverageFieldsObject(firstObject(root.optJSONObject("assetIndicatorCoverage"), results.optJSONObject("assetIndicatorCoverage"), legacyAppCompat?.optJSONObject("assetIndicatorCoverage"), root.optObject("data.assetIndicatorCoverage")))
        val sections = firstObject(results.optJSONObject("sections"), root.optJSONObject("sections")) ?: results
        val normalized = mergedObject(
            root.optJSONObject("normalized"),
            results.optJSONObject("normalized"),
            legacyAppCompat?.optJSONObject("normalized"),
            root.optObject("data.normalized"),
            results.optObject("data.normalized"),
            appPayload?.optObject("metrics.canonical"),
            appSnapshot?.optJSONObject("metrics"),
            chartCoverageFields,
            chartContractFields
        )
        val financialCharts = firstObject(
            results.optJSONObject("chartsFinanceiros"),
            results.optJSONObject("financialCharts"),
            results.optJSONObject("demonstrativos"),
            sections.optJSONObject("chartsFinanceiros"),
            sections.optJSONObject("demonstrativos"),
            appPayload?.optObject("charts.financeiros"),
            appPayload?.optObject("charts.financial"),
            appPayload?.optObject("charts.dre"),
            appSnapshot?.optObject("charts.financeiros"),
            appSnapshot?.optObject("charts.financial"),
            root.optObject("appPayload.charts.financeiros"),
            root.optObject("appPayload.charts.financial"),
            root.optObject("appMobileSnapshot.charts.financeiros"),
            root.optObject("appMobileSnapshot.charts.financial")
        )
        val canonicalCharts = firstObject(
            results.optJSONObject("assetChartsCanonical"),
            sections.optJSONObject("assetChartsCanonical"),
            root.optJSONObject("assetChartsCanonical"),
            root.optObject("data.assetChartsCanonical"),
            appPayload?.optObject("charts.canonical"),
            appSnapshot?.optObject("charts.canonical")
        )
        val canonicalProfitability = canonicalCharts?.optJSONObject("profitability")
        val canonicalFinancial = canonicalCharts?.optJSONObject("financial")
        val canonicalFii = canonicalCharts?.optJSONObject("fii")
        val canonicalCoverage = firstObject(
            canonicalCharts?.optJSONObject("coverage"),
            results.optJSONObject("assetChartsCoverage"),
            sections.optJSONObject("assetChartsCoverage"),
            root.optJSONObject("assetChartsCoverage"),
            root.optObject("data.assetChartsCoverage")
        )

        fun coverageList(vararg arrays: JSONArray?): List<String> {
            val out = linkedSetOf<String>()
            arrays.filterNotNull().forEach { arr ->
                for (i in 0 until arr.length()) {
                    val raw = arr.opt(i)
                    val key = when (raw) {
                        is JSONObject -> firstText(raw.optAny("key"), raw.optAny("name"), raw.optAny("label"), raw.optAny("title"), raw.optAny("id"))
                        else -> firstText(raw)
                    }.trim()
                    if (key.isNotBlank()) out.add(key)
                }
            }
            return out.toList()
        }
        val coverageSummary = canonicalCoverage?.optJSONObject("summary")
        val coverageCaptured = coverageList(
            canonicalCoverage?.optJSONArray("requiredCaptured"),
            canonicalCoverage?.optJSONArray("captured"),
            coverageSummary?.optJSONArray("requiredCaptured"),
            coverageSummary?.optJSONArray("captured")
        )
        val coverageMissing = coverageList(
            canonicalCoverage?.optJSONArray("requiredMissing"),
            canonicalCoverage?.optJSONArray("missing"),
            coverageSummary?.optJSONArray("requiredMissing"),
            coverageSummary?.optJSONArray("missing")
        )
        val coverageNotApplicable = coverageList(
            canonicalCoverage?.optJSONArray("notApplicable"),
            canonicalCoverage?.optJSONArray("not_applicable"),
            coverageSummary?.optJSONArray("notApplicable"),
            coverageSummary?.optJSONArray("not_applicable")
        )

        val warnings = mutableListOf<String>()
        if (root.optString("status") == "PARTIAL" || root.optBoolean("partial", false)) {
            warnings.add("Dados parciais recebidos")
        }
        val warningsArr = results.optJSONArray("warnings") ?: root.optJSONArray("warnings")
        if (warningsArr != null) {
            for (i in 0 until warningsArr.length()) {
                val w = warningsArr.optString(i, "")
                if (w.isNotBlank()) warnings.add(w)
            }
        }
        if (coverageMissing.isNotEmpty()) {
            warnings.add("Alguns gráficos visíveis no Investidor10 ainda não vieram completos do Proxy: ${coverageMissing.take(4).joinToString(", ")}")
        }

        var finalPriceHistory = priceHistory
        if (finalPriceHistory.isEmpty()) {
            val hPoints = mutableListOf<ChartPoint>()
            val chartsObj = firstObject(
                sections.optJSONObject("charts"),
                sections.optJSONObject("rentabilidadeChart"),
                results.optJSONObject("charts"),
                results.optJSONObject("history"),
                results.optJSONObject("chart"),
                results.optJSONObject("prices")
            )
            val points = firstArray(
                chartsObj?.optJSONArray("points"),
                chartsObj?.optJSONArray("series"),
                chartsObj?.optJSONArray("history"),
                chartsObj?.optJSONArray("prices"),
                chartsObj?.optJSONArray("items"),
                sections.optJSONArray("historicalPrice"),
                sections.optJSONArray("historicoPrecos"),
                results.optJSONArray("historicalPrice"),
                results.optJSONArray("historicoPrecos"),
                results.optJSONArray("priceHistory"),
                results.optJSONArray("history"),
                results.optJSONArray("prices"),
                root.optArray("data.points"),
                root.optArray("data.series"),
                root.optArray("data.history"),
                root.optArray("data.prices"),
                root.optArray("data.items")
            )
            if (points != null) {
                for (i in 0 until points.length()) {
                    val p = points.optJSONObject(i) ?: continue
                    val close = firstNumber(p.optAny("close"), p.optAny("adjClose"), p.optAny("value"), p.optAny("valor"), p.optAny("price"), p.optAny("preco"), p.optAny("regularMarketPrice"), p.optAny("y"))
                    if (close <= 0.0) continue
                    val rawDate = firstText(p.optAny("date"), p.optAny("data"), p.optAny("timestamp"), p.optAny("time"), p.optAny("datetime"), p.optAny("x"))
                    val ts = parseFlexibleDateMillis(rawDate)
                    val label = if (ts > 0L) normalizeDisplayDate(rawDate) else firstText(p.optAny("label"), p.optAny("dateLabel"), "P${i + 1}")
                    hPoints.add(ChartPoint(if (ts > 0L) ts / 1000L else i.toLong(), label, close))
                }
            }
            finalPriceHistory = hPoints
        }

        val appChartSeries = firstArray(
            root.optArray("appPayload.charts.series"),
            root.optArray("chartSeries.series"),
            root.optArray("appMobileSnapshot.charts"),
            appPayload?.optArray("charts.series"),
            appSnapshot?.optArray("charts")
        )
        val dreChartSeries = filterChartSeriesByKeywords(appChartSeries, "receita", "revenue", "lucro", "profit", "income", "dre", "resultados")
        val profitQuoteChartSeries = filterChartSeriesByKeywords(appChartSeries, "lucro", "profit", "cotacao", "cotação", "quote", "price", "preco", "preço")
        val equityChartSeries = filterChartSeriesByKeywords(appChartSeries, "balanco", "balanço", "balance", "patrimonio", "patrimônio", "equity", "ativo", "assets", "passivo", "liabilities")
        val payoutChartSeries = filterChartSeriesByKeywords(appChartSeries, "payout")
        val revenueRegionChartSeries = filterChartSeriesByKeywords(appChartSeries, "revenuegeography", "geografia", "regiao", "região", "region", "regional")
        val revenueBusinessChartSeries = filterChartSeriesByKeywords(appChartSeries, "revenuebusiness", "revenuesegment", "negocio", "negócio", "segmento", "business")

        val profitability = mutableListOf<AssetPeriodReturn>()
        val realProfitability = mutableListOf<AssetPeriodReturn>()
        fun appendCanonicalReturns(arr: JSONArray?, fallbackKind: String, target: MutableList<AssetPeriodReturn>) {
            if (arr == null) return
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val period = firstText(item.optAny("period"), item.optAny("label"), item.optAny("periodo"), "P${i + 1}")
                val value = firstNumber(item.optAny("valuePercent"), item.optAny("percent"), item.optAny("percentage"), item.optAny("value"), item.optAny("valor"))
                val kind = firstText(item.optAny("kind"), item.optAny("type"), fallbackKind)
                if (period.isNotBlank() && value.isFinite()) {
                    target.add(AssetPeriodReturn(period = period, valuePercent = value, label = period, kind = kind))
                }
            }
        }
        appendCanonicalReturns(canonicalProfitability?.optJSONArray("nominal"), "nominal", profitability)
        appendCanonicalReturns(canonicalProfitability?.optJSONArray("real"), "real", realProfitability)


        val rentabilidadeObj = firstObject(
            sections.optJSONObject("rentabilidade"),
            sections.optJSONObject("rentabilidadeChart"),
            results.optJSONObject("rentabilidade"),
            results.optJSONObject("rentabilidadeChart"),
            results.optJSONObject("profitability"),
            root.optObject("appPayload.charts.rentabilidade"),
            root.optObject("appPayload.charts.profitability"),
            root.optObject("appPayload.charts.rentabilidadeReal"),
            root.optObject("appMobileSnapshot.charts.rentabilidade"),
            root.optObject("appMobileSnapshot.charts.profitability"),
            root.optObject("appMobileSnapshot.rentabilidade"),
            root.optObject("appMobileSnapshot.profitability"),
            root.optObject("assetClassContract.groups.performance.fields.rentabilidade.value"),
            root.optObject("assetClassContract.groups.performance.fields.profitability.value"),
            root.optObject("assetClassContract.groups.statements.fields.rentabilidade.value")
        )
        val rentKeyValues = firstArray(
            rentabilidadeObj?.optJSONArray("keyValues"),
            rentabilidadeObj?.optJSONArray("profitabilities"),
            rentabilidadeObj?.optJSONArray("items"),
            rentabilidadeObj?.optJSONArray("data"),
            rentabilidadeObj?.optJSONArray("values"),
            rentabilidadeObj?.optJSONArray("series"),
            rentabilidadeObj?.optJSONArray("points")
        )
        if (rentKeyValues != null && rentKeyValues.length() > 0) {
            for (i in 0 until rentKeyValues.length()) {
                val kv = rentKeyValues.optJSONObject(i) ?: continue
                val p = firstText(kv.optAny("period"), kv.optAny("label"), kv.optAny("periodo"))
                val valPct = firstNumber(kv.optAny("valuePercent"), kv.optAny("value"), kv.optAny("valor"), kv.optAny("percent"))
                val type = firstText(kv.optAny("kind"), kv.optAny("type"), "nominal")
                if (p.isNotBlank()) {
                    val apReturn = AssetPeriodReturn(period = p, valuePercent = valPct, label = p, kind = type)
                    if (type.equals("real", ignoreCase = true) || p.lowercase().contains("real")) {
                        realProfitability.add(apReturn)
                    } else {
                        profitability.add(apReturn)
                    }
                }
            }
        }
        if (profitability.isEmpty() || realProfitability.isEmpty()) {
            val profitabilitySeries = filterChartSeriesByKeywords(appChartSeries, "rentabilidade", "return", "performance", "variacao", "variação", "ipca", "real", "nominal")
            val (nominalFromSeries, realFromSeries) = parseProfitabilityReturnsFromChartSeries(profitabilitySeries)
            if (profitability.isEmpty()) profitability.addAll(nominalFromSeries)
            if (realProfitability.isEmpty()) realProfitability.addAll(realFromSeries)
        }

        if (profitability.isEmpty()) {
            val adv = sections.optJSONObject("indicadoresAvancados") ?: results.optJSONObject("indicadoresAvancados") ?: root.optJSONObject("indicadoresAvancados")
            if (adv != null) {
                val mapPeriods = listOf(
                    "30D" to listOf("variation_30_days", "variation_1_month"),
                    "6M" to listOf("variation_6_months"),
                    "12M" to listOf("variation_12_months", "variation_1_year"),
                    "2A" to listOf("variation_24_months", "variation_2_years"),
                    "5A" to listOf("variation_5_years"),
                    "10A" to listOf("variation_10_years")
                )
                for ((period, keys) in mapPeriods) {
                    for (k in keys) {
                        val v = firstNumber(adv.optAny(k))
                        if (v != 0.0) {
                            profitability.add(AssetPeriodReturn(period = period, valuePercent = v, label = period))
                            break
                        }
                    }
                }
            }
            // Sem fallback sintético por variação 12M: o gráfico nominal x real deve refletir
            // a tabela/estrutura de rentabilidade do Investidor10, não um único ponto derivado.
        }

        val indicatorCards = mutableListOf<AssetIndicatorPoint>()
        val adv = mergedObject(
            sections.optJSONObject("indicadoresAvancados"),
            results.optJSONObject("indicadoresAvancados"),
            results.optJSONObject("advancedMetrics"),
            root.optJSONObject("indicadoresAvancados"),
            root.optJSONObject("advancedMetrics")
        ) ?: JSONObject()
        val indicadores = mergedObject(
            results.optJSONObject("indicadores"),
            sections.optJSONObject("indicadores"),
            results.optObject("indicadoresFundamentalistas.semComparativos"),
            results.optObject("indicadoresFundamentalistas.comComparativos"),
            results.optObject("indicadoresFundamentalistas.comparativos"),
            results.optJSONObject("fundamentalistIndicators"),
            root.optJSONObject("indicadores")
        ) ?: JSONObject()
        val financialSummary = mergedObject(
            root.optJSONObject("financialSummary"),
            results.optJSONObject("financialSummary"),
            sections.optJSONObject("financialSummary"),
            root.optObject("data.financialSummary")
        ) ?: JSONObject()
        val ratiosChave = mergedObject(
            financialSummary.optJSONObject("ratiosChave"),
            financialSummary.optJSONObject("keyRatios"),
            financialSummary.optJSONObject("ratios"),
            results.optJSONObject("ratiosChave"),
            results.optJSONObject("keyRatios")
        ) ?: JSONObject()
        val infoFiiForIndicators = mergedObject(
            results.optJSONObject("informacoesFundo"),
            sections.optJSONObject("informacoesFundo"),
            results.optJSONObject("dadosFundo"),
            sections.optJSONObject("dadosFundo"),
            results.optJSONObject("fund"),
            sections.optJSONObject("fund"),
            sections.optJSONObject("fundo"),
            root.optJSONObject("informacoesFundo")
        ) ?: JSONObject()
        val valorPatrimonialIndicadores = mergedObject(
            results.optJSONObject("valorPatrimonial"),
            sections.optJSONObject("valorPatrimonial"),
            root.optJSONObject("valorPatrimonial")
        ) ?: JSONObject()

        val indicatorHistory = mutableMapOf<String, List<AssetIndicatorPoint>>()

        fun displayIndicator(value: Double, unit: String): String = when (unit) {
            "%" -> String.format(Locale.ROOT, "%.2f%%", value)
            "BRL" -> when {
                value >= 1_000_000_000.0 -> String.format(Locale.ROOT, "R$ %.2f bi", value / 1_000_000_000.0)
                value >= 1_000_000.0 -> String.format(Locale.ROOT, "R$ %.2f mi", value / 1_000_000.0)
                else -> String.format(Locale.ROOT, "R$ %.2f", value)
            }
            "number" -> String.format(Locale.ROOT, "%.0f", value)
            else -> String.format(Locale.ROOT, "%.2f", value)
        }

        fun hasExplicitValue(vararg values: Any?): Boolean {
            return values.any { raw ->
                when (raw) {
                    null -> false
                    is JSONObject -> listOf("value", "valor", "display", "raw").any { k -> raw.has(k) && !raw.isNull(k) && firstText(raw.optAny(k)).isNotBlank() }
                    else -> firstText(raw).isNotBlank()
                }
            }
        }

        fun keepZeroIndicator(label: String): Boolean {
            val key = canonicalKey(label)
            return key.contains("vacancia") || key.contains("divida") || key.contains("debt") || key.contains("passivos")
        }

        fun addIndicator(label: String, unit: String = "", vararg values: Any?) {
            if (indicatorCards.any { it.label.equals(label, ignoreCase = true) }) return
            val value = firstNumber(*values)
            val explicit = hasExplicitValue(*values)
            if (value.isFinite() && (value != 0.0 || (explicit && keepZeroIndicator(label)))) {
                indicatorCards.add(
                    AssetIndicatorPoint(
                        label = label,
                        value = value,
                        display = displayIndicator(value, unit),
                        unit = unit
                    )
                )
            }
        }

        fun addIndicatorWithDisplay(label: String, unit: String = "", displayOverride: String = "", vararg values: Any?) {
            if (indicatorCards.any { it.label.equals(label, ignoreCase = true) }) return
            val value = firstNumber(*values)
            val explicit = hasExplicitValue(*values) || displayOverride.isNotBlank()
            if (value.isFinite() && (value != 0.0 || (explicit && keepZeroIndicator(label)))) {
                indicatorCards.add(
                    AssetIndicatorPoint(
                        label = label,
                        value = value,
                        display = displayOverride.ifBlank { displayIndicator(value, unit) },
                        unit = unit
                    )
                )
            }
        }

        fun normalizedOrElse(key: String, vararg values: Any?): Array<Any?> = arrayOf(normalizedValue(normalized, key), *values)

        // Indicadores comuns: usa normalized primeiro e depois todas as estruturas reais que o Proxy pode devolver.
        addIndicatorWithDisplay("Preço Atual", "BRL", normalizedDisplay(normalized, "precoAtual"), *normalizedOrElse("precoAtual", results.optAny("precoAtual"), results.optObject("cotacao")?.optAny("precoAtual")))
        addIndicatorWithDisplay("Variação Dia", "%", normalizedDisplay(normalized, "variacaoDay"), *normalizedOrElse("variacaoDay", results.optAny("variacaoDay"), results.optObject("cotacao")?.optAny("variacaoDay")))
        addIndicatorWithDisplay("Variação 12M", "%", normalizedDisplay(normalized, "variacao12m"), *normalizedOrElse("variacao12m", results.optAny("variacao12m")))
        addIndicator("Dividend Yield", "%", *normalizedOrElse("dividendYield", indicadores.optAny("dividendYield"), ratiosChave.optAny("dividendYield"), results.optAny("dividendYield"), results.optAny("dy"), adv.optAny("dividend_yield_last_12_months"), adv.optAny("dividend_yield")))
        addIndicator("DY Médio 5A", "%", *normalizedOrElse("dyMedio5a", indicadores.optAny("dyMedio5a"), results.optAny("dyMedio5a"), adv.optAny("dividend_yield_last_5_years")))
        addIndicator("P/VP", "", *normalizedOrElse("pvp", indicadores.optAny("pvp"), ratiosChave.optAny("pvp"), results.optAny("pvp"), adv.optAny("p_vp")))
        if (!isFii) addIndicator("P/L", "", *normalizedOrElse("pl", indicadores.optAny("pl"), ratiosChave.optAny("pl"), results.optAny("pl"), adv.optAny("p_l")))
        addIndicator("Payout", "%", *normalizedOrElse("payout", indicadores.optAny("payout"), ratiosChave.optAny("payout"), results.optAny("payout"), adv.optAny("payout")))
        addIndicator("ROE", "%", *normalizedOrElse("roe", indicadores.optAny("roe"), ratiosChave.optAny("roe"), results.optAny("roe"), adv.optAny("roe")))
        addIndicator("ROIC", "%", *normalizedOrElse("roic", indicadores.optAny("roic"), ratiosChave.optAny("roic"), results.optAny("roic"), adv.optAny("roic")))
        addIndicator("ROA", "%", *normalizedOrElse("roa", indicadores.optAny("roa"), ratiosChave.optAny("roa"), results.optAny("roa"), adv.optAny("roa")))
        addIndicator("Margem Líquida", "%", *normalizedOrElse("margemLiquida", indicadores.optAny("margemLiquida"), ratiosChave.optAny("margemLiquida"), results.optAny("margemLiquida"), adv.optAny("net_margin")))
        addIndicator("Margem Bruta", "%", *normalizedOrElse("margemBruta", indicadores.optAny("margemBruta"), results.optAny("margemBruta"), adv.optAny("gross_margin")))
        addIndicator("Margem EBIT", "%", *normalizedOrElse("margemEbit", indicadores.optAny("margemEbit"), results.optAny("margemEbit"), adv.optAny("ebit_margin")))
        addIndicator("Margem EBITDA", "%", *normalizedOrElse("margemEbitda", indicadores.optAny("margemEbitda"), results.optAny("margemEbitda"), adv.optAny("ebitda_margin")))
        addIndicator("EV/EBITDA", "", *normalizedOrElse("evEbitda", indicadores.optAny("evEbitda"), ratiosChave.optAny("evEbitda"), results.optAny("evEbitda"), adv.optAny("ev_ebitda")))
        addIndicator("EV/EBIT", "", *normalizedOrElse("evEbit", indicadores.optAny("evEbit"), ratiosChave.optAny("evEbit"), results.optAny("evEbit"), adv.optAny("ev_ebit")))
        addIndicator("P/EBITDA", "", *normalizedOrElse("pEbitda", indicadores.optAny("pEbitda"), results.optAny("pEbitda"), adv.optAny("p_ebitda")))
        addIndicator("P/EBIT", "", *normalizedOrElse("pEbit", indicadores.optAny("pEbit"), results.optAny("pEbit"), adv.optAny("p_ebit")))
        addIndicator("PSR", "", *normalizedOrElse("psr", indicadores.optAny("psr"), results.optAny("psr"), adv.optAny("psr")))
        addIndicator("LPA", "BRL", indicadores.optAny("lpa"), results.optAny("lpa"), adv.optAny("lpa"))
        addIndicator("VPA", "BRL", *normalizedOrElse("valorPatrimonialCota", indicadores.optAny("vpa"), results.optAny("vpa"), valorPatrimonialIndicadores.optAny("valorPatrimonial"), valorPatrimonialIndicadores.optAny("valorPatrimonialRaw"), infoFiiForIndicators.optAny("valorPatrimonialCota"), adv.optAny("vpa")))
        addIndicator("Valor de Mercado", "BRL", *normalizedOrElse("valorDeMercado", financialSummary.optAny("valorDeMercado"), results.optAny("valorDeMercado"), adv.optAny("market_value")))
        addIndicator("Patrimônio Líquido", "BRL", *normalizedOrElse("patrimonioLiquido", financialSummary.optAny("patrimonioLiquido"), results.optAny("patrimonioLiquido"), valorPatrimonialIndicadores.optAny("patrimonioLiquido"), valorPatrimonialIndicadores.optAny("patrimonioLiquidoRaw"), infoFiiForIndicators.optAny("patrimonioLiquido"), adv.optAny("balance_net_worth")))
        addIndicator("Liquidez Média Diária", "BRL", *normalizedOrElse("liquidezMediaDiaria", results.optAny("liquidezMediaDiaria"), results.optAny("liquidezDiaria"), financialSummary.optAny("liquidezMediaDiaria"), adv.optAny("liquidez_media_diaria")))
        addIndicator("Liquidez Corrente", "", *normalizedOrElse("liquidezCorrente", indicadores.optAny("liquidezCorrente"), results.optAny("liquidezCorrente"), adv.optAny("current_liquidity")))
        addIndicator("Dívida Líquida/EBITDA", "", *normalizedOrElse("dividaLiquidaEbitda", indicadores.optAny("dividaLiquidaEbitda"), results.optAny("dividaLiquidaEbitda"), adv.optAny("net_debt_ebitda")))
        addIndicator("Dívida Líquida/EBIT", "", *normalizedOrElse("dividaLiquidaEbit", indicadores.optAny("dividaLiquidaEbit"), results.optAny("dividaLiquidaEbit"), adv.optAny("net_debt_ebit")))
        addIndicator("Dívida Bruta/Patrimônio", "", *normalizedOrElse("dividaBrutaPatrimonio", indicadores.optAny("dividaBrutaPatrimonio"), results.optAny("dividaBrutaPatrimonio"), adv.optAny("gross_debt_net_worth")))
        addIndicator("Patrimônio/Ativos", "%", *normalizedOrElse("patrimonioAtivos", indicadores.optAny("patrimonioAtivos"), results.optAny("patrimonioAtivos"), adv.optAny("net_worth_assets")))
        addIndicator("Passivos/Ativos", "%", *normalizedOrElse("passivosAtivos", indicadores.optAny("passivosAtivos"), results.optAny("passivosAtivos"), adv.optAny("liabilities_assets")))
        addIndicator("Giro dos Ativos", "", *normalizedOrElse("giroAtivos", indicadores.optAny("giroAtivos"), results.optAny("giroAtivos"), adv.optAny("active_turns")))
        addIndicator("CAGR Receitas 5A", "%", *normalizedOrElse("cagrReceitas5a", indicadores.optAny("cagrReceitas5a"), results.optAny("cagrReceitas5a"), adv.optAny("growth_net_revenue_last_5_years")))
        addIndicator("CAGR Lucros 5A", "%", *normalizedOrElse("cagrLucros5a", indicadores.optAny("cagrLucros5a"), results.optAny("cagrLucros5a"), adv.optAny("growth_net_profit_last_5_years")))

        if (isFii) {
            addIndicator("Yield 1M", "%", *normalizedOrElse("yield1m", results.optAny("yield1m"), indicadores.optAny("yield1m")))
            addIndicator("Yield 3M", "%", *normalizedOrElse("yield3m", results.optAny("yield3m"), indicadores.optAny("yield3m")))
            addIndicator("Yield 6M", "%", *normalizedOrElse("yield6m", results.optAny("yield6m"), indicadores.optAny("yield6m")))
            addIndicator("Yield 12M", "%", *normalizedOrElse("yield12m", results.optAny("yield12m"), indicadores.optAny("yield12m"), results.optAny("dividendYield")))
            addIndicator("Vacância Física", "%", *normalizedOrElse("vacanciaFisica", results.optAny("vacanciaFisica"), infoFiiForIndicators.optAny("vacanciaFisica"), infoFiiForIndicators.optAny("vacancia")))
            addIndicator("Número de Imóveis", "number", results.optAny("numeroImoveis"), infoFiiForIndicators.optAny("numeroImoveis"), sections.optJSONArray("listaImoveis")?.length(), results.optJSONArray("listaImoveis")?.length())
            addIndicator("Cotistas", "number", results.optAny("numeroCotistas"), results.optAny("cotistas"), infoFiiForIndicators.optAny("numeroCotistas"), infoFiiForIndicators.optAny("cotistas"))
            addIndicator("Cotas Emitidas", "number", results.optAny("cotasEmitidas"), results.optAny("totalPapeis"), infoFiiForIndicators.optAny("cotasEmitidas"), infoFiiForIndicators.optAny("quantidadeCotas"))
        }

        fun indicatorLabelForKey(raw: String): String {
            val key = canonicalKey(raw)
            return when {
                key in setOf("precoatual", "price", "quote", "cotacao") -> "Preço Atual"
                key in setOf("variacaoday", "daychange", "changepercent") -> "Variação Dia"
                key in setOf("variacao12m", "variation12months", "variation1year") -> "Variação 12M"
                key in setOf("pl") -> "P/L"
                key in setOf("pvp") -> "P/VP"
                key.contains("dividend") || key == "dy" || key.contains("yield") -> if (key.contains("1m")) "Yield 1M" else if (key.contains("3m")) "Yield 3M" else if (key.contains("6m")) "Yield 6M" else if (key.contains("12m")) "Yield 12M" else "Dividend Yield"
                key.contains("payout") -> "Payout"
                key.contains("roe") -> "ROE"
                key.contains("roic") -> "ROIC"
                key.contains("roa") -> "ROA"
                key.contains("vacancia") || key.contains("vacancy") -> "Vacância Física"
                key.contains("valorpatrimonialcota") || key.contains("valorpatrimonialporcota") || key == "vpa" -> "VPA"
                key.contains("patrimonioliquido") || key.contains("networth") -> "Patrimônio Líquido"
                key.contains("liquidezmedia") || key.contains("liquidezdiaria") -> "Liquidez Média Diária"
                key.contains("cotistas") || key.contains("holders") -> "Cotistas"
                key.contains("cotasemitidas") || key.contains("issued") -> "Cotas Emitidas"
                key.contains("margemliquida") || key.contains("netmargin") -> "Margem Líquida"
                key.contains("margembruta") || key.contains("grossmargin") -> "Margem Bruta"
                key.contains("ebitda") && key.contains("margin") -> "Margem EBITDA"
                key.contains("ebit") && key.contains("margin") -> "Margem EBIT"
                else -> raw.replace('_', ' ').replace('-', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }

        fun unitForIndicator(label: String, rawKey: String = ""): String {
            val text = (label + " " + rawKey).lowercase(Locale.ROOT)
            return when {
                text.contains("yield") || text.contains("dividend") || text.contains("variação") || text.contains("margem") || text.contains("payout") || text.contains("vacância") || text.contains("roe") || text.contains("roic") || text.contains("roa") || text.contains("cagr") -> "%"
                text.contains("preço") || text.contains("valor") || text.contains("patrimônio") || text.contains("liquidez") || text == "vpa" || text.contains("vpa") -> "BRL"
                text.contains("cotistas") || text.contains("cotas") || text.contains("imóveis") -> "number"
                else -> ""
            }
        }

        fun addGenericIndicator(rawLabel: String, rawValue: Any?, displayCandidate: Any? = null) {
            val label = indicatorLabelForKey(rawLabel)
            if (label.isBlank() || label.startsWith("_") || indicatorCards.any { it.label.equals(label, ignoreCase = true) }) return
            val unit = unitForIndicator(label, rawLabel)
            val display = firstText(displayCandidate)
            addIndicatorWithDisplay(label, unit, display, rawValue)
        }

        fun addIndicatorsFromObject(obj: JSONObject?) {
            if (obj == null || obj.length() == 0) return
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key.startsWith("_") || key.equals("warnings", true) || key.equals("source", true)) continue
                val value = obj.optAny(key) ?: continue
                when (value) {
                    is JSONObject -> addGenericIndicator(key, value.optAny("value") ?: value.optAny("valor") ?: value.optAny("display"), value.optAny("display"))
                    is JSONArray -> Unit
                    else -> addGenericIndicator(key, value, value)
                }
            }
        }

        fun addIndicatorsFromArray(arr: JSONArray?) {
            if (arr == null) return
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val label = firstText(item.optAny("label"), item.optAny("name"), item.optAny("nome"), item.optAny("key"), item.optAny("indicador"), item.optAny("title"))
                if (label.isBlank()) continue
                val value = item.optAny("value") ?: item.optAny("valor") ?: item.optAny("percent") ?: item.optAny("percentage") ?: item.optAny("display")
                addGenericIndicator(label, value, item.optAny("display") ?: value)
            }
        }

        addIndicatorsFromObject(normalized)
        addIndicatorsFromObject(indicadores)
        addIndicatorsFromObject(ratiosChave)
        addIndicatorsFromObject(financialSummary)
        addIndicatorsFromObject(infoFiiForIndicators)
        addIndicatorsFromObject(valorPatrimonialIndicadores)
        addIndicatorsFromObject(adv)
        addIndicatorsFromArray(results.optJSONArray("indicadores"))
        addIndicatorsFromArray(sections.optJSONArray("indicadores"))
        addIndicatorsFromArray(results.optJSONArray("metrics"))
        addIndicatorsFromArray(results.optJSONArray("fundamentalistIndicators"))
        addIndicatorsFromArray(results.optJSONArray("keyValues"))
        addIndicatorsFromArray(sections.optJSONArray("keyValues"))
        addIndicatorsFromArray(results.optArray("indicadoresFundamentalistas.semComparativos"))
        addIndicatorsFromArray(results.optArray("indicadoresFundamentalistas.comComparativos"))
        addIndicatorsFromArray(results.optArray("indicadoresFundamentalistas.comparativos"))
        addIndicatorsFromArray(financialSummary.optJSONArray("keyValues"))
        addIndicatorsFromArray(financialSummary.optJSONArray("items"))

        val dyVal = firstNumber(normalizedValue(normalized, "dividendYield"), indicadores.optAny("dividendYield"), ratiosChave.optAny("dividendYield"), results.optAny("dividendYield"), results.optAny("yield12m"))

        val histInds = sections.optJSONObject("historicoIndicadores") ?: sections.optJSONObject("charts")
        if (histInds != null) {
            val keys = histInds.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = histInds.optJSONArray(k)
                if (arr != null) {
                    val list = mutableListOf<AssetIndicatorPoint>()
                    for (i in 0 until arr.length()) {
                        val pt = arr.optJSONObject(i) ?: continue
                        val year = firstText(pt.optAny("year"), pt.optAny("ano"), pt.optAny("period"), pt.optAny("label"), pt.optAny("date"), "P${i+1}")
                        val value = firstNumber(pt.optAny("value"), pt.optAny("valor"), pt.optAny("percent"), pt.optAny("percentage"), pt.optAny("y"))
                        val disp = pt.optString("display", String.format(Locale.ROOT, "%.2f", value))
                        val label = indicatorLabelForKey(k)
                        if (value.isFinite()) list.add(AssetIndicatorPoint(label = label, value = value, display = disp, year = year))
                    }
                    if (list.isNotEmpty()) {
                        indicatorHistory[indicatorLabelForKey(k)] = list
                    }
                }
            }
        }

        val dividendEvents = mutableListOf<DividendEvent>()
        val dividendSection = firstObject(
            results.optJSONObject("dividendos"),
            results.optJSONObject("dividends"),
            results.optJSONObject("proventos"),
            results.optJSONObject("income"),
            sections.optJSONObject("dividendos"),
            sections.optJSONObject("dividends"),
            sections.optJSONObject("proventos"),
            results.optJSONObject("distribuicoes"),
            sections.optJSONObject("distribuicoes"),
            appPayload?.optJSONObject("dividends"),
            appSnapshot?.optJSONObject("dividends"),
            root.optObject("data.dividendos"),
            root.optObject("data.dividends"),
            root.optObject("data.proventos"),
            root.optObject("data.distribuicoes")
        )
        appendDividendEventsFromArray(
            ticker,
            firstArray(
                results.optJSONArray("historicoDividendos"),
                results.optJSONArray("dividends"),
                results.optJSONArray("historicoProventos"),
                results.optJSONArray("proventos"),
                results.optJSONArray("dividendos"),
                results.optJSONArray("income"),
                results.optJSONArray("earnings"),
                dividendSection?.optJSONArray("historico"),
                dividendSection?.optJSONArray("history"),
                dividendSection?.optJSONArray("items"),
                dividendSection?.optJSONArray("events"),
                dividendSection?.optJSONArray("proventos"),
                dividendSection?.optJSONArray("dividendos"),
                dividendSection?.optJSONArray("distribuicoes"),
                dividendSection?.optJSONArray("distributions"),
                dividendSection?.optJSONArray("ultimos12Meses"),
                dividendSection?.optJSONArray("last12Months"),
                results.optObject("dividendos.historico")?.optJSONArray("items"),
                results.optObject("dividendos")?.optJSONArray("historico"),
                results.optObject("dividends")?.optJSONArray("history"),
                results.optObject("proventos")?.optJSONArray("historico"),
                sections.optJSONArray("historicoDividendos"),
                sections.optJSONArray("proventos"),
                appPayload?.optArray("dividends.history"),
                appSnapshot?.optArray("dividends.recentHistory"),
                root.optArray("data.dividendos.historico"),
                root.optArray("data.dividends.history"),
                root.optArray("data.proventos")
            ),
            dividendEvents
        )

        val dividendYearly = mutableListOf<AssetIndicatorPoint>()
        val dividendMonthly = mutableListOf<AssetIndicatorPoint>()
        val dividendYieldHistory = mutableListOf<AssetIndicatorPoint>()
        val payoutHistory = mutableListOf<AssetIndicatorPoint>()

        if (dividendEvents.isNotEmpty()) {
            val byYear = mutableMapOf<String, Double>()
            val byMonth = mutableMapOf<String, Double>()
            for (ev in dividendEvents) {
                val year = try {
                    ev.paymentDate.ifBlank { ev.dateCom }.substringAfterLast("/", "Previsto")
                } catch (_: Exception) { "Previsto" }
                if (year.length == 4 && year.all { it.isDigit() }) {
                    byYear[year] = (byYear[year] ?: 0.0) + ev.valuePerShare
                }

                val monthYear = try {
                    val parts = ev.paymentDate.ifBlank { ev.dateCom }.split("/")
                    if (parts.size >= 3) {
                        parts[1] + "/" + parts[2]
                    } else "Previsto"
                } catch (_: Exception) { "Previsto" }
                if (monthYear != "Previsto") {
                    byMonth[monthYear] = (byMonth[monthYear] ?: 0.0) + ev.valuePerShare
                }
            }

            byYear.entries.sortedBy { it.key }.forEach { (year, valSum) ->
                dividendYearly.add(AssetIndicatorPoint(label = "Anual", value = valSum, display = String.format(Locale.ROOT, "R$ %.4f", valSum), year = year))
                val currentPrice = firstNumber(normalizedValue(normalized, "precoAtual"), results.optAny("precoAtual"))
                if (currentPrice > 0.0) {
                    val estDy = (valSum / currentPrice) * 100.0
                    dividendYieldHistory.add(AssetIndicatorPoint(label = "DY %", value = estDy, display = String.format(Locale.ROOT, "%.2f%%", estDy), year = year))
                }
            }

            byMonth.entries.sortedWith { o1, o2 ->
                val p1 = o1.key.split("/")
                val p2 = o2.key.split("/")
                if (p1.size == 2 && p2.size == 2) {
                    val cmpYear = p1[1].compareTo(p2[1])
                    if (cmpYear != 0) cmpYear else p1[0].compareTo(p2[0])
                } else o1.key.compareTo(o2.key)
            }.takeLast(24).forEach { (my, valSum) ->
                dividendMonthly.add(AssetIndicatorPoint(label = "Mensal", value = valSum, display = String.format(Locale.ROOT, "R$ %.4f", valSum), period = my))
            }
        }

        fun addPayoutHistoryPoint(year: String, value: Double) {
            if (year.isNotBlank() && value.isFinite() && value != 0.0 && payoutHistory.none { it.year == year }) {
                payoutHistory.add(AssetIndicatorPoint("Payout", value, String.format(Locale.ROOT, "%.2f%%", value), "%", year = year))
            }
        }

        fun appendPayoutHistoryFromAny(source: Any?) {
            when (source) {
                is JSONObject -> {
                    val yearsArr = firstArray(
                        source.optJSONArray("years"), source.optJSONArray("anos"), source.optJSONArray("labels"),
                        source.optJSONArray("categories"), source.optJSONArray("periods"), source.optJSONArray("periodos")
                    )
                    val payoutArr = firstArray(
                        source.optJSONArray("payOutCompanyIndicators"), source.optJSONArray("payout"), source.optJSONArray("payoutHistory"),
                        source.optJSONArray("payoutHistorico"), source.optJSONArray("values"), source.optJSONArray("data"),
                        source.optJSONArray("items"), source.optJSONArray("points"), source.optJSONArray("series")
                    )
                    if (yearsArr != null && payoutArr != null) {
                        val len = kotlin.math.min(yearsArr.length(), payoutArr.length())
                        for (i in 0 until len) {
                            val rawPoint = payoutArr.opt(i)
                            val objPoint = rawPoint as? JSONObject
                            val yr = firstText(objPoint?.optAny("year"), objPoint?.optAny("ano"), objPoint?.optAny("period"), objPoint?.optAny("label"), yearsArr.optString(i))
                            val pay = firstNumber(objPoint?.optAny("value"), objPoint?.optAny("valor"), objPoint?.optAny("payout"), objPoint?.optAny("percent"), objPoint?.optAny("percentage"), rawPoint)
                            addPayoutHistoryPoint(yr, pay)
                        }
                    } else if (payoutArr != null) {
                        for (i in 0 until payoutArr.length()) {
                            val objPoint = payoutArr.optJSONObject(i)
                            if (objPoint != null) {
                                val yr = firstText(objPoint.optAny("year"), objPoint.optAny("ano"), objPoint.optAny("period"), objPoint.optAny("label"), "P${i + 1}")
                                val pay = firstNumber(objPoint.optAny("value"), objPoint.optAny("valor"), objPoint.optAny("payout"), objPoint.optAny("percent"), objPoint.optAny("percentage"))
                                addPayoutHistoryPoint(yr, pay)
                            }
                        }
                    } else {
                        val yr = firstText(source.optAny("year"), source.optAny("ano"), source.optAny("period"), source.optAny("label"))
                        val pay = firstNumber(source.optAny("value"), source.optAny("valor"), source.optAny("payout"), source.optAny("percent"), source.optAny("percentage"))
                        addPayoutHistoryPoint(yr, pay)
                    }
                }
                is JSONArray -> {
                    for (i in 0 until source.length()) {
                        val item = source.opt(i)
                        if (item is JSONObject) {
                            val seriesName = canonicalKey(firstText(item.optAny("key"), item.optAny("name"), item.optAny("label"), item.optAny("title")))
                            val nested = firstArray(item.optJSONArray("points"), item.optJSONArray("data"), item.optJSONArray("values"), item.optJSONArray("items"))
                            if (nested != null && seriesName.contains("payout")) {
                                for (j in 0 until nested.length()) {
                                    val point = nested.opt(j)
                                    val yr = when (point) {
                                        is JSONObject -> firstText(point.optAny("year"), point.optAny("ano"), point.optAny("period"), point.optAny("label"), point.optAny("date"), point.optAny("x"), "P${j + 1}")
                                        is JSONArray -> firstText(point.opt(0), "P${j + 1}")
                                        else -> "P${j + 1}"
                                    }
                                    val pay = when (point) {
                                        is JSONObject -> firstNumber(point.optAny("value"), point.optAny("valor"), point.optAny("payout"), point.optAny("percent"), point.optAny("percentage"), point.optAny("y"))
                                        is JSONArray -> firstNumber(if (point.length() > 1) point.opt(1) else point.opt(0))
                                        else -> firstNumber(point)
                                    }
                                    addPayoutHistoryPoint(yr, pay)
                                }
                            } else {
                                val yr = firstText(item.optAny("year"), item.optAny("ano"), item.optAny("period"), item.optAny("label"), item.optAny("date"), item.optAny("x"), "P${i + 1}")
                                val pay = firstNumber(item.optAny("value"), item.optAny("valor"), item.optAny("payout"), item.optAny("percent"), item.optAny("percentage"), item.optAny("y"))
                                addPayoutHistoryPoint(yr, pay)
                            }
                        }
                    }
                }
            }
        }

        appendPayoutHistoryFromAny(canonicalFinancial?.optAny("payoutHistory"))
        appendPayoutHistoryFromAny(canonicalFinancial?.optAny("payoutHistorico"))

        listOf(
            payoutChartSeries,
            sections.optAny("payoutHistorico"),
            financialCharts?.optAny("payoutHistorico"),
            sections.optObject("demonstrativos.payoutHistorico"),
            results.optAny("payoutHistorico"),
            results.optAny("payoutHistory"),
            results.optAny("payout"),
            root.optObject("appPayload.charts.payoutHistorico"),
            root.optObject("appPayload.charts.payoutHistory"),
            root.optObject("appPayload.charts.payout"),
            root.optObject("appMobileSnapshot.charts.payoutHistorico"),
            root.optObject("appMobileSnapshot.charts.payoutHistory"),
            root.optObject("appMobileSnapshot.charts.payout"),
            root.optObject("assetClassContract.groups.statements.fields.payoutHistorico.value"),
            root.optObject("assetClassContract.groups.statements.fields.payoutHistory.value"),
            root.optObject("assetClassContract.groups.statements.fields.payout.value")
        ).forEach(::appendPayoutHistoryFromAny)

        val indexComparison = mutableListOf<AssetComparisonSeries>()
        canonicalCharts?.optAny("indexComparison")?.let { indexComparison.addAll(parseComparisonSeriesFromAny(it, ticker)) }
        val compIndices = firstObject(
            sections.optJSONObject("comparacaoIndices"),
            sections.optJSONObject("rentabilidadeChart"),
            sections.optJSONObject("indexComparison"),
            sections.optJSONObject("indicesComparison"),
            sections.optJSONObject("rentabilidade"),
            results.optJSONObject("comparacaoIndices"),
            results.optJSONObject("rentabilidadeChart"),
            results.optJSONObject("indexComparison"),
            results.optJSONObject("indicesComparison"),
            results.optJSONObject("rentabilidade"),
            root.optJSONObject("comparison"),
            root.optJSONObject("compare")
        )
        if (indexComparison.isEmpty() && compIndices != null) {
            indexComparison.addAll(parseComparisonSeriesFromObject(compIndices, ticker))
        }

        val commodityComparison = mutableListOf<AssetComparisonSeries>()
        canonicalCharts?.optAny("commodityComparison")?.let { commodityComparison.addAll(parseComparisonSeriesFromAny(it, "BRENT")) }
        val compCommObj = sections.optJSONObject("comparacaoCommodity") ?: results.optJSONObject("comparacaoCommodity") ?: sections.optJSONObject("commodities")
        if (commodityComparison.isEmpty() && compCommObj != null) {
            val brentArr = compCommObj.optJSONArray("brent") ?: compCommObj.optJSONArray("BRENT") ?: compCommObj.optJSONArray("petroleoBrent")
            if (brentArr != null) {
                val pointsList = mutableListOf<AssetComparisonPoint>()
                for (i in 0 until brentArr.length()) {
                    val pt = brentArr.optJSONObject(i) ?: continue
                    pointsList.add(AssetComparisonPoint(label = firstText(pt.optAny("label"), pt.optAny("date"), "P${i+1}"), value = firstNumber(pt.optAny("value"), pt.optAny("valor"), pt.optAny("percent"))))
                }
                if (pointsList.size >= 2) {
                    commodityComparison.add(AssetComparisonSeries("BRENT", pointsList))
                }
            }
            if (commodityComparison.isEmpty()) {
                commodityComparison.addAll(parsePercentSeriesFromText(textSection(compCommObj), setOf("BRENT", "OURO", "MILHO", "CAFÉ", "COBRE")))
            }
        }

        val revenueProfit = mutableListOf<FinancialStatementPoint>()
        val demObj = firstObject(sections.optJSONObject("demonstrativos"), results.optJSONObject("demonstrativos"), financialCharts)
        revenueProfit.addAll(
            parseFirstFinancialStatementPoints(
                canonicalFinancial?.optAny("revenueProfit"),
                canonicalFinancial?.optAny("receitasLucros"),
                dreChartSeries,
                financialCharts?.optAny("receitasLucros"),
                financialCharts?.optAny("revenueProfit"),
                financialCharts?.optAny("incomeStatement"),
                financialCharts?.optAny("dre"),
                financialCharts?.optAny("resultados"),
                financialCharts?.optAny("items"),
                demObj?.optAny("receitasLucros"),
                demObj?.optAny("revenueProfit"),
                demObj?.optAny("incomeStatement"),
                demObj?.optAny("dre"),
                demObj?.optAny("resultados"),
                demObj?.optAny("items"),
                sections.optAny("receitasLucros"),
                sections.optAny("revenueProfit"),
                sections.optObject("demonstrativos.receitasLucros"),
                sections.optObject("demonstrativos.revenueProfit"),
                results.optAny("receitasLucros"),
                results.optAny("revenueProfit"),
                results.optAny("incomeStatement"),
                normalized?.optAny("receitasLucros"),
                normalized?.optAny("revenueProfit"),
                normalized?.optAny("incomeStatement"),
                root.optObject("appPayload.charts.receitasLucros"),
                root.optObject("appPayload.charts.revenueProfit"),
                root.optObject("appPayload.charts.incomeStatement"),
                root.optObject("appPayload.charts.dre"),
                root.optObject("appMobileSnapshot.charts.receitasLucros"),
                root.optObject("appMobileSnapshot.charts.revenueProfit"),
                root.optObject("appMobileSnapshot.charts.incomeStatement"),
                root.optObject("appMobileSnapshot.charts.dre"),
                root.optObject("assetClassContract.groups.statements.fields.receitasLucros.value"),
                root.optObject("assetClassContract.groups.statements.fields.revenueProfit.value"),
                root.optObject("assetClassContract.groups.statements.fields.incomeStatement.value"),
                root.optObject("assetClassContract.groups.statements.fields.dre.value"),
                root.optObject("assetClassContract.groups.statements.fields.resultados.value")
            )
        )

        val profitVsQuote = mutableListOf<AssetComparisonPoint>()
        profitVsQuote.addAll(
            parseFirstProfitVsQuotePoints(
                canonicalFinancial?.optAny("profitVsQuote"),
                canonicalFinancial?.optAny("lucroCotacao"),
                profitQuoteChartSeries,
                financialCharts?.optAny("lucroCotacao"),
                financialCharts?.optAny("profitVsQuote"),
                financialCharts?.optAny("quoteProfit"),
                financialCharts?.optAny("cotacaoLucro"),
                demObj?.optAny("lucroCotacao"),
                demObj?.optAny("profitVsQuote"),
                demObj?.optAny("quoteProfit"),
                sections.optAny("lucroCotacao"),
                sections.optAny("profitVsQuote"),
                sections.optObject("demonstrativos.lucroCotacao"),
                results.optAny("lucroCotacao"),
                results.optAny("profitVsQuote"),
                normalized?.optAny("lucroCotacao"),
                normalized?.optAny("profitVsQuote"),
                normalized?.optAny("quoteProfit"),
                root.optObject("appPayload.charts.lucroCotacao"),
                root.optObject("appPayload.charts.profitVsQuote"),
                root.optObject("appPayload.charts.quoteProfit"),
                root.optObject("appPayload.charts.cotacaoLucro"),
                root.optObject("appMobileSnapshot.charts.lucroCotacao"),
                root.optObject("appMobileSnapshot.charts.profitVsQuote"),
                root.optObject("appMobileSnapshot.charts.quoteProfit"),
                root.optObject("appMobileSnapshot.charts.cotacaoLucro"),
                root.optObject("assetClassContract.groups.statements.fields.lucroCotacao.value"),
                root.optObject("assetClassContract.groups.statements.fields.profitVsQuote.value"),
                root.optObject("assetClassContract.groups.statements.fields.quoteProfit.value"),
                root.optObject("assetClassContract.groups.statements.fields.cotacaoLucro.value")
            )
        )

        val balanceSheet = mutableListOf<FinancialStatementPoint>()
        balanceSheet.addAll(
            parseFirstFinancialStatementPoints(
                canonicalFinancial?.optAny("balanceSheet"),
                canonicalFinancial?.optAny("balancoPatrimonial"),
                financialCharts?.optAny("balancoPatrimonial"),
                financialCharts?.optAny("balanceSheet"),
                demObj?.optAny("balancoPatrimonial"),
                demObj?.optAny("balanceSheet"),
                sections.optAny("balancoPatrimonial"),
                sections.optObject("demonstrativos.balancoPatrimonial"),
                results.optAny("balancoPatrimonial"),
                normalized?.optAny("balancoPatrimonial"),
                normalized?.optAny("balanceSheet"),
                root.optObject("appPayload.charts.balancoPatrimonial"),
                root.optObject("appPayload.charts.balanceSheet"),
                root.optObject("appMobileSnapshot.charts.balancoPatrimonial"),
                root.optObject("appMobileSnapshot.charts.balanceSheet"),
                root.optObject("assetClassContract.groups.statements.fields.balancoPatrimonial.value"),
                root.optObject("assetClassContract.groups.statements.fields.balanceSheet.value")
            )
        )

        val equityEvolution = mutableListOf<FinancialStatementPoint>()
        equityEvolution.addAll(
            parseFirstFinancialStatementPoints(
                canonicalFinancial?.optAny("equityEvolution"),
                canonicalFinancial?.optAny("evolucaoPatrimonio"),
                equityChartSeries,
                financialCharts?.optAny("evolucaoPatrimonio"),
                financialCharts?.optAny("equityEvolution"),
                demObj?.optAny("evolucaoPatrimonio"),
                demObj?.optAny("equityEvolution"),
                sections.optAny("evolucaoPatrimonio"),
                sections.optAny("equityEvolution"),
                sections.optObject("demonstrativos.evolucaoPatrimonio"),
                results.optAny("evolucaoPatrimonio"),
                results.optAny("equityEvolution"),
                normalized?.optAny("evolucaoPatrimonio"),
                normalized?.optAny("equityEvolution"),
                root.optObject("appPayload.charts.evolucaoPatrimonio"),
                root.optObject("appPayload.charts.equityEvolution"),
                root.optObject("appMobileSnapshot.charts.evolucaoPatrimonio"),
                root.optObject("appMobileSnapshot.charts.equityEvolution"),
                root.optObject("assetClassContract.groups.statements.fields.evolucaoPatrimonio.value"),
                root.optObject("assetClassContract.groups.statements.fields.equityEvolution.value")
            )
        )
        if (equityEvolution.isEmpty()) equityEvolution.addAll(balanceSheet)

        val revenueByRegion = mutableMapOf<String, List<AssetBreakdownPoint>>()
        val revenueByBusiness = mutableMapOf<String, List<AssetBreakdownPoint>>()

        revenueByRegion.putAll(
            parseFirstBreakdownMap(
                revenueRegionChartSeries,
                sections.optAny("regioesReceita"),
                sections.optObject("empresa.regioesReceita"),
                results.optAny("regioesReceita"),
                results.optAny("geografiaReceita"),
                results.optAny("revenueGeography"),
                results.optAny("revenueByRegion"),
                results.optAny("distribuicaoFaturamento.regioes"),
                results.optAny("distribuicaoFaturamento.regioesReceita"),
                root.optAny("revenueGeography"),
                root.optAny("revenueByRegion"),
                normalized?.optAny("revenueGeography"),
                normalized?.optAny("revenueByRegion"),
                normalized?.optAny("regioesReceita"),
                root.optObject("appPayload.charts.revenueGeography"),
                root.optObject("appPayload.charts.regioesReceita"),
                root.optObject("appPayload.charts.revenueByRegion"),
                root.optAny("appPayload.charts.revenueBreakdowns.geography"),
                root.optAny("appPayload.charts.revenueBreakdowns.byRegion"),
                root.optAny("appPayload.charts.revenueBreakdowns.regions"),
                root.optObject("appPayload.charts.revenueBreakdowns.revenueGeography"),
                root.optObject("appPayload.charts.revenueBreakdowns.regioesReceita"),
                root.optObject("appPayload.charts.revenueBreakdowns.revenueByRegion"),
                root.optAny("appMobileSnapshot.revenueBreakdowns.geography"),
                root.optAny("appMobileSnapshot.revenueBreakdowns.byRegion"),
                root.optAny("appMobileSnapshot.revenueBreakdowns.regions"),
                root.optObject("appMobileSnapshot.revenueBreakdowns.revenueGeography"),
                root.optObject("appMobileSnapshot.revenueBreakdowns.regioesReceita"),
                root.optObject("appMobileSnapshot.revenueBreakdowns.revenueByRegion"),
                root.optObject("assetClassContract.groups.statements.fields.regioesReceita.value"),
                root.optObject("assetClassContract.groups.statements.fields.revenueGeography.value")
            )
        )

        revenueByBusiness.putAll(
            parseFirstBreakdownMap(
                revenueBusinessChartSeries,
                sections.optAny("negociosReceita"),
                sections.optObject("empresa.negociosReceita"),
                results.optAny("negociosReceita"),
                results.optAny("segmentosReceita"),
                results.optAny("revenueSegment"),
                results.optAny("revenueByBusiness"),
                results.optAny("distribuicaoFaturamento.negocios"),
                results.optAny("distribuicaoFaturamento.negociosReceita"),
                results.optAny("distribuicaoFaturamento.segmentosReceita"),
                root.optAny("revenueSegment"),
                root.optAny("revenueByBusiness"),
                normalized?.optAny("negociosReceita"),
                normalized?.optAny("segmentosReceita"),
                normalized?.optAny("revenueSegment"),
                normalized?.optAny("revenueByBusiness"),
                root.optObject("appPayload.charts.revenueSegment"),
                root.optObject("appPayload.charts.revenueByBusiness"),
                root.optObject("appPayload.charts.negociosReceita"),
                root.optObject("appPayload.charts.segmentosReceita"),
                root.optAny("appPayload.charts.revenueBreakdowns.business"),
                root.optAny("appPayload.charts.revenueBreakdowns.byBusiness"),
                root.optAny("appPayload.charts.revenueBreakdowns.segments"),
                root.optObject("appPayload.charts.revenueBreakdowns.revenueSegment"),
                root.optObject("appPayload.charts.revenueBreakdowns.revenueByBusiness"),
                root.optObject("appPayload.charts.revenueBreakdowns.negociosReceita"),
                root.optObject("appPayload.charts.revenueBreakdowns.segmentosReceita"),
                root.optAny("appMobileSnapshot.revenueBreakdowns.business"),
                root.optAny("appMobileSnapshot.revenueBreakdowns.byBusiness"),
                root.optAny("appMobileSnapshot.revenueBreakdowns.segments"),
                root.optObject("appMobileSnapshot.revenueBreakdowns.revenueSegment"),
                root.optObject("appMobileSnapshot.revenueBreakdowns.revenueByBusiness"),
                root.optObject("appMobileSnapshot.revenueBreakdowns.negociosReceita"),
                root.optObject("appMobileSnapshot.revenueBreakdowns.segmentosReceita"),
                root.optObject("assetClassContract.groups.statements.fields.negociosReceita.value"),
                root.optObject("assetClassContract.groups.statements.fields.revenueSegment.value"),
                root.optObject("assetClassContract.groups.statements.fields.revenueByBusiness.value")
            )
        )

        if (!hasUsableBreakdownMap(revenueByRegion)) {
            revenueByRegion.clear()
            revenueByRegion.putAll(
                parseBreakdownMap(
                    firstObject(
                        results.optJSONObject("revenueGeography"),
                        results.optJSONObject("revenueByRegion"),
                        results.optJSONObject("regioesReceita"),
                        results.optJSONObject("geografiaReceita"),
                        results.optJSONObject("distribuicaoFaturamento"),
                        sections.optJSONObject("revenueGeography"),
                        sections.optJSONObject("geografiaReceita"),
                        root.optObject("appPayload.charts.revenueGeography"),
                        root.optObject("appPayload.charts.revenueByRegion"),
                        root.optObject("appPayload.charts.revenueBreakdowns.revenueGeography"),
                        root.optObject("appMobileSnapshot.revenueBreakdowns.revenueGeography"),
                        root.optObject("assetClassContract.groups.statements.fields.regioesReceita.value"),
                        root.optObject("assetClassContract.groups.statements.fields.revenueGeography.value")
                    )
                )
            )
        }
        if (!hasUsableBreakdownMap(revenueByBusiness)) {
            revenueByBusiness.clear()
            revenueByBusiness.putAll(
                parseBreakdownMap(
                    firstObject(
                        results.optJSONObject("revenueSegment"),
                        results.optJSONObject("revenueByBusiness"),
                        results.optJSONObject("negociosReceita"),
                        results.optJSONObject("segmentosReceita"),
                        results.optJSONObject("distribuicaoFaturamento"),
                        sections.optJSONObject("revenueSegment"),
                        sections.optJSONObject("segmentosReceita"),
                        root.optObject("appPayload.charts.revenueSegment"),
                        root.optObject("appPayload.charts.revenueByBusiness"),
                        root.optObject("appPayload.charts.revenueBreakdowns.revenueByBusiness"),
                        root.optObject("appMobileSnapshot.revenueBreakdowns.revenueByBusiness"),
                        root.optObject("assetClassContract.groups.statements.fields.negociosReceita.value"),
                        root.optObject("assetClassContract.groups.statements.fields.revenueSegment.value")
                    )
                )
            )
        }

        val fiiDistribution12m = mutableListOf<AssetIndicatorPoint>()
        val fiiPeerAverage = mutableListOf<AssetComparisonPoint>()
        val fiiPatrimonialInfo = mutableListOf<AssetIndicatorPoint>()
        val fiiAssetDistribution = mutableMapOf<String, List<AssetBreakdownPoint>>()

        fun addFiiDistribution12mPoint(labelRaw: String, yieldRaw: Any?, amountRaw: Any? = null) {
            val label = when (val clean = labelRaw.trim()) {
                "1M", "1 M", "1 MÊS", "1 MES" -> "Yield 1M"
                "3M", "3 M", "3 MESES" -> "Yield 3M"
                "6M", "6 M", "6 MESES" -> "Yield 6M"
                "12M", "12 M", "12 MESES", "1 ANO" -> "Yield 12M"
                else -> clean.ifBlank { "Yield" }
            }
            val yield = firstNumber(yieldRaw)
            if (!yield.isFinite() || yield == 0.0) return
            if (fiiDistribution12m.any { canonicalKey(it.label) == canonicalKey(label) }) return
            val amount = firstNumber(amountRaw)
            val display = if (amount > 0.0) {
                String.format(Locale.ROOT, "%.2f%% • R$ %.2f", yield, amount)
            } else {
                String.format(Locale.ROOT, "%.2f%%", yield)
            }
            fiiDistribution12m.add(AssetIndicatorPoint(label = label, value = yield, display = display, unit = "%"))
        }

        fun appendFiiDistribution12mFromAny(source: Any?) {
            when (source) {
                is JSONObject -> {
                    val labelsArr = firstArray(
                        source.optJSONArray("labels"), source.optJSONArray("periods"), source.optJSONArray("periodos"),
                        source.optJSONArray("categories"), source.optJSONArray("keys")
                    )
                    val yieldArr = firstArray(
                        source.optJSONArray("yieldPercent"), source.optJSONArray("yieldPercents"), source.optJSONArray("yields"),
                        source.optJSONArray("yield"), source.optJSONArray("percent"), source.optJSONArray("percentual"),
                        source.optJSONArray("values"), source.optJSONArray("data")
                    )
                    val amountArr = firstArray(
                        source.optJSONArray("amounts"), source.optJSONArray("amount"), source.optJSONArray("valores"),
                        source.optJSONArray("valor"), source.optJSONArray("paidPerShare"), source.optJSONArray("rendimento")
                    )
                    if (labelsArr != null && yieldArr != null) {
                        val len = kotlin.math.min(labelsArr.length(), yieldArr.length())
                        for (i in 0 until len) {
                            addFiiDistribution12mPoint(labelsArr.optString(i, "P${i + 1}"), yieldArr.opt(i), amountArr?.opt(i))
                        }
                    }
                    val items = firstArray(
                        source.optJSONArray("items"), source.optJSONArray("points"), source.optJSONArray("rows"),
                        source.optJSONArray("series"), source.optJSONArray("distribution12m"), source.optJSONArray("distribuicoes12m")
                    )
                    if (items != null) appendFiiDistribution12mFromAny(items)

                    val directLabel = firstText(source.optAny("period"), source.optAny("periodo"), source.optAny("label"), source.optAny("name"), source.optAny("prazo"))
                    val directYield = firstNumber(source.optAny("yieldPercent"), source.optAny("yield"), source.optAny("percent"), source.optAny("percentual"), source.optAny("value"), source.optAny("valor"))
                    if (directLabel.isNotBlank() && directYield != 0.0) {
                        addFiiDistribution12mPoint(directLabel, directYield, firstNumber(source.optAny("amount"), source.optAny("valorPago"), source.optAny("paid"), source.optAny("rendimento")))
                    }
                }
                is JSONArray -> {
                    for (i in 0 until source.length()) {
                        when (val item = source.opt(i)) {
                            is JSONObject -> {
                                val label = firstText(item.optAny("period"), item.optAny("periodo"), item.optAny("label"), item.optAny("name"), item.optAny("prazo"), "P${i + 1}")
                                val yield = firstNumber(item.optAny("yieldPercent"), item.optAny("yield"), item.optAny("percent"), item.optAny("percentual"), item.optAny("value"), item.optAny("valor"), item.optAny("y"))
                                val amount = firstNumber(item.optAny("amount"), item.optAny("valorPago"), item.optAny("paid"), item.optAny("rendimento"), item.optAny("secondaryValue"))
                                addFiiDistribution12mPoint(label, yield, amount)
                                val nested = firstArray(item.optJSONArray("points"), item.optJSONArray("items"), item.optJSONArray("data"), item.optJSONArray("values"))
                                if (nested != null) appendFiiDistribution12mFromAny(nested)
                            }
                            is JSONArray -> {
                                val label = if (item.length() > 0) firstText(item.opt(0), "P${i + 1}") else "P${i + 1}"
                                val yield = if (item.length() > 1) item.opt(1) else null
                                val amount = if (item.length() > 2) item.opt(2) else null
                                addFiiDistribution12mPoint(label, yield, amount)
                            }
                        }
                    }
                }
            }
        }

        if (isFii) {
            appendFiiDistribution12mFromAny(canonicalFii?.optAny("distribution12m"))
            appendFiiDistribution12mFromAny(canonicalCharts?.optAny("fii.distribution12m"))
            appendFiiDistribution12mFromAny(sections.optAny("distribuicoes12m"))
            appendFiiDistribution12mFromAny(results.optAny("distribuicoes12m"))
            appendFiiDistribution12mFromAny(root.optAny("data.distribuicoes12m"))
            val y1m = firstNumber(normalizedValue(normalized, "yield1m"), results.optAny("yield1m"), indicadores.optAny("yield1m"))
            val y3m = firstNumber(normalizedValue(normalized, "yield3m"), results.optAny("yield3m"), indicadores.optAny("yield3m"))
            val y6m = firstNumber(normalizedValue(normalized, "yield6m"), results.optAny("yield6m"), indicadores.optAny("yield6m"))
            val y12m_val = firstNumber(normalizedValue(normalized, "yield12m"), results.optAny("yield12m"), indicadores.optAny("yield12m"), dyVal)
            if (y1m != 0.0) addFiiDistribution12mPoint("Yield 1M", y1m)
            if (y3m != 0.0) addFiiDistribution12mPoint("Yield 3M", y3m)
            if (y6m != 0.0) addFiiDistribution12mPoint("Yield 6M", y6m)
            if (y12m_val != 0.0) addFiiDistribution12mPoint("Yield 12M", y12m_val)
            if (fiiDistribution12m.isEmpty() && dividendMonthly.isNotEmpty()) {
                val currentPriceForYield = firstNumber(normalizedValue(normalized, "precoAtual"), results.optAny("precoAtual"), results.optObject("cotacao")?.optAny("precoAtual"))
                dividendMonthly.takeLast(12).forEach { month ->
                    val pct = if (currentPriceForYield > 0.0) (month.value / currentPriceForYield) * 100.0 else month.value
                    if (pct > 0.0 && pct.isFinite()) {
                        addFiiDistribution12mPoint(month.period.ifBlank { "Mês" }, pct)
                    }
                }
            }

            val infoFii = mergedObject(
                canonicalFii?.optJSONObject("info"),
                canonicalFii?.optJSONObject("informacoesFundo"),
                results.optJSONObject("informacoesFundo"),
                sections.optJSONObject("informacoesFundo"),
                results.optJSONObject("dadosFundo"),
                sections.optJSONObject("dadosFundo"),
                results.optJSONObject("fund"),
                sections.optJSONObject("fund"),
                root.optJSONObject("informacoesFundo")
            )
            val fiiValorPatrimonialObj = mergedObject(results.optJSONObject("valorPatrimonial"), sections.optJSONObject("valorPatrimonial"), root.optJSONObject("valorPatrimonial"))
            val vpaVal = firstNumber(normalizedValue(normalized, "valorPatrimonialCota"), results.optAny("vpa"), results.optAny("valorPatrimonialCota"), fiiValorPatrimonialObj?.optAny("valorPatrimonial"), fiiValorPatrimonialObj?.optAny("valorPatrimonialRaw"), infoFii?.optAny("valorPatrimonialCota"))
            val currentPrice = firstNumber(normalizedValue(normalized, "precoAtual"), results.optAny("precoAtual"), results.optObject("cotacao")?.optAny("precoAtual"))
            val numCotas = firstText(results.optAny("cotasEmitidas"), results.optAny("totalPapeis"), infoFii?.optAny("cotasEmitidas"), infoFii?.optAny("quantidadeCotas"))
            val pvpVal = firstNumber(normalizedValue(normalized, "pvp"), results.optAny("pvp"), results.optObject("indicadores")?.optAny("pvp"))
            val plTotal = firstNumber(normalizedValue(normalized, "patrimonioLiquido"), results.optAny("patrimonioLiquido"), fiiValorPatrimonialObj?.optAny("patrimonioLiquido"), fiiValorPatrimonialObj?.optAny("patrimonioLiquidoRaw"), infoFii?.optAny("patrimonioLiquido"))
            val ultimoRendimento = firstNumber(results.optAny("ultimoRendimento"), results.optAny("lastDividend"), results.optAny("rendimento"), results.optObject("indicadores")?.optAny("ultimoRendimento"), infoFii?.optAny("ultimoRendimento"))
            val vacancia = firstNumber(normalizedValue(normalized, "vacanciaFisica"), results.optAny("vacanciaFisica"), infoFii?.optAny("vacanciaFisica"), infoFii?.optAny("vacancia"))
            val numeroImoveis = firstNumber(results.optAny("numeroImoveis"), infoFii?.optAny("numeroImoveis"), sections.optJSONArray("listaImoveis")?.length(), results.optJSONArray("listaImoveis")?.length())
            val cotistas = firstText(results.optAny("numeroCotistas"), results.optAny("cotistas"), infoFii?.optAny("numeroCotistas"), infoFii?.optAny("cotistas"))

            if (vpaVal > 0.0) fiiPatrimonialInfo.add(AssetIndicatorPoint("VPA por Cota", vpaVal, String.format(Locale.ROOT, "R$ %.2f", vpaVal)))
            if (currentPrice > 0.0) fiiPatrimonialInfo.add(AssetIndicatorPoint("Valor da Cota", currentPrice, String.format(Locale.ROOT, "R$ %.2f", currentPrice)))
            if (ultimoRendimento > 0.0) fiiPatrimonialInfo.add(AssetIndicatorPoint("Último Rendimento", ultimoRendimento, String.format(Locale.ROOT, "R$ %.4f", ultimoRendimento)))
            if (vacancia > 0.0) fiiPatrimonialInfo.add(AssetIndicatorPoint("Vacância Física", vacancia, String.format(Locale.ROOT, "%.2f%%", vacancia), "%"))
            if (numeroImoveis > 0.0) fiiPatrimonialInfo.add(AssetIndicatorPoint("Imóveis", numeroImoveis, numeroImoveis.toInt().toString()))
            if (cotistas.isNotBlank()) {
                val cotistasNum = parseLocaleFinancialNumber(cotistas)
                if (cotistasNum > 0.0) fiiPatrimonialInfo.add(AssetIndicatorPoint("Cotistas", cotistasNum, cotistas))
            }
            if (numCotas.isNotBlank()) {
                val cn = parseLocaleFinancialNumber(numCotas)
                if (cn > 0.0) fiiPatrimonialInfo.add(AssetIndicatorPoint("Número de Cotas", cn, numCotas))
            }
            if (pvpVal > 0.0) fiiPatrimonialInfo.add(AssetIndicatorPoint("P/VP", pvpVal, String.format(Locale.ROOT, "%.2f", pvpVal)))
            if (plTotal > 0.0) fiiPatrimonialInfo.add(AssetIndicatorPoint("Patrimônio Total", plTotal, formatCurrencyPtBr(plTotal)))

            val compFiisObj = mergedObject(
                sections.optJSONObject("comparador"),
                sections.optJSONObject("comparadorFiis"),
                sections.optJSONObject("mediaTipoSegmento"),
                sections.optJSONObject("mediaSetor"),
                sections.optJSONObject("comparativoSetor"),
                sections.optJSONObject("peerAverage"),
                results.optJSONObject("comparador"),
                results.optJSONObject("comparadorFiis"),
                results.optJSONObject("mediaTipoSegmento"),
                results.optJSONObject("mediaSetor"),
                results.optJSONObject("comparativoSetor"),
                results.optJSONObject("peerAverage"),
                appPayload?.optJSONObject("comparador"),
                appPayload?.optJSONObject("peerAverage"),
                appPayload?.optJSONObject("segmentAverage"),
                appPayload?.optObject("charts.peerAverage"),
                appSnapshot?.optJSONObject("peerAverage"),
                appSnapshot?.optJSONObject("segmentAverage"),
                root.optObject("data.comparador"),
                root.optObject("data.peerAverage"),
                root.optObject("data.segmentAverage")
            )
            if (compFiisObj != null) {
                val avgPvp = firstNumber(
                    compFiisObj.optAny("pvpMedia"), compFiisObj.optAny("avgPvp"), compFiisObj.optAny("mediaPvp"),
                    compFiisObj.optAny("pvpSetor"), compFiisObj.optAny("pvpSegmento"), compFiisObj.optAny("pvpTipo"),
                    compFiisObj.optAny("mediaPVP"), compFiisObj.optAny("P/VP")
                )
                val avgDy = firstNumber(
                    compFiisObj.optAny("dyMedia"), compFiisObj.optAny("avgDy"), compFiisObj.optAny("mediaDy"),
                    compFiisObj.optAny("dySetor"), compFiisObj.optAny("dySegmento"), compFiisObj.optAny("yieldMedia"),
                    compFiisObj.optAny("dividendYieldMedia"), compFiisObj.optAny("DY")
                )
                val avgVacancy = firstNumber(
                    compFiisObj.optAny("vacanciaMedia"), compFiisObj.optAny("avgVacancy"), compFiisObj.optAny("vacanciaSetor"),
                    compFiisObj.optAny("vacanciaSegmento"), compFiisObj.optAny("vacancyAverage")
                )
                if (avgPvp > 0.0 && pvpVal > 0.0) fiiPeerAverage.add(AssetComparisonPoint("P/VP", pvpVal, avgPvp))
                if (avgDy > 0.0 && dyVal > 0.0) fiiPeerAverage.add(AssetComparisonPoint("DY 12M", dyVal, avgDy))
                if (avgVacancy > 0.0 && vacancia > 0.0) fiiPeerAverage.add(AssetComparisonPoint("Vacância", vacancia, avgVacancy))
            }

            val distAtivos = firstArray(
                canonicalFii?.optArray("physicalAssets"),
                canonicalFii?.optObject("physicalAssets")?.optJSONArray("items"),
                canonicalFii?.optObject("physicalAssets")?.optJSONArray("assets"),
                sections.optJSONArray("distribuicaoAtivos"),
                sections.optJSONArray("distribuicaoDeAtivos"),
                sections.optJSONArray("ativosFundo"),
                sections.optJSONArray("listaImoveis"),
                results.optJSONArray("distribuicaoAtivos"),
                results.optJSONArray("distribuicaoDeAtivos"),
                results.optJSONArray("ativosFundo"),
                results.optJSONArray("listaImoveis"),
                results.optArray("fundo.distribuicaoAtivos"),
                root.optArray("data.distribuicaoAtivos"),
                root.optArray("data.ativosFundo"),
                root.optArray("data.listaImoveis")
            )
            if (distAtivos != null) {
                val list = mutableListOf<AssetBreakdownPoint>()
                for (i in 0 until distAtivos.length()) {
                    val d = distAtivos.optJSONObject(i) ?: continue
                    val nameStr = firstText(d.optAny("name"), d.optAny("estado"), d.optAny("nome"), d.optAny("category"))
                    val pct = firstNumber(d.optAny("valuePercent"), d.optAny("percent"), d.optAny("percentage"))
                    if (nameStr.isNotBlank()) {
                        list.add(AssetBreakdownPoint(name = nameStr, valuePercent = pct))
                    }
                }
                if (list.isNotEmpty()) {
                    fiiAssetDistribution["Ativos"] = list
                }
            } else {
                val imoveisArr = sections.optJSONArray("listaImoveis") ?: results.optJSONArray("listaImoveis")
                if (imoveisArr != null && imoveisArr.length() > 0) {
                    val countByState = mutableMapOf<String, Int>()
                    for (i in 0 until imoveisArr.length()) {
                        val imovel = imoveisArr.optJSONObject(i) ?: continue
                        val state = firstText(imovel.optAny("estado"), imovel.optAny("uf"), "Outros")
                        countByState[state] = (countByState[state] ?: 0) + 1
                    }
                    val total = imoveisArr.length().toDouble()
                    val list = countByState.map { (state, count) ->
                        AssetBreakdownPoint(name = state, valuePercent = (count / total) * 100.0, displayValue = "$count Imóveis")
                    }
                    if (list.isNotEmpty()) fiiAssetDistribution["Ativos"] = list
                }
            }
            if (fiiAssetDistribution.isEmpty()) {
                val classificacao = firstText(
                    infoFii?.optAny("segmento"),
                    infoFii?.optAny("segmentoFii"),
                    infoFii?.optAny("tipoFundo"),
                    infoFii?.optAny("mandato"),
                    results.optAny("segmentoFii"),
                    results.optAny("tipoFundo"),
                    "Classificação não informada"
                )
                if (classificacao.isNotBlank() && classificacao != "Classificação não informada") {
                    fiiAssetDistribution["Ativos"] = listOf(
                        AssetBreakdownPoint(
                            name = classificacao,
                            valuePercent = 100.0,
                            displayValue = "Classificação do fundo"
                        )
                    )
                }
            }
        }

        return AssetChartBundle(
            ticker = ticker,
            type = if (isFii) "FII" else "ACAO",
            priceHistory = finalPriceHistory,
            profitability = profitability,
            realProfitability = realProfitability,
            indicatorCards = indicatorCards,
            indicatorHistory = indicatorHistory,
            dividendEvents = dividendEvents,
            dividendMonthly = dividendMonthly,
            dividendYearly = dividendYearly,
            dividendYieldHistory = dividendYieldHistory,
            indexComparison = indexComparison,
            commodityComparison = commodityComparison,
            revenueProfit = revenueProfit,
            profitVsQuote = profitVsQuote,
            equityEvolution = equityEvolution,
            balanceSheet = balanceSheet,
            payoutHistory = payoutHistory,
            revenueByRegion = revenueByRegion,
            revenueByBusiness = revenueByBusiness,
            fiiDistribution12m = fiiDistribution12m,
            fiiPeerAverage = fiiPeerAverage,
            fiiPatrimonialInfo = fiiPatrimonialInfo,
            fiiAssetDistribution = fiiAssetDistribution,
            warnings = warnings.distinct(),
            coverageCaptured = coverageCaptured,
            coverageMissing = coverageMissing,
            coverageNotApplicable = coverageNotApplicable,
            source = "VALORAE / Investidor10"
        )
    }

    private fun parseFlexibleDateMillis(value: String): Long {
        if (value.isBlank()) return 0L
        val raw = value.trim()
        raw.toLongOrNull()?.let { n ->
            if (n in 20_000L..80_000L) {
                val base = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(1899, Calendar.DECEMBER, 30, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return base.timeInMillis + (n * 86_400_000L)
            }
            return when {
                n > 10_000_000_000L -> n
                n > 1_000_000_000L -> n * 1000L
                else -> 0L
            }
        }
        raw.replace(',', '.').toDoubleOrNull()?.let { serial ->
            if (serial >= 20_000.0 && serial <= 80_000.0) {
                val base = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(1899, Calendar.DECEMBER, 30, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return base.timeInMillis + (serial * 86_400_000L).toLong()
            }
        }
        val normalized = raw.replace("às", " ", ignoreCase = true).replace(Regex("\\s+"), " ").trim()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy",
            "dd/MM/yy",
            "dd-MM-yyyy",
            "yyyy/MM/dd",
            "MM/yyyy",
            "yyyy-MM"
        )
        for (pattern in patterns) {
            try {
                val locale = if (pattern.startsWith("yyyy")) Locale.US else Locale("pt", "BR")
                val sdf = SimpleDateFormat(pattern, locale).apply {
                    isLenient = false
                    if (pattern.contains("'Z'") || pattern.contains("XXX")) timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(normalized)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return 0L
    }

    private fun normalizeDisplayDate(value: String): String {
        if (value.isBlank()) return ""
        val ts = parseFlexibleDateMillis(value)
        return if (ts > 0L) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ts)) else value
    }


    /**
     * Fallback direto legado. O caminho principal usa Serviço de dados VALORAE.
     */
    private fun fetchAssetDataDirect(ticker: String, bypassCache: Boolean = false): B3AssetData? {
        Log.w("B3NetworkService", "Fallback direto desativado para $ticker. Use somente Serviço de dados VALORAE.")
        return null
    }


    /**
     * Fetch historical chart points for graphing
     */
    private fun fetchHistoricalChartDirect(ticker: String, range: String = "1y"): List<ChartPoint> {
        Log.w("B3NetworkService", "Histórico direto desativado para $ticker/$range. Use somente Serviço de dados VALORAE.")
        return emptyList()
    }


    /**
     * Fetch news RSS search for Brazilian stocks
     */
    private fun fetchNewsDirect(ticker: String = ""): List<NewsItem> {
        Log.w("B3NetworkService", "Notícias diretas desativadas para ${ticker.ifBlank { "mercado" }}. Use somente Serviço de dados VALORAE.")
        return emptyList()
    }


    private fun enrichAssetDetails(base: B3AssetData): B3AssetData {
        // Não adiciona dados inventados. O caminho principal e único para dados de mercado é o Serviço de dados VALORAE.
        return base
    }
}
