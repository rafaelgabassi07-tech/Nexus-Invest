package com.example.network

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class UpdateManager(context: Context) {

    private val context: Context = context.applicationContext

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    @Volatile
    private var lastDownloadedApk: File? = null

    private companion object {
        const val UPDATE_CHECK_PREFS = "valorae_update_check"
        const val LAST_UPDATE_CHECK_AT = "last_update_check_at"
        const val LAST_DOWNLOADED_APK_PATH = "last_downloaded_apk_path"
        const val LAST_DOWNLOADED_VERSION_CODE = "last_downloaded_version_code"
        const val LAST_DOWNLOADED_SHA256 = "last_downloaded_sha256"
        const val UPDATE_CHECK_TTL_MS = 3 * 60 * 60 * 1000L
        const val UPDATE_CACHE_DIR = "valorae_updates"
        const val APK_MIME = "application/vnd.android.package-archive"
        const val APK_WRITE_NAME = "valorae_update_base.apk"
    }

    init {
        cleanupOldApks(keepLatestPointer = true)
        restoreLastDownloadedApkPointer()
    }

    suspend fun checkForUpdate(fullUrl: String, force: Boolean = false) {
        try {
            cleanupOldApks(keepLatestPointer = true)
            val manifestUrl = normalizeManifestUrl(fullUrl)
            if (manifestUrl.isEmpty() || !manifestUrl.startsWith("https://")) {
                _updateStatus.value = UpdateStatus.UpToDate
                return
            }

            val prefs = context.getSharedPreferences(UPDATE_CHECK_PREFS, Context.MODE_PRIVATE)
            val lastCheckAt = prefs.getLong(LAST_UPDATE_CHECK_AT, 0L)
            val now = System.currentTimeMillis()
            if (!force && lastCheckAt > 0L && now - lastCheckAt < UPDATE_CHECK_TTL_MS) {
                _updateStatus.value = UpdateStatus.UpToDate
                return
            }

            _updateStatus.value = UpdateStatus.Checking

            val updateInfo = fetchUpdateInfoWithFallback(manifestUrl)
            android.util.Log.d("UpdateManager", "Update info received from $manifestUrl: $updateInfo")

            prefs.edit().putLong(LAST_UPDATE_CHECK_AT, now).apply()
            if (updateInfo.versionCode > BuildConfig.VERSION_CODE) {
                when {
                    !updateInfo.downloadUrl.startsWith("https://") -> {
                        _updateStatus.value = UpdateStatus.Error("Nova versão encontrada, mas a URL do APK é inválida ou não usa HTTPS.")
                    }
                    updateInfo.versionName.isBlank() -> {
                        _updateStatus.value = UpdateStatus.Error("Nova versão encontrada, mas o manifesto não informou versionName/latest_version.")
                    }
                    else -> {
                        _updateStatus.value = UpdateStatus.UpdateAvailable(updateInfo)
                    }
                }
            } else {
                _updateStatus.value = UpdateStatus.UpToDate
            }
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Erro ao checar atualizações")
        }
    }

    fun startDownload(updateInfo: AppUpdateInfo) {
        val downloadUrl = updateInfo.downloadUrl.trim()
        if (!downloadUrl.startsWith("https://")) {
            _updateStatus.value = UpdateStatus.Error("URL de download inválida: apenas HTTPS é permitido.")
            return
        }

        managerScope.launch {
            try {
                findReusableDownloadedApk(updateInfo)?.let { apk ->
                    lastDownloadedApk = apk
                    _updateStatus.value = UpdateStatus.ReadyToInstall(updateInfo, apk.absolutePath)
                    launchPackageInstaller(apk, updateInfo)
                    return@launch
                }

                cleanupOldApks()
                val destination = createUpdateApkFile(updateInfo)
                downloadApk(downloadUrl, destination, updateInfo)

                _updateStatus.value = UpdateStatus.Validating(updateInfo)
                validateDownloadedApk(destination, updateInfo)

                lastDownloadedApk = destination
                val calculatedSha = calculateSha256(destination)
                context.getSharedPreferences(UPDATE_CHECK_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(LAST_DOWNLOADED_APK_PATH, destination.absolutePath)
                    .putInt(LAST_DOWNLOADED_VERSION_CODE, updateInfo.versionCode)
                    .putString(LAST_DOWNLOADED_SHA256, calculatedSha)
                    .apply()

                _updateStatus.value = UpdateStatus.ReadyToInstall(updateInfo, destination.absolutePath)
                launchPackageInstaller(destination, updateInfo)
            } catch (e: Exception) {
                try { lastDownloadedApk?.delete() } catch (_: Exception) {}
                _updateStatus.value = UpdateStatus.Error(e.message ?: "Erro ao baixar atualização")
            }
        }
    }

    fun installDownloadedApk() {
        val status = _updateStatus.value
        val info = when (status) {
            is UpdateStatus.ReadyToInstall -> status.info
            is UpdateStatus.InstallPermissionRequired -> status.info
            is UpdateStatus.NativeInstallStarted -> status.info
            is UpdateStatus.UpdateAvailable -> status.info
            is UpdateStatus.Downloading -> status.info
            is UpdateStatus.Validating -> status.info
            is UpdateStatus.PreparingNativeInstaller -> status.info
            else -> null
        }
        val apk = lastDownloadedApk?.takeIf { it.exists() }
            ?: (status as? UpdateStatus.ReadyToInstall)?.apkPath?.let(::File)?.takeIf { it.exists() }
            ?: (status as? UpdateStatus.InstallPermissionRequired)?.apkPath?.let(::File)?.takeIf { it.exists() }
            ?: restoreLastDownloadedApkPointer()

        if (apk == null || !apk.exists()) {
            _updateStatus.value = UpdateStatus.Error("APK baixado não encontrado. Baixe a atualização novamente.")
            return
        }

        managerScope.launch {
            try {
                if (info != null) validateDownloadedApk(apk, info)
                launchPackageInstaller(apk, info)
            } catch (e: Exception) {
                _updateStatus.value = UpdateStatus.Error(e.message ?: "Não foi possível preparar a instalação.")
            }
        }
    }

    fun openInstallPermissionSettings() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error("Não foi possível abrir a permissão de instalação: ${e.message}")
        }
    }

    fun cleanupOldApks(keepLatestPointer: Boolean = false) {
        val protected = if (keepLatestPointer) {
            lastDownloadedApk?.absolutePath ?: context.getSharedPreferences(UPDATE_CHECK_PREFS, Context.MODE_PRIVATE)
                .getString(LAST_DOWNLOADED_APK_PATH, null)
        } else {
            null
        }
        listOfNotNull(context.externalCacheDir, context.cacheDir)
            .map { File(it, UPDATE_CACHE_DIR) }
            .forEach { dir ->
                if (!dir.exists()) return@forEach
                dir.listFiles { file -> file.isFile && file.extension.equals("apk", ignoreCase = true) }
                    ?.forEach { file ->
                        if (protected != null && file.absolutePath == protected) return@forEach
                        runCatching { file.delete() }
                    }
            }
    }

    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }

    fun close() {
        managerScope.cancel()
    }

    private suspend fun fetchUpdateInfoWithFallback(manifestUrl: String): AppUpdateInfo {
        val candidates = buildManifestCandidates(manifestUrl)
        var lastError: Exception? = null
        for (candidate in candidates) {
            try {
                return fetchUpdateInfo(candidate)
            } catch (e: Exception) {
                android.util.Log.w("UpdateManager", "Manifesto indisponível em $candidate", e)
                lastError = e
            }
        }
        throw lastError ?: IOException("Não foi possível consultar o manifesto de atualização.")
    }

    private suspend fun fetchUpdateInfo(manifestUrl: String): AppUpdateInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(cacheBust(manifestUrl))
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Falha HTTP ${response.code} ao consultar atualização em $manifestUrl.")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IOException("Manifesto de atualização vazio.")
            val adapter = moshi.adapter(AppUpdateInfo::class.java)
            adapter.fromJson(body) ?: throw IOException("Manifesto de atualização inválido.")
        }
    }

    private fun buildManifestCandidates(primary: String): List<String> {
        val clean = primary.trim()
        val root = runCatching {
            val parsed = Uri.parse(clean)
            val scheme = parsed.scheme ?: "https"
            val authority = parsed.authority.orEmpty()
            if (authority.isBlank()) "" else "$scheme://$authority/"
        }.getOrDefault("")

        return buildList {
            add(clean)
            if (root.isNotBlank()) {
                add(root + "api/update")
                add(root + "update.json")
                add(root + "version.json")
            }
        }.filter { it.isNotBlank() }.distinct()
    }

    private suspend fun downloadApk(url: String, destination: File, info: AppUpdateInfo) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
            .header("Cache-Control", "no-cache")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Falha HTTP ${response.code} ao baixar APK.")
            val body = response.body ?: throw IOException("Resposta de download vazia.")
            val contentLength = body.contentLength()
            val expectedSize = info.normalizedFileSizeBytes
            if (expectedSize != null && contentLength > 0 && contentLength != expectedSize) {
                throw IOException("O tamanho informado pelo servidor não bate com o manifesto de atualização.")
            }
            val usableSpace = destination.parentFile?.usableSpace ?: 0L
            val requiredSpace = maxOf(contentLength, expectedSize ?: 0L)
            if (requiredSpace > 0 && usableSpace > 0L && usableSpace < requiredSpace) {
                throw IOException("Espaço insuficiente para baixar a atualização.")
            }

            destination.parentFile?.mkdirs()
            var downloaded = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            body.byteStream().use { input ->
                destination.outputStream().use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val totalForProgress = if (contentLength > 0) contentLength else expectedSize ?: -1L
                        val progress = if (totalForProgress > 0) ((downloaded * 100) / totalForProgress).toInt().coerceIn(0, 100) else null
                        _updateStatus.value = UpdateStatus.Downloading(info, progress, downloaded, totalForProgress.takeIf { it > 0 })
                    }
                }
            }
        }
    }

    private fun launchPackageInstaller(apkFile: File, info: AppUpdateInfo?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val updateInfo = info ?: AppUpdateInfo()
            _updateStatus.value = UpdateStatus.InstallPermissionRequired(updateInfo, apkFile.absolutePath)
            openInstallPermissionSettings()
            return
        }

        _updateStatus.value = UpdateStatus.PreparingNativeInstaller(info ?: AppUpdateInfo(), apkFile.absolutePath)
        try {
            installWithNativePackageInstaller(apkFile, info)
        } catch (e: Exception) {
            android.util.Log.w("UpdateManager", "PackageInstaller.Session falhou; usando fallback FileProvider.", e)
            launchFileProviderInstaller(apkFile)
        }
    }

    private fun installWithNativePackageInstaller(apkFile: File, info: AppUpdateInfo?) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apkFile.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setInstallReason(PackageInstaller.INSTALL_REASON_USER)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }
        }

        val sessionId = packageInstaller.createSession(params)
        var session: PackageInstaller.Session? = null
        try {
            session = packageInstaller.openSession(sessionId)
            apkFile.inputStream().use { input ->
                session.openWrite(APK_WRITE_NAME, 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            val callbackIntent = Intent(context, UpdateInstallReceiver::class.java).apply {
                action = UpdateInstallReceiver.ACTION_INSTALL_COMMIT
                putExtra(UpdateInstallReceiver.EXTRA_VERSION_NAME, info?.versionName.orEmpty())
                putExtra(UpdateInstallReceiver.EXTRA_VERSION_CODE, info?.versionCode ?: 0)
                putExtra(UpdateInstallReceiver.EXTRA_APK_PATH, apkFile.absolutePath)
            }
            val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> android.app.PendingIntent.FLAG_MUTABLE
                else -> 0
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(context, sessionId, callbackIntent, flags)
            session.commit(pendingIntent.intentSender)
            _updateStatus.value = UpdateStatus.NativeInstallStarted(info ?: AppUpdateInfo(), sessionId)
        } catch (e: Exception) {
            runCatching { packageInstaller.abandonSession(sessionId) }
            throw e
        } finally {
            runCatching { session?.close() }
        }
    }

    private fun launchFileProviderInstaller(apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, APK_MIME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            _updateStatus.value = UpdateStatus.Error("Instalador de pacotes não encontrado neste dispositivo.")
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error("Erro ao iniciar instalação: ${e.message}")
        }
    }

    private fun restoreLastDownloadedApkPointer(): File? {
        val prefs = context.getSharedPreferences(UPDATE_CHECK_PREFS, Context.MODE_PRIVATE)
        val path = prefs.getString(LAST_DOWNLOADED_APK_PATH, null)
        val file = path?.let(::File)?.takeIf { it.exists() && it.extension.equals("apk", true) }
        lastDownloadedApk = file
        return file
    }

    private fun findReusableDownloadedApk(info: AppUpdateInfo): File? {
        val prefs = context.getSharedPreferences(UPDATE_CHECK_PREFS, Context.MODE_PRIVATE)
        val storedVersion = prefs.getInt(LAST_DOWNLOADED_VERSION_CODE, 0)
        if (storedVersion != info.versionCode) return null
        val file = restoreLastDownloadedApkPointer() ?: return null
        return runCatching {
            validateDownloadedApk(file, info)
            file
        }.getOrNull()
    }

    private fun createUpdateApkFile(info: AppUpdateInfo): File {
        val baseDir = File(context.externalCacheDir ?: context.cacheDir, UPDATE_CACHE_DIR)
        baseDir.mkdirs()
        val safeVersion = info.versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(baseDir, "valorae-update-v${safeVersion}-${info.versionCode}.apk")
    }

    private fun validateDownloadedApk(file: File, info: AppUpdateInfo) {
        if (!file.exists() || file.length() < 1024) {
            throw IOException("Arquivo APK baixado está vazio ou incompleto.")
        }
        info.normalizedFileSizeBytes?.let { expected ->
            if (expected > 0 && file.length() != expected) {
                file.delete()
                throw IOException("O tamanho do APK baixado não confere com o manifesto.")
            }
        }
        val header = file.inputStream().use { input -> ByteArray(4).also { input.read(it) } }
        val isZipApk = header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()
        if (!isZipApk) {
            file.delete()
            throw IOException("O arquivo baixado não parece ser um APK válido. Verifique a URL do GitHub Releases.")
        }

        info.normalizedSha256?.let { expected ->
            val actual = calculateSha256(file)
            if (!actual.equals(expected, ignoreCase = true)) {
                file.delete()
                throw IOException("A assinatura SHA-256 do APK não confere com o manifesto. Download bloqueado por segurança.")
            }
        }

        validatePackageMetadata(file, info)
    }

    private fun validatePackageMetadata(file: File, info: AppUpdateInfo) {
        val archiveInfo: PackageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            ?: throw IOException("Não foi possível ler os metadados do APK baixado.")
        if (archiveInfo.packageName != context.packageName) {
            file.delete()
            throw IOException("O APK baixado pertence ao pacote ${archiveInfo.packageName}, mas o VALORAE instalado usa ${context.packageName}.")
        }
        val archiveVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }
        if (archiveVersionCode <= BuildConfig.VERSION_CODE.toLong()) {
            file.delete()
            throw IOException("O APK baixado não é mais recente que a versão instalada.")
        }
        if (info.versionCode > 0 && archiveVersionCode != info.versionCode.toLong()) {
            file.delete()
            throw IOException("O versionCode do APK ($archiveVersionCode) não confere com o manifesto (${info.versionCode}).")
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun normalizeManifestUrl(raw: String): String {
        val clean = raw.trim()
        if (clean.isEmpty() || clean.contains("seusite-atualizacao.vercel.app")) return ""
        return when {
            clean.endsWith(".json", ignoreCase = true) -> clean
            clean.endsWith("/api/update", ignoreCase = true) -> clean
            clean.endsWith("/") -> "${clean}update.json"
            else -> "${clean}/update.json"
        }
    }

    private fun cacheBust(url: String): String {
        val separator = if (url.contains('?')) '&' else '?'
        return "$url${separator}t=${System.currentTimeMillis()}"
    }

    sealed class UpdateStatus {
        object Idle : UpdateStatus()
        object Checking : UpdateStatus()
        data class UpdateAvailable(val info: AppUpdateInfo) : UpdateStatus()
        object UpToDate : UpdateStatus()
        data class Downloading(
            val info: AppUpdateInfo,
            val progressPercent: Int? = null,
            val downloadedBytes: Long = 0L,
            val totalBytes: Long? = null
        ) : UpdateStatus()
        data class Validating(val info: AppUpdateInfo) : UpdateStatus()
        data class ReadyToInstall(val info: AppUpdateInfo, val apkPath: String) : UpdateStatus()
        data class PreparingNativeInstaller(val info: AppUpdateInfo, val apkPath: String) : UpdateStatus()
        data class NativeInstallStarted(val info: AppUpdateInfo, val sessionId: Int) : UpdateStatus()
        data class InstallPermissionRequired(val info: AppUpdateInfo, val apkPath: String) : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
    }
}
