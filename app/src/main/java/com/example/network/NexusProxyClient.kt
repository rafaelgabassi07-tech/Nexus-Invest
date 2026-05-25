package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import com.example.BuildConfig

/**
 * Resultado da busca do Proxy
 */
data class NexusAssetScrapeResult(
    val results: Map<String, String>,
    val source: String = "Proxy"
)

class NexusProxyClient {

    private val client = OkHttpClient()

    private fun inferAssetType(ticker: String): String {
        val t = ticker.trim().uppercase()
        val knownEtfs = setOf("BOVA11", "IVVB11", "SMAL11", "DIVO11", "XFIX11", "HASH11")
        val knownUnits = setOf("TAEE11", "SANB11", "ALUP11", "KLBN11", "BPAC11")

        if (knownEtfs.contains(t)) return "etfs"
        if (knownUnits.contains(t)) return "acoes"
        if (t.matches(Regex(".*3[2-5]$"))) return "bdrs"
        if (t.endsWith("11")) return "fiis"
        if (t.matches(Regex("^[A-Z]{1,5}$"))) return "stocks"
        return "acoes"
    }

    /**
     * Realiza a busca de dados via API do Nexus Engine (Processamento no Servidor)
     */
    suspend fun buscarDadosAtivo(ticker: String): NexusAssetScrapeResult? = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.VERCEL_BACKEND_URL.trimEnd('/')
        if (baseUrl.isEmpty() || baseUrl.contains("your-backend")) {
            android.util.Log.e("NexusProxy", "VERCEL_BACKEND_URL ausente. O processamento remoto do Nexus Engine está desativado.")
            return@withContext null
        }

        try {
            // Tenta a API do Nexus Engine que faz o parse no servidor
            val url = "$baseUrl/api/asset?ticker=${ticker.uppercase()}"
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Cache-Version", "nexus-ultra-v1")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                
                // O NexusEngine Ultra retorna os dados processados na chave "data"
                val dataObj = json.optJSONObject("data") ?: json.optJSONObject("results") ?: return@withContext null
                
                val map = mutableMapOf<String, String>()
                val keys = dataObj.keys()
                while(keys.hasNext()) {
                    val key = keys.next()
                    val value = dataObj.opt(key)
                    if (value != null && value != JSONObject.NULL) {
                        // O Engine no servidor já entrega as chaves normalizadas
                        map[key] = value.toString()
                    }
                }
                
                // Garantir campos legados se o engine retornar novos nomes
                if (map.containsKey("precoAtual") && !map.containsKey("price")) map["price"] = map["precoAtual"]!!
                if (map.containsKey("dy12m") && !map.containsKey("dy")) map["dy"] = map["dy12m"]!!
                if (map.containsKey("variacaoDay") && !map.containsKey("changePercent")) map["changePercent"] = map["variacaoDay"]!!

                return@withContext NexusAssetScrapeResult(results = map, source = "Nexus Smart Engine")
            } else {
                android.util.Log.e("NexusProxy", "Erro na API Nexus (${response.code}): ${response.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("NexusProxy", "Falha catastrófica ao conectar com o Nexus Engine: ${e.message}")
            e.printStackTrace()
        }

        return@withContext null
    }

    // Removido o scraping local redundante (Jsoup) para garantir que a lógica 
    // resida unicamente no servidor (nexus-engine.js), permitindo atualizações 
    // instantâneas sem necessidade de novo APK.
}
