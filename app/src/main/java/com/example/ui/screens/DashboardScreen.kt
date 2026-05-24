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
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    summary: PortfolioSummary,
    assets: List<AssetSummary>,
    transactions: List<Transaction>,
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
    onUpdateAvailable: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Ativos, 1: Transações
    var selectedAssetForDetail by remember { mutableStateOf<AssetSummary?>(null) }
    
    val acoes = remember(assets) { assets.filter { it.type == "ACAO" } }
    val fiis = remember(assets) { assets.filter { it.type != "ACAO" } }
    
    val transactionsByMonth = remember(transactions) {
        val cal = java.util.Calendar.getInstance()
        transactions.sortedByDescending { it.date }.groupBy {
            cal.timeInMillis = it.date
            "${cal.get(java.util.Calendar.MONTH) + 1}/${cal.get(java.util.Calendar.YEAR)}"
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Screen Header
            item {
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
            item {
                PortfolioHeaderCard(summary, hideValues, onClick = onPortfolioClick)
            }

            // 2. Diversification Ratio Widget
            if (summary.totalInvested > 0.0) {
                item {
                    val stockAssetsCount = assets.count { it.type == "ACAO" || it.type.uppercase() == "ACAO" }
                    val fiiAssetsCount = assets.count { it.type == "FII" || it.type.uppercase() == "FII" }
                    
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 600))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "COMPOSIÇÃO DA CARTEIRA",
                                    color = GoldPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                
                            Text(
                                text = "${stockAssetsCount + fiiAssetsCount} ativos",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        SegmentedAllocationBar(
                            stockValue = if (summary.totalCurrentValue > 0) summary.totalStocksCurrent else summary.totalStocksInvested,
                            fiiValue = if (summary.totalCurrentValue > 0) summary.totalFiisCurrent else summary.totalFiisInvested
                        )
                    }
                }
            }
        }

        // Tabs Selector: Holdings vs raw transactional logs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                    TabButton(
                        text = "Meus Ativos (${assets.size})",
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "Histórico de Compras",
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (activeTab == 0) {
                if (assets.isEmpty()) {
                    item {
                        EmptyStateWidget(
                            title = "Carteira Vazia",
                            desc = "Adicione fundos imobiliários e ações clicando no botão flutuante abaixo para iniciar seu acompanhamento de proventos!"
                        )
                    }
                } else {
                    if (acoes.isNotEmpty()) {
                        item { 
                            Text("Ações", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) 
                        }
                        itemsIndexed(items = acoes, key = { _, asset -> asset.ticker }) { index, asset ->
                            HoldingsListItem(
                                asset = asset,
                                hideValues = hideValues,
                                onClick = {
                                    selectedAssetForDetail = asset
                                }
                            )
                            if (index < acoes.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                    if (fiis.isNotEmpty()) {
                        item { 
                            Text("FIIs", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)) 
                        }
                        itemsIndexed(items = fiis, key = { _, asset -> asset.ticker }) { index, asset ->
                            HoldingsListItem(
                                asset = asset,
                                hideValues = hideValues,
                                onClick = {
                                    selectedAssetForDetail = asset
                                }
                            )
                            if (index < fiis.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                if (transactions.isEmpty()) {
                    item {
                        EmptyStateWidget(
                            title = "Nenhuma transação cadastrada",
                            desc = "Seu histórico de compras e vendas ficará registrado aqui. Adicione uma transação utilizando o botão (+)."
                        )
                    }
                } else {
                    transactionsByMonth.forEach { (monthYear, monthTxs) ->
                        item(key = "header_$monthYear") {
                            val totalMonth = monthTxs.sumOf { it.quantity * it.purchasePrice }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(monthYear, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Total: R$ ${String.format("%.2f", totalMonth)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        itemsIndexed(items = monthTxs, key = { _, tx -> tx.id }) { index, tx ->
                            TransactionHistoryItem(
                                tx = tx,
                                onEdit = { editingTransaction = tx },
                                onDelete = { transactionToDelete = tx }
                            )
                            if (index < monthTxs.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                        
                        item(key = "spacer_$monthYear") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            
            // Add spacing so bottom content is not clipped by Fab / navigation bars
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

    // Floating Action Button with entry animation
    var fabVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        fabVisible = true
    }

    // Fixed alignment for Floating Action Button
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp, end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = fabVisible,
            enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 600))
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GoldPrimary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_asset_fab")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Adicionar Ativo",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

        // Add Transaction Dialog Modal
        if (showAddDialog || editingTransaction != null) {
            AddTransactionDialog(
                transactionToEdit = editingTransaction,
                onDismiss = {
                    showAddDialog = false
                    editingTransaction = null
                },
                onConfirm = { ticker, quantity, price, type, broker, sector, purchaseDate, notes, isSell ->
                    val tx = editingTransaction
                    if (tx != null) {
                        onUpdateTransaction(tx.id, ticker, quantity, price, type, broker, sector, purchaseDate, notes, isSell)
                        editingTransaction = null
                    } else {
                        onAddTransaction(ticker, quantity, price, type, broker, sector, purchaseDate, notes, isSell)
                        showAddDialog = false
                    }
                }
            )
        }

        // Asset Detail Modal
        if (selectedAssetForDetail != null) {
            val asset = selectedAssetForDetail!!
            val filteredTxs = remember(transactions, asset.ticker) {
                transactions.filter { it.ticker.trim().uppercase() == asset.ticker.trim().uppercase() }
            }
            
            // Check if user still holds this asset
            val currentAsset = assets.find { it.ticker.trim().uppercase() == asset.ticker.trim().uppercase() }
            if (currentAsset == null) {
                // Asset was deleted or has 0 shares
                selectedAssetForDetail = null
            } else {
                AssetDetailModal(
                    asset = currentAsset,
                    chartPoints = chartHistory,
                    chartRange = chartRange,
                    onRangeChange = onRangeChange,
                    transactions = filteredTxs,
                    onDeleteTransaction = { onDeleteTransaction(it) },
                    onEditTransaction = { editingTransaction = it },
                    isSearching = isSearchingChart,
                    onDismiss = {
                        selectedAssetForDetail = null
                    }
                )
            }
        }
        
        // Delete Confirmation Dialog
        if (transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text("Excluir Transação") },
                text = { Text("Tem certeza que deseja excluir esta transação? Esta ação não pode ser desfeita.") },
                confirmButton = {
                    TextButton(onClick = {
                        transactionToDelete?.let { onDeleteTransaction(it) }
                        transactionToDelete = null
                    }) {
                        Text("Excluir", color = DangerRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { transactionToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
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
fun PortfolioHeaderCard(summary: PortfolioSummary, hideValues: Boolean = false, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(GoldPrimary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Patrimônio Consolidado",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Toque para ver detalhes",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Pulsing indicator for "clickable" affordance
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(GoldPrimary.copy(alpha = 0.4f), CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = if (hideValues) "R$ •••••" else "R$ ${String.format("%,.2f", summary.totalCurrentValue)}",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Contrast Highlight for Total Invested
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL INVESTIDO: ",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (hideValues) "R$ •••••" else "R$ ${String.format("%,.2f", summary.totalInvested)}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Return Stats
                Column(horizontalAlignment = Alignment.Start) {
                    Text("RENTABILIDADE", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    val retPct = summary.returnPercent
                    val textClr = if (summary.totalInvested <= 0.0) TextSecondary else if (retPct >= 0) SuccessGreen else DangerRed
                    Text(if (hideValues) "••%" else String.format("%+.2f%%", retPct), color = textClr, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }

                // Divider (Visual only)
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderColor.copy(alpha = 0.1f)))

                // Return Real
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RETORNO TOTAL", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    val retTotal = summary.totalReturn
                    val textClr = if (summary.totalInvested <= 0.0) TextSecondary else if (retTotal >= 0) SuccessGreen else DangerRed
                    Text(if (hideValues) "R$ •••" else "R$ ${String.format("%,.2f", retTotal)}", color = textClr, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }

                // Divider (Visual only)
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderColor.copy(alpha = 0.1f)))

                // Divisão
                Column(horizontalAlignment = Alignment.End) {
                    Text("DIVISÃO", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(GoldPrimary, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${(summary.sharesRatioStock * 100).toInt()}%", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(6.dp).background(SuccessGreen, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${(summary.sharesRatioFii * 100).toInt()}%", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.5.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Atualizado em tempo real", 
                    color = TextSecondary, 
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Análise da Carteira ➔", 
                    color = GoldPrimary, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Black
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
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("asset_item_${asset.ticker}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Asset Ticker Badge Indicator
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(1.dp, BorderColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = asset.ticker.take(4),
                color = GoldPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Shares and Average cost columns
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = asset.ticker,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (asset.type == "FII") {
                    Text(
                        text = "FII",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
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

        // Return absolute vs live prices columns
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Text(
                text = if (hideValues) "R$ ••" else "R$ ${String.format("%,.2f", asset.totalCurrentValue)}",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            
            val isPositive = asset.totalReturn >= 0.0
            val accentColor = if (isPositive) SuccessGreen else DangerRed
            
            Text(
                text = if (hideValues) "••%"  else "${if (isPositive) "+" else ""}R$ ${String.format("%.2f", asset.totalReturn)}",
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
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
                String.format(java.util.Locale.US, "%.2f", it)
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
    val pVal = price.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
    val cVal = otherCosts.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
    val valorTotal = (qVal * pVal) + cVal

    var expandedType by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background // Solid background for full-screen like behavior or use white-ish
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (transactionToEdit != null) "Editar Transação" else "Nova Transação",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
                
                // Compra/Venda Segmented Control (Investidor10 style)
                Surface(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.08f))
                ) {
                    Row(modifier = Modifier.padding(6.dp)) {
                        // Compra
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isSell = false },
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isSell) SuccessGreen else Color.Transparent,
                            tonalElevation = if (!isSell) 4.dp else 0.dp
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                                    contentDescription = null,
                                    tint = if (!isSell) Color.White else SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Compra",
                                    color = if (!isSell) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = if (!isSell) FontWeight.Black else FontWeight.Medium
                                )
                            }
                        }
                        
                        // Venda
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isSell = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSell) DangerRed else Color.Transparent,
                            tonalElevation = if (isSell) 4.dp else 0.dp
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isSell) Color.White else DangerRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Venda",
                                    color = if (isSell) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSell) FontWeight.Black else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tipo de Ativo
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text("Tipo de ativo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedType,
                        onExpandedChange = { expandedType = it }
                    ) {
                        OutlinedTextField(
                            value = if (type == "ACAO") "Ações" else "Fundos Imobiliários",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedType,
                            onDismissRequest = { expandedType = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ações") },
                                onClick = { type = "ACAO"; expandedType = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Fundos Imobiliários") },
                                onClick = { type = "FII"; expandedType = false }
                            )
                        }
                    }
                }
                
                // Ativo
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("Ativo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    OutlinedTextField(
                        value = ticker,
                        onValueChange = { 
                            val newTicker = it.trim().uppercase()
                            ticker = newTicker
                            if (newTicker.length >= 5) {
                                if (newTicker.endsWith("11") && !newTicker.startsWith("BOVA") && !newTicker.startsWith("SMAL")) {
                                    type = "FII"
                                } else if (newTicker.last().isDigit()) {
                                    type = "ACAO"
                                }
                            }
                        },
                        placeholder = { Text("Ex: PETR4, MXRF11") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Row: Data de Compra | Quantidade
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isSell) "Data de Venda" else "Data de Compra", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                        OutlinedTextField(
                            value = dateStr,
                            onValueChange = { dateStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Outlined.Edit, "Selecionar Data")
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Quantidade", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                
                // Row: Preço | Outros custos
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Preço em R$", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                        OutlinedTextField(
                            value = price,
                            onValueChange = { raw -> 
                                val cleanString = raw.filter { it.isDigit() }
                                if (cleanString.isNotEmpty()) {
                                    val parsed = cleanString.toDouble() / 100
                                    price = String.format(java.util.Locale("pt", "BR"), "%,.2f", parsed)
                                } else {
                                    price = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Outros custos", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                        OutlinedTextField(
                            value = otherCosts,
                            onValueChange = { raw -> 
                                val cleanString = raw.filter { it.isDigit() }
                                if (cleanString.isNotEmpty()) {
                                    val parsed = cleanString.toDouble() / 100
                                    otherCosts = String.format(java.util.Locale("pt", "BR"), "%,.2f", parsed)
                                } else {
                                    otherCosts = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                
                // Valor total box
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Valor total", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("R$ ${String.format(java.util.Locale.Builder().setLanguage("pt").setRegion("BR").build(), "%,.2f", valorTotal)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = DangerRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Add Button
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
                            errorMsg = "Ticker inválido (ex: PETR4, MXRF11)"
                        } else if (dateVal == null) {
                            errorMsg = "Data inválida (formato DD/MM/AAAA)"
                        } else if (qVal <= 0.0) {
                            errorMsg = "Quantidade inválida (deve ser > 0)"
                        } else if (pVal <= 0.0 && cVal <= 0.0) {
                            errorMsg = "Preço unitário inválido (deve ser > 0)"
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
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF232323), // Dark color from design
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (transactionToEdit != null) "Salvar alterações" else "Adicionar ativo",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
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
