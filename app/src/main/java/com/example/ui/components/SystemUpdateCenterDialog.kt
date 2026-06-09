package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.network.AppUpdateInfo
import com.example.network.UpdateManager
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemUpdateCenterDialog(
    updateManager: UpdateManager,
    updateStatus: UpdateManager.UpdateStatus,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var checkTriggeredByHand by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Determine current status (always real)
    val activeStatus = updateStatus

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Allow full-screen/modal feel
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Central de Atualizações",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Mantenha o VALORAE em sua melhor versão",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar Janela",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Identity Card & Version Badge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Versão Instalada",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Build Nº ${BuildConfig.VERSION_CODE}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        // Version Status Indicator Chip
                        Box(
                            modifier = Modifier
                                .background(
                                    when (activeStatus) {
                                        is UpdateManager.UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        is UpdateManager.UpdateStatus.ReadyToInstall -> SuccessGreen.copy(alpha = 0.18f)
                                        is UpdateManager.UpdateStatus.PreparingNativeInstaller,
                                        is UpdateManager.UpdateStatus.NativeInstallStarted,
                                        is UpdateManager.UpdateStatus.Validating -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                                        is UpdateManager.UpdateStatus.InstallPermissionRequired -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                        is UpdateManager.UpdateStatus.Downloading -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                        is UpdateManager.UpdateStatus.Checking -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        else -> SuccessGreen.copy(alpha = 0.12f)
                                    },
                                    RoundedCornerShape(32.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            when (activeStatus) {
                                                is UpdateManager.UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.primary
                                                is UpdateManager.UpdateStatus.ReadyToInstall -> SuccessGreen
                                                is UpdateManager.UpdateStatus.PreparingNativeInstaller,
                                                is UpdateManager.UpdateStatus.NativeInstallStarted,
                                                is UpdateManager.UpdateStatus.Validating -> MaterialTheme.colorScheme.tertiary
                                                is UpdateManager.UpdateStatus.InstallPermissionRequired -> MaterialTheme.colorScheme.error
                                                is UpdateManager.UpdateStatus.Downloading -> Color(0xFF3B82F6)
                                                is UpdateManager.UpdateStatus.Checking -> MaterialTheme.colorScheme.onSurface
                                                else -> SuccessGreen
                                            },
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (activeStatus) {
                                        is UpdateManager.UpdateStatus.UpdateAvailable -> "Pendente"
                                        is UpdateManager.UpdateStatus.ReadyToInstall -> "Pronto"
                                        is UpdateManager.UpdateStatus.Validating -> "Validando"
                                        is UpdateManager.UpdateStatus.PreparingNativeInstaller -> "Nativo"
                                        is UpdateManager.UpdateStatus.NativeInstallStarted -> "Instalador"
                                        is UpdateManager.UpdateStatus.InstallPermissionRequired -> "Permissão"
                                        is UpdateManager.UpdateStatus.Downloading -> "Baixando"
                                        is UpdateManager.UpdateStatus.Checking -> "Checando..."
                                        else -> "Atualizado"
                                    },
                                    color = when (activeStatus) {
                                        is UpdateManager.UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.primary
                                        is UpdateManager.UpdateStatus.ReadyToInstall -> SuccessGreen
                                        is UpdateManager.UpdateStatus.PreparingNativeInstaller,
                                        is UpdateManager.UpdateStatus.NativeInstallStarted,
                                        is UpdateManager.UpdateStatus.Validating -> MaterialTheme.colorScheme.tertiary
                                        is UpdateManager.UpdateStatus.InstallPermissionRequired -> MaterialTheme.colorScheme.error
                                        is UpdateManager.UpdateStatus.Downloading -> Color(0xFF3B82F6)
                                        is UpdateManager.UpdateStatus.Checking -> MaterialTheme.colorScheme.onSurface
                                        else -> SuccessGreen
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Core State Dynamic Layout
                Crossfade(targetState = activeStatus, label = "UpdateStatusCrossfade") { status ->
                    when (status) {
                        is UpdateManager.UpdateStatus.Checking -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Buscando atualizações de segurança...",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Conectando ao canal de produção estável...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        is UpdateManager.UpdateStatus.UpdateAvailable -> {
                            val info = status.info
                            UpdateDetailsCard(
                                info = info,
                                onConfirmDownload = {
                                    if (false) {
                                        // No simulation
                                    } else {
                                        updateManager.startDownload(info)
                                    }
                                }
                            )
                        }

                        is UpdateManager.UpdateStatus.Downloading -> {
                            val info = status.info
                            val progress = status.progressPercent
                            val downloadedMb = status.downloadedBytes / (1024.0 * 1024.0)
                            val totalMb = status.totalBytes?.let { it / (1024.0 * 1024.0) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Baixando APK em cache interno",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Versão de destino: ${info.versionName}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = progress?.let { "$it%" } ?: "Baixando...",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                if (progress != null) {
                                    LinearProgressIndicator(
                                        progress = { progress / 100f },
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (totalMb != null) "${"%.1f".format(downloadedMb)} / ${"%.1f".format(totalMb)} MB" else "${"%.1f".format(downloadedMb)} MB baixados",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Sem salvar na pasta Downloads",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        is UpdateManager.UpdateStatus.Validating -> {
                            NativeInstallStageCard(
                                title = "Validando pacote de atualização",
                                subtitle = "Conferindo formato APK, pacote do VALORAE, versionCode e SHA-256 quando informado pelo Vercel.",
                                icon = Icons.Default.Verified
                            )
                        }

                        is UpdateManager.UpdateStatus.PreparingNativeInstaller -> {
                            NativeInstallStageCard(
                                title = "Preparando instalador nativo",
                                subtitle = "O Android está recebendo o APK via PackageInstaller.Session. Se necessário, a confirmação oficial do sistema será aberta em seguida.",
                                icon = Icons.Default.SystemUpdate
                            )
                        }

                        is UpdateManager.UpdateStatus.NativeInstallStarted -> {
                            NativeInstallStageCard(
                                title = "Instalador nativo iniciado",
                                subtitle = "Confirme a atualização na tela oficial do Android. A substituição só acontece se o APK estiver assinado com a mesma chave do VALORAE instalado.",
                                icon = Icons.Default.InstallMobile
                            )
                        }

                        is UpdateManager.UpdateStatus.ReadyToInstall -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Pacote de atualização pronto",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "O APK foi salvo no cache do app, validado e preparado para instalação nativa. Ele não foi enviado para a pasta pública Downloads.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = { updateManager.installDownloadedApk() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Abrir instalador nativo do Android", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        is UpdateManager.UpdateStatus.InstallPermissionRequired -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Permissão de instalação necessária",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "O Android exige que você autorize este app como fonte confiável antes de instalar APKs fora da Play Store.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = { updateManager.openInstallPermissionSettings() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Autorizar instalação deste app", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { updateManager.installDownloadedApk() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Já autorizei, tentar instalar novamente", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        is UpdateManager.UpdateStatus.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Falha ao Sincronizar Canais",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = status.message,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        is UpdateManager.UpdateStatus.UpToDate,
                        is UpdateManager.UpdateStatus.Idle -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(SuccessGreen.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Seu aplicativo está 100% atualizado",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Você já está rodando a última versão pública com segurança contra vulnerabilidades e cotações em tempo real.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        checkTriggeredByHand = true


                                        coroutineScope.launch {

                                            updateManager.checkForUpdate(BuildConfig.VALORAE_UPDATE_MANIFEST_URL, force = true)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Verificar Manualmente", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}


@Composable
private fun NativeInstallStageCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        LinearProgressIndicator(
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun UpdateDetailsCard(
    info: AppUpdateInfo,
    onConfirmDownload: () -> Unit
) {
    var isChangelogExpanded by remember { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Nova Versão Encontrada",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Text(
                    text = "Lançamento Oficial: Versão ${info.versionName}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                val releaseDate = info.normalizedReleaseDate
                if (!releaseDate.isNullOrEmpty()) {
                    Text(
                        text = "Data: ${releaseDate.take(10)}", // Just show YYYY-MM-DD
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isChangelogExpanded = !isChangelogExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "O que há de novo nesta versão",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.3.sp
            )
            Icon(
                imageVector = if (isChangelogExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isChangelogExpanded) "Esconder novidades" else "Mostrar novidades",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = isChangelogExpanded) {
            // Simple render of release notes with custom bullet lines - prefer 'changelog' structure
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                when {
                    !info.changelog.isNullOrEmpty() -> {
                        info.changelog.forEach { line ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp, end = 10.dp)
                                        .size(5.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Text(
                                    text = line.trim(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    !info.features.isNullOrEmpty() -> {
                        info.features.forEach { feature ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp, end = 10.dp)
                                        .size(6.dp)
                                        .background(
                                            when (feature.type) {
                                                "fix" -> MaterialTheme.colorScheme.error
                                                "design" -> MaterialTheme.colorScheme.secondary
                                                else -> MaterialTheme.colorScheme.primary
                                            },
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = feature.text,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    else -> {
                        val notes = info.normalizedReleaseNotes?.split("\n") ?: listOf("Melhorias cosméticas e correções gerais do app")
                        notes.filter { it.isNotBlank() }.forEach { note ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp, end = 10.dp)
                                        .size(5.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Text(
                                    text = note.trim().removePrefix("•").trim(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onConfirmDownload,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Baixar e Atualizar Agora ${if (!info.normalizedFileSize.isNullOrEmpty()) "(${info.normalizedFileSize})" else ""}", fontWeight = FontWeight.Bold)
        }
    }
}
