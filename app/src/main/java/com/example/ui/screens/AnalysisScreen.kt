package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.B3UIUtils
import com.example.network.B3AssetData
import com.example.network.ChartPoint
import com.example.network.NewsItem
import com.example.ui.components.HistoricalPriceLineChart
import com.example.ui.components.AssetProxyIndicatorSection
import com.example.ui.components.AssetProxyProfileSection
import com.example.ui.components.CustomBarChart
import com.example.ui.components.PieChart
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    tickerInput: String,
    onTickerInputChanges: (String) -> Unit,
    onSearchClick: (String) -> Unit,
    searchResult: B3AssetData?,
    chartHistory: List<ChartPoint>,
    chartRange: String = "1y",
    onRangeChange: (String) -> Unit = {},
    assetNews: List<NewsItem>,
    isSearching: Boolean,
    favoriteTickers: List<String> = emptyList(),
    onToggleFavorite: (String) -> Unit = {},
    assetChartBundles: Map<String, com.example.network.AssetChartBundle> = emptyMap(),
    isLoadingChartBundle: Boolean = false,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sticky Header Style (matching ChartsScreen)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "Análise de Mercado",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Busque ativos da B3.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        // 1. Search Bar Header (Integrated into content)
        var suggestionsVisible by remember { mutableStateOf(true) }
        var isFocused by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tickerInput,
                        onValueChange = { 
                            onTickerInputChanges(it)
                            suggestionsVisible = true
                        },
                        placeholder = { Text("MGLU3, MXRF11...", color = TextSecondary.copy(alpha = 0.4f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp)) },
                        trailingIcon = if (tickerInput.isNotEmpty()) {
                            {
                                IconButton(onClick = { 
                                    onTickerInputChanges("")
                                    suggestionsVisible = true 
                                }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                suggestionsVisible = false
                                onSearchClick(tickerInput)
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier.weight(1f).height(50.dp).onFocusChanged { isFocused = it.isFocused }
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    IconButton(
                        onClick = {
                            suggestionsVisible = false
                            focusManager.clearFocus()
                            onSearchClick(tickerInput)
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .background(GoldPrimary, MaterialTheme.shapes.medium)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.background, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Search, contentDescription = "Analisar", tint = MaterialTheme.colorScheme.background)
                        }
                    }
                }
            }

            // Suggestions Overlay (Expanded & Intelligent)
            val suggestions = remember(tickerInput) {
                val input = tickerInput.uppercase()
                if (input.length >= 2) {
                    val hardcodedList = listOf(
                        "PETR4", "PETR3", "VALE3", "ITUB4", "BBDC4", "BBAS3", "ABEV3", "MGLU3", "WEGE3", "RENT3",
                        "ITSA4", "SANB11", "B3SA3", "LREN3", "GGBR4", "CSNA3", "VBBR3", "RAIL3", "SUZB3", "JBSS3",
                        "MXRF11", "HGLG11", "KNRI11", "XPLG11", "VISC11", "BTLG11", "BCFF11", "HFOF11", "HCTR11", "IRDM11",
                        "CPLE6", "EQTL3", "EGIE3", "TRPL4", "TAEE11", "VIVT3", "TIMS3", "PSSA3", "BBSE3", "FLRY3",
                        "XPML11", "MALL11", "KNCR11", "KNIP11", "HGBS11", "BRCR11", "GALG11", "ALZR11", "TGAR11", "TRXF11",
                        "CPAR3", "ELET3", "GOAU4", "PETZ3", "SOMA3", "COGN3", "EMBR3", "PRIO3", "TOTS3", "VIVA3"
                    )
                    val fromHardcoded = hardcodedList
                        .filter { it.contains(input) && it != input }
                        .sortedBy { if (it.startsWith(input)) 0 else 1 }
                        
                    val generated = if (input.length in 3..4 && input.all { it.isLetter() }) {
                        listOf("${input}3", "${input}4", "${input}11")
                    } else emptyList()
                    
                    val exactMatch = if (input.length >= 5 && input.any { it.isDigit() }) {
                        listOf(input)
                    } else emptyList()
                    
                    (exactMatch + generated + fromHardcoded).distinct().filter { exactMatch.contains(it) || it != input }.take(6)
                } else emptyList()
            }
            
            if (suggestions.isNotEmpty() && suggestionsVisible && isFocused && !isSearching) {
                Surface(
                    modifier = Modifier
                        .padding(top = 74.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = DarkSurfaceElevated,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.12f))
                ) {
                    Column {
                        suggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_suggestion_$suggestion")
                                    .clickable {
                                        onTickerInputChanges(suggestion)
                                        onSearchClick(suggestion)
                                        focusManager.clearFocus()
                                        suggestionsVisible = false
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(suggestion, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("Sugestão B3", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            if (suggestion != suggestions.last()) {
                                HorizontalDivider(color = BorderColor.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }

        // 1.5 STORIES: Round circular favorites row below search bar
        if (favoriteTickers.isNotEmpty()) {
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "FAVORITOS ATIVOS",
                        color = GoldPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp)
                    ) {
                        items(favoriteTickers, key = { it }) { ticker ->
                            val isSelected = searchResult?.ticker?.uppercase() == ticker.uppercase()
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .width(54.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        color = if (isSelected) GoldPrimary.copy(alpha = 0.15f) else DarkSurfaceElevated
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) GoldPrimary else BorderColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable(
                                        onClick = {
                                            onTickerInputChanges(ticker)
                                            onSearchClick(ticker)
                                            focusManager.clearFocus()
                                        },
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = androidx.compose.foundation.LocalIndication.current
                                    )
                                    .testTag("favorite_story_$ticker")
                            ) {
                                Text(
                                    text = if (ticker.length > 5) ticker.take(5) else ticker,
                                    color = if (isSelected) GoldPrimary else TextPrimary,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 2. Active search analysis display
        androidx.compose.animation.AnimatedVisibility(
            visible = searchResult != null,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn()
        ) {
            if (searchResult != null) {
                val asset = searchResult
                val isFii = asset.isFii

                Column {
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
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = GoldPrimary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .border(1.dp, GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isFii) "FII" else "AÇÃO",
                                            color = GoldPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    val isFavorited = favoriteTickers.any { it.uppercase() == asset.ticker.uppercase() }
                                    IconButton(
                                        onClick = { onToggleFavorite(asset.ticker) },
                                        modifier = Modifier.size(36.dp).testTag("action_favorite_${asset.ticker}")
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = "Favoritar ativo",
                                            tint = if (isFavorited) DangerRed else TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = asset.name,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "R$ ${String.format("%.2f", asset.price)}",
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                
                                val isPositive = asset.changePercent >= 0.0
                                val valColor = if (isPositive) SuccessGreen else DangerRed
                                Text(
                                    text = "${if (isPositive) "+" else ""}${String.format("%.2f", asset.changePercent)}%",
                                    color = valColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Elegant description & profile card
                    if (asset.assetDescription.isNotEmpty()) {
                        Surface(
                            color = DarkSurface,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.05f))
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
                                    val actSector = if (isFii) asset.fiiSegment else asset.subSector
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
                                    text = asset.assetDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    lineHeight = 18.sp,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    var mainAnalysisTabIdx by remember { mutableStateOf(0) }
                    val analysisTabs = listOf("Resumo", "Desempenho & Índices", "Finanças & Balanço", "Proventos & Payout", "Indicadores", "Perfil & Dados")
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(analysisTabs.size) { idx ->
                            val isSelected = mainAnalysisTabIdx == idx
                            Surface(
                                color = if (isSelected) GoldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, if (isSelected) GoldPrimary else BorderColor.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable { mainAnalysisTabIdx = idx }
                            ) {
                                Text(
                                    text = analysisTabs[idx],
                                    color = if (isSelected) GoldPrimary else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    if (mainAnalysisTabIdx == 0) {
                        val tickerKey = asset.ticker.trim().uppercase()
                        val bundle = assetChartBundles[tickerKey]

                        // Proxy Chart & 52-wk
                        if (chartHistory.isNotEmpty()) {
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Variação de Preço",
                                            color = GoldPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                        
                                        // Range Selector
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.horizontalScroll(rememberScrollState())
                                        ) {
                                            listOf("1D", "5D", "1M", "6M", "YTD", "1Y", "5Y", "MAX").forEach { rawRange ->
                                                val range = rawRange.lowercase()
                                                val isSelected = range == chartRange.lowercase()
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .testTag("chart_range_$range")
                                                        .background(if (isSelected) GoldPrimary else Color.Transparent)
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isSelected) Color.Transparent else BorderColor.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { onRangeChange(range) }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = range.uppercase(),
                                                        color = if (isSelected) DarkBackground else TextSecondary,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    HistoricalPriceLineChart(
                                        points = chartHistory,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        lineColor = GoldPrimary
                                    )
                                }
                            }
                        }

                        // Always show 52-week oscillation independently
                        if (!asset.low52.isNaN() && !asset.high52.isNaN() && asset.high52 > 0) {
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.05f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Mín 52 Semanas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("Máx 52 Semanas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .background(BorderColor.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        val low = asset.low52
                                        val high = asset.high52
                                        val current = asset.price
                                        
                                        if (high > low && current >= low) {
                                            val den = high - low
                                            val progress = if (den > 0) ((current - low) / den).coerceIn(0.0, 1.0).toFloat() else 0f
                                            
                                            if (!progress.isNaN()) {
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
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("R$ ${String.format("%.2f", asset.low52)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        Text("R$ ${String.format("%.2f", asset.high52)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    } // end tab 0

                    if (mainAnalysisTabIdx == 1) {
                        val tickerKey = asset.ticker.trim().uppercase()
                        val bundle = assetChartBundles[tickerKey]

                        // Aba própria para desempenho, rentabilidade e comparações. Nenhum painel genérico é usado aqui.
                        if (isLoadingChartBundle) {
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.05f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Carregando desempenho, rentabilidade e índices...", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        } else if (bundle != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            com.example.ui.components.ChartCategoryHeader(
                                title = "Desempenho e Rentabilidade",
                                subtitle = if (isFii) "Rentabilidade nominal e real do FII" else "Rentabilidade nominal vs real extraída do Investidor10"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isFii) {
                                com.example.ui.components.FiiGeneralTab(bundle)
                            } else {
                                com.example.ui.components.StockAnalysisTab(bundle)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            com.example.ui.components.ChartCategoryHeader(
                                title = "Comparação e Correlações",
                                subtitle = if (isFii) "Comparação com IFIX, CDI, IPCA e pares quando disponível" else "Comparação com índices, CDI, IPCA, IBOV e commodities quando disponível"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isFii) {
                                com.example.ui.components.FiiComparisonTab(bundle)
                            } else {
                                com.example.ui.components.StockComparisonTab(bundle)
                            }
                        } else {
                            com.example.ui.components.ChartCardContainer(title = "Desempenho e Índices") {
                                com.example.ui.components.EmptyChartState(
                                    title = "Gráficos indisponíveis",
                                    message = "O VALORAE Proxy não retornou séries reais de desempenho e comparação para este ativo no momento."
                                )
                            }
                        }
                    } // end tab 1
                    
                    if (mainAnalysisTabIdx == 2) {
                        val bundleProfile = assetChartBundles[asset.ticker.trim().uppercase()]
                        if (isLoadingChartBundle) {
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.05f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Carregando gráficos de finanças...", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        } else if (bundleProfile != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (isFii) {
                                com.example.ui.components.ChartCategoryHeader(
                                    title = "Ativos e Patrimônio Imobiliário",
                                    subtitle = "Composição física imobiliária, segmentos e estados"
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                com.example.ui.components.FiiPatrimonialTab(bundleProfile)
                            } else {
                                com.example.ui.components.ChartCategoryHeader(
                                    title = "Finanças, Balanço e Payout",
                                    subtitle = "Receitas, lucros, lucro x cotação, Ativo / PL / Passivo e payout histórico"
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                com.example.ui.components.StockDreTab(bundleProfile)
                                Spacer(modifier = Modifier.height(16.dp))
                                com.example.ui.components.StockBusinessTab(bundleProfile)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        } else {
                            com.example.ui.components.ChartCardContainer(title = "Gráficos de Finanças") {
                                com.example.ui.components.EmptyChartState(
                                    title = "Dados indisponíveis",
                                    message = "O VALORAE Proxy não retornou dados de DRE ou patrimônio para este ativo."
                                )
                            }
                        }
                    } // end tab 2

                    if (mainAnalysisTabIdx == 3) {
                        val bundleTab1 = assetChartBundles[asset.ticker.trim().uppercase()]
                        if (isLoadingChartBundle) {
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.05f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Carregando histórico de proventos...", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        } else if (bundleTab1 != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            com.example.ui.components.ChartCategoryHeader(
                                title = "Proventos, Dividendos e Yield",
                                subtitle = if (isFii) "Distribuições, dividend yield e histórico de rendimentos" else "Dividendos, dividend yield e eventos de distribuição"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isFii) {
                                com.example.ui.components.FiiDividendTab(bundleTab1)
                            } else {
                                com.example.ui.components.StockDividendTab(bundleTab1)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        } else {
                            com.example.ui.components.ChartCardContainer(title = "Histórico de Proventos") {
                                com.example.ui.components.EmptyChartState(
                                    title = "Dados indisponíveis",
                                    message = "O VALORAE Proxy não retornou histórico de proventos para este ativo."
                                )
                            }
                        }
                    } // end tab 3

                    if (mainAnalysisTabIdx == 4) {
                        val bundleTab1ForIndicators = assetChartBundles[asset.ticker.trim().uppercase()]
                        AssetProxyIndicatorSection(
                            assetData = asset,
                            bundle = bundleTab1ForIndicators,
                            isFii = isFii,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    } // end tab 4

                    if (mainAnalysisTabIdx == 5) {
                        val bundleProfile = assetChartBundles[asset.ticker.trim().uppercase()]
                        AssetProxyProfileSection(
                            assetData = asset,
                            bundle = bundleProfile,
                            isFii = isFii,
                            newsItems = assetNews,
                            isLoadingNews = isSearching,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    } // end tab 5

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (searchResult == null) {
            // Screen launch empty state message
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = androidx.compose.animation.fadeIn()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Pesquise um ativo B3 para analisar",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Insira acima o símbolo do papel de sua escolha (Ex: ITUB4, MXRF11, TAEE11) para ver cotações e variações fundamentais.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun NewsEntry(
    news: NewsItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.link))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Ignore
                }
            }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = news.source.uppercase(),
                    color = GoldPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp
                )
                Text(
                    text = news.pubDate,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = news.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AnalysisIndicatorItem(
    label: String,
    value: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(), 
            color = MaterialTheme.colorScheme.primary, 
            fontSize = 8.sp, 
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value, 
            color = MaterialTheme.colorScheme.onSurface, 
            fontSize = 15.sp, 
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}

