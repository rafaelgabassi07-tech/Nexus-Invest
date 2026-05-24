package com.example.network

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class UpdateManager(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private var downloadId: Long = -1

    private var currentRetrofit: Retrofit? = null
    private var currentApi: UpdateApiService? = null

    suspend fun checkForUpdate(fullUrl: String) {
        try {
            _updateStatus.value = UpdateStatus.Checking
            
            if (fullUrl.isEmpty() || fullUrl.contains("seusite-atualizacao.vercel.app")) {
                _updateStatus.value = UpdateStatus.UpToDate
                return
            }
            
            // Handle cases where the full URL to the json file is passed
            val baseUrl = if (fullUrl.endsWith("/update.json")) {
                fullUrl.substring(0, fullUrl.lastIndexOf("/update.json") + 1)
            } else {
                if (fullUrl.endsWith("/")) fullUrl else "$fullUrl/"
            }
            
            // Rebuild only if baseUrl changes
            if (currentRetrofit?.baseUrl()?.toString() != baseUrl) {
                currentRetrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                currentApi = currentRetrofit!!.create(UpdateApiService::class.java)
            }

            val api = currentApi ?: return
            val updateInfo = api.getLatestUpdateInfo(
                timestamp = System.currentTimeMillis(),
                cacheControl = "no-cache, no-store, must-revalidate"
            )
            android.util.Log.d("UpdateManager", "Update info received: $updateInfo")

            if (updateInfo.versionCode > BuildConfig.VERSION_CODE) {
                _updateStatus.value = UpdateStatus.UpdateAvailable(updateInfo)
            } else {
                _updateStatus.value = UpdateStatus.UpToDate
            }
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Erro ao checar atualizações")
        }
    }

    fun startDownload(updateInfo: AppUpdateInfo) {
        _updateStatus.value = UpdateStatus.Downloading(updateInfo)

        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
            .setTitle("Atualizando app...")
            .setDescription("Baixando nova versão: ${updateInfo.versionName}")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app_update_${updateInfo.versionName}.apk")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId == id) {
                    _updateStatus.value = UpdateStatus.ReadyToInstall
                    installApk(downloadId)
                    try {
                        ctx.unregisterReceiver(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        
        ContextCompat.registerReceiver(
            context,
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun installApk(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = downloadManager.getUriForDownloadedFile(downloadId) ?: return

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("VALORAE", "Erro ao iniciar instalação de APK: ${e.message}")
        }
    }

    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }

    sealed class UpdateStatus {
        object Idle : UpdateStatus()
        object Checking : UpdateStatus()
        data class UpdateAvailable(val info: AppUpdateInfo) : UpdateStatus()
        object UpToDate : UpdateStatus()
        data class Downloading(val info: AppUpdateInfo) : UpdateStatus()
        object ReadyToInstall : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
    }
}
