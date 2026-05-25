package com.example.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
    val source: String = "ValorAe Engine",
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
    val magicNumber: Double = 0.0     // Magic Number (FII)
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

object B3NetworkService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val USER_AGENTS = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.7; rv:133.0) Gecko/20100101 Firefox/133.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"
    )

    private fun getRandomAgent() = USER_AGENTS.random()

    private class CacheEntry<T>(val data: T, val expiresAt: Long)
    private val memoryCache = ConcurrentHashMap<String, CacheEntry<Any>>()

    private fun <T> getFromCache(key: String): T? {
        val entry = memoryCache[key] ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) {
            memoryCache.remove(key)
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return entry.data as? T
    }

    private fun putInCache(key: String, data: Any, ttlMinutes: Int = 5) {
        val expiresAt = System.currentTimeMillis() + (ttlMinutes * 60 * 1000)
        memoryCache[key] = CacheEntry(data, expiresAt)
        
        // Optimize memory consumption by pruning expired elements when cache is growing
        if (memoryCache.size > 80) {
            val now = System.currentTimeMillis()
            memoryCache.entries.removeIf { it.value.expiresAt < now }
        }
    }

    private fun getTickerWithSuffix(ticker: String): String {
        val clean = ticker.trim().uppercase()
        if (clean.startsWith("^") || clean.contains("=") || clean.endsWith(".SA")) return clean
        return "$clean.SA"
    }

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



    /**
     * Fetch asset indicators using Yahoo Finance API with Nexus scraping fallback
     */
    fun fetchAssetData(ticker: String, bypassCache: Boolean = false): B3AssetData? {
        val cacheKey = "asset_data_$ticker"
        if (!bypassCache) {
            getFromCache<B3AssetData>(cacheKey)?.let { return it }
        } else {
            memoryCache.remove(cacheKey)
        }

        val symbol = getTickerWithSuffix(ticker)
        val isFii = inferIsFii(ticker)
        
        try {
            var price = 0.0
            var changePercent = 0.0
            var name = ticker.uppercase()

            // 1. Fetch Chart endpoint for real-time price & daily change
            val chartUrl = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=1d&interval=1d&includePrePost=false"
            val chartRequest = Request.Builder()
                .url(chartUrl)
                .addHeader("User-Agent", getRandomAgent())
                .build()

            client.newCall(chartRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.contains("meta")) {
                        val json = JSONObject(bodyString)
                        val chart = json.optJSONObject("chart")
                        val resultList = chart?.optJSONArray("result")
                        if (resultList != null && resultList.length() > 0) {
                            val result0 = resultList.getJSONObject(0)
                            val meta = result0.getJSONObject("meta")
                            price = meta.optDouble("regularMarketPrice", 0.0)
                            
                            val prevClose = if (meta.has("chartPreviousClose")) {
                                meta.optDouble("chartPreviousClose", price)
                            } else {
                                meta.optDouble("regularMarketPreviousClose", price)
                            }
                            
                            if (prevClose > 0.0 && !price.isNaN() && !prevClose.isNaN()) {
                                changePercent = ((price - prevClose) / prevClose) * 100.0
                            }
                            name = meta.optString("symbol", name).replace(".SA", "")
                        }
                    }
                }
            }

            // 2. Fetch QuoteSummary
            val summaryUrl = "https://query1.finance.yahoo.com/v10/finance/quoteSummary/$symbol?modules=financialData,defaultKeyStatistics,summaryDetail"
            val summaryRequest = Request.Builder()
                .url(summaryUrl)
                .addHeader("User-Agent", getRandomAgent())
                .build()

            var dy = 0.0
            var pl = 0.0
            var pvp = 0.0
            var vpa = 0.0
            var lpa = 0.0
            var marketCap = 0.0
            var roe = 0.0
            var margins = 0.0
            var lastDividend = 0.0
            var dailyLiquidity = 0.0
            var high52 = 0.0
            var low52 = 0.0
            var forwardPE = 0.0
            var priceToSales = 0.0
            var nextEarningsDate = ""
            var fiiVacancy = 0.0

            client.newCall(summaryRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.contains("quoteSummary")) {
                        val json = JSONObject(bodyString)
                        val quoteSummary = json.optJSONObject("quoteSummary")
                        val resultList = quoteSummary?.optJSONArray("result")
                        if (resultList != null && resultList.length() > 0) {
                            val result0 = resultList.getJSONObject(0)

                            val detail = result0.optJSONObject("summaryDetail")
                            if (detail != null) {
                                if (detail.has("trailingAnnualDividendYield") && !detail.isNull("trailingAnnualDividendYield")) {
                                    dy = detail.getJSONObject("trailingAnnualDividendYield").optDouble("raw", 0.0) * 100.0
                                } else if (detail.has("yield") && !detail.isNull("yield")) {
                                    dy = detail.getJSONObject("yield").optDouble("raw", 0.0) * 100.0
                                }
                                if (detail.has("dividendRate") && !detail.isNull("dividendRate")) {
                                    lastDividend = detail.getJSONObject("dividendRate").optDouble("raw", 0.0)
                                }
                                if (detail.has("marketCap") && !detail.isNull("marketCap")) {
                                    marketCap = detail.getJSONObject("marketCap").optDouble("raw", 0.0)
                                }
                                if (detail.has("averageVolume") && !detail.isNull("averageVolume")) {
                                    dailyLiquidity = detail.getJSONObject("averageVolume").optDouble("raw", 0.0)
                                }
                                if (detail.has("fiftyTwoWeekHigh") && !detail.isNull("fiftyTwoWeekHigh")) {
                                    high52 = detail.getJSONObject("fiftyTwoWeekHigh").optDouble("raw", 0.0)
                                }
                                if (detail.has("fiftyTwoWeekLow") && !detail.isNull("fiftyTwoWeekLow")) {
                                    low52 = detail.getJSONObject("fiftyTwoWeekLow").optDouble("raw", 0.0)
                                }
                                if (detail.has("forwardPE") && !detail.isNull("forwardPE")) {
                                    forwardPE = detail.getJSONObject("forwardPE").optDouble("raw", 0.0)
                                }
                                if (detail.has("priceToSalesTrailing12Months") && !detail.isNull("priceToSalesTrailing12Months")) {
                                    priceToSales = detail.getJSONObject("priceToSalesTrailing12Months").optDouble("raw", 0.0)
                                }
                            }

                            val stats = result0.optJSONObject("defaultKeyStatistics")
                            if (stats != null) {
                                if (stats.has("trailingPE") && !stats.isNull("trailingPE")) {
                                    pl = stats.getJSONObject("trailingPE").optDouble("raw", 0.0)
                                }
                                if (stats.has("priceToBook") && !stats.isNull("priceToBook")) {
                                    pvp = stats.getJSONObject("priceToBook").optDouble("raw", 0.0)
                                }
                                if (stats.has("bookValue") && !stats.isNull("bookValue")) {
                                    vpa = stats.getJSONObject("bookValue").optDouble("raw", 0.0)
                                }
                                if (stats.has("trailingEps") && !stats.isNull("trailingEps")) {
                                    lpa = stats.getJSONObject("trailingEps").optDouble("raw", 0.0)
                                }
                                if (stats.has("nextFiscalYearEnd") && !stats.isNull("nextFiscalYearEnd")) {
                                    val dateTs = stats.getJSONObject("nextFiscalYearEnd").optLong("raw", 0)
                                    if (dateTs > 0) {
                                        nextEarningsDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dateTs * 1000))
                                    }
                                }
                            }

                            val fd = result0.optJSONObject("financialData")
                            if (fd != null) {
                                if (fd.has("returnOnEquity") && !fd.isNull("returnOnEquity")) {
                                    roe = fd.getJSONObject("returnOnEquity").optDouble("raw", 0.0) * 100.0
                                }
                                if (fd.has("profitMargins") && !fd.isNull("profitMargins")) {
                                    margins = fd.getJSONObject("profitMargins").optDouble("raw", 0.0) * 100.0
                                }
                            }
                        }
                    }
                }
            }

            // 3. Fallback and Enrich (Scraping from Nexus Proxy)
            val numericFallback = mutableMapOf<String, Double>()
            val textFallback = mutableMapOf<String, String>()

            try {
                kotlinx.coroutines.runBlocking {
                    fun parseNumberOrNull(value: String): Double? {
                        var str = value
                            .replace("R$", "")
                            .replace("%", "")
                            .replace("Bilhões", "")
                            .replace("Bilhão", "")
                            .replace("Milhões", "")
                            .replace("Milhão", "")
                            .replace("B", "") // common short suffixes
                            .replace("M", "")
                            .trim()

                        if (str.isBlank() || str == "-" || str == "N/A" || str.equals("undefined", true) || str.equals("n.d.", true)) return null
                        if (str.contains(",")) str = str.replace(".", "").replace(",", ".")
                        return str.toDoubleOrNull()
                    }

                    val result = NexusProxyClient().buscarDadosAtivo(ticker)
                    if (result != null) {
                        val textKeys = setOf(
                            "cnpj", "listSegment", "foundationYear", "listingYear", "employeesCount", 
                            "totalPapers", "fiiTotalHolders", "fiiIssuedShares", "fiiAdminFee", 
                            "fiiFundType", "fiiMandate", "fiiTargetAudience", "fiiManagementType", 
                            "fiiDuration", "fiiSegment", "nextEarningsDate", "assetDescription", "subSector", "name"
                        )
                        for ((key, value) in result.results) {
                            if (value.isNotEmpty()) {
                                if (textKeys.contains(key)) {
                                    textFallback[key] = value
                                } else {
                                    val num = parseNumberOrNull(value)
                                    if (num != null) numericFallback[key] = num
                                }
                            }
                        }
                    }
                }
            } catch(e: Exception) {
                Log.e("B3NetworkService", "Nexus Error: ${e.message}")
            }

            // Sync from fallbacks if Yahoo is missing (0.0 or empty)
            if (price == 0.0) price = numericFallback["price"] ?: 0.0
            if (changePercent == 0.0) changePercent = numericFallback["changePercent"] ?: 0.0
            if (dy == 0.0) dy = numericFallback["dy"] ?: 0.0
            if (pl == 0.0 && !isFii) pl = numericFallback["pl"] ?: 0.0
            if (pvp == 0.0) pvp = numericFallback["pvp"] ?: 0.0
            if (vpa == 0.0) vpa = numericFallback["vpa"] ?: 0.0
            if (lpa == 0.0 && !isFii) lpa = numericFallback["lpa"] ?: 0.0
            if (roe == 0.0) roe = numericFallback["roe"] ?: 0.0
            if (margins == 0.0) margins = numericFallback["margins"] ?: 0.0
            if (dailyLiquidity == 0.0) dailyLiquidity = numericFallback["liquidity"] ?: 0.0
            if (marketCap == 0.0) marketCap = numericFallback["marketCap"] ?: 0.0
            if (isFii) fiiVacancy = numericFallback["fiiVacancy"] ?: 0.0
            
            if (nextEarningsDate.isEmpty()) nextEarningsDate = textFallback["nextEarningsDate"] ?: ""
            if (lastDividend == 0.0) lastDividend = numericFallback["lastDividend"] ?: 0.0
            if (name == ticker.uppercase()) name = textFallback["name"] ?: name

            val result = B3AssetData(
                ticker = ticker.uppercase(),
                name = name,
                price = price,
                changePercent = changePercent,
                dy = dy,
                pl = pl,
                pvp = pvp,
                vpa = vpa,
                lpa = lpa,
                marketCap = marketCap,
                roe = roe,
                margins = margins,
                lastDividend = lastDividend,
                dailyLiquidity = dailyLiquidity,
                high52 = high52,
                low52 = low52,
                forwardPE = forwardPE,
                priceToSales = numericFallback["priceToSales"] ?: priceToSales,
                nextEarningsDate = nextEarningsDate,
                isFii = isFii,
                fiiVacancy = fiiVacancy,
                
                // Novos Campos Investidor10
                grossMargin = numericFallback["grossMargin"] ?: 0.0,
                ebitMargin = numericFallback["ebitMargin"] ?: 0.0,
                ebitdaMargin = numericFallback["ebitdaMargin"] ?: 0.0,
                evEbitda = numericFallback["evEbitda"] ?: 0.0,
                evEbit = numericFallback["evEbit"] ?: 0.0,
                priceEbitda = numericFallback["priceEbitda"] ?: 0.0,
                priceEbit = numericFallback["priceEbit"] ?: 0.0,
                priceAsset = numericFallback["priceAsset"] ?: 0.0,
                priceCapGiro = numericFallback["priceCapGiro"] ?: 0.0,
                priceAtivoCircLiq = numericFallback["priceAtivoCircLiq"] ?: 0.0,
                giroAtivos = numericFallback["giroAtivos"] ?: 0.0,
                roic = numericFallback["roic"] ?: 0.0,
                roa = numericFallback["roa"] ?: 0.0,
                divLiqPatrimonio = numericFallback["divLiqPatrimonio"] ?: 0.0,
                debtEbitda = numericFallback["debtEbitda"] ?: 0.0,
                divLiqEbit = numericFallback["divLiqEbit"] ?: 0.0,
                divBrutaPatrimonio = numericFallback["divBrutaPatrimonio"] ?: 0.0,
                patrimonioAtivos = numericFallback["patrimonioAtivos"] ?: 0.0,
                passivosAtivos = numericFallback["passivosAtivos"] ?: 0.0,
                liquidezCorrente = numericFallback["liquidezCorrente"] ?: 0.0,
                cagrRevenue5y = numericFallback["cagrRevenue5y"] ?: 0.0,
                cagrProfit5y = numericFallback["cagrProfit5y"] ?: 0.0,
                payout = numericFallback["payout"] ?: 0.0,

                // FII
                fiiTotalHolders = textFallback["fiiTotalHolders"] ?: "",
                fiiIssuedShares = textFallback["fiiIssuedShares"] ?: "",
                fiiAdminFee = textFallback["fiiAdminFee"] ?: "",
                fiiFundType = textFallback["fiiFundType"] ?: "",
                fiiMandate = textFallback["fiiMandate"] ?: "",
                fiiTargetAudience = textFallback["fiiTargetAudience"] ?: "",
                fiiManagementType = textFallback["fiiManagementType"] ?: "",
                fiiDuration = textFallback["fiiDuration"] ?: "",
                fiiSegment = textFallback["fiiSegment"] ?: "",
                magicNumber = if (isFii && lastDividend > 0.0 && price > 0.0) kotlin.math.ceil(price / lastDividend) else numericFallback["magicNumber"] ?: 0.0,

                // Textos
                cnpj = textFallback["cnpj"] ?: "",
                listSegment = textFallback["listSegment"] ?: "",
                foundationYear = textFallback["foundationYear"] ?: "",
                listingYear = textFallback["listingYear"] ?: "",
                employeesCount = textFallback["employeesCount"] ?: "",
                totalPapers = textFallback["totalPapers"] ?: "",

                // Balanço Monetário
                firmValue = numericFallback["firmValue"] ?: 0.0,
                netWorth = numericFallback["netWorth"] ?: 0.0,
                totalAssets = numericFallback["totalAssets"] ?: 0.0,
                currentAssets = numericFallback["currentAssets"] ?: 0.0,
                grossDebt = numericFallback["grossDebt"] ?: 0.0,
                netDebt = numericFallback["netDebt"] ?: 0.0,
                availability = numericFallback["availability"] ?: 0.0,
                freeFloat = numericFallback["freeFloat"] ?: 0.0,
                tagAlong = numericFallback["tagAlong"] ?: 0.0
            )
            val enriched = enrichAssetDetails(result)
            putInCache(cacheKey, enriched, 5) // Cache for 5 mins
            return enriched

        } catch (e: Exception) {
            Log.e("B3NetworkService", "Error fetching stock: $ticker", e)
            return null
        }
    }

    /**
     * Fetch historical chart points for graphing
     */
    fun fetchHistoricalChart(ticker: String, range: String = "1y"): List<ChartPoint> {
        val symbol = getTickerWithSuffix(ticker)
        val interval = when (range) {
            "1d" -> "5m"
            "5d" -> "15m"
            "1mo" -> "1d"
            "6mo" -> "1wk"
            "1y" -> "1wk"
            else -> "1mo"
        }
        
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=$range&interval=$interval&includePrePost=false"
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", getRandomAgent())
            .build()

        val list = mutableListOf<ChartPoint>()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val json = JSONObject(bodyString)
                    val chart = json.optJSONObject("chart")
                    val resultList = chart?.optJSONArray("result")
                    val result = resultList?.optJSONObject(0)
                    if (result != null) {
                        val timestamps = result.optJSONArray("timestamp")
                        val indicators = result.optJSONObject("indicators")
                        val quote = indicators?.optJSONArray("quote")?.optJSONObject(0)
                        val closeValues = quote?.optJSONArray("close")

                        if (timestamps != null && closeValues != null) {
                            val sdf = SimpleDateFormat(if (range == "1d" || range == "5d") "HH:mm" else "dd/MM", Locale.getDefault())

                            for (i in 0 until timestamps.length()) {
                                if (i < closeValues.length() && !closeValues.isNull(i)) {
                                    val ts = timestamps.optLong(i)
                                    val closeVal = closeValues.optDouble(i)
                                    if (!closeVal.isNaN() && !closeVal.isInfinite()) {
                                        val dateLabel = sdf.format(Date(ts * 1000))
                                        list.add(ChartPoint(ts, dateLabel, closeVal))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("B3NetworkService", "Error historical: $ticker", e)
        }
        
        // Return actual list, don't generate mock data
        return list
    }

    /**
     * Fetch news RSS search for Brazilian stocks
     */
    fun fetchNews(ticker: String = ""): List<NewsItem> {
        val query = if (ticker.isNotEmpty()) {
            "${ticker.trim().uppercase()}+ação+OR+fii+OR+B3+OR+investimento"
        } else {
            "ações+fii+B3+dividendos+investimentos+economia"
        }
        
        val url = "https://news.google.com/rss/search?q=$query&hl=pt-BR&gl=BR&ceid=BR:pt-419"
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", getRandomAgent())
            .build()

        val list = mutableListOf<NewsItem>()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val xmlString = response.body?.string() ?: ""
                    
                    // RSS parsing via regex
                    val itemPattern = Pattern.compile("<item>([\\s\\S]*?)</item>")
                    val itemMatcher = itemPattern.matcher(xmlString)
                    
                    val titlePattern = Pattern.compile("<title>(?:<!\\[CDATA\\[)?([\\s\\S]*?)(?:\\]\\]>)?</title>")
                    val linkPattern = Pattern.compile("<link>(?:<!\\[CDATA\\[)?([\\s\\S]*?)(?:\\]\\]>)?</link>")
                    val pubDatePattern = Pattern.compile("<pubDate>(?:<!\\[CDATA\\[)?([\\s\\S]*?)(?:\\]\\]>)?</pubDate>")
                    val sourcePattern = Pattern.compile("<source[^>]*>(?:<!\\[CDATA\\[)?([\\s\\S]*?)(?:\\]\\]>)?</source>")
                    val descPattern = Pattern.compile("<description>(?:<!\\[CDATA\\[)?([\\s\\S]*?)(?:\\]\\]>)?</description>")

                    var count = 0
                    while (itemMatcher.find() && count < 20) {
                        val itemXml = itemMatcher.group(1) ?: ""
                        
                        val tMatcher = titlePattern.matcher(itemXml)
                        val lMatcher = linkPattern.matcher(itemXml)
                        val pMatcher = pubDatePattern.matcher(itemXml)
                        val sMatcher = sourcePattern.matcher(itemXml)
                        val dMatcher = descPattern.matcher(itemXml)

                        if (tMatcher.find() && lMatcher.find()) {
                            var title = tMatcher.group(1) ?: ""
                            val link = lMatcher.group(1) ?: ""
                            var pubDate = if (pMatcher.find()) pMatcher.group(1) ?: "" else ""
                            val source = if (sMatcher.find()) sMatcher.group(1) ?: "" else ""
                            var description = if (dMatcher.find()) dMatcher.group(1) ?: "" else ""

                            // Unescape basic XML symbols
                            title = title.replace("&amp;", "&")
                                .replace("&quot;", "\"")
                                .replace("&apos;", "'")
                                .replace("&lt;", "<")
                                .replace("&gt;", ">")

                            description = description.replace("&amp;", "&")
                                .replace("&quot;", "\"")
                                .replace("&apos;", "'")
                                .replace("&lt;", "<")
                                .replace("&gt;", ">")
                                .replace(Regex("<[^>]*>"), "") // Remove HTML tags
                                .trim()

                            if (description.length > 150) {
                                description = description.take(147) + "..."
                            }
                            
                            // Remove duplicate title from description
                            if (description.lowercase().startsWith(title.lowercase().take(10))) {
                                description = ""
                            }

                            // Format date nicely (usually "EEE, d MMM yyyy HH:mm:ss z")
                            var timestamp = 0L
                            try {
                                val inputFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US)
                                val date = inputFormat.parse(pubDate)
                                if (date != null) {
                                    timestamp = date.time
                                    pubDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
                                }
                            } catch (e: Exception) {
                                // Default format fallback
                                pubDate = pubDate.substringBefore(" :").replace(" GMT", "")
                            }

                            // Trim Google News suffix if present " - Fonte"
                            if (title.contains(" - ")) {
                                title = title.substringBeforeLast(" - ")
                            }

                            list.add(NewsItem(title, link, pubDate, source, description, timestamp))
                            count++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("B3NetworkService", "Error RSS news", e)
        }

        return list.sortedByDescending { it.timestamp }
    }

    private fun enrichAssetDetails(base: B3AssetData): B3AssetData {
        val t = base.ticker.trim().uppercase()
        val isFii = base.isFii
        
        var debtEbitda = 0.0
        var payout = 0.0
        var cagrRevenue5y = 0.0
        var grossMargin = 0.0
        var fiiVacancy = 0.0
        var fiiPropertyCount = 0
        var fiiSegment = ""
        var assetDescription = ""
        var subSector = ""
        var name = base.name

        // Define local mocks/defaults ONLY if base value is zero/empty
        if (isFii) {
            when (t) {
                "MXRF11" -> {
                    if (name.isEmpty() || name == t) name = "Maxi Renda FII"
                    fiiSegment = "Papel (Recebíveis Imobiliários)"
                    assetDescription = "Maior fundo imobiliário brasileiro do mercado em quantidade de cotistas. Foco principal em investir em CRIs, gerando dividendos mensais previsíveis."
                    subSector = "Títulos e Valores Imobiliários"
                }
                "HGLG11" -> {
                    if (name.isEmpty() || name == t) name = "CSHG Logística FII"
                    fiiSegment = "Lajes e Galpões Logísticos (Tijolo)"
                    fiiPropertyCount = 21
                    assetDescription = "Um dos fundos de logística mais tradicionais do mercado. Investe em galpões industriais e centros logísticos classe A."
                    subSector = "Imóveis Industriais e Logísticos"
                }
                "VISC11" -> {
                    if (name.isEmpty() || name == t) name = "Vinci Shopping Centers FII"
                    fiiSegment = "Shoppings (Tijolo)"
                    fiiPropertyCount = 20
                    assetDescription = "Destina-se ao investimento em shopping centers maduros e consolidados em várias regiões do Brasil."
                    subSector = "Varejo e Lazer"
                }
                else -> {
                    fiiSegment = "Fundo Imobiliário"
                    assetDescription = "Fundo Imobiliário listado na B3."
                }
            }
        } else {
            when (t) {
                "PETR4", "PETR3" -> {
                    if (name.isEmpty() || name == t) name = "Petrobras S.A."
                    subSector = "Petróleo, Gás e Biocombustíveis"
                    debtEbitda = 0.78
                    payout = 50.0
                    cagrRevenue5y = 11.2
                    grossMargin = 49.5
                    assetDescription = "Líder da exploração de óleo e gás no Brasil, impulsionada pelo pré-sal."
                }
                "VALE3" -> {
                    if (name.isEmpty() || name == t) name = "Vale S.A."
                    subSector = "Mineração e Siderurgia"
                    debtEbitda = 0.35
                    payout = 60.0
                    cagrRevenue5y = 6.8
                    grossMargin = 42.0
                    assetDescription = "Uma das maiores mineradoras de ferro do mundo."
                }
                "WEGE3" -> {
                    if (name.isEmpty() || name == t) name = "WEG S.A."
                    subSector = "Bens Industriais / Motores"
                    debtEbitda = -0.42
                    payout = 52.0
                    cagrRevenue5y = 15.6
                    grossMargin = 31.8
                    assetDescription = "Multinacional em motores elétricos e automação industrial."
                }
            }
        }

        // Preserve real data from base if it exists
        return base.copy(
            name = if (base.name.isNotEmpty() && base.name != t) base.name else name,
            debtEbitda = if (base.debtEbitda != 0.0) base.debtEbitda else debtEbitda,
            payout = if (base.payout != 0.0) base.payout else payout,
            cagrRevenue5y = if (base.cagrRevenue5y != 0.0) base.cagrRevenue5y else cagrRevenue5y,
            grossMargin = if (base.grossMargin != 0.0) base.grossMargin else grossMargin,
            fiiVacancy = if (base.fiiVacancy != 0.0) base.fiiVacancy else fiiVacancy,
            fiiPropertyCount = if (base.fiiPropertyCount != 0) base.fiiPropertyCount else fiiPropertyCount,
            fiiSegment = base.fiiSegment.ifBlank { fiiSegment },
            assetDescription = base.assetDescription.ifBlank { assetDescription },
            subSector = base.subSector.ifBlank { subSector }
        )
    }
}
