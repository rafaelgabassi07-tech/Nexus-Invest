package com.example.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.Transaction
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CloudSyncManager handles secure operations to back up and restore investment portfolios (Transactions)
 * to a cloud Supabase PostgreSQL database.
 * Includes complete implementation of client-side Zero-Knowledge AES-256-GCM encryption.
 */
object CloudSyncManager {
    private const val TAG = "CloudSyncManager"

    // Retrieve environment variables through BuildConfig injected via AI Studio secrets
    val supabaseUrl: String = try { BuildConfig.SUPABASE_URL } catch (e: Exception) { "" }
    val supabaseKey: String = try { BuildConfig.SUPABASE_ANON_KEY } catch (e: Exception) { "" }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Helper to verify if cloud configurations are active.
     */
    fun isCloudConfigured(): Boolean {
        // Desativado por política do VALORAE APK: a carteira não deve depender nem oferecer
        // integração direta com banco externo/serviço potencialmente pago. Backups devem ser
        // locais ou passar por contratos seguros do Valorae Proxy em implementação futura.
        return false
    }

    /**
     * Check connections asynchronously.
     */
    suspend fun testSupabaseConnection(): Result<String> = withContext(Dispatchers.IO) {
        if (!isCloudConfigured()) {
            return@withContext Result.failure(Exception("Nenhum serviço de nuvem (Supabase) está configurado. Adicione suas chaves no painel de Segredos."))
        }
        
        // Query metadata from REST API to verify credentials
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .get()
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404 || response.code == 200) {
                    Result.success("Conexão direta estabelecida com sucesso com o Supabase!")
                } else {
                    Result.failure(Exception("Supabase retornou erro HTTP: ${response.code} (Verifique as credenciais)"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Falha na conexão de rede: ${e.message}"))
        }
    }

    /**
     * Backup all transactions in JSON format.
     * Integrates Client-Side AES-256-GCM encryption if passPhrase is provided.
     * Routes through standard Supabase direct call.
     */
    suspend fun backupData(
        userId: String,
        transactions: List<Transaction>,
        passPhrase: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isCloudConfigured()) {
            return@withContext Result.failure(Exception("Nenhum serviço de nuvem está configurado."))
        }

        try {
            // Serialize transactions list to JSON
            val jsonArray = JSONArray()
            transactions.forEach { trans ->
                val obj = JSONObject().apply {
                    put("id", trans.id)
                    put("ticker", trans.ticker)
                    put("name", trans.name)
                    put("quantity", trans.quantity)
                    put("purchasePrice", trans.purchasePrice)
                    put("date", trans.date)
                    put("type", trans.type)
                    put("isSell", trans.isSell)
                    put("broker", trans.broker)
                    put("sector", trans.sector)
                    put("notes", trans.notes)
                }
                jsonArray.put(obj)
            }

            val serializedJson = jsonArray.toString()
            val finalPayload: String = if (!passPhrase.isNullOrBlank()) {
                // Apply Client-side Zero-Knowledge Encryption (Ciphertext only leaves device)
                CryptoHelper.encrypt(serializedJson, passPhrase)
            } else {
                serializedJson
            }
            val isEncrypted = !passPhrase.isNullOrBlank()

            // Database row payload
            val jsonBody = JSONObject().apply {
                put("user_id", userId)
                put("payload", finalPayload)
                put("encrypted", isEncrypted)
                put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)

            // Supabase Direct Upsert
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/valorae_sync_backups")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Backup sincronizado na nuvem com sucesso!")
                } else if (response.code == 404 || response.code == 400) {
                    Result.failure(Exception("Tabela 'valorae_sync_backups' não localizada no seu Supabase. Verifique se executou os scripts de migração SQL."))
                } else {
                    Result.failure(Exception("Erro ao salvar: ${response.code} - ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ", e)
            Result.failure(e)
        }
    }

    /**
     * Download and restore transactions list from Supabase cloud repository.
     */
    suspend fun restoreData(
        userId: String,
        passPhrase: String? = null
    ): Result<List<Transaction>> = withContext(Dispatchers.IO) {
        if (!isCloudConfigured()) {
            return@withContext Result.failure(Exception("Nenhum serviço de nuvem está configurado."))
        }

        // Direct call
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/valorae_sync_backups?user_id=eq.$userId")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Erro ao buscar dados: HTTP ${response.code}"))
                }

                val bodyStr = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(bodyStr)
                if (jsonArray.length() == 0) {
                    return@withContext Result.failure(Exception("Nenhum backup encontrado para o usuário id '$userId'."))
                }

                val record = jsonArray.getJSONObject(0)
                val payload = record.getString("payload")
                val isEncrypted = record.optBoolean("encrypted", false)

                val decryptedJson: String
                if (isEncrypted) {
                    if (passPhrase.isNullOrBlank()) {
                        return@withContext Result.failure(Exception("Estes dados estão criptografados. É necessário informar o PIN/Senha de backup para restaurar."))
                    }
                    try {
                        decryptedJson = CryptoHelper.decrypt(payload, passPhrase)
                    } catch (cryptoEx: Exception) {
                        return@withContext Result.failure(Exception("PIN ou Senha de backup incorreta. Não foi possível descriptografar os dados na nuvem."))
                    }
                } else {
                    decryptedJson = payload
                }

                val transactionsList = mutableListOf<Transaction>()
                val itemsArray = JSONArray(decryptedJson)
                for (i in 0 until itemsArray.length()) {
                    val item = itemsArray.getJSONObject(i)
                    transactionsList.add(
                        Transaction(
                            id = item.optInt("id", 0),
                            ticker = item.getString("ticker"),
                            name = item.optString("name", ""),
                            quantity = item.getDouble("quantity"),
                            purchasePrice = item.getDouble("purchasePrice"),
                            date = item.optLong("date", System.currentTimeMillis()),
                            type = item.optString("type", "ACAO"),
                            isSell = item.optBoolean("isSell", false),
                            broker = item.optString("broker", ""),
                            sector = item.optString("sector", ""),
                            notes = item.optString("notes", "")
                        )
                    )
                }

                Result.success(transactionsList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Restore error: ", e)
            Result.failure(e)
        }
    }

    /**
     * Zero-Knowledge AES-256-GCM symmetric encryption helper class.
     */
    object CryptoHelper {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val TAG_LENGTH = 128
        private const val IV_LENGTH = 12

        fun encrypt(plainText: String, secret: String): String {
            val iv = ByteArray(IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val keyBytes = secret.toByteArray(Charsets.UTF_8).let { bytes ->
                MessageDigest.getInstance("SHA-256").digest(bytes)
            }
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
            val iv = ByteArray(IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            val cipherText = ByteArray(combined.size - iv.size)
            System.arraycopy(combined, iv.size, cipherText, 0, cipherText.size)
            val keyBytes = secret.toByteArray(Charsets.UTF_8).let { bytes ->
                MessageDigest.getInstance("SHA-256").digest(bytes)
            }
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))
            val plainTextBytes = cipher.doFinal(cipherText)
            return String(plainTextBytes, Charsets.UTF_8)
        }
    }
}

