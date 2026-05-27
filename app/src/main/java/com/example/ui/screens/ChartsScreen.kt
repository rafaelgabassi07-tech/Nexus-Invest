package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.PortfolioViewModel
import com.example.viewmodel.AssetSummary
import com.example.viewmodel.PortfolioAnalyticsState
import com.example.network.DividendEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(viewModel: PortfolioViewModel, modifier: Modifier = Modifier) {
    val summaries by viewModel.assetSummaries.collectAsStateWithLifecycle()
    val summaryModel by viewModel.portfolioSummary.collectAsStateWithLifecycle()
    val analytics by viewModel.portfolioAnalytics.collectAsStateWithLifecycle()
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    
    val firstTransactionTime = remember(allTransactions) {
        allTransactions.minOfOrNull { it.date } ?: System.currentTimeMillis()
    }
    
    // State to toggle dynamic premium sub-screens/pages ("Proventos", "IPCA+", "Diversificação", "Agenda")
    var activeDetailPage by remember { mutableStateOf<String?>(null) }
    
    val totalCurrent = if (summaryModel.totalCurrentValue.isNaN() || summaryModel.totalCurrentValue.isInfinite()) 0.0 else summaryModel.totalCurrentValue

    // Calculate Average Yield %
    val avgDy = remember(summaryModel, summaries) {
        if (totalCurrent > 0) {
            (summaries.sumOf { it.totalCurrentValue * it.dividendYield } / totalCurrent).let {
                if (it.isNaN() || it.isInfinite()) 0.0 else it
            }
        } else 0.0
    }
    
    // Calculate final monthly dividend
    val finalMonthlyDiv = remember(totalCurrent, avgDy, analytics.analysis) {
        val proxyMonthly = analytics.analysis?.monthlyDividendEstimate ?: 0.0
        val localMonthly = ((totalCurrent * (avgDy / 100.0)) / 12.0).let {
            if (it.isNaN() || it.isInfinite()) 0.0 else it
        }
        if (proxyMonthly > 0.0) proxyMonthly else localMonthly
    }

    // Generate monthly projected dividend values
    val divDataValues = remember(finalMonthlyDiv, totalCurrent) {
        if (totalCurrent > 0) {
            List(12) { i -> 
                val scale = (i + 1) / 12f // Linear growth modeling
                (finalMonthlyDiv * scale).toFloat()
            }
        } else {
            List(12) { 0f }
        }
    }
    
    val currentCalendar = java.util.Calendar.getInstance()
    val divStackedDataValues = remember(divDataValues, firstTransactionTime) {
        val calFirst = java.util.Calendar.getInstance().apply { timeInMillis = firstTransactionTime }
        val startMonthIdx = calFirst.get(java.util.Calendar.YEAR) * 12 + calFirst.get(java.util.Calendar.MONTH)

        List(6) { i -> 
            // We want last 2 received, next 4 projected (for demonstration)
            val monthIndex = i - 1
            val cal = currentCalendar.clone() as java.util.Calendar
            cal.add(java.util.Calendar.MONTH, monthIndex)
            val currentMonthIdx = cal.get(java.util.Calendar.YEAR) * 12 + cal.get(java.util.Calendar.MONTH)
            
            val monthStr = String.format("%02d/%d", cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.YEAR))
            
            // Map the 6 values dynamically from divDataValues or just linear growth
            val value = divDataValues.getOrNull(i * 2) ?: 0f
            
            val isBeforeExistence = currentMonthIdx < startMonthIdx

            if (i < 2) {
                com.example.ui.components.StackedBarData(
                    received = if (isBeforeExistence) 0f else value, 
                    projected = 0f, 
                    label = monthStr
                )
            } else {
                com.example.ui.components.StackedBarData(
                    received = if (isBeforeExistence) 0f else value * 0.8f, 
                    projected = if (isBeforeExistence) 0f else value * 0.2f, 
                    label = monthStr
                )
            }
        }
    }
    
    // Generate Portfolio Curve vs IPCA (assuming 5.5% IPCA over the period)
    val portReturnPct = remember(summaryModel) {
        summaryModel.returnPercent.toFloat().let {
            if (it.isNaN() || it.isInfinite()) 0f else it
        }
    }
    
    val portDataValues = remember(portReturnPct, analytics.portfolioHistory) {
        if (analytics.portfolioHistory.isNotEmpty()) {
            analytics.portfolioHistory.map { it.returnPercent.toFloat() }
        } else {
            List(12) { i -> (portReturnPct / 12f) * (i + 1) }
        }
    }
    
    val ipcaDataValues = remember(totalCurrent, analytics.ipcaSeries) {
        if (analytics.ipcaSeries.isNotEmpty()) {
            analytics.ipcaSeries.map { it.accumulatedPercent.toFloat() }
        } else {
            val ipcaAccumulated = if (totalCurrent > 0) 5.5f else 0f
            List(12) { i -> (ipcaAccumulated / 12f) * (i + 1) }
        }
    }

    val currentIpcaAccumulated = ipcaDataValues.lastOrNull() ?: 0f

    // Segment data for visual allocation
    val alocacaoData = remember(summaryModel, analytics.analysis) {
        val remote = analytics.analysis?.allocationByClass?.map { it.first to it.second.toFloat() }.orEmpty().filter { it.second > 0f }
        if (remote.isNotEmpty()) {
            remote
        } else {
            val acoes = (summaryModel.sharesRatioStock * 100).toFloat()
            val fiis = (summaryModel.sharesRatioFii * 100).toFloat()
            val data = mutableListOf<Pair<String, Float>>()
            if (acoes > 0f) data.add("Ações" to acoes)
            if (fiis > 0f) data.add("FIIs" to fiis)
            if (data.isEmpty()) data.add("Sem Cotas" to 100f)
            data
        }
    }
    
    // Sector-specific calculations
    val segmentosData = remember(summaries, totalCurrent, analytics.analysis) {
        val remote = analytics.analysis?.allocationBySector?.map { it.first to it.second.toFloat() }.orEmpty().filter { it.second > 0f }
        if (remote.isNotEmpty()) return@remember remote
        if (totalCurrent <= 0.0) return@remember listOf("Sem Cotas" to 100f)
        
        val setores = summaries.groupBy {
            val t = it.ticker.uppercase()
            when {
                t.startsWith("ITUB") || t.startsWith("BBDC") || t.startsWith("BBAS") || t.startsWith("SANB") || t.startsWith("BPAC") -> "Bancos"
                t.startsWith("EGIE") || t.startsWith("TAEE") || t.startsWith("CPFE") || t.startsWith("ENBR") || t.startsWith("CMIG") -> "Energia"
                t.startsWith("VALE") || t.startsWith("CSNA") || t.startsWith("USIM") -> "Mineração"
                t.startsWith("PETR") || t.startsWith("PRIO") || t.startsWith("RRRP") -> "Petróleo/Gás"
                t.startsWith("WEGE") || t.startsWith("EMBR") -> "Indústria"
                it.type == "FII" || it.type.uppercase() == "FII" -> "Fundos Imobiliários"
                else -> "Outros"
            }
        }.mapValues { entry -> 
            val sectorVal = entry.value.sumOf { it.totalCurrentValue }
            (sectorVal / totalCurrent * 100.0).toFloat().let { if (it.isNaN() || it.isInfinite()) 0f else it }
        }.toList()
        if (setores.isEmpty()) listOf("Sem Cotas" to 100f) else setores
    }
    
    val topAgenda = remember(summaries) {
        summaries.sortedByDescending { it.dividendYield * it.totalCurrentValue }.take(4)
    }

    // Intercept physical Back presses to return to listing
    BackHandler(enabled = activeDetailPage != null) {
        activeDetailPage = null
    }

    Box(
        modifier = modifier.fillMaxSize().background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Gráficos & Estatísticas",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Aprecie análises avançadas de rentabilidade, proventos e calendário.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            // 1. Proventos Card
            item {
                ChartCard(
                    title = "Evolução de Proventos",
                    description = "Seus dividendos projetados lineares nos próximos 12 meses.",
                    subStats = "Yield Médio: ${String.format("%.2f%%", avgDy)}",
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    onClick = { activeDetailPage = "Proventos" }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        com.example.ui.components.StackedBarChart(
                            data = divStackedDataValues,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 2. IPCA+ Card
            item {
                ChartCard(
                    title = "Rentabilidade vs IPCA+",
                    description = "Sua carteira comparada com IPCA via Proxy quando disponível, com fallback transparente local.",
                    subStats = "Ganho Real Líquido: ${String.format("%+.2f%%", portReturnPct - currentIpcaAccumulated)}",
                    icon = Icons.Outlined.QueryStats,
                    onClick = { activeDetailPage = "IPCA+" }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        CustomLineChartCompare(
                            portfolioValues = portDataValues,
                            ipcaValues = ipcaDataValues,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 3. Diversificação Board (Pie Charts preview)
            item {
                ChartCard(
                    title = "Equilíbrio de Carteira",
                    description = "Distribuição percentual por classe de ativos e diversificação do mercado.",
                    subStats = "${segmentosData.filter { it.first != "Sem Cotas" }.size} Setores ativos",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    onClick = { activeDetailPage = "Diversificação" }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val allocatedPercent = alocacaoData.sumOf { it.second.toDouble() }.coerceAtMost(100.0)
                        
                        PieChart(
                            data = alocacaoData,
                            colors = listOf(GoldPrimary, GoldPale, GoldSecondary, GoldBronze, GoldTertiary),
                            centerText = "Carteira",
                            centerSubtext = "${allocatedPercent.toInt()}% Alocados"
                        )
                    }
                }
            }

            // 4. Agenda de Dividendos Preview
            item {
                val agendaPreviewChartData = remember(topAgenda, analytics.dividendEvents) {
                    if (analytics.dividendEvents.isNotEmpty()) {
                        analytics.dividendEvents.take(6).map { event ->
                            com.example.ui.components.StackedBarData(
                                received = if (event.status.contains("pago", ignoreCase = true)) event.estimatedAmount.toFloat() else 0f,
                                projected = if (!event.status.contains("pago", ignoreCase = true)) event.estimatedAmount.toFloat() else 0f,
                                label = event.ticker.take(5)
                            )
                        }
                    } else {
                        topAgenda.mapNotNull { asset ->
                            val divAmt = asset.lastDividend * asset.sharesCount
                            if (divAmt <= 0.0) null else com.example.ui.components.StackedBarData(
                                received = 0f,
                                projected = divAmt.toFloat(),
                                label = asset.ticker.take(5)
                            )
                        }
                    }
                }
                ChartCard(
                    title = "Agenda de Dividendos",
                    description = "Datas-com e pagamentos projetados por ativos da sua carteira.",
                    subStats = "R$ ${String.format("%.2f", analytics.dividendEvents.sumOf { it.estimatedAmount }.takeIf { it > 0.0 } ?: topAgenda.sumOf { it.lastDividend * it.sharesCount })} estimado",
                    icon = Icons.AutoMirrored.Outlined.EventNote,
                    onClick = { activeDetailPage = "Agenda" }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        if (agendaPreviewChartData.isNotEmpty()) {
                            com.example.ui.components.StackedBarChart(
                                data = agendaPreviewChartData,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Sem eventos de dividendos disponíveis", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (activeDetailPage != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { activeDetailPage = null },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    ChartDetailPage(
                        pageName = activeDetailPage!!,
                        summaries = summaries,
                        summaryModel = summaryModel,
                        divDataValues = divDataValues,
                        divStackedDataValues = divStackedDataValues,
                        portDataValues = portDataValues,
                        ipcaDataValues = ipcaDataValues,
                        alocacaoData = alocacaoData,
                        segmentosData = segmentosData,
                        topAgenda = topAgenda,
                        allTransactions = allTransactions,
                        analytics = analytics,
                        onBack = { activeDetailPage = null },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    )
                }
            }
        }
    }
}

@Composable
fun ChartCard(
    title: String,
    description: String,
    subStats: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.04f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title, 
                            color = TextPrimary, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description, 
                        color = TextSecondary, 
                        fontSize = 12.sp, 
                        lineHeight = 16.sp
                    )
                    
                    if (subStats.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .background(GoldPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = subStats, 
                                color = GoldPrimary, 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward, 
                    contentDescription = "Ver Detalhes", 
                    tint = GoldPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fonte de dados: Valorae Proxy", 
                    color = TextSecondary, 
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Análise Detalhada ➔", 
                    color = GoldPrimary, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun DividendScheduleList(agenda: List<AssetSummary>, transactions: List<com.example.data.Transaction>, limit: Int = Int.MAX_VALUE) {
    val showList = remember(agenda, limit) { agenda.take(limit) }
    if (showList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Sem previsões de dividendos ativos na carteira.", color = TextSecondary, fontSize = 13.sp)
        }
        return
    }
    
    val sdf = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        showList.forEachIndexed { index, asset ->
            val tickerTxs = remember(transactions, asset.ticker) {
                transactions.filter { it.ticker.uppercase() == asset.ticker.uppercase() }
            }

            var comDateMillis: Long? = null
            if (asset.nextEarningsDate.isNotBlank()) {
                try {
                    comDateMillis = sdf.parse(asset.nextEarningsDate)?.time
                } catch (e: Exception) {}
            }

            // Check how many shares the user had at Data Com
            val sharesAtDataCom = remember(tickerTxs, comDateMillis) {
                if (comDateMillis == null) asset.sharesCount // Fallback to current if date unknown
                else {
                    var count = 0.0
                    tickerTxs.filter { it.date <= comDateMillis }.forEach {
                        if (it.isSell) count -= it.quantity else count += it.quantity
                    }
                    count
                }
            }

            val divAmt = sharesAtDataCom * asset.lastDividend
            val isEligible = sharesAtDataCom > 0.0001
            
            val comDateStr = if (asset.nextEarningsDate.isNotBlank()) asset.nextEarningsDate else "Breve"
            
            // Parse real day and month from comDateStr if available (dd/MM/yyyy)
            var displayDay = ""
            var displayMonth = ""
            if (asset.nextEarningsDate.contains("/")) {
                val parts = asset.nextEarningsDate.split("/")
                if (parts.size >= 2) {
                    displayDay = parts[0]
                    displayMonth = when (parts[1]) {
                        "01" -> "JAN"
                        "02" -> "FEV"
                        "03" -> "MAR"
                        "04" -> "ABR"
                        "05" -> "MAI"
                        "06" -> "JUN"
                        "07" -> "JUL"
                        "08" -> "AGO"
                        "09" -> "SET"
                        "10" -> "OUT"
                        "11" -> "NOV"
                        "12" -> "DEZ"
                        else -> "Mês"
                    }
                }
            }
            
            // Fallback for fallback/simulation assets (No longer using speculative/fake dates based on hash)
            if (displayDay.isEmpty()) {
                displayDay = "—"
                displayMonth = "BRV"
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Calendar Indicator Badge
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 44.dp)
                        .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = displayMonth,
                            color = GoldPrimary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = displayDay,
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = asset.ticker,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Data Com: $comDateStr",
                        color = TextSecondary,
                        fontSize = 10.5.sp
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("R$ %.2f", divAmt),
                        color = if (isEligible) SuccessGreen else TextSecondary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEligible) "Confirmado" else "Inelegível",
                        color = if (isEligible) SuccessGreen else TextSecondary.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun DividendEventsList(events: List<DividendEvent>, limit: Int = Int.MAX_VALUE) {
    val showList = remember(events, limit) { events.take(limit) }
    if (showList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Sem eventos de dividendos retornados pelo Proxy.", color = TextSecondary, fontSize = 13.sp)
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        showList.forEach { event ->
            val payDate = event.paymentDate.ifBlank { "A confirmar" }
            val comDate = event.dateCom.ifBlank { "A confirmar" }
            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 50.dp, height = 44.dp)
                            .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.22f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PGTO", color = GoldPrimary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            Text(payDate.take(5).ifBlank { "—" }, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(event.ticker, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(event.status, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("Data COM: $comDate · ${event.source}", color = TextSecondary, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("R$ ${String.format("%.2f", event.estimatedAmount)}", color = SuccessGreen, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text("R$ ${String.format("%.4f", event.valuePerShare)}/cota", color = TextSecondary, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// DETAILED BOARD PAGES WITH METRICS AND ADVISORS
// -----------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartDetailPage(
    pageName: String,
    summaries: List<AssetSummary>,
    summaryModel: com.example.viewmodel.PortfolioSummary,
    divDataValues: List<Float>,
    divStackedDataValues: List<com.example.ui.components.StackedBarData>,
    portDataValues: List<Float>,
    ipcaDataValues: List<Float>,
    alocacaoData: List<Pair<String, Float>>,
    segmentosData: List<Pair<String, Float>>,
    topAgenda: List<AssetSummary>,
    allTransactions: List<com.example.data.Transaction>,
    analytics: PortfolioAnalyticsState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCurrent = if (summaryModel.totalCurrentValue.isNaN()) 0.0 else summaryModel.totalCurrentValue
    val avgDy = remember(summaryModel, summaries) {
        if (totalCurrent > 0) summaries.sumOf { it.totalCurrentValue * it.dividendYield } / totalCurrent else 0.0
    }
    val finalMonthlyDiv = (totalCurrent * (avgDy / 100.0)) / 12.0

    val firstTransactionTime = remember(allTransactions) {
        allTransactions.minOfOrNull { it.date } ?: System.currentTimeMillis()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Sticky/Fixed standardized header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(DarkSurface, RoundedCornerShape(14.dp))
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack, 
                    contentDescription = "Voltar", 
                    tint = GoldPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = pageName,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = getPageSubtitle(pageName),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        
        HorizontalDivider(color = BorderColor.copy(alpha = 0.08f), thickness = 1.dp)
        
        // Scrollable Content Pane
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
        
        if (pageName == "Proventos") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Yield Médio", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${String.format("%.1f", avgDy)}%", color = GoldPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Média Mensal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("R$ ${String.format("%.2f", finalMonthlyDiv)}", color = SuccessGreen, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Projetado 12M", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("R$ ${String.format("%.2f", finalMonthlyDiv * 12.0)}", color = GoldPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 24.dp))
        }
        
        if (pageName == "IPCA+") {
            val portReturnPct = if (summaryModel.returnPercent.isNaN() || summaryModel.returnPercent.isInfinite()) 0f else summaryModel.returnPercent.toFloat()
            val effectiveIpca = analytics.ipcaSeries.lastOrNull()?.accumulatedPercent?.toFloat() ?: 5.5f
            val realReturn = portReturnPct - effectiveIpca
            
            // Streamlined non-container info ribbon with a gold accent icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (realReturn >= 0) "Sua carteira está vencendo a inflação, garantindo aumento e preservação do seu poder de compra estrutural."
                           else "A inflação acumulada superou o rendimento. Considere focar em aportes em ativos de valor ou atrelados ao IPCA para proteger seu capital.",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // -----------------------------------------------------------------
        // PRIMARY CHART SUB-VIEW
        // -----------------------------------------------------------------
        if (pageName == "Proventos") {
            var selectedTime by remember { mutableStateOf("12 meses") }
            var selectedFilter by remember { mutableStateOf("Todos") }
            
            var selectedTimePie by remember { mutableStateOf("Últimos 12 meses") }
            var selectedFilterPie by remember { mutableStateOf("Todos") }

            val currentCalendar = java.util.Calendar.getInstance()
            
            val dynamicStackedData = remember(summaries, selectedTime, selectedFilter, firstTransactionTime) {
                val calFirst = java.util.Calendar.getInstance().apply { timeInMillis = firstTransactionTime }
                val startMonthIdx = calFirst.get(java.util.Calendar.YEAR) * 12 + calFirst.get(java.util.Calendar.MONTH)

                val filteredAssets = when (selectedFilter) {
                    "Apenas FIIs" -> summaries.filter { it.type == "FII" }
                    "Apenas Ações" -> summaries.filter { it.type == "ACAO" }
                    else -> summaries
                }
                val totalMonthlyFilteredDiv = filteredAssets.sumOf {
                    it.sharesCount * (it.currentPrice * (it.dividendYield / 100.0) / 12.0)
                }
                
                val barCount = when (selectedTime) {
                    "6 meses" -> 6
                    "24 meses" -> 24
                    else -> 12
                }
                
                val startOffset = barCount / 3
                
                List(barCount) { i ->
                    val cal = currentCalendar.clone() as java.util.Calendar
                    cal.add(java.util.Calendar.MONTH, i - startOffset)
                    val monthStr = String.format("%02d/%d", cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.YEAR))
                    val currentMonthIdx = cal.get(java.util.Calendar.YEAR) * 12 + cal.get(java.util.Calendar.MONTH)
                    
                    val isBeforeExistence = currentMonthIdx < startMonthIdx

                    val baseFactor = 0.618f + 0.382f * (i.toFloat() / barCount.coerceAtLeast(1))
                    val totalVal = (totalMonthlyFilteredDiv * baseFactor).toFloat()
                    
                    val receivedVal = if (isBeforeExistence) 0f else if (i < startOffset) {
                        totalVal
                    } else if (i == startOffset) {
                        totalVal * 0.45f
                    } else {
                        0f
                    }
                    val projectedVal = if (isBeforeExistence) 0f else if (i < startOffset) {
                        0f
                    } else if (i == startOffset) {
                        totalVal * 0.55f
                    } else {
                        totalVal
                    }
                    
                    com.example.ui.components.StackedBarData(
                        received = receivedVal,
                        projected = projectedVal,
                        label = monthStr
                    )
                }
            }

            val dynamicTopDividendAssets = remember(summaries, selectedTimePie, selectedFilterPie) {
                val filteredAssets = when (selectedFilterPie) {
                    "FIIs" -> summaries.filter { it.type == "FII" }
                    "Ações" -> summaries.filter { it.type == "ACAO" }
                    else -> summaries
                }
                
                val multiplier = when (selectedTimePie) {
                    "Últimos 12 meses" -> 12f
                    "Últimos 6 meses" -> 6f
                    "Neste ano" -> 5f
                    else -> 12f
                }
                
                if (totalCurrent <= 0.0) emptyList<Pair<String, Float>>()
                else {
                    filteredAssets.map { asset ->
                        val monthlyDiv = asset.sharesCount * (asset.currentPrice * (asset.dividendYield / 100.0) / 12.0)
                        Pair(asset.ticker, (monthlyDiv * multiplier).toFloat())
                    }.sortedByDescending { it.second }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EVOLUÇÃO DE PROVENTOS",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var expandedTime by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f)),
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth().clickable { expandedTime = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.DateRange, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(selectedTime, color = TextPrimary, fontSize = 13.sp)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = expandedTime,
                            onDismissRequest = { expandedTime = false }
                        ) {
                            listOf("6 meses", "12 meses", "24 meses").forEach {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = { selectedTime = it; expandedTime = false }
                                )
                            }
                        }
                    }

                    var expandedFilter by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f)),
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth().clickable { expandedFilter = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FilterList, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(selectedFilter, color = TextPrimary, fontSize = 13.sp)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = expandedFilter,
                            onDismissRequest = { expandedFilter = false }
                        ) {
                            listOf("Todos", "Apenas FIIs", "Apenas Ações").forEach {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = { selectedFilter = it; expandedFilter = false }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(GoldPrimary))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recebidos", color = TextPrimary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(GoldPale))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("A receber", color = TextPrimary, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    com.example.ui.components.StackedBarChart(
                        data = dynamicStackedData,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                
                if (dynamicTopDividendAssets.isNotEmpty()) {
                    Text(
                        text = "PROVENTOS POR ATIVOS",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var expandedTimePie by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f)),
                                color = Color.Transparent,
                                modifier = Modifier.fillMaxWidth().clickable { expandedTimePie = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.DateRange, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(selectedTimePie, color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = expandedTimePie,
                                onDismissRequest = { expandedTimePie = false }
                            ) {
                                listOf("Últimos 12 meses", "Neste ano", "Últimos 6 meses").forEach {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = { selectedTimePie = it; expandedTimePie = false }
                                    )
                                }
                            }
                        }

                        var expandedFilterPie by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f)),
                                color = Color.Transparent,
                                modifier = Modifier.fillMaxWidth().clickable { expandedFilterPie = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FilterList, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(selectedFilterPie, color = TextPrimary, fontSize = 13.sp)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = expandedFilterPie,
                                onDismissRequest = { expandedFilterPie = false }
                            ) {
                                listOf("Todos", "FIIs", "Ações").forEach {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = { selectedFilterPie = it; expandedFilterPie = false }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val pieColors = listOf(GoldPrimary, GoldPale, GoldSecondary, GoldBronze, GoldTertiary, GoldDeep)
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PieChart(
                            data = dynamicTopDividendAssets,
                            colors = pieColors,
                            centerText = "Top 4",
                            centerSubtext = "Pagadores",
                            modifier = Modifier.size(150.dp)
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            var cumulativeSum = dynamicTopDividendAssets.sumOf { it.second.toDouble() }
                            if (cumulativeSum == 0.0) cumulativeSum = 1.0
                            
                            dynamicTopDividendAssets.forEachIndexed { index, pair ->
                                val pct = (pair.second / cumulativeSum) * 100
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(pair.first, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.3f))
                                    Text(String.format("%.2f%%", pct), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.3f))
                                    Box(
                                        modifier = Modifier.weight(0.4f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(BorderColor.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth((pct / 100f).toFloat().coerceAtMost(1f)).fillMaxHeight().background(pieColors[index % pieColors.size])
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = BorderColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))
        } else {
            HorizontalDivider(color = BorderColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when(pageName) {
                            "IPCA+" -> Icons.AutoMirrored.Outlined.TrendingUp
                            "Diversificação" -> Icons.Default.PieChart
                            "Agenda" -> Icons.Default.DateRange
                            else -> Icons.AutoMirrored.Filled.ShowChart
                        },
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when(pageName) {
                            "IPCA+" -> "RENDIMENTO VS IPCA+"
                            "Diversificação" -> "EQUILÍBRIO DA CARTEIRA E CONCENTRAÇÃO"
                            "Agenda" -> "AGENDA DE DIVIDENDOS"
                            else -> "ANALISE VETORIAL"
                        },
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                when (pageName) {
                    "IPCA+" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "EVOLUÇÃO DOS JUROS REAIS (CARTEIRA VS IPCA)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                                CustomLineChartCompare(
                                    portfolioValues = portDataValues,
                                    ipcaValues = ipcaDataValues,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = "Fonte: ${analytics.source} · IPCA: ${analytics.ipcaSeries.lastOrNull()?.source ?: "estimativa local"}",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    "Diversificação" -> {
                        val top5 = remember(summaries, totalCurrent) {
                            if (totalCurrent <= 0.0) 0f
                            else (summaries.sortedByDescending { it.totalCurrentValue }.take(5).sumOf { it.totalCurrentValue } / totalCurrent * 100.0).toFloat()
                        }
                        
                        Column {
                            // 1. Big Pie Chart to represent Diversification
                            val alocColors = listOf(GoldPrimary, GoldPale, GoldSecondary, GoldBronze, GoldTertiary)
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(bottom = 16.dp)
                            ) {
                                com.example.ui.components.PieChart(
                                    data = alocacaoData,
                                    colors = alocColors,
                                    centerText = "Carteira",
                                    centerSubtext = "Diversificação",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            // 1. Concentration Alert / Summary
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(48.dp).background(GoldPrimary.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.PieChart, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Índice de Concentração", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Top 5 ativos representam ${String.format("%.1f%%", top5)}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                        Text(
                                            text = when {
                                                top5 > 60f -> "Alta concentração. Verifique se os ativos são sólidos."
                                                top5 > 40f -> "Concentração moderada. Bom equilíbrio de risco."
                                                else -> "Carteira bem diversificada. Baixa exposição individual."
                                            },
                                            color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // 2. Class Allocation (Linear Bars)
                            Text(
                                text = "POR CLASSE DE ATIVO",
                                color = GoldPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            alocacaoData.filter { it.first != "Sem Cotas" }.forEach { (label, percent) ->
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("${String.format("%.1f", percent)}%", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val barColor = if (label == "Ações") GoldPrimary else GoldPale
                                    LinearProgressIndicator(
                                        progress = { percent / 100f },
                                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                                        color = barColor,
                                        trackColor = BorderColor.copy(alpha = 0.05f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.05f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(24.dp))

                            // 3. Sector Breakdown (Table/List)
                            Text(
                                text = "POR SETOR / SEGMENTO",
                                color = GoldPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            segmentosData.filter { it.first != "Sem Cotas" }.sortedByDescending { it.second }.forEach { (sector, percent) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(8.dp).background(GoldSecondary, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(sector, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    Text("${String.format("%.1f", percent)}%", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                }
                                HorizontalDivider(color = BorderColor.copy(alpha = 0.03f))
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // 4. Detailed Asset Weights
                            Text(
                                text = "DETALHAMENTO POR ATIVO",
                                color = GoldPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            summaries.sortedByDescending { it.totalCurrentValue }.forEach { asset ->
                                val weight = if (totalCurrent > 0) (asset.totalCurrentValue / totalCurrent * 100.0).toFloat() else 0f
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(asset.ticker, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape).background(BorderColor.copy(alpha = 0.05f))) {
                                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(weight/100f).background(GoldSecondary))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("${String.format("%.1f", weight)}%", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                    "Agenda" -> {
                        val agendaChartData = remember(topAgenda) {
                            topAgenda.map { asset ->
                                val divAmt = asset.lastDividend * asset.sharesCount
                                com.example.ui.components.StackedBarData(
                                    received = if (divAmt > 0) divAmt.toFloat() else 0f,
                                    projected = 0f,
                                    label = asset.ticker.take(5)
                                )
                            }
                        }
                        val proxyEvents = analytics.dividendEvents
                        val eventChartData = if (proxyEvents.isNotEmpty()) {
                            proxyEvents.take(8).map { event ->
                                com.example.ui.components.StackedBarData(
                                    received = if (event.paymentDate.isNotBlank()) event.estimatedAmount.toFloat() else 0f,
                                    projected = if (event.paymentDate.isBlank()) event.estimatedAmount.toFloat() else 0f,
                                    label = event.ticker.take(5)
                                )
                            }
                        } else agendaChartData
                        if (eventChartData.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(160.dp).padding(bottom = 16.dp)) {
                                com.example.ui.components.StackedBarChart(
                                    data = eventChartData,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        if (proxyEvents.isNotEmpty()) {
                            DividendEventsList(proxyEvents)
                        } else {
                            DividendScheduleList(topAgenda, allTransactions)
                        }
                    }
                }
            }
        } // ends else block
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // -----------------------------------------------------------------
        // MULTI-YEAR ACCUMULATION CARDS / ACTIVE SELECTORS
           if (pageName == "Proventos") {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.TrendingUp, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EFEITO BOLA DE NEVE",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                val avgAssetPrice = if (summaries.isNotEmpty()) summaries.sumOf { it.averageCost } / summaries.size else 50.0
                val snowballProgress = (finalMonthlyDiv / avgAssetPrice).coerceIn(0.0, 1.1).toFloat()
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape).background(BorderColor.copy(alpha = 0.1f))) {
                        Box(modifier = Modifier.fillMaxWidth(snowballProgress.coerceAtMost(1f)).fillMaxHeight().background(TextPrimary))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (snowballProgress >= 1f) "ATIVADO!" else "${String.format("%.0f", snowballProgress * 100)}%",
                        color = TextPrimary,
                        fontWeight = FontWeight.Light,
                        fontSize = 13.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (snowballProgress >= 1f) 
                        "Parabéns! Seus dividendos médios mensais já são suficientes para comprar pelo menos 1 nova cota da sua carteira mensalmente sem nenhum aporte extra." 
                        else "Você está a R$ ${String.format("%.2f", avgAssetPrice - finalMonthlyDiv)} de atingir o primeiro estágio da Bola de Neve: comprar 1 nova cota mensalmente apenas com dividendos.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.QueryStats, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SIMULADOR DE ACUMULAÇÃO",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1 Year projection
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("1 ANO", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "R$ ${String.format("%.0f", finalMonthlyDiv * 12.0)}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Light,
                            fontSize = 16.sp
                        )
                    }
                    
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(BorderColor.copy(alpha = 0.1f)))
                    
                    // 5 Year compounding projection
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("5 ANOS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        val r = (avgDy / 100.0) + 0.05
                        val futureCapital = totalCurrent * Math.pow(1.0 + r, 5.0)
                        val composite5years = futureCapital - totalCurrent
                        Text(
                            text = "R$ ${String.format("%.0f", composite5years)}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Light,
                            fontSize = 16.sp
                        )
                    }
                    
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(BorderColor.copy(alpha = 0.1f)))
                    
                    // 10 Year compounding projection
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("10 ANOS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        val r = (avgDy / 100.0) + 0.05
                        val futureCapital10 = totalCurrent * Math.pow(1.0 + r, 10.0)
                        val composite10years = futureCapital10 - totalCurrent
                        Text(
                            text = "R$ ${String.format("%.0f", composite10years)}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Light,
                            fontSize = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "* Projeção estimada considerando reinvestimento total de dividendos e valorização anual conservadora de 5% (IPCA).",
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // -----------------------------------------------------------------
        // REBALANCING CONTROLLER (Only inside "Diversificação")
        // -----------------------------------------------------------------
        if (pageName == "Diversificação") {
            var targetStockPercent by remember { mutableStateOf(0.5f) } 
            
            HorizontalDivider(color = BorderColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SIMULADOR PORTFÓLIO IDEAL",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "Ajuste sua meta e veja onde realizar o próximo aporte para balancear o risco.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Ações", color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${(targetStockPercent * 100).toInt()}%", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Light)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("FIIs", color = GoldPrimary.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${((1f - targetStockPercent) * 100).toInt()}%", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Light)
                    }
                }
                
                Slider(
                    value = targetStockPercent,
                    onValueChange = { targetStockPercent = it },
                    valueRange = 0f..1f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = GoldPrimary,
                        activeTrackColor = GoldPrimary,
                        inactiveTrackColor = BorderColor.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                
                val stockCurrent = summaryModel.totalStocksCurrent
                val fiiCurrent = summaryModel.totalFiisCurrent
                val totalVVal = stockCurrent + fiiCurrent
                
                if (totalVVal > 0.0) {
                    val stockTargetVal = targetStockPercent * totalVVal
                    val fiiTargetVal = (1f - targetStockPercent) * totalVVal
                    
                    val stockDiff = stockTargetVal - stockCurrent
                    val fiiDiff = fiiTargetVal - fiiCurrent
                    val stockCurrentPct = ((stockCurrent / totalVVal) * 100).toFloat()
                    val fiiCurrentPct = ((fiiCurrent / totalVVal) * 100).toFloat()
                    
                    val isStockUnder = stockDiff > 0.0
                    val targetDiff = if (isStockUnder) stockDiff else fiiDiff
                    val targetClassLabel = if (isStockUnder) "Ações" else "Fundos Imobiliários"
                    val highlightClr = if (isStockUnder) GoldPrimary else GoldPale

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Distribuição Atual", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            Text("${String.format("%.1f", stockCurrentPct)}% : ${String.format("%.1f", fiiCurrentPct)}%", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Light)
                        }
                        Row(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(BorderColor.copy(alpha = 0.1f))) {
                            if (stockCurrentPct > 0) {
                                Box(modifier = Modifier.weight(stockCurrentPct).fillMaxHeight().background(GoldPrimary))
                            }
                            if (fiiCurrentPct > 0) {
                                Box(modifier = Modifier.weight(fiiCurrentPct).fillMaxHeight().background(GoldPale))
                            }
                        }
                        
                        // Beautiful, standardized, responsive alert container for suggestions at the bottom
                        Surface(
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "REBALANCEAMENTO RECOMENDADO",
                                        color = GoldPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Sugerimos realizar o próximo aporte de R$ ${String.format("%,.2f", kotlin.math.abs(targetDiff))} em $targetClassLabel.",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                
                                val isHighlyConcentrated = stockCurrentPct > 70f || fiiCurrentPct > 70f
                                if (isHighlyConcentrated) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = BorderColor.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = DangerRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Atenção: Carteira altamente concentrada.",
                                            color = DangerRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // -----------------------------------------------------------------
        // INFINITY QUOTA METRICS
        if (pageName == "Proventos") {
            HorizontalDivider(color = BorderColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "COTA DO INFINITO (RENDA PASSIVA)",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            
            if (summaries.isEmpty()) {
                TableEmptyState()
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    summaries.forEachIndexed { index, asset ->
                        val estAmtPerShare = asset.currentPrice * (asset.dividendYield / 100.0) / 12.0
                        val monthlyDiv = asset.sharesCount * estAmtPerShare
                        val sharesNeeded = if (estAmtPerShare > 0.0) kotlin.math.ceil(asset.currentPrice / estAmtPerShare) else 0.0
                        val diff = (sharesNeeded - asset.sharesCount).coerceAtLeast(0.0)
                        val progress = if (sharesNeeded > 0.0) (asset.sharesCount / sharesNeeded).toFloat().coerceIn(0f, 1f) else 0f
                        
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = asset.ticker, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Text(
                                    text = "R$ ${String.format("%.2f", monthlyDiv)} /mês", 
                                    color = TextPrimary, 
                                    fontWeight = FontWeight.Light, 
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Eficiência: %.0f de %.0f cotas".format(asset.sharesCount, sharesNeeded), 
                                    color = TextSecondary, 
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                if (diff == 0.0) {
                                    Text(text = "✓ Composto", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Light)
                                } else {
                                    Text(text = "Faltam %.0f".format(diff), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Light)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = TextPrimary,
                                trackColor = BorderColor.copy(alpha = 0.12f)
                            )
                        }
                        
                        if (index < summaries.lastIndex) {
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.05f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Reusable KPI Metrics summary row
        if (pageName != "Proventos") {
            val kpiData = remember(pageName, summaryModel, summaries) {
                getKpiMetricsForPage(pageName, summaries, summaryModel, analytics)
            }
            if (pageName == "Agenda") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, BorderColor.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    kpiData.forEachIndexed { index, metric ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = metric.first,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = metric.second,
                                color = when (metric.third) {
                                    "GREEN" -> SuccessGreen
                                    "RED" -> DangerRed
                                    else -> GoldPrimary
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (index < kpiData.lastIndex) {
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.06f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                KpiRow(metrics = kpiData)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // Collapsible Tabular representation - highly polished modern accordion interface to declutter space
        var isTableExpanded by remember { mutableStateOf(false) }

        HorizontalDivider(color = BorderColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { isTableExpanded = !isTableExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (pageName == "Agenda") Icons.Default.DateRange else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DETALHAMENTO E TABELA DE VALORES",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(
                        onClick = { isTableExpanded = !isTableExpanded }
                    ) {
                        Icon(
                            imageVector = if (isTableExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isTableExpanded) "Recolher" else "Expandir",
                            tint = GoldPrimary
                        )
                    }
                }
                
                AnimatedVisibility(visible = isTableExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        when (pageName) {
                            "Proventos" -> {
                                TableHeaderRow(headers = listOf("Ativo", "Qtd. Cotas", "DY Anual", "Provento Mensal"))
                                if (summaries.isEmpty()) {
                                    TableEmptyState()
                                } else {
                                    val sortedSummaries = summaries.sortedByDescending { it.sharesCount * (it.currentPrice * (it.dividendYield / 100.0) / 12.0) }
                                    sortedSummaries.forEach { asset ->
                                        val estAmtPerShare = asset.currentPrice * (asset.dividendYield / 100.0) / 12.0
                                        val monthlyDiv = asset.sharesCount * estAmtPerShare
                                        TableDataRow(
                                            items = listOf(
                                                asset.ticker,
                                                String.format("%.0f un", asset.sharesCount),
                                                String.format("%.2f%%", asset.dividendYield),
                                                String.format("R$ %.2f", monthlyDiv)
                                            ),
                                            highlightIndex = 3
                                        )
                                    }
                                }
                            }
                            "IPCA+" -> {
                                TableHeaderRow(listOf("Mês", "Rend. Carteira", "IPCA Acum.", "Juros Reais"))
                                val maxRows = maxOf(portDataValues.size, ipcaDataValues.size).coerceAtMost(24)
                                if (maxRows == 0) {
                                    TableEmptyState()
                                } else {
                                    repeat(maxRows) { i ->
                                        val ipcaVal = ipcaDataValues.getOrNull(i) ?: 0f
                                        val portfolioVal = portDataValues.getOrNull(i) ?: 0f
                                        val realReturn = portfolioVal - ipcaVal
                                        val label = analytics.ipcaSeries.getOrNull(i)?.dateLabel ?: analytics.portfolioHistory.getOrNull(i)?.dateLabel ?: "Mês ${String.format("%02d", i + 1)}"
                                        TableDataRow(
                                            items = listOf(
                                                label,
                                                String.format("%+.2f%%", portfolioVal),
                                                String.format("%+.2f%%", ipcaVal),
                                                String.format("%+.2f%%", realReturn)
                                            ),
                                            isPositive = portfolioVal >= ipcaVal,
                                            highlightIndex = 3
                                        )
                                    }
                                }
                            }
                            "Diversificação" -> {
                                TableHeaderRow(listOf("Setor", "Ativos monitorados", "Alocado", "Participação"))
                                if (summaries.isEmpty() || totalCurrent == 0.0) {
                                    TableEmptyState()
                                } else {
                                    val sectorsGroup = summaries.groupBy {
                                        val t = it.ticker.uppercase()
                                        when {
                                            t.startsWith("ITUB") || t.startsWith("BBDC") || t.startsWith("BBAS") || t.startsWith("SANB") || t.startsWith("BPAC") -> "Bancos"
                                            t.startsWith("EGIE") || t.startsWith("TAEE") || t.startsWith("CPFE") || t.startsWith("ENBR") || t.startsWith("CMIG") -> "Energia"
                                            t.startsWith("VALE") || t.startsWith("CSNA") || t.startsWith("USIM") -> "Mineração"
                                            t.startsWith("PETR") || t.startsWith("PRIO") || t.startsWith("RRRP") -> "Petróleo"
                                            t.startsWith("WEGE") || t.startsWith("EMBR") -> "Indústria"
                                            it.type == "FII" || it.type.uppercase() == "FII" -> "FIIs"
                                            else -> "Outros"
                                        }
                                    }
                                    
                                    sectorsGroup.forEach { (sector, list) ->
                                        val sectorSum = list.sumOf { it.totalCurrentValue }
                                        val weight = (sectorSum / totalCurrent) * 100.0
                                        val joinedTickers = list.joinToString(", ") { it.ticker }
                                        TableDataRow(
                                            items = listOf(
                                                sector,
                                                joinedTickers,
                                                String.format("R$ %.1f", sectorSum),
                                                String.format("%.1f%%", weight)
                                            )
                                        )
                                    }
                                }
                            }
                            "Agenda" -> {
                                if (analytics.dividendEvents.isNotEmpty()) {
                                    DividendEventsList(analytics.dividendEvents)
                                } else {
                                    DividendScheduleList(summaries, allTransactions)
                                }
                            }
                        }
                    }
                }
            }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Expert Guidance Tip Box
        HorizontalDivider(color = BorderColor.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Info, 
                contentDescription = "Guia", 
                tint = GoldPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ACONSELHAMENTO",
                    color = GoldPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getPageExpertGuidance(pageName),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(50.dp))
        } // End scrollable Content Pane
    }
}

@Composable
fun TableHeaderRow(headers: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        headers.forEachIndexed { index, header ->
            Text(
                text = header,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(if (index == 1 && headers.size > 3) 1.5f else 1f),
                textAlign = if (index == 0) TextAlign.Start else if (index == headers.lastIndex) TextAlign.End else TextAlign.Center
            )
        }
    }
    HorizontalDivider(color = BorderColor.copy(alpha = 0.1f), thickness = 1.dp)
}

@Composable
fun TableDataRow(
    items: List<String>,
    highlightIndex: Int = -1,
    isPositive: Boolean? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val color = when {
                index == highlightIndex -> GoldPrimary
                isPositive != null && index == 1 -> if (isPositive == true) SuccessGreen else DangerRed
                isPositive != null && index == 2 -> TextSecondary
                else -> TextPrimary
            }
            
            val fontWeight = when {
                index == 0 -> FontWeight.Black
                index == highlightIndex -> FontWeight.Bold
                isPositive != null && index == 1 -> FontWeight.ExtraBold
                else -> FontWeight.Normal
            }

            Text(
                text = item,
                color = color,
                fontSize = 11.5.sp,
                fontWeight = fontWeight,
                modifier = Modifier.weight(if (index == 1 && items.size > 3) 1.5f else 1f),
                textAlign = if (index == 0) TextAlign.Start else if (index == items.lastIndex) TextAlign.End else TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(color = BorderColor.copy(alpha = 0.05f))
}

@Composable
fun TableEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nenhum ativo adicionado na carteira para detalhar.",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun KpiRow(
    metrics: List<Triple<String, String, String>>
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        metrics.forEach { metric ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(DarkSurface, RoundedCornerShape(20.dp))
                    .border(1.dp, BorderColor.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = metric.first.uppercase(),
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = metric.second,
                    color = when (metric.third) {
                        "GREEN" -> SuccessGreen
                        "RED" -> DangerRed
                        else -> GoldPrimary
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TEXT CONTENT & KPI COMPUTATION HELPERS
// -------------------------------------------------------------
fun getPageSubtitle(topic: String): String {
    return when (topic) {
        "Proventos" -> "Renda passiva gerada e projeção bola de neve para 12 meses"
        "IPCA+" -> "Variação patrimonial real comparada com o índice oficial IPCA"
        "Diversificação" -> "Equilíbrio de riscos e diversificação setorial do patrimônio"
        "Agenda" -> "Próximas datas com e pagamentos previstos para seus ativos"
        else -> ""
    }
}

fun getKpiMetricsForPage(
    page: String, 
    summaries: List<AssetSummary>, 
    summaryModel: com.example.viewmodel.PortfolioSummary,
    analytics: PortfolioAnalyticsState
): List<Triple<String, String, String>> {
    val totalCurrent = if (summaryModel.totalCurrentValue.isNaN()) 0.0 else summaryModel.totalCurrentValue
    val avgDy = if (totalCurrent > 0) summaries.sumOf { it.totalCurrentValue * it.dividendYield } / totalCurrent else 0.0
    val finalMonthlyDiv = (totalCurrent * (avgDy / 100.0)) / 12.0

    return when (page) {
        "Proventos" -> listOf(
            Triple("Yield Médio", String.format("%.2f%%", avgDy), "GOLD"),
            Triple("Média Mensal", String.format("R$ %.2f", finalMonthlyDiv), "GREEN"),
            Triple("Projeção Anual", String.format("R$ %.2f", finalMonthlyDiv * 12), "GOLD")
        )
        "IPCA+" -> {
            val portReturn = summaryModel.returnPercent
            val ipca = analytics.ipcaSeries.lastOrNull()?.accumulatedPercent ?: 5.5
            val realReturn = portReturn - ipca
            listOf(
                Triple("Carteira Acc.", String.format("%+.2f%%", portReturn), if (portReturn >= 0) "GREEN" else "RED"),
                Triple("IPCA Período", String.format("%+.2f%%", ipca), "GOLD"),
                Triple("Ganho Real", String.format("%+.2f%%", realReturn), if (realReturn >= 0) "GREEN" else "RED")
            )
        }
        "Diversificação" -> {
            val sectorsCount = summaries.map { it.ticker.take(4) }.distinct().size
            listOf(
                Triple("Sectores", "$sectorsCount", "GOLD"),
                Triple("Ações Peso", "%.1f%%".format(summaryModel.sharesRatioStock * 100), "GREEN"),
                Triple("Total Alocado", String.format("R$ %.0f", totalCurrent), "GOLD")
            )
        }
        "Agenda" -> {
            val events = analytics.dividendEvents
            val agendaCount = if (events.isNotEmpty()) events.size else summaries.size
            val sumDY = if (events.isNotEmpty()) events.sumOf { it.estimatedAmount } else summaries.sumOf { it.lastDividend * it.sharesCount }
            listOf(
                Triple("Eventos", "$agendaCount un", "GOLD"),
                Triple("Próx. Estimado", String.format("R$ %.2f", sumDY), "GREEN"),
                Triple("Fonte", if (events.isNotEmpty()) "Proxy" else "Local", "GOLD")
            )
        }
        else -> emptyList()
    }
}

fun getPageExpertGuidance(page: String): String {
    return when (page) {
        "Proventos" -> "O reinvestimento automático dos proventos recebidos permite que você compre mais cotas sem precisar tirar dinheiro extra do bolso. Isso cria o chamado 'Efeito Bola de Neve', reduzindo drasticamente o tempo necessário para conquistar sua independência financeira."
        "IPCA+" -> "Rentabilidades nominais altas podem enganar. Se a inflação do período for equivalente, você não enriqueceu. Foque em manter ativos sólidos de valor (ações geradoras e FIIs de tijolos de alta qualidade) cujo reajuste natural de contratos tendem a vencer a inflação histórica."
        "Diversificação" -> "Mantenha uma carteira equilibrada entre classes (Ações/FIIs) e diversificada em múltiplos setores perenes. O VALORAE ajuda você a mitigar riscos sistêmicos focando na solidez do seu patrimônio."
        "Agenda" -> "Utilize a Agenda de proventos para mapear as datas-com (dia limite para ter direito ao recebimento) e as datas de pagamento. Programe os aportes nos dias que antecedem a data-com para potencializar sua taxa de retorno passivo."
        else -> ""
    }
}
