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

    // Calculate Yield on Cost (YOC)
    val annualDividend = asset.currentPrice * (asset.dividendYield / 100.0)
    val yieldOnCost = if (asset.averageCost > 0.0) {
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
                    val mainTabs = listOf("Resumo & Gráficos", "Indicadores & Perfil", "Minha Custódia", "Transações")
                    
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
                                            text = "Atualizando dados do Proxy para $tickerKey...",
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
                                        val priceToDisplay = realData?.price ?: asset.currentPrice
                                        Text(
                                            text = "R$ ${String.format("%.2f", priceToDisplay)}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val changeToDisplay = realData?.changePercent ?: asset.dailyChangePercent
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
                                    }
                                }
                            }
                        }

                        // 1.05 Diagnóstico de dados do Valorae Proxy
                        if (mainTabIdx == 1) {
                        if (realData != null) {
                            item {
                                val filledFields = listOf(
                                    realData.price, realData.dy, realData.pvp, realData.vpa, realData.lastDividend,
                                    realData.marketCap, realData.roe, realData.roic, realData.margins,
                                    realData.dailyLiquidity, realData.high52, realData.low52
                                ).count { it > 0.0 } + listOf(
                                    realData.name, realData.cnpj, realData.assetDescription, realData.subSector,
                                    realData.fiiSegment, realData.fiiTotalHolders, realData.fiiIssuedShares
                                ).count { it.isNotBlank() }
                                val completeness = ((filledFields / 19.0) * 100.0).coerceIn(0.0, 100.0)
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.06f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.16f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("DADOS RECEBIDOS PELO PROXY", color = GoldPrimary, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.8.sp)
                                            Text("${String.format("%.0f", completeness)}%", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        }
                                        Text(
                                            text = "Fonte: ${realData.source}. Campos ausentes permanecem vazios para evitar dados simulados.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        // 1.1 Perfil Operacional card (if available)
                        if (realData != null && realData.assetDescription.isNotEmpty()) {
                            item {
                                Surface(
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "PERFIL OPERACIONAL",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = GoldPrimary,
                                                letterSpacing = 0.5.sp
                                            )
                                            val actSector = if (isFii) realData.fiiSegment else realData.subSector
                                            if (actSector.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(DarkBackground, RoundedCornerShape(8.dp))
                                                        .border(1.dp, BorderColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = actSector,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = realData.assetDescription,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        } // End mainTabIdx == 1

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

                        // 1.3 Indicadores Fundamentalistas Grid
                        if (mainTabIdx == 1) {
                        item {
                            Surface(
                                color = DarkSurface,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "INDICADORES FUNDAMENTALISTAS",
                                        color = GoldPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))

                                    val metrics = mutableListOf<Triple<String, String, String>>()

                                    if (realData != null) {
                                        metrics.add(Triple("Dividend Yield", B3UIUtils.formatValue(realData.dy, suffix = "%"), "Retorno em proventos"))
                                        metrics.add(Triple("P/VP", B3UIUtils.formatValue(realData.pvp), "Preço / Valor Patrimonial"))
                                        metrics.add(Triple(if (isFii) "Últ. Provento" else "P/L", if (isFii) B3UIUtils.formatValue(realData.lastDividend, prefix = "R$ ") else B3UIUtils.formatValue(realData.pl), if (isFii) "Baseado na última distr." else "Preço / Lucro anual"))
                                        metrics.add(Triple("VPA", B3UIUtils.formatValue(realData.vpa, prefix = "R$ "), "Valor Justo Contábil"))
                                        
                                        if (isFii) {
                                            metrics.add(Triple("Vacância", B3UIUtils.formatValue(realData.fiiVacancy, suffix = "%", precision = 1), "Proporção de área vaga nos imóveis"))
                                            metrics.add(Triple("Liquidez Diária", B3UIUtils.formatLargeNumber(realData.dailyLiquidity).replace("R$ ", ""), "Volume financeiro mensal"))
                                            metrics.add(Triple("Segmento", B3UIUtils.formatText(realData.fiiSegment, "Outros"), "Tipo de operação do FII"))
                                            metrics.add(Triple("Número de Imóveis", if (realData.fiiPropertyCount == 0) "--" else "${realData.fiiPropertyCount} prop.", "Ativos físicos"))
                                            metrics.add(Triple("P/VP Máximo Alvo", "1.00", "Parâmetro do mercado de tijolo"))
                                            if (realData.magicNumber > 0) {
                                                metrics.add(Triple("Magic Number", "${realData.magicNumber.toInt()} cotas", "Para comprar 1 cota c/ div."))
                                            }
                                         } else {
                                             metrics.add(Triple("LPA", B3UIUtils.formatValue(realData.lpa, prefix = "R$ "), "Lucro líquido por ação anual"))
                                             metrics.add(Triple("P/Receita (PSR)", B3UIUtils.formatValue(realData.priceToSales), "Preço / Receita Líquida"))
                                             metrics.add(Triple("Margem Líquida", B3UIUtils.formatValue(realData.margins, suffix = "%"), "Eficiência líquida"))
                                             metrics.add(Triple("Margem Bruta", B3UIUtils.formatValue(realData.grossMargin, suffix = "%"), "Eficiência bruta"))
                                             metrics.add(Triple("Margem Ebit", B3UIUtils.formatValue(realData.ebitMargin, suffix = "%"), "Eficiência Ebit"))
                                             metrics.add(Triple("Margem Ebitda", B3UIUtils.formatValue(realData.ebitdaMargin, suffix = "%"), "Eficiência Ebtida"))
                                             metrics.add(Triple("EV/Ebitda", B3UIUtils.formatValue(realData.evEbitda), "Valor da Firma / Ebitda"))
                                             metrics.add(Triple("EV/Ebit", B3UIUtils.formatValue(realData.evEbit), "Valor da Firma / Ebit"))
                                             metrics.add(Triple("P/Ebitda", B3UIUtils.formatValue(realData.priceEbitda), "Preço / Ebitda"))
                                             metrics.add(Triple("P/Ebit", B3UIUtils.formatValue(realData.priceEbit), "Preço / Ebit"))
                                             metrics.add(Triple("P/Ativo", B3UIUtils.formatValue(realData.priceAsset), "Preço / Ativo Total"))
                                             metrics.add(Triple("P/Cap.Giro", B3UIUtils.formatValue(realData.priceCapGiro), "Preço / Capital de Giro"))
                                             metrics.add(Triple("P/Ativo Circ. Liq.", B3UIUtils.formatValue(realData.priceAtivoCircLiq), "Preço / Ativo Circ. Líq."))
                                             metrics.add(Triple("Giro Ativos", B3UIUtils.formatValue(realData.giroAtivos), "Giro de Ativos"))
                                             metrics.add(Triple("ROE", B3UIUtils.formatValue(realData.roe, suffix = "%"), "Retorno s/ Patrimônio Líq."))
                                             metrics.add(Triple("ROIC", B3UIUtils.formatValue(realData.roic, suffix = "%"), "Retorno s/ Capital Invest."))
                                             metrics.add(Triple("ROA", B3UIUtils.formatValue(realData.roa, suffix = "%"), "Retorno s/ Ativos"))
                                             metrics.add(Triple("Dív. Líq / Patrimônio", B3UIUtils.formatValue(realData.divLiqPatrimonio), "Dívida Líquida / Patrimônio"))
                                             metrics.add(Triple("Dív. Líq / EBITDA", B3UIUtils.formatValue(realData.debtEbitda), "Dívida Líquida / EBITDA"))
                                             metrics.add(Triple("Dívida Líq / Ebit", B3UIUtils.formatValue(realData.divLiqEbit), "Dívida Líq. / EBIT"))
                                             metrics.add(Triple("Dívida Bruta / Patrim.", B3UIUtils.formatValue(realData.divBrutaPatrimonio), "Dívida Bruta / Patrimônio"))
                                             metrics.add(Triple("Patrimônio / Ativos", B3UIUtils.formatValue(realData.patrimonioAtivos), "Patrimônio / Ativos"))
                                             metrics.add(Triple("Passivos / Ativos", B3UIUtils.formatValue(realData.passivosAtivos), "Passivos / Ativos"))
                                             metrics.add(Triple("Liquidez Corrente", B3UIUtils.formatValue(realData.liquidezCorrente), "Liquidez Corrente"))
                                             metrics.add(Triple("CAGR Receitas (5a)", B3UIUtils.formatValue(realData.cagrRevenue5y, suffix = "%"), "Cresc. Receita Anual"))
                                             metrics.add(Triple("CAGR Lucros (5a)", B3UIUtils.formatValue(realData.cagrProfit5y, suffix = "%"), "Cresc. Lucro Anual"))
                                             metrics.add(Triple("Payout", B3UIUtils.formatValue(realData.payout, suffix = "%"), "Lucro distribuído"))
                                         }
                                    } else {
                                        // Fallback to local asset stats
                                        metrics.add(Triple("Dividend Yield", B3UIUtils.formatValue(asset.dividendYield, suffix = "%"), "Últimos 12 meses"))
                                        metrics.add(Triple("P/VP", "--", "Preço / Valor Patrimonial (Est.)"))
                                        metrics.add(Triple("VPA", "--", "Valor Patrimonial por Ação (Est.)"))
                                        metrics.add(Triple("Últ. Provento", B3UIUtils.formatValue(asset.lastDividend, prefix = "R$ "), "Última distribuição paga"))
                                    }

                                    // Completa o grid com indicadores vindos do bundle avançado.
                                    // Alguns campos do Investidor10/Proxy chegam somente no pacote de gráficos
                                    // e não no B3AssetData resumido; sem este merge a aba Detalhes parecia incompleta.
                                    val existingMetricLabels = metrics.map { it.first.lowercase() }.toMutableSet()
                                    chartBundle?.indicatorCards
                                        ?.filter { it.label.isNotBlank() && (it.display.isNotBlank() || it.value.isFinite()) }
                                        ?.forEach { point ->
                                            val key = point.label.lowercase()
                                            if (!existingMetricLabels.contains(key)) {
                                                val valueText = point.display.ifBlank {
                                                    when (point.unit) {
                                                        "%" -> B3UIUtils.formatValue(point.value, suffix = "%")
                                                        "BRL" -> B3UIUtils.formatValue(point.value, prefix = "R$ ")
                                                        "number" -> B3UIUtils.formatLargeNumber(point.value).replace("R$ ", "")
                                                        else -> B3UIUtils.formatValue(point.value)
                                                    }
                                                }
                                                metrics.add(Triple(point.label, valueText, "Dado normalizado pelo Valorae Proxy/Investidor10"))
                                                existingMetricLabels.add(key)
                                            }
                                        }

                                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                        metrics.chunked(2).forEachIndexed { index, rowItems ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                rowItems.forEach { (label, value, desc) ->
                                                    IndicatorItemFromAnalysis(
                                                        label = label,
                                                        value = value,
                                                        desc = desc,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                if (rowItems.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                            if (index < (metrics.size + 1) / 2 - 1) {
                                                HorizontalDivider(color = BorderColor.copy(alpha = 0.08f), thickness = 1.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 1.4 Conselho Valorae (Qualitative Analysis Box)
                        if (realData != null) {
                            item {
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "CONSELHO VALORAE",
                                                color = GoldPrimary,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val advice = when {
                                                isFii && realData.fiiVacancy > 15.0 -> 
                                                    "ALERTA: Vacância acima de 15%. Verifique o motivo da desocupação e a localização dos imóveis antes de aportar."
                                                !isFii && realData.debtEbitda > 4.0 -> 
                                                    "CUIDADO: Alavancagem financeira elevada (Dívida/EBITDA > 4x). A empresa pode ter dificuldades em cenários de juros altos."
                                                realData.pvp < 0.8 && realData.dy > 8.0 -> 
                                                    "OPORTUNIDADE: Ativo descontado (P/VP < 0.8) e com DY atrativo. Pode haver uma distorção de preço favorável."
                                                realData.pvp > 1.5 -> 
                                                    "ÁGIO ELEVADO: O mercado está pagando um prêmio alto por este ativo. Certifique-se de que o crescimento futuro justifica o preço."
                                                else -> 
                                                    "FUNDAMENTOS SÓLIDOS: Ativo com indicadores em equilíbrio. Ideal para estratégia de longo prazo focada em rendimentos."
                                            }
                                            Text(
                                                text = advice,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // 1.5 Valuation Section
                            item {
                                Surface(
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "ANÁLISE DE VALUATION E PREÇO TETO",
                                            color = GoldPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))

                                        val grahamValue = kotlin.math.sqrt(22.5 * realData.lpa * realData.vpa).takeIf { !it.isNaN() } ?: 0.0
                                        val marginGraham = if (grahamValue > 0) ((grahamValue / realData.price) - 1.0) * 100 else 0.0
                                        
                                        if (!isFii) {
                                            // Graham Formula
                                            val marginColorG = if (marginGraham > 0) SuccessGreen else DangerRed

                                            Surface(
                                                color = DarkBackground,
                                                shape = RoundedCornerShape(16.dp),
                                                border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text("Fórmula de Benjamin Graham", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Preço Justo (VI): R$ ${String.format("%.2f", grahamValue)}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                                        Box(modifier = Modifier.background(marginColorG.copy(alpha=0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                            Text("Margem: ${String.format("%+.1f%%", marginGraham)}", color = marginColorG, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }

                                        // Décio Bazin Formula (Based on Dividends)
                                        val bazinYieldTarget = 0.06 // 6%
                                        val bazinValue = realData.lastDividend / bazinYieldTarget
                                        val marginBazin = if (bazinValue > 0) ((bazinValue / realData.price) - 1.0) * 100 else 0.0
                                        val marginColorB = if (marginBazin > 0) SuccessGreen else DangerRed

                                        Surface(
                                            color = DarkBackground,
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text("Método Décio Bazin (Mín. 6% DY)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Preço Teto: R$ ${String.format("%.2f", bazinValue)}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                                    Box(modifier = Modifier.background(marginColorB.copy(alpha=0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                        Text("Margem: ${String.format("%+.1f%%", marginBazin)}", color = marginColorB, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        } // End Tab 1

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

                        // 1.5 Investidor10 Chart Bundle panel
                        if (mainTabIdx == 0) { // Gráficos Avançados
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
                                        Text("Carregando gráficos de análise...", color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            } else if (bundle != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                AssetChartBundlePanel(
                                    bundle = bundle,
                                    isFii = isFii,
                                    currentRange = localChartRange,
                                    onRangeChange = { selectedRange ->
                                        localChartRange = selectedRange
                                        onRangeChange(selectedRange)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
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

                        if (mainTabIdx == 2) { // Detalhes da Posição
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

                        } // end of mainTabIdx == 2

                        if (mainTabIdx == 3) { // Transações
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
                                
                                val currentPriceToUse = realData?.price ?: asset.currentPrice
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
                        } // end mainTabIdx == 3

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
        price = currentPrice.takeIf { it > 0.0 } ?: averageCost,
        changePercent = dailyChangePercent,
        dy = dividendYield,
        lastDividend = lastDividend,
        isFii = inferredFii,
        source = "Carteira local + aguardando Proxy"
    )
}

private fun B3AssetData.hasUsefulProxyData(): Boolean {
    return price > 0.0 || dy > 0.0 || pvp > 0.0 || pl > 0.0 || marketCap > 0.0 ||
        assetDescription.isNotBlank() || cnpj.isNotBlank() || name.isNotBlank()
}

private fun B3AssetData.mergeWithFallback(fallback: B3AssetData): B3AssetData {
    return copy(
        name = name.ifBlank { fallback.name },
        price = if (price > 0.0) price else fallback.price,
        changePercent = if (changePercent != 0.0) changePercent else fallback.changePercent,
        dy = if (dy > 0.0) dy else fallback.dy,
        lastDividend = if (lastDividend > 0.0) lastDividend else fallback.lastDividend,
        isFii = isFii || fallback.isFii,
        source = if (source.isNotBlank()) source else fallback.source
    )
}
