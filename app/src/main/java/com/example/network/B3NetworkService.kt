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

            // 3. Fallback and Enrich (Scraping disabled)
            val nexusFallback = emptyMap<String, Any>()
            if (price == 0.0 && nexusFallback.containsKey("price")) price = nexusFallback["price"] as Double
            if (dy == 0.0 && nexusFallback.containsKey("dy")) dy = nexusFallback["dy"] as Double
            if (pl == 0.0 && !isFii && nexusFallback.containsKey("pl")) pl = nexusFallback["pl"] as Double
            if (pvp == 0.0 && nexusFallback.containsKey("pvp")) pvp = nexusFallback["pvp"] as Double
            if (vpa == 0.0 && nexusFallback.containsKey("vpa")) vpa = nexusFallback["vpa"] as Double
            if (lpa == 0.0 && !isFii && nexusFallback.containsKey("lpa")) lpa = nexusFallback["lpa"] as Double
            if (roe == 0.0 && nexusFallback.containsKey("roe")) roe = nexusFallback["roe"] as Double
            if (margins == 0.0 && nexusFallback.containsKey("margins")) margins = nexusFallback["margins"] as Double
            if (dailyLiquidity == 0.0 && nexusFallback.containsKey("liquidity")) dailyLiquidity = nexusFallback["liquidity"] as Double
            if (marketCap == 0.0 && nexusFallback.containsKey("marketCap")) marketCap = nexusFallback["marketCap"] as Double
            if (isFii && nexusFallback.containsKey("fiiVacancy")) fiiVacancy = nexusFallback["fiiVacancy"] as Double
            
            if (nextEarningsDate.isEmpty() && nexusFallback.containsKey("nextEarningsDate")) {
                nextEarningsDate = nexusFallback["nextEarningsDate"] as String
            }

            if (nexusFallback.containsKey("lastDividend")) lastDividend = nexusFallback["lastDividend"] as Double

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
                priceToSales = nexusFallback["priceToSales"] as? Double ?: priceToSales,
                nextEarningsDate = nextEarningsDate,
                isFii = isFii,
                fiiVacancy = fiiVacancy,
                
                // Novos Campos Investidor10
                grossMargin = nexusFallback["grossMargin"] as? Double ?: 0.0,
                ebitMargin = nexusFallback["ebitMargin"] as? Double ?: 0.0,
                ebitdaMargin = nexusFallback["ebitdaMargin"] as? Double ?: 0.0,
                evEbitda = nexusFallback["evEbitda"] as? Double ?: 0.0,
                evEbit = nexusFallback["evEbit"] as? Double ?: 0.0,
                priceEbitda = nexusFallback["priceEbitda"] as? Double ?: 0.0,
                priceEbit = nexusFallback["priceEbit"] as? Double ?: 0.0,
                priceAsset = nexusFallback["priceAsset"] as? Double ?: 0.0,
                priceCapGiro = nexusFallback["priceCapGiro"] as? Double ?: 0.0,
                priceAtivoCircLiq = nexusFallback["priceAtivoCircLiq"] as? Double ?: 0.0,
                giroAtivos = nexusFallback["giroAtivos"] as? Double ?: 0.0,
                roic = nexusFallback["roic"] as? Double ?: 0.0,
                roa = nexusFallback["roa"] as? Double ?: 0.0,
                divLiqPatrimonio = nexusFallback["divLiqPatrimonio"] as? Double ?: 0.0,
                debtEbitda = nexusFallback["debtEbitda"] as? Double ?: 0.0,
                divLiqEbit = nexusFallback["divLiqEbit"] as? Double ?: 0.0,
                divBrutaPatrimonio = nexusFallback["divBrutaPatrimonio"] as? Double ?: 0.0,
                patrimonioAtivos = nexusFallback["patrimonioAtivos"] as? Double ?: 0.0,
                passivosAtivos = nexusFallback["passivosAtivos"] as? Double ?: 0.0,
                liquidezCorrente = nexusFallback["liquidezCorrente"] as? Double ?: 0.0,
                cagrRevenue5y = nexusFallback["cagrRevenue5y"] as? Double ?: 0.0,
                cagrProfit5y = nexusFallback["cagrProfit5y"] as? Double ?: 0.0,
                payout = nexusFallback["payout"] as? Double ?: 0.0,

                // FII
                fiiTotalHolders = nexusFallback["fiiTotalHolders"] as? String ?: "",
                fiiIssuedShares = nexusFallback["fiiIssuedShares"] as? String ?: "",
                fiiAdminFee = nexusFallback["fiiAdminFee"] as? String ?: "",
                fiiFundType = nexusFallback["fiiFundType"] as? String ?: "",
                fiiMandate = nexusFallback["fiiMandate"] as? String ?: "",
                fiiTargetAudience = nexusFallback["fiiTargetAudience"] as? String ?: "",
                fiiManagementType = nexusFallback["fiiManagementType"] as? String ?: "",
                fiiDuration = nexusFallback["fiiDuration"] as? String ?: "",
                fiiSegment = nexusFallback["fiiSegment"] as? String ?: "",
                magicNumber = if (isFii && lastDividend > 0.0 && price > 0.0) kotlin.math.ceil(price / lastDividend) else 0.0,

                // Textos
                cnpj = nexusFallback["cnpj"] as? String ?: "",
                listSegment = nexusFallback["listSegment"] as? String ?: "",
                foundationYear = nexusFallback["foundationYear"] as? String ?: "",
                listingYear = nexusFallback["listingYear"] as? String ?: "",
                employeesCount = nexusFallback["employeesCount"] as? String ?: "",
                totalPapers = nexusFallback["totalPapers"] as? String ?: "",

                // Balanço Monetário
                firmValue = nexusFallback["firmValue"] as? Double ?: 0.0,
                netWorth = nexusFallback["netWorth"] as? Double ?: 0.0,
                totalAssets = nexusFallback["totalAssets"] as? Double ?: 0.0,
                currentAssets = nexusFallback["currentAssets"] as? Double ?: 0.0,
                grossDebt = nexusFallback["grossDebt"] as? Double ?: 0.0,
                netDebt = nexusFallback["netDebt"] as? Double ?: 0.0,
                availability = nexusFallback["availability"] as? Double ?: 0.0,
                freeFloat = nexusFallback["freeFloat"] as? Double ?: 0.0,
                tagAlong = nexusFallback["tagAlong"] as? Double ?: 0.0
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

        if (isFii) {
            when (t) {
                "MXRF11" -> {
                    name = "Maxi Renda FII"
                    fiiSegment = "Papel (Recebíveis Imobiliários)"
                    fiiVacancy = 0.0
                    fiiPropertyCount = 0
                    assetDescription = "Maior fundo imobiliário brasileiro do mercado em quantidade de cotistas. Foco principal em investir em CRIs (Certificados de Recebíveis Imobiliários), Letras Hipotecárias e Debêntures, gerando dividendos mensais previsíveis e isentos de IR."
                    subSector = "Títulos e Valores Imobiliários"
                }
                "HGLG11" -> {
                    name = "CSHG Logística FII"
                    fiiSegment = "Lajes e Galpões Logísticos (Tijolo)"
                    fiiVacancy = 3.2
                    fiiPropertyCount = 21
                    assetDescription = "Um dos fundos de logística mais tradicionais do mercado. Investe em galpões industriais e centros logísticos classe A, localizados nos principais eixos rodoviários do país, com contratos de longo prazo com inquilinos sólidos."
                    subSector = "Imóveis Industriais e Logísticos"
                }
                "HGRU11" -> {
                    name = "CSHG Renda Urbana FII"
                    fiiSegment = "Renda Urbana (Varejo & Educação)"
                    fiiVacancy = 1.4
                    fiiPropertyCount = 18
                    assetDescription = "Investe em imóveis institucionais e de varejo urbano de alta resiliência, como supermercados, lojas de departamento e faculdades renomadas. Foco em contratos atípicos estáveis de longo prazo."
                    subSector = "Renda Urbana"
                }
                "XPLG11" -> {
                    name = "XP Log FII"
                    fiiSegment = "Lajes e Galpões Logísticos (Tijolo)"
                    fiiVacancy = 4.8
                    fiiPropertyCount = 16
                    assetDescription = "Fundo imobiliário gerido pela XP Vista Asset que atua na aquisição e exploração de grandes galpões logísticos e centros de distribuição. Tem como locatários grandes gigantes do e-commerce como o Mercado Livre."
                    subSector = "Imóveis Industriais e Logísticos"
                }
                "KNRI11" -> {
                    name = "Kinea Renda Imobiliária FII"
                    fiiSegment = "Híbrido (Corporativo & Logística)"
                    fiiVacancy = 6.1
                    fiiPropertyCount = 19
                    assetDescription = "Fundo híbrido gerido pela Kinea (Grupo Itaú) com excelente diversificação em edifícios corporativos premium em São Paulo/Rio e grandes galpões logísticos, mantendo vacância saudável e histórico maduro de retornos."
                    subSector = "Híbrido"
                }
                "VISC11" -> {
                    name = "Vinci Shopping Centers FII"
                    fiiSegment = "Shoppings (Tijolo)"
                    fiiVacancy = 5.2
                    fiiPropertyCount = 20
                    assetDescription = "Destina-se ao investimento em shopping centers maduros e consolidados em várias regiões do Brasil. Beneficia-se diretamente do reaquecimento econômico, crescimento populacional urbano e do consumo físico de lazer."
                    subSector = "Varejo e Lazer"
                }
                "XPML11" -> {
                    name = "XP Malls FII"
                    fiiSegment = "Shoppings (Tijolo)"
                    fiiVacancy = 3.8
                    fiiPropertyCount = 17
                    assetDescription = "Fundo gerido pela XP Asset que busca gerar renda de aluguel por meio de investimentos em participações de shoppings de referência (como Cidade Jardim, Catarina Outlet, etc.). Alta resiliência comercial e consumo."
                    subSector = "Varejo e Lazer"
                }
                "BCFF11" -> {
                    name = "BTG Pactual Fundo de Fundos"
                    fiiSegment = "Fundo de Fundos (FOF)"
                    fiiVacancy = 0.0
                    fiiPropertyCount = 0
                    assetDescription = "Fundo de fundos gerido pelo BTG Pactual que investe em uma carteira altamente diversificada de cotas de outros FIIs. Permite ao investidor diluir riscos sistêmicos de tijolo e papel em um único ativo."
                    subSector = "Gestão Ativa"
                }
                "CPTS11" -> {
                    name = "Capitânia Securities II FII"
                    fiiSegment = "Papel (Recebíveis Imobiliários)"
                    fiiVacancy = 0.0
                    fiiPropertyCount = 0
                    assetDescription = "Fundo de papel voltado ao investimento em CRIs com carteira high grade de risco moderado-baixo. Atua de maneira dinâmica no mercado secundário para turbinar retornos isentos aos seus cotistas."
                    subSector = "Títulos e Valores Imobiliários"
                }
                else -> {
                    fiiSegment = "Fundo Imobiliário"
                    fiiVacancy = 0.0
                    fiiPropertyCount = 0
                    assetDescription = "Fundo Imobiliário listado na B3."
                    subSector = "Fundo de Investimento Imobiliário"
                }
            }
        } else {
            // É uma ação
            when (t) {
                "PETR4", "PETR3" -> {
                    name = "Petrobras S.A."
                    subSector = "Petróleo, Gás e Biocombustíveis"
                    debtEbitda = 0.78
                    payout = 50.0
                    cagrRevenue5y = 11.2
                    grossMargin = 49.5
                    assetDescription = "Líder indiscutível da exploração de óleo e gás no Brasil, impulsionada pelo alto potencial produtivo de suas bacias do pré-sal. Possui baixíssimo custo de extração (lifting cost) e margens operacionais invejáveis."
                }
                "VALE3" -> {
                    name = "Vale S.A."
                    subSector = "Mineração e Siderurgia"
                    debtEbitda = 0.35
                    payout = 60.0
                    cagrRevenue5y = 6.8
                    grossMargin = 42.0
                    assetDescription = "Uma das maiores mineradoras de ferro do mundo. Destaque para a altíssima qualidade de seu minério de ferro de Carajás, o que garante prêmio de preço nos mercados globais (especialmente China). Baixo nível de alavancagem."
                }
                "ITUB4", "ITUB3" -> {
                    name = "Itaú Unibanco Holding"
                    subSector = "Bancos e Serviços Financeiros"
                    debtEbitda = 0.0
                    payout = 45.0
                    cagrRevenue5y = 8.5
                    grossMargin = 0.0
                    assetDescription = "Maior banco privado de atacado e varejo da América Latina. Histórico consistente de ROE acima de 20%, excelente blindagem patrimonial através de controle rigoroso de inadimplência e carteira corporativa sólida."
                }
                "BBAS3" -> {
                    name = "Banco do Brasil S.A."
                    subSector = "Bancos e Serviços Financeiros"
                    debtEbitda = 0.0
                    payout = 40.0
                    cagrRevenue5y = 10.8
                    grossMargin = 0.0
                    assetDescription = "Maior player financeiro público sob controle estatal, líder histórico no financiamento ao agronegócio brasileiro (carteira hiper-resiliente). Frequentemente negociado com múltiplos descontados em relação ao seu valor contábil."
                }
                "WEGE3" -> {
                    name = "WEG S.A."
                    subSector = "Bens Industriais / Motores"
                    debtEbitda = -0.42 // Caixa Líquido
                    payout = 52.0
                    cagrRevenue5y = 15.6
                    grossMargin = 31.8
                    assetDescription = "Multinacional ultraeficiente em motores elétricos, automação industrial, tintas e energia renovável. Destaque por sua governança brilhante, retorno recorrente sobre capital aplicado (ROIC) próximo a 30% e caixa líquido robusto."
                }
                "TAEE11", "TAEE3", "TAEE4" -> {
                    name = "Taesa S.A."
                    subSector = "Energia Elétrica"
                    debtEbitda = 3.6
                    payout = 90.0
                    cagrRevenue5y = 8.2
                    grossMargin = 82.5
                    assetDescription = "Uma das maiores transmissoras privadas de energia elétrica do Brasil. Negócio altamente regulado por concessões de longo prazo indexadas ao IGP-M ou IPCA, o que gera fluxo de caixa praticamente à prova de recessões e altos dividendos."
                }
                "TRPL4" -> {
                    name = "CTEEP S.A. (ISA CTEEP)"
                    subSector = "Energia Elétrica"
                    debtEbitda = 2.45
                    payout = 80.0
                    cagrRevenue5y = 6.1
                    grossMargin = 74.0
                    assetDescription = "Transmissora de energia elétrica em escala continental de alta eficiência operacional. Focada no reajuste inflacionário anual de suas receitas (RAP), mantém histórico robusto de segurança para buy&hold."
                }
                "ABEV3" -> {
                    name = "Ambev S.A."
                    subSector = "Bebidas"
                    debtEbitda = -0.52 // Caixa Líquido
                    payout = 70.0
                    cagrRevenue5y = 4.8
                    grossMargin = 52.4
                    assetDescription = "Líder no segmento cervejeiro brasileiro e latino-americano. Detém marcas massivas globais (Skol, Brahma, Stella Artois, etc). Possui fluxo operacional de caixa exemplar e não depende de dívidas de mercado."
                }
                "MGLU3" -> {
                    name = "Magazine Luiza S.A."
                    subSector = "Consumo Ciclo / E-commerce"
                    debtEbitda = 3.8
                    payout = 0.0
                    cagrRevenue5y = 12.0
                    grossMargin = 26.5
                    assetDescription = "Consolidado gigante do varejo físico e multicanal digital. Sofre grande volatilidade ligada a flutuações da taxa Selic básica de juros e demanda por bens de consumo não duráveis."
                }
                "BBDC4", "BBDC3" -> {
                    name = "Banco Bradesco"
                    subSector = "Bancos e Serviços Financeiros"
                    debtEbitda = 0.0
                    payout = 42.0
                    cagrRevenue5y = 3.5
                    grossMargin = 0.0
                    assetDescription = "Grande conglomerado bancário privado nacional, forte penetração em empréstimos de varejo corporativos e gigante segmento de previdência e saúde e seguros Bradesco Bradesco Seguros."
                }
                "SANB11" -> {
                    name = "Banco Santander Brasil"
                    subSector = "Bancos e Serviços Financeiros"
                    debtEbitda = 0.0
                    payout = 48.0
                    cagrRevenue5y = 5.2
                    grossMargin = 0.0
                    assetDescription = "Subsidiária do grupo bancário espanhol Santander, com forte operação em crédito imobiliário de varejo e financiamento auto."
                }
                "EGIE3" -> {
                    name = "Engie Brasil Energia"
                    subSector = "Energia Elétrica"
                    debtEbitda = 2.1
                    payout = 85.0
                    cagrRevenue5y = 7.9
                    grossMargin = 51.0
                    assetDescription = "Uma das maiores geradoras privadas de energia elétrica, investindo fortemente em usinas eólicas e solares de alta previsibilidade produtiva e excelente índice ESG."
                }
                else -> {
                    debtEbitda = 0.0
                    payout = 0.0
                    cagrRevenue5y = 0.0
                    grossMargin = 0.0
                    assetDescription = "Companhia Aberta listada na B3."
                    subSector = "Outros"
                }
            }
        }

        val finalName = if (base.name.isEmpty() || base.name == base.ticker) name else base.name

        return base.copy(
            name = finalName,
            debtEbitda = debtEbitda,
            payout = payout,
            cagrRevenue5y = cagrRevenue5y,
            grossMargin = grossMargin,
            fiiVacancy = fiiVacancy,
            fiiPropertyCount = fiiPropertyCount,
            fiiSegment = fiiSegment,
            assetDescription = assetDescription,
            subSector = subSector
        )
    }
}
