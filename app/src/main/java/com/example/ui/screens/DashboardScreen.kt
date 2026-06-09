package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.ui.components.SegmentedAllocationBar
import com.example.ui.components.AssetDetailModal
import com.example.ui.theme.*
import com.example.viewmodel.AssetSummary
import com.example.viewmodel.PortfolioSummary
import com.example.network.B3AssetData
import com.example.network.AssetChartBundle
import com.example.network.B3NetworkService
import com.example.viewmodel.PortfolioAnalyticsState
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    summary: PortfolioSummary,
    assets: List<AssetSummary>,
    transactions: List<Transaction>,
    cachedAssetData: Map<String, B3AssetData> = emptyMap(),
    assetChartBundles: Map<String, AssetChartBundle> = emptyMap(),
    isLoadingChartBundle: Boolean = false,
    onLoadAssetChartBundle: (String, String) -> Unit = { _, _ -> },
    chartHistory: List<com.example.network.ChartPoint> = emptyList(),
    chartRange: String = "1y",
    onRangeChange: (String) -> Unit = {},
    isSearchingChart: Boolean = false,
    hideValues: Boolean = false,
    onAddTransaction: (String, Double, Double, String, String, String, Long?, String, Boolean) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onUpdateTransaction: (Int, String, Double, Double, String, String, String, Long?, String, Boolean) -> Unit = {_,_,_,_,_,_,_,_,_,_ ->},
    onAssetClick: (String) -> Unit,
    onPortfolioClick: () -> Unit = {},
    updateStatus: com.example.network.UpdateManager.UpdateStatus = com.example.network.UpdateManager.UpdateStatus.Idle,
    analytics: PortfolioAnalyticsState = PortfolioAnalyticsState(),
    onUpdateAvailable: () -> Unit = {},
    onRefreshRankings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Screen Header
            item(key = "dashboard_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Meus Investimentos",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Acompanhe sua carteira.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // 1. Portfolio Value Summary Widget
            item(key = "portfolio_header") {
                val totalDividendsReceived = remember(analytics.dividendEvents, transactions) {
                    analytics.dividendEvents
                        .filter { isPaidDividendEvent(it) }
                        .sumOf { eligibleDividendAmount(it, transactions) }
                }
                PortfolioHeaderCard(
                    summary = summary,
                    totalProventos = totalDividendsReceived,
                    hideValues = hideValues,
                    onClick = onPortfolioClick
                )
            }

            // Diversification Ratio Widget removed per user request

            item(key = "home_market_movers") {
                HomeMarketMoversPreview(
                    ranking = analytics.liveMarketRanking,
                    assetData = cachedAssetData,
                    onAssetClick = onAssetClick,
                    isLoading = analytics.isLoading,
                    rankingsAttempted = analytics.marketRankingsAttempted,
                    onRetry = onRefreshRankings
                )
            }
        }
    }
}


@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (selected) GoldPrimary else TextSecondary,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            fontSize = 14.sp
        )
        if (selected) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.width(40.dp).height(3.dp).background(GoldPrimary, CircleShape))
        }
    }
}

@Composable
fun MarketGlanceWidget() {
    val indices = remember {
        listOf(
            Triple("IBOV", "124.810", 1.25),
            Triple("IFIX", "3.394", -0.08),
            Triple("DÓLAR", "R$ 5,14", 0.32)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.04f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            indices.forEachIndexed { index, (name, value, change) ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(BorderColor.copy(alpha = 0.15f))
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = name,
                            color = TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        )
                        if (change != 0.0) {
                            val isPos = change > 0
                            val color = if (isPos) SuccessGreen else DangerRed
                            Text(
                                text = "${if (isPos) "▲" else "▼"}${String.format("%.1f", Math.abs(change))}%",
                                color = color,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = value,
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioHeaderCard(
    summary: PortfolioSummary,
    totalProventos: Double = 0.0,
    hideValues: Boolean = false,
    onClick: () -> Unit = {}
) {
    val gradientBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                DarkSurfaceElevated,
                DarkSurface
            )
        )
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .background(gradientBrush, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Wallet indicator & Quick Analysis text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(GoldPrimary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Patrimônio Consolidado",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "VALE ATUAL DA CARTEIRA",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                
                // Click badge
                Surface(
                    color = GoldPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "Análise ➔",
                        color = GoldPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Total net worth and absolute return chip in one row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (hideValues) "R$ •••••" else "R$ ${String.format("%,.2f", summary.totalCurrentValue)}",
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.0).sp
                    )
                }
                
                // Rentabilidade Chip (Pill shaped)
                val retPct = summary.returnPercent
                val isPos = retPct >= 0
                val chipBg = if (summary.totalInvested <= 0.0) Color.White.copy(alpha = 0.05f) else if (isPos) SuccessGreen.copy(alpha = 0.12f) else DangerRed.copy(alpha = 0.12f)
                val chipTxt = if (summary.totalInvested <= 0.0) TextSecondary else if (isPos) SuccessGreen else DangerRed
                
                Surface(
                    color = chipBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, chipTxt.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPos) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = chipTxt,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hideValues) "••%" else String.format("%+.2f%%", retPct),
                            color = chipTxt,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Micro-cards grid for extra details (Total Invested, Return Real, and Dividends)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Micro-Card: Aplicado
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "TOTAL INVESTIDO",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hideValues) "R$ •••••" else "R$ ${String.format("%,.2f", summary.totalInvested)}",
                        color = TextPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Middle Micro-Card: Retorno Total Real
                val retTotal = summary.totalReturn
                val valueColor = if (summary.totalInvested <= 0.0) TextSecondary else if (retTotal >= 0) SuccessGreen else DangerRed
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "RETORNO TOTAL",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hideValues) "R$ •••••" else "R$ ${String.format("%,.2f", retTotal)}",
                        color = valueColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right Micro-Card: Proventos Recebidos
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "PROVENTOS REC.",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hideValues) "R$ •••••" else "R$ ${String.format("%,.2f", totalProventos)}",
                        color = GoldPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Atualizado em tempo real", 
                    color = TextSecondary.copy(alpha = 0.8f), 
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Toque para ver detalhes", 
                    color = GoldPrimary, 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun IndicatorItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(5.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label $value",
            color = TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun HoldingsListItem(
    asset: AssetSummary,
    hideValues: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("asset_item_${asset.ticker}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = asset.ticker,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (asset.type == "FII") "FII" else "AÇÃO",
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            val formattedShares = if (asset.sharesCount % 1.0 == 0.0) {
                asset.sharesCount.toInt().toString()
            } else {
                String.format("%.2f", asset.sharesCount)
            }
            Text(
                text = if (hideValues) "•• cotas • Médio: R$ ••" else "$formattedShares cotas • Médio: R$ ${String.format("%.2f", asset.averageCost)}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (hideValues) "R$ ••" else "R$ ${String.format("%,.2f", asset.totalCurrentValue)}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isMarketPositive = asset.dailyChangePercent >= 0.0
                val marketAccentColor = if (isMarketPositive) SuccessGreen else DangerRed
                Text(
                    text = if (hideValues) "••%" else "${if (isMarketPositive) "+" else ""}${String.format("%.2f", asset.dailyChangePercent)}%",
                    color = marketAccentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                val isTotalPositive = asset.returnPercent >= 0.0
                val totalAccentColor = if (isTotalPositive) SuccessGreen else DangerRed
                Text(
                    text = if (hideValues) "••%" else "(${if (isTotalPositive) "+" else ""}${String.format("%.2f", asset.returnPercent)}%)",
                    color = totalAccentColor.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Detalhes",
            tint = TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun TransactionHistoryItem(
    tx: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tx.ticker,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (tx.type == "FII") {
                    Text(
                        text = "FII",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            val formattedQty = if (tx.quantity % 1.0 == 0.0) {
                tx.quantity.toInt().toString()
            } else {
                String.format("%.2f", tx.quantity)
            }
            Text(
                text = "${if(tx.isSell) "Venda" else "Compra"} de $formattedQty a R$ ${String.format("%.2f", tx.purchasePrice)}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "R$ ${String.format("%,.2f", tx.quantity * tx.purchasePrice)}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if(tx.isSell) "- Saída" else "+ Entrada",
                color = if(tx.isSell) DangerRed else SuccessGreen,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Editar Transação",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Deletar Transação",
                tint = DangerRed,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EmptyStateWidget(title: String, desc: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    transactionToEdit: Transaction? = null,
    cachedAssetData: Map<String, com.example.network.B3AssetData> = emptyMap(),
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String, String, String, Long?, String, Boolean) -> Unit
) {
    var ticker by remember { mutableStateOf(transactionToEdit?.ticker ?: "") }
    var type by remember { mutableStateOf(transactionToEdit?.type ?: "ACAO") } // "ACAO" or "FII"
    var isSell by remember { mutableStateOf(transactionToEdit?.isSell ?: false) }
    var quantity by remember {
        mutableStateOf(
            transactionToEdit?.quantity?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else String.format(java.util.Locale.US, "%.2f", it)
            } ?: ""
        )
    }
    var price by remember {
        mutableStateOf(
            transactionToEdit?.purchasePrice?.let {
                String.format(java.util.Locale("pt", "BR"), "%,.2f", it)
            } ?: ""
        )
    }
    var otherCosts by remember { mutableStateOf("0,00") }
    
    var dateStr by remember {
        mutableStateOf(
            transactionToEdit?.date?.let {
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
            } ?: java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        )
    }
    
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Value total calculation
    val qVal = quantity.replace(",", ".").toDoubleOrNull() ?: 0.0
    val pVal = price.let {
        if (it.contains(",") && it.contains(".")) {
            it.replace(".", "").replace(",", ".").toDoubleOrNull()
        } else if (it.contains(",")) {
            it.replace(",", ".").toDoubleOrNull()
        } else {
            it.toDoubleOrNull()
        }
    } ?: 0.0
    val cVal = otherCosts.let {
        if (it.contains(",") && it.contains(".")) {
            it.replace(".", "").replace(",", ".").toDoubleOrNull()
        } else if (it.contains(",")) {
            it.replace(",", ".").toDoubleOrNull()
        } else {
            it.toDoubleOrNull()
        }
    } ?: 0.0
    val valorTotal = (qVal * pVal) + cVal

    var expandedType by remember { mutableStateOf(false) }

    // Auto lookup and auto-fill price from proxy cache
    LaunchedEffect(ticker) {
        val upperTicker = ticker.trim().uppercase()
        val asset = cachedAssetData[upperTicker]
        if (asset != null) {
            type = if (asset.isFii) "FII" else "ACAO"
            if ((price.isBlank() || price == "0,00" || price == "0.00") && asset.price > 0.0) {
                price = String.format(java.util.Locale("pt", "BR"), "%,.2f", asset.price)
            }
        }
    }

    val matchedAsset = remember(ticker, cachedAssetData) {
        cachedAssetData[ticker.trim().uppercase()]
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 480.dp)
                    .clickable(enabled = false) {} // Prevent close on card clicking
                    .animateContentSize(),
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2C2C))
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (transactionToEdit != null) "Editar Transação" else "Nova Transação",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.White.copy(alpha = 0.04f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    // Compra/Venda Segmented Pill Control
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF141414),
                        border = BorderStroke(1.dp, Color(0xFF222222))
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            // Compra
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isSell = false },
                                shape = RoundedCornerShape(8.dp),
                                color = if (!isSell) SuccessGreen.copy(alpha = 0.15f) else Color.Transparent,
                                border = BorderStroke(1.dp, if (!isSell) SuccessGreen.copy(alpha = 0.4f) else Color.Transparent)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                                        contentDescription = null,
                                        tint = if (!isSell) SuccessGreen else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Compra",
                                        color = if (!isSell) SuccessGreen else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            // Venda
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isSell = true },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSell) DangerRed.copy(alpha = 0.15f) else Color.Transparent,
                                border = BorderStroke(1.dp, if (isSell) DangerRed.copy(alpha = 0.4f) else Color.Transparent)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isSell) DangerRed else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Venda",
                                        color = if (isSell) DangerRed else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Tipo de Ativo Selector
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Text(
                            "TIPO DE ATIVO",
                            color = GoldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expandedType,
                            onExpandedChange = { expandedType = it }
                        ) {
                            OutlinedTextField(
                                value = if (type == "ACAO") "Ações (Renda Variável)" else "Fundos de Investimento Imobiliário (FII)",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFF444444),
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedType,
                                onDismissRequest = { expandedType = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Ações (B3)", fontSize = 13.sp) },
                                    onClick = { type = "ACAO"; expandedType = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Fundos Imobiliários (FII)", fontSize = 13.sp) },
                                    onClick = { type = "FII"; expandedType = false }
                                )
                            }
                        }
                    }
                    
                    // Ativo
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Text(
                            "TICKER DO ATIVO",
                            color = GoldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = ticker,
                            onValueChange = { 
                                val newTicker = it.trim().uppercase()
                                ticker = newTicker
                                if (newTicker.length >= 5 && newTicker.last().isDigit()) {
                                    type = if (B3NetworkService.inferIsFii(newTicker)) "FII" else "ACAO"
                                }
                            },
                            placeholder = { Text("Ex: PETR4, MXRF11", color = TextSecondary, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFF444444),
                                focusedBorderColor = GoldPrimary,
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Live Metadata Preview Card (Satisfies accurate Proxy requirements)
                    if (matchedAsset != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            color = GoldPrimary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(getTickerBrandColor(matchedAsset.ticker)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = matchedAsset.ticker.take(2).uppercase(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = matchedAsset.name.ifBlank { "Ativo Validado pela B3" },
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Preço Real: R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", matchedAsset.price)}",
                                            color = TextSecondary,
                                            fontSize = 9.sp
                                        )
                                        if (matchedAsset.changePercent != 0.0) {
                                            val isUp = matchedAsset.changePercent > 0.0
                                            Text(
                                                text = "${if (isUp) "+" else ""}${String.format("%.2f", matchedAsset.changePercent)}%",
                                                color = if (isUp) SuccessGreen else DangerRed,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Row: Data de Compra | Quantidade
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                if (isSell) "DATA DE VENDA" else "DATA DE COMPRA",
                                color = GoldPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = { dateStr = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Outlined.Edit, "Selecionar Data", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFF444444),
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text(
                                "QUANTIDADE",
                                color = GoldPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { quantity = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFF444444),
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                    
                    // Row: Preço | Outros custos
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "PREÇO UNITÁRIO R$",
                                color = GoldPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = price,
                                onValueChange = { raw -> 
                                    price = raw
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFF444444),
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "DEPESAS / TAXAS",
                                color = GoldPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = otherCosts,
                                onValueChange = { raw -> 
                                    otherCosts = raw
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFF444444),
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                    
                    // Valor total box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        color = Color(0xFF141414),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF1F1F1F))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Valor total líquido",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "R$ ${String.format(java.util.Locale("pt", "BR"), "%,.2f", valorTotal)}",
                                color = GoldPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    errorMsg?.let { message ->
                        Text(
                            text = message,
                            color = DangerRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Confirm Button
                    Button(
                        onClick = {
                            val sanitizedTicker = ticker.trim().uppercase()
                            val unitPriceVal = if (qVal > 0) (qVal * pVal + cVal) / qVal else pVal
                            
                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).apply {
                                isLenient = false
                            }
                            val dateVal = try {
                                sdf.parse(dateStr.trim())?.time
                            } catch (e: Exception) {
                                null
                            }

                            if (sanitizedTicker.isEmpty() || sanitizedTicker.length < 4) {
                                errorMsg = "Ativo inválido (ex: PETR4, MXRF11)"
                            } else if (dateVal == null) {
                                errorMsg = "Data inválida (formato DD/MM/AAAA)"
                            } else if (qVal <= 0.0) {
                                errorMsg = "Insira uma quantidade maior que zero"
                            } else if (pVal <= 0.0 && cVal <= 0.0) {
                                errorMsg = "Preço unitário ou despesas inválidos"
                            } else {
                                onConfirm(
                                    sanitizedTicker,
                                    qVal,
                                    unitPriceVal,
                                    type,
                                    transactionToEdit?.broker ?: "",
                                    transactionToEdit?.sector ?: "",
                                    dateVal,
                                    transactionToEdit?.notes ?: "",
                                    isSell
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (transactionToEdit != null) "Salvar Alterações" else "Confirmar Transação",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = try {
                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).apply { isLenient = false }.parse(dateStr)?.time
                    } catch (e: Exception) {
                        java.util.Date().time
                    }
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
                            }
                            showDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancelar")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}

@Composable
fun DoubleChevronIcon(isUp: Boolean, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = 2.dp.toPx()
        
        val spacing = height * 0.22f 
        val yOffset = if (isUp) height * 0.12f else -height * 0.12f
        
        val path1 = androidx.compose.ui.graphics.Path()
        val path2 = androidx.compose.ui.graphics.Path()
        
        if (isUp) {
            path1.moveTo(0f, height * 0.38f + yOffset)
            path1.lineTo(width / 2f, height * 0.12f + yOffset)
            path1.lineTo(width, height * 0.38f + yOffset)
            
            path2.moveTo(0f, height * 0.38f + yOffset + spacing)
            path2.lineTo(width / 2f, height * 0.12f + yOffset + spacing)
            path2.lineTo(width, height * 0.38f + yOffset + spacing)
        } else {
            path1.moveTo(0f, height * 0.12f + yOffset)
            path1.lineTo(width / 2f, height * 0.38f + yOffset)
            path1.lineTo(width, height * 0.12f + yOffset)
            
            path2.moveTo(0f, height * 0.12f + yOffset + spacing)
            path2.lineTo(width / 2f, height * 0.38f + yOffset + spacing)
            path2.lineTo(width, height * 0.12f + yOffset + spacing)
        }
        
        drawPath(
            path = path1,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        drawPath(
            path = path2,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

fun getTickerBrandColor(ticker: String): Color {
    val prefix = ticker.trim().uppercase()
    return when {
        prefix.startsWith("AMAR") -> Color(0xFFE02B89)
        prefix.startsWith("CGAS") -> Color(0xFF1E3A8A)
        prefix.startsWith("ONCO") -> Color(0xFF00B4D8)
        prefix.startsWith("SIMH") -> Color(0xFFE63946)
        prefix.startsWith("PCAR") -> Color(0xFF005DA5)
        prefix.startsWith("AURE") -> Color(0xFFE02B89)
        prefix.startsWith("CSMG") -> Color(0xFF3B82F6)
        prefix.startsWith("CSED") -> Color(0xFF1E3A8A)
        prefix.startsWith("VVEO") -> Color(0xFF10B981)
        prefix.startsWith("ARML") -> Color(0xFF1E5BB8)
        prefix.startsWith("EQTL") -> Color(0xFF0C1D7F)
        prefix.startsWith("VIVA") -> Color(0xFFF3965D)
        else -> {
            val colors = listOf(
                Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899),
                Color(0xFFF43F5E), Color(0xFF10B981), Color(0xFF3B82F6),
                Color(0xFFF59E0B), Color(0xFF06B6D4)
            )
            val hash = kotlin.math.abs(prefix.hashCode())
            colors[hash % colors.size]
        }
    }
}

@Composable
fun MarketMoversSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = alpha))
                    )
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = alpha * 0.3f))
                    )
                }
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = alpha * 0.2f))
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { i ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = alpha * 0.15f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = alpha * 0.25f))
                            )
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = alpha * 0.15f))
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = alpha * 0.2f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(45.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = alpha * 0.2f))
                        )
                    }
                    if ( i < 3) {
                        HorizontalDivider(
                            color = Color(0xFF1F1F1F),
                            thickness = 0.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketMoversErrorCard(onRetry: () -> Unit) {
    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = GoldPrimary.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )
            
            Text(
                text = "Não foi possível carregar os rankings",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Dificuldade temporária na conexão do serviço. Verifique sua rede.",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tentar Novamente",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun homeMarketMoverAsset(
    assetData: Map<String, B3AssetData>,
    ticker: String
): B3AssetData? {
    val key = ticker.trim().uppercase(java.util.Locale.ROOT)
    if (key.isBlank()) return null
    return assetData[key] ?: assetData.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
}

private fun homeMarketMoverPriceText(
    item: com.example.network.MarketRankingItem,
    asset: B3AssetData?
): String {
    val itemText = item.priceDisplay.ifBlank {
        if (item.price > 0.0 && item.price.isFinite()) {
            String.format(java.util.Locale("pt", "BR"), "R$ %.2f", item.price)
        } else ""
    }
    if (itemText.isNotBlank()) return itemText
    val assetPrice = asset?.price ?: 0.0
    return if (assetPrice > 0.0 && assetPrice.isFinite()) {
        String.format(java.util.Locale("pt", "BR"), "R$ %.2f", assetPrice)
    } else "—"
}

private fun homeMarketMoverChangeMagnitude(
    item: com.example.network.MarketRankingItem,
    asset: B3AssetData?
): Double {
    val candidates = listOf(item.changePercent, item.value, asset?.changePercent ?: 0.0)
    return candidates.firstOrNull { it != 0.0 && it.isFinite() }?.let { kotlin.math.abs(it) } ?: 0.0
}

private fun homeMarketMoverChangeText(
    item: com.example.network.MarketRankingItem,
    asset: B3AssetData?,
    isPositive: Boolean
): String {
    val arrow = if (isPositive) "▲" else "▼"
    val rawDisplay = item.changeDisplay.ifBlank {
        if (item.displayValue.contains("%")) item.displayValue else ""
    }.trim()
    // Não renderize placeholders quebrados vindos do parser/proxy, como "+, %", "-, %" ou apenas "%".
    val hasDigit = rawDisplay.any { it.isDigit() }
    if (hasDigit) {
        val display = rawDisplay
            .replace("▲", "")
            .replace("▼", "")
            .replace("+", "")
            .replace("-", "")
            .replace(Regex("\\s+"), "")
            .trim()
        if (display.any { it.isDigit() }) {
            val normalized = if (display.contains("%")) display else "$display%"
            return "$arrow $normalized"
        }
    }
    val magnitude = homeMarketMoverChangeMagnitude(item, asset)
    return if (magnitude > 0.0) {
        "$arrow ${String.format(java.util.Locale("pt", "BR"), "%.2f%%", magnitude)}"
    } else {
        ""
    }
}

@Composable
fun HomeMarketMoversPreview(
    ranking: com.example.network.MarketRankingSnapshot?,
    assetData: Map<String, B3AssetData>,
    onAssetClick: (String) -> Unit,
    isLoading: Boolean = false,
    rankingsAttempted: Boolean = false,
    onRetry: () -> Unit = {}
) {
    val highs = remember(ranking) { ranking?.highs.orEmpty().filter { it.ticker.isNotBlank() }.take(6) }
    val lows = remember(ranking) { ranking?.lows.orEmpty().filter { it.ticker.isNotBlank() }.take(6) }

    if (ranking == null || (highs.isEmpty() && lows.isEmpty())) {
        if (isLoading || (!rankingsAttempted && ranking == null)) {
            MarketMoversSkeleton()
        } else {
            MarketMoversErrorCard(onRetry = onRetry)
        }
        return
    }

    var activePage by remember(highs.size, lows.size) { mutableStateOf(if (highs.isNotEmpty()) 0 else 1) }
    LaunchedEffect(highs.size, lows.size) {
        if (activePage == 0 && highs.isEmpty() && lows.isNotEmpty()) activePage = 1
        if (activePage == 1 && lows.isEmpty() && highs.isNotEmpty()) activePage = 0
    }

    val currentItems = if (activePage == 0) highs else lows
    val isPositive = activePage == 0
    val title = if (isPositive) "Maiores Altas" else "Maiores Baixas"
    val accentColor = if (isPositive) SuccessGreen else DangerRed
    val sourceLabel = ranking.source.ifBlank { "Serviço de dados VALORAE" }

    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("home_market_movers_card")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Authoritative Title & Info Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accentColor.copy(alpha = 0.1f), CircleShape)
                            .border(1.dp, accentColor.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Destaques do Mercado",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = sourceLabel.take(48),
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Tech/Data badge
                Surface(
                    color = Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = "B3",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Segmented Switcher Pill Bar (Pill design)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ALTAS Tab
                val altasSelected = activePage == 0
                val altasBgColor by animateColorAsState(
                    targetValue = if (altasSelected) SuccessGreen.copy(alpha = 0.16f) else Color.Transparent,
                    label = "altasBg"
                )
                val altasBorderColor by animateColorAsState(
                    targetValue = if (altasSelected) SuccessGreen.copy(alpha = 0.35f) else Color.Transparent,
                    label = "altasBorder"
                )
                val altasTxtColor by animateColorAsState(
                    targetValue = if (altasSelected) SuccessGreen else TextSecondary,
                    label = "altasTxt"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(altasBgColor)
                        .border(1.dp, altasBorderColor, RoundedCornerShape(8.dp))
                        .clickable(enabled = highs.isNotEmpty()) { activePage = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = altasTxtColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Maiores Altas",
                            color = altasTxtColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(3.dp))

                // BAIXAS Tab
                val baixasSelected = activePage == 1
                val baixasBgColor by animateColorAsState(
                    targetValue = if (baixasSelected) DangerRed.copy(alpha = 0.16f) else Color.Transparent,
                    label = "baixasBg"
                )
                val baixasBorderColor by animateColorAsState(
                    targetValue = if (baixasSelected) DangerRed.copy(alpha = 0.35f) else Color.Transparent,
                    label = "baixasBorder"
                )
                val baixasTxtColor by animateColorAsState(
                    targetValue = if (baixasSelected) DangerRed else TextSecondary,
                    label = "baixasTxt"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(baixasBgColor)
                        .border(1.dp, baixasBorderColor, RoundedCornerShape(8.dp))
                        .clickable(enabled = lows.isNotEmpty()) { activePage = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = baixasTxtColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Maiores Baixas",
                            color = baixasTxtColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (currentItems.isEmpty()) {
                Text(
                    text = if (isPositive) "Sem maiores altas disponíveis agora." else "Sem maiores baixas disponíveis agora.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    val halfSize = (currentItems.size + 1) / 2
                    val firstCol = currentItems.take(halfSize)
                    val secondCol = currentItems.drop(halfSize)

                    // First column (left)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        firstCol.forEachIndexed { idx, item ->
                            MarketMoverCompactRow(
                                item = item,
                                indexOffset = 0,
                                index = idx,
                                isPositive = isPositive,
                                accentColor = accentColor,
                                assetData = assetData,
                                onAssetClick = onAssetClick
                            )
                        }
                    }

                    // Divider line in between
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .align(Alignment.CenterVertically)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    // Second column (right)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        secondCol.forEachIndexed { idx, item ->
                            MarketMoverCompactRow(
                                item = item,
                                indexOffset = halfSize,
                                index = idx,
                                isPositive = isPositive,
                                accentColor = accentColor,
                                assetData = assetData,
                                onAssetClick = onAssetClick
                            )
                        }
                    }
                }
            }

            if (ranking.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 0.5.dp)
                Text(
                    text = ranking.warnings.first().take(96),
                    color = TextSecondary,
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun AsyncCompanyLogo(
    ticker: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    // Otimização definitiva de abertura: monograma local instantâneo, sem qualquer
    // requisição externa durante composição da Dashboard/carteira. Isso evita jank,
    // recomposições caras e dependência de Clearbit/domínios fora do Proxy.
    val monogram = remember(ticker) { ticker.trim().take(2).uppercase() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(getTickerBrandColor(ticker)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = monogram,
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
private fun MarketMoverCompactRow(
    item: com.example.network.MarketRankingItem,
    indexOffset: Int,
    index: Int,
    isPositive: Boolean,
    accentColor: Color,
    assetData: Map<String, B3AssetData>,
    onAssetClick: (String) -> Unit
) {
    val asset = homeMarketMoverAsset(assetData, item.ticker)
    val displayRank = if (item.rank > 0) item.rank else indexOffset + index + 1
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onAssetClick(item.ticker) }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Indicator
        Text(
            text = "$displayRank",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(12.dp)
        )

        // Asynchronous Corporate Logo
        AsyncCompanyLogo(
            ticker = item.ticker,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Ticker next to logo
        Text(
            text = item.ticker,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Price and percentage next to each other
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = homeMarketMoverPriceText(item, asset).replace("R$ ", ""),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.End
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            val changeLine = homeMarketMoverChangeText(item, asset, isPositive)
            Text(
                text = if (changeLine.isNotBlank()) changeLine else "+0.00%",
                color = accentColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.End
            )
        }
    }
}

private fun parseInsightDateMillis(value: String): Long {
    if (value.isBlank()) return 0L
    val raw = value.trim()
    raw.toLongOrNull()?.let { return if (it > 10_000_000_000L) it else it * 1000L }
    val normalized = raw
        .replace(Regex("\\s+"), " ")
        .replace("às", " ", ignoreCase = true)
        .trim()
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd HH:mm:ss",
        "dd/MM/yyyy HH:mm:ss",
        "dd/MM/yyyy HH:mm",
        "dd/MM/yyyy",
        "dd/MM/yy",
        "yyyy-MM-dd"
    )
    for (p in patterns) {
        try {
            val sdf = java.text.SimpleDateFormat(p, java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            return sdf.parse(normalized)?.time ?: 0L
        } catch (e: Exception) {}
    }
    return 0L
}

private fun eventRelevantMillis(event: com.example.network.DividendEvent): Long {
    return parseInsightDateMillis(event.paymentDate).takeIf { it > 0L }
        ?: parseInsightDateMillis(event.dateCom).takeIf { it > 0L }
        ?: 0L
}

private fun eventEligibilityMillis(event: com.example.network.DividendEvent): Long {
    return parseInsightDateMillis(event.dateCom).takeIf { it > 0L }
        ?: parseInsightDateMillis(event.paymentDate).takeIf { it > 0L }
        ?: 0L
}

private fun startOfInsightDayMillis(millis: Long): Long {
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = millis
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun endOfInsightDayMillis(millis: Long): Long {
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = millis
        set(java.util.Calendar.HOUR_OF_DAY, 23)
        set(java.util.Calendar.MINUTE, 59)
        set(java.util.Calendar.SECOND, 59)
        set(java.util.Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
}

private fun isPaidDividendEvent(event: com.example.network.DividendEvent, nowMillis: Long = System.currentTimeMillis()): Boolean {
    val status = event.status.lowercase(java.util.Locale.ROOT)
    val ts = eventRelevantMillis(event)
    val todayStart = startOfInsightDayMillis(nowMillis)
    return status.contains("pago") || status.contains("recebido") || status.contains("último") || status.contains("ultimo") || (ts > 0L && ts < todayStart)
}

private fun safeDividendAmount(event: com.example.network.DividendEvent): Double {
    return event.estimatedAmount.coerceAtLeast(0.0)
}

private fun sharesOwnedAtInsightDate(
    transactions: List<com.example.data.Transaction>,
    ticker: String,
    millis: Long
): Double {
    if (transactions.isEmpty()) return 0.0
    val key = ticker.trim().uppercase(java.util.Locale.ROOT)
    return transactions.asSequence()
        .filter { it.ticker.trim().uppercase(java.util.Locale.ROOT) == key && it.date <= millis }
        .fold(0.0) { acc, tx -> if (tx.isSell) acc - tx.quantity else acc + tx.quantity }
        .coerceAtLeast(0.0)
}

private fun eligibleDividendAmount(
    event: com.example.network.DividendEvent,
    transactions: List<com.example.data.Transaction>
): Double {
    if (transactions.isEmpty()) return safeDividendAmount(event)

    val eligibilityTs = eventEligibilityMillis(event)
    val relevantTs = eventRelevantMillis(event)
    val todayStart = startOfInsightDayMillis(System.currentTimeMillis())
    val isPastPayment = relevantTs > 0L && relevantTs < todayStart
    val fallbackTs = if (eligibilityTs > 0L) eligibilityTs else if (relevantTs > 0L) relevantTs else System.currentTimeMillis()

    val shares = sharesOwnedAtInsightDate(transactions, event.ticker, endOfInsightDayMillis(fallbackTs))

    val finalShares = when {
        shares > 0.0001 -> shares
        !isPastPayment && event.quantity > 0.0 -> event.quantity
        !isPastPayment -> 0.0
        else -> 0.0
    }

    if (finalShares <= 0.0001) return 0.0

    if (event.valuePerShare > 0.0) return event.valuePerShare * finalShares
    if (event.estimatedAmount > 0.0 && event.quantity > 0.0) return event.estimatedAmount * (finalShares / event.quantity)

    return event.estimatedAmount.coerceAtLeast(0.0)
}
