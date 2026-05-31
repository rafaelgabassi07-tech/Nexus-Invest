package com.example

import android.os.Bundle
import android.view.WindowManager
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.filled.Add
import com.example.data.AppDatabase
import com.example.data.TransactionRepository
import com.example.ui.screens.ChartsScreen

import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NewsScreen
import com.example.network.B3NetworkService
import com.example.ui.theme.*
import com.example.viewmodel.PortfolioViewModel
import com.example.viewmodel.PortfolioViewModelFactory

import com.example.data.ThemePreferences

class MainActivity : androidx.fragment.app.FragmentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        B3NetworkService.initialize(applicationContext)
        
        // Initialize local database database context dependencies manually (simple injection)
        val database = AppDatabase.getDatabase(this)
        val repository = TransactionRepository(
            database.transactionDao(),
            database.notificationDao(),
            database.changelogDao()
        )
        val viewModelFactory = PortfolioViewModelFactory(repository)
        val themePreferences = ThemePreferences(this)
        
        setContent {
            val appTheme by themePreferences.theme.collectAsStateWithLifecycle(com.example.data.AppTheme.SYSTEM)
            val fontScale by themePreferences.fontScale.collectAsStateWithLifecycle(com.example.data.FontScale.MEDIUM)
            val cornerStyle by themePreferences.cornerStyle.collectAsStateWithLifecycle(com.example.data.CornerStyle.MODERN)
            val darkVariant by themePreferences.darkVariant.collectAsStateWithLifecycle(com.example.data.DarkVariant.CARBON)
            val lightVariant by themePreferences.lightVariant.collectAsStateWithLifecycle(com.example.data.LightVariant.CLASSIC)
            
            val biometricEnabledState = themePreferences.biometricEnabled.collectAsStateWithLifecycle(initialValue = null)
            val hideValues by themePreferences.hideValues.collectAsStateWithLifecycle(initialValue = false)

            var isAppUnlocked by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(biometricEnabledState.value) {
                if (biometricEnabledState.value == false) isAppUnlocked = true
            }

            val biometricEnabled = biometricEnabledState.value ?: false

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, biometricEnabled) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                        if (biometricEnabled) {
                            isAppUnlocked = false
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val isDarkTheme = when (appTheme) {
                com.example.data.AppTheme.LIGHT -> false
                com.example.data.AppTheme.DARK -> true
                com.example.data.AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                fontScale = fontScale,
                cornerStyle = cornerStyle,
                darkVariant = darkVariant,
                lightVariant = lightVariant
            ) {
                // Instantiate our centralized Portfolio ViewModel using factory
                val viewModel: PortfolioViewModel = viewModel(factory = viewModelFactory)
                
                // Collect basic state for background/general usage
                val context = androidx.compose.ui.platform.LocalContext.current
                val updateManager = remember { com.example.network.UpdateManager(context) }
                val updateStatus by updateManager.updateStatus.collectAsStateWithLifecycle()

                // Persist navigation states even when app is locked
                var activePage by rememberSaveable { mutableIntStateOf(0) }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var showNotifications by rememberSaveable { mutableStateOf(false) }
                var showingPortfolioDetail by rememberSaveable { mutableStateOf(false) }
                
                var showStartupUpdateDialog by rememberSaveable { mutableStateOf(false) }
                var updateDismissedAtStartup by rememberSaveable { mutableStateOf(false) }
                var showSystemUpdateCenter by rememberSaveable { mutableStateOf(false) }

                if (!isAppUnlocked) {
                    LockScreen(
                        biometricEnabled = biometricEnabledState.value,
                        onUnlock = { isAppUnlocked = true }
                    )
                } else {
                    // Collect state reactively with lifecycle awareness
                    val portfolioSummary by viewModel.portfolioSummary.collectAsStateWithLifecycle()
                    val assetSummaries by viewModel.assetSummaries.collectAsStateWithLifecycle()
                    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
                    
                    val newsFeed by viewModel.newsFeed.collectAsStateWithLifecycle()
                    val isLoadingNews by viewModel.isLoadingNews.collectAsStateWithLifecycle()

                    val tickerInput by viewModel.searchTickerInput.collectAsStateWithLifecycle()
                    val searchResult by viewModel.searchQueryResult.collectAsStateWithLifecycle()
                    val chartHistory by viewModel.searchQueryHistory.collectAsStateWithLifecycle()
                    val assetNews by viewModel.searchQueryNews.collectAsStateWithLifecycle()
                    val isSearchingAsset by viewModel.isSearchingAsset.collectAsStateWithLifecycle()
                    val chartRange by viewModel.searchQueryRange.collectAsStateWithLifecycle()
                    val cachedAssetData by viewModel.cachedAssetData.collectAsStateWithLifecycle()
                    val assetChartBundles by viewModel.assetChartBundles.collectAsStateWithLifecycle()
                    val isLoadingChartBundle by viewModel.isLoadingChartBundle.collectAsStateWithLifecycle()
                    val notificationsList by viewModel.notifications.collectAsStateWithLifecycle()
                    val proxyHealth by viewModel.proxyHealth.collectAsStateWithLifecycle()

                    val favoriteTickers by themePreferences.favoriteTickers.collectAsStateWithLifecycle(emptyList())
                    val favoriteScope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        updateManager.checkForUpdate("https://raw.githubusercontent.com/rafaelgabassi07-tech/app-atualizacoes/refs/heads/main/update.json")
                    }

                    LaunchedEffect(updateStatus) {
                        val status = updateStatus
                        if (status is com.example.network.UpdateManager.UpdateStatus.UpdateAvailable) {
                            viewModel.addUpdateNotification(status.info)
                            if (!updateDismissedAtStartup) {
                                showStartupUpdateDialog = true
                            }
                        }
                    }

                    if (showSettings) {
                        com.example.ui.screens.SettingsScreen(
                            updateManager = updateManager,
                            updateStatus = updateStatus,
                            themePreferences = themePreferences,
                            viewModel = viewModel,
                            onDismiss = { showSettings = false }
                        )
                    }

                    if (showNotifications) {
                        com.example.ui.components.NotificationsDialog(
                            viewModel = viewModel,
                            onDismiss = { showNotifications = false }
                        )
                    }

                    if (showStartupUpdateDialog && updateStatus is com.example.network.UpdateManager.UpdateStatus.UpdateAvailable) {
                        val info = (updateStatus as com.example.network.UpdateManager.UpdateStatus.UpdateAvailable).info
                        var isChangelogExpanded by remember { mutableStateOf(true) }
                        AlertDialog(
                            onDismissRequest = {
                                showStartupUpdateDialog = false
                                updateDismissedAtStartup = true
                            },
                            shape = RoundedCornerShape(28.dp),
                            containerColor = DarkSurfaceElevated,
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SystemUpdate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = "Nova Atualização",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "O VALORAE v${info.versionName} está disponível com melhorias de performance e novas ferramentas de análise.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                    
                                    // New Info Row (Date & Size)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        info.releaseDate?.take(10)?.let {
                                            Text("Data: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        info.fileSize?.let {
                                            Text("Tamanho: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isChangelogExpanded = !isChangelogExpanded }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "O QUE HÁ DE NOVO:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Icon(
                                            imageVector = if (isChangelogExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isChangelogExpanded) "Esconder novidades" else "Mostrar novidades",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    AnimatedVisibility(visible = isChangelogExpanded) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (!info.changelog.isNullOrEmpty()) {
                                                info.changelog.forEach { line ->
                                                    Row(verticalAlignment = Alignment.Top) {
                                                        Box(
                                                            modifier = Modifier
                                                                .padding(top = 6.dp, end = 10.dp)
                                                                .size(6.dp)
                                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                        )
                                                        Text(
                                                            text = line.trim(),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = TextSecondary,
                                                            lineHeight = 18.sp
                                                        )
                                                    }
                                                }
                                            } else if (!info.features.isNullOrEmpty()) {
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
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = TextSecondary,
                                                            lineHeight = 18.sp
                                                        )
                                                    }
                                                }
                                            } else if (info.releaseNotes?.isNotEmpty() == true) {
                                                Text(
                                                    text = info.releaseNotes ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showStartupUpdateDialog = false
                                        showSystemUpdateCenter = true
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Atualizar Agora", fontWeight = FontWeight.Black)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showStartupUpdateDialog = false
                                        updateDismissedAtStartup = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Lembrar mais tarde", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                    }

                    if (showSystemUpdateCenter) {
                        com.example.ui.components.SystemUpdateCenterDialog(
                            updateManager = updateManager,
                            updateStatus = updateStatus,
                            onDismiss = { showSystemUpdateCenter = false }
                        )
                    }



                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                        topBar = {
                            Column {
                                CenterAlignedTopAppBar(
                                    title = {
                                        Text(
                                            text = "VALORAE",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 2.sp
                                            )
                                        )
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background,
                                        titleContentColor = MaterialTheme.colorScheme.onBackground
                                    ),
                                    actions = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(end = 4.dp)) {
                                            val proxyChipColor = when {
                                                proxyHealth.isOnline -> SuccessGreen
                                                proxyHealth.isUsingCache -> WarningOrange
                                                proxyHealth.status.equals("Parcial", ignoreCase = true) -> WarningOrange
                                                else -> DangerRed
                                            }
                                            Surface(
                                                modifier = Modifier
                                                    .height(26.dp)
                                                    .clickable { viewModel.refreshProxyHealth() },
                                                color = proxyChipColor.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(50),
                                                border = BorderStroke(1.dp, proxyChipColor.copy(alpha = 0.35f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(Modifier.size(6.dp).background(proxyChipColor, CircleShape))
                                                    Text(
                                                        text = when {
                                                            proxyHealth.isOnline -> "Proxy"
                                                            proxyHealth.isUsingCache -> "Cache"
                                                            proxyHealth.status.equals("Parcial", true) -> "Parcial"
                                                            else -> "Offline"
                                                        },
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = proxyChipColor
                                                    )
                                                }
                                            }
                                            if (updateStatus is com.example.network.UpdateManager.UpdateStatus.UpdateAvailable) {
                                                IconButton(
                                                    onClick = { showSystemUpdateCenter = true },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.SystemUpdate,
                                                        contentDescription = "Atualização Disponível",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { showNotifications = true },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Notifications,
                                                        contentDescription = "Notificações",
                                                        tint = MaterialTheme.colorScheme.onBackground,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    val hasUnread = notificationsList.any { !it.isRead }
                                                    if (hasUnread) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .background(DangerRed, CircleShape)
                                                                .align(Alignment.TopEnd)
                                                                .offset(x = 1.dp, y = (-1).dp)
                                                        )
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = { showSettings = true },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Settings,
                                                    contentDescription = "Configurações",
                                                    tint = MaterialTheme.colorScheme.onBackground,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                                // Elegant and persistent tickers below the top bar visible on all pages!
                                // com.example.ui.components.MarketTicker()
                            }
                        },
                        bottomBar = {
                            Column {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                    thickness = 1.5.dp
                                )
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier
                                        .height(84.dp)
                                        .padding(horizontal = 0.dp)
                                ) {
                                    NavigationBarItem(
                                        selected = activePage == 0,
                                        onClick = { activePage = 0 },
                                        icon = { 
                                            Icon(
                                                imageVector = if (activePage == 0) Icons.Default.Home else Icons.Outlined.Home, 
                                                contentDescription = "Dashboard",
                                                modifier = Modifier.size(24.dp)
                                            ) 
                                        },
                                        label = { 
                                            Text(
                                                text = "Início", 
                                                fontSize = 11.sp,
                                                fontWeight = if (activePage == 0) FontWeight.ExtraBold else FontWeight.Medium,
                                                letterSpacing = 0.5.sp
                                            ) 
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = activePage == 2,
                                        onClick = { activePage = 2 },
                                        icon = { 
                                            Icon(
                                                imageVector = if (activePage == 2) Icons.Default.Search else Icons.Outlined.Search, 
                                                contentDescription = "Análise",
                                                modifier = Modifier.size(24.dp)
                                            ) 
                                        },
                                        label = { 
                                            Text(
                                                text = "Análise", 
                                                fontSize = 11.sp,
                                                fontWeight = if (activePage == 2) FontWeight.ExtraBold else FontWeight.Medium,
                                                letterSpacing = 0.5.sp
                                            ) 
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = activePage == 3,
                                        onClick = { activePage = 3 },
                                        icon = { 
                                            Icon(
                                                imageVector = if (activePage == 3) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Outlined.TrendingUp, 
                                                contentDescription = "Insights",
                                                modifier = Modifier.size(24.dp)
                                            ) 
                                        },
                                        label = { 
                                            Text(
                                                text = "Insights", 
                                                fontSize = 11.sp,
                                                fontWeight = if (activePage == 3) FontWeight.ExtraBold else FontWeight.Medium,
                                                letterSpacing = 0.5.sp
                                            ) 
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = activePage == 4,
                                        onClick = { activePage = 4 },
                                        icon = { 
                                            Icon(
                                                imageVector = if (activePage == 4) Icons.Default.Newspaper else Icons.Outlined.Newspaper, 
                                                contentDescription = "Notícias",
                                                modifier = Modifier.size(24.dp)
                                            ) 
                                        },
                                        label = { 
                                            Text(
                                                text = "Notícias",
                                                fontSize = 11.sp,
                                                fontWeight = if (activePage == 4) FontWeight.ExtraBold else FontWeight.Medium,
                                                letterSpacing = 0.5.sp
                                            ) 
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        // Beautiful fade-in navigation switcher based on page index
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            androidx.compose.animation.Crossfade<Int>(
                                targetState = activePage,
                                label = "PageTransition"
                            ) { page ->
                                when (page) {
                                    0 -> {
                                        DashboardScreen(
                                            summary = portfolioSummary,
                                            assets = assetSummaries,
                                            transactions = transactions,
                                            cachedAssetData = cachedAssetData,
                                            assetChartBundles = assetChartBundles,
                                            isLoadingChartBundle = isLoadingChartBundle,
                                            onLoadAssetChartBundle = { ticker, range -> viewModel.loadAssetChartBundle(ticker, range) },
                                            chartHistory = chartHistory,
                                            chartRange = chartRange,
                                            onRangeChange = { range -> viewModel.changeSearchChartRange(range) },
                                            isSearchingChart = isSearchingAsset,
                                            hideValues = hideValues,
                                            onAddTransaction = { ticker, qty, prc, type, broker, sector, date, notes, isSell ->
                                                viewModel.insertTransaction(ticker, qty, prc, type, broker, sector, date, notes, isSell)
                                            },
                                            onDeleteTransaction = { tx ->
                                                viewModel.deleteTransaction(tx)
                                            },
                                            onUpdateTransaction = { id, ticker, qty, prc, type, broker, sector, date, notes, isSell ->
                                                viewModel.updateTransaction(id, ticker, qty, prc, type, broker, sector, date, notes, isSell)
                                            },
                                            onAssetClick = { ticker ->
                                                viewModel.searchTickerInput.value = ticker
                                                viewModel.searchAndAnalyzeAsset(ticker)
                                                activePage = 2
                                            },
                                            onPortfolioClick = { showingPortfolioDetail = true },
                                            updateStatus = updateStatus,
                                            onUpdateAvailable = { showStartupUpdateDialog = true }
                                        )
                                    }
                                    2 -> {
                                        AnalysisScreen(
                                            tickerInput = tickerInput,
                                             assetChartBundles = assetChartBundles,
                                             isLoadingChartBundle = isLoadingChartBundle,
                                            onTickerInputChanges = { viewModel.searchTickerInput.value = it },
                                            onSearchClick = { ticker -> viewModel.searchAndAnalyzeAsset(ticker) },
                                            searchResult = searchResult,
                                            chartHistory = chartHistory,
                                            chartRange = chartRange,
                                            onRangeChange = { viewModel.changeSearchChartRange(it) },
                                            assetNews = assetNews,
                                            isSearching = isSearchingAsset,
                                            favoriteTickers = favoriteTickers,
                                            onToggleFavorite = { ticker ->
                                                favoriteScope.launch {
                                                    themePreferences.toggleFavorite(ticker)
                                                }
                                            }
                                        )
                                    }
                                    3 -> ChartsScreen(viewModel = viewModel)
                                    4 -> NewsScreen(
                                        news = newsFeed,
                                        isLoadingNews = isLoadingNews,
                                        onRefreshNews = { viewModel.fetchGlobalNews() }
                                    )
                                }
                            }
                        }
                    }

                    // Elegant and smooth transition for the detailed consolidado view
                    AnimatedVisibility(
                        visible = showingPortfolioDetail,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.zIndex(100f)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            com.example.ui.screens.PortfolioDetailScreen(
                                summary = portfolioSummary,
                                assets = assetSummaries,
                                hideValues = hideValues,
                                onBack = { showingPortfolioDetail = false }
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
fun LockScreen(
    biometricEnabled: Boolean?,
    onUnlock: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    val authenticateDevice = {
        if (activity != null && biometricEnabled == true) {
            val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
            val biometricPrompt = androidx.biometric.BiometricPrompt(
                activity,
                executor,
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onUnlock()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Allow user to manually tap button if they dismiss
                    }
                }
            )

            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloqueio VALORAE")
                .setSubtitle("Autentique-se para acessar seu portfólio")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Throwable) {
                // If device lacks secure lock entirely or it failed to show
            }
        } else {
            // Unlocked if somehow it's not enabled but lock screen shows
            onUnlock()
        }
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled == true) {
            kotlinx.coroutines.delay(300)
            authenticateDevice()
        } else if (biometricEnabled == false) {
            onUnlock()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF050505)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.valorae_logo_vector),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = GoldPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "VALORAE",
                style = MaterialTheme.typography.headlineMedium.copy(
                    letterSpacing = 8.sp,
                    fontWeight = FontWeight.Black
                ),
                color = GoldPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "INVESTIDOR",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Black
                ),
                color = Color.White.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(48.dp))
            
            Box(modifier = Modifier.height(56.dp), contentAlignment = Alignment.Center) {
                if (biometricEnabled == true) {
                    // Try again button only shown if they dismissed the prompt implicitly, usually they'd see the prompt.
                    // But to look like a loading screen, we just show an indicator, or a subtle clickable text.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = GoldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Toque para autenticar",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.clickable { authenticateDevice() }
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        color = GoldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
