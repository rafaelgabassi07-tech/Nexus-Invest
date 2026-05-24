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
    chartRange: String = "1y",
    onRangeChange: (String) -> Unit = {},
    assetNews: List<NewsItem>,
    isSearching: Boolean,
    favoriteTickers: List<String> = emptyList(),
    onToggleFavorite: (String) -> Unit = {},
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

                    // 3. Historical Canvas Line Chart card
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("1d", "5d", "1mo", "1y", "5y").forEach { range ->
                                            val isSelected = range == chartRange
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
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                // 52 Week Range Indicator
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
                                        val low = asset.low52
                                        val high = asset.high52
                                        val current = asset.price
                                        
                                        if (high > low && current >= low && !current.isNaN() && !low.isNaN() && !high.isNaN()) {
                                            val den = high - low
                                            val progress = if (den > 0) {
                                                ((current - low) / den).coerceIn(0.0, 1.0).toFloat()
                                            } else 0f
                                            
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
                                    Spacer(modifier = Modifier.height(6.dp))
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
                    }

                    // 4. Metrics Indicators Grid (VALORAE inspired)
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
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
                            metrics.add(Triple("Dividend Yield", "${String.format(java.util.Locale.US, "%.2f", asset.dy)}%", "Retorno em proventos"))
                            metrics.add(Triple("P/VP", String.format(java.util.Locale.US, "%.2f", asset.pvp), "Preço / Valor Patrimonial"))
                            metrics.add(Triple(if (isFii) "Últ. Provento" else "P/L", if (isFii) "R$ ${String.format(java.util.Locale.US, "%.2f", asset.lastDividend)}" else String.format(java.util.Locale.US, "%.2f", asset.pl), if (isFii) "Baseado na última distr." else "Preço / Lucro anual"))
                            metrics.add(Triple("VPA", "R$ ${String.format(java.util.Locale.US, "%.2f", asset.vpa)}", "Valor Justo Contábil"))
                            
                            if (isFii) {
                                metrics.add(Triple("Vacância", "${String.format(java.util.Locale.US, "%.1f", asset.fiiVacancy)}%", "Proporção de área vaga nos imóveis"))
                                metrics.add(Triple("Liquidez Diária", if (asset.dailyLiquidity > 1_000_000) String.format(java.util.Locale.US, "%.1fM", asset.dailyLiquidity / 1_000_000) else String.format(java.util.Locale.US, "%.0f", asset.dailyLiquidity), "Volume financeiro mensal"))
                                metrics.add(Triple("Segmento", asset.fiiSegment.ifEmpty { "Outros" }, "Tipo de operação do FII"))
                                metrics.add(Triple("Número de Imóveis", if (asset.fiiPropertyCount == 0) "N/A" else "${asset.fiiPropertyCount} prop.", "Ativos físicos"))
                                metrics.add(Triple("P/VP Máximo Alvo", "1.00", "Parâmetro do mercado de tijolo"))
                                if (asset.magicNumber > 0) {
                                    metrics.add(Triple("Magic Number", String.format(java.util.Locale.US, "%.0f cotas", asset.magicNumber), "Para comprar 1 cota c/ div."))
                                }
                            } else {
                                metrics.add(Triple("LPA", "R$ ${String.format(java.util.Locale.US, "%.2f", asset.lpa)}", "Lucro líquido por ação anual"))
                                metrics.add(Triple("P/Receita (PSR)", String.format(java.util.Locale.US, "%.2f", asset.priceToSales), "Preço / Receita Líquida"))
                                metrics.add(Triple("Margem Líquida", "${String.format(java.util.Locale.US, "%.2f", asset.margins)}%", "Eficiência líquida"))
                                metrics.add(Triple("Margem Bruta", "${String.format(java.util.Locale.US, "%.2f", asset.grossMargin)}%", "Eficiência bruta"))
                                metrics.add(Triple("Margem Ebit", "${String.format(java.util.Locale.US, "%.2f", asset.ebitMargin)}%", "Eficiência Ebit"))
                                metrics.add(Triple("Margem Ebitda", "${String.format(java.util.Locale.US, "%.2f", asset.ebitdaMargin)}%", "Eficiência Ebtida"))
                                metrics.add(Triple("EV/Ebitda", String.format(java.util.Locale.US, "%.2f", asset.evEbitda), "Valor da Firma / Ebitda"))
                                metrics.add(Triple("EV/Ebit", String.format(java.util.Locale.US, "%.2f", asset.evEbit), "Valor da Firma / Ebit"))
                                metrics.add(Triple("P/Ebitda", String.format(java.util.Locale.US, "%.2f", asset.priceEbitda), "Preço / Ebitda"))
                                metrics.add(Triple("P/Ebit", String.format(java.util.Locale.US, "%.2f", asset.priceEbit), "Preço / Ebit"))
                                metrics.add(Triple("P/Ativo", String.format(java.util.Locale.US, "%.2f", asset.priceAsset), "Preço / Ativo Total"))
                                metrics.add(Triple("P/Cap.Giro", String.format(java.util.Locale.US, "%.2f", asset.priceCapGiro), "Preço / Capital de Giro"))
                                metrics.add(Triple("P/Ativo Circ. Liq.", String.format(java.util.Locale.US, "%.2f", asset.priceAtivoCircLiq), "Preço / Ativo Circ. Líq."))
                                metrics.add(Triple("Giro Ativos", String.format(java.util.Locale.US, "%.2f", asset.giroAtivos), "Giro de Ativos"))
                                metrics.add(Triple("ROE", "${String.format(java.util.Locale.US, "%.2f", asset.roe)}%", "Retorno s/ Patrimônio Líq."))
                                metrics.add(Triple("ROIC", "${String.format(java.util.Locale.US, "%.2f", asset.roic)}%", "Retorno s/ Capital Invest."))
                                metrics.add(Triple("ROA", "${String.format(java.util.Locale.US, "%.2f", asset.roa)}%", "Retorno s/ Ativos"))
                                metrics.add(Triple("Dív. Líq / Patrimônio", String.format(java.util.Locale.US, "%.2f", asset.divLiqPatrimonio), "Dívida Líquida / Patrimônio"))
                                metrics.add(Triple("Dív. Líq / EBITDA", String.format(java.util.Locale.US, "%.2f", asset.debtEbitda), "Dívida Líquida / EBITDA"))
                                metrics.add(Triple("Dívida Líq / Ebit", String.format(java.util.Locale.US, "%.2f", asset.divLiqEbit), "Dívida Líq. / EBIT"))
                                metrics.add(Triple("Dívida Bruta / Patrim.", String.format(java.util.Locale.US, "%.2f", asset.divBrutaPatrimonio), "Dívida Bruta / Patrimônio"))
                                metrics.add(Triple("Patrimônio / Ativos", String.format(java.util.Locale.US, "%.2f", asset.patrimonioAtivos), "Patrimônio / Ativos"))
                                metrics.add(Triple("Passivos / Ativos", String.format(java.util.Locale.US, "%.2f", asset.passivosAtivos), "Passivos / Ativos"))
                                metrics.add(Triple("Liquidez Corrente", String.format(java.util.Locale.US, "%.2f", asset.liquidezCorrente), "Liquidez Corrente"))
                                metrics.add(Triple("CAGR Receitas (5a)", "${String.format(java.util.Locale.US, "%.2f", asset.cagrRevenue5y)}%", "Cresc. Receita Anual"))
                                metrics.add(Triple("CAGR Lucros (5a)", "${String.format(java.util.Locale.US, "%.2f", asset.cagrProfit5y)}%", "Cresc. Lucro Anual"))
                                metrics.add(Triple("Payout", "${String.format(java.util.Locale.US, "%.2f", asset.payout)}%", "Lucro distribuído"))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                metrics.chunked(2).forEach { rowItems ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        rowItems.forEach { (label, value, desc) ->
                                            IndicatorItem(
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
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4B. Informações da Empresa (Investidor10 inspired)
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "SOBRE A EMPRESA / BALANÇO PATRIMONIAL",
                                color = GoldPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            val companyInfo = mutableListOf<Triple<String, String, String>>()
                            if (asset.cnpj.isNotEmpty()) companyInfo.add(Triple("CNPJ", asset.cnpj, ""))
                            if (asset.foundationYear.isNotEmpty()) companyInfo.add(Triple("Ano de Fundação", asset.foundationYear, ""))
                            if (asset.listingYear.isNotEmpty()) companyInfo.add(Triple("Estreia na Bolsa", asset.listingYear, ""))
                            if (asset.listSegment.isNotEmpty()) companyInfo.add(Triple("Segmento de Listagem", asset.listSegment, ""))
                            if (asset.employeesCount.isNotEmpty()) companyInfo.add(Triple("Nº de Funcionários", asset.employeesCount, ""))
                            if (asset.totalPapers.isNotEmpty()) companyInfo.add(Triple("Nº Total de Papéis", asset.totalPapers, ""))
                            
                            // Specific FII fields
                            if (isFii) {
                                if (asset.fiiTotalHolders.isNotEmpty()) companyInfo.add(Triple("Número de Cotistas", asset.fiiTotalHolders, ""))
                                if (asset.fiiIssuedShares.isNotEmpty()) companyInfo.add(Triple("Cotas Emitidas", asset.fiiIssuedShares, ""))
                                if (asset.fiiAdminFee.isNotEmpty()) companyInfo.add(Triple("Taxa de Administração", asset.fiiAdminFee, ""))
                                if (asset.fiiFundType.isNotEmpty()) companyInfo.add(Triple("Tipo de Fundo", asset.fiiFundType, ""))
                                if (asset.fiiTargetAudience.isNotEmpty()) companyInfo.add(Triple("Público-alvo", asset.fiiTargetAudience, ""))
                                if (asset.fiiMandate.isNotEmpty()) companyInfo.add(Triple("Mandato", asset.fiiMandate, ""))
                                if (asset.fiiManagementType.isNotEmpty()) companyInfo.add(Triple("Tipo de Gestão", asset.fiiManagementType, ""))
                                if (asset.fiiDuration.isNotEmpty()) companyInfo.add(Triple("Prazo de Duração", asset.fiiDuration, ""))
                            }
                            
                            val mcap = if(asset.marketCap > 1_000_000_000) String.format(java.util.Locale.US, "%.2f B", asset.marketCap / 1_000_000_000) else if(asset.marketCap > 1_000_000) String.format(java.util.Locale.US, "%.2f M", asset.marketCap / 1_000_000) else String.format(java.util.Locale.US, "%.0f", asset.marketCap)
                            companyInfo.add(Triple("Valor de Mercado", "R$ $mcap", ""))
                            
                            if (asset.firmValue > 0) {
                                val frmVal = if(asset.firmValue > 1_000_000_000) String.format(java.util.Locale.US, "%.2f B", asset.firmValue / 1_000_000_000) else if(asset.firmValue > 1_000_000) String.format(java.util.Locale.US, "%.2f M", asset.firmValue / 1_000_000) else String.format(java.util.Locale.US, "%.0f", asset.firmValue)
                                companyInfo.add(Triple("Valor de Firma", "R$ $frmVal", ""))
                            }
                            if (asset.netWorth > 0) {
                                val ntWrth = if(asset.netWorth > 1_000_000_000) String.format(java.util.Locale.US, "%.2f B", asset.netWorth / 1_000_000_000) else if(asset.netWorth > 1_000_000) String.format(java.util.Locale.US, "%.2f M", asset.netWorth / 1_000_000) else String.format(java.util.Locale.US, "%.0f", asset.netWorth)
                                companyInfo.add(Triple("Patrimônio Líquido", "R$ $ntWrth", ""))
                            }
                            if (asset.totalAssets > 0) {
                                val tAssets = if(asset.totalAssets > 1_000_000_000) String.format(java.util.Locale.US, "%.2f B", asset.totalAssets / 1_000_000_000) else if(asset.totalAssets > 1_000_000) String.format(java.util.Locale.US, "%.2f M", asset.totalAssets / 1_000_000) else String.format(java.util.Locale.US, "%.0f", asset.totalAssets)
                                companyInfo.add(Triple("Ativo Total", "R$ $tAssets", ""))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                companyInfo.chunked(2).forEach { rowItems ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        rowItems.forEach { (label, value, desc) ->
                                            IndicatorItem(
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
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Qualitative Analysis Box
                    Surface(
                        color = GoldPrimary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
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
                                    isFii && asset.fiiVacancy > 15.0 -> 
                                        "ALERTA: Vacância acima de 15%. Verifique o motivo da desocupação e a localização dos imóveis antes de aportar."
                                    !isFii && asset.debtEbitda > 4.0 -> 
                                        "CUIDADO: Alavancagem financeira elevada (Dívida/EBITDA > 4x). A empresa pode ter dificuldades em cenários de juros altos."
                                    asset.pvp < 0.8 && asset.dy > 8.0 -> 
                                        "OPORTUNIDADE: Ativo descontado (P/VP < 0.8) e com DY atrativo. Pode haver uma distorção de preço favorável."
                                    asset.pvp > 1.5 -> 
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
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // VALUATION SECTION
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
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

                            val grahamValue = kotlin.math.sqrt(22.5 * asset.lpa * asset.vpa).takeIf { !it.isNaN() } ?: 0.0
                            val marginGraham = if (grahamValue > 0) ((grahamValue / asset.price) - 1.0) * 100 else 0.0
                            
                            if (!isFii) {
                                // Graham Formula
                                val marginColorG = if (marginGraham > 0) SuccessGreen else DangerRed

                                Surface(
                                    color = DarkBackground,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f)),
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
                            val bazinValue = asset.lastDividend / bazinYieldTarget
                            val marginBazin = if (bazinValue > 0) ((bazinValue / asset.price) - 1.0) * 100 else 0.0
                            val marginColorB = if (marginBazin > 0) SuccessGreen else DangerRed

                            Surface(
                                color = DarkBackground,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f)),
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
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Recommender Banner Mock
                            val avgMargin = if (!isFii) (marginGraham + marginBazin)/2 else marginBazin
                            val recommendation = if (avgMargin > 15) "COMPRA RECOM. (FORTE)" else if (avgMargin > 5) "COMPRA (MODERADA)" else if (avgMargin > -5) "MANTER" else "ALERTA (ÁGIO)"
                            val recColor = if (avgMargin > 5) SuccessGreen else if (avgMargin > -5) GoldPrimary else DangerRed
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(recColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                    .border(1.dp, recColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Info, contentDescription=null, tint = recColor, modifier=Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Consenso VALORAE: $recommendation", color = recColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    ExtendedAnalysisMocks(isFii = isFii)

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // News Section for the specific asset
                    if (assetNews.isNotEmpty()) {
                        val sortedAssetNews = remember(assetNews) {
                            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                            assetNews.sortedByDescending { 
                                try {
                                    dateFormat.parse(it.pubDate)?.time ?: 0L
                                } catch (e: Exception) {
                                    0L
                                }
                            }
                        }

                        Text(
                            text = "ÚLTIMAS NOTÍCIAS DE ${asset.ticker}",
                            color = GoldPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        sortedAssetNews.take(5).forEach { news ->
                            NewsEntry(news = news)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

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
fun IndicatorItem(
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

@Composable
fun ExtendedAnalysisMocks(isFii: Boolean) {
}
