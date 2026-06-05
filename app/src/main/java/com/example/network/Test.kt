import com.example.network.B3NetworkService
import org.json.JSONObject
import java.net.URL

fun main() {
    val url = "https://servidor-valorae.vercel.app/api/v1/asset?ticker=WEGE3&view=app"
    val jsonString = URL(url).readText()
    val json = JSONObject(jsonString)
    println("json lengths: ${json.length()}")
    
    // We want to see if the json contains "negociosReceita" or "revenueBreakdowns"
    println("has appPayload: ${json.has("appPayload")}")
    
    val str = json.toString()
    println("Contains 'revenueGeography': ${str.contains("revenueGeography")}")
    println("Contains 'negociosReceita': ${str.contains("negociosReceita")}")
    println("Contains 'receitasLucros': ${str.contains("receitasLucros")}")
    
    try {
        val method = B3NetworkService::class.java.getDeclaredMethod("parseAssetCharts", String::class.java, JSONObject::class.java)
        method.isAccessible = true
        val bundle = method.invoke(null, "WEGE3", json)
        println("Bundle = $bundle")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
