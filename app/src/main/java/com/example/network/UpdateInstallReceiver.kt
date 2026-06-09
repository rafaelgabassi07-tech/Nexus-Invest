package com.example.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast

class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
        val versionName = intent.getStringExtra(EXTRA_VERSION_NAME).orEmpty()

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmationIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
                }
                confirmationIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirmationIntent != null) {
                    context.startActivity(confirmationIntent)
                } else {
                    Toast.makeText(context, "Não foi possível abrir a confirmação nativa de instalação.", Toast.LENGTH_LONG).show()
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                val suffix = if (versionName.isNotBlank()) " $versionName" else ""
                Toast.makeText(context, "Atualização VALORAE$suffix instalada com sucesso.", Toast.LENGTH_LONG).show()
            }
            else -> {
                val readable = when (status) {
                    PackageInstaller.STATUS_FAILURE_ABORTED -> "Instalação cancelada."
                    PackageInstaller.STATUS_FAILURE_BLOCKED -> "Instalação bloqueada pelo Android."
                    PackageInstaller.STATUS_FAILURE_CONFLICT -> "Conflito com o app instalado. Verifique se a assinatura é a mesma."
                    PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "APK incompatível com este dispositivo."
                    PackageInstaller.STATUS_FAILURE_INVALID -> "APK inválido ou corrompido."
                    PackageInstaller.STATUS_FAILURE_STORAGE -> "Espaço insuficiente para instalar a atualização."
                    else -> "Falha ao instalar a atualização."
                }
                Toast.makeText(
                    context,
                    if (message.isBlank()) readable else "$readable $message",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_COMMIT = "com.example.network.VALORAE_PACKAGE_INSTALL_COMMIT"
        const val EXTRA_VERSION_NAME = "extra_version_name"
        const val EXTRA_VERSION_CODE = "extra_version_code"
        const val EXTRA_APK_PATH = "extra_apk_path"
    }
}
