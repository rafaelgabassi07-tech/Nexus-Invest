package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.*
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AssetChartBundlePanel(
    bundle: AssetChartBundle,
    isFii: Boolean,
    modifier: Modifier = Modifier,
    onRangeChange: ((String) -> Unit)? = null,
    currentRange: String = "1Y"
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gráficos Avançados",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GoldPrimary
                )
                Text(
                    text = "Fonte: ${bundle.source}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            if (bundle.warnings.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Aviso",
                            tint = DangerRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dados Parciais",
                            color = DangerRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (onRangeChange != null) {
            BundleRangeSelector(
                currentRange = currentRange,
                onRangeSelected = onRangeChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
        }

        val tabTitles = if (isFii) {
            listOf("Visão Geral", "Rendimentos", "Patrimonial", "Comparação")
        } else {
            listOf("Análise", "Dividendos", "Comparação", "DRE", "Negócios")
        }
        var selectedTab by remember(bundle.ticker, bundle.range, isFii) { mutableIntStateOf(0) }
        val safeSelectedTab = selectedTab.coerceIn(0, tabTitles.lastIndex.coerceAtLeast(0))

        ScrollableTabRow(
            selectedTabIndex = safeSelectedTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = GoldPrimary,
            divider = {},
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[safeSelectedTab]),
                        color = GoldPrimary
                    )
                }
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = safeSelectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (safeSelectedTab == index) FontWeight.Black else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        AnimatedContent(
            targetState = safeSelectedTab,
            label = "AssetChartBundleTab"
        ) { tab ->
            if (isFii) {
                when (tab) {
                    0 -> FiiGeneralTab(bundle)
                    1 -> FiiDividendTab(bundle)
                    2 -> FiiPatrimonialTab(bundle)
                    else -> FiiComparisonTab(bundle)
                }
            } else {
                when (tab) {
                    0 -> StockAnalysisTab(bundle)
                    1 -> StockDividendTab(bundle)
                    2 -> StockComparisonTab(bundle)
                    3 -> StockDreTab(bundle)
                    else -> StockBusinessTab(bundle)
                }
            }
        }
    }
}

// ======================== TABS IMPLEMENTATIONS ========================

@Composable
fun StockAnalysisTab(bundle: AssetChartBundle) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Rentabilidade nominal vs real
        ChartCardContainer(title = "Rentabilidade Nominal vs Real") {
            if (bundle.profitability.isNotEmpty() || bundle.realProfitability.isNotEmpty()) {
                AssetProfitabilityChart(
                    profitability = bundle.profitability,
                    realProfitability = bundle.realProfitability,
                    modifier = Modifier.height(160.dp)
                )
            } else {
                EmptyChartState("Sem rentabilidade histórica", "Valores históricos de variação indisponíveis.")
            }
        }

        // Fundamental Indicators Selection
        ChartCardContainer(title = "Indicadores Fundamentalistas") {
            if (bundle.indicatorCards.isNotEmpty()) {
                AssetIndicatorHistoryChart(
                    cards = bundle.indicatorCards,
                    history = bundle.indicatorHistory,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                EmptyChartState("Sem indicadores", "Os indicadores estão vazios no Proxy.")
            }
        }
    }
}

@Composable
fun StockDividendTab(bundle: AssetChartBundle) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        FilteredChartCard(title = "Proventos por Ano", filterOptions = listOf("3A", "5A", "10A", "MAX"), defaultFilter = "10A") { filter ->
            val filterYears = filter.replace("A", "").toIntOrNull() ?: Int.MAX_VALUE
            val filteredPoints = bundle.dividendYearly.takeLast(filterYears)
            if (filteredPoints.isNotEmpty()) {
                AssetDividendPaidChart(
                    points = filteredPoints,
                    label = "Ano",
                    modifier = Modifier.height(150.dp)
                )
            } else {
                EmptyChartState("Sem dividendos pagos", "Nenhum histórico de ano encontrado.")
            }
        }

        FilteredChartCard(title = "Histórico de Dividend Yield", filterOptions = listOf("3A", "5A", "10A", "MAX"), defaultFilter = "10A") { filter ->
            val filterYears = filter.replace("A", "").toIntOrNull() ?: Int.MAX_VALUE
            val filteredPoints = bundle.dividendYieldHistory.takeLast(filterYears)
            if (filteredPoints.isNotEmpty()) {
                AssetDividendYieldChart(
                    points = filteredPoints,
                    modifier = Modifier.height(150.dp)
                )
            } else {
                EmptyChartState("Sem Dividend Yield", "Não foi gerada a estimativa de yield por ano.")
            }
        }

        ChartCardContainer(title = "Sazonalidade Mensal (Últimos 24m)") {
            if (bundle.dividendMonthly.isNotEmpty()) {
                AssetDividendPaidChart(
                    points = bundle.dividendMonthly,
                    label = "Mês",
                    modifier = Modifier.height(150.dp),
                    barColor = GoldPale
                )
            } else {
                EmptyChartState("Sem distribuição mensal", "Distribuições mensais recentes indisponíveis.")
            }
        }

        ChartCardContainer(title = "Eventos de Distribuição") {
            if (bundle.dividendEvents.isNotEmpty()) {
                DividendLedgerTable(bundle.dividendEvents)
            } else {
                EmptyChartState("Sem eventos", "Nenhum evento registrado de dividendo.")
            }
        }
    }
}

@Composable
fun StockComparisonTab(bundle: AssetChartBundle) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        FilteredChartCard(title = "Comparação com Índices (%)", filterOptions = listOf("1A", "3A", "5A", "MAX"), defaultFilter = "5A") { filter ->
            val filteredSeries = remember(bundle.indexComparison, filter) {
                filterComparisonSeries(bundle.indexComparison, filter)
            }
            if (filteredSeries.isNotEmpty()) {
                AssetIndexComparisonChart(
                    series = filteredSeries,
                    modifier = Modifier.height(180.dp)
                )
            } else {
                EmptyChartState("Sem séries comparativas", "O Proxy não retornou IBOV/IFIX/CDI/IPCA para este período. O app tentará novamente ao trocar o intervalo.")
            }
        }

        if (bundle.commodityComparison.isNotEmpty()) {
            FilteredChartCard(title = "Correlação com Commodities (Brent/Brent Oil)", filterOptions = listOf("1A", "3A", "5A", "MAX"), defaultFilter = "5A") { filter ->
                val filteredSeries = remember(bundle.commodityComparison, filter) {
                    filterComparisonSeries(bundle.commodityComparison, filter)
                }
                AssetIndexComparisonChart(
                    series = filteredSeries,
                    modifier = Modifier.height(180.dp)
                )
            }
        }
    }
}

@Composable
fun StockDreTab(bundle: AssetChartBundle) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        FilteredChartCard(title = "DRE: Receitas x Lucros", filterOptions = listOf("3A", "5A", "8A", "MAX"), defaultFilter = "8A") { filter ->
            val filterYears = filter.replace("A", "").toIntOrNull() ?: Int.MAX_VALUE
            val filteredPoints = bundle.revenueProfit.sortedBy { it.year }.takeLast(filterYears)
            if (filteredPoints.isNotEmpty()) {
                AssetRevenueProfitChart(
                    points = filteredPoints,
                    modifier = Modifier.height(160.dp)
                )
            } else {
                EmptyChartState("Sem DRE", "Dados financeiros históricos indisponíveis no Proxy.")
            }
        }

        FilteredChartCard(title = "Evolução Lucro x Cotação", filterOptions = listOf("3A", "5A", "10A", "MAX"), defaultFilter = "10A") { filter ->
            val filterYears = filter.replace("A", "").toIntOrNull() ?: Int.MAX_VALUE
            val filteredPoints = bundle.profitVsQuote.takeLast(filterYears)
            if (filteredPoints.isNotEmpty()) {
                AssetProfitVsQuoteChart(
                    points = filteredPoints,
                    modifier = Modifier.height(160.dp)
                )
            } else {
                EmptyChartState("Aguardando Lucro x Cotação", "Série histórica lucro contra preço indisponível.")
            }
        }

        FilteredChartCard(title = "Balanço Patrimonial: Ativo/PL/Passivo", filterOptions = listOf("3A", "5A", "8A", "MAX"), defaultFilter = "8A") { filter ->
            val filterYears = filter.replace("A", "").toIntOrNull() ?: Int.MAX_VALUE
            val filteredPoints = bundle.equityEvolution.sortedBy { it.year }.takeLast(filterYears)
            if (filteredPoints.isNotEmpty()) {
                AssetEquityEvolutionChart(
                    points = filteredPoints,
                    modifier = Modifier.height(160.dp)
                )
            } else {
                EmptyChartState("Aguardando Ativos e Patrimônio", "Visão de evolução patrimonial indisponível.")
            }
        }

        FilteredChartCard(title = "Payout Histórico (%)", filterOptions = listOf("3A", "5A", "10A", "MAX"), defaultFilter = "10A") { filter ->
            val filterYears = filter.replace("A", "").toIntOrNull() ?: Int.MAX_VALUE
            val filteredPoints = bundle.payoutHistory.takeLast(filterYears)
            if (filteredPoints.isNotEmpty()) {
                AssetPayoutHistoryChart(
                    points = filteredPoints,
                    modifier = Modifier.height(150.dp)
                )
            } else {
                EmptyChartState("Indisponível", "Histórico de payout não entregue pelo Proxy.")
            }
        }
    }
}

@Composable
fun StockBusinessTab(bundle: AssetChartBundle) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ChartCardContainer(title = "Faturamento por Negócio (%)") {
            val latestEntry = bundle.revenueByBusiness.entries
                .sortedBy { it.key }
                .lastOrNull { normalizeBreakdownPoints(it.value).isNotEmpty() }
            if (latestEntry != null) {
                AssetBreakdownDonutChart(
                    title = "Origem Faturamento (${latestEntry.key})",
                    points = latestEntry.value,
                    modifier = Modifier.height(200.dp)
                )
            } else {
                EmptyChartState("Sem divisão de negócio", "O Proxy não retornou percentuais válidos de segmentos operacionais para este ativo.")
            }
        }

        ChartCardContainer(title = "Faturamento por Região (%)") {
            val latestEntry = bundle.revenueByRegion.entries
                .sortedBy { it.key }
                .lastOrNull { normalizeBreakdownPoints(it.value).isNotEmpty() }
            if (latestEntry != null) {
                AssetBreakdownDonutChart(
                    title = "Divisão Geográfica (${latestEntry.key})",
                    points = latestEntry.value,
                    modifier = Modifier.height(200.dp)
                )
            } else {
                EmptyChartState("Sem divisão regional", "O Proxy não retornou percentuais válidos de geografia de receita para este ativo.")
            }
        }
    }
}

// ======================== FII TABS IMPLEMENTATIONS ========================

@Composable
fun FiiGeneralTab(bundle: AssetChartBundle) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ChartCardContainer(title = "Rentabilidade do FII") {
            if (bundle.profitability.isNotEmpty() || bundle.realProfitability.isNotEmpty()) {
                AssetProfitabilityChart(
                    profitability = bundle.profitability,
                    realProfitability = bundle.realProfitability,
                    modifier = Modifier.height(160.dp)
                )
            } else {
                EmptyChartState("Indisponível", "Série de rentabilidade histórica ausente.")
            }
        }

        ChartCardContainer(title = "Indicadores Fundamentalistas do FII") {
            if (bundle.indicatorCards.isNotEmpty()) {
                AssetIndicatorHistoryChart(
                    cards = bundle.indicatorCards,
                    history = bundle.indicatorHistory,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                EmptyChartState("Sem indicadores", "O Proxy ainda não retornou indicadores fundamentalistas para este FII.")
            }
        }

        ChartCardContainer(title = "Distribuições 12 Meses (Yield %)") {
            if (bundle.fiiDistribution12m.isNotEmpty()) {
                FiiDistribution12mChart(bundle.fiiDistribution12m)
            } else {
                EmptyChartState("Indisponível", "Valores de rentabilidade sobre cotas ausentes.")
            }
        }
    }
}

@Composable
fun FiiDividendTab(bundle: AssetChartBundle) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        FilteredChartCard(title = "Rendimentos Pagos por Ano", filterOptions = listOf("3A", "5A", "10A", "MAX"), defaultFilter = "10A") { filter ->
            val filterYears = filter.replace("A", "").toIntOrNull() ?: Int.MAX_VALUE
            val filteredPoints = bundle.dividendYearly.takeLast(filterYears)
            if (filteredPoints.isNotEmpty()) {
                AssetDividendPaidChart(
                    points = filteredPoints,
                    label = "Ano",
                    modifier = Modifier.height(150.dp),
                    barColor = GoldPrimary
                )
            } else {
                EmptyChartState("Sem dividendos", "Série anual ausente.")
            }
        }

        FilteredChartCard(title = "Histórico de Dividend Yield", filterOptions = listOf("3A", "5A", "10A", "MAX"), defaultFilter = "10A") { filter ->
            val filterYears = filter.replace("A", "").toIntOrNull() ?: Int.MAX_VALUE
            val filteredPoints = bundle.dividendYieldHistory.takeLast(filterYears)
            if (filteredPoints.isNotEmpty()) {
                AssetDividendYieldChart(
                    points = filteredPoints,
                    modifier = Modifier.height(150.dp)
                )
            } else {
                EmptyChartState("Sem DY histórico", "DY anual ausente.")
            }
        }

        ChartCardContainer(title = "Sazonalidade Mensal (Últimos 24m)") {
            if (bundle.dividendMonthly.isNotEmpty()) {
                AssetDividendPaidChart(
                    points = bundle.dividendMonthly,
                    label = "Mês",
                    modifier = Modifier.height(150.dp),
                    barColor = GoldPale
                )
            } else {
                EmptyChartState("Sem mensalidade", "Distribuições mensais recentes indisponíveis.")
            }
        }

        ChartCardContainer(title = "Acontecimentos e Proventos") {
            if (bundle.dividendEvents.isNotEmpty()) {
                DividendLedgerTable(bundle.dividendEvents)
            } else {
                EmptyChartState("Sem eventos", "Nenhuma distribuição anterior listada.")
            }
        }
    }
}

@Composable
fun FiiPatrimonialTab(bundle: AssetChartBundle) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ChartCardContainer(title = "Métricas Patrimoniais") {
            if (bundle.fiiPatrimonialInfo.isNotEmpty()) {
                FiiPatrimonialInfoChart(bundle.fiiPatrimonialInfo)
            } else {
                EmptyChartState("Indisponível", "Detalhamento patrimonial indisponível.")
            }
        }

        ChartCardContainer(title = "Distribuição Física dos Ativos (Estados/Segmentos)") {
            if (bundle.fiiAssetDistribution.isNotEmpty()) {
                val list = bundle.fiiAssetDistribution["Ativos"] ?: emptyList()
                AssetBreakdownDonutChart(
                    title = "Composição Imobiliária",
                    points = list,
                    modifier = Modifier.height(200.dp)
                )
            } else {
                EmptyChartState("Sem divisão física", "Pode ser um Fundo de Papel (CRI) ou dados indisponíveis.")
            }
        }
    }
}

@Composable
fun FiiComparisonTab(bundle: AssetChartBundle) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        FilteredChartCard(title = "Retorno em Comparação com o IFIX (%)", filterOptions = listOf("1A", "3A", "5A", "MAX"), defaultFilter = "5A") { filter ->
            val filteredSeries = remember(bundle.indexComparison, filter) {
                filterComparisonSeries(bundle.indexComparison, filter)
            }
            if (filteredSeries.isNotEmpty()) {
                AssetIndexComparisonChart(
                    series = filteredSeries,
                    modifier = Modifier.height(180.dp)
                )
            } else {
                EmptyChartState("Falta Comparação", "O Proxy não retornou IFIX/CDI/IPCA para este período.")
            }
        }

        ChartCardContainer(title = "Métricas Comparativas com Segmento") {
            if (bundle.fiiPeerAverage.isNotEmpty()) {
                FiiPeerAverageChart(
                    peers = bundle.fiiPeerAverage,
                    modifier = Modifier.height(160.dp)
                )
            } else {
                EmptyChartState("Ausente", "Dados de segmentação industrial comparativa ausentes.")
            }
        }
    }
}

// ======================== INNER SUBCOMPONENTS & CHARTS ========================

@Composable
fun EmptyChartState(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground.copy(alpha = 0.5f))
            .border(1.dp, BorderColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "info",
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun comparisonWindowLimit(filter: String): Int {
    return when (filter.trim().uppercase(Locale.ROOT)) {
        "1A", "1Y" -> 260
        "3A", "3Y" -> 780
        "5A", "5Y" -> 1300
        "10A", "10Y" -> 2600
        "MAX", "TUDO", "ALL" -> Int.MAX_VALUE
        else -> Int.MAX_VALUE
    }
}

private fun filterComparisonSeries(series: List<AssetComparisonSeries>, filter: String): List<AssetComparisonSeries> {
    val limit = comparisonWindowLimit(filter)
    return series.mapNotNull { s ->
        val validPoints = s.points
            .filter { it.value.isFinite() }
            .sortedWith(compareBy<AssetComparisonPoint> { if (it.dateMillis > 0L) it.dateMillis else Long.MAX_VALUE }.thenBy { it.label })
        val selected = if (limit == Int.MAX_VALUE) validPoints else validPoints.takeLast(limit.coerceAtMost(validPoints.size))
        val sampled = downsampleComparisonPoints(selected)
        if (sampled.size >= 2) s.copy(points = sampled) else null
    }
}

private fun downsampleComparisonPoints(points: List<AssetComparisonPoint>, maxPoints: Int = 240): List<AssetComparisonPoint> {
    if (points.size <= maxPoints || maxPoints < 3) return points
    val lastIndex = points.lastIndex
    val step = lastIndex.toDouble() / (maxPoints - 1).toDouble()
    val sampled = ArrayList<AssetComparisonPoint>(maxPoints)
    var previousIndex = -1
    for (i in 0 until maxPoints) {
        val idx = kotlin.math.round(i * step).toInt().coerceIn(0, lastIndex)
        if (idx != previousIndex) {
            sampled.add(points[idx])
            previousIndex = idx
        }
    }
    if (sampled.lastOrNull() != points.last()) sampled.add(points.last())
    return sampled
}

private fun sanitizeFinancialPoints(points: List<FinancialStatementPoint>): List<FinancialStatementPoint> {
    return points.filter { p ->
        listOf(p.netRevenue, p.netProfit, p.netWorth, p.totalAssets, p.totalLiabilities, p.ebit, p.ebitda)
            .all { it.isFinite() }
    }
}

private fun normalizeBreakdownPoints(points: List<AssetBreakdownPoint>): List<AssetBreakdownPoint> {
    val clean = points
        .filter { it.name.isNotBlank() && it.valuePercent.isFinite() && it.valuePercent > 0.0 }
        .take(8)
    val total = clean.sumOf { it.valuePercent }.takeIf { it.isFinite() && it > 0.0 } ?: return emptyList()
    return clean.map { it.copy(valuePercent = (it.valuePercent / total) * 100.0) }
}

@Composable
private fun BundleRangeSelector(
    currentRange: String,
    onRangeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ranges = listOf("1Y", "3Y", "5Y", "10Y", "MAX")
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(ranges) { range ->
            val selected = currentRange.equals(range, ignoreCase = true) ||
                (currentRange.equals("1A", ignoreCase = true) && range == "1Y") ||
                (currentRange.equals("5A", ignoreCase = true) && range == "5Y")
            Surface(
                color = if (selected) GoldPrimary.copy(alpha = 0.16f) else DarkBackground.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (selected) GoldPrimary else BorderColor.copy(alpha = 0.18f)),
                modifier = Modifier.clickable { onRangeSelected(range) }
            ) {
                Text(
                    text = range,
                    color = if (selected) GoldPrimary else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun defaultChartDescription(title: String): String {
    return when {
        title.contains("Comparação com Índices", ignoreCase = true) -> "Compara o retorno acumulado do ativo contra benchmarks de mercado como IBOV, IFIX, CDI e IPCA, quando essas séries são entregues pelo Proxy."
        title.contains("Retorno em Comparação", ignoreCase = true) -> "Mostra se o FII superou ou ficou abaixo de referências como IFIX, CDI e IPCA no intervalo selecionado."
        title.contains("Commodit", ignoreCase = true) -> "Ajuda a observar sensibilidade do ativo a commodities relevantes, especialmente petróleo Brent para empresas expostas ao setor."
        title.contains("Rentabilidade", ignoreCase = true) -> "Mostra retorno nominal e, quando disponível, retorno real descontado da inflação."
        title.contains("Indicadores", ignoreCase = true) -> "Resume múltiplos indicadores fundamentalistas normalizados pelo Proxy/Investidor10."
        title.contains("Proventos", ignoreCase = true) || title.contains("Dividend", ignoreCase = true) || title.contains("Rendimentos", ignoreCase = true) -> "Mostra histórico de pagamentos, distribuição anual/mensal e consistência de renda do ativo."
        title.contains("DRE", ignoreCase = true) || title.contains("Receitas", ignoreCase = true) -> "Compara evolução operacional, receita e lucro para avaliar crescimento e margem ao longo do tempo."
        title.contains("Lucro x Cotação", ignoreCase = true) -> "Cruza lucro histórico com preço para indicar se a cotação acompanhou a evolução dos resultados."
        title.contains("Balanço", ignoreCase = true) || title.contains("Patrimonial", ignoreCase = true) -> "Mostra composição patrimonial, patrimônio líquido, ativos, passivos ou métricas patrimoniais relevantes."
        title.contains("Payout", ignoreCase = true) -> "Indica a parcela do lucro distribuída como proventos; valores muito altos podem exigir cautela."
        title.contains("Faturamento", ignoreCase = true) || title.contains("Distribuição Física", ignoreCase = true) -> "Mostra a composição por segmento, região ou tipo de ativo quando o Investidor10 disponibiliza a quebra."
        title.contains("Segmento", ignoreCase = true) -> "Compara métricas do FII com médias do segmento quando o Proxy consegue obter esses dados."
        else -> "Dados extraídos e normalizados via Valorae Proxy/Investidor10. Campos ausentes são tratados como indisponíveis, sem simulação."
    }
}

@Composable
fun FilteredChartCard(
    title: String,
    filterOptions: List<String> = emptyList(),
    defaultFilter: String? = null,
    description: String = "",
    content: @Composable (selectedFilter: String) -> Unit
) {
    var selectedFilter by remember(filterOptions) { 
        mutableStateOf(defaultFilter ?: filterOptions.firstOrNull() ?: "") 
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, BorderColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            
            if (filterOptions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .background(DarkBackground.copy(alpha=0.5f), RoundedCornerShape(10.dp))
                        .border(1.dp, BorderColor.copy(alpha=0.1f), RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    filterOptions.forEach { option ->
                        val isSelected = option == selectedFilter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                .clickable { selectedFilter = option }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) GoldPrimary else TextSecondary
                            )
                        }
                    }
                }
            }
        }
        val effectiveDescription = description.ifBlank { defaultChartDescription(title) }
        if (effectiveDescription.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = effectiveDescription,
                color = TextSecondary.copy(alpha = 0.82f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        content(selectedFilter)
    }
}

@Composable
fun ChartCardContainer(title: String, description: String = "", content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, BorderColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = TextPrimary
        )
        val effectiveDescription = description.ifBlank { defaultChartDescription(title) }
        if (effectiveDescription.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = effectiveDescription,
                color = TextSecondary.copy(alpha = 0.82f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

@Composable
fun HistoricalRangeSelector(currentRange: String, onRangeSelected: (String) -> Unit) {
    val ranges = listOf("1D", "5D", "1M", "6M", "YTD", "1Y", "5Y", "MAX")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground.copy(alpha=0.5f), RoundedCornerShape(10.dp))
            .border(1.dp, BorderColor.copy(alpha=0.1f), RoundedCornerShape(10.dp))
            .padding(4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ranges.forEach { r ->
            val isSelected = currentRange.trim().uppercase() == r.uppercase()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                    .clickable { onRangeSelected(r) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = r, 
                    fontSize = 12.sp, 
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) GoldPrimary else TextSecondary
                )
            }
        }
    }
}

@Composable
fun AssetProfitabilityChart(
    profitability: List<AssetPeriodReturn>,
    realProfitability: List<AssetPeriodReturn>,
    modifier: Modifier = Modifier
) {
    val items = profitability.take(6)
    if (items.isEmpty()) return

    val maxVal = maxOf(
        items.maxOf { it.valuePercent },
        realProfitability.take(6).maxOfOrNull { it.valuePercent } ?: 0.0
    ).coerceAtLeast(1.0).toFloat()
    val minVal = minOf(
        items.minOf { it.valuePercent },
        realProfitability.take(6).minOfOrNull { it.valuePercent } ?: 0.0
    ).coerceAtMost(0.0).toFloat()
    val range = maxVal - minVal
    
    var activeIndex by remember(items) { mutableStateOf<Int?>(null) }
    var touchX by remember { mutableFloatStateOf(0f) }

    Row(modifier = modifier) {
        // Y-Axis reference
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 16.dp, end = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text("%.1f%%".format(maxVal), color = TextSecondary, fontSize = 8.sp)
            Text("%.1f%%".format(minVal + range * 0.5f), color = TextSecondary, fontSize = 8.sp)
            Text("%.1f%%".format(minVal), color = TextSecondary, fontSize = 8.sp)
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .pointerInput(items) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val space = size.width.toFloat() / items.size
                            activeIndex = (offset.x / space).toInt().coerceIn(0, items.size - 1)
                        },
                        onDrag = { change, _ ->
                            val space = size.width.toFloat() / items.size
                            activeIndex = (change.position.x / space).toInt().coerceIn(0, items.size - 1)
                        },
                        onDragEnd = { activeIndex = null },
                        onDragCancel = { activeIndex = null }
                    )
                }
                .pointerInput(items, "tap") {
                    detectTapGestures(
                        onPress = { offset ->
                            val space = size.width.toFloat() / items.size
                            activeIndex = (offset.x / space).toInt().coerceIn(0, items.size - 1)
                            tryAwaitRelease()
                            activeIndex = null
                        }
                    )
                }
            ) {
                val w = size.width
                val h = size.height - 20.dp.toPx()
                val totalBars = items.size
                val spaceBetweenGroup = w / totalBars
                val barWidth = (spaceBetweenGroup * 0.35f).coerceAtLeast(4.dp.toPx())

                // Draw center guideline (0%)
                val zeroY = h - (((0f - minVal) / range) * h).coerceIn(0f, h)
                drawLine(
                    color = BorderColor.copy(alpha = 0.5f),
                    start = Offset(0f, zeroY),
                    end = Offset(w, zeroY),
                    strokeWidth = 1.dp.toPx()
                )

                // Render nominal vs real bars side-by-side
                items.forEachIndexed { idx, nom ->
                    val groupCenter = idx * spaceBetweenGroup + spaceBetweenGroup / 2

                    val isSelected = activeIndex == idx
                    val alpha = if (activeIndex == null || isSelected) 1f else 0.4f

                    // Nominal bar (GoldPrimary)
                    val nVal = nom.valuePercent.toFloat()
                    val nNorm = (nVal - minVal) / range
                    val nHeight = (nNorm * h).coerceIn(0f, h)
                    val nY = h - nHeight

                    // Real bar (SuccessGreen)
                    val realItem = realProfitability.firstOrNull { it.period == nom.period }
                    val rVal = realItem?.valuePercent?.toFloat() ?: 0f
                    val rNorm = (rVal - minVal) / range
                    val rHeight = (rNorm * h).coerceIn(0f, h)
                    val rY = h - rHeight

                    // Draw Nominal
                    drawRoundRect(
                        color = GoldPrimary.copy(alpha = alpha),
                        topLeft = Offset(groupCenter - barWidth, nY),
                        size = Size(barWidth * 0.9f, nHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // Draw Real
                    drawRoundRect(
                        color = SuccessGreen.copy(alpha = alpha),
                        topLeft = Offset(groupCenter, rY),
                        size = Size(barWidth * 0.9f, rHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }

            // Labels x-axis row overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEach { nom ->
                    val cleanLabel = nom.period.uppercase()
                    Text(
                        text = cleanLabel,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Tooltip Overlay
            activeIndex?.let { idx ->
                val nom = items[idx]
                val realItem = realProfitability.firstOrNull { it.period == nom.period }
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 28.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderColor.copy(alpha=0.2f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = nom.period.uppercase(), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(GoldPrimary, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Nominal: ${String.format("%.1f", nom.valuePercent)}%", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(SuccessGreen, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Real: ${String.format("%.1f", realItem?.valuePercent ?: 0.0)}%", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssetIndicatorHistoryChart(
    cards: List<AssetIndicatorPoint>,
    history: Map<String, List<AssetIndicatorPoint>>,
    modifier: Modifier = Modifier
) {
    var selectedIdx by remember { mutableStateOf(0) }
    val selectedCard = cards.getOrNull(selectedIdx) ?: return
    val cardHistory = history[selectedCard.label] ?: emptyList()

    Column(modifier = modifier) {
        // Multi indicators horizontal chip choices
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            cards.forEachIndexed { i, c ->
                val isSel = i == selectedIdx
                Surface(
                    onClick = { selectedIdx = i },
                    color = if (isSel) GoldPrimary.copy(alpha = 0.2f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSel) GoldPrimary else BorderColor.copy(alpha = 0.5f)),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(text = c.label, fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        Text(text = c.display.ifBlank { "%.2f".format(c.value) }, fontSize = 12.sp, color = if (isSel) GoldPrimary else TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Year-by-year history series or single reference value
        if (cardHistory.isNotEmpty()) {
            Text(
                text = "Histórico de ${selectedCard.label}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Map history points to ChartPoints
            val mapToChartPoints = cardHistory.mapIndexed { idx, point ->
                ChartPoint(
                    timestamp = idx.toLong(),
                    dateLabel = point.year,
                    close = point.value
                )
            }
            HistoricalPriceLineChart(
                points = mapToChartPoints,
                modifier = Modifier.height(120.dp),
                lineColor = GoldPrimary
            )
        } else {
            // Single Value reference description
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkBackground.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "info",
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Valor atual listado de ${selectedCard.label} é R$ ${selectedCard.display.ifBlank { "%.2f".format(selectedCard.value) }}. Sem série histórica presente no Proxy.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AssetDividendYieldChart(points: List<AssetIndicatorPoint>, modifier: Modifier = Modifier) {
    val chartPoints = points.mapIndexed { idx, p ->
        ChartPoint(idx.toLong(), p.year, p.value)
    }
    HistoricalPriceLineChart(points = chartPoints, modifier = modifier, lineColor = GoldPrimary)
}

@Composable
fun AssetDividendPaidChart(
    points: List<AssetIndicatorPoint>,
    label: String,
    modifier: Modifier = Modifier,
    barColor: Color = SuccessGreen
) {
    if (points.isEmpty()) return
    val maxValue = points.maxOf { it.value }.let { if (it <= 0.0) 1.0 else it }.toFloat()
    
    var activeIndex by remember(points) { mutableStateOf<Int?>(null) }
    var touchX by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchX = offset.x
                            val width = size.width.coerceAtLeast(0)
                            val total = points.size
                            val gap = 6.dp.toPx()
                            val barW = ((width - (gap * (total + 1))) / total).coerceAtLeast(1.dp.toPx())
                            val idx = ((offset.x - gap) / (barW + gap)).toInt().coerceIn(0, total - 1)
                            activeIndex = idx
                        },
                        onDrag = { change, _ ->
                            touchX = change.position.x
                            val width = size.width.coerceAtLeast(0)
                            val total = points.size
                            val gap = 6.dp.toPx()
                            val barW = ((width - (gap * (total + 1))) / total).coerceAtLeast(1.dp.toPx())
                            val idx = ((touchX - gap) / (barW + gap)).toInt().coerceIn(0, total - 1)
                            activeIndex = idx
                        },
                        onDragEnd = { activeIndex = null },
                        onDragCancel = { activeIndex = null }
                    )
                }
                .pointerInput(points, "tap") {
                    detectTapGestures(
                        onPress = { offset ->
                            touchX = offset.x
                            val width = size.width.coerceAtLeast(0)
                            val total = points.size
                            val gap = 6.dp.toPx()
                            val barW = ((width - (gap * (total + 1))) / total).coerceAtLeast(1.dp.toPx())
                            val idx = ((offset.x - gap) / (barW + gap)).toInt().coerceIn(0, total - 1)
                            activeIndex = idx
                            tryAwaitRelease()
                            activeIndex = null
                        }
                    )
                }
            ) {
                val w = size.width
                val h = size.height
                val total = points.size
                val gap = 6.dp.toPx()
                val barW = ((w - (gap * (total + 1))) / total).coerceAtLeast(1.dp.toPx())

                points.forEachIndexed { i, p ->
                    val x = gap + i * (barW + gap)
                    val barH = ((p.value.toFloat() / maxValue) * h).coerceAtLeast(1f)
                    
                    val isSelected = activeIndex == i
                    val alpha = if (activeIndex == null || isSelected) 1f else 0.4f
                    
                    drawRoundRect(
                        color = barColor.copy(alpha = alpha),
                        topLeft = Offset(x, h - barH),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
            
            // Tooltip Overlay
            activeIndex?.let { idx ->
                val p = points[idx]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderColor.copy(alpha=0.2f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (label == "Mês") p.period else p.year,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "R$ ${String.format("%,.2f", p.value)}",
                                color = barColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val step = if (points.size > 8) (points.size / 4).coerceAtLeast(1) else 1
            points.forEachIndexed { i, p ->
                if (i % step == 0) {
                    Text(
                        text = if (label == "Mês") p.period else p.year,
                        color = TextSecondary,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AssetIndexComparisonChart(series: List<AssetComparisonSeries>, modifier: Modifier = Modifier) {
    val visibleSeries = series.mapNotNull { s ->
        val valid = s.points
            .filter { it.value.isFinite() }
            .sortedWith(compareBy<AssetComparisonPoint> { if (it.dateMillis > 0L) it.dateMillis else Long.MAX_VALUE }.thenBy { it.label })
        val sampled = downsampleComparisonPoints(valid)
        if (sampled.size >= 2) s.copy(points = sampled) else null
    }
    if (visibleSeries.isEmpty()) return
    val primaryColor = GoldPrimary
    val seriesColors = listOf(primaryColor, SuccessGreen, DangerRed, GoldPale, Color.Cyan, Color.Magenta)

    val allReturns = visibleSeries.flatMap { it.points.map { pt -> pt.value } }
    val rawMax = allReturns.maxOrNull() ?: 1.0
    val rawMin = allReturns.minOrNull() ?: 0.0
    val safeRawMax = if (rawMax.isFinite()) rawMax else 1.0
    val safeRawMin = if (rawMin.isFinite()) rawMin else 0.0
    val paddedMax = if (safeRawMax == safeRawMin) safeRawMax + 1.0 else safeRawMax
    val paddedMin = if (safeRawMax == safeRawMin) safeRawMin - 1.0 else safeRawMin
    val range = (paddedMax - paddedMin).takeIf { it.isFinite() && it > 0.000001 } ?: 1.0

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            visibleSeries.take(6).forEachIndexed { idx, s ->
                val c = seriesColors.getOrElse(idx) { Color.Gray }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(c, shape = CircleShape))
                    Spacer(modifier = Modifier.width(3.dp))
                    val last = s.points.lastOrNull()?.value ?: 0.0
                    Text(text = "${s.name} ${String.format(Locale.ROOT, "%+.1f%%", last)}", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(modifier = Modifier.fillMaxHeight().padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(String.format(Locale.ROOT, "%+.0f%%", paddedMax), color = TextSecondary, fontSize = 8.sp)
                Text("0%", color = TextSecondary, fontSize = 8.sp)
                Text(String.format(Locale.ROOT, "%+.0f%%", paddedMin), color = TextSecondary, fontSize = 8.sp)
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    for (g in 0..4) {
                        val y = h * g / 4f
                        drawLine(color = BorderColor.copy(alpha = 0.15f), start = Offset(0f, y), end = Offset(w, y))
                    }
                    val zeroY = h - (((0.0 - paddedMin) / range).toFloat() * h).coerceIn(0f, h)
                    drawLine(color = TextSecondary.copy(alpha = 0.25f), start = Offset(0f, zeroY), end = Offset(w, zeroY), strokeWidth = 1.dp.toPx())

                    visibleSeries.take(6).forEachIndexed { sIdx, s ->
                        val c = seriesColors.getOrElse(sIdx) { Color.Gray }
                        val p = Path()
                        val itemW = if (s.points.size > 1) w / (s.points.size - 1) else w
                        s.points.forEachIndexed { idx, pt ->
                            val x = idx * itemW
                            val normY = ((pt.value - paddedMin) / range).toFloat()
                            val y = h - (normY * h).coerceIn(0f, h)
                            if (idx == 0) p.moveTo(x, y) else p.lineTo(x, y)
                        }
                        drawPath(path = p, color = c, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }
        }
    }
}

@Composable
fun AssetRevenueProfitChart(points: List<FinancialStatementPoint>, modifier: Modifier = Modifier) {
    val items = sanitizeFinancialPoints(points)
        .filter { it.netRevenue != 0.0 || it.netProfit != 0.0 }
        .sortedBy { it.year }
    if (items.isEmpty()) return

    // Bounds Max and Min
    val maxVal = items.maxOf { kotlin.math.max(it.netRevenue, it.netProfit) }.let { if (!it.isFinite() || it <= 0.0) 1.0 else it }.toFloat()
    val minVal = items.minOf { kotlin.math.min(0.0, it.netProfit) }.let { if (!it.isFinite()) 0.0 else it }.toFloat()
    val range = (maxVal - minVal).takeIf { it.isFinite() && it > 0.0001f } ?: 1f
    
    var activeIndex by remember(items) { mutableStateOf<Int?>(null) }
    var touchX by remember { mutableFloatStateOf(0f) }

    Row(modifier = modifier) {
        Column(modifier = Modifier.fillMaxHeight().padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("%.1fB".format(maxVal / 1e9), color = TextSecondary, fontSize = 8.sp)
            Text("%.1fB".format(minVal / 1e9), color = TextSecondary, fontSize = 8.sp)
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .pointerInput(items) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val space = size.width.toFloat() / items.size
                            activeIndex = (offset.x / space).toInt().coerceIn(0, items.size - 1)
                        },
                        onDrag = { change, _ ->
                            val space = size.width.toFloat() / items.size
                            activeIndex = (change.position.x / space).toInt().coerceIn(0, items.size - 1)
                        },
                        onDragEnd = { activeIndex = null },
                        onDragCancel = { activeIndex = null }
                    )
                }
                .pointerInput(items, "tap") {
                    detectTapGestures(
                        onPress = { offset ->
                            val space = size.width.toFloat() / items.size
                            activeIndex = (offset.x / space).toInt().coerceIn(0, items.size - 1)
                            tryAwaitRelease()
                            activeIndex = null
                        }
                    )
                }
            ) {
                val w = size.width
                val h = size.height - 16.dp.toPx()
                val steps = items.size
                val space = w / steps
                val barW = (space * 0.28f).coerceAtLeast(3.dp.toPx())

                val zeroY = h - (((0f - minVal) / range) * h).coerceIn(0f, h)
                drawLine(color = BorderColor.copy(alpha = 0.3f), start = Offset(0f, zeroY), end = Offset(w, zeroY))

                items.forEachIndexed { idx, p ->
                    val cx = idx * space + space / 2
                    
                    val isSelected = activeIndex == idx
                    val alpha = if (activeIndex == null || isSelected) 1f else 0.4f

                    // Net Revenue (SuccessGreen)
                    val revH = ((p.netRevenue.toFloat() - minVal) / range) * h
                    drawRoundRect(
                        color = SuccessGreen.copy(alpha = alpha),
                        topLeft = Offset(cx - barW, h - revH.toFloat()),
                        size = Size(barW, revH.toFloat()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // Net Profit (GoldPrimary)
                    val profH = ((p.netProfit.toFloat() - minVal) / range) * h
                    drawRoundRect(
                        color = GoldPrimary.copy(alpha = alpha),
                        topLeft = Offset(cx, h - profH.toFloat()),
                        size = Size(barW, profH.toFloat()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
                items.forEach { pt ->
                    Text(
                        text = pt.label,
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Tooltip Overlay
            activeIndex?.let { idx ->
                val p = items[idx]
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderColor.copy(alpha=0.2f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = p.label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(SuccessGreen, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "R$ ${String.format("%,.0f", p.netRevenue)}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(GoldPrimary, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "R$ ${String.format("%,.0f", p.netProfit)}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssetProfitVsQuoteChart(points: List<AssetComparisonPoint>, modifier: Modifier = Modifier) {
    val items = points.filter { it.value.isFinite() && it.secondaryValue.isFinite() }
    if (items.isEmpty()) return

    val maxVal = items.maxOf { it.value.coerceAtLeast(it.secondaryValue) }.let { if (!it.isFinite() || it <= 0.0) 1.0 else it }.toFloat()
    val minVal = items.minOf { it.value.coerceAtMost(it.secondaryValue) }.let { if (!it.isFinite()) 0.0 else it }.toFloat()
    val range = (maxVal - minVal).takeIf { it.isFinite() && it > 0.0001f } ?: 1f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height - 16.dp.toPx()
            val total = items.size
            val space = w / total
            val pathQuote = Path()
            val pathProfit = Path()

            items.forEachIndexed { i, pt ->
                val x = i * space + space / 2
                val yQuote = h - (((pt.value.toFloat() - minVal) / range) * h).coerceIn(0f, h)
                val yProfit = h - (((pt.secondaryValue.toFloat() - minVal) / range) * h).coerceIn(0f, h)

                if (i == 0) {
                    pathQuote.moveTo(x, yQuote)
                    pathProfit.moveTo(x, yProfit)
                } else {
                    pathQuote.lineTo(x, yQuote)
                    pathProfit.lineTo(x, yProfit)
                }

                // Interaction dots
                drawCircle(color = GoldPrimary, radius = 2.dp.toPx(), center = Offset(x, yQuote))
                drawCircle(color = SuccessGreen, radius = 2.dp.toPx(), center = Offset(x, yProfit))
            }
            drawPath(path = pathQuote, color = GoldPrimary, style = Stroke(width = 2.dp.toPx()))
            drawPath(path = pathProfit, color = SuccessGreen, style = Stroke(width = 2.dp.toPx()))
        }

        Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
            items.forEach { pt ->
                Text(
                    text = pt.label,
                    color = TextSecondary,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AssetEquityEvolutionChart(points: List<FinancialStatementPoint>, modifier: Modifier = Modifier) {
    val items = sanitizeFinancialPoints(points)
        .filter { it.totalAssets > 0.0 || it.netWorth > 0.0 }
        .sortedBy { it.year }
    if (items.isEmpty()) return

    val maxVal = items.maxOf { kotlin.math.max(it.totalAssets, it.netWorth) }.let { if (!it.isFinite() || it <= 0) 1.0 else it }.toFloat()
    val minVal = 0f
    val range = (maxVal - minVal).takeIf { it.isFinite() && it > 0.0001f } ?: 1f
    
    var activeIndex by remember(items) { mutableStateOf<Int?>(null) }
    var touchX by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(items) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val space = size.width.toFloat() / items.size
                        activeIndex = (offset.x / space).toInt().coerceIn(0, items.size - 1)
                    },
                    onDrag = { change, _ ->
                        val space = size.width.toFloat() / items.size
                        activeIndex = (change.position.x / space).toInt().coerceIn(0, items.size - 1)
                    },
                    onDragEnd = { activeIndex = null },
                    onDragCancel = { activeIndex = null }
                )
            }
            .pointerInput(items, "tap") {
                detectTapGestures(
                    onPress = { offset ->
                        val space = size.width.toFloat() / items.size
                        activeIndex = (offset.x / space).toInt().coerceIn(0, items.size - 1)
                        tryAwaitRelease()
                        activeIndex = null
                    }
                )
            }
        ) {
            val w = size.width
            val h = size.height - 16.dp.toPx()
            val space = w / items.size
            val barW = (space * 0.25f).coerceAtLeast(3.dp.toPx())

            items.forEachIndexed { i, pt ->
                val cx = i * space + space / 2
                
                val isSelected = activeIndex == i
                val alpha = if (activeIndex == null || isSelected) 1f else 0.4f

                // Assets (Blue/GoldPale)
                val hAssets = (pt.totalAssets.toFloat() / range) * h
                drawRoundRect(
                    color = GoldPale.copy(alpha = alpha),
                    topLeft = Offset(cx - barW, h - hAssets),
                    size = Size(barW, hAssets),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // Equity/Worth (GoldPrimary)
                val hNetWorth = (pt.netWorth.toFloat() / range) * h
                drawRoundRect(
                    color = GoldPrimary.copy(alpha = alpha),
                    topLeft = Offset(cx, h - hNetWorth),
                    size = Size(barW, hNetWorth),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
            items.forEach { pt ->
                Text(
                    text = pt.label,
                    color = TextSecondary,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Tooltip Overlay
        activeIndex?.let { idx ->
            val pt = items[idx]
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor.copy(alpha=0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = pt.label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(GoldPale, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Ativos: R$ ${String.format("%,.0f", pt.totalAssets)}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(GoldPrimary, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "PL: R$ ${String.format("%,.0f", pt.netWorth)}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssetPayoutHistoryChart(points: List<AssetIndicatorPoint>, modifier: Modifier = Modifier) {
    val chartPoints = points.mapIndexed { idx, pt ->
        ChartPoint(idx.toLong(), pt.year, pt.value)
    }
    HistoricalPriceLineChart(points = chartPoints, modifier = modifier, lineColor = SuccessGreen)
}

@Composable
fun AssetBreakdownDonutChart(title: String, points: List<AssetBreakdownPoint>, modifier: Modifier = Modifier) {
    val normalizedPoints = normalizeBreakdownPoints(points)
    if (normalizedPoints.isEmpty()) return
    val donutColors = listOf(GoldPrimary, SuccessGreen, GoldPale, Color.Cyan, Color.Magenta, Color.Yellow)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(100.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val r = size.width * 0.45f
                var startAngle = -90f

                normalizedPoints.forEachIndexed { idx, pt ->
                    val color = donutColors.getOrElse(idx) { Color.Gray }
                    val sweep = (pt.valuePercent.toFloat() / 100f) * 360f
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = 12.dp.toPx())
                    )
                    startAngle += sweep
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            normalizedPoints.forEachIndexed { idx, pt ->
                val color = donutColors.getOrElse(idx) { Color.Gray }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(color, shape = CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pt.name,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "%.1f%%".format(pt.valuePercent),
                        color = GoldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.3f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun FiiDistribution12mChart(points: List<AssetIndicatorPoint>) {
    val cleanPoints = points.filter { it.value.isFinite() && it.value >= 0.0 }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cleanPoints.forEach { p ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = p.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = p.display, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                }
                Spacer(modifier = Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { (p.value.toFloat() / 15f).coerceIn(0f, 1f) },
                    color = GoldPrimary,
                    trackColor = BorderColor.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
fun FiiPatrimonialInfoChart(points: List<AssetIndicatorPoint>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        points.forEach { p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBackground.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = p.label, fontSize = 12.sp, color = TextSecondary)
                Text(text = p.display, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
    }
}

@Composable
fun FiiPeerAverageChart(peers: List<AssetComparisonPoint>, modifier: Modifier = Modifier) {
    val cleanPeers = peers.filter { it.value.isFinite() && it.secondaryValue.isFinite() && (it.value > 0.0 || it.secondaryValue > 0.0) }
    if (cleanPeers.isEmpty()) return

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        cleanPeers.forEach { peer ->
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = DarkBackground.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = peer.label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextSecondary)

                    Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val maxV = kotlin.math.max(peer.value, peer.secondaryValue).coerceAtLeast(1.0).toFloat()
                            val barW = w * 0.25f

                            // Self (GoldPrimary)
                            val selfH = (peer.value.toFloat() / maxV) * h
                            drawRoundRect(
                                color = GoldPrimary,
                                topLeft = Offset(w * 0.2f, h - selfH),
                                size = Size(barW, selfH),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )

                            // Average peer (GoldPale)
                            val peerH = (peer.secondaryValue.toFloat() / maxV) * h
                            drawRoundRect(
                                color = GoldPale,
                                topLeft = Offset(w * 0.55f, h - peerH),
                                size = Size(barW, peerH),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Fundo", fontSize = 8.sp, color = TextSecondary)
                            Text("%.2f".format(peer.value), fontSize = 10.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Setor", fontSize = 8.sp, color = TextSecondary)
                            Text("%.2f".format(peer.secondaryValue), fontSize = 10.sp, color = GoldPale, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DividendLedgerTable(events: List<DividendEvent>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BorderColor.copy(alpha = 0.3f))
                .padding(6.dp)
        ) {
            Text(text = "Tipo", modifier = Modifier.weight(1.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
            Text(text = "Data Com", modifier = Modifier.weight(2f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = "Pagamento", modifier = Modifier.weight(2f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = "Valor", modifier = Modifier.weight(1.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.End)
        }

        LazyColumn(
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            items(events.take(50)) { ev ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .border(
                            width = 0.dp,
                            color = Color.Transparent
                        )
                ) {
                    Text(text = "Dividendo", modifier = Modifier.weight(1.5f), fontSize = 9.sp, color = TextSecondary)
                    Text(text = ev.dateCom, modifier = Modifier.weight(2f), fontSize = 9.sp, color = TextPrimary)
                    Text(text = ev.paymentDate, modifier = Modifier.weight(2f), fontSize = 9.sp, color = TextPrimary)
                    Text(
                        text = "R$ %.4f".format(ev.valuePerShare),
                        modifier = Modifier.weight(1.5f),
                        fontSize = 9.sp,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(color = BorderColor.copy(alpha = 0.1f), thickness = 0.5.dp)
            }
        }
    }
}
