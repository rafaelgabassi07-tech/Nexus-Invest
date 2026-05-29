package com.example.network

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
    val source: String = "Valorae Proxy",
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

data class PortfolioProxyPosition(
    val ticker: String,
    val quantity: Double,
    val averagePrice: Double,
    val type: String = "ACAO",
    val currentPrice: Double = 0.0,
    val totalInvested: Double = 0.0
)

data class PortfolioHistoryPoint(
    val timestamp: Long,
    val dateLabel: String,
    val totalValue: Double,
    val investedValue: Double = 0.0,
    val returnPercent: Double = 0.0,
    val source: String = "Valorae Proxy"
)

data class IpcaPoint(
    val timestamp: Long,
    val dateLabel: String,
    val accumulatedPercent: Double,
    val monthlyPercent: Double = 0.0,
    val source: String = "Valorae Proxy"
)

data class DividendEvent(
    val ticker: String,
    val dateCom: String = "",
    val paymentDate: String = "",
    val valuePerShare: Double = 0.0,
    val quantity: Double = 0.0,
    val estimatedAmount: Double = 0.0,
    val status: String = "Previsto",
    val source: String = "Valorae Proxy"
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
    val allocationByClass: List<Pair<String, Double>> = emptyList(),
    val allocationBySector: List<Pair<String, Double>> = emptyList(),
    val warnings: List<String> = emptyList(),
    val source: String = "Valorae Proxy"
)

object B3NetworkService {
    private val httpDispatcher = OkHttpDispatcher().apply {
        maxRequests = 12
        maxRequestsPerHost = 8
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .callTimeout(24, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .dispatcher(httpDispatcher)
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .build()

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
            envString { BuildConfig.VALORAE_PROXY_BASE_URL }
        )
        val selected = candidates.firstOrNull { isUsableProxyUrl(it) }?.trim()?.trimEnd('/')
        return if (!selected.isNullOrBlank()) selected else "https://servidor-valorae.vercel.app"
    }

    private fun isUsableProxyUrl(raw: String): Boolean {
        val value = raw.trim().trimEnd('/')
        if (value.isBlank()) return false
        if (!value.startsWith("https://") && !value.startsWith("http://")) return false
        val lower = value.lowercase(Locale.ROOT)
        return !lower.contains("your-backend") &&
            !lower.contains("seu-dominio") &&
            !lower.contains("seu-valorae") &&
            !lower.contains("your-valorae") &&
            !lower.contains("example.com") &&
            !lower.contains("localhost") &&
            // Host legado que causava telas vazias. Mesmo que venha do Studio/env,
            // deve ser ignorado para preservar o contrato atual do Valorae Proxy.
            !lower.contains("valorae-proxy.vercel.app")
    }

    private fun directFallbackEnabled(): Boolean {
        // Regra do app: não fazer scraping/chamadas diretas como fallback no Android.
        // Todo dado externo deve vir pelo Valorae Proxy oficial para manter CORS,
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
            .addHeader("X-Valorae-Client-Id", proxyClientId())
            .addHeader("X-Valorae-Client-Version", BuildConfig.VERSION_NAME.ifBlank { "21.5.13" })
            .addHeader("X-Valorae-Environment", "production")
    }

    private fun getProxyJson(path: String, params: Map<String, String?> = emptyMap()): JSONObject? {
        val url = proxyUrl(path, params) ?: return null
        return try {
            client.newCall(proxyRequest(url).get().build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    Log.w("B3NetworkService", "Valorae Proxy GET response body is empty: $path (HTTP ${response.code})")
                    return null
                }
                val json = try { JSONObject(body) } catch (_: Exception) { null }
                if (!response.isSuccessful) {
                    val errMsg = json?.optString("message") ?: json?.optString("error") ?: "HTTP ${response.code}"
                    Log.w("B3NetworkService", "Valorae Proxy GET failed: $path, errorMsg: $errMsg")
                    return null
                }
                if (json?.optString("status", "")?.equals("ERROR", ignoreCase = true) == true || json?.has("error") == true) {
                    val errMsg = json.optString("message", json.optString("error", "payload error"))
                    Log.w("B3NetworkService", "Valorae Proxy GET payload error: $path, errorMsg: $errMsg")
                    return null
                }
                json
            }
        } catch (e: Exception) {
            Log.w("B3NetworkService", "Valorae Proxy GET exception: $path", e)
            null
        }
    }

    private fun postProxyJson(path: String, payload: JSONObject): JSONObject? {
        val url = proxyUrl(path) ?: return null
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        return try {
            client.newCall(
                proxyRequest(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
            ).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (raw.isBlank()) {
                    Log.w("B3NetworkService", "Valorae Proxy POST response body is empty: $path (HTTP ${response.code})")
                    return null
                }
                val json = try { JSONObject(raw) } catch (_: Exception) { null }
                if (!response.isSuccessful) {
                    val errMsg = json?.optString("message") ?: json?.optString("error") ?: "HTTP ${response.code}"
                    Log.w("B3NetworkService", "Valorae Proxy POST failed: $path, errorMsg: $errMsg")
                    return null
                }
                if (json?.optString("status", "")?.equals("ERROR", ignoreCase = true) == true || json?.has("error") == true) {
                    val errMsg = json.optString("message", json.optString("error", "payload error"))
                    Log.w("B3NetworkService", "Valorae Proxy POST payload error: $path, errorMsg: $errMsg")
                    return null
                }
                json
            }
        } catch (e: Exception) {
            Log.w("B3NetworkService", "Valorae Proxy POST exception: $path", e)
            null
        }
    }

    private fun unwrapValoraePayload(json: JSONObject?): JSONObject? {
        if (json == null) return null
        return json.optJSONObject("data") ?: json
    }

    private fun JSONArray.optJsonObjectOrNull(index: Int): JSONObject? = try {
        optJSONObject(index)
    } catch (_: Exception) { null }

    private fun JSONObject.optObject(path: String): JSONObject? {
        var current: JSONObject? = this
        for (part in path.split('.')) {
            current = current?.optJSONObject(part) ?: return null
        }
        return current
    }

    private fun JSONObject.optArray(path: String): JSONArray? {
        val parts = path.split('.')
        if (parts.isEmpty()) return null
        var current: JSONObject? = this
        for (part in parts.dropLast(1)) {
            current = current?.optJSONObject(part) ?: return null
        }
        return current?.optJSONArray(parts.last())
    }

    private fun JSONObject.optAny(key: String): Any? {
        return if (has(key) && !isNull(key)) opt(key) else null
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
            lastComma >= 0 -> s.replace('.', '\u0000').replace(',', '.').replace("\u0000", "")
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
                "/api/compare",
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
        val years = source.keys()
        while (years.hasNext()) {
            val yr = years.next()
            val list = mutableListOf<AssetBreakdownPoint>()
            val arr = source.optJSONArray(yr)
            if (arr != null) {
                for (item in arr.toJsonObjectList()) {
                    val name = firstText(item.optAny("name"), item.optAny("label"), item.optAny("region"), item.optAny("regiao"), item.optAny("business"), item.optAny("negocio"), item.optAny("segment"), item.optAny("segmento"))
                    val value = firstNumber(item.optAny("valuePercent"), item.optAny("percent"), item.optAny("percentage"), item.optAny("share"), item.optAny("value"), item.optAny("valor"))
                    val display = firstText(item.optAny("displayValue"), item.optAny("display"), item.optAny("revenue"), item.optAny("faturamento"))
                    if (name.isNotBlank()) list.add(AssetBreakdownPoint(name, value, display, yr))
                }
            } else {
                val obj = source.optJSONObject(yr)
                if (obj != null) {
                    val names = obj.keys()
                    while (names.hasNext()) {
                        val name = names.next()
                        val nested = obj.optJSONObject(name)
                        val value = if (nested != null) {
                            firstNumber(nested.optAny("valuePercent"), nested.optAny("percent"), nested.optAny("percentage"), nested.optAny("share"), nested.optAny("value"), nested.optAny("valor"))
                        } else {
                            firstNumber(obj.optAny(name))
                        }
                        val display = firstText(nested?.optAny("displayValue"), nested?.optAny("display"), nested?.optAny("revenue"), nested?.optAny("faturamento"))
                        if (name.isNotBlank()) list.add(AssetBreakdownPoint(name, value, display, yr))
                    }
                }
            }
            if (list.isNotEmpty()) out[yr] = list
        }
        return out
    }

    private fun appendDividendEventsFromArray(ticker: String, arr: JSONArray?, out: MutableList<DividendEvent>) {
        if (arr == null) return
        for (ev in arr.toJsonObjectList()) {
            val type = firstText(ev.optAny("tipo"), ev.optAny("type"), ev.optAny("kind"), ev.optAny("provento"), "Dividendo")
            val datacom = normalizeDisplayDate(firstText(ev.optAny("dataCom"), ev.optAny("dateCom"), ev.optAny("comDate"), ev.optAny("date_with"), ev.optAny("dataBase")))
            val payment = normalizeDisplayDate(firstText(ev.optAny("dataPagamento"), ev.optAny("paymentDate"), ev.optAny("payDate"), ev.optAny("pagamento"), ev.optAny("date_payment")))
            val value = firstNumber(ev.optAny("valor"), ev.optAny("value"), ev.optAny("amount"), ev.optAny("valuePerShare"), ev.optAny("valorPorCota"), ev.optAny("rendimento"), ev.optAny("ultimoRendimento"))
            if (value > 0.0 || datacom.isNotBlank() || payment.isNotBlank()) {
                out.add(
                    DividendEvent(
                        ticker = ticker,
                        dateCom = datacom,
                        paymentDate = payment,
                        valuePerShare = value,
                        status = type.ifBlank { "Pago" },
                        source = "Valorae Proxy / Investidor10"
                    )
                )
            }
        }
    }


    private fun fetchAssetDividendEvents(ticker: String): List<DividendEvent> {
        val clean = ticker.trim().uppercase(Locale.ROOT)
        if (clean.isBlank()) return emptyList()
        val cacheKey = "asset_dividend_events_$clean"
        getFromCache<List<DividendEvent>>(cacheKey)?.let { return it }
        val json = getProxyJson("/api/asset/dividends", mapOf("ticker" to clean, "limit" to "120"))
        val root = unwrapValoraePayload(json) ?: return emptyList()
        val out = mutableListOf<DividendEvent>()
        appendDividendEventsFromArray(
            clean,
            firstArray(
                root.optJSONArray("events"),
                root.optJSONArray("items"),
                root.optJSONArray("dividends"),
                root.optJSONArray("dividendos"),
                root.optJSONArray("historicoDividendos"),
                root.optJSONArray("proventos"),
                root.optJSONArray("income"),
                root.optArray("data.events"),
                root.optArray("data.items"),
                root.optArray("data.dividends"),
                root.optArray("data.dividendos"),
                root.optArray("data.proventos")
            ),
            out
        )
        val distinct = out.distinctBy { listOf(it.ticker, it.dateCom, it.paymentDate, it.valuePerShare).joinToString("|") }
            .sortedByDescending { parseFlexibleDateMillis(it.paymentDate.ifBlank { it.dateCom }) }
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

    private fun mapProxyAsset(payload: JSONObject?): B3AssetData? {
        val root = unwrapValoraePayload(payload) ?: return null
        val ticker = firstText(root.optAny("ticker"), root.optString("symbol", "")).uppercase(Locale.ROOT)
        if (ticker.isBlank()) return null
        val results = root.optJSONObject("results") ?: root
        val sections = firstObject(results.optJSONObject("sections"), root.optJSONObject("sections"))
        val normalized = mergedObject(
            root.optJSONObject("normalized"),
            results.optJSONObject("normalized"),
            root.optObject("data.normalized"),
            results.optObject("data.normalized")
        )
        val cotacao = firstObject(results.optJSONObject("cotacao"), root.optJSONObject("cotacao"))
        val indicadores = mergedObject(
            results.optJSONObject("indicadores"),
            sections?.optJSONObject("indicadores"),
            results.optObject("indicadoresFundamentalistas.semComparativos"),
            results.optObject("indicadoresFundamentalistas.comComparativos"),
            results.optObject("indicadoresFundamentalistas.comparativos"),
            root.optJSONObject("indicadores")
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
            root.optArray("data.dividendos.historico"),
            root.optArray("data.dividends.history")
        )
        val firstDividend = history?.optJsonObjectOrNull(0)
        val type = firstText(root.optAny("type"), results.optAny("tipo"))
        val isFii = type.equals("FII", ignoreCase = true) || inferIsFii(ticker)
        val price = firstNumber(
            normalizedValue(normalized, "precoAtual"),
            cotacao?.optAny("precoAtual"),
            results.optAny("precoAtual"),
            root.optAny("precoAtual")
        )
        val dy = firstNumber(
            normalizedValue(normalized, "dividendYield"),
            indicadores?.optAny("dividendYield"),
            ratiosChave?.optAny("dividendYield"),
            dividendos?.optAny("dividendYield"),
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
            results.optAny("ultimoRendimento"),
            results.optAny("lastDividend"),
            indicadores?.optAny("ultimoRendimento")
        )
        val name = firstText(
            results.optAny("nome"),
            dadosEmpresa?.optAny("nomeCompleto"),
            infoFundo?.optAny("nome"),
            root.optAny("name"),
            ticker
        )
        val description = firstText(
            results.optAny("sobre"),
            results.optAny("descricao"),
            root.optAny("assetDescription")
        )
        val segment = firstText(
            infoFundo?.optAny("segmentoFii"),
            infoFundo?.optAny("segmento"),
            results.optAny("segmentoFii"),
            infoEmpresa?.optAny("segmento"),
            infoEmpresa?.optAny("setor")
        )
        val companySector = firstText(
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

        return B3AssetData(
            ticker = ticker,
            name = name,
            price = price,
            changePercent = firstNumber(normalizedValue(normalized, "variacaoDay"), cotacao?.optAny("variacaoDay"), results.optAny("variacaoDay")),
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
            source = "Valorae Proxy",
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
            magicNumber = if (isFii && lastDividend > 0.0 && price > 0.0) kotlin.math.ceil(price / lastDividend) else 0.0
        )
    }

    private fun fetchAssetDataFromProxy(ticker: String, bypassCache: Boolean = false): B3AssetData? {
        val clean = ticker.trim().uppercase(Locale.ROOT)
        if (clean.startsWith("^") || clean.contains("=X")) return fetchMarketIndexFromProxy(clean)
        val json = getProxyJson(
            "/api/asset",
            mapOf(
                "ticker" to clean,
                "mode" to "super",
                "view" to "full",
                "profile" to "deep",
                "includeNews" to "1",
                "nocache" to if (bypassCache) "1" else null
            )
        ) ?: getProxyJson(
            "/api/asset",
            mapOf(
                "ticker" to clean,
                "mode" to "basic",
                "view" to "compact",
                "nocache" to if (bypassCache) "1" else null
            )
        ) ?: return null
        return mapProxyAsset(json)
    }

    private fun fetchMarketIndexFromProxy(symbol: String): B3AssetData? {
        val json = getProxyJson("/api/market/indices") ?: return null
        val root = unwrapValoraePayload(json) ?: return null
        val rows = root.optJSONArray("indices") ?: return null
        val desired = when (symbol.uppercase(Locale.ROOT)) {
            "^BVSP", "IBOV", "IBOVESPA" -> "IBOV"
            "^IFIX", "IFIX" -> "IFIX_PROXY"
            "USDBRL=X", "DOLAR", "USD" -> "USDBRL=X"
            else -> symbol.uppercase(Locale.ROOT)
        }
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            val name = row.optString("name", "")
            val sym = row.optString("symbol", "")
            if (!name.equals(desired, true) && !sym.equals(desired, true) && !sym.equals(symbol, true)) continue
            val price = firstNumber(row.optAny("price"))
            if (price <= 0.0) return null
            return B3AssetData(
                ticker = when (desired) { "IBOV" -> "IBOV"; "IFIX_PROXY" -> "IFIX"; else -> desired },
                name = name.ifBlank { desired },
                price = price,
                changePercent = firstNumber(row.optAny("variationPct")),
                source = "Valorae Proxy",
                isFii = false
            )
        }
        return null
    }

    fun fetchAssetData(ticker: String, bypassCache: Boolean = false): B3AssetData? {
        val cacheKey = "asset_data_proxy_${ticker.trim().uppercase(Locale.ROOT)}"
        if (!bypassCache) getFromCache<B3AssetData>(cacheKey)?.let { return it }
        val fromProxy = fetchAssetDataFromProxy(ticker, bypassCache)
        if (fromProxy != null && (fromProxy.price > 0.0 || fromProxy.dy > 0.0 || fromProxy.pvp > 0.0 || fromProxy.assetDescription.isNotBlank())) {
            putInCache(cacheKey, fromProxy, 5)
            return fromProxy
        }
        if (directFallbackEnabled()) {
            val direct = fetchAssetDataDirect(ticker, bypassCache)
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
                .put("mode", "super")
                .put("view", "standard")
                .put("profile", "portfolio")
                .put("includeNews", false)
                .put("nocache", bypassCache)
            val json = postProxyJson("/api/assets", body)
                ?: getProxyJson(
                    "/api/assets",
                    mapOf(
                        "tickers" to toFetch.joinToString(","),
                        "mode" to "super",
                        "view" to "standard",
                        "profile" to "portfolio",
                        "includeNews" to "0",
                        "nocache" to if (bypassCache) "1" else null
                    )
                )
                ?: getProxyJson(
                    "/api/assets",
                    mapOf(
                        "tickers" to toFetch.joinToString(","),
                        "mode" to "basic",
                        "view" to "compact",
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
            fun acceptMapped(mapped: B3AssetData?) {
                if (mapped?.ticker?.isNotBlank() == true) {
                    out[mapped.ticker] = mapped
                    putInCache("asset_data_proxy_${mapped.ticker}", mapped, 5)
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
                    acceptMapped(mapProxyAsset(item))
                }
            }
        }
        val missing = cleanTickers.filter { !out.containsKey(it) }
        if (missing.isNotEmpty()) {
            for (ticker in missing) {
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
            "/api/asset/history",
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
                putInCache(cacheKey, list, 2)
                return list
            }
        }
        return if (directFallbackEnabled()) fetchHistoricalChartDirect(ticker, range) else emptyList()
    }

    fun fetchNews(ticker: String = ""): List<NewsItem> {
        val cacheKey = "news_proxy_${ticker.trim().uppercase(Locale.ROOT)}"
        getFromCache<List<NewsItem>>(cacheKey)?.let { return it }
        val queryTicker = ticker.trim().uppercase(Locale.ROOT).ifBlank { "PETR4" }
        val json = getProxyJson("/api/news", mapOf("ticker" to queryTicker, "limit" to "20"))
        val root = unwrapValoraePayload(json)
        val items = root?.optJSONArray("items")
        if (items != null && items.length() > 0) {
            val out = mutableListOf<NewsItem>()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val pubRaw = firstText(item.optAny("pubDate"), item.optAny("date"), item.optAny("publishedAt"))
                val ts = parseIsoDateMillis(pubRaw)
                val formatted = if (ts > 0L) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts)) else pubRaw
                out.add(
                    NewsItem(
                        title = firstText(item.optAny("title")),
                        link = firstText(item.optAny("link"), item.optAny("url")),
                        pubDate = formatted,
                        source = firstText(item.optAny("source"), root.optAny("source")),
                        description = firstText(item.optAny("snippet"), item.optAny("description")),
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
                    String.format(Locale.ROOT, "%.2f", p.totalInvested)
                ).joinToString(":")
            }
            .sorted()
            .joinToString("|")
    }

    fun fetchPortfolioAnalysis(positions: List<PortfolioProxyPosition>): PortfolioProxyAnalysis? {
        if (positions.isEmpty()) return null
        val cacheKey = "portfolio_analysis_${positionsCacheSignature(positions)}"
        getFromCache<PortfolioProxyAnalysis>(cacheKey)?.let { return it }
        val payload = JSONObject()
            .put("positions", positionArray(positions))
            .put("mode", "complete")
            .put("view", "standard")
            .put("includeAssets", false)
            .put("include", JSONArray(listOf("allocation", "risk", "income", "events", "quality", "warnings", "intelligence")))
        val json = postProxyJson("/api/portfolio/analyze", payload) ?: return null
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
            dataQuality = firstNumber(quality?.optAny("score"), quality?.optAny("percent"), quality?.optAny("completeness"), summary.optAny("averageQualityScore"), summary.optAny("dataQuality")),
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
            warnings = warnings.distinct().take(8),
            source = "Valorae Proxy"
        )
        putInCache(cacheKey, result, 3)
        return result
    }

    fun fetchPortfolioHistory(positions: List<PortfolioProxyPosition>, range: String = "1Y"): List<PortfolioHistoryPoint> {
        if (positions.isEmpty()) return emptyList()
        val normalizedRange = normalizeProxyRange(range)
        val cacheKey = "portfolio_history_${normalizedRange}_${positionsCacheSignature(positions)}"
        getFromCache<List<PortfolioHistoryPoint>>(cacheKey)?.let { return it }
        val payload = JSONObject()
            .put("positions", positionArray(positions))
            .put("range", normalizedRange)
            .put("limit", historyLimitForRange(normalizedRange).toIntOrNull() ?: 370)
        val json = postProxyJson("/api/portfolio/history", payload) ?: return emptyList()
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
                    source = firstText(root.optAny("source"), "Valorae Proxy")
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
        val json = getProxyJson("/api/market/ipca", mapOf("last" to safeMonths.toString(), "months" to safeMonths.toString(), "range" to "${safeMonths}M")) ?: return emptyList()
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
                        source = "Valorae Proxy"
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
            .put("limit", 50)
        val json = postProxyJson("/api/portfolio/next-dividends", payload)
            ?: getProxyJson("/api/portfolio/next-dividends", mapOf("tickers" to tickers.joinToString(","), "limit" to "50"))
            ?: return emptyList()
        val root = unwrapValoraePayload(json) ?: return emptyList()
        val items = firstArray(
            root.optJSONArray("events"),
            root.optJSONArray("items"),
            root.optJSONArray("dividends"),
            root.optJSONArray("dividendos"),
            root.optJSONArray("proventos"),
            root.optJSONArray("income"),
            root.optJSONArray("earnings"),
            root.optArray("data.dividendos"),
            root.optArray("data.dividends"),
            root.optArray("data.events"),
            root.optArray("data.items")
        ) ?: return emptyList()
        val quantityByTicker = positions.associateBy({ it.ticker.trim().uppercase(Locale.ROOT) }, { it.quantity })
        val out = mutableListOf<DividendEvent>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val nested = item.optJSONObject("nextDividend")
                ?: item.optJSONObject("upcoming")
                ?: item.optJSONObject("lastDividend")
                ?: item
            val ticker = firstText(item.optAny("ticker"), nested.optAny("ticker"), item.optAny("symbol"), nested.optAny("symbol")).uppercase(Locale.ROOT)
            if (ticker.isBlank()) continue
            val q = firstNumber(item.optAny("quantity"), nested.optAny("quantity"), nested.optAny("quantidade"), quantityByTicker[ticker])
            val value = firstNumber(
                nested.optAny("valuePerShare"),
                nested.optAny("valorPorCota"),
                nested.optAny("valor"),
                nested.optAny("value"),
                nested.optAny("amount"),
                nested.optAny("dividend")
            )
            val estimated = firstNumber(
                nested.optAny("estimatedAmount"),
                nested.optAny("valorEstimado"),
                nested.optAny("total"),
                item.optAny("estimatedAmount"),
                item.optAny("valorEstimado"),
                q * value
            )
            val isUpcoming = item.optJSONObject("nextDividend") != null || item.optJSONObject("upcoming") != null
            out.add(
                DividendEvent(
                    ticker = ticker,
                    dateCom = normalizeDisplayDate(firstText(nested.optAny("dateCom"), nested.optAny("comDate"), nested.optAny("dataCom"))),
                    paymentDate = normalizeDisplayDate(firstText(nested.optAny("paymentDate"), nested.optAny("payDate"), nested.optAny("dataPagamento"), nested.optAny("dataPagamentoPrevista"))),
                    valuePerShare = value,
                    quantity = q,
                    estimatedAmount = estimated,
                    status = firstText(nested.optAny("status"), nested.optAny("type"), nested.optAny("tipo"), if (isUpcoming) "Previsto" else "Último provento"),
                    source = firstText(root.optAny("source"), "Valorae Proxy")
                )
            )
        }
        val sorted = out.sortedWith(compareBy<DividendEvent> { parseFlexibleDateMillis(it.paymentDate).let { ts -> if (ts > 0L) ts else Long.MAX_VALUE } }.thenBy { it.ticker })
        if (sorted.isNotEmpty()) putInCache(cacheKey, sorted, 10)
        return sorted
    }

    fun checkHealth(): JSONObject? {
        return getProxyJson("/api/health")
    }

    fun checkReady(): JSONObject? {
        return getProxyJson("/api/ready")
    }

    fun fetchObservability(minutes: Int = 60): JSONObject? {
        return getProxyJson("/api/observability", mapOf("minutes" to minutes.toString()))
    }

    fun fetchFields(): JSONObject? {
        return getProxyJson("/api/fields")
    }

    fun fetchOpenApi(): JSONObject? {
        return getProxyJson("/api/openapi")
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
            "/api/asset",
            mapOf(
                "ticker" to clean,
                "profile" to "analysis",
                "view" to "analysis",
                "mode" to "super",
                "includeNews" to "0",
                "nocache" to if (bypassCache) "1" else null
            )
        )
        if (json == null) {
            json = getProxyJson(
                "/api/asset",
                mapOf(
                    "ticker" to clean,
                    "profile" to "standard",
                    "view" to "full",
                    "mode" to "basic",
                    "includeNews" to "0",
                    "nocache" to if (bypassCache) "1" else null
                )
            )
        }
        if (json == null) {
            json = getProxyJson(
                "/api/asset",
                mapOf(
                    "ticker" to clean,
                    "mode" to "basic",
                    "view" to "compact",
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
        if (bundle.profitability.isEmpty() && bundle.priceHistory.size >= 2) {
            val first = bundle.priceHistory.first().close
            val last = bundle.priceHistory.last().close
            if (first > 0.0 && last > 0.0) {
                bundle = bundle.copy(
                    profitability = listOf(
                        AssetPeriodReturn(
                            period = normalizedRange,
                            valuePercent = ((last / first) - 1.0) * 100.0,
                            label = normalizedRange,
                            kind = "nominal"
                        )
                    )
                )
            }
        }
        val needsIndexFallback = bundle.indexComparison.count { it.points.size >= 2 } < 2
        if (needsIndexFallback) {
            val fallbackComparison = fetchProxyComparisonSeries(clean, isFii, normalizedRange, priceHistory)
            if (fallbackComparison.isNotEmpty()) {
                bundle = bundle.copy(indexComparison = mergeComparisonSeries(bundle.indexComparison, fallbackComparison, clean))
            }
        }
        putInCache(cacheKey, bundle, 6)
        return bundle
    }

    internal fun parseAssetChartBundle(ticker: String, isFii: Boolean, root: JSONObject, priceHistory: List<ChartPoint>): AssetChartBundle {
        val results = root.optJSONObject("results") ?: root
        val sections = firstObject(results.optJSONObject("sections"), root.optJSONObject("sections")) ?: results
        val normalized = mergedObject(
            root.optJSONObject("normalized"),
            results.optJSONObject("normalized"),
            root.optObject("data.normalized"),
            results.optObject("data.normalized")
        )
        val financialCharts = firstObject(
            results.optJSONObject("chartsFinanceiros"),
            results.optJSONObject("financialCharts"),
            results.optJSONObject("demonstrativos"),
            sections.optJSONObject("chartsFinanceiros"),
            sections.optJSONObject("demonstrativos")
        )

        val warnings = mutableListOf<String>()
        if (root.optString("status") == "PARTIAL" || root.optBoolean("partial", false)) {
            warnings.add("Dados parciais do Proxy")
        }
        val warningsArr = results.optJSONArray("warnings") ?: root.optJSONArray("warnings")
        if (warningsArr != null) {
            for (i in 0 until warningsArr.length()) {
                val w = warningsArr.optString(i, "")
                if (w.isNotBlank()) warnings.add(w)
            }
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

        val profitability = mutableListOf<AssetPeriodReturn>()
        val realProfitability = mutableListOf<AssetPeriodReturn>()

        val rentabilidadeObj = sections.optJSONObject("rentabilidade") ?: sections.optJSONObject("rentabilidadeChart")
        val rentKeyValues = firstArray(
            rentabilidadeObj?.optJSONArray("keyValues"),
            rentabilidadeObj?.optJSONArray("profitabilities"),
            rentabilidadeObj?.optJSONArray("items")
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
            if (profitability.isEmpty()) {
                val var12m = firstNumber(normalizedValue(normalized, "variacao12m"))
                if (var12m != 0.0) {
                    profitability.add(AssetPeriodReturn(period = "12M", valuePercent = var12m, label = "Est. 12M"))
                }
            }
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

        val payoutHistObj = sections.optJSONObject("payoutHistorico") ?: financialCharts?.optJSONObject("payoutHistorico") ?: sections.optJSONObject("demonstrativos")?.optJSONObject("payoutHistorico")
        if (payoutHistObj != null) {
            val yearsArr = payoutHistObj.optJSONArray("years")
            val payoutArr = payoutHistObj.optJSONArray("payOutCompanyIndicators")
                ?: payoutHistObj.optJSONArray("payout")
                ?: payoutHistObj.optJSONArray("items")
            if (yearsArr != null && payoutArr != null) {
                val len = kotlin.math.min(yearsArr.length(), payoutArr.length())
                for (i in 0 until len) {
                    val rawPoint = payoutArr.opt(i)
                    val objPoint = rawPoint as? JSONObject
                    val yr = firstText(objPoint?.optAny("year"), objPoint?.optAny("ano"), objPoint?.optAny("label"), yearsArr.optString(i))
                    val pay = firstNumber(objPoint?.optAny("value"), objPoint?.optAny("valor"), objPoint?.optAny("payout"), rawPoint)
                    if (yr.isNotBlank() && pay != 0.0) {
                        payoutHistory.add(AssetIndicatorPoint("Payout", pay, String.format(Locale.ROOT, "%.2f%%", pay), "%", year = yr))
                    }
                }
            } else if (payoutArr != null) {
                for (i in 0 until payoutArr.length()) {
                    val objPoint = payoutArr.optJSONObject(i) ?: continue
                    val yr = firstText(objPoint.optAny("year"), objPoint.optAny("ano"), objPoint.optAny("label"))
                    val pay = firstNumber(objPoint.optAny("value"), objPoint.optAny("valor"), objPoint.optAny("payout"))
                    if (yr.isNotBlank() && pay != 0.0) {
                        payoutHistory.add(AssetIndicatorPoint("Payout", pay, String.format(Locale.ROOT, "%.2f%%", pay), "%", year = yr))
                    }
                }
            }
        }

        val indexComparison = mutableListOf<AssetComparisonSeries>()
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
        if (compIndices != null) {
            indexComparison.addAll(parseComparisonSeriesFromObject(compIndices, ticker))
        }
        if (indexComparison.isEmpty()) {
            val var12m = firstNumber(normalizedValue(normalized, "variacao12m"), results.optAny("variacao12m"))
            if (var12m != 0.0) {
                indexComparison.add(AssetComparisonSeries(ticker, listOf(AssetComparisonPoint("Base", 0.0), AssetComparisonPoint("12M", var12m))))
            }
        }

        val commodityComparison = mutableListOf<AssetComparisonSeries>()
        val compCommObj = sections.optJSONObject("comparacaoCommodity") ?: results.optJSONObject("comparacaoCommodity") ?: sections.optJSONObject("commodities")
        if (compCommObj != null) {
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
        val revProfArr = firstArray(
            financialCharts?.optJSONArray("receitasLucros"),
            financialCharts?.optJSONArray("revenueProfit"),
            financialCharts?.optJSONArray("items"),
            demObj?.optJSONArray("receitasLucros"),
            demObj?.optJSONArray("revenueProfit"),
            demObj?.optJSONArray("items"),
            sections.optJSONArray("receitasLucros"),
            results.optJSONArray("receitasLucros")
        )
        if (revProfArr != null) {
            for (i in 0 until revProfArr.length()) {
                val r = revProfArr.optJSONObject(i) ?: continue
                val yr = firstText(r.optAny("year"), r.optAny("ano"), r.optAny("date"), "")
                val qt = firstText(r.optAny("quarter"), r.optAny("trimestre"), r.optAny("period"), "")
                val rev = firstNumber(r.optAny("net_revenue"), r.optAny("netRevenue"), r.optAny("revenue"), r.optAny("receitaLiquida"), r.optAny("receita"), r.optAny("faturamento"))
                val profit = firstNumber(r.optAny("net_profit"), r.optAny("netProfit"), r.optAny("profit"), r.optAny("lucroLiquido"), r.optAny("lucro"))
                val costVal = firstNumber(r.optAny("cost"), r.optAny("custo"), r.optAny("costs"))
                val gp = firstNumber(r.optAny("gross_profit"), r.optAny("grossProfit"), r.optAny("lucroBruto"))
                val ebtda = firstNumber(r.optAny("ebitda"), r.optAny("EBITDA"))
                val ebt = firstNumber(r.optAny("ebit"), r.optAny("EBIT"))
                if (yr.isNotBlank() && (rev != 0.0 || profit != 0.0 || costVal != 0.0 || gp != 0.0 || ebtda != 0.0 || ebt != 0.0)) {
                    revenueProfit.add(
                        FinancialStatementPoint(
                            label = if (qt.isNotBlank()) "$qt/$yr" else yr,
                            year = yr,
                            quarter = qt,
                            netRevenue = rev,
                            cost = costVal,
                            grossProfit = gp,
                            ebitda = ebtda,
                            ebit = ebt,
                            netProfit = profit
                        )
                    )
                }
            }
        }

        val profitVsQuote = mutableListOf<AssetComparisonPoint>()
        val profitQuoteObj = firstObject(
            financialCharts?.optJSONObject("lucroCotacao"),
            financialCharts?.optJSONObject("profitVsQuote"),
            demObj?.optJSONObject("lucroCotacao"),
            demObj?.optJSONObject("profitVsQuote"),
            sections.optJSONObject("lucroCotacao"),
            results.optJSONObject("lucroCotacao")
        )
        if (profitQuoteObj != null) {
            val quotesArr = profitQuoteObj.optJSONArray("quotes") ?: profitQuoteObj.optJSONArray("cotacoes") ?: profitQuoteObj.optJSONArray("cotações")
            val profitsArr = profitQuoteObj.optJSONArray("profits") ?: profitQuoteObj.optJSONArray("lucros") ?: profitQuoteObj.optJSONArray("netProfit")
            if (quotesArr != null && profitsArr != null) {
                val len = kotlin.math.min(quotesArr.length(), profitsArr.length())
                for (i in 0 until len) {
                    val q = quotesArr.optJSONObject(i) ?: continue
                    val p = profitsArr.optJSONObject(i) ?: continue
                    val lbl = firstText(q.optAny("label"), q.optAny("year"), q.optAny("ano"), "P${i+1}")
                    val quote = firstNumber(q.optAny("quotation"), q.optAny("quote"), q.optAny("cotacao"), q.optAny("value"), q.optAny("valor"))
                    val profit = firstNumber(p.optAny("net_profit"), p.optAny("netProfit"), p.optAny("profit"), p.optAny("lucro"), p.optAny("value"), p.optAny("valor"))
                    if (quote != 0.0 || profit != 0.0) {
                        profitVsQuote.add(AssetComparisonPoint(label = lbl, value = quote, secondaryValue = profit))
                    }
                }
            } else {
                val years = profitQuoteObj.keys()
                while (years.hasNext()) {
                    val year = years.next()
                    val obj = profitQuoteObj.optJSONObject(year) ?: continue
                    val quote = firstNumber(obj.optAny("quotation"), obj.optAny("quote"), obj.optAny("cotacao"), obj.optAny("price"), obj.optAny("value"))
                    val profit = firstNumber(obj.optAny("net_profit"), obj.optAny("netProfit"), obj.optAny("profit"), obj.optAny("lucro"), obj.optAny("lucroLiquido"))
                    if ((quote != 0.0 || profit != 0.0) && year.isNotBlank()) {
                        profitVsQuote.add(AssetComparisonPoint(label = year, value = quote, secondaryValue = profit))
                    }
                }
            }
        }

        val equityEvolution = mutableListOf<FinancialStatementPoint>()
        val eqArr = firstArray(
            financialCharts?.optJSONArray("evolucaoPatrimonio"),
            financialCharts?.optJSONArray("equityEvolution"),
            financialCharts?.optJSONArray("balancoPatrimonial"),
            demObj?.optJSONArray("evolucaoPatrimonio"),
            demObj?.optJSONArray("equityEvolution"),
            sections.optJSONArray("evolucaoPatrimonio"),
            results.optJSONArray("evolucaoPatrimonio")
        )
        if (eqArr != null) {
            for (i in 0 until eqArr.length()) {
                val eq = eqArr.optJSONObject(i) ?: continue
                val yr = firstText(eq.optAny("year"), eq.optAny("ano"), eq.optAny("date"), "")
                val qt = firstText(eq.optAny("quarter"), eq.optAny("trimestre"), "")
                val plVal = firstNumber(eq.optAny("net_worth"), eq.optAny("netWorth"), eq.optAny("patrimonioLiquido"), eq.optAny("pl"))
                val assetsVal = firstNumber(eq.optAny("balance_total_assets"), eq.optAny("totalAssets"), eq.optAny("ativos"), eq.optAny("assets"), eq.optAny("ativoTotal"))
                val liabVal = firstNumber(eq.optAny("balance_total_liabilities"), eq.optAny("totalLiabilities"), eq.optAny("passivos"), eq.optAny("liabilities"), eq.optAny("passivoTotal"))
                if (yr.isNotBlank() && (plVal != 0.0 || assetsVal != 0.0 || liabVal != 0.0)) {
                    equityEvolution.add(
                        FinancialStatementPoint(
                            label = if (qt.isNotBlank()) "$qt/$yr" else yr,
                            year = yr,
                            quarter = qt,
                            netWorth = plVal,
                            totalAssets = assetsVal,
                            totalLiabilities = liabVal
                        )
                    )
                }
            }
        }

        val revenueByRegion = mutableMapOf<String, List<AssetBreakdownPoint>>()
        val revenueByBusiness = mutableMapOf<String, List<AssetBreakdownPoint>>()

        val regObj = firstObject(
            sections.optJSONObject("regioesReceita"),
            sections.optObject("empresa.regioesReceita"),
            results.optJSONObject("regioesReceita"),
            results.optJSONObject("geografiaReceita"),
            results.optJSONObject("revenueGeography"),
            root.optJSONObject("revenueGeography")
        )
        if (regObj != null) {
            val years = regObj.keys()
            while (years.hasNext()) {
                val yr = years.next()
                val list = mutableListOf<AssetBreakdownPoint>()
                val yrObj = regObj.optJSONObject(yr)
                if (yrObj != null) {
                    val regions = yrObj.keys()
                    while (regions.hasNext()) {
                        val rName = regions.next()
                        val rObj = yrObj.optJSONObject(rName) ?: continue
                        val valPct = firstNumber(rObj.optAny("valuePercent"), rObj.optAny("percent"), rObj.optAny("value"))
                        val disp = firstText(rObj.optAny("displayValue"), rObj.optAny("revenue"), rObj.optAny("faturamento"))
                        list.add(AssetBreakdownPoint(name = rName, valuePercent = valPct, displayValue = disp, year = yr))
                    }
                } else {
                    val yrArr = regObj.optJSONArray(yr)
                    if (yrArr != null) {
                        for (i in 0 until yrArr.length()) {
                            val r = yrArr.optJSONObject(i) ?: continue
                            val rName = firstText(r.optAny("name"), r.optAny("region"), r.optAny("regiao"))
                            val valPct = firstNumber(r.optAny("valuePercent"), r.optAny("percent"), r.optAny("value"))
                            list.add(AssetBreakdownPoint(name = rName, valuePercent = valPct, year = yr))
                        }
                    }
                }
                if (list.isNotEmpty()) revenueByRegion[yr] = list
            }
        }

        val busObj = firstObject(
            sections.optJSONObject("negociosReceita"),
            sections.optObject("empresa.negociosReceita"),
            results.optJSONObject("negociosReceita"),
            results.optJSONObject("segmentosReceita"),
            results.optJSONObject("revenueSegment"),
            root.optJSONObject("revenueSegment")
        )
        if (busObj != null) {
            val years = busObj.keys()
            while (years.hasNext()) {
                val yr = years.next()
                val list = mutableListOf<AssetBreakdownPoint>()
                val yrObj = busObj.optJSONObject(yr)
                if (yrObj != null) {
                    val businesses = yrObj.keys()
                    while (businesses.hasNext()) {
                        val bName = businesses.next()
                        val bObj = yrObj.optJSONObject(bName) ?: continue
                        val valPct = firstNumber(bObj.optAny("valuePercent"), bObj.optAny("percent"), bObj.optAny("value"))
                        list.add(AssetBreakdownPoint(name = bName, valuePercent = valPct, year = yr))
                    }
                } else {
                    val yrArr = busObj.optJSONArray(yr)
                    if (yrArr != null) {
                        for (i in 0 until yrArr.length()) {
                            val b = yrArr.optJSONObject(i) ?: continue
                            val bName = firstText(b.optAny("name"), b.optAny("business"), b.optAny("negocio"))
                            val valPct = firstNumber(b.optAny("valuePercent"), b.optAny("percent"), b.optAny("value"))
                            list.add(AssetBreakdownPoint(name = bName, valuePercent = valPct, year = yr))
                        }
                    }
                }
                if (list.isNotEmpty()) revenueByBusiness[yr] = list
            }
        }

        if (revenueByRegion.isEmpty()) {
            revenueByRegion.putAll(
                parseBreakdownMap(
                    firstObject(
                        results.optJSONObject("revenueGeography"),
                        results.optJSONObject("regioesReceita"),
                        results.optJSONObject("geografiaReceita"),
                        sections.optJSONObject("revenueGeography"),
                        sections.optJSONObject("geografiaReceita")
                    )
                )
            )
        }
        if (revenueByBusiness.isEmpty()) {
            revenueByBusiness.putAll(
                parseBreakdownMap(
                    firstObject(
                        results.optJSONObject("revenueSegment"),
                        results.optJSONObject("revenueByBusiness"),
                        results.optJSONObject("negociosReceita"),
                        results.optJSONObject("segmentosReceita"),
                        sections.optJSONObject("revenueSegment"),
                        sections.optJSONObject("segmentosReceita")
                    )
                )
            )
        }

        val fiiDistribution12m = mutableListOf<AssetIndicatorPoint>()
        val fiiPeerAverage = mutableListOf<AssetComparisonPoint>()
        val fiiPatrimonialInfo = mutableListOf<AssetIndicatorPoint>()
        val fiiAssetDistribution = mutableMapOf<String, List<AssetBreakdownPoint>>()

        if (isFii) {
            val y1m = firstNumber(normalizedValue(normalized, "yield1m"), results.optAny("yield1m"), indicadores.optAny("yield1m"))
            val y3m = firstNumber(normalizedValue(normalized, "yield3m"), results.optAny("yield3m"), indicadores.optAny("yield3m"))
            val y6m = firstNumber(normalizedValue(normalized, "yield6m"), results.optAny("yield6m"), indicadores.optAny("yield6m"))
            val y12m_val = firstNumber(normalizedValue(normalized, "yield12m"), results.optAny("yield12m"), indicadores.optAny("yield12m"), dyVal)
            if (y1m != 0.0) fiiDistribution12m.add(AssetIndicatorPoint("Yield 1M", y1m, String.format(Locale.ROOT, "%.2f%%", y1m), "%"))
            if (y3m != 0.0) fiiDistribution12m.add(AssetIndicatorPoint("Yield 3M", y3m, String.format(Locale.ROOT, "%.2f%%", y3m), "%"))
            if (y6m != 0.0) fiiDistribution12m.add(AssetIndicatorPoint("Yield 6M", y6m, String.format(Locale.ROOT, "%.2f%%", y6m), "%"))
            if (y12m_val != 0.0) fiiDistribution12m.add(AssetIndicatorPoint("Yield 12M", y12m_val, String.format(Locale.ROOT, "%.2f%%", y12m_val), "%"))
            if (fiiDistribution12m.isEmpty() && dividendMonthly.isNotEmpty()) {
                val currentPriceForYield = firstNumber(normalizedValue(normalized, "precoAtual"), results.optAny("precoAtual"), results.optObject("cotacao")?.optAny("precoAtual"))
                dividendMonthly.takeLast(12).forEach { month ->
                    val pct = if (currentPriceForYield > 0.0) (month.value / currentPriceForYield) * 100.0 else month.value
                    if (pct > 0.0 && pct.isFinite()) {
                        fiiDistribution12m.add(AssetIndicatorPoint(month.period.ifBlank { "Mês" }, pct, String.format(Locale.ROOT, "%.2f%%", pct), "%"))
                    }
                }
            }

            val infoFii = mergedObject(
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
            if (plTotal > 0.0) fiiPatrimonialInfo.add(AssetIndicatorPoint("Patrimônio Total", plTotal, String.format(Locale.ROOT, "R$ %.2f B", plTotal / 1_000_000_000.0)))

            val compFiisObj = sections.optJSONObject("comparador") ?: sections.optJSONObject("comparadorFiis") ?: sections.optJSONObject("mediaTipoSegmento")
            if (compFiisObj != null) {
                val avgPvp = firstNumber(compFiisObj.optAny("vpaMedia"), compFiisObj.optAny("pvpMedia"), compFiisObj.optAny("avgPvp"))
                val avgDy = firstNumber(compFiisObj.optAny("dyMedia"), compFiisObj.optAny("avgDy"))
                if (avgPvp != 0.0) fiiPeerAverage.add(AssetComparisonPoint("P/VP", pvpVal, avgPvp))
                if (avgDy != 0.0) fiiPeerAverage.add(AssetComparisonPoint("DY 12M", dyVal, avgDy))
            }

            val distAtivos = firstArray(
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
            payoutHistory = payoutHistory,
            revenueByRegion = revenueByRegion,
            revenueByBusiness = revenueByBusiness,
            fiiDistribution12m = fiiDistribution12m,
            fiiPeerAverage = fiiPeerAverage,
            fiiPatrimonialInfo = fiiPatrimonialInfo,
            fiiAssetDistribution = fiiAssetDistribution,
            warnings = warnings,
            source = "Valorae Proxy / Investidor10"
        )
    }

    private fun parseFlexibleDateMillis(value: String): Long {
        if (value.isBlank()) return 0L
        val raw = value.trim()
        raw.toLongOrNull()?.let { return if (it > 10_000_000_000L) it else it * 1000L }
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "MM/yyyy",
            "yyyy-MM"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(raw)
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
     * Fallback direto legado. O caminho principal usa Valorae Proxy.
     */
    private fun fetchAssetDataDirect(ticker: String, bypassCache: Boolean = false): B3AssetData? {
        Log.w("B3NetworkService", "Fallback direto desativado para $ticker. Use somente Valorae Proxy.")
        return null
    }


    /**
     * Fetch historical chart points for graphing
     */
    private fun fetchHistoricalChartDirect(ticker: String, range: String = "1y"): List<ChartPoint> {
        Log.w("B3NetworkService", "Histórico direto desativado para $ticker/$range. Use somente Valorae Proxy.")
        return emptyList()
    }


    /**
     * Fetch news RSS search for Brazilian stocks
     */
    private fun fetchNewsDirect(ticker: String = ""): List<NewsItem> {
        Log.w("B3NetworkService", "Notícias diretas desativadas para ${ticker.ifBlank { "mercado" }}. Use somente Valorae Proxy.")
        return emptyList()
    }


    private fun enrichAssetDetails(base: B3AssetData): B3AssetData {
        // Não adiciona dados inventados. O caminho principal e único para dados de mercado é o Valorae Proxy.
        return base
    }
}
