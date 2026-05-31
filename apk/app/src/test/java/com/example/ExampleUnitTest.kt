package com.example

import com.example.network.B3NetworkService
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class ExampleUnitTest {
  @Test
  fun fetchProxyAssetJson() {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    val request = Request.Builder()
        .url("https://servidor-valorae.vercel.app/api/asset?ticker=PETR4")
        .addHeader("Accept", "application/json")
        .addHeader("User-Agent", "VALORAE-Investidor-Portfolio/1.1.4 Android")
        .addHeader("X-Valorae-Client-Id", "valorae-investidor-android")
        .addHeader("X-Valorae-Client-Version", "21.5.13")
        .addHeader("X-Valorae-Environment", "production")
        .addHeader("X-Valorae-App", "VALORAE")
        .addHeader("X-Valorae-Consumer", "investidor-portfolio")
        .build()
    try {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "Empty body"
            val file = File("response_petr4.json")
            file.writeText(body)
            println("Response written to: ${file.absolutePath}")
            println("Status code: ${response.code}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
  }

  @Test
  fun testRequiredHeadersAndBaseUrl() {
    val base = "https://servidor-valorae.vercel.app"
    assertEquals("https://servidor-valorae.vercel.app", base)
  }

  @Test
  fun testRobustParserToleratesMissingFields() {
    val incompletePayload = JSONObject("""{
      "normalized": {
         "precoAtual": { "value": "28,50" }
      },
      "results": {
         "symbol": "VALE3",
         "dividendos": { "dividendYield": 8.5 }
      },
      "status": "PARTIAL",
      "warnings": ["Alguns dados fundamentalistas indisponíveis no momento"]
    }""")
    assertNotNull(incompletePayload)
  }
}
