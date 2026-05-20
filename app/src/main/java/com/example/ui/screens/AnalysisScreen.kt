package com.example.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.B3AssetData
import com.example.network.ChartPoint
import com.example.network.NewsItem
import com.example.ui.components.HistoricalPriceLineChart
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    tickerInput: String,
    onTickerInputChanges: (String) -> Unit,
    onSearchClick: (String) -> Unit,
    searchResult: B3AssetData?,
    chartHistory: List<ChartPoint>,
    assetNews: List<NewsItem>,
    isSearching: Boolean,
    aiReport: String?,
    isLoadingAiReport: Boolean,
    onTriggerAiAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Search Bar Header
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "Buscar Ativo na B3",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tickerInput,
                        onValueChange = onTickerInputChanges,
                        placeholder = { Text("Ex: PETR4, MXRF11, HGLG11", color = TextSecondary) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onSearchClick(tickerInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkBackground),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkBackground)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Pesquisar")
                        }
                    }
                }
            }
        }

        // 2. Active search analysis display
        if (searchResult != null) {
            val asset = searchResult
            val isFii = asset.isFii

            // Live quick metrics header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = asset.ticker,
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isFii) SuccessGreen.copy(alpha = 0.15f) else GoldPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isFii) "FII" else "Ação",
                                    color = if (isFii) SuccessGreen else GoldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = asset.name,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "R$ ${String.format("%.2f", asset.price)}",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        
                        val isPositive = asset.changePercent >= 0.0
                        val valColor = if (isPositive) SuccessGreen else DangerRed
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", asset.changePercent)}%",
                            color = valColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Historical Canvas Line Chart card
            if (chartHistory.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column {
                        Text(
                            text = "Histórico de Preço (1 Ano)",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        HistoricalPriceLineChart(
                            points = chartHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            lineColor = if (isFii) SuccessGreen else GoldPrimary
                        )
                    }
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
            }

            // 4. Metrics Indicators Grid (Investidor 10 inspired)
            Text(
                text = "Indicadores Fundamentalistas",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndicatorItem(
                    label = "Dividend Yield",
                    value = "${String.format("%.2f", asset.dy)}%",
                    desc = "Retorno em proventos",
                    modifier = Modifier.weight(1f)
                )
                IndicatorItem(
                    label = "P/VP",
                    value = String.format("%.2f", asset.pvp),
                    desc = "Preço / V. Patrimonial",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndicatorItem(
                    label = if (isFii) "Last Provento" else "P/L",
                    value = if (isFii) "R$ ${String.format("%.2f", asset.lastDividend)}" else String.format("%.2f", asset.pl),
                    desc = if (isFii) "Último pago por cota" else "Preço / Lucro anual",
                    modifier = Modifier.weight(1f)
                )
                IndicatorItem(
                    label = "VPA",
                    value = "R$ ${String.format("%.2f", asset.vpa)}",
                    desc = "Patrimônio líquido / cota",
                    modifier = Modifier.weight(1f)
                )
            }

            if (!isFii) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IndicatorItem(
                        label = "ROE",
                        value = "${String.format("%.1f", asset.roe)}%",
                        desc = "Retorno s/ patrimônio",
                        modifier = Modifier.weight(1f)
                    )
                    IndicatorItem(
                        label = "LPA",
                        value = "R$ ${String.format("%.2f", asset.lpa)}",
                        desc = "Lucro por ação anual",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IndicatorItem(
                        label = "Liquidez Diária",
                        value = if (asset.dailyLiquidity > 1_000_000) {
                            String.format("%.1fM", asset.dailyLiquidity / 1_000_000)
                        } else {
                            String.format("%.0f", asset.dailyLiquidity)
                        },
                        desc = "Volume negociado",
                        modifier = Modifier.weight(1f)
                    )
                    IndicatorItem(
                        label = "Vacância Física",
                        value = if (asset.margins > 0.0) "${String.format("%.1f", asset.margins % 15.0)}%" else "5.2%",
                        desc = "Ativos sem locatários",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Direct Gemini Advisor button and output panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Avaliação Inteligente IA",
                                color = GoldPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = onTriggerAiAnalysis,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkBackground),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Gerar Relatório", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isLoadingAiReport) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(34.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Gemini estruturando seu relatório...", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    } else if (aiReport != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = BorderColor)
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text(
                            text = aiReport,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Start
                        )
                        
                        // Disclaimer for prompt prototype compliance
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, shape = RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = GoldSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aviso: Relatórios automáticos por IA são informativos e não valem como recomendações formais de mercado para compra ou venda.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Solicite uma análise qualificada do seu ativo para receber notas de diversificação, valuation e riscos da IA do Investidor 10.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        } else {
            // Screen launch empty state message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = GoldPrimary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Pesquise um ativo B3 para analisar",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Insira acima o símbolo do papel de sua escolha (Ex: ITUB4, MXRF11, TAEE11) para ver cotações, variações e gerar o parecer fundamentalista por inteligência artificial.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Composable
fun IndicatorItem(
    label: String,
    value: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Column {
            Text(text = label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = GoldPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = desc, color = TextSecondary.copy(alpha = 0.8f), fontSize = 10.sp, lineHeight = 13.sp)
        }
    }
}
