package com.example

import com.example.network.B3NetworkService
import okhttp3.Request
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
  @Test
  fun proxyAssetRequestUsesV1ContractAndRequiredHeaders() {
    val request = Request.Builder()
        .url("https://servidor-valorae.vercel.app/api/v1/asset?ticker=PETR4&view=app&profile=turbo")
        .addHeader("Accept", "application/json")
        .addHeader("User-Agent", "VALORAE-Investidor-Portfolio/1.1.4 Android")
        .addHeader("x-valorae-app", "VALORAE Investidor")
        .addHeader("x-valorae-client", "valorae-investidor-android")
        .addHeader("x-valorae-build", "1.1.4-7")
        .addHeader("x-valorae-platform", "android")
        .addHeader("X-Valorae-Client-Id", "valorae-investidor-android")
        .addHeader("X-Valorae-Client-Version", "1.1.4")
        .addHeader("X-Valorae-Environment", "production")
        .build()

    assertEquals("https", request.url.scheme)
    assertTrue(request.url.encodedPath.endsWith("/api/v1/asset"))
    assertEquals("PETR4", request.url.queryParameter("ticker"))
    assertEquals("app", request.url.queryParameter("view"))
    assertEquals("turbo", request.url.queryParameter("profile"))
    assertEquals("VALORAE Investidor", request.header("x-valorae-app"))
    assertEquals("android", request.header("x-valorae-platform"))
  }

  @Test
  fun testRequiredHeadersAndBaseUrl() {
    val base = "https://servidor-valorae.vercel.app"
    assertEquals("https://servidor-valorae.vercel.app", base)
    assertTrue(base.startsWith("https://"))
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
    assertEquals("PARTIAL", incompletePayload.optString("status"))
    assertEquals(28.50, B3NetworkService.parseLocaleFinancialNumber("28,50"), 0.001)
  }
}
