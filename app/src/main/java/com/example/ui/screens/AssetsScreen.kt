package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.ui.components.AssetDetailModal
import com.example.ui.theme.*
import com.example.viewmodel.AssetSummary
import com.example.network.B3AssetData
import com.example.network.AssetChartBundle

@Composable
fun AssetsScreen(
    assets: List<AssetSummary>,
    transactions: List<Transaction>,
    cachedAssetData: Map<String, B3AssetData> = emptyMap(),
    assetChartBundles: Map<String, AssetChartBundle> = emptyMap(),
    isLoadingChartBundle: Boolean = false,
    onLoadAssetChartBundle: (String, String) -> Unit = { _, _ -> },
    chartRange: String = "1y",
    onRangeChange: (String) -> Unit = {},
    isSearchingChart: Boolean = false,
    hideValues: Boolean = false,
    onAddTransaction: (String, Double, Double, String, String, String, Long?, String, Boolean) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onUpdateTransaction: (Int, String, Double, Double, String, String, String, Long?, String, Boolean) -> Unit = {_,_,_,_,_,_,_,_,_,_ ->},
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
                        text = "Meus Ativos",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Gerencie posições, compras, vendas e histórico operacional.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
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
                        text = "Ativos (${assets.size})",
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "Histórico",
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
                                    val tickerKey = asset.ticker.trim().uppercase()
                                    onLoadAssetChartBundle(tickerKey, chartRange.ifBlank { "1Y" })
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
                                    val tickerKey = asset.ticker.trim().uppercase()
                                    onLoadAssetChartBundle(tickerKey, chartRange.ifBlank { "1Y" })
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
                            val totalMonth = monthTxs.sumOf { tx -> (if (tx.isSell) -1 else 1) * tx.quantity * tx.purchasePrice }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(monthYear, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Mov.: ${if (totalMonth < 0) "-" else ""}R$ ${String.format(java.util.Locale("pt", "BR"), "%,.2f", kotlin.math.abs(totalMonth))}",
                                    color = if (totalMonth < 0) DangerRed else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                cachedAssetData = cachedAssetData,
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
        selectedAssetForDetail?.let { selectedAsset ->
            val filteredTxs = remember(transactions, selectedAsset.ticker) {
                transactions.filter { it.ticker.trim().uppercase() == selectedAsset.ticker.trim().uppercase() }
            }
            
            // Check if user still holds this asset
            val currentAsset = assets.find { it.ticker.trim().uppercase() == selectedAsset.ticker.trim().uppercase() }
            if (currentAsset == null) {
                // Asset was deleted or has 0 shares
                selectedAssetForDetail = null
            } else {
                val tickerKey = currentAsset.ticker.trim().uppercase()
                val cachedBundle = assetChartBundles[tickerKey]
                AssetDetailModal(
                    asset = currentAsset,
                    initialAssetData = cachedAssetData[tickerKey],
                    initialChartBundle = cachedBundle,
                    isLoadingInitialChartBundle = isLoadingChartBundle && cachedBundle == null,
                    onLoadChartBundle = { ticker, range -> onLoadAssetChartBundle(ticker, range) },
                    chartPoints = cachedBundle?.priceHistory.orEmpty(),
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
