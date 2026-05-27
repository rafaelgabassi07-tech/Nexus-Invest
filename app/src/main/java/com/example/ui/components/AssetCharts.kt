package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
    var selectedCategoryTab by remember { mutableStateOf(0) }
    val categories = if (isFii) {
        listOf("Geral", "Dividendos", "Patrimônio", "Comparativo")
    } else {
        listOf("Análise", "Dividendos", "Comparativo", "DRE", "Negócio")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gráficos Investidor10",
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

        Spacer(modifier = Modifier.height(12.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedCategoryTab,
            containerColor = Color.Transparent,
            contentColor = GoldPrimary,
            edgePadding = 0.dp,
            divider = {},
            indicator = { tabPositions ->
                if (selectedCategoryTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCategoryTab]),
                        color = GoldPrimary
                    )
                }
            }
        ) {
            categories.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedCategoryTab == idx,
                    onClick = { selectedCategoryTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedCategoryTab == idx) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content Screens depending on selections
        Crossfade(targetState = selectedCategoryTab, label = "tabs") { index ->
            if (isFii) {
                when (index) {
                    0 -> FiiGeneralTab(bundle, onRangeChange, currentRange)
                    1 -> FiiDividendTab(bundle)
                    2 -> FiiPatrimonialTab(bundle)
                    3 -> FiiComparisonTab(bundle)
                    else -> FiiGeneralTab(bundle, onRangeChange, currentRange)
                }
            } else {
                when (index) {
                    0 -> StockAnalysisTab(bundle, onRangeChange, currentRange)
                    1 -> StockDividendTab(bundle)
                    2 -> StockComparisonTab(bundle)
                    3 -> StockDreTab(bundle)
                    4 -> StockBusinessTab(bundle)
                    else -> StockAnalysisTab(bundle, onRangeChange, currentRange)
                }
            }
        }
    }
}

// ======================== TABS IMPLEMENTATIONS ========================

@Composable
fun StockAnalysisTab(bundle: AssetChartBundle, onRangeChange: ((String) -> Unit)?, currentRange: String) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Price history
        ChartCardContainer(title = "Cotação Histórica (Proxy)") {
            HistoricalRangeSelector(currentRange = currentRange) { onRangeChange?.invoke(it) }
            Spacer(modifier = Modifier.height(12.dp))
            if (bundle.priceHistory.isNotEmpty()) {
                HistoricalPriceLineChart(
                    points = bundle.priceHistory,
                    modifier = Modifier.height(150.dp),
                    lineColor = SuccessGreen
                )
            } else {
                EmptyChartState("Sem série de preços", "Não foi entregue cotação histórica.")
            }
        }

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
    var subTabIdx by remember { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TabRow(
            selectedTabIndex = subTabIdx,
            containerColor = Color.Transparent,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[subTabIdx]),
                    color = GoldPrimary
                )
            },
            divider = {}
        ) {
            Tab(selected = subTabIdx == 0, onClick = { subTabIdx = 0 }, text = { Text("Graficos", fontSize = 12.sp) })
            Tab(selected = subTabIdx == 1, onClick = { subTabIdx = 1 }, text = { Text("Agenda / Ledger", fontSize = 12.sp) })
        }

        if (subTabIdx == 0) {
            ChartCardContainer(title = "Proventos por Ano") {
                if (bundle.dividendYearly.isNotEmpty()) {
                    AssetDividendPaidChart(
                        points = bundle.dividendYearly,
                        label = "Ano",
                        modifier = Modifier.height(150.dp)
                    )
                } else {
                    EmptyChartState("Sem dividendos pagos", "Nenhum histórico de ano encontrado.")
                }
            }

            ChartCardContainer(title = "Histórico de Dividend Yield") {
                if (bundle.dividendYieldHistory.isNotEmpty()) {
                    AssetDividendYieldChart(
                        points = bundle.dividendYieldHistory,
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
        } else {
            ChartCardContainer(title = "Eventos de Distribuição") {
                if (bundle.dividendEvents.isNotEmpty()) {
                    DividendLedgerTable(bundle.dividendEvents)
                } else {
                    EmptyChartState("Sem eventos", "Nenhum evento registrado de dividendo.")
                }
            }
        }
    }
}

@Composable
fun StockComparisonTab(bundle: AssetChartBundle) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ChartCardContainer(title = "Comparação com Índices (%)") {
            if (bundle.indexComparison.isNotEmpty()) {
                AssetIndexComparisonChart(
                    series = bundle.indexComparison,
                    modifier = Modifier.height(160.dp)
                )
            } else {
                EmptyChartState("Sem séries comparativas", "Índices de mercado indisponíveis no Proxy.")
            }
        }

        if (bundle.commodityComparison.isNotEmpty()) {
            ChartCardContainer(title = "Correlação com Commodities (Brent/Brent Oil)") {
                AssetIndexComparisonChart(
                    series = bundle.commodityComparison,
                    modifier = Modifier.height(160.dp)
                )
            }
        }
    }
}

@Composable
fun StockDreTab(bundle: AssetChartBundle) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ChartCardContainer(title = "DRE: Receitas x Lucros") {
            if (bundle.revenueProfit.isNotEmpty()) {
                AssetRevenueProfitChart(
                    points = bundle.revenueProfit,
                    modifier = Modifier.height(160.dp)
                )
            } else {
                EmptyChartState("Sem DRE", "Dados financeiros históricos indisponíveis no Proxy.")
            }
        }

        ChartCardContainer(title = "Evolução Lucro x Cotação") {
            if (bundle.profitVsQuote.isNotEmpty()) {
                AssetProfitVsQuoteChart(
                    points = bundle.profitVsQuote,
                    modifier = Modifier.height(160.dp)
                )
            } else {
                EmptyChartState("Aguardando Lucro x Cotação", "Série histórica lucro contra preço indisponível.")
            }
        }

        ChartCardContainer(title = "Balanço Patrimonial: Ativo/PL/Passivo") {
            if (bundle.equityEvolution.isNotEmpty()) {
                AssetEquityEvolutionChart(
                    points = bundle.equityEvolution,
                    modifier = Modifier.height(160.dp)
                )
            } else {
                EmptyChartState("Aguardando Ativos e Patrimônio", "Visão de evolução patrimonial indisponível.")
            }
        }

        ChartCardContainer(title = "Payout Histórico (%)") {
            if (bundle.payoutHistory.isNotEmpty()) {
                AssetPayoutHistoryChart(
                    points = bundle.payoutHistory,
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
            if (bundle.revenueByBusiness.isNotEmpty()) {
                val latestYear = bundle.revenueByBusiness.keys.sorted().lastOrNull().orEmpty()
                val points = bundle.revenueByBusiness[latestYear] ?: emptyList()
                AssetBreakdownDonutChart(
                    title = "Origem Faturamento ($latestYear)",
                    points = points,
                    modifier = Modifier.height(200.dp)
                )
            } else {
                EmptyChartState("Sem divisão de negócio", "Divisões em segmentos operacionais indisponíveis.")
            }
        }

        ChartCardContainer(title = "Faturamento por Região (%)") {
            if (bundle.revenueByRegion.isNotEmpty()) {
                val latestYear = bundle.revenueByRegion.keys.sorted().lastOrNull().orEmpty()
                val points = bundle.revenueByRegion[latestYear] ?: emptyList()
                AssetBreakdownDonutChart(
                    title = "Divisão Geográfica ($latestYear)",
                    points = points,
                    modifier = Modifier.height(200.dp)
                )
            } else {
                EmptyChartState("Sem divisão regional", "Geografia de receita indisponível para este ativo.")
            }
        }
    }
}

// ======================== FII TABS IMPLEMENTATIONS ========================

@Composable
fun FiiGeneralTab(bundle: AssetChartBundle, onRangeChange: ((String) -> Unit)?, currentRange: String) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ChartCardContainer(title = "Cotação Histórica do FII") {
            HistoricalRangeSelector(currentRange = currentRange) { onRangeChange?.invoke(it) }
            Spacer(modifier = Modifier.height(12.dp))
            if (bundle.priceHistory.isNotEmpty()) {
                HistoricalPriceLineChart(
                    points = bundle.priceHistory,
                    modifier = Modifier.height(150.dp),
                    lineColor = GoldPale
                )
            } else {
                EmptyChartState("Sem preços", "Série de preços indisponível.")
            }
        }

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
    var subTabIdx by remember { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TabRow(
            selectedTabIndex = subTabIdx,
            containerColor = Color.Transparent,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[subTabIdx]),
                    color = GoldPrimary
                )
            },
            divider = {}
        ) {
            Tab(selected = subTabIdx == 0, onClick = { subTabIdx = 0 }, text = { Text("Performance", fontSize = 12.sp) })
            Tab(selected = subTabIdx == 1, onClick = { subTabIdx = 1 }, text = { Text("Agenda Proventos", fontSize = 12.sp) })
        }

        if (subTabIdx == 0) {
            ChartCardContainer(title = "Rendimentos Pagos por Ano") {
                if (bundle.dividendYearly.isNotEmpty()) {
                    AssetDividendPaidChart(
                        points = bundle.dividendYearly,
                        label = "Ano",
                        modifier = Modifier.height(150.dp),
                        barColor = GoldPrimary
                    )
                } else {
                    EmptyChartState("Sem dividendos", "Série anual ausente.")
                }
            }

            ChartCardContainer(title = "Histórico de Dividend Yield") {
                if (bundle.dividendYieldHistory.isNotEmpty()) {
                    AssetDividendYieldChart(
                        points = bundle.dividendYieldHistory,
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
        } else {
            ChartCardContainer(title = "Acontecimentos e Proventos") {
                if (bundle.dividendEvents.isNotEmpty()) {
                    DividendLedgerTable(bundle.dividendEvents)
                } else {
                    EmptyChartState("Sem eventos", "Nenhuma distribuição anterior listada.")
                }
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
        ChartCardContainer(title = "Retorno em Comparação com o IFIX (%)") {
            if (bundle.indexComparison.isNotEmpty()) {
                AssetIndexComparisonChart(
                    series = bundle.indexComparison,
                    modifier = Modifier.height(160.dp)
                )
            } else {
                EmptyChartState("Falta Comparação", "Índices indisponíveis para o FII.")
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

@Composable
fun ChartCardContainer(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBackground.copy(alpha = 0.4f))
            .border(1.dp, BorderColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
fun HistoricalRangeSelector(currentRange: String, onRangeSelected: (String) -> Unit) {
    val ranges = listOf("1D", "7D", "30D", "6M", "YTD", "1A", "5A", "10A")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ranges.forEach { r ->
            val isSelected = currentRange.trim().uppercase() == r.uppercase()
            Button(
                onClick = { onRangeSelected(r) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) GoldPrimary else Color.Transparent,
                    contentColor = if (isSelected) Color.Black else TextSecondary
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                border = if (isSelected) null else BorderStroke(0.5.dp, BorderColor),
                modifier = Modifier.height(28.dp).minimumInteractiveComponentSize()
            ) {
                Text(text = r, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
            Canvas(modifier = Modifier.fillMaxSize()) {
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
                        color = GoldPrimary,
                        topLeft = Offset(groupCenter - barWidth, nY),
                        size = Size(barWidth * 0.9f, nHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // Draw Real
                    drawRoundRect(
                        color = SuccessGreen,
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

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val total = points.size
                val gap = 6.dp.toPx()
                val barW = ((w - (gap * (total + 1))) / total).coerceAtLeast(1.dp.toPx())

                points.forEachIndexed { i, p ->
                    val x = gap + i * (barW + gap)
                    val barH = ((p.value.toFloat() / maxValue) * h).coerceAtLeast(1f)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, h - barH),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val step = if (points.size > 8) (points.size / 4) else 1
            points.forEachIndexed { i, p ->
                if (i % step == 0) {
                    Text(
                        text = if (label == "Mês") p.period else p.year,
                        color = TextSecondary,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
        }
    }
}

@Composable
fun AssetIndexComparisonChart(series: List<AssetComparisonSeries>, modifier: Modifier = Modifier) {
    if (series.isEmpty()) return
    val primaryColor = GoldPrimary
    val seriesColors = listOf(primaryColor, SuccessGreen, DangerRed, GoldPale, Color.Cyan, Color.Magenta)

    // Merge points to find bounds
    val allPrices = series.flatMap { it.points.map { pt -> pt.value } }
    val maxVal = allPrices.maxOrNull()?.let { if (it == 0.0) 1.0 else it }?.toFloat() ?: 100f
    val minVal = allPrices.minOrNull()?.toFloat() ?: 0f
    val range = maxVal - minVal

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            series.forEachIndexed { idx, s ->
                val c = seriesColors.getOrElse(idx) { Color.Gray }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(c, shape = CircleShape))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = s.name, color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Grid limits lines
                val density = 3
                for (g in 0..density) {
                    val y = h * g / density
                    drawLine(color = BorderColor.copy(alpha = 0.15f), start = Offset(0f, y), end = Offset(w, y))
                }

                series.forEachIndexed { sIdx, s ->
                    if (s.points.size < 2) return@forEachIndexed
                    val c = seriesColors.getOrElse(sIdx) { Color.Gray }
                    val p = Path()
                    val itemW = w / (s.points.size - 1)

                    s.points.forEachIndexed { idx, pt ->
                        val x = idx * itemW
                        val normY = (pt.value.toFloat() - minVal) / range
                        val y = h - (normY * h).coerceIn(0f, h)
                        if (idx == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    drawPath(path = p, color = c, style = Stroke(width = 2.dp.toPx()))
                }
            }
        }
    }
}

@Composable
fun AssetRevenueProfitChart(points: List<FinancialStatementPoint>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val items = points.sortedBy { it.year }.takeLast(8)

    // Bounds Max and Min
    val maxVal = items.maxOf { kotlin.math.max(it.netRevenue, it.netProfit) }.let { if (it <= 0.0) 1.0 else it }.toFloat()
    val minVal = items.minOf { kotlin.math.min(0.0, it.netProfit) }.toFloat()
    val range = maxVal - minVal

    Row(modifier = modifier) {
        Column(modifier = Modifier.fillMaxHeight().padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("%.1fB".format(maxVal / 1e9), color = TextSecondary, fontSize = 8.sp)
            Text("%.1fB".format(minVal / 1e9), color = TextSecondary, fontSize = 8.sp)
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height - 16.dp.toPx()
                val steps = items.size
                val space = w / steps
                val barW = (space * 0.28f).coerceAtLeast(3.dp.toPx())

                val zeroY = h - (((0f - minVal) / range) * h).coerceIn(0f, h)
                drawLine(color = BorderColor.copy(alpha = 0.3f), start = Offset(0f, zeroY), end = Offset(w, zeroY))

                items.forEachIndexed { idx, p ->
                    val cx = idx * space + space / 2

                    // Net Revenue (SuccessGreen)
                    val revH = ((p.netRevenue.toFloat() - minVal) / range) * h
                    drawRoundRect(
                        color = SuccessGreen,
                        topLeft = Offset(cx - barW, h - revH.toFloat()),
                        size = Size(barW, revH.toFloat()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // Net Profit (GoldPrimary)
                    val profH = ((p.netProfit.toFloat() - minVal) / range) * h
                    drawRoundRect(
                        color = GoldPrimary,
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
        }
    }
}

@Composable
fun AssetProfitVsQuoteChart(points: List<AssetComparisonPoint>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val items = points.takeLast(10)

    val maxVal = items.maxOf { it.value.coerceAtLeast(it.secondaryValue) }.let { if (it <= 0.0) 1.0 else it }.toFloat()
    val minVal = items.minOf { it.value.coerceAtMost(items.minOf { it.secondaryValue }) }.toFloat()
    val range = maxVal - minVal

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
    if (points.isEmpty()) return
    val items = points.sortedBy { it.year }.takeLast(8)

    val maxVal = items.maxOf { kotlin.math.max(it.totalAssets, it.netWorth) }.let { if (it <= 0) 1.0 else it }.toFloat()
    val minVal = 0f
    val range = maxVal - minVal

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height - 16.dp.toPx()
            val space = w / items.size
            val barW = (space * 0.25f).coerceAtLeast(3.dp.toPx())

            items.forEachIndexed { i, pt ->
                val cx = i * space + space / 2

                // Assets (Blue/GoldPale)
                val hAssets = (pt.totalAssets.toFloat() / range) * h
                drawRoundRect(
                    color = GoldPale,
                    topLeft = Offset(cx - barW, h - hAssets),
                    size = Size(barW, hAssets),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // Equity/Worth (GoldPrimary)
                val hNetWorth = (pt.netWorth.toFloat() / range) * h
                drawRoundRect(
                    color = GoldPrimary,
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
    if (points.isEmpty()) return
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

                points.forEachIndexed { idx, pt ->
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
            points.forEachIndexed { idx, pt ->
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
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        points.forEach { p ->
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
    if (peers.isEmpty()) return

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        peers.forEach { peer ->
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
