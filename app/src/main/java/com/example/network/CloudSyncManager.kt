package com.example.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.Transaction
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Camada opcional de sincronização VALORAE <-> Supabase.
 *
 * Princípios:
 * - Não quebra o app se o Supabase não estiver configurado.
 * - Nunca exige recurso pago obrigatório.
 * - Não embute service_role no APK; use publishable/anon key com RLS no acesso direto.
 * - Também suporta ponte via VALORAE Proxy quando você preferir deixar chaves secretas no backend.
 * - Permite salvar snapshots amplos: carteira, transações, preços, analytics, IPCA, proventos, notícias e diagnóstico.
 */
object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private const val PREFS_NAME = "valorae_supabase_sync"
    private const val DEVICE_ID_KEY = "device_id"
    private const val INSTALL_USER_ID_KEY = "install_user_id"
    private const val SNAPSHOT_TABLE = "valorae_user_snapshots"
    private const val BACKUP_TABLE = "valorae_sync_backups"
    private const val SCHEMA_VERSION = 1

    val supabaseUrl: String = cleanUrl(runCatching { BuildConfig.SUPABASE_URL }.getOrDefault(""))
    val supabasePublishableKey: String = runCatching { BuildConfig.SUPABASE_PUBLISHABLE_KEY }.getOrDefault("").trim()
    val supabaseAnonKey: String = runCatching { BuildConfig.SUPABASE_ANON_KEY }.getOrDefault("").trim()
    val supabaseKey: String = supabasePublishableKey.ifBlank { supabaseAnonKey }
    val valoraeProxyUrl: String = cleanUrl(runCatching { BuildConfig.VALORAE_API_BASE_URL }.getOrDefault(""))
    val proxySyncToken: String = runCatching { BuildConfig.VALORAE_SUPABASE_SYNC_TOKEN }.getOrDefault("").trim()

    private val syncEnabled: Boolean = boolConfig(runCatching { BuildConfig.SUPABASE_SYNC_ENABLED }.getOrDefault("true"), true)
    private val proxySyncEnabled: Boolean = boolConfig(runCatching { BuildConfig.VALORAE_SUPABASE_PROXY_SYNC_ENABLED }.getOrDefault("true"), true)
    private val autoBackupEnabled: Boolean = boolConfig(runCatching { BuildConfig.VALORAE_SUPABASE_AUTO_BACKUP_ENABLED }.getOrDefault("false"), false)
    private val autoEncryptionSecret: String = runCatching { BuildConfig.VALORAE_SUPABASE_AUTO_ENCRYPTION_SECRET }.getOrDefault("").trim()

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    private fun cleanUrl(raw: String): String = raw.trim().removeSuffix("/")

    private fun boolConfig(value: String?, default: Boolean = false): Boolean {
        val raw = value?.trim()?.lowercase(Locale.ROOT) ?: return default
        if (raw.isBlank()) return default
        return raw in setOf("1", "true", "yes", "sim", "on", "enabled")
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun nowIso(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

    fun isDirectSupabaseConfigured(): Boolean =
        syncEnabled && supabaseUrl.startsWith("https://") && supabaseKey.isNotBlank()

    fun isProxySupabaseConfigured(): Boolean =
        syncEnabled && proxySyncEnabled && valoraeProxyUrl.startsWith("https://")

    fun isCloudConfigured(): Boolean = isDirectSupabaseConfigured() || isProxySupabaseConfigured()

    fun isAutoBackupEnabled(): Boolean = autoBackupEnabled && isCloudConfigured()

    fun configurationLabel(): String = when {
        !syncEnabled -> "Supabase desativado por configuração"
        isDirectSupabaseConfigured() && isProxySupabaseConfigured() -> "Supabase direto + ponte Proxy disponíveis"
        isDirectSupabaseConfigured() -> "Supabase direto configurado"
        isProxySupabaseConfigured() -> "Ponte Proxy Supabase disponível"
        else -> "Supabase ainda não configurado"
    }

    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(DEVICE_ID_KEY, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "android-${UUID.randomUUID()}"
        prefs.edit().putString(DEVICE_ID_KEY, generated).apply()
        return generated
    }

    fun getOrCreateInstallUserId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(INSTALL_USER_ID_KEY, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "valorae-${UUID.randomUUID()}"
        prefs.edit().putString(INSTALL_USER_ID_KEY, generated).apply()
        return generated
    }

    suspend fun testSupabaseConnection(): Result<String> = withContext(Dispatchers.IO) {
        if (!syncEnabled) return@withContext Result.failure(Exception("Supabase Sync está desativado em BuildConfig."))

        val direct = if (isDirectSupabaseConfigured()) {
            runCatching {
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/?apikey=${enc(supabaseKey.take(12))}")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    response.code in 200..499
                }
            }.getOrDefault(false)
        } else false

        val proxy = if (!direct && isProxySupabaseConfigured()) {
            runCatching {
                val request = Request.Builder()
                    .url("$valoraeProxyUrl/api/sync?action=health")
                    .addHeader("x-valorae-sync-token", proxySyncToken)
                    .get()
                    .build()
                client.newCall(request).execute().use { response -> response.isSuccessful }
            }.getOrDefault(false)
        } else false

        when {
            direct -> Result.success("Conexão Supabase direta ativa. Tabelas e RLS ainda dependem do SQL de migração.")
            proxy -> Result.success("Ponte VALORAE Proxy para Supabase ativa.")
            else -> Result.failure(Exception("Supabase não respondeu. Verifique SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY/ANON_KEY ou a ponte /api/sync do Proxy."))
        }
    }

    suspend fun backupData(
        userId: String,
        transactions: List<Transaction>,
        passPhrase: String? = null,
        context: Context? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isCloudConfigured()) return@withContext Result.failure(Exception("Supabase não está configurado."))
        val arr = transactionsToJsonArray(transactions)
        val payload = JSONObject().put("transactions", arr).put("count", arr.length())
        val secret = passPhrase?.takeIf { it.isNotBlank() } ?: autoEncryptionSecret.takeIf { it.isNotBlank() }
        val result = upsertSnapshot(
            userId = userId,
            domain = "portfolio",
            snapshotKey = "transactions_backup",
            payload = payload,
            passPhrase = secret,
            source = "apk-backup",
            context = context
        )
        result.fold(
            onSuccess = { Result.success("Backup enviado ao Supabase com ${transactions.size} movimentações.") },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun restoreData(userId: String, passPhrase: String? = null): Result<List<Transaction>> = withContext(Dispatchers.IO) {
        val result = fetchSnapshot(userId, "portfolio", "transactions_backup", passPhrase)
        if (result.isFailure) return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Falha ao buscar backup."))
        val payload = result.getOrThrow()
        val arr = payload.optJSONArray("transactions") ?: payload.optJSONArray("items") ?: JSONArray()
        Result.success(transactionsFromJsonArray(arr))
    }

    suspend fun upsertSnapshot(
        userId: String,
        domain: String,
        snapshotKey: String,
        payload: JSONObject,
        passPhrase: String? = null,
        source: String = "apk",
        context: Context? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isCloudConfigured()) return@withContext Result.failure(Exception("Supabase não configurado."))
        val safeUser = userId.ifBlank { context?.let { getOrCreateInstallUserId(it) } ?: "local-device" }
        val deviceId = context?.let { getOrCreateDeviceId(it) }.orEmpty()
        val encrypted = !passPhrase.isNullOrBlank()
        val row = JSONObject().apply {
            put("user_id", safeUser)
            put("domain", domain.trim().lowercase(Locale.ROOT))
            put("snapshot_key", snapshotKey.trim().lowercase(Locale.ROOT))
            put("schema_version", SCHEMA_VERSION)
            put("app_version", BuildConfig.VERSION_NAME)
            put("device_id", deviceId)
            put("source", source)
            put("encrypted", encrypted)
            if (encrypted) {
                put("payload", JSONObject.NULL)
                put("payload_ciphertext", CryptoHelper.encrypt(payload.toString(), passPhrase!!))
            } else {
                put("payload", payload)
                put("payload_ciphertext", JSONObject.NULL)
            }
            put("updated_at", nowIso())
        }

        val direct = if (isDirectSupabaseConfigured()) upsertSnapshotDirect(row) else Result.failure(Exception("Supabase direto indisponível."))
        if (direct.isSuccess) return@withContext direct

        val proxy = if (isProxySupabaseConfigured()) upsertSnapshotViaProxy(row) else direct
        proxy.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(Exception("Falha Supabase: ${it.message ?: direct.exceptionOrNull()?.message ?: "desconhecida"}")) }
        )
    }

    suspend fun fetchSnapshot(
        userId: String,
        domain: String,
        snapshotKey: String,
        passPhrase: String? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        if (isDirectSupabaseConfigured()) {
            val direct = fetchSnapshotDirect(userId, domain, snapshotKey, passPhrase)
            if (direct.isSuccess) return@withContext direct
        }
        if (isProxySupabaseConfigured()) {
            return@withContext fetchSnapshotViaProxy(userId, domain, snapshotKey, passPhrase)
        }
        Result.failure(Exception("Supabase não configurado."))
    }

    suspend fun upsertManySnapshots(
        userId: String,
        snapshots: List<Pair<Pair<String, String>, JSONObject>>,
        context: Context? = null,
        source: String = "apk-batch"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (snapshots.isEmpty()) return@withContext Result.success("Nenhum snapshot para sincronizar.")
        var ok = 0
        val errors = mutableListOf<String>()
        for ((key, payload) in snapshots) {
            val (domain, snapshotKey) = key
            val r = upsertSnapshot(userId, domain, snapshotKey, payload, context = context, source = source)
            if (r.isSuccess) ok++ else errors.add("$domain/$snapshotKey: ${r.exceptionOrNull()?.message}")
        }
        if (ok > 0) Result.success("$ok/${snapshots.size} snapshots salvos no Supabase${if (errors.isNotEmpty()) " (${errors.size} avisos)" else ""}.")
        else Result.failure(Exception(errors.joinToString("; ").ifBlank { "Nenhum snapshot salvo." }))
    }

    private fun upsertSnapshotDirect(row: JSONObject): Result<String> {
        return try {
            val body = row.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/$SNAPSHOT_TABLE?on_conflict=user_id,domain,snapshot_key")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success("Snapshot salvo diretamente no Supabase.")
                else Result.failure(Exception("HTTP ${response.code}: ${response.body?.string()?.take(240) ?: response.message}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha no upsert direto", e)
            Result.failure(e)
        }
    }

    private fun upsertSnapshotViaProxy(row: JSONObject): Result<String> {
        return try {
            val body = JSONObject().put("action", "upsert_snapshot").put("record", row).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val req = Request.Builder()
                .url("$valoraeProxyUrl/api/sync")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-valorae-sync-token", proxySyncToken)
                .post(body)
                .build()
            client.newCall(req).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.isSuccessful) Result.success("Snapshot salvo via VALORAE Proxy.")
                else Result.failure(Exception("Proxy HTTP ${response.code}: ${text.take(240)}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha no upsert via proxy", e)
            Result.failure(e)
        }
    }

    private fun fetchSnapshotDirect(userId: String, domain: String, snapshotKey: String, passPhrase: String?): Result<JSONObject> {
        return try {
            val url = "$supabaseUrl/rest/v1/$SNAPSHOT_TABLE" +
                "?user_id=eq.${enc(userId)}&domain=eq.${enc(domain.lowercase(Locale.ROOT))}" +
                "&snapshot_key=eq.${enc(snapshotKey.lowercase(Locale.ROOT))}" +
                "&select=payload,payload_ciphertext,encrypted,updated_at&order=updated_at.desc&limit=1"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .get()
                .build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return Result.failure(Exception("HTTP ${response.code}"))
                val arr = JSONArray(response.body?.string().orEmpty().ifBlank { "[]" })
                if (arr.length() == 0) return Result.failure(Exception("Snapshot não encontrado."))
                Result.success(decodeSnapshotRecord(arr.getJSONObject(0), passPhrase))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchSnapshotViaProxy(userId: String, domain: String, snapshotKey: String, passPhrase: String?): Result<JSONObject> {
        return try {
            val url = "$valoraeProxyUrl/api/sync?action=get_snapshot&userId=${enc(userId)}&domain=${enc(domain)}&snapshotKey=${enc(snapshotKey)}"
            val req = Request.Builder()
                .url(url)
                .addHeader("x-valorae-sync-token", proxySyncToken)
                .get()
                .build()
            client.newCall(req).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) return Result.failure(Exception("Proxy HTTP ${response.code}: ${text.take(240)}"))
                val obj = JSONObject(text)
                val record = obj.optJSONObject("record") ?: obj
                Result.success(decodeSnapshotRecord(record, passPhrase))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decodeSnapshotRecord(record: JSONObject, passPhrase: String?): JSONObject {
        val encrypted = record.optBoolean("encrypted", false)
        if (encrypted) {
            val secret = passPhrase?.takeIf { it.isNotBlank() } ?: autoEncryptionSecret.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Snapshot criptografado. Informe senha/PIN de backup.")
            val cipher = record.optString("payload_ciphertext", "")
            return JSONObject(CryptoHelper.decrypt(cipher, secret))
        }
        return record.optJSONObject("payload") ?: JSONObject(record.optString("payload", "{}"))
    }

    fun transactionsToJsonArray(transactions: List<Transaction>): JSONArray {
        val arr = JSONArray()
        transactions.forEach { tx ->
            arr.put(JSONObject().apply {
                put("id", tx.id)
                put("ticker", tx.ticker)
                put("name", tx.name)
                put("quantity", tx.quantity)
                put("purchasePrice", tx.purchasePrice)
                put("date", tx.date)
                put("type", tx.type)
                put("isSell", tx.isSell)
                put("broker", tx.broker)
                put("sector", tx.sector)
                put("notes", tx.notes)
            })
        }
        return arr
    }

    fun transactionsFromJsonArray(itemsArray: JSONArray): List<Transaction> {
        val list = mutableListOf<Transaction>()
        for (i in 0 until itemsArray.length()) {
            val item = itemsArray.optJSONObject(i) ?: continue
            val ticker = item.optString("ticker", item.optString("symbol", "")).trim().uppercase(Locale.ROOT).replace(".SA", "")
            if (ticker.isBlank()) continue
            list.add(
                Transaction(
                    id = item.optInt("id", 0),
                    ticker = ticker,
                    name = item.optString("name", ticker),
                    quantity = item.optDouble("quantity", 0.0),
                    purchasePrice = item.optDouble("purchasePrice", item.optDouble("price", 0.0)),
                    date = item.optLong("date", System.currentTimeMillis()),
                    type = item.optString("type", if (B3NetworkService.inferIsFii(ticker)) "FII" else "ACAO"),
                    isSell = item.optBoolean("isSell", false),
                    broker = item.optString("broker", ""),
                    sector = item.optString("sector", ""),
                    notes = item.optString("notes", "")
                )
            )
        }
        return list
    }

    object CryptoHelper {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val TAG_LENGTH = 128
        private const val IV_LENGTH = 12

        fun encrypt(plainText: String, secret: String): String {
            val iv = ByteArray(IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        }

        fun decrypt(cipherTextBase64: String, secret: String): String {
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            require(combined.size > IV_LENGTH) { "Payload criptografado inválido." }
            val iv = ByteArray(IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            val cipherText = ByteArray(combined.size - iv.size)
            System.arraycopy(combined, iv.size, cipherText, 0, cipherText.size)
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))
            return String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }
    }
}
