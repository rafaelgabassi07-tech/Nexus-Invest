package com.example.network

import android.util.Log
import com.example.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
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
        val value = raw.trim()
        if (value.isBlank()) return false
        if (!value.startsWith("https://") && !value.startsWith("http://")) return false
        val lower = value.lowercase(Locale.ROOT)
        return !lower.contains("your-backend") &&
            !lower.contains("seu-dominio") &&
            !lower.contains("seu-valorae") &&
            !lower.contains("your-valorae") &&
            !lower.contains("example.com") &&
            !lower.contains("localhost")
    }

    private fun directFallbackEnabled(): Boolean {
        return envString { BuildConfig.VALORAE_DIRECT_FALLBACK_ENABLED }
            .lowercase(Locale.ROOT)
            .let { it == "1" || it == "true" || it == "yes" || it == "sim" || it == "on" }
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
            "1y", "1a", "y1", "12m" -> "1Y"
            "5y", "5a", "y5" -> "5Y"
            "max", "tudo", "all" -> "MAX"
            else -> range.trim().uppercase(Locale.ROOT)
        }
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
            .addHeader("User-Agent", "VALORAE-Investidor-Portfolio/1.1.4 Android")
            .addHeader("X-Valorae-Client-Id", proxyClientId())
            .addHeader("X-Valorae-Client-Version", "21.5.13")
            .addHeader("X-Valorae-Environment", "production")
            .addHeader("X-Valorae-App", "VALORAE")
            .addHeader("X-Valorae-Consumer", "investidor-portfolio")
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
                    return json
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
                    return json
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
        val field = normalized?.optJSONObject(key) ?: return null
        return field.optAny("value") ?: field.optAny("display")
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
            val n = parseLocaleFinancialNumber(value)
            if (n != 0.0) return n
        }
        return 0.0
    }

    private fun parseLocaleFinancialNumber(value: Any?): Double {
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

    private fun mapProxyAsset(payload: JSONObject?): B3AssetData? {
        val root = unwrapValoraePayload(payload) ?: return null
        val ticker = firstText(root.optAny("ticker"), root.optString("symbol", "")).uppercase(Locale.ROOT)
        if (ticker.isBlank()) return null
        val results = root.optJSONObject("results") ?: root
        val normalized = root.optJSONObject("normalized")
        val cotacao = results.optJSONObject("cotacao")
        val indicadores = results.optJSONObject("indicadores")
            ?: results.optObject("indicadoresFundamentalistas.semComparativos")
        val indicadoresAvancados = results.optJSONObject("indicadoresAvancados")
            ?: results.optJSONObject("advancedMetrics")
            ?: root.optJSONObject("indicadoresAvancados")
            ?: root.optJSONObject("advancedMetrics")
        val dadosEmpresa = results.optJSONObject("dadosEmpresa")
            ?: results.optObject("sections.empresa.dados")
        val infoEmpresa = results.optJSONObject("informacoesEmpresa")
        val sections = results.optJSONObject("sections")
        val infoFundo = results.optJSONObject("informacoesFundo")
            ?: sections?.optJSONObject("informacoesFundo")
        val dividendos = results.optJSONObject("dividendos")
            ?: results.optJSONObject("dividends")
            ?: results.optJSONObject("proventos")
            ?: results.optJSONObject("income")
            ?: results.optJSONObject("earnings")
            ?: root.optJSONObject("data.dividendos")
            ?: root.optJSONObject("data.dividends")
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

        val valorPatrimonialObj = results.optJSONObject("valorPatrimonial")
        val vpaRaw = if (valorPatrimonialObj != null) {
            valorPatrimonialObj.optAny("valorPatrimonial") ?: valorPatrimonialObj.optAny("valorPatrimonialRaw")
        } else {
            results.optAny("valorPatrimonial")
        }

        val patrimonioLiquidoRaw = if (valorPatrimonialObj != null) {
            valorPatrimonialObj.optAny("patrimonioLiquidoRaw") ?: valorPatrimonialObj.optAny("patrimonioLiquido")
        } else {
            results.optAny("patrimonioLiquido")
        }

        val valorDeMercadoObj = results.optJSONObject("valorDeMercado") ?: results.optJSONObject("valorMercado")
        val marketCapRaw = if (valorDeMercadoObj != null) {
            valorDeMercadoObj.optAny("valorDeMercadoRaw") ?: valorDeMercadoObj.optAny("valorDeMercado")
        } else {
            results.optAny("valorDeMercado") ?: results.optAny("valorMercado")
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
            pl = if (isFii) 0.0 else firstNumber(normalizedValue(normalized, "pl"), indicadores?.optAny("pl"), results.optAny("pl"), indicadoresAvancados?.optAny("p_l")),
            pvp = firstNumber(normalizedValue(normalized, "pvp"), indicadores?.optAny("pvp"), results.optAny("pvp"), indicadoresAvancados?.optAny("p_vp")),
            vpa = vpaVal,
            lpa = if (isFii) 0.0 else firstNumber(indicadores?.optAny("lpa"), results.optAny("lpa"), indicadoresAvancados?.optAny("lpa")),
            marketCap = marketCapVal,
            roe = firstNumber(normalizedValue(normalized, "roe"), indicadores?.optAny("roe"), results.optAny("roe"), indicadoresAvancados?.optAny("roe")),
            margins = firstNumber(normalizedValue(normalized, "margemLiquida"), indicadores?.optAny("margemLiquida"), results.optAny("margemLiquida"), indicadoresAvancados?.optAny("net_margin")),
            lastDividend = lastDividend,
            dailyLiquidity = dailyLiquidityVal,
            high52 = firstNumber(cotacao?.optAny("max52Semanas"), cotacao?.optAny("high52"), results.optAny("max52Semanas"), results.optAny("high52"), results.optAny("fiftyTwoWeekHigh")),
            low52 = firstNumber(cotacao?.optAny("min52Semanas"), cotacao?.optAny("low52"), results.optAny("min52Semanas"), results.optAny("low52"), results.optAny("fiftyTwoWeekLow")),
            forwardPE = firstNumber(indicadores?.optAny("forwardPE"), results.optAny("forwardPE")),
            priceToSales = firstNumber(indicadores?.optAny("psr"), results.optAny("psr"), normalizedValue(normalized, "psr"), indicadoresAvancados?.optAny("psr")),
            nextEarningsDate = firstText(firstDividend?.optAny("dataCom"), results.optAny("dataCom")),
            isFii = isFii,
            source = "Valorae Proxy",
            payout = firstNumber(normalizedValue(normalized, "payout"), indicadores?.optAny("payout"), results.optAny("payout"), indicadoresAvancados?.optAny("payout")),
            cagrRevenue5y = firstNumber(normalizedValue(normalized, "cagrReceitas5a"), indicadores?.optAny("cagrReceitas5a"), results.optAny("cagrReceitas5a"), indicadoresAvancados?.optAny("growth_net_revenue_last_5_years")),
            grossMargin = firstNumber(normalizedValue(normalized, "margemBruta"), indicadores?.optAny("margemBruta"), results.optAny("margemBruta"), indicadoresAvancados?.optAny("gross_margin")),
            ebitMargin = firstNumber(normalizedValue(normalized, "margemEbit"), indicadores?.optAny("margemEbit"), results.optAny("margemEbit"), indicadoresAvancados?.optAny("ebit_margin")),
            ebitdaMargin = firstNumber(normalizedValue(normalized, "margemEbitda"), indicadores?.optAny("margemEbitda"), results.optAny("margemEbitda"), indicadoresAvancados?.optAny("ebitda_margin")),
            evEbitda = firstNumber(normalizedValue(normalized, "evEbitda"), indicadores?.optAny("evEbitda"), results.optAny("evEbitda"), indicadoresAvancados?.optAny("ev_ebitda")),
            evEbit = firstNumber(normalizedValue(normalized, "evEbit"), indicadores?.optAny("evEbit"), results.optAny("evEbit"), indicadoresAvancados?.optAny("ev_ebit")),
            priceEbitda = firstNumber(normalizedValue(normalized, "pEbitda"), indicadores?.optAny("pEbitda"), results.optAny("pEbitda"), indicadores?.optAny("priceEbitda"), indicadoresAvancados?.optAny("p_ebitda")),
            priceEbit = firstNumber(normalizedValue(normalized, "pEbit"), indicadores?.optAny("pEbit"), results.optAny("pEbit"), indicadores?.optAny("priceEbit"), indicadoresAvancados?.optAny("p_ebit")),
            priceAsset = firstNumber(normalizedValue(normalized, "pAtivo"), indicadores?.optAny("pAtivo"), results.optAny("pAtivo"), indicadoresAvancados?.optAny("p_assets")),
            priceCapGiro = firstNumber(normalizedValue(normalized, "pCapGiro"), indicadores?.optAny("pCapGiro"), results.optAny("pCapGiro"), indicadoresAvancados?.optAny("p_working_capital")),
            priceAtivoCircLiq = firstNumber(normalizedValue(normalized, "pAtivoCircLiq"), indicadores?.optAny("pAtivoCircLiq"), results.optAny("pAtivoCircLiq"), indicadoresAvancados?.optAny("p_asset_current_net")),
            giroAtivos = firstNumber(normalizedValue(normalized, "giroAtivos"), indicadores?.optAny("giroAtivos"), results.optAny("giroAtivos"), indicadoresAvancados?.optAny("active_turns")),
            roic = firstNumber(normalizedValue(normalized, "roic"), indicadores?.optAny("roic"), results.optAny("roic"), indicadoresAvancados?.optAny("roic")),
            roa = firstNumber(normalizedValue(normalized, "roa"), indicadores?.optAny("roa"), results.optAny("roa"), indicadoresAvancados?.optAny("roa")),
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
            firmValue = firstNumber(infoEmpresa?.optAny("valorDeFirma"), results.optAny("valorDeFirma"), indicadoresAvancados?.optAny("enterprise_value")),
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
            fiiTotalHolders = firstText(infoFundo?.optAny("numeroCotistas"), results.optAny("numeroCotistas")),
            fiiIssuedShares = firstText(infoFundo?.optAny("cotasEmitidas"), results.optAny("cotasEmitidas")),
            fiiAdminFee = firstText(infoFundo?.optAny("taxaAdministracao"), results.optAny("taxaAdministracao")),
            fiiFundType = firstText(infoFundo?.optAny("tipoFundo"), results.optAny("tipoFundo")),
            fiiMandate = firstText(infoFundo?.optAny("mandato"), results.optAny("mandato")),
            fiiTargetAudience = firstText(infoFundo?.optAny("publicoAlvo"), results.optAny("publicoAlvo")),
            fiiManagementType = firstText(infoFundo?.optAny("tipoGestao"), results.optAny("tipoGestao")),
            fiiDuration = firstText(infoFundo?.optAny("prazoDuracao"), results.optAny("prazoDuracao")),
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
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val mapped = mapProxyAsset(assets.optJSONObject(i)) ?: continue
                    if (mapped.ticker.isNotBlank()) {
                        out[mapped.ticker] = mapped
                        putInCache("asset_data_proxy_${mapped.ticker}", mapped, 5)
                    }
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
            mapOf("ticker" to ticker.trim().uppercase(Locale.ROOT), "range" to normalizedRange, "limit" to "260")
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

    fun fetchPortfolioAnalysis(positions: List<PortfolioProxyPosition>): PortfolioProxyAnalysis? {
        if (positions.isEmpty()) return null
        val cacheKey = "portfolio_analysis_${positions.joinToString("|") { it.ticker + ":" + it.quantity }}"
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
        val cacheKey = "portfolio_history_${normalizedRange}_${positions.joinToString("|") { it.ticker + ":" + it.quantity }}"
        getFromCache<List<PortfolioHistoryPoint>>(cacheKey)?.let { return it }
        val payload = JSONObject()
            .put("positions", positionArray(positions))
            .put("range", normalizedRange)
            .put("limit", 260)
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
                accumulated = if (explicitAccumulated != 0.0) explicitAccumulated else accumulated + monthly
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
        val cacheKey = "next_dividends_${positions.joinToString("|") { it.ticker + ":" + it.quantity }}"
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
                "profile" to "deep",
                "view" to "full",
                "mode" to "super",
                "includeNews" to "0",
                "lean" to "1",
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
                    "lean" to "1",
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

        val bundle = parseAssetChartBundle(clean, isFii, root, priceHistory)
        putInCache(cacheKey, bundle, 4)
        return bundle
    }

    private fun parseAssetChartBundle(ticker: String, isFii: Boolean, root: JSONObject, priceHistory: List<ChartPoint>): AssetChartBundle {
        val results = root.optJSONObject("results") ?: root
        val normalized = root.optJSONObject("normalized")
        val sections = results.optJSONObject("sections") ?: results

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
            val chartsObj = sections.optJSONObject("charts") ?: sections.optJSONObject("rentabilidadeChart")
            val points = firstArray(
                chartsObj?.optJSONArray("points"),
                chartsObj?.optJSONArray("series"),
                sections.optJSONArray("historicalPrice")
            )
            if (points != null) {
                for (i in 0 until points.length()) {
                    val p = points.optJSONObject(i) ?: continue
                    val close = firstNumber(p.optAny("close"), p.optAny("value"), p.optAny("y"), p.optAny("preco"))
                    if (close <= 0.0) continue
                    val label = firstText(p.optAny("label"), p.optAny("date"), "P${i + 1}")
                    hPoints.add(ChartPoint(i.toLong(), label, close))
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
        val adv = sections.optJSONObject("indicadoresAvancados") ?: results.optJSONObject("indicadoresAvancados") ?: root.optJSONObject("indicadoresAvancados") ?: JSONObject()

        val indicatorKeys = listOf(
            Triple("p_l", "P/L", ""),
            Triple("p_vp", "P/VP", ""),
            Triple("dy", "Dividend Yield", "%"),
            Triple("payout", "Payout", "%"),
            Triple("net_margin", "Margem Líquida", "%"),
            Triple("gross_margin", "Margem Bruta", "%"),
            Triple("ebit_margin", "Margem EBIT", "%"),
            Triple("ebitda_margin", "Margem EBITDA", "%"),
            Triple("ev_ebitda", "EV/EBITDA", ""),
            Triple("ev_ebit", "EV/EBIT", ""),
            Triple("roic", "ROIC", "%"),
            Triple("roe", "ROE", "%"),
            Triple("roa", "ROA", "%"),
            Triple("net_debt_ebitda", "Dívida Líquida/EBITDA", ""),
            Triple("net_debt_ebit", "Dívida Líquida/EBIT", ""),
            Triple("gross_debt_net_worth", "Dívida Bruta/Patrimônio", ""),
            Triple("net_worth_assets", "Patrimônio/Ativos", "%"),
            Triple("liabilities_assets", "Passivos/Ativos", "%"),
            Triple("current_liquidity", "Liquidez Corrente", ""),
            Triple("growth_net_revenue_last_5_years", "CAGR Receitas 5A", "%"),
            Triple("growth_net_profit_last_5_years", "CAGR Lucros 5A", "%")
        )

        val indicatorHistory = mutableMapOf<String, List<AssetIndicatorPoint>>()

        for ((field, label, unit) in indicatorKeys) {
            val v = firstNumber(adv.optAny(field))
            if (v != 0.0) {
                val display = if (unit == "%") String.format(Locale.ROOT, "%.2f%%", v) else String.format(Locale.ROOT, "%.2f", v)
                indicatorCards.add(AssetIndicatorPoint(label = label, value = v, display = display, unit = unit))
            }
        }

        val dyVal = firstNumber(normalizedValue(normalized, "dividendYield"), results.optAny("dividendYield"))

        if (indicatorCards.none { it.label == "P/L" }) {
            val plVal = firstNumber(normalizedValue(normalized, "pl"), results.optAny("pl"))
            if (plVal != 0.0) indicatorCards.add(AssetIndicatorPoint("P/L", plVal, String.format(Locale.ROOT, "%.2f", plVal)))
        }
        if (indicatorCards.none { it.label == "P/VP" }) {
            val pvpVal = firstNumber(normalizedValue(normalized, "pvp"), results.optAny("pvp"))
            if (pvpVal != 0.0) indicatorCards.add(AssetIndicatorPoint("P/VP", pvpVal, String.format(Locale.ROOT, "%.2f", pvpVal)))
        }
        if (indicatorCards.none { it.label == "Dividend Yield" }) {
            if (dyVal != 0.0) indicatorCards.add(AssetIndicatorPoint("Dividend Yield", dyVal, String.format(Locale.ROOT, "%.2f%%", dyVal), "%"))
        }

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
                        val year = firstText(pt.optAny("year"), pt.optAny("label"), pt.optAny("date"), "P${i+1}")
                        val value = firstNumber(pt.optAny("value"), pt.optAny("valor"))
                        val disp = pt.optString("display", String.format(Locale.ROOT, "%.2f", value))
                        list.add(AssetIndicatorPoint(label = k, value = value, display = disp, year = year))
                    }
                    if (list.isNotEmpty()) {
                        indicatorHistory[k] = list
                    }
                }
            }
        }

        val dividendEvents = mutableListOf<DividendEvent>()
        val pEvents = firstArray(
            results.optJSONArray("historicoDividendos"),
            results.optJSONArray("dividends"),
            results.optJSONArray("historicoProventos"),
            results.optJSONArray("proventos"),
            results.optObject("dividendos.historico")?.optJSONArray("items") ?: results.optObject("dividendos")?.optJSONArray("historico")
        )
        if (pEvents != null && pEvents.length() > 0) {
            for (i in 0 until pEvents.length()) {
                val ev = pEvents.optJSONObject(i) ?: continue
                val type = firstText(ev.optAny("tipo"), ev.optAny("type"), ev.optAny("kind"), "Dividendo")
                val datacom = normalizeDisplayDate(firstText(ev.optAny("dataCom"), ev.optAny("dateCom"), ev.optAny("comDate"), ev.optAny("date_with")))
                val payment = normalizeDisplayDate(firstText(ev.optAny("dataPagamento"), ev.optAny("paymentDate"), ev.optAny("pagamento"), ev.optAny("date_payment")))
                val value = firstNumber(ev.optAny("valor"), ev.optAny("value"), ev.optAny("amount"), ev.optAny("valuePerShare"), ev.optAny("valorPorCota"))
                if (value > 0.0) {
                    dividendEvents.add(
                        DividendEvent(
                            ticker = ticker,
                            dateCom = datacom,
                            paymentDate = payment,
                            valuePerShare = value,
                            status = "Pago"
                        )
                    )
                }
            }
        }

        val dividendYearly = mutableListOf<AssetIndicatorPoint>()
        val dividendMonthly = mutableListOf<AssetIndicatorPoint>()
        val dividendYieldHistory = mutableListOf<AssetIndicatorPoint>()
        val payoutHistory = mutableListOf<AssetIndicatorPoint>()

        if (dividendEvents.isNotEmpty()) {
            val byYear = mutableMapOf<String, Double>()
            val byMonth = mutableMapOf<String, Double>()
            for (ev in dividendEvents) {
                val year = try {
                    ev.dateCom.substringAfterLast("/", "Previsto")
                } catch (_: Exception) { "Previsto" }
                if (year.length == 4 && year.all { it.isDigit() }) {
                    byYear[year] = (byYear[year] ?: 0.0) + ev.valuePerShare
                }

                val monthYear = try {
                    val parts = ev.dateCom.split("/")
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

        val payoutHistObj = sections.optJSONObject("payoutHistorico") ?: sections.optJSONObject("demonstrativos")?.optJSONObject("payoutHistorico")
        if (payoutHistObj != null) {
            val yearsArr = payoutHistObj.optJSONArray("years")
            val payoutArr = payoutHistObj.optJSONArray("payOutCompanyIndicators")
            if (yearsArr != null && payoutArr != null) {
                val len = kotlin.math.min(yearsArr.length(), payoutArr.length())
                for (i in 0 until len) {
                    val yr = yearsArr.optString(i)
                    val pay = firstNumber(payoutArr.opt(i))
                    if (yr.isNotBlank() && pay != 0.0) {
                        payoutHistory.add(AssetIndicatorPoint("Payout", pay, String.format(Locale.ROOT, "%.2f%%", pay), "%", year = yr))
                    }
                }
            }
        }

        val indexComparison = mutableListOf<AssetComparisonSeries>()
        val compIndices = sections.optJSONObject("comparacaoIndices") ?: sections.optJSONObject("rentabilidadeChart")
        if (compIndices != null) {
            val seriesNames = listOf("ativo", "ibov", "ifix", "cdi", "ipca")
            for (sName in seriesNames) {
                val sArr = compIndices.optJSONArray(sName) ?: compIndices.optJSONArray(sName.uppercase())
                if (sArr != null) {
                    val pointsList = mutableListOf<AssetComparisonPoint>()
                    for (i in 0 until sArr.length()) {
                        val ptObj = sArr.optJSONObject(i) ?: continue
                        val lbl = firstText(ptObj.optAny("label"), ptObj.optAny("date"), "P${i+1}")
                        val v = firstNumber(ptObj.optAny("value"), ptObj.optAny("close"), ptObj.optAny("valor"))
                        pointsList.add(AssetComparisonPoint(label = lbl, value = v))
                    }
                    if (pointsList.isNotEmpty()) {
                        indexComparison.add(AssetComparisonSeries(sName.uppercase(Locale.ROOT), pointsList))
                    }
                }
            }
        }

        val commodityComparison = mutableListOf<AssetComparisonSeries>()
        val compCommObj = sections.optJSONObject("comparacaoCommodity")
        if (compCommObj != null) {
            val brentArr = compCommObj.optJSONArray("brent") ?: compCommObj.optJSONArray("BRENT")
            if (brentArr != null) {
                val pointsList = mutableListOf<AssetComparisonPoint>()
                for (i in 0 until brentArr.length()) {
                    val pt = brentArr.optJSONObject(i) ?: continue
                    pointsList.add(AssetComparisonPoint(label = firstText(pt.optAny("label"), "P${i+1}"), value = firstNumber(pt.optAny("value"))))
                }
                if (pointsList.isNotEmpty()) {
                    commodityComparison.add(AssetComparisonSeries("BRENT OIL", pointsList))
                }
            }
        }

        val revenueProfit = mutableListOf<FinancialStatementPoint>()
        val demObj = sections.optJSONObject("demonstrativos")
        val revProfArr = firstArray(
            demObj?.optJSONArray("receitasLucros"),
            sections.optJSONArray("receitasLucros"),
            demObj?.optJSONArray("items")
        )
        if (revProfArr != null) {
            for (i in 0 until revProfArr.length()) {
                val r = revProfArr.optJSONObject(i) ?: continue
                val yr = firstText(r.optAny("year"), r.optAny("ano"), "")
                val qt = firstText(r.optAny("quarter"), r.optAny("trimestre"), "")
                val rev = firstNumber(r.optAny("net_revenue"), r.optAny("revenue"), r.optAny("receitaLiquida"), r.optAny("receita"))
                val profit = firstNumber(r.optAny("net_profit"), r.optAny("profit"), r.optAny("lucroLiquido"), r.optAny("lucro"))
                val costVal = firstNumber(r.optAny("cost"), r.optAny("custo"))
                val gp = firstNumber(r.optAny("gross_profit"), r.optAny("lucroBruto"))
                val ebtda = firstNumber(r.optAny("ebitda"))
                val ebt = firstNumber(r.optAny("ebit"))
                if (yr.isNotBlank()) {
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
        val profitQuoteObj = demObj?.optJSONObject("lucroCotacao") ?: sections.optJSONObject("lucroCotacao")
        if (profitQuoteObj != null) {
            val quotesArr = profitQuoteObj.optJSONArray("quotes") ?: profitQuoteObj.optJSONArray("cotações")
            val profitsArr = profitQuoteObj.optJSONArray("profits") ?: profitQuoteObj.optJSONArray("lucros")
            if (quotesArr != null && profitsArr != null) {
                val len = kotlin.math.min(quotesArr.length(), profitsArr.length())
                for (i in 0 until len) {
                    val q = quotesArr.optJSONObject(i) ?: continue
                    val p = profitsArr.optJSONObject(i) ?: continue
                    val lbl = firstText(q.optAny("label"), q.optAny("year"), "P${i+1}")
                    profitVsQuote.add(
                        AssetComparisonPoint(
                            label = lbl,
                            value = firstNumber(q.optAny("value")),
                            secondaryValue = firstNumber(p.optAny("value"))
                        )
                    )
                }
            }
        }

        val equityEvolution = mutableListOf<FinancialStatementPoint>()
        val eqArr = firstArray(
            demObj?.optJSONArray("evolucaoPatrimonio"),
            sections.optJSONArray("evolucaoPatrimonio")
        )
        if (eqArr != null) {
            for (i in 0 until eqArr.length()) {
                val eq = eqArr.optJSONObject(i) ?: continue
                val yr = firstText(eq.optAny("year"), eq.optAny("ano"), "")
                val plVal = firstNumber(eq.optAny("net_worth"), eq.optAny("patrimonioLiquido"), eq.optAny("pl"))
                val assetsVal = firstNumber(eq.optAny("balance_total_assets"), eq.optAny("ativos"), eq.optAny("assets"))
                val liabVal = firstNumber(eq.optAny("balance_total_liabilities"), eq.optAny("passivos"), eq.optAny("liabilities"))
                if (yr.isNotBlank()) {
                    equityEvolution.add(
                        FinancialStatementPoint(
                            label = yr,
                            year = yr,
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

        val regObj = sections.optJSONObject("regioesReceita") ?: sections.optObject("empresa.regioesReceita")
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

        val busObj = sections.optJSONObject("negociosReceita") ?: sections.optObject("empresa.negociosReceita")
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

        val fiiDistribution12m = mutableListOf<AssetIndicatorPoint>()
        val fiiPeerAverage = mutableListOf<AssetComparisonPoint>()
        val fiiPatrimonialInfo = mutableListOf<AssetIndicatorPoint>()
        val fiiAssetDistribution = mutableMapOf<String, List<AssetBreakdownPoint>>()

        if (isFii) {
            val y1m = firstNumber(normalizedValue(normalized, "yield1m"))
            val y3m = firstNumber(normalizedValue(normalized, "yield3m"))
            val y6m = firstNumber(normalizedValue(normalized, "yield6m"))
            val y12m_val = firstNumber(normalizedValue(normalized, "yield12m"))
            if (y1m != 0.0) fiiDistribution12m.add(AssetIndicatorPoint("Yield 1M", y1m, String.format(Locale.ROOT, "%.2f%%", y1m), "%"))
            if (y3m != 0.0) fiiDistribution12m.add(AssetIndicatorPoint("Yield 3M", y3m, String.format(Locale.ROOT, "%.2f%%", y3m), "%"))
            if (y6m != 0.0) fiiDistribution12m.add(AssetIndicatorPoint("Yield 6M", y6m, String.format(Locale.ROOT, "%.2f%%", y6m), "%"))
            if (y12m_val != 0.0) fiiDistribution12m.add(AssetIndicatorPoint("Yield 12M", y12m_val, String.format(Locale.ROOT, "%.2f%%", y12m_val), "%"))

            val vpaVal = firstNumber(normalizedValue(normalized, "valorPatrimonialCota"), results.optAny("vpa"), results.optAny("valorPatrimonialCota"))
            val currentPrice = firstNumber(normalizedValue(normalized, "precoAtual"), results.optAny("precoAtual"))
            val numCotas = firstText(results.optAny("cotasEmitidas"), results.optAny("totalPapeis"))
            val pvpVal = firstNumber(normalizedValue(normalized, "pvp"), results.optAny("pvp"))
            val plTotal = firstNumber(normalizedValue(normalized, "patrimonioLiquido"), results.optAny("patrimonioLiquido"))

            if (vpaVal > 0.0) fiiPatrimonialInfo.add(AssetIndicatorPoint("VPA por Cota", vpaVal, String.format(Locale.ROOT, "R$ %.2f", vpaVal)))
            if (currentPrice > 0.0) fiiPatrimonialInfo.add(AssetIndicatorPoint("Valor da Cota", currentPrice, String.format(Locale.ROOT, "R$ %.2f", currentPrice)))
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
                sections.optJSONArray("listaImoveis")
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
                val imoveisArr = sections.optJSONArray("listaImoveis")
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

            // Scraping fallbacks removed as requested (deep clean)
            val numericFallback = emptyMap<String, Double>()
            val textFallback = emptyMap<String, String>()

            // Sync from optional direct fallback if Proxy is unavailable and fallback is explicitly enabled
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
    private fun fetchHistoricalChartDirect(ticker: String, range: String = "1y"): List<ChartPoint> {
        val normalizedRange = normalizeProxyRange(range)
        val symbol = getTickerWithSuffix(ticker)
        val yahooRange = when (normalizedRange) {
            "1D" -> "1d"
            "5D" -> "5d"
            "1M" -> "1mo"
            "3M" -> "3mo"
            "6M" -> "6mo"
            "1Y" -> "1y"
            "5Y" -> "5y"
            "MAX" -> "max"
            else -> range.trim().lowercase(Locale.ROOT).ifBlank { "1y" }
        }
        val interval = when (normalizedRange) {
            "1D" -> "5m"
            "5D" -> "15m"
            "1M", "3M" -> "1d"
            "6M", "1Y" -> "1wk"
            else -> "1mo"
        }
        
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=$yahooRange&interval=$interval&includePrePost=false"
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
                            val sdf = SimpleDateFormat(if (normalizedRange == "1D" || normalizedRange == "5D") "HH:mm" else "dd/MM", Locale.getDefault())

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
    private fun fetchNewsDirect(ticker: String = ""): List<NewsItem> {
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
        // Não adiciona dados inventados. O caminho principal é o Valorae Proxy;
        // fallback direto só preserva campos obtidos por fonte externa real.
        return base
    }
}
