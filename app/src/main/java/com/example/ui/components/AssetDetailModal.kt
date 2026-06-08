package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Info
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
import com.example.data.Transaction
import com.example.network.ChartPoint
import com.example.network.B3NetworkService
import com.example.network.B3AssetData
import com.example.network.AssetChartBundle
import com.example.ui.theme.*
import com.example.viewmodel.AssetSummary
import com.example.ui.B3UIUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailModal(
    asset: AssetSummary,
    initialAssetData: B3AssetData? = null,
    initialChartBundle: AssetChartBundle? = null,
    isLoadingInitialChartBundle: Boolean = false,
    onLoadChartBundle: (String, String) -> Unit = { _, _ -> },
    chartPoints: List<ChartPoint>,
    chartRange: String,
    onRangeChange: (String) -> Unit,
    transactions: List<Transaction>,
    onDeleteTransaction: (Transaction) -> Unit,
    onEditTransaction: (Transaction) -> Unit = {},
    isSearching: Boolean,
    onDismiss: () -> Unit
) {
    val tickerKey = remember(asset.ticker) { asset.ticker.trim().uppercase() }
    val fallbackAssetData = remember(tickerKey, asset) { asset.toFallbackB3AssetData() }

    // Reuse the same state that powers the Analysis page when available.
    // If the proxy is slow, degraded or returns PARTIAL, keep a safe local fallback
    // from the portfolio summary so Detalhes do Ativo never opens blank.
    var assetData by remember(tickerKey) { mutableStateOf(initialAssetData ?: fallbackAssetData) }
    var isLoadingData by remember(tickerKey) { mutableStateOf(initialAssetData == null) }

    // Local historical chart points. Prefer a ticker-specific AssetChartBundle over
    // generic search history, because the generic Analysis chart can belong to another asset.
    var localChartPoints by remember(tickerKey) { mutableStateOf(initialChartBundle?.priceHistory?.ifEmpty { chartPoints } ?: chartPoints) }
    var localChartRange by remember(tickerKey) { mutableStateOf(chartRange.ifBlank { "1y" }) }
    var lastResolvedChartRange by remember(tickerKey) {
        mutableStateOf(
            if (initialChartBundle?.priceHistory?.isNotEmpty() == true || chartPoints.isNotEmpty()) {
                chartRange.ifBlank { "1y" }
            } else {
                ""
            }
        )
    }
    var isFetchingChart by remember(tickerKey) { mutableStateOf(localChartPoints.isEmpty()) }

    var chartBundle by remember(tickerKey) { mutableStateOf(initialChartBundle) }
    var isLoadingChartBundle by remember(tickerKey) { mutableStateOf(isLoadingInitialChartBundle && initialChartBundle == null) }

    var newsItems by remember(tickerKey) { mutableStateOf<List<com.example.network.NewsItem>>(emptyList()) }
    var isLoadingNews by remember(tickerKey) { mutableStateOf(true) }

    LaunchedEffect(tickerKey) {
        isLoadingNews = true
        val fetchedNews = withContext(Dispatchers.IO) {
            runCatching { B3NetworkService.fetchNews(tickerKey) }.getOrDefault(emptyList())
        }
        newsItems = fetchedNews
        isLoadingNews = false
    }

    val isFii = assetData.isFii || asset.type.equals("FII", ignoreCase = true) || B3NetworkService.inferIsFii(asset.ticker)
    val lineColor = GoldPrimary

    LaunchedEffect(tickerKey, initialAssetData) {
        assetData = initialAssetData ?: assetData.takeIf { it.hasUsefulProxyData() } ?: fallbackAssetData
        isLoadingData = initialAssetData == null && !assetData.hasUsefulProxyData()
    }

    LaunchedEffect(tickerKey, initialChartBundle) {
        if (initialChartBundle != null) {
            chartBundle = initialChartBundle
            isLoadingChartBundle = false
            if (initialChartBundle.priceHistory.isNotEmpty()) {
                localChartPoints = initialChartBundle.priceHistory
                lastResolvedChartRange = localChartRange
                isFetchingChart = false
            }
        }
    }

    LaunchedEffect(tickerKey, chartPoints) {
        if (chartPoints.isNotEmpty() && localChartPoints.isEmpty()) {
            localChartPoints = chartPoints
            lastResolvedChartRange = localChartRange
            isFetchingChart = false
        }
    }

    LaunchedEffect(tickerKey, chartRange) {
        // O período global da tela de Análise pode pertencer a outro ticker.
        // Sincronize apenas na primeira abertura, antes do Detalhes resolver histórico próprio.
        if (lastResolvedChartRange.isBlank() && localChartPoints.isEmpty() && chartRange.isNotBlank()) {
            localChartRange = chartRange
        }
    }

    LaunchedEffect(tickerKey, localChartRange) {
        val chartRangeChanged = lastResolvedChartRange.isBlank() || !lastResolvedChartRange.equals(localChartRange, ignoreCase = true)
        val needsAssetRefresh = !assetData.hasUsefulProxyData()
        val needsHistoryRefresh = localChartPoints.isEmpty() || chartRangeChanged
        val needsBundleRefresh = chartBundle == null || chartRangeChanged

        isLoadingData = needsAssetRefresh
        isFetchingChart = needsHistoryRefresh
        isLoadingChartBundle = needsBundleRefresh || isLoadingInitialChartBundle
        if (needsBundleRefresh) {
            onLoadChartBundle(tickerKey, localChartRange)
        }

        val shouldFetchBundleLocally = needsBundleRefresh && (chartBundle == null || localChartPoints.isEmpty())
        val result = withContext(Dispatchers.IO) {
            val fetchedAsset = if (needsAssetRefresh) runCatching { B3NetworkService.fetchAssetData(tickerKey) }.getOrNull() else null
            val fetchedHistory = if (needsHistoryRefresh) runCatching { B3NetworkService.fetchHistoricalChart(tickerKey, localChartRange) }.getOrDefault(emptyList()) else emptyList()
            val fetchedBundle = if (shouldFetchBundleLocally) runCatching { B3NetworkService.fetchAssetChartBundle(tickerKey, localChartRange) }.getOrNull() else null
            Triple(fetchedAsset, fetchedHistory, fetchedBundle)
        }

        val (fetchedAsset, fetchedHistory, fetchedBundle) = result
        if (fetchedAsset != null) {
            assetData = fetchedAsset.mergeWithFallback(fallbackAssetData)
        } else if (!assetData.hasUsefulProxyData()) {
            assetData = fallbackAssetData
        }
        if (fetchedHistory.isNotEmpty()) {
            localChartPoints = fetchedHistory
        }
        if (fetchedBundle != null) {
            chartBundle = fetchedBundle
            if (fetchedBundle.priceHistory.isNotEmpty()) {
                localChartPoints = fetchedBundle.priceHistory
            }
        }
        val receivedFreshHistory = fetchedHistory.isNotEmpty() || fetchedBundle?.priceHistory?.isNotEmpty() == true
        if (!chartRangeChanged || receivedFreshHistory) {
            lastResolvedChartRange = localChartRange
        }
        isLoadingData = false
        isFetchingChart = false
        isLoadingChartBundle = false
    }

    // Yield on Cost usa somente dividend yield e preço confirmados pelo Proxy.
    val proxyPriceForYoc = assetData.price.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    val proxyDyForYoc = assetData.dy.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    val annualDividend = proxyPriceForYoc * (proxyDyForYoc / 100.0)
    val yieldOnCost = if (asset.averageCost > 0.0 && annualDividend > 0.0) {
        (annualDividend / asset.averageCost) * 100.0
    } else {
        0.0
    }

    val sortedTransactions = remember(transactions) {
        transactions.sortedByDescending { it.date }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // Custom full-screen dialog
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
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Modal Header Title bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "DETALHES DO ATIVO",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp,
                        color = lineColor
                    )

                    Spacer(modifier = Modifier.width(40.dp)) // Equal balance offset
                }

                run {
                    val realData = assetData

                    var mainTabIdx by remember { mutableStateOf(0) }
                    val mainTabs = listOf("Resumo", "Desempenho & Índices", "Finanças & Balanço", "Proventos & Payout", "Indicadores", "Perfil & Dados", "Minha Custódia", "Transações")
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mainTabs.size) { idx ->
                            val title = mainTabs[idx]
                            val isSelected = mainTabIdx == idx
                            Surface(
                                color = if (isSelected) GoldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) GoldPrimary else BorderColor.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.clickable { mainTabIdx = idx }
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) GoldPrimary else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Main Scrollable body with LazyColumn for high performance
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isLoadingData) {
                            item {
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.18f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Atualizando dados de $tickerKey...",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // 1. Core Badge & Corporate Identifier card
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = asset.ticker,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 24.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(lineColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = if (isFii) "FII" else "AÇÃO",
                                                    color = lineColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = realData?.name?.ifEmpty { if (isFii) "Fundo de Investimento Imobiliário" else "Ativo de Renda Variável" } ?: (if (isFii) "Fundo de Investimento Imobiliário" else "Ativo de Renda Variável"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        val proxyPrice = realData.price.takeIf { it.isFinite() && it > 0.0 }
                                        Text(
                                            text = proxyPrice?.let { "R$ ${String.format("%.2f", it)}" } ?: "Proxy indisponível",
                                            fontWeight = FontWeight.Black,
                                            fontSize = if (proxyPrice != null) 24.sp else 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val changeToDisplay = realData.changePercent.takeIf { it.isFinite() && it != 0.0 }
                                        if (changeToDisplay != null) {
                                            val isPos = changeToDisplay >= 0.0
                                            val trendColor = if (isPos) SuccessGreen else DangerRed
                                            Surface(
                                                color = trendColor.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${if (isPos) "+" else ""}${String.format("%.2f", changeToDisplay)}%",
                                                    color = trendColor,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else {
                                            Text("Variação indisponível", color = TextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Finanças & DRE: dados do VALORAE Proxy para DRE, Patrimônio, etc.
                        if (mainTabIdx == 2) {
                            val bundleProfile = chartBundle
                            if (bundleProfile != null) {
                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (isFii) {
                                        ChartCategoryHeader(
                                            title = "Ativos e Patrimônio Imobiliário",
                                            subtitle = "Composição física imobiliária, segmentos e estados"
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FiiPatrimonialTab(bundleProfile)
                                    } else {
                                        ChartCategoryHeader(
                                            title = "Finanças, Balanço e Payout",
                                            subtitle = "Receitas, lucros, lucro x cotação, Ativo / PL / Passivo e payout histórico"
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        StockDreTab(bundleProfile)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        StockBusinessTab(bundleProfile)
                                    }
                                }
                            } else {
                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ChartCardContainer(title = "Gráficos Financeiros") {
                                        EmptyChartState(
                                            title = "Dados indisponíveis",
                                            message = "O VALORAE Proxy não retornou dados de DRE ou patrimônio para este ativo."
                                        )
                                    }
                                }
                            }
                        } // End Finanças & DRE

                        // Proventos & Dividendos
                        if (mainTabIdx == 3) {
                            val currentBundle = chartBundle
                            if (currentBundle != null) {
                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ChartCategoryHeader(
                                        title = "Proventos, Dividendos e Yield",
                                        subtitle = if (isFii) "Distribuições, dividend yield e histórico de rendimentos" else "Dividendos, dividend yield e eventos de distribuição"
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (isFii) {
                                        FiiDividendTab(currentBundle)
                                    } else {
                                        StockDividendTab(currentBundle)
                                    }
                                }
                            } else {
                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ChartCardContainer(title = "Histórico de Proventos") {
                                        EmptyChartState(
                                            title = "Dados indisponíveis",
                                            message = "O VALORAE Proxy não retornou histórico de proventos para este ativo."
                                        )
                                    }
                                }
                            }
                        } // End Proventos & Dividendos

                        // 1.2 Oscilação 52 Semanas (if available)
                        if (mainTabIdx == 0) {
                        if (realData != null && realData.high52 > 0.0 && realData.low52 > 0.0) {
                            item {
                                Surface(
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "OSCILAÇÃO 52 SEMANAS",
                                            color = GoldPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Mín 52sem", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                Text("Máx 52sem", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .background(BorderColor.copy(alpha = 0.1f), CircleShape)
                                            ) {
                                                val low = realData.low52
                                                val high = realData.high52
                                                val current = realData.price
                                                if (high > low && current >= low) {
                                                    val progress = ((current - low) / (high - low)).coerceIn(0.0, 1.0).toFloat()
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(progress)
                                                            .fillMaxHeight()
                                                            .background(
                                                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                                    listOf(GoldPrimary.copy(alpha = 0.6f), GoldPrimary)
                                                                ),
                                                                shape = CircleShape
                                                            )
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("R$ ${String.format("%.2f", realData.low52)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                Text("R$ ${String.format("%.2f", realData.high52)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        } // End Tab 0

                        // Indicadores Gerais: todos os indicadores vêm do VALORAE Proxy ou do bundle oficial.
                        if (mainTabIdx == 4) {
                            item {
                                AssetProxyIndicatorSection(
                                    assetData = realData,
                                    bundle = chartBundle,
                                    isFii = isFii,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } // End Indicadores Gerais

                        // Perfil & Dados
                        if (mainTabIdx == 5) {
                            item {
                                AssetProxyProfileSection(
                                    assetData = realData,
                                    bundle = chartBundle,
                                    isFii = isFii,
                                    newsItems = newsItems,
                                    isLoadingNews = isLoadingNews,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } // End Perfil & Dados

                        // 3. Interactive Chart
                        if (mainTabIdx == 0) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Gráfico Histórico",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        // Selector range pills
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.horizontalScroll(rememberScrollState())
                                        ) {
                                            listOf("1D", "5D", "1M", "6M", "YTD", "1Y", "5Y", "MAX").forEach { rawRange ->
                                                val range = rawRange.lowercase()
                                                val isSelected = range == localChartRange.lowercase()
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) lineColor else Color.Transparent)
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            localChartRange = rawRange
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = range.uppercase(),
                                                        color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    if (isFetchingChart && localChartPoints.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = lineColor, strokeWidth = 3.dp)
                                        }
                                    } else if (localChartPoints.isNotEmpty()) {
                                        HistoricalPriceLineChart(
                                            points = localChartPoints,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            lineColor = lineColor
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Histórico indisponível", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        } // end of Interactive Chart constraint

                        // Desempenho, rentabilidade e comparação ficam na aba própria.
                        if (mainTabIdx == 1) { // Desempenho & Índices
                        item {
                            val bundle = chartBundle
                            if (isLoadingChartBundle) {
                                Surface(
                                    color = DarkSurfaceElevated,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.05f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Carregando desempenho, rentabilidade e índices...", color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            } else if (bundle != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                ChartCategoryHeader(
                                    title = "Desempenho e Rentabilidade",
                                    subtitle = if (isFii) "Evolução do FII e histórico de retornos" else "Rentabilidade histórica comparando nominal vs real"
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (isFii) {
                                    FiiGeneralTab(bundle)
                                } else {
                                    StockAnalysisTab(bundle)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                ChartCategoryHeader(
                                    title = "Comparação e Correlações",
                                    subtitle = if (isFii) "Retorno acumulado contra o IFIX e médias do segmento" else "Evolução comparativa contra índices e cotação de commodities"
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (isFii) {
                                    FiiComparisonTab(bundle)
                                } else {
                                    StockComparisonTab(bundle)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            } else {
                                Surface(
                                    color = DarkSurfaceElevated,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.08f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Outlined.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Dados avançados ainda não disponíveis", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("O resumo do ativo continua disponível; tente atualizar ou trocar o período.", color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }

                        } // end of mainTabIdx == 0

                        if (mainTabIdx == 6) { // Detalhes da Posição
                        // 2. Personal Holdings Dashboard summary
                        item {
                            Text(
                                text = "MINHA CARTEIRA DE CUSTÓDIA",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    // Title row: total position val
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Saldo Atual",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "R$ ${String.format("%,.2f", asset.totalCurrentValue)}",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }

                                        // Return indicator
                                        val isReturnPos = asset.totalReturn >= 0.0
                                        val returnColor = if (isReturnPos) SuccessGreen else DangerRed
                                        Box(
                                            modifier = Modifier
                                                .background(returnColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                                .border(1.dp, returnColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isReturnPos) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                                                    contentDescription = null,
                                                    tint = returnColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${if (isReturnPos) "+" else ""}${String.format("%.1f", asset.returnPercent)}%",
                                                    color = returnColor,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = BorderColor.copy(alpha = 0.08f), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Grid of support indicators
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Cotas/Frações", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                            Text(
                                                text = if (asset.sharesCount % 1.0 == 0.0) asset.sharesCount.toInt().toString() else String.format("%.2f", asset.sharesCount),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Preço Médio", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                            Text(
                                                text = "R$ ${String.format("%.2f", asset.averageCost)}",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                            Text("Total Investido", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                            Text(
                                                text = "R$ ${String.format("%.2f", asset.totalInvested)}",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = BorderColor.copy(alpha = 0.08f), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Yield on Cost Highlight panel
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(lineColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Yield on Cost (YOC)",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Rentabilidade real sobre as compras realizadas.",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontSize = 8.sp,
                                                maxLines = 1
                                            )
                                        }

                                        Text(
                                            text = "${String.format("%.2f", yieldOnCost)}%",
                                            color = lineColor,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }

                        } // end of mainTabIdx == 6

                        if (mainTabIdx == 7) { // Transações
                        // 4. Detailed Purchase Logs
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "HISTÓRICO DE TRANSAÇÕES",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 12.dp)
                            )
                        }

                        if (transactions.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "Nenhuma transação registrada.", 
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        } else {
                            items(sortedTransactions, key = { it.id }) { tx ->
                                val isSale = tx.isSell
                                val isPurchase = !isSale
                                val itemColor = if (isSale) DangerRed else SuccessGreen
                                val bgColor = itemColor.copy(alpha = 0.05f)
                                
                                val currentPriceToUse = realData.price.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
                                val txTotalValue = tx.quantity * tx.purchasePrice
                                
                                val currentTxValue = if (isPurchase) currentPriceToUse * tx.quantity else 0.0
                                val profitAbs = if (isPurchase) currentTxValue - txTotalValue else 0.0
                                val profitPct = if (isPurchase && txTotalValue > 0) (profitAbs / txTotalValue) * 100.0 else 0.0
                                val isProfit = profitAbs >= 0

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Column {
                                        // Main Header Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Icon
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(bgColor, RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isSale) Icons.AutoMirrored.Outlined.TrendingDown else Icons.AutoMirrored.Outlined.TrendingUp,
                                                    contentDescription = null,
                                                    tint = itemColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(16.dp))
                                            
                                            // Titles and Qty
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        color = itemColor.copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isSale) "VENDA" else "COMPRA",
                                                            color = itemColor,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(tx.date)),
                                                        color = TextSecondary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                val fmtQty = if (tx.quantity % 1.0 == 0.0) tx.quantity.toInt().toString() else String.format("%.2f", tx.quantity)
                                                Text(
                                                    text = "$fmtQty cotas",
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            
                                            // Price & Total
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "R$ ${String.format("%.2f", txTotalValue)}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 15.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "R$ ${String.format("%.2f", tx.purchasePrice)} /cota",
                                                    color = TextSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        // Expanded detail or bottom row
                                        HorizontalDivider(color = BorderColor.copy(alpha = 0.1f), thickness = 1.dp)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isPurchase && txTotalValue > 0) {
                                                    val pColor = if (isProfit) SuccessGreen else DangerRed
                                                    val icon = if (isProfit) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = pColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "${if (isProfit) "+" else ""}R$ ${String.format("%.2f", profitAbs)} (${String.format("%.2f", profitPct)}%)",
                                                        color = pColor,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                } else {
                                                    Text("Status indisponível", color = TextSecondary, fontSize = 13.sp)
                                                }
                                            }
                                            
                                            // Action buttons
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Edit,
                                                    contentDescription = "Editar",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp).clickable { onEditTransaction(tx) }
                                                )
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Deletar",
                                                    tint = DangerRed,
                                                    modifier = Modifier.size(20.dp).clickable { onDeleteTransaction(tx) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        } // end mainTabIdx == 7

                        item {
                            Spacer(modifier = Modifier.navigationBarsPadding().height(56.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IndicatorItemFromAnalysis(
    label: String,
    value: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(), 
            color = GoldPrimary, 
            fontSize = 8.sp, 
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value, 
            color = MaterialTheme.colorScheme.onSurface, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Black
        )
        if (desc.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), 
                fontSize = 8.sp, 
                lineHeight = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun AssetSummary.toFallbackB3AssetData(): B3AssetData {
    val inferredFii = type.equals("FII", ignoreCase = true) || B3NetworkService.inferIsFii(ticker)
    return B3AssetData(
        ticker = ticker.trim().uppercase(),
        name = ticker.trim().uppercase(),
        price = 0.0,
        changePercent = 0.0,
        dy = 0.0,
        lastDividend = 0.0,
        isFii = inferredFii,
        source = "Carteira local — aguardando VALORAE Proxy"
    )
}

private fun B3AssetData.hasUsefulProxyData(): Boolean {
    return price > 0.0 || dy > 0.0 || pvp > 0.0 || pl > 0.0 || marketCap > 0.0 ||
        assetDescription.isNotBlank() || cnpj.isNotBlank() || extractionCompleteness > 0.0
}

private fun B3AssetData.mergeWithFallback(fallback: B3AssetData): B3AssetData {
    return copy(
        name = name.ifBlank { fallback.name },
        price = price,
        changePercent = changePercent,
        dy = dy,
        lastDividend = lastDividend,
        isFii = isFii || fallback.isFii,
        source = if (source.isNotBlank()) source else fallback.source
    )
}
