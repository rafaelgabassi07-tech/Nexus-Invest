package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import java.util.UUID
import androidx.compose.foundation.lazy.LazyRow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.network.UpdateManager
import com.example.ui.theme.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.rememberCoroutineScope

import com.example.data.AppTheme
import com.example.data.FontScale
import com.example.data.CornerStyle
import com.example.data.DarkVariant
import com.example.data.LightVariant
import com.example.data.ThemePreferences

private enum class SettingsPage {
    MAIN, UPDATES, DISPLAY, ABOUT, SECURITY, NOTIFICATIONS, DATA_BACKUP, HELP, DARF_GUIDE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    updateManager: UpdateManager,
    updateStatus: UpdateManager.UpdateStatus,
    themePreferences: ThemePreferences,
    viewModel: com.example.viewmodel.PortfolioViewModel,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }

        Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (currentPage) {
                            SettingsPage.MAIN -> "Configurações"
                            SettingsPage.UPDATES -> "Atualizações"
                            SettingsPage.DISPLAY -> "Aparência"
                            SettingsPage.ABOUT -> "Sobre o VALORAE"
                            SettingsPage.SECURITY -> "Segurança"
                            SettingsPage.NOTIFICATIONS -> "Notificações"
                            SettingsPage.DATA_BACKUP -> "Backup e Dados"
                            SettingsPage.HELP -> "Central de Ajuda"
                            SettingsPage.DARF_GUIDE -> "Guia de DARF e Tributação"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentPage == SettingsPage.MAIN) {
                            onDismiss()
                        } else {
                            currentPage = SettingsPage.MAIN
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "SettingsTransition"
            ) { page ->
                when (page) {
                    SettingsPage.MAIN -> MainSettingsPage(
                        updateStatus = updateStatus,
                        onNavigate = { currentPage = it }
                    )
                    SettingsPage.UPDATES -> UpdatesSettingsPage(
                        updateManager = updateManager,
                        updateStatus = updateStatus
                    )
                    SettingsPage.DISPLAY -> DisplaySettingsPage(themePreferences)
                    SettingsPage.ABOUT -> AboutSettingsPage()
                    SettingsPage.SECURITY -> SecuritySettingsPage(themePreferences)
                    SettingsPage.NOTIFICATIONS -> NotificationsSettingsPage()
                    SettingsPage.DATA_BACKUP -> DataBackupPage(viewModel)
                    SettingsPage.HELP -> HelpGuidePage()
                    SettingsPage.DARF_GUIDE -> DarfGuidePage(viewModel)
                }
            }
        }
    }
}
}

@Composable
private fun MainSettingsPage(
    updateStatus: UpdateManager.UpdateStatus,
    onNavigate: (SettingsPage) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- PREFERÊNCIAS VISUAIS ---
        Text(
            text = "EXIBIÇÃO E PERSONALIZAÇÃO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = GoldPrimary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = "Aparência e Estilo",
                    subtitle = "Personalize modo escuro, cores e fontes",
                    onClick = { onNavigate(SettingsPage.DISPLAY) }
                )
                HorizontalDivider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.5.dp)
                SettingsItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notificações",
                    subtitle = "Alertas e Resumo Diário",
                    onClick = { onNavigate(SettingsPage.NOTIFICATIONS) }
                )
            }
        }

        // --- PROTEÇÃO E DADOS ---
        Text(
            text = "PROTEÇÃO E DADOS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = GoldPrimary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Outlined.Security,
                    title = "Segurança e Privacidade",
                    subtitle = "Face ID, PIN e Ocultação de Valores",
                    onClick = { onNavigate(SettingsPage.SECURITY) }
                )
                HorizontalDivider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.5.dp)
                SettingsItem(
                    icon = Icons.Outlined.CloudUpload,
                    title = "Backup e Importação B3",
                    subtitle = "Exportar, importar e ler planilhas",
                    onClick = { onNavigate(SettingsPage.DATA_BACKUP) }
                )
            }
        }

        // --- SISTEMA ---
        Text(
            text = "SISTEMA E SUPORTE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = GoldPrimary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Outlined.SystemUpdate,
                    title = "Versão e Atualizações",
                    subtitle = if (updateStatus is UpdateManager.UpdateStatus.UpdateAvailable) 
                        "NOVA VERSÃO DISPONÍVEL" else "Sua plataforma está atualizada",
                    hasBadge = updateStatus is UpdateManager.UpdateStatus.UpdateAvailable,
                    onClick = { onNavigate(SettingsPage.UPDATES) }
                )
                HorizontalDivider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.5.dp)
                SettingsItem(
                    icon = Icons.Outlined.CollectionsBookmark,
                    title = "Central de Ajuda",
                    subtitle = "Dicas de uso e suporte técnico",
                    onClick = { onNavigate(SettingsPage.HELP) }
                )
                HorizontalDivider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.5.dp)
                SettingsItem(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Guia de Tributação (DARF)",
                    subtitle = "Impostos sobre Ações e FIIs",
                    onClick = { onNavigate(SettingsPage.DARF_GUIDE) }
                )
                HorizontalDivider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.5.dp)
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "Sobre o VALORAE",
                    subtitle = "Especificações e propósito da plataforma",
                    onClick = { onNavigate(SettingsPage.ABOUT) }
                )
            }
        }
        
        // --- SOCIAL ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .clickable {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val packageName = context.packageName
                            val originalApkFile = java.io.File(context.applicationInfo.sourceDir)
                            
                            // Copy APK to a temporary file named VALORAE.apk for a better sharing experience
                            val shareFile = java.io.File(context.cacheDir, "VALORAE.apk")
                            if (!shareFile.exists() || shareFile.length() != originalApkFile.length()) {
                                originalApkFile.copyTo(shareFile, overwrite = true)
                            }

                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "$packageName.fileprovider",
                                shareFile
                            )
                            
                            withContext(Dispatchers.Main) {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_TEXT, "🎯 Olá! Convido você a conhecer o VALORAE: A plataforma definitiva para gestão de patrimônio e investimentos B3. Controle seu preço médio, dividendos e performance com design premium. Baixe o instalador oficial acima!")
                                    type = "application/vnd.android.package-archive"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Distribuir VALORAE (APK)"))
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("VALORAE", "Erro sharing APK: ${e.message}")
                            withContext(Dispatchers.Main) {
                                try {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "🎯 Olá! Evolua sua gestão de ativos com o VALORAE. O controle total dos seus investimentos B3 em um só lugar! Acesse a plataforma oficial.")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, null))
                                } catch (innerEx: Exception) {
                                    android.util.Log.e("VALORAE", "Erro sharing text fallback: ${innerEx.message}")
                                }
                            }
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fidelidade VALORAE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Convide amigos e expanda a comunidade",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun UpdatesSettingsPage(
    updateManager: UpdateManager,
    updateStatus: UpdateManager.UpdateStatus
) {
    val coroutineScope = rememberCoroutineScope()
    var lastCheckTime by remember {
        mutableStateOf(java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    if (updateStatus is UpdateManager.UpdateStatus.UpdateAvailable) 
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    else 
                        GoldPrimary.copy(alpha = 0.08f), 
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (updateStatus is UpdateManager.UpdateStatus.UpdateAvailable) 
                    Icons.Outlined.SystemUpdate 
                else 
                    Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (updateStatus is UpdateManager.UpdateStatus.UpdateAvailable) 
                    MaterialTheme.colorScheme.error 
                else 
                    GoldPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = if (updateStatus is UpdateManager.UpdateStatus.UpdateAvailable) 
                "Patch de Performance Pronto" 
            else 
                "Plataforma Atualizada",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Sua versão instalada: v${BuildConfig.VERSION_NAME} (Estável)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                val statusTitle = when (updateStatus) {
                    is UpdateManager.UpdateStatus.UpdateAvailable -> "Otimização Disponível"
                    is UpdateManager.UpdateStatus.Checking -> "Sincronizando barramento..."
                    is UpdateManager.UpdateStatus.Downloading -> "Baixando novos cabeçalhos..."
                    else -> "Plataforma Atualizada e Segura"
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (updateStatus is UpdateManager.UpdateStatus.UpdateAvailable) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    SuccessGreen, 
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val description = when (updateStatus) {
                    is UpdateManager.UpdateStatus.UpdateAvailable -> "O sistema detectou um patch corretivo que aprimora os algoritmos de valuation e acelera a renderização dos gráficos de rentabilidade."
                    is UpdateManager.UpdateStatus.Checking -> "Aguarde enquanto verificamos a integridade dos dados locais estruturados nos repositórios institucionais da nuvem."
                    is UpdateManager.UpdateStatus.Downloading -> "Sincronizando as bibliotecas financeiras mais robustas para garantir cálculos lógicos impecáveis."
                    else -> "Todos os módulos analíticos, banco de dados Room persistido e rotinas tributárias estão operando perfeitamente nos melhores padrões de segurança local 'zero-cloud'."
                }
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                
                if (updateStatus is UpdateManager.UpdateStatus.UpdateAvailable) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { 
                            updateManager.startDownload(updateStatus.info)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Instalar Patch de Otimização",
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
        
        if (updateStatus !is UpdateManager.UpdateStatus.UpdateAvailable) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Última verificação automática realizada hoje às $lastCheckTime",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // --- CHANGELOG SECTION ---
        Text(
            text = "NOTAS DA VERSÃO (CHANGELOG)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )
        
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ChangelogItem(
                    version = "1.1.0",
                    date = "Maio 2026",
                    changes = listOf(
                        "🎨 Interface e UX (Design)",
                        "Otimização de Espaço: Redução geral de padding, espaçamentos e ajustes de fontes para melhorar a densidade de informações.",
                        "Cards Consolidados: O card de Patrimônio Consolidado na Dashboard foi reduzido em altura para melhor aproveitamento da tela.",
                        "Listas Modernizadas: Redesign completo dos cards de Meus Ativos e Histórico de Transações para um visual mais limpo e técnico.",
                        "⚙️ Funcionalidades e Fluxos",
                        "Organização de Carteira: Ativos na Dashboard agora são agrupados automaticamente em Ações e FIIs, com cabeçalhos dedicados.",
                        "Histórico de Compras: Organizado por mês e ano, exibindo subtotais automáticos para cada período.",
                        "Segurança: Caixa de diálogo de confirmação adicionada antes da exclusão de transações.",
                        "Busca Aprimorada: Suporte a gatilho direto de pesquisa via teclado na tela de análise.",
                        "📊 Gráficos e Estatísticas",
                        "Interatividade: Gráficos agora respondem ao toque com painéis dinâmicos de Recebido vs. A Receber.",
                        "Visualização Moderna: Diversificação da carteira apresentada através de Pie Charts dinâmicos.",
                        "Filtros Inteligentes: Seletores de período e filtros atualizam os dados dos gráficos em tempo real.",
                        "Ajustes Visuais: Palette institucional refinada (GoldPrimary) e melhor distinção entre valores recebidos e projetados.",
                        "📂 Dados e Importação (Planilhas B3)",
                        "Leitura Robusta: Motor refatorado para respeitar coordenadas exatas de células e aumentar a resiliência a formatos diversos.",
                        "Identificação Inteligente: Melhorada a extração de tickers e expandido o mapeamento de cabeçalhos.",
                        "Scraping e Resiliência: Motor de captura mais robusto contra bloqueios de rede, com tratamento automático para ETFs."
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun ChangelogItem(version: String, date: String, changes: List<String>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Versão $version",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        changes.forEach { change ->
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = change,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun DisplaySettingsPage(themePreferences: ThemePreferences) {
    val currentTheme by themePreferences.theme.collectAsStateWithLifecycle(AppTheme.SYSTEM)
    val fontScale by themePreferences.fontScale.collectAsStateWithLifecycle(FontScale.MEDIUM)
    val cornerStyle by themePreferences.cornerStyle.collectAsStateWithLifecycle(CornerStyle.MODERN)
    val darkVariant by themePreferences.darkVariant.collectAsStateWithLifecycle(DarkVariant.CARBON)
    val lightVariant by themePreferences.lightVariant.collectAsStateWithLifecycle(LightVariant.CLASSIC)
    
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "MODO DE EXIBIÇÃO",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val themes = listOf(
                AppTheme.SYSTEM to "Auto",
                AppTheme.DARK to "Escuro",
                AppTheme.LIGHT to "Claro"
            )
            themes.forEach { (theme, label) ->
                val isSelected = currentTheme == theme
                Surface(
                    onClick = { scope.launch { themePreferences.setTheme(theme) } },
                    modifier = Modifier.weight(1f),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when(theme) {
                                AppTheme.SYSTEM -> Icons.Outlined.SettingsSuggest
                                AppTheme.DARK -> Icons.Outlined.DarkMode
                                AppTheme.LIGHT -> Icons.Outlined.LightMode
                            },
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (currentTheme != AppTheme.LIGHT) {
            Text(
                text = "VARIANTE DO TEMA ESCURO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkVariant.entries.forEach { variant ->
                    val isSelected = darkVariant == variant
                    OutlinedButton(
                        onClick = { scope.launch { themePreferences.setDarkVariant(variant) } },
                        border = BorderStroke(
                            1.dp, 
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(variant.name, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        if (currentTheme != AppTheme.DARK) {
            Text(
                text = "VARIANTE DO TEMA CLARO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LightVariant.entries.forEach { variant ->
                    val isSelected = lightVariant == variant
                    OutlinedButton(
                        onClick = { scope.launch { themePreferences.setLightVariant(variant) } },
                        border = BorderStroke(
                            1.dp, 
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(variant.name, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        Text(
            text = "ESTILO DOS CANTOS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CornerStyle.entries.forEach { style ->
                val isSelected = cornerStyle == style
                OutlinedButton(
                    onClick = { scope.launch { themePreferences.setCornerStyle(style) } },
                    border = BorderStroke(
                        1.dp, 
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(style.name, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        Text(
            text = "ESCALA DA FONTE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val scales = listOf("P" to FontScale.SMALL, "M" to FontScale.MEDIUM, "G" to FontScale.LARGE, "GG" to FontScale.EXTRA_LARGE)
            scales.forEach { (label, scale) ->
                val isSelected = fontScale == scale
                OutlinedButton(
                    onClick = { scope.launch { themePreferences.setFontScale(scale) } },
                    border = BorderStroke(
                        1.dp, 
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(label, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun AboutSettingsPage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = Color.Transparent,
            shadowElevation = 4.dp
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.valorae_logo_vector),
                contentDescription = "VALORAE Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "VALORAE",
            style = androidx.compose.ui.text.TextStyle(
                color = GoldPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
            )
        )
        
        Text(
            text = "ECOSSISTEMA INTEGRADO DE COMPOSIÇÃO DE RIQUEZA",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "O VALORAE nasceu para consolidar o controle patrimonial definitivo em uma única interface inteligente de alto padrão. Unimos ferramentas analíticas de nível institucional para que investidores individuais operem com máxima eficiência estratégica, segurança local absoluta e autonomia nas tomadas de decisão.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Text(
            text = "ESPECIFICAÇÕES DA PLATAFORMA",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = GoldPrimary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AboutItemCard(
                icon = Icons.Outlined.AccountBalance,
                title = "Custódia Institucional de Ativos",
                content = "Consolidação algorítmica de ativos de classe variável (Ações e FIIs da B3) com cálculo inteligente do Custo Médio Ponderado de Aquisição, contemplando todas as taxas de liquidação e emolumentos da bolsa nacional."
            )
            
            AboutItemCard(
                icon = Icons.Outlined.TrendingUp,
                title = "Valuation via Graham & Bazin",
                content = "Aplicação de fórmulas clássicas de precificação preditiva sob os critérios de Benjamin Graham (Preço Justo intrínseco baseado em Lucro e Valor Patrimonial) e Décio Bazin (Preço Teto projetado sob dividend yield mínimo)."
            )
            
            AboutItemCard(
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                title = "Planejamento Tributário (Regime DARF)",
                content = "Monitoramento contínuo de volumes liquidados por mês calendário, sinalizando proximidade operacional do limite brasileiro de R$ 20.000,00 para isenção tributária de imposto sobre ganhos de capital."
            )
            
            AboutItemCard(
                icon = Icons.Outlined.Shield,
                title = "Privacidade 'Zero-Cloud' Blindada",
                content = "Sua carteira de investimentos e saldos patrimoniais pertencem unicamente a você. Toda a persistência é efetuada localmente através de banco de dados Room encriptado. Seus dados sigilosos jamais saem do dispositivo."
            )
            
            AboutItemCard(
                icon = Icons.Outlined.MonetizationOn,
                title = "Gestão de Fluxos e Proventos",
                content = "Calendário detalhado de fluxos de caixa baseado em datas de anúncio (data Com) e data de liquidação de proventos ativos ou agendados, otimizando o reinvestimento tático e balanceamento da carteira."
            )
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Text(
            text = "DADOS GERAIS DO DEPLOYMENT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = GoldPrimary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Versão Compilada", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("v${BuildConfig.VERSION_NAME} (PRO Stable)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Storage, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Banco de Dados", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Room Persistence DB engine", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Code, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Ambiente Módulos", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Jetpack Compose v1.6.0 / SDK 34", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.EnhancedEncryption, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Estruturação Criptográfica", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("AES-256 GCM Local", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Gavel, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Licença de Distribuição", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Premium Estendido Perpétuo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "VALORAE Labs Corporation • Proudly Offline-First",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun AboutItemCard(icon: ImageVector, title: String, content: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(GoldPrimary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    hasBadge: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_item_${title.replace(" ", "_").lowercase()}")
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = DarkBackground,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp),
            border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                if (hasBadge) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(DangerRed, CircleShape)
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .border(2.dp, DarkSurface, CircleShape)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SecuritySettingsPage(themePreferences: ThemePreferences) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val biometricManager = androidx.biometric.BiometricManager.from(context)
    val authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val canAuthenticate = remember { 
        biometricManager.canAuthenticate(authenticators) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS 
    }
    
    val biometricEnabled by themePreferences.biometricEnabled.collectAsStateWithLifecycle(initialValue = false)
    val hideValues by themePreferences.hideValues.collectAsStateWithLifecycle(initialValue = false)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "AUTENTICAÇÃO DO DISPOSITIVO",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Desbloqueio Nativos", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Exigir Biometria, PIN ou Padrão do celular", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!canAuthenticate) {
                            Text(text = "Nenhuma segurança configurada no dispositivo", style = MaterialTheme.typography.labelSmall, color = DangerRed)
                        }
                    }
                    Switch(
                        checked = biometricEnabled, 
                        onCheckedChange = { 
                            if (canAuthenticate) {
                                scope.launch { themePreferences.setBiometricEnabled(it) } 
                            } else {
                                Toast.makeText(context, "Configure biometria ou PIN no Android primeiro", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = canAuthenticate
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "PRIVACIDADE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Ocultar Valores Sensíveis", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Mascarar saldo total e portfólio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = hideValues, 
                    onCheckedChange = { scope.launch { themePreferences.setHideValues(it) } }
                )
            }
        }
    }
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isConfirming) "Confirme o PIN" else "Defina um PIN de 4 dígitos") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isConfirming) confirmPin.padEnd(4, '•') else pin.padEnd(4, '•'),
                    style = MaterialTheme.typography.headlineLarge,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                error?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Simple numeric keypad
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0 until 4) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (j in 0 until 3) {
                                val key = keys.getOrNull(i * 3 + j) ?: break
                                OutlinedButton(
                                    onClick = {
                                        when (key) {
                                            "C" -> {
                                                if (isConfirming) {
                                                    if (confirmPin.isEmpty()) isConfirming = false
                                                    else confirmPin = ""
                                                } else {
                                                    pin = ""
                                                }
                                                error = null
                                            }
                                            "OK" -> {
                                                if (isConfirming) {
                                                    if (confirmPin == pin) onConfirm(pin)
                                                    else error = "PINs não conferem"
                                                } else if (pin.length == 4) {
                                                    isConfirming = true
                                                }
                                            }
                                            else -> {
                                                if (isConfirming) {
                                                    if (confirmPin.length < 4) confirmPin += key
                                                } else {
                                                    if (pin.length < 4) pin += key
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(key, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun NotificationsSettingsPage() {
    val scope = rememberCoroutineScope()
    var alertsEnabled by remember { mutableStateOf(true) }
    var newsEnabled by remember { mutableStateOf(true) }
    var dividendsEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ALERTAS E AVISOS DE PORTFÓLIO",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Alertas de Preço", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Variações atípicas em seus ativos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = alertsEnabled, onCheckedChange = { alertsEnabled = it })
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Agenda de Proventos", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Avisar sobre pagamento de dividendos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = dividendsEnabled, onCheckedChange = { dividendsEnabled = it })
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Notícias do Mercado", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Resumo diário e fatos relevantes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = newsEnabled, onCheckedChange = { newsEnabled = it })
                }
            }
        }
    }
}

@Composable
private fun HelpGuidePage() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "GUIA INTEGRADO VALORAE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HelpItem(
            icon = Icons.Outlined.AddCircle,
            title = "Como Registrar seus Ativos",
            description = "Toque no botão '+' flutuante na Dashboard. Ao digitar o Ticker (Ex: PETR4), o sistema buscará automaticamente o setor e o preço atual. Selecione 'Compra' ou 'Venda' e insira os dados da nota de corretagem."
        )
        
        HelpItem(
            icon = Icons.Outlined.Sync,
            title = "Sincronização B3 (Excel)",
            description = "Economize tempo! Vá em 'Backup e Dados', copie as linhas da sua planilha de negociações do Portal do Investidor B3 e cole no campo de importação. O importador inteligente extrai os dados e cadastra tudo instantaneamente."
        )
        
        HelpItem(
            icon = Icons.Outlined.Insights,
            title = "O que são os Conselhos?",
            description = "Na página de Análise, o VALORAE cruza indicadores como P/VP, DY e Dívida/EBITDA. Se um FII tem vacância alta, ou uma ação está muito cara (Ágio), nós exibimos um alerta inteligente no card 'Conselho Valorae'."
        )
        
        HelpItem(
            icon = Icons.Outlined.Calculate,
            title = "Cálculo de Preço Médio",
            description = "Utilizamos a média ponderada para calcular seu custo de aquisição. Dividendos não alteram o preço médio no sistema (foco em custo fiscal), mas são contabilizados na rentabilidade total do portfólio."
        )

        HelpItem(
            icon = Icons.Outlined.Security,
            title = "Privacidade e Segurança",
            description = "Seus dados nunca saem do celular. O backup gerado é um código JSON cifrado que você deve guardar. O VALORAE não possui servidores que armazenam seu CPF ou saldo bancário."
        )

        HelpItem(
            icon = Icons.Outlined.Update,
            title = "Como Atualizar o App?",
            description = "Utilize a seção 'Atualizações' para baixar o APK mais recente. Você não perde seus dados ao atualizar, mas recomendamos sempre gerar um backup antes de grandes migrações de versão."
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Canal de Suporte VALORAE", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Encontrou um erro ou tem sugestões de novos indicadores? Nossa equipe labs está pronta para ouvir você.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("suporte.labs@valorae.com.br", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DarfGuidePage(viewModel: com.example.viewmodel.PortfolioViewModel) {
    val darfSummaries by viewModel.darfSummaries.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Dynamic Banner
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Calculadora de DARF",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Motor Fiscal • Gestão Fiscal Automática",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (darfSummaries.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nenhuma venda identificada", fontWeight = FontWeight.Bold)
                    Text(
                        "O sistema VALORAE precisa de registros de VENDA para calcular o lucro e impostos devidos no mês.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            darfSummaries.forEach { summary ->
                DarfMonthCard(summary)
            }
        }

        // Section: How to pay
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Informações Importantes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                DarfItem("Ações (Swing)", "Isenção R$ 20k", "Vendas totais de ações no mês abaixo deste valor não pagam IR sobre lucro.")
                DarfItem("FIIs (Fundos Imob.)", "Sem Isenção", "Qualquer lucro na venda de cotas de FIIs deve pagar 20% de IR.")
                DarfItem("Cálculo de Preço Médio", "Sempre Ponderado", "O lucro é calculado sobre a diferença entre o preço de venda e o custo médio de aquisição.")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "O VALORAE é uma ferramenta informativa. Confirme sempre os valores com o sistema Sicalc da Receita Federal.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DarfMonthCard(summary: com.example.viewmodel.DarfMonthSummary) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = summary.monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (summary.totalTax > 0) {
                    Surface(
                        color = DangerRed.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            "DARF DEVIDA",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = DangerRed,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    Text("PAGO / ISENTO", style = MaterialTheme.typography.labelSmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("LUCRO AÇÕES (Swing)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        "R$ ${String.format("%.2f", summary.stockProfit)}", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = if (summary.stockProfit > 0) SuccessGreen else if(summary.stockProfit < 0) DangerRed else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("LUCRO FIIs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        "R$ ${String.format("%.2f", summary.fiiProfit)}", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = if (summary.fiiProfit > 0) SuccessGreen else if(summary.fiiProfit < 0) DangerRed else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Ações calculation details
                    Text(
                        text = "DETALHAMENTO DE AÇÕES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Volume de Vendas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("R$ ${String.format("%.2f", summary.stockSalesVolume)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status de Isenção (R$ 20k)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val isExempt = summary.stockSalesVolume <= 20000.0
                        Text(
                            text = if (isExempt) "Isento (Vendas < R$ 20.000)" else "Tributável (Vendas > R$ 20.000)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black,
                            color = if (isExempt) SuccessGreen else DangerRed
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Imposto de Renda Calculado (15%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("R$ ${String.format("%.2f", summary.stockTax)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (summary.stockTax > 0) DangerRed else MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // FIIs calculation details
                    Text(
                        text = "DETALHAMENTO DE FIIS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Volume de Vendas FIIs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("R$ ${String.format("%.2f", summary.fiiSalesVolume)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Imposto de Renda Calculado (20%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("R$ ${String.format("%.2f", summary.fiiTax)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (summary.fiiTax > 0) DangerRed else MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Valores de IR estimam alíquotas oficiais da RFB: 15% em ações (Swing) e 20% em fundos imobiliários sobre o ganho de capital líquido.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        lineHeight = 14.sp
                    )
                }
            }
            
            if (summary.totalTax > 0) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Imposto Estimado a Pagar", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelSmall)
                            Text(
                                "R$ ${String.format("%.2f", summary.totalTax)}",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DarfSection(title: String, icon: ImageVector, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun DarfItem(label: String, value: String, desc: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), lineHeight = 14.sp)
    }
}

@Composable
private fun DarfStep(number: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text("$number.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(20.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun HelpItem(icon: ImageVector, title: String, description: String, onClick: (() -> Unit)? = null) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (onClick != null) onClick() else expanded = !expanded 
            }
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title, 
                    fontWeight = FontWeight.SemiBold, 
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataBackupPage(viewModel: com.example.viewmodel.PortfolioViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    
    var showSuccessBanner by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf("") }
    
    var showErrorBanner by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    
    var clearOnImport by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    // Backup Save Launchers
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { destUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    val json = viewModel.exportTransactionsToJson()
                    context.contentResolver.openOutputStream(destUri)?.use { output ->
                        output.write(json.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        successMsg = "Backup exportado com sucesso para arquivo JSON!"
                        showSuccessBanner = true
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMsg = "Erro ao exportar JSON: ${e.localizedMessage}"
                        showErrorBanner = true
                    }
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { destUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    val csv = viewModel.exportTransactionsToCsv()
                    context.contentResolver.openOutputStream(destUri)?.use { output ->
                        output.write(csv.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        successMsg = "Planilha de transações exportada com sucesso (CSV)!"
                        showSuccessBanner = true
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMsg = "Erro ao exportar CSV: ${e.localizedMessage}"
                        showErrorBanner = true
                    }
                }
            }
        }
    }

    // Comprehensive multi-format File Picker Launcher (XLSX, JSON, CSV)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    val fileName = com.example.utils.DataImportExportUtils.getFileName(context, selectedUri)
                    val fileNameLower = fileName.lowercase()
                    var countImported = 0
                    var isJson = false

                    withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(selectedUri)
                            ?: throw Exception("Não foi possível carregar o arquivo anexado.")

                        if (fileNameLower.endsWith(".xlsx")) {
                            val sheetText = com.example.utils.DataImportExportUtils.parseXlsx(inputStream)
                            if (sheetText.isNotBlank()) {
                                viewModel.importFromB3Spreadsheet(sheetText) { count ->
                                    countImported = count
                                }
                            } else {
                                throw Exception("Planilha Excel está vazia ou corrompida.")
                            }
                        } else if (fileNameLower.endsWith(".json")) {
                            isJson = true
                            val jsonText = inputStream.reader().readText()
                            viewModel.importTransactionsFromJson(jsonText, clearOnImport)
                        } else {
                            // Supposed CSV or Text format
                            val textContent = inputStream.reader().readText()
                            if (textContent.isNotBlank()) {
                                viewModel.importFromB3Spreadsheet(textContent) { count ->
                                    countImported = count
                                }
                            } else {
                                throw Exception("O arquivo de texto/CSV selecionado está vazio.")
                            }
                        }
                        inputStream.close()
                    }

                    // A brief pause for background db processing
                    kotlinx.coroutines.delay(1000)

                    if (isJson) {
                        successMsg = "Corte de backup JSON em execução! Os ativos serão carregados em breve."
                    } else {
                        if (countImported > 0) {
                            successMsg = "Importado com sucesso! $countImported transações adicionadas de '$fileName'."
                        } else {
                            successMsg = "Arquivo '$fileName' anexado! Verifique a aba de Notificações para detalhes."
                        }
                    }
                    showSuccessBanner = true
                } catch (e: Exception) {
                    errorMsg = "Falha ao ler arquivo: ${e.localizedMessage ?: "formato incorreto"}"
                    showErrorBanner = true
                }
            }
        }
    }

    LaunchedEffect(showSuccessBanner) {
        if (showSuccessBanner) {
            kotlinx.coroutines.delay(5000)
            showSuccessBanner = false
        }
    }
    
    LaunchedEffect(showErrorBanner) {
        if (showErrorBanner) {
            kotlinx.coroutines.delay(5000)
            showErrorBanner = false
        }
    }

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("Zerar Portfólio?", fontWeight = FontWeight.Bold) },
            text = { Text("Isso excluirá permanentemente todas as suas transações em andamento de forma irreversível.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.clearAllTransactions()
                        showConfirmDeleteDialog = false
                        successMsg = "Todas as transações foram apagadas do banco de dados local."
                        showSuccessBanner = true
                    }
                ) {
                    Text("Confirmar Exclusão", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Success and Error Info Banners
        AnimatedVisibility(visible = showSuccessBanner) {
            Surface(
                color = SuccessGreen.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
                    Text(
                        text = successMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(visible = showErrorBanner) {
            Surface(
                color = DangerRed.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Error, contentDescription = null, tint = DangerRed)
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DangerRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Card 1: Resumo do Banco de Dados
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Status de Armazenamento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Controle local de cópias de segurança",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${transactions.size} transações",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Card 2: Importador Premium de Arquivo (Excel XLSX, JSON, CSV)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FileOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Importação Integrada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Selecione ou anexe arquivos criados anteriormente para carregar dados de forma segura no portfólio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // High fidelity interactive attachment box
                OutlinedCard(
                    onClick = {
                        try {
                            filePickerLauncher.launch("*/*")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
                    ),
                    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Selecionar e Anexar Arquivo",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Excel (.xlsx), backup (.json) ou planilha (.csv)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = clearOnImport,
                        onCheckedChange = { clearOnImport = it }
                    )
                    Text(
                        text = "Sobrescrever dados existentes no app (somente JSON)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { clearOnImport = !clearOnImport }
                    )
                }
            }
        }

        // Card Cloud Sync: Integração Supabase
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 1.dp
        ) {
            val prefs = remember { context.getSharedPreferences("cloud_sync_prefs", android.content.Context.MODE_PRIVATE) }
            var syncUserId by remember { mutableStateOf(prefs.getString("sync_user_id", "") ?: "") }
            var cryptoPassphrase by remember { mutableStateOf(prefs.getString("crypto_passphrase", "") ?: "") }
            var passphraseVisible by remember { mutableStateOf(false) }
            var connectionStatusSupabase by remember { mutableStateOf("Não verificado") }
            var isTestingSupabase by remember { mutableStateOf(false) }
            var isBackingUp by remember { mutableStateOf(false) }
            var isRestoring by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (syncUserId.isEmpty()) {
                    val randomId = "v_" + UUID.randomUUID().toString().substring(0, 8)
                    syncUserId = randomId
                    prefs.edit().putString("sync_user_id", randomId).apply()
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sincronização em Nuvem",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Suporte unificado ao Supabase (Banco de Dados em Nuvem). Seu portfólio seguro em qualquer lugar com criptografia ponta-a-ponta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Indicação de Status de Configuração das Chaves de Ambiente do Gradle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSupaConfigured = com.example.network.CloudSyncManager.isCloudConfigured()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSupaConfigured) SuccessGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isSupaConfigured) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Supabase DB", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (isSupaConfigured) "Integrado (.env Ativo)" else "Pendente (Configurações)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSupaConfigured) SuccessGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Identificador Único do Usuário
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().testTag("sync_user_id_field"),
                    value = syncUserId,
                    onValueChange = {
                        syncUserId = it
                        prefs.edit().putString("sync_user_id", it).apply()
                    },
                    label = { Text("ID de Sincronização Única") },
                    placeholder = { Text("Ex: v_7a4b0d21") },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Frase-senha para criptografia local Client-Side (AES-256 Zero Knowledge)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().testTag("crypto_passphrase_field"),
                    value = cryptoPassphrase,
                    onValueChange = {
                        cryptoPassphrase = it
                        prefs.edit().putString("crypto_passphrase", it).apply()
                    },
                    label = { Text("Senha / PIN de Criptografia de Backup") },
                    placeholder = { Text("Frase secreta opcional para codificação das transações") },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        val image = if (passphraseVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passphraseVisible = !passphraseVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    visualTransformation = if (passphraseVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = "🔒 Proteção militar AES-256 GCM: Caso defina uma senha de backup, suas transações são criptografadas diretamente no seu celular antes de serem enviadas para a nuvem. Nem mesmo os administradores do banco de dados poderão visualizar seus ativos.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    lineHeight = 13.sp
                )

                // Botões de Teste de Conexões de Rede Real-time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isTestingSupabase) return@OutlinedButton
                            isTestingSupabase = true
                            scope.launch {
                                val result = com.example.network.CloudSyncManager.testSupabaseConnection()
                                result.fold(
                                    onSuccess = { msg ->
                                        connectionStatusSupabase = "Ativo"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { err ->
                                        connectionStatusSupabase = "Erro"
                                        Toast.makeText(context, err.message, Toast.LENGTH_LONG).show()
                                    }
                                )
                                isTestingSupabase = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        if (isTestingSupabase) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testar Supabase", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Botões de Sincronização Ativa: Backup e Restauração
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f).testTag("backup_supabase_button"),
                        enabled = !isBackingUp && !isRestoring && com.example.network.CloudSyncManager.isCloudConfigured(),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            isBackingUp = true
                            scope.launch {
                                try {
                                    val result = com.example.network.CloudSyncManager.backupData(
                                        userId = syncUserId,
                                        transactions = transactions,
                                        passPhrase = cryptoPassphrase.takeIf { it.isNotBlank() }
                                    )
                                    result.fold(
                                        onSuccess = { msg ->
                                            successMsg = msg
                                            showSuccessBanner = true
                                        },
                                        onFailure = { err ->
                                            errorMsg = "Erro ao efetuar backup: ${err.message}"
                                            showErrorBanner = true
                                        }
                                    )
                                } catch (e: Exception) {
                                    errorMsg = "Erro inesperado: ${e.message}"
                                    showErrorBanner = true
                                } finally {
                                    isBackingUp = false
                                }
                            }
                        }
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nuvem Backup", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Button(
                        modifier = Modifier.weight(1f).testTag("restore_supabase_button"),
                        enabled = !isBackingUp && !isRestoring && com.example.network.CloudSyncManager.isCloudConfigured(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            isRestoring = true
                            scope.launch {
                                try {
                                    val result = com.example.network.CloudSyncManager.restoreData(
                                        userId = syncUserId,
                                        passPhrase = cryptoPassphrase.takeIf { it.isNotBlank() }
                                    )
                                    result.fold(
                                        onSuccess = { list ->
                                            // Serialize list and feed into import to avoid any manual mapping errors
                                            val arr = org.json.JSONArray()
                                            list.forEach { t ->
                                                arr.put(org.json.JSONObject().apply {
                                                    put("ticker", t.ticker)
                                                    put("name", t.name)
                                                    put("quantity", t.quantity)
                                                    put("purchasePrice", t.purchasePrice)
                                                    put("date", t.date)
                                                    put("type", t.type)
                                                    put("isSell", t.isSell)
                                                    put("broker", t.broker)
                                                    put("sector", t.sector)
                                                    put("notes", t.notes)
                                                })
                                            }
                                            viewModel.importTransactionsFromJson(arr.toString(), clearOnImport)
                                            successMsg = "Backup da nuvem selecionado e restaurado com sucesso! ${list.size} transações recarregadas."
                                            showSuccessBanner = true
                                        },
                                        onFailure = { err ->
                                            errorMsg = "Controle de restauração falhou: ${err.message}"
                                            showErrorBanner = true
                                        }
                                    )
                                } catch (e: Exception) {
                                    errorMsg = "Erro inesperado: ${e.message}"
                                    showErrorBanner = true
                                } finally {
                                    isRestoring = false
                                }
                            }
                        }
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurar Nuvem", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Dica caso queira habilitar o DB
                if (!com.example.network.CloudSyncManager.isCloudConfigured()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "💡 Para sincronizar remotamente, configure \"SUPABASE_URL\" e \"SUPABASE_ANON_KEY\" usando o painel Secrets na barra lateral do AI Studio e reinicie a compilação. Use o script SQL abaixo para criar a tabela no editor SQL do Supabase.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Card 3: Exportação Nativa
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Exportação de Dados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Salve seu portfólio para abrir no Excel ou restaurar em outros aparelhos de forma imediata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            try {
                                exportJsonLauncher.launch("valorae_backup.json")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Backup JSON", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            try {
                                exportCsvLauncher.launch("valorae_transacoes.csv")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Icon(Icons.Filled.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Planilha Excel/CSV", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            scope.launch {
                                try {
                                    val json = withContext(Dispatchers.Default) { viewModel.exportTransactionsToJson() }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        type = "application/json"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Cópia de Segurança VALORAE")
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    android.util.Log.e("VALORAE", "Erro ao compartilhar backup: ${e.message}")
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartilhar Cópia", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Card 4: Zona de Perigo
        Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.04f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Zona de Perigo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Ações permanentes que removem os dados locais.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { showConfirmDeleteDialog = true }
                ) {
                    Text("Zerar e Limpar todo o App", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
