package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.ui.components.SegmentedAllocationBar
import com.example.ui.theme.*
import com.example.viewmodel.AssetSummary
import com.example.viewmodel.PortfolioSummary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    summary: PortfolioSummary,
    assets: List<AssetSummary>,
    transactions: List<Transaction>,
    onAddTransaction: (String, Double, Double, String) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onAssetClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Ativos, 1: Transações

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Portfolio Value Summary Widget
            item {
                PortfolioHeaderCard(summary)
            }

            // 2. Diversification Ratio Widget
            if (summary.totalInvested > 0.0) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "COMPOSIÇÃO",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        SegmentedAllocationBar(
                            stockValue = summary.totalStocksCurrent,
                            fiiValue = summary.totalFiisCurrent
                        )
                    }
                }
            }

            // Tabs Selector: Holdings vs raw transactional logs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
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
                // Holdings Portfolio List
                if (assets.isEmpty()) {
                    item {
                        EmptyStateWidget(
                            title = "Carteira Vazia",
                            desc = "Adicione fundos imobiliários e ações clicando no botão flutuante abaixo para iniciar seu acompanhamento de proventos!"
                        )
                    }
                } else {
                    items(assets) { asset ->
                        HoldingsListItem(
                            asset = asset,
                            onClick = { onAssetClick(asset.ticker) }
                        )
                    }
                }
            } else {
                // Transactions History List
                if (transactions.isEmpty()) {
                    item {
                        EmptyStateWidget(
                            title = "Nenhuma transação cadastrada",
                            desc = "Seu histórico de compras e vendas ficará registrado aqui. Adicione uma transação utilizando o botão (+)."
                        )
                    }
                } else {
                    items(transactions) { tx ->
                        TransactionHistoryItem(
                            tx = tx,
                            onDelete = { onDeleteTransaction(tx) }
                        )
                    }
                }
            }
            
            // Add spacing so bottom content is not clipped by Fab / navigation bars
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = GoldPrimary,
            contentColor = DarkBackground,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar Ativo")
        }

        // Add Transaction Dialog Modal
        if (showAddDialog) {
            AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { ticker, quantity, price, type ->
                    onAddTransaction(ticker, quantity, price, type)
                    showAddDialog = false
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
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (selected) GoldPrimary else TextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        if (selected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.width(32.dp).height(2.dp).background(GoldPrimary))
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun PortfolioHeaderCard(summary: PortfolioSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column {
            Text(
                text = "Patrimônio Consolidado",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "R$ ${String.format("%,.2f", summary.totalCurrentValue)}",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            HorizontalDivider(color = BorderColor, thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Investido",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "R$ ${String.format("%,.2f", summary.totalInvested)}",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Retorno Total",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    val isPositive = summary.totalReturn >= 0.0
                    val returnColor = if (isPositive) SuccessGreen else DangerRed
                    val trendIcon = if (isPositive) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = trendIcon,
                            contentDescription = null,
                            tint = returnColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isPositive) "+" else ""}R$ ${String.format("%,.2f", summary.totalReturn)} (${String.format("%.2f", summary.returnPercent)}%)",
                            color = returnColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HoldingsListItem(
    asset: AssetSummary,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Asset Ticker Badge Indicator
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        color = if (asset.type == "ACAO") GoldPrimary.copy(alpha = 0.12f) else SuccessGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = asset.ticker.take(4),
                        color = if (asset.type == "ACAO") GoldPrimary else SuccessGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Text(
                        text = asset.ticker.drop(4),
                        color = if (asset.type == "ACAO") GoldPrimary else SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Shares and Average cost columns
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = asset.ticker,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                val formattedShares = if (asset.sharesCount % 1.0 == 0.0) {
                    asset.sharesCount.toInt().toString()
                } else {
                    String.format("%.2f", asset.sharesCount)
                }
                Text(
                    text = "$formattedShares cotas • PM: R$ ${String.format("%.2f", asset.averageCost)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Return absolute vs live prices columns
            Column(
                modifier = Modifier.weight(1.5f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "R$ ${String.format("%,.2f", asset.totalCurrentValue)}",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                
                val isPositive = asset.totalReturn >= 0.0
                val accentColor = if (isPositive) SuccessGreen else DangerRed
                
                Text(
                    text = "${if (isPositive) "+" else ""}R$ ${String.format("%.2f", asset.totalReturn)} (${String.format("%.2f", asset.returnPercent)}%)",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TransactionHistoryItem(
    tx: Transaction,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (tx.type == "ACAO") GoldPrimary.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (tx.type == "ACAO") "Ação" else "FII",
                            color = if (tx.type == "ACAO") GoldPrimary else SuccessGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tx.ticker,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val formattedQty = if (tx.quantity % 1.0 == 0.0) {
                    tx.quantity.toInt().toString()
                } else {
                    String.format("%.2f", tx.quantity)
                }
                Text(
                    text = "Comprou $formattedQty x R$ ${String.format("%.2f", tx.purchasePrice)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Total Investido",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "R$ ${String.format("%.2f", tx.quantity * tx.purchasePrice)}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Deletar Transação",
                    tint = DangerRed.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
fun EmptyStateWidget(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = GoldPrimary.copy(alpha = 0.5f),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = desc,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String) -> Unit
) {
    var ticker by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("ACAO") } // "ACAO" or "FII"
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Adicionar Ativo", color = TextPrimary, fontWeight = FontWeight.Black)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Ticker Input
                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it.trim().uppercase() },
                    label = { Text("Ticker (Ex: PETR4, MXRF11)") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurface
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Asset Type toggle selection
                Column {
                    Text("Tipo de Ativo", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground, shape = RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { type = "ACAO" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "ACAO") GoldPrimary else Color.Transparent,
                                contentColor = if (type == "ACAO") DarkBackground else TextSecondary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ações", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { type = "FII" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "FII") SuccessGreen else Color.Transparent,
                                contentColor = if (type == "FII") DarkBackground else TextSecondary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fundo Imobiliário", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantidade de Cotas") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Price Input
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Preço de Custo (R$)") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sanitizedTicker = ticker.trim().uppercase()
                    val qtyVal = quantity.replace(",", ".").toDoubleOrNull()
                    val prcVal = price.replace(",", ".").toDoubleOrNull()
                    if (sanitizedTicker.isEmpty() || sanitizedTicker.length < 5) {
                        errorMsg = "Ticker inválido. Use um formato como MXRF11 ou VALE3."
                    } else if (qtyVal == null || qtyVal <= 0.0) {
                        errorMsg = "Insira uma quantidade de cotas válida."
                    } else if (prcVal == null || prcVal <= 0.0) {
                        errorMsg = "Insira um preço de custo válido."
                    } else {
                        onConfirm(sanitizedTicker, qtyVal, prcVal, type)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkBackground)
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("Cancelar")
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
