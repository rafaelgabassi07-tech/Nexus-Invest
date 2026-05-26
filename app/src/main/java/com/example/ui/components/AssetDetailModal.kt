package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.theme.*
import com.example.viewmodel.AssetSummary
import com.example.ui.B3UIUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailModal(
    asset: AssetSummary,
    chartPoints: List<ChartPoint>,
    chartRange: String,
    onRangeChange: (String) -> Unit,
    transactions: List<Transaction>,
    onDeleteTransaction: (Transaction) -> Unit,
    onEditTransaction: (Transaction) -> Unit = {},
    isSearching: Boolean,
    onDismiss: () -> Unit
) {
    val isFii = asset.type == "FII"
    val lineColor = GoldPrimary
    
    // Asynchronously fetch live fundamental data (B3AssetData)
    var assetData by remember { mutableStateOf<B3AssetData?>(null) }
    var isLoadingData by remember { mutableStateOf(true) }
    
    // Local historical chart points to avoid using global state from parent that might be stale
    var localChartPoints by remember { mutableStateOf<List<ChartPoint>>(emptyList()) }
    var localChartRange by remember { mutableStateOf(chartRange) }
    var isFetchingChart by remember { mutableStateOf(false) }
    
    LaunchedEffect(asset.ticker, localChartRange) {
        isLoadingData = true
        isFetchingChart = true
        withContext(Dispatchers.IO) {
            try {
                // Fetch fundamental indicators
                assetData = B3NetworkService.fetchAssetData(asset.ticker)
                // Fetch local historical points for the chart
                localChartPoints = B3NetworkService.fetchHistoricalChart(asset.ticker, localChartRange)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingData = false
                isFetchingChart = false
            }
        }
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

                if (isLoadingData) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
                    }
                } else {
                    val realData = assetData

                    // Main Scrollable body with LazyColumn for high performance
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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

                        // 1.2 Oscilação 52 Semanas (if available)
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

                        // 1.3 Indicadores Fundamentalistas Grid
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
                                            metrics.add(Triple("Vacância", B3UIUtils.formatValue(realData.fiiVacancy, suffix = "%", precision = 1), "Área corporativa vaga"))
                                            metrics.add(Triple("Liquidez Diária", B3UIUtils.formatLargeNumber(realData.dailyLiquidity).replace("R$ ", ""), "Volume financeiro mensal"))
                                            metrics.add(Triple("Segmento", B3UIUtils.formatText(realData.fiiSegment, "Outros"), "Tipo de operação"))
                                            if (realData.magicNumber > 0) {
                                                metrics.add(Triple("Magic Number", "${realData.magicNumber.toInt()} cotas", "Auto-compra com dividendos"))
                                            }
                                        } else {
                                            metrics.add(Triple("LPA", B3UIUtils.formatValue(realData.lpa, prefix = "R$ "), "Lucro líquido por ação"))
                                            metrics.add(Triple("Margem Líquida", B3UIUtils.formatValue(realData.margins, suffix = "%"), "Eficiência líquida"))
                                            metrics.add(Triple("ROE", B3UIUtils.formatValue(realData.roe, suffix = "%"), "Retorno s/ Patr. Líq."))
                                            metrics.add(Triple("ROIC", B3UIUtils.formatValue(realData.roic, suffix = "%"), "Retorno s/ Capital Invest."))
                                        }
                                    } else {
                                        // Fallback to local asset stats
                                        metrics.add(Triple("Dividend Yield", B3UIUtils.formatValue(asset.dividendYield, suffix = "%"), "Últimos 12 meses"))
                                        metrics.add(Triple("P/VP", "--", "Preço / Valor Patrimonial (Est.)"))
                                        metrics.add(Triple("VPA", "--", "Valor Patrimonial por Ação (Est.)"))
                                        metrics.add(Triple("Últ. Provento", B3UIUtils.formatValue(asset.lastDividend, prefix = "R$ "), "Última distribuição paga"))
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

                        // 3. Interactive Chart
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
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("1d", "5d", "1mo", "1y", "5y").forEach { range ->
                                                val isSelected = range == localChartRange
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
                                                            localChartRange = range
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

                        // 4. Detailed Purchase Logs
                        item {
                            Text(
                                text = "HISTÓRICO DE COMPRAS DETALHADO",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 2.dp)
                            )
                        }

                        if (transactions.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Nenhuma transação cadastrada", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            items(sortedTransactions, key = { it.id }) { tx ->
                                val isSale = tx.isSell
                                val itemColor = if (isSale) DangerRed else GoldPrimary
                                val currentPriceToUse = realData?.price ?: asset.currentPrice

                                val txTotalValue = tx.quantity * tx.purchasePrice
                                val isPurchase = !isSale

                                val currentTxValue = if (isPurchase) currentPriceToUse * tx.quantity else 0.0
                                val profitAbs = if (isPurchase) currentTxValue - txTotalValue else 0.0
                                val profitPct = if (isPurchase && txTotalValue > 0) (profitAbs / txTotalValue) * 100.0 else 0.0
                                val isProfit = profitAbs >= 0

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(itemColor.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isSale) Icons.AutoMirrored.Outlined.TrendingDown else Icons.AutoMirrored.Outlined.TrendingUp,
                                                    contentDescription = null,
                                                    tint = itemColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = if (isSale) "VENDA" else "COMPRA",
                                                        color = itemColor,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 11.sp,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault()).format(java.util.Date(tx.date)),
                                                        color = TextSecondary,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val fmtQty = if (tx.quantity % 1.0 == 0.0) tx.quantity.toInt().toString() else String.format("%.2f", tx.quantity)
                                                Text(
                                                    text = "$fmtQty cotas",
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = "R$ ${String.format("%.2f", tx.purchasePrice)} /cota",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(text = "VALOR TOTAL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = "R$ ${String.format("%.2f", txTotalValue)}",
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 14.sp
                                                )
                                                
                                                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    IconButton(
                                                        onClick = { onEditTransaction(tx) },
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                                                            .size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Edit,
                                                            contentDescription = "Editar",
                                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { onDeleteTransaction(tx) },
                                                        modifier = Modifier
                                                            .background(Color.Red.copy(alpha = 0.08f), CircleShape)
                                                            .size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Delete,
                                                            contentDescription = "Deletar",
                                                            tint = DangerRed.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (isPurchase && txTotalValue > 0) {
                                            HorizontalDivider(color = BorderColor.copy(alpha = 0.08f), thickness = 1.dp)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("STATUS DO APORTE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextSecondary, letterSpacing = 0.5.sp)
                                                val pColor = if (isProfit) SuccessGreen else DangerRed
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${if (isProfit) "+" else ""}R$ ${String.format("%.2f", profitAbs)}",
                                                        color = pColor,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(pColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                            .border(1.dp, pColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "${if (isProfit) "+" else ""}${String.format("%.1f", profitPct)}%",
                                                            color = pColor,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

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
