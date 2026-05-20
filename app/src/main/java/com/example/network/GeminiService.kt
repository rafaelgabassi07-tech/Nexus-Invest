package com.example.network

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val MODEL = "gemini-3.5-flash"
    
    // Retrieves the API key injected dynamically from secrets to local BuildConfig
    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    suspend fun generateAnalysis(prompt: String): String {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiService", "Gemini API Key is invalid or placeholder. Using local rule-based engine.")
            return generateFallbacks(prompt)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
        
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(bodyString)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            parts.getJSONObject(0).optString("text", "Não foi possível obter uma resposta detalhada.")
                        } else {
                            "Erro: Estrutura de resposta vazia do modelo."
                        }
                    } else {
                        "Erro: Candidatos de resposta vazios."
                    }
                } else {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiService", "API Error (${response.code}): $errBody")
                    "Erro na API (${response.code}). Mostrando análise local em fallback:\n\n${generateFallbacks(prompt)}"
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Network call failed", e)
            "Falha de conexão. Mostrando análise local em fallback:\n\n${generateFallbacks(prompt)}"
        }
    }

    private fun generateFallbacks(prompt: String): String {
        // Generates beautiful rule-based recommendations styled after Investidor 10 in Portuguese
        val tickerPattern = Regex("(?i)\\b([A-Z]{4}\\d{1,2})\\b")
        val match = tickerPattern.find(prompt)
        val ticker = match?.value?.uppercase() ?: "PETR4"
        
        return """
        🔍 **Análise do Orientador de Carteira Investidor 10 (Modo Offline / Fallback)**
        
        Analisamos o ativo **$ticker** com base nos princípios consolidados de Value Investing (Ações) e Geração de Renda Passiva (Fundos Imobiliários):
        
        1. **Atração de Dividendos (Dividend Yield)**:
           Recomendamos priorizar ativos com DY histórico constante acima de **6% ao ano** em FIIs e de **5% ao ano** em Ações pagadoras estáveis. O ativo possui presença relevante na busca do investidor individual.
           
        2. **Relação Preço / Valor Patrimonial (P/VP)**:
           - Para Fundos Imobiliários: Um P/VP ideal situa-se entre 0.92 e 1.05. Evite comprar FIIs com P/VP acima de 1.10 para não pagar ágio excessivo.
           - Para Ações: P/VP abaixo de 1.50 sugere margem de segurança atrativa.
           
        3. **Alocação por Fração de Risco**:
           - Mantenha a diversificação: Recomendamos um máximo de 10% do seu capital investido em um único papel FII e 12% em uma única empresa. 
           - Rebalanceie a cada trimestre direcionando novos aportes aos ativos que estejam temporariamente abaixo da sua meta de alocação estabelecida.
        
        *Nota: Este relatório foi gerado localmente pelo engine de fallback por ausência de conexão ativa com a API Gemini.*
        """.trimIndent()
    }
}
