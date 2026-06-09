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
import com.example.network.MarketRankingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(viewModel: PortfolioViewModel, modifier: Modifier = Modifier) {
    val summaries by viewModel.assetSummaries.collectAsStateWithLifecycle()
    val summaryModel by viewModel.portfolioSummary.collectAsStateWithLifecycle()
    val analytics by viewModel.portfolioAnalytics.collectAsStateWithLifecycle()
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    
    val firstTransactionTime = remember(allTransactions) {
        allTransactions.filter { !it.isSell && it.quantity > 0.0 }.minOfOrNull { it.date }
            ?: allTransactions.minOfOrNull { it.date }
            ?: System.currentTimeMillis()
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
    
    // Média confirmada de dividendos: não usa DY/último rendimento como simulação.
    val finalMonthlyDiv = remember(analytics.dividendEvents, allTransactions, firstTransactionTime) {
        val paid = analytics.dividendEvents
            .filter { isPaidDividendEvent(it) }
            .sumOf { eligibleDividendAmount(it, allTransactions) }
        val months = monthsInSelectedPeriod("Últimos 12 meses", firstTransactionTime).coerceAtLeast(1)
        if (paid > 0.0) paid / months else 0.0
    }

    // Série auxiliar sem projeção artificial.
    val divDataValues = remember(finalMonthlyDiv) {
        List(12) { finalMonthlyDiv.toFloat() }
    }
    
    val dividendEvolutionMonths = remember(firstTransactionTime) {
        portfolioAgeMonthsForInsights(firstTransactionTime).coerceAtLeast(3)
    }
    val divStackedDataValues = remember(analytics.dividendEvents, summaries, allTransactions, finalMonthlyDiv, firstTransactionTime, dividendEvolutionMonths) {
        buildDividendEvolutionData(
            events = analytics.dividendEvents,
            summaries = summaries,
            transactions = allTransactions,
            firstTransactionTime = firstTransactionTime,
            months = dividendEvolutionMonths,
            fallbackMonthly = finalMonthlyDiv
        )
    }
    
    // Curva Carteira vs IPCA. Prioriza séries reais do Proxy, ordenadas por tempo,
    // e só usa fallback local transparente quando o Proxy ainda não trouxe histórico.
    val portReturnPct = remember(summaryModel) {
        summaryModel.returnPercent.toFloat().takeIf { it.isFinite() } ?: 0f
    }
    val insightAgeMonths = remember(firstTransactionTime) { portfolioAgeMonthsForInsights(firstTransactionTime) }
    
    val portDataValues = remember(portReturnPct, analytics.portfolioHistory, insightAgeMonths) {
        val remote = analytics.portfolioHistory
            .sortedBy { it.timestamp }
            .mapNotNull { it.returnPercent.toFloat().takeIf { value -> value.isFinite() } }
        remote.ifEmpty { List(insightAgeMonths) { i -> (portReturnPct / insightAgeMonths.toFloat()) * (i + 1) } }
    }
    
    val ipcaDataValues = remember(totalCurrent, analytics.ipcaSeries, insightAgeMonths) {
        val remote = analytics.ipcaSeries
            .sortedBy { it.timestamp }
            .mapNotNull { it.accumulatedPercent.toFloat().takeIf { value -> value.isFinite() } }
        remote.ifEmpty {
            val ipcaAccumulated = if (totalCurrent > 0) 5.5f else 0f
            List(insightAgeMonths) { i -> (ipcaAccumulated / insightAgeMonths.toFloat()) * (i + 1) }
        }
    }

    val alignedIpcaPreview = remember(ipcaDataValues, portDataValues) { resampleInsightSeries(ipcaDataValues, portDataValues.size.coerceAtLeast(2)) }
    val currentIpcaAccumulated = alignedIpcaPreview.lastOrNull() ?: 0f

    // Segment data for visual allocation
    val alocacaoData = remember(summaryModel, analytics.analysis) {
        val remote = normalizeInsightPercentPairs(
            analytics.analysis?.allocationByClass?.map { it.first to it.second.toFloat() }.orEmpty()
        )
        if (remote.isNotEmpty()) {
            remote
        } else {
            val acoes = (summaryModel.sharesRatioStock * 100).toFloat()
            val fiis = (summaryModel.sharesRatioFii * 100).toFloat()
            val data = mutableListOf<Pair<String, Float>>()
            if (acoes > 0f) data.add("Ações" to acoes)
            if (fiis > 0f) data.add("FIIs" to fiis)
            normalizeInsightPercentPairs(data).ifEmpty { listOf("Sem Cotas" to 100f) }
        }
    }
    
    // Sector-specific calculations
    val segmentosData = remember(summaries, totalCurrent, analytics.analysis) {
        val remote = normalizeInsightPercentPairs(
            analytics.analysis?.allocationBySector?.map { it.first to it.second.toFloat() }.orEmpty()
        )
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
        normalizeInsightPercentPairs(setores).ifEmpty { listOf("Sem Cotas" to 100f) }
    }
    
    val topAgenda = remember(summaries) {
        summaries
            .filter { it.lastDividend > 0.0 || it.dividendYield > 0.0 }
            .sortedByDescending { (it.lastDividend * it.sharesCount).coerceAtLeast(it.dividendYield * it.totalCurrentValue) }
            .take(6)
            .ifEmpty { summaries.sortedByDescending { it.dividendYield * it.totalCurrentValue }.take(4) }
    }

    val rankingsPreview = remember(analytics.portfolioRanking, analytics.liveMarketRanking) {
        analytics.portfolioRanking?.score?.take(4).orEmpty()
            .ifEmpty { analytics.portfolioRanking?.dividendYield?.take(4).orEmpty() }
            .ifEmpty { analytics.liveMarketRanking?.highs?.take(4).orEmpty() }
            .ifEmpty { analytics.liveMarketRanking?.score?.take(4).orEmpty() }
            .ifEmpty { analytics.liveMarketRanking?.dividendYield?.take(4).orEmpty() }
    }
    val rankingsSourceLabel = remember(analytics.portfolioRanking, analytics.liveMarketRanking) {
        when {
            analytics.portfolioRanking != null -> "Ranking da carteira"
            analytics.liveMarketRanking != null -> "Ranking do mercado"
            else -> "Aguardando dados"
        }
    }

    // Intercept physical Back presses to return to listing
    BackHandler(enabled = activeDetailPage != null) {
        activeDetailPage = null
    }

    // A refatoração da página de Proventos/Agenda não pode depender apenas do carregamento
    // automático em background. Ao abrir essas páginas, force uma sincronização leve com o
    // VALORAE Proxy para garantir que `portfolio/next-dividends`, `portfolio/dividends`
    // e os fallbacks por ativo sejam consultados antes de declarar estado vazio.
    LaunchedEffect(activeDetailPage, summaries.size, allTransactions.size) {
        if ((activeDetailPage == "Agenda" || activeDetailPage == "Proventos") && summaries.isNotEmpty()) {
            viewModel.refreshPortfolioAnalytics(force = false)
        }
    }

    // A página Insights é composta apenas quando a aba é aberta. Por isso ela precisa
    // disparar uma sincronização própria, além do pré-aquecimento do ViewModel. Sem isso,
    // uma carteira recém-importada/adicionada podia aparecer no Dashboard enquanto os cards
    // de Insights continuavam com estado anterior ou apenas rankings de mercado.
    val insightsPortfolioSignature = remember(summaries, allTransactions) {
        val assets = summaries.joinToString("|") { item ->
            "${item.ticker}:${item.sharesCount}:${item.averageCost}:${item.currentPrice}:${item.totalInvested}"
        }
        val txs = allTransactions.maxOfOrNull { it.date } ?: 0L
        "$assets#tx=${allTransactions.size}#max=$txs"
    }
    LaunchedEffect(insightsPortfolioSignature) {
        if (summaries.isNotEmpty()) {
            val needsHardRefresh = analytics.analysis == null || analytics.lastUpdated == 0L
            viewModel.refreshPortfolioAnalytics(force = needsHardRefresh)
        }
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
                    description = "Proventos recebidos e previstos por mês, usando apenas eventos confirmados quando disponíveis.",
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
                    description = "Sua carteira comparada com IPCA quando disponível, com fallback transparente local.",
                    subStats = "Ganho Real Líquido: ${String.format("%+.2f%%", portReturnPct - currentIpcaAccumulated)}",
                    icon = Icons.Outlined.QueryStats,
                    onClick = { activeDetailPage = "IPCA+" }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        CustomLineChartCompare(
                            portfolioValues = portDataValues,
                            ipcaValues = resampleInsightSeries(ipcaDataValues, portDataValues.size.coerceAtLeast(2)),
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val allocatedPercent = alocacaoData.sumOf { it.second.toDouble() }.coerceAtMost(100.0)
                        
                        PieChart(
                            data = alocacaoData,
                            colors = listOf(GoldPrimary, GoldPale, GoldSecondary, GoldBronze, GoldTertiary),
                            centerText = "Carteira",
                            centerSubtext = "${allocatedPercent.toInt()}% Alocados",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 4. Agenda de Dividendos Preview
            item {
                val agendaPreviewEvents = remember(analytics.dividendEvents, allTransactions) {
                    agendaDividendEvents(analytics.dividendEvents, allTransactions)
                        .ifEmpty { analytics.dividendEvents.sortedByDescending { eventRelevantMillis(it) }.take(6) }
                }
                val agendaPreviewAmount = remember(agendaPreviewEvents, allTransactions) {
                    agendaPreviewEvents.sumOf { eligibleDividendAmount(it, allTransactions).takeIf { amount -> amount > 0.0 } ?: safeDividendAmount(it) }
                }
                
                ChartCard(
                    title = "Agenda de Dividendos",
                    description = "Próximos pagamentos previstos em agenda pública ou provisionados.",
                    subStats = "Valor Confirmado: R$ ${String.format("%,.2f", agendaPreviewAmount)}",
                    icon = Icons.Default.DateRange,
                    onClick = { activeDetailPage = "Agenda" }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        val upcoming = agendaPreviewEvents.take(3)
                        if (upcoming.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .background(DarkBackground, RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(vertical = 6.dp)
                            ) {
                                upcoming.forEachIndexed { index, ev ->
                                    val amountStr = String.format("R$ %.2f", eligibleDividendAmount(ev, allTransactions).takeIf { it > 0.0 } ?: safeDividendAmount(ev))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(6.dp).background(GoldPrimary, CircleShape))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(ev.ticker, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                ev.paymentDate.takeIf { it.isNotBlank() } ?: ev.dateCom,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(amountStr, color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    }
                                    if (index < upcoming.lastIndex) {
                                        HorizontalDivider(color = BorderColor.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Nenhum evento registrado", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            
            // Rankings agora têm página própria na barra inferior.

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        activeDetailPage?.let { detailPage ->
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
                        pageName = detailPage,
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
                    text = "Fonte de dados: VALORAE", 
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
    val showList = remember(agenda, limit) {
        val todayStart = startOfInsightDayMillis(System.currentTimeMillis())
        agenda
            .map { asset ->
                val eventMillis = parseInsightDateMillis(asset.nextEarningsDate)
                // Datas antigas não devem refletir como se fossem no passado.
                // Limpamos para cair na estimativa local "Breve".
                if (eventMillis > 0L && eventMillis < todayStart) {
                    asset.copy(nextEarningsDate = "")
                } else {
                    asset
                }
            }
            .take(limit)
    }
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
    
    Column(modifier = Modifier.fillMaxWidth()) {
        showList.forEachIndexed { index, asset ->
            val tickerTxs = remember(transactions, asset.ticker) {
                transactions.filter { it.ticker.uppercase() == asset.ticker.uppercase() }
            }

            val parsedComDateMillis = parseInsightDateMillis(asset.nextEarningsDate)
            val comDateMillis: Long? = parsedComDateMillis.takeIf { it > 0L }

            // Check how many shares the user had at Data Com
            val sharesAtDataCom = remember(tickerTxs, comDateMillis) {
                if (comDateMillis == null) asset.sharesCount // Fallback to current if date unknown
                else {
                    val endOfComDayMillis = comDateMillis + (24 * 60 * 60 * 1000 - 1000)
                    var count = 0.0
                    tickerTxs.filter { it.date <= endOfComDayMillis }.forEach {
                        if (it.isSell) count -= it.quantity else count += it.quantity
                    }
                    count
                }
            }

            val calculatedAmt = sharesAtDataCom * asset.lastDividend
            val divAmt = calculatedAmt
            val isEligible = sharesAtDataCom > 0.0001
            
            val comDateStr = if (asset.nextEarningsDate.isNotBlank()) asset.nextEarningsDate else "Breve"
            
            // Parse real day and month from the supported date formats.
            var displayDay = ""
            var displayMonth = ""
            if (parsedComDateMillis > 0L) {
                val date = java.util.Date(parsedComDateMillis)
                displayDay = java.text.SimpleDateFormat("dd", java.util.Locale("pt", "BR")).format(date)
                displayMonth = java.text.SimpleDateFormat("MMM", java.util.Locale("pt", "BR"))
                    .format(date)
                    .take(3)
                    .uppercase(java.util.Locale("pt", "BR"))
            }
            
            // Fallback for fallback/local assets: no speculative/fake dates based on hash.
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
fun DividendEventsList(
    events: List<DividendEvent>,
    transactions: List<com.example.data.Transaction> = emptyList(),
    limit: Int = Int.MAX_VALUE
) {
    val groupedList = remember(events, transactions, limit) {
        val now = System.currentTimeMillis()
        events
            .mapNotNull { event ->
                val amount = eligibleDividendAmount(event, transactions)
                // A agenda deve aparecer mesmo quando o usuário não tinha posição na data-com.
                // Nesse caso o valor estimado fica R$ 0,00, mas o evento real do Proxy/Investidor10
                // continua visível com datas, valor por ação/cota quando houver e status de elegibilidade.
                val hasRealEventMarker = event.ticker.isNotBlank() && (event.dateCom.isNotBlank() || event.paymentDate.isNotBlank() || event.source.isNotBlank())
                if (amount <= 0.0 && event.valuePerShare <= 0.0 && event.estimatedAmount <= 0.0 && !hasRealEventMarker) null else event to amount.coerceAtLeast(0.0)
            }
            .sortedWith(
                compareBy<Pair<DividendEvent, Double>> {
                    val ts = eventRelevantMillis(it.first)
                    if (!isPaidDividendEvent(it.first, now) && ts > 0L) 0 else 1
                }.thenBy { eventRelevantMillis(it.first).takeIf { ts -> ts > 0L } ?: Long.MAX_VALUE }
                    .thenBy { it.first.ticker }
            ).take(limit).groupBy {
               val ts = eventRelevantMillis(it.first)
               if (ts <= 0L) "A confirmar" else {
                   val cal = java.util.Calendar.getInstance()
                   cal.timeInMillis = ts
                   val monthNames = arrayOf("Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")
                   "${monthNames[cal.get(java.util.Calendar.MONTH)]} ${cal.get(java.util.Calendar.YEAR)}"
               }
            }
    }
    if (groupedList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Sem eventos confirmados pelo VALORAE Proxy para os ativos da carteira.", color = TextSecondary, fontSize = 13.sp)
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        groupedList.forEach { (monthLabel, items) ->
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = monthLabel,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "R$ ${String.format("%.2f", items.sumOf { it.second })}",
                    color = SuccessGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            items.forEach { (event, eligibleAmount) ->
                val payDate = event.paymentDate.ifBlank { "A confirmar" }
                val comDate = event.dateCom.ifBlank { "A confirmar" }
                val normalizedStatus = when {
                    event.paymentDate.isNotBlank() -> "Confirmado"
                    event.status.contains("provision", ignoreCase = true) || event.status.contains("anunci", ignoreCase = true) -> "Anunciado"
                    event.status.contains("jscp", ignoreCase = true) || event.status.contains("jcp", ignoreCase = true) -> "JCP"
                    else -> event.status.ifBlank { "Anunciado" }
                }
                val eligibleShares = eventEligibilityMillis(event).takeIf { it > 0L }?.let {
                    sharesOwnedAtInsightDate(transactions, event.ticker, endOfInsightDayMillis(it)).takeIf { qty -> qty > 0.0001 } ?: event.quantity
                } ?: event.quantity
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
                                Text(normalizedStatus, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text("Data COM: $comDate · ${event.source}", color = TextSecondary, fontSize = 10.sp)
                            if (eligibleShares > 0.0) {
                                Text("Calculado para ${String.format("%.2f", eligibleShares)} cotas", color = TextSecondary.copy(alpha = 0.85f), fontSize = 9.sp)
                            } else {
                                Text("Cálculo indisponível para sua carteira.", color = WarningOrange, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (eligibleAmount > 0.0) "R$ ${String.format("%.2f", eligibleAmount)}" else "R$ --", color = if (eligibleAmount > 0.0) SuccessGreen else TextSecondary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(if (event.valuePerShare > 0.0) "R$ ${String.format("%.4f", event.valuePerShare)}/cota" else "Valor/cota a confirmar", color = TextSecondary, fontSize = 9.sp)
                        }
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
    val totalCurrent = if (summaryModel.totalCurrentValue.isNaN() || summaryModel.totalCurrentValue.isInfinite()) 0.0 else summaryModel.totalCurrentValue
    val avgDy = remember(summaryModel, summaries) {
        if (totalCurrent > 0) summaries.sumOf { it.totalCurrentValue * it.dividendYield } / totalCurrent else 0.0
    }
    val firstTransactionTime = remember(allTransactions) {
        allTransactions.filter { !it.isSell && it.quantity > 0.0 }.minOfOrNull { it.date }
            ?: allTransactions.minOfOrNull { it.date }
            ?: System.currentTimeMillis()
    }

    val finalMonthlyDiv = remember(analytics.dividendEvents, allTransactions, firstTransactionTime) {
        val paid = analytics.dividendEvents
            .filter { isPaidDividendEvent(it) }
            .sumOf { eligibleDividendAmount(it, allTransactions) }
        val months = monthsInSelectedPeriod("Últimos 12 meses", firstTransactionTime).coerceAtLeast(1)
        if (paid > 0.0) paid / months else 0.0
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
            val effectiveIpca = resampleInsightSeries(ipcaDataValues, portDataValues.size.coerceAtLeast(1)).lastOrNull() ?: 0f
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
        
        if (pageName == "Rankings") {
            val portfolioRanking = analytics.portfolioRanking
            val liveRanking = analytics.liveMarketRanking
            val analysis = analytics.analysis
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GoldPrimary.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Rankings de mercado são atuais/fundamentalistas e não entram no cálculo de proventos passados. Proventos, IPCA e histórico continuam limitados à existência real da carteira.",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
                // ProxyActionPlanSection removed as requested by user
                RankingSection("Carteira — Score Valorae", portfolioRanking?.score.orEmpty(), "Ainda não há ranking por score para os ativos atuais.")
                RankingSection("Carteira — Dividend Yield", portfolioRanking?.dividendYield.orEmpty(), "Sem ranking de dividend yield para os ativos atuais.")
                RankingSection("Carteira — Perfil Conservador", portfolioRanking?.conservative.orEmpty(), "Sem ranking conservador para a carteira atual.")
                val rendaFii = portfolioRanking?.incomeFii.orEmpty()
                if (rendaFii.isNotEmpty()) {
                    RankingSection("Carteira — Renda FII", rendaFii)
                }
                val hasLiveHighLow = liveRanking?.highs.orEmpty().isNotEmpty() || liveRanking?.lows.orEmpty().isNotEmpty()
                if (hasLiveHighLow) {
                    RankingSection("Mercado — Maiores Altas", liveRanking?.highs.orEmpty(), "Ranking ao vivo indisponível ou bloqueado pela fonte; o app mantém a carteira funcional.")
                    RankingSection("Mercado — Maiores Baixas", liveRanking?.lows.orEmpty(), "Ranking ao vivo indisponível ou bloqueado pela fonte; o app mantém a carteira funcional.")
                } else {
                    RankingSection("Mercado — Score Valorae", liveRanking?.score.orEmpty(), "Ranking de mercado indisponível no momento; a carteira continua usando cache e dados locais.")
                    RankingSection("Mercado — Dividend Yield", liveRanking?.dividendYield.orEmpty(), "Ranking de DY de mercado indisponível no momento; a carteira continua funcional.")
                }
                val warnings = (portfolioRanking?.warnings.orEmpty() + liveRanking?.warnings.orEmpty()).distinct()
                if (warnings.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(18.dp))
                            .border(1.dp, BorderColor.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Text("AVISOS DE DADOS", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        warnings.take(5).forEach { warning ->
                            Text("• $warning", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(bottom = 5.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // -----------------------------------------------------------------
        // PRIMARY CHART SUB-VIEW
        // -----------------------------------------------------------------
        if (pageName == "Proventos") {
            var selectedTime by remember { mutableStateOf("12 meses") }
            var selectedFilter by remember { mutableStateOf("Todos") }
            
            var selectedTimePie by remember { mutableStateOf("Últimos 12 meses") }
            var selectedFilterPie by remember { mutableStateOf("Todos") }

            val dynamicStackedData = remember(summaries, analytics.dividendEvents, selectedTime, selectedFilter, allTransactions, firstTransactionTime) {
                val filteredAssets = when (selectedFilter) {
                    "Apenas FIIs" -> summaries.filter { it.type.equals("FII", ignoreCase = true) }
                    "Apenas Ações" -> summaries.filter { it.type.equals("ACAO", ignoreCase = true) }
                    else -> summaries
                }
                val filteredTickers = filteredAssets.map { it.ticker.uppercase() }.toSet()
                val filteredEvents = analytics.dividendEvents.filter { it.ticker.uppercase() in filteredTickers }
                var totalMonthlyFilteredDiv = filteredAssets.sumOf {
                    it.sharesCount * (it.currentPrice * (it.dividendYield / 100.0) / 12.0)
                }
                val barCount = when (selectedTime) {
                    "Todo o período" -> portfolioAgeMonthsForInsights(firstTransactionTime).coerceAtLeast(3)
                    "6 meses" -> 6
                    "24 meses" -> 24
                    else -> 12
                }
                buildDividendEvolutionData(
                    events = filteredEvents,
                    summaries = filteredAssets,
                    transactions = allTransactions,
                    firstTransactionTime = firstTransactionTime,
                    months = barCount,
                    fallbackMonthly = totalMonthlyFilteredDiv
                )
            }

            val dynamicTopDividendAssets = remember(summaries, analytics.dividendEvents, selectedTimePie, selectedFilterPie, allTransactions, firstTransactionTime) {
                val filteredAssets = when (selectedFilterPie) {
                    "FIIs" -> summaries.filter { it.type.equals("FII", ignoreCase = true) }
                    "Ações" -> summaries.filter { it.type.equals("ACAO", ignoreCase = true) }
                    else -> summaries
                }
                buildTopDividendAssetsForPeriod(
                    events = analytics.dividendEvents,
                    summaries = filteredAssets,
                    transactions = allTransactions,
                    periodLabel = selectedTimePie,
                    firstTransactionTime = firstTransactionTime
                )
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
                            listOf("6 meses", "12 meses", "24 meses", "Todo o período").forEach {
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
                                listOf("Desde o início", "Últimos 12 meses", "Neste ano", "Últimos 6 meses").forEach {
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
                                            modifier = Modifier.fillMaxWidth((pct / 100f).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(pieColors[index % pieColors.size])
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
                                    ipcaValues = resampleInsightSeries(ipcaDataValues, portDataValues.size.coerceAtLeast(2)),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = "Fonte: ${analytics.source.replace("Serviço de dados VALORAE", "dados VALORAE")} · IPCA: ${analytics.ipcaSeries.lastOrNull()?.source?.replace("Serviço de dados VALORAE", "dados VALORAE") ?: "estimativa local"}",
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

                            // 2. Class Allocation (Clean Summary Cards)
                            Text(
                                text = "ALOCAÇÃO POR CLASSE",
                                color = GoldPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val alocList = alocacaoData.filter { it.first != "Sem Cotas" }
                                if (alocList.isEmpty()) {
                                    Text("Nenhum ativo alocado", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                                } else {
                                    alocList.forEach { (label, percent) ->
                                        Surface(
                                            color = DarkSurfaceElevated,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.05f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp), 
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("${String.format("%,.1f", percent)}%", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Sector Breakdown (Clean List)
                            Text(
                                text = "EXPOSIÇÃO SETORIAL",
                                color = GoldPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.05f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    val sectorsList = segmentosData.filter { it.first != "Sem Cotas" }.sortedByDescending { it.second }
                                    if (sectorsList.isEmpty()) {
                                        Text("Sem dados de setor", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                                    } else {
                                        sectorsList.forEachIndexed { index, data ->
                                            val (sector, percent) = data
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(6.dp).background(GoldSecondary, CircleShape))
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(sector, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Text("${String.format("%,.1f", percent)}%", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                            }
                                            if (index < sectorsList.lastIndex) {
                                                HorizontalDivider(color = BorderColor.copy(alpha = 0.03f))
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Detailed Asset Weights (Top Holdings Clean List)
                            Text(
                                text = "CONCENTRAÇÃO POR ATIVO (TOP 10)",
                                color = GoldPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            val topAssets = summaries.sortedByDescending { it.totalCurrentValue }.take(10)
                            if (topAssets.isNotEmpty()) {
                                Surface(
                                    color = DarkSurfaceElevated,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.05f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        topAssets.forEachIndexed { index, asset ->
                                            val weight = if (totalCurrent > 0) (asset.totalCurrentValue / totalCurrent * 100.0).toFloat() else 0f
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(asset.ticker, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Text("${String.format("%,.1f", weight)}%", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                            }
                                            if (index < topAssets.lastIndex) {
                                                HorizontalDivider(color = BorderColor.copy(alpha = 0.03f))
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("Nenhum ativo alocado", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                    "Agenda" -> {
                        val agendaEvents = remember(analytics.dividendEvents, allTransactions) {
                            agendaDividendEvents(analytics.dividendEvents, allTransactions)
                        }
                        if (agendaEvents.isNotEmpty()) {
                            DividendEventsList(agendaEvents, allTransactions)
                        } else if (analytics.dividendEvents.isNotEmpty()) {
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Nenhum pagamento futuro encontrado. Exibindo histórico disponível do VALORAE Proxy.",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    DividendEventsList(analytics.dividendEvents, allTransactions, limit = 20)
                                }
                            }
                        } else {
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = if (analytics.isLoading) "Consultando agenda no VALORAE Proxy..." else "Nenhum evento de dividendos encontrado para este período.",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    if (!analytics.isLoading) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Verifique se há ativos cadastrados na carteira e toque em atualizar. A agenda aceita eventos futuros e histórico do Proxy.",
                                            color = TextSecondary.copy(alpha = 0.8f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
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
                
                val avgAssetPrice = summaries
                    .mapNotNull { (it.averageCost.takeIf { price -> price.isFinite() && price > 0.0 } ?: it.currentPrice.takeIf { price -> price.isFinite() && price > 0.0 }) }
                    .takeIf { it.isNotEmpty() }
                    ?.average()
                    ?: 50.0
                val snowballProgress = if (avgAssetPrice > 0.0 && finalMonthlyDiv.isFinite()) {
                    (finalMonthlyDiv / avgAssetPrice).coerceIn(0.0, 1.1).toFloat()
                } else 0f
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape).background(BorderColor.copy(alpha = 0.1f))) {
                        Box(modifier = Modifier.fillMaxWidth(snowballProgress.coerceIn(0f, 1f)).fillMaxHeight().background(TextPrimary))
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
                        val r = (avgDy.coerceIn(0.0, 30.0) / 100.0) + 0.05
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
                        val r = (avgDy.coerceIn(0.0, 30.0) / 100.0) + 0.05
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
        if (pageName != "Proventos" && pageName != "Agenda") {
            val kpiData = remember(pageName, summaryModel, summaries, analytics, allTransactions) {
                getKpiMetricsForPage(pageName, summaries, summaryModel, analytics, allTransactions)
            }
            KpiRow(metrics = kpiData)
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        if (pageName != "Agenda") {
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
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
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
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(GoldPrimary))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Recebidos", color = TextPrimary, fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                                    com.example.ui.components.StackedBarChart(
                                        data = divStackedDataValues,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "MÉTRICAS ATUAIS",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                TableHeaderRow(headers = listOf("Ativo", "Qtd. Cotas", "DY Anual", "Val/Mês (Est.)"))
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
                                val alignedIpcaTable = resampleInsightSeries(ipcaDataValues, portDataValues.size.coerceAtLeast(2))
                                val maxRows = portDataValues.size.coerceAtMost(24)
                                if (maxRows == 0) {
                                    TableEmptyState()
                                } else {
                                    repeat(maxRows) { i ->
                                        val ipcaVal = alignedIpcaTable.getOrNull(i) ?: 0f
                                        val portfolioVal = portDataValues.getOrNull(i) ?: 0f
                                        val realReturn = portfolioVal - ipcaVal
                                        val label = analytics.portfolioHistory.getOrNull(i)?.dateLabel ?: analytics.ipcaSeries.getOrNull(i)?.dateLabel ?: "Mês ${String.format("%02d", i + 1)}"
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
                            "Rankings" -> {
                                val rows = analytics.portfolioRanking?.score.orEmpty()
                                    .ifEmpty { analytics.portfolioRanking?.dividendYield.orEmpty() }
                                    .ifEmpty { analytics.liveMarketRanking?.highs.orEmpty() }
                                    .ifEmpty { analytics.liveMarketRanking?.score.orEmpty() }
                                    .ifEmpty { analytics.liveMarketRanking?.dividendYield.orEmpty() }
                                TableHeaderRow(listOf("Rank", "Ativo", "Valor", "Origem"))
                                if (rows.isEmpty()) {
                                    TableEmptyState()
                                } else {
                                    rows.take(12).forEach { item ->
                                        TableDataRow(
                                            items = listOf(
                                                if (item.rank > 0) "#${item.rank}" else "-",
                                                item.ticker,
                                                item.displayValue.ifBlank { if (item.value != 0.0) String.format("%.2f", item.value) else item.grade.ifBlank { item.direction } },
                                                item.source
                                            ),
                                            highlightIndex = 2
                                        )
                                    }
                                }
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
fun RankingCompactRow(item: MarketRankingItem) {
    val isUp = item.direction.equals("alta", true) || item.changePercent > 0.0
    val isDown = item.direction.equals("baixa", true) || item.changePercent < 0.0
    val badgeColor = when {
        isDown -> DangerRed.copy(alpha = 0.12f)
        isUp -> SuccessGreen.copy(alpha = 0.12f)
        else -> GoldPrimary.copy(alpha = 0.08f)
    }
    val textColor = when {
        isDown -> DangerRed
        isUp -> SuccessGreen
        else -> GoldPrimary
    }

    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.04f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Rank, Ticker & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldPrimary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.rank > 0) "#${item.rank}" else "•",
                            color = GoldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = item.ticker,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.name.isNotBlank()) {
                            Text(
                                text = item.name,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Right: Value (Variation/Grade) & Price
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val displayVal = item.displayValue.ifBlank {
                        when {
                            item.changeDisplay.isNotBlank() -> item.changeDisplay
                            item.changePercent != 0.0 -> {
                                val prefix = if (item.changePercent > 0.0) "+" else ""
                                String.format(java.util.Locale("pt", "BR"), "%s%.2f%%", prefix, item.changePercent)
                            }
                            item.value.isFinite() && item.value != 0.0 -> String.format("%.2f", item.value)
                            item.grade.isNotBlank() -> item.grade
                            else -> item.direction
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = displayVal,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    val formattedPrice = item.priceDisplay.ifBlank {
                        if (item.price > 0.0) String.format(java.util.Locale("pt", "BR"), "R$ %.2f", item.price) else ""
                    }
                    if (formattedPrice.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedPrice,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // Footer Info: Explanation, Source or Type label
            val hasExplanation = item.explanation.isNotBlank()
            val hasSource = item.source.isNotBlank() && item.source != "Serviço de dados VALORAE" && item.source != "VALORAE Proxy"
            val showFooter = hasExplanation || hasSource || isUp || isDown
            
            if (showFooter) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.04f))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val typeLabel = when {
                        isUp -> "Alta"
                        isDown -> "Baixa"
                        else -> "Neutro"
                    }
                    
                    Text(
                        text = "Tipo: $typeLabel",
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (hasSource) {
                        Text(
                            text = "Fonte: ${item.source}",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        )
                    } else if (hasExplanation) {
                        Text(
                            text = item.explanation,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RankingSection(title: String, items: List<MarketRankingItem>, emptyMessage: String = "Sem dados disponíveis") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(18.dp))
            .border(1.dp, BorderColor.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title.uppercase(),
            color = GoldPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (items.isEmpty()) {
            Text(emptyMessage, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.take(8).forEach { RankingCompactRow(it) }
            }
        }
    }
}

@Composable
fun ProxyActionPlanSection(analysis: com.example.network.PortfolioProxyAnalysis?) {
    val actions = analysis?.actionPlan.orEmpty()
    val ranking = analysis?.positionRanking.orEmpty()
    if (actions.isEmpty() && ranking.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(18.dp))
            .border(1.dp, BorderColor.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text("INTELIGÊNCIA DA CARTEIRA", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(10.dp))
        actions.take(4).forEach { action ->
            Text(
                text = "• ${action.message}",
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        if (ranking.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))
            ranking.take(5).forEach { item ->
                val scoreText = String.format("%.0f", item.score)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#${item.rank} ${item.ticker}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Score $scoreText", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (item.reason.isNotBlank()) Text(item.reason, color = TextSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
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
// INSIGHTS DATA HELPERS
// -------------------------------------------------------------
private fun safeDividendAmount(event: DividendEvent): Double {
    return when {
        event.estimatedAmount > 0.0 -> event.estimatedAmount
        event.valuePerShare > 0.0 && event.quantity > 0.0 -> event.valuePerShare * event.quantity
        event.valuePerShare > 0.0 -> event.valuePerShare
        else -> 0.0
    }
}

private fun normalizeInsightPercentPairs(raw: List<Pair<String, Float>>): List<Pair<String, Float>> {
    val cleaned = raw
        .mapNotNull { (label, value) ->
            val safe = value.takeIf { it.isFinite() && it > 0f } ?: return@mapNotNull null
            label.trim().ifBlank { "Outros" } to safe
        }
    if (cleaned.isEmpty()) return emptyList()
    val sum = cleaned.sumOf { it.second.toDouble() }.toFloat()
    val max = cleaned.maxOf { it.second }
    val scaled = when {
        max <= 1.5f && sum <= 1.5f -> cleaned.map { it.first to it.second * 100f }
        sum > 100.5f -> cleaned.map { it.first to (it.second / sum * 100f) }
        else -> cleaned
    }
    return scaled
        .filter { it.second.isFinite() && it.second > 0f }
        .sortedByDescending { it.second }
}

private fun resampleInsightSeries(values: List<Float>, targetSize: Int): List<Float> {
    val clean = values.filter { it.isFinite() }
    if (targetSize <= 0) return emptyList()
    if (clean.isEmpty()) return List(targetSize) { 0f }
    if (clean.size == targetSize) return clean
    if (clean.size == 1) return List(targetSize) { clean.first() }
    return List(targetSize) { index ->
        val sourceIndex = if (targetSize == 1) 0.0 else index.toDouble() * (clean.size - 1).toDouble() / (targetSize - 1).toDouble()
        val lower = kotlin.math.floor(sourceIndex).toInt().coerceIn(0, clean.lastIndex)
        val upper = kotlin.math.ceil(sourceIndex).toInt().coerceIn(0, clean.lastIndex)
        if (lower == upper) clean[lower] else {
            val t = (sourceIndex - lower).toFloat().coerceIn(0f, 1f)
            clean[lower] + ((clean[upper] - clean[lower]) * t)
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
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "dd-MM-yyyy",
        "MM/yyyy",
        "yyyy-MM"
    )
    for (pattern in patterns) {
        try {
            val locale = if (pattern.startsWith("yyyy")) java.util.Locale.US else java.util.Locale("pt", "BR")
            val sdf = java.text.SimpleDateFormat(pattern, locale)
            sdf.isLenient = false
            if (pattern.contains("'Z'") || pattern.contains("XXX")) {
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            return sdf.parse(normalized)?.time ?: 0L
        } catch (_: Exception) {}
    }
    return 0L
}

private fun portfolioAgeMonthsForInsights(firstTransactionMillis: Long): Int {
    val start = java.util.Calendar.getInstance().apply {
        timeInMillis = firstTransactionMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val now = java.util.Calendar.getInstance()
    val months = (now.get(java.util.Calendar.YEAR) - start.get(java.util.Calendar.YEAR)) * 12 +
        (now.get(java.util.Calendar.MONTH) - start.get(java.util.Calendar.MONTH)) + 1
    return months.coerceIn(1, 120)
}

private fun startOfInsightDayMillis(millis: Long): Long {
    if (millis <= 0L) return 0L
    return java.util.Calendar.getInstance().apply {
        timeInMillis = millis
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun endOfInsightDayMillis(millis: Long): Long {
    if (millis <= 0L) return 0L
    return java.util.Calendar.getInstance().apply {
        timeInMillis = millis
        set(java.util.Calendar.HOUR_OF_DAY, 23)
        set(java.util.Calendar.MINUTE, 59)
        set(java.util.Calendar.SECOND, 59)
        set(java.util.Calendar.MILLISECOND, 999)
    }.timeInMillis
}

private fun eventRelevantMillis(event: DividendEvent): Long {
    return parseInsightDateMillis(event.paymentDate).takeIf { it > 0L }
        ?: parseInsightDateMillis(event.dateCom).takeIf { it > 0L }
        ?: 0L
}

private fun eventEligibilityMillis(event: DividendEvent): Long {
    return parseInsightDateMillis(event.dateCom).takeIf { it > 0L }
        ?: parseInsightDateMillis(event.paymentDate).takeIf { it > 0L }
        ?: 0L
}

private fun eventMonthLabel(event: DividendEvent): String {
    val ts = eventRelevantMillis(event)
    if (ts <= 0L) return event.ticker.take(5)
    return java.text.SimpleDateFormat("MM/yyyy", java.util.Locale("pt", "BR")).format(java.util.Date(ts))
}

private fun insightMonthIndex(millis: Long): Int {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    return cal.get(java.util.Calendar.YEAR) * 12 + cal.get(java.util.Calendar.MONTH)
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
    event: DividendEvent,
    transactions: List<com.example.data.Transaction>
): Double {
    if (transactions.isEmpty()) return safeDividendAmount(event)

    val eligibilityTs = eventEligibilityMillis(event)
    val relevantTs = eventRelevantMillis(event)
    val todayStart = startOfInsightDayMillis(System.currentTimeMillis())
    val isPastPayment = relevantTs > 0L && relevantTs < todayStart
    val fallbackTs = if (eligibilityTs > 0L) eligibilityTs else if (relevantTs > 0L) relevantTs else System.currentTimeMillis()

    val shares = sharesOwnedAtInsightDate(transactions, event.ticker, endOfInsightDayMillis(fallbackTs))

    // Para eventos futuros/anunciados, usa a quantidade atual injetada pelo ViewModel como projeção.
    // Para histórico pago, NÃO usa quantidade atual quando não há posição na data-com/pagamento,
    // evitando criar proventos retroativos falsos.
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

private fun isPaidDividendEvent(event: DividendEvent, nowMillis: Long = System.currentTimeMillis()): Boolean {
    val status = event.status.lowercase(java.util.Locale.ROOT)
    val ts = eventRelevantMillis(event)
    val todayStart = startOfInsightDayMillis(nowMillis)
    return status.contains("pago") || status.contains("recebido") || status.contains("último") || status.contains("ultimo") || (ts > 0L && ts < todayStart)
}

private fun monthEndMillis(calendar: java.util.Calendar): Long {
    return (calendar.clone() as java.util.Calendar).apply {
        set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        set(java.util.Calendar.HOUR_OF_DAY, 23)
        set(java.util.Calendar.MINUTE, 59)
        set(java.util.Calendar.SECOND, 59)
        set(java.util.Calendar.MILLISECOND, 999)
    }.timeInMillis
}

private fun monthlyDividendEstimateForMonth(
    summaries: List<AssetSummary>,
    transactions: List<com.example.data.Transaction>,
    monthEndMillis: Long,
    fallbackMonthly: Double
): Double {
    val local = summaries.sumOf { asset ->
        val qty = if (transactions.isNotEmpty()) {
            sharesOwnedAtInsightDate(transactions, asset.ticker, monthEndMillis)
        } else {
            asset.sharesCount
        }
        qty.coerceAtLeast(0.0) * (asset.currentPrice * (asset.dividendYield / 100.0) / 12.0)
    }
    return local.takeIf { it.isFinite() && it > 0.0 } ?: fallbackMonthly.coerceAtLeast(0.0)
}

private fun upcomingEligibleDividendEvents(
    events: List<DividendEvent>,
    transactions: List<com.example.data.Transaction>,
    nowMillis: Long = System.currentTimeMillis()
): List<DividendEvent> {
    val todayStart = startOfInsightDayMillis(nowMillis)
    return events
        .asSequence()
        .filter { event ->
            val relevant = eventRelevantMillis(event)
            relevant >= todayStart &&
                !isPaidDividendEvent(event, nowMillis) &&
                (eligibleDividendAmount(event, transactions) > 0.0 || event.valuePerShare > 0.0 || event.estimatedAmount > 0.0)
        }
        .sortedWith(compareBy<DividendEvent> {
            eventRelevantMillis(it).let { ts -> if (ts > 0L) ts else Long.MAX_VALUE }
        }.thenBy { it.ticker })
        .toList()
}

private fun agendaDividendEvents(
    events: List<DividendEvent>,
    transactions: List<com.example.data.Transaction>,
    nowMillis: Long = System.currentTimeMillis()
): List<DividendEvent> {
    val todayStart = startOfInsightDayMillis(nowMillis)
    return events
        .asSequence()
        .filter { event ->
            val relevant = eventRelevantMillis(event)
            val status = event.status.lowercase(java.util.Locale.ROOT)
            val isFutureLike = status.contains("prev") || status.contains("futur") || status.contains("agenda") ||
                status.contains("confirm") || status.contains("jscp") || status.contains("jcp") ||
                status.contains("dividend") || status.contains("rendimento")
            val hasRealMarker = event.ticker.isNotBlank() && (event.dateCom.isNotBlank() || event.paymentDate.isNotBlank() || event.valuePerShare > 0.0 || event.estimatedAmount > 0.0)
            hasRealMarker && (
                (relevant > 0L && relevant >= todayStart && !isPaidDividendEvent(event, nowMillis)) ||
                (relevant <= 0L && isFutureLike)
            )
        }
        .sortedWith(compareBy<DividendEvent> {
            eventRelevantMillis(it).let { ts -> if (ts > 0L) ts else Long.MAX_VALUE }
        }.thenBy { it.ticker })
        .toList()
}

private fun buildDividendEvolutionData(
    events: List<DividendEvent>,
    summaries: List<AssetSummary>,
    transactions: List<com.example.data.Transaction> = emptyList(),
    firstTransactionTime: Long,
    months: Int,
    fallbackMonthly: Double
): List<com.example.ui.components.StackedBarData> {
    // Evolução de Proventos: counting backwards from current month so it's always populated.
    val safeMonths = months.coerceIn(3, 120)
    val start = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        // Go back (safeMonths - 1) months
        add(java.util.Calendar.MONTH, -(safeMonths - 1))
    }
    val currentTickers = summaries.map { it.ticker.trim().uppercase(java.util.Locale.ROOT) }.filter { it.isNotBlank() }.toSet()
    val lowerBoundTime = start.timeInMillis

    val eventsByMonth = events
        .asSequence()
        .filter { event ->
            val ticker = event.ticker.trim().uppercase(java.util.Locale.ROOT)
            ticker.isNotBlank() && (currentTickers.isEmpty() || ticker in currentTickers)
        }
        .mapNotNull { event ->
            val ts = eventRelevantMillis(event)
            val amount = eligibleDividendAmount(event, transactions)
            if (amount <= 0.0 || ts < lowerBoundTime) null else event to amount
        }
        .groupBy { (event, _) -> eventMonthLabel(event) }

    return List(safeMonths) { i ->
        val cal = start.clone() as java.util.Calendar
        cal.add(java.util.Calendar.MONTH, i)
        val monthLabel = String.format("%02d/%d", cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.YEAR))
        val bucket = eventsByMonth[monthLabel].orEmpty()
        var received = bucket.filter { isPaidDividendEvent(it.first) }.sumOf { it.second }.toFloat()
        var projected = bucket.filter { !isPaidDividendEvent(it.first) }.sumOf { it.second }.toFloat()
        
        com.example.ui.components.StackedBarData(received = received, projected = projected, label = monthLabel)
    }
}

private fun periodStartMillis(periodLabel: String, firstTransactionTime: Long): Long {
    val now = java.util.Calendar.getInstance()
    val start = when (periodLabel) {
        "Desde o início" -> if (firstTransactionTime > 0L) firstTransactionTime else (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -11); set(java.util.Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        "Últimos 6 meses" -> (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -5); set(java.util.Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        "Neste ano" -> (now.clone() as java.util.Calendar).apply { set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY); set(java.util.Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        else -> (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -11); set(java.util.Calendar.DAY_OF_MONTH, 1) }.timeInMillis
    }
    return start
}

private fun monthsInSelectedPeriod(periodLabel: String, firstTransactionTime: Long): Int {
    val start = java.util.Calendar.getInstance().apply { timeInMillis = periodStartMillis(periodLabel, firstTransactionTime) }
    val now = java.util.Calendar.getInstance()
    return ((now.get(java.util.Calendar.YEAR) - start.get(java.util.Calendar.YEAR)) * 12 +
        (now.get(java.util.Calendar.MONTH) - start.get(java.util.Calendar.MONTH)) + 1).coerceAtLeast(3)
}

private fun monthStartForInsight(millis: Long): java.util.Calendar {
    return java.util.Calendar.getInstance().apply {
        timeInMillis = millis
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
}

private fun buildTopDividendAssetsForPeriod(
    events: List<DividendEvent>,
    summaries: List<AssetSummary>,
    transactions: List<com.example.data.Transaction>,
    periodLabel: String,
    firstTransactionTime: Long
): List<Pair<String, Float>> {
    if (summaries.isEmpty()) return emptyList()
    val start = periodStartMillis(periodLabel, firstTransactionTime)
    val end = System.currentTimeMillis()
    val allowedTickers = summaries.map { it.ticker.trim().uppercase(java.util.Locale.ROOT) }.toSet()
    val fromEvents = events
        .asSequence()
        .filter { it.ticker.trim().uppercase(java.util.Locale.ROOT) in allowedTickers }
        .mapNotNull { event ->
            val ts = eventRelevantMillis(event)
            if (ts <= 0L || ts < start || ts > end) return@mapNotNull null
            val amount = eligibleDividendAmount(event, transactions)
            if (amount > 0.0) event.ticker.trim().uppercase(java.util.Locale.ROOT) to amount else null
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, values) -> values.sum() }
        .filterValues { it > 0.0 }
    if (fromEvents.isNotEmpty()) {
        return fromEvents.entries.sortedByDescending { it.value }.map { it.key to it.value.toFloat() }
    }

    // Sem eventos confirmados não cria ranking estimado: evita mostrar valores simulados
    // como se fossem proventos recebidos desde a criação da carteira.
    return emptyList()
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
        "Rankings" -> "Rankings do mercado e da sua carteira com dados de mercado"
        else -> ""
    }
}

fun getKpiMetricsForPage(
    page: String, 
    summaries: List<AssetSummary>, 
    summaryModel: com.example.viewmodel.PortfolioSummary,
    analytics: PortfolioAnalyticsState,
    transactions: List<com.example.data.Transaction> = emptyList()
): List<Triple<String, String, String>> {
    val totalCurrent = if (summaryModel.totalCurrentValue.isNaN() || summaryModel.totalCurrentValue.isInfinite()) 0.0 else summaryModel.totalCurrentValue
    val avgDy = if (totalCurrent > 0) summaries.sumOf { it.totalCurrentValue * it.dividendYield } / totalCurrent else 0.0
    val paidDividendAmounts = analytics.dividendEvents
        .filter { isPaidDividendEvent(it) }
        .map { eligibleDividendAmount(it, transactions) }
        .filter { it > 0.0 }
    val confirmedMonthlyAverage = if (paidDividendAmounts.isNotEmpty()) paidDividendAmounts.sum() / monthsInSelectedPeriod("Últimos 12 meses", transactions.filter { !it.isSell && it.quantity > 0.0 }.minOfOrNull { it.date } ?: transactions.minOfOrNull { it.date } ?: System.currentTimeMillis()).coerceAtLeast(1) else 0.0

    return when (page) {
        "Proventos" -> listOf(
            Triple("Yield Médio", String.format("%.2f%%", avgDy), "GOLD"),
            Triple("Média Confirmada", String.format("R$ %.2f", confirmedMonthlyAverage), "GREEN"),
            Triple("Eventos", "${paidDividendAmounts.size}", "GOLD")
        )
        "IPCA+" -> {
            val portReturn = summaryModel.returnPercent
            val ipca = analytics.ipcaSeries.lastOrNull()?.accumulatedPercent ?: 5.5
            val realReturn = portReturn - ipca
            listOf(
                Triple("Carteira Acum.", String.format("%+.2f%%", portReturn), if (portReturn >= 0) "GREEN" else "RED"),
                Triple("IPCA Período", String.format("%+.2f%%", ipca), "GOLD"),
                Triple("Ganho Real", String.format("%+.2f%%", realReturn), if (realReturn >= 0) "GREEN" else "RED")
            )
        }
        "Diversificação" -> {
            val remoteSectors = analytics.analysis?.allocationBySector.orEmpty().filter { it.second > 0.0 }
            val sectorsCount = if (remoteSectors.isNotEmpty()) {
                remoteSectors.size
            } else {
                summaries.map { it.type.ifBlank { it.ticker.take(4) } }.distinct().size
            }
            listOf(
                Triple("Setores", "$sectorsCount", "GOLD"),
                Triple("Ações Peso", "%.1f%%".format(summaryModel.sharesRatioStock * 100), "GREEN"),
                Triple("Total Alocado", String.format("R$ %.0f", totalCurrent), "GOLD")
            )
        }
        "Agenda" -> {
            val events = agendaDividendEvents(analytics.dividendEvents, transactions)
            val sumDY = events.sumOf { eligibleDividendAmount(it, transactions) }
            
            listOf(
                Triple("Eventos", "${events.size} un", "GOLD"),
                Triple("Valor Confirmado", String.format("R$ %.2f", sumDY), "GREEN"),
                Triple("Fonte", if (events.isNotEmpty()) "Confirmada" else "Sem previsão", "GOLD")
            )
        }
        "Rankings" -> {
            val portfolioCount = analytics.portfolioRanking?.score?.size ?: 0
            val marketCount = (analytics.liveMarketRanking?.highs?.size ?: 0) + (analytics.liveMarketRanking?.lows?.size ?: 0)
            val health = analytics.analysis?.healthScore ?: analytics.analysis?.score ?: 0.0
            listOf(
                Triple("Carteira", if (portfolioCount > 0) "$portfolioCount ranks" else "sem ranking", "GOLD"),
                Triple("Mercado", if (marketCount > 0) "$marketCount itens" else "indisp.", "GOLD"),
                Triple("Score", if (health > 0.0) String.format("%.0f", health) else "--", if (health >= 70.0) "GREEN" else "GOLD")
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
        "Rankings" -> "Use rankings como triagem e comparação. Eles são atuais e fundamentalistas; não substituem a linha do tempo real da carteira nem autorizam lançar dividendos antes da existência da posição."
        else -> ""
    }
}
