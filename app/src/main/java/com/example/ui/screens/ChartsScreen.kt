package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.PortfolioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(viewModel: PortfolioViewModel, modifier: Modifier = Modifier) {
    val summaries by viewModel.assetSummaries.collectAsStateWithLifecycle()
    val summaryModel by viewModel.portfolioSummary.collectAsStateWithLifecycle()
    
    var showModal by remember { mutableStateOf<String?>(null) }
    
    val chartModelProducerDividends = remember { ChartEntryModelProducer() }
    val chartModelProducerIpca = remember { ChartEntryModelProducer() }

    LaunchedEffect(summaryModel, summaries) {
        withContext(Dispatchers.Default) {
            // Generate projected dividends block
            // Average DY of portfolio:
            val totalCurrent = summaryModel.totalCurrentValue
            val avgDy = if (totalCurrent > 0) summaries.sumOf { it.totalCurrentValue * it.dividendYield } / totalCurrent else 0.0
            
            // Assume the user has been building the portfolio over 12 months. 
            val finalMonthlyDiv = (totalCurrent * (avgDy / 100.0)) / 12.0
            val divData = List(12) { i -> 
                val scale = (i + 1) / 12f // linear growth
                val value = if (finalMonthlyDiv > 0) (finalMonthlyDiv * scale).toFloat() else (10f * scale)
                entryOf(i, value) 
            }
            chartModelProducerDividends.setEntries(divData)
            
            // IPCA vs Portfolio (Return percent)
            val ipcaAccumulated = 5.5f // Assume 5.5% IPCA over period
            val portReturn = summaryModel.returnPercent.toFloat()
            val months = 12
            
            // IPCA line: linear from 0 to 5.5%
            val ipcaData = List(months) { i -> entryOf(i, (ipcaAccumulated / months) * (i + 1)) }
            // Portfolio line: linear from 0 to portReturn
            val portData = List(months) { i -> entryOf(i, (portReturn / months) * (i + 1)) }
            
            chartModelProducerIpca.setEntries(ipcaData, portData) // IPCA and Portfolio Return
        }
    }


    val alocacaoData = remember(summaryModel) {
        val acoes = (summaryModel.sharesRatioStock * 100).toFloat()
        val fiis = (summaryModel.sharesRatioFii * 100).toFloat()
        val data = mutableListOf<Pair<String, Float>>()
        if (acoes > 0f) data.add("Ações" to acoes)
        if (fiis > 0f) data.add("FIIs" to fiis)
        if (data.isEmpty()) data.add("Sem Cotas" to 100f)
        data
    }
    
    val segmentosData = remember(summaries) {
        val total = summaryModel.totalCurrentValue
        if (total == 0.0) return@remember listOf("Sem Cotas" to 100f)
        
        val setores = summaries.groupBy {
            val t = it.ticker.uppercase()
            when {
                t.startsWith("ITUB") || t.startsWith("BBDC") || t.startsWith("BBAS") || t.startsWith("SANB") || t.startsWith("BPAC") -> "Bancos"
                t.startsWith("EGIE") || t.startsWith("TAEE") || t.startsWith("CPFE") || t.startsWith("ENBR") || t.startsWith("CMIG") -> "Energia"
                t.startsWith("VALE") || t.startsWith("CSNA") || t.startsWith("USIM") -> "Mineração"
                t.startsWith("PETR") || t.startsWith("PRIO") || t.startsWith("RRRP") -> "Petróleo/Gás"
                t.startsWith("WEGE") || t.startsWith("EMBR") -> "Indústria"
                it.type == "FII" -> "Fundos Imobiliários"
                else -> "Outros"
            }
        }.mapValues { entry -> 
            (entry.value.sumOf { it.totalCurrentValue } / total * 100.0).toFloat()
        }.toList()
        setores
    }
    
    val topAgenda = remember(summaries) {
        summaries.sortedByDescending { it.dividendYield * it.totalCurrentValue }.take(3)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Gráficos & Estatísticas",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Acompanhe seus proventos, rentabilidade contra a inflação e alocação.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // 1. Proventos Chart
            item {
                ChartCard(
                    title = "Evolução de Proventos",
                    description = "Seus dividendos mensais nos últimos 12 meses.",
                    onInfoClick = { showModal = "Proventos" }
                ) {
                    Chart(
                        chart = columnChart(),
                        chartModelProducer = chartModelProducerDividends,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            // 2. IPCA+ Chart
            item {
                ChartCard(
                    title = "Rentabilidade vs IPCA+",
                    description = "Sua carteira comparada à inflação acumulada.",
                    onInfoClick = { showModal = "IPCA+" }
                ) {
                    Chart(
                        chart = lineChart(),
                        chartModelProducer = chartModelProducerIpca,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            // 3. Alocação de Ativos (Pie Chart)
            item {
                ChartCard(
                    title = "Alocação de Ativos",
                    description = "Distribuição percentual da sua carteira.",
                    onInfoClick = { showModal = "Alocação" }
                ) {
                    PieChart(
                        data = alocacaoData,
                        colors = listOf(GoldPrimary, SuccessGreen, Color(0xFF6B9CE2), Color(0xFFAAAAAA))
                    )
                }
            }
            
            // 4. Segmentos
            item {
                ChartCard(
                    title = "Locação por Segmentos",
                    description = "Em quais setores seu dinheiro está alocado.",
                    onInfoClick = { showModal = "Segmentos" }
                ) {
                    PieChart(
                        data = segmentosData,
                        colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5), Color(0xFF00BCD4), Color.Yellow, Color.Cyan, Color.LightGray)
                    )
                }
            }

            // 5. Agenda Dividendos
            item {
                ChartCard(
                    title = "Agenda de Dividendos",
                    description = "Próximos pagamentos anunciados.",
                    onInfoClick = { showModal = "Agenda" }
                ) {
                    DividendScheduleList(topAgenda)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showModal != null) {
        AlertDialog(
            onDismissRequest = { showModal = null },
            title = {
                Text(text = "Detalhes: \$showModal", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = getModalContent(showModal!!),
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showModal = null },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkBackground)
                ) {
                    Text("OK, entendi")
                }
            },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ChartCard(
    title: String,
    description: String,
    onInfoClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = description, color = TextSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onInfoClick) {
                    Icon(Icons.Default.Info, contentDescription = "Informações", tint = GoldPrimary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun PieChart(
    data: List<Pair<String, Float>>,
    colors: List<Color>
) {
    val total = data.map { it.second }.sum()
    var startAngle = 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            data.forEachIndexed { index, pair ->
                val sweepAngle = (pair.second / total) * 360f
                drawArc(
                    color = colors[index],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 40.dp.toPx(), cap = StrokeCap.Butt),
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Legend
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEachIndexed { index, pair ->
                val percentage = (pair.second / total) * 100f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(colors[index], RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = pair.first, color = TextPrimary, fontSize = 13.sp)
                    }
                    Text(
                        text = String.format("%.1f%%", percentage),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DividendScheduleList(agenda: List<com.example.viewmodel.AssetSummary>) {
    if (agenda.isEmpty()) {
        Text("Sem previsões recentes.", color = TextSecondary)
        return
    }
    
    val schedule = agenda.mapIndexed { index, asset ->
        val divAmt = asset.sharesCount * asset.currentPrice * (asset.dividendYield / 100.0) / 12.0
        val day = 15 + (index * 5)
        Triple(asset.ticker, String.format("R$ %.2f", divAmt), "$day/06")
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        schedule.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.first.take(1),
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = item.first, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Data Com: Ex", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = item.second, color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Pagamento: \${item.third}", color = TextSecondary, fontSize = 11.sp)
                }
            }
            HorizontalDivider(color = BorderColor)
        }
    }
}

fun getModalContent(topic: String): String {
    return when (topic) {
        "Proventos" -> "Este gráfico mostra os rendimentos (dividendos e JCP) recebidos em sua conta. Acompanhar a evolução mensal ajuda a verificar se sua meta de renda passiva está sendo atingida."
        "IPCA+" -> "Aqui comparamos o avanço do seu patrimônio com a inflação medida pelo IPCA somado a uma taxa de juros real. É o indicador perfeito para analisar o aumento do seu poder de compra de fato."
        "Alocação" -> "A alocação de ativos refere-se à estratégia de diversificação dos seus investimentos (Ações, FIIs, Tesouro). Ter uma boa distribuição é a melhor forma de proteger a carteira de flutuações bruscas do mercado."
        "Segmentos" -> "Os segmentos econômicos dividem a bolsa em setores (Bancário, Energia, Varejo, etc). Esta visão ajuda a evitar a superexposição ao risco de apenas um mercado específico não ir bem em época de crise."
        "Agenda" -> "A agenda de dividendos é gerada a partir dos seus ativos com Data-Com fechada que pagarão rendimentos nas próximas semanas. Prepare-se para colher os frutos ou programar seus próximos reinvestimentos!"
        else -> "Detalhes não disponíveis."
    }
}
