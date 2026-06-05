package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.ProxyCapabilityRow
import com.example.network.ProxyCapabilitySection
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.PortfolioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProxyToolsScreen(
    viewModel: PortfolioViewModel,
    onAssetClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.proxyCapabilities.collectAsStateWithLifecycle()
    val summaries by viewModel.assetSummaries.collectAsStateWithLifecycle()
    var tickerInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.selectedTicker, summaries) {
        if (tickerInput.isBlank()) {
            tickerInput = state.selectedTicker.ifBlank { summaries.firstOrNull()?.ticker.orEmpty() }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshProxyCapabilities(force = false)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ProxyToolsHeader(
                isLoading = state.isLoading,
                lastUpdated = state.lastUpdated,
                onRefresh = { viewModel.refreshProxyCapabilities(tickerInput, force = true) }
            )
        }

        item {
            TickerCapabilitySearch(
                value = tickerInput,
                onValueChange = { tickerInput = it.uppercase(Locale.ROOT).take(12) },
                onAnalyze = { viewModel.refreshProxyCapabilities(tickerInput, force = true) },
                onOpenAsset = {
                    val clean = tickerInput.trim().uppercase(Locale.ROOT)
                    if (clean.isNotBlank()) {
                        viewModel.searchTickerInput.value = clean
                        viewModel.searchAndAnalyzeAsset(clean)
                        onAssetClick(clean)
                    }
                }
            )
        }

        item {
            FeatureGrid(
                items = listOf(
                    Triple("Raio-X", "Qualidade, cobertura, fontes e plano de ação por ativo.", Icons.Outlined.Info),
                    Triple("FIIs", "Renda, vacância, patrimônio, comunicados e checklist.", Icons.Outlined.AccountBalanceWallet),
                    Triple("Carteira", "Rebalanceamento, risco, renda, eventos e alocação.", Icons.Outlined.PieChart),
                    Triple("Radar", "Watchlist e oportunidades analisadas pelo VALORAE.", Icons.Outlined.QueryStats)
                )
            )
        }

        if (state.isLoading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                    color = GoldPrimary
                )
            }
        }

        if (state.error.isNotBlank()) {
            item { ProxyNoticeCard(state.error) }
        }

        state.assetCapabilities?.let { caps ->
            item { SectionGroupTitle("RAIO-X DO ATIVO ${caps.ticker}", Icons.Outlined.Info) }
            items(caps.qualitySections, key = { "quality-${it.endpoint}-${it.title}" }) { section ->
                CapabilitySectionCard(section = section)
            }

            item { SectionGroupTitle("ANÁLISE FUNDAMENTALISTA AVANÇADA", Icons.Outlined.QueryStats) }
            items(caps.advancedSections, key = { "asset-${it.endpoint}-${it.title}" }) { section ->
                CapabilitySectionCard(section = section)
            }

            if (caps.isFii || caps.fiiSections.isNotEmpty()) {
                item { SectionGroupTitle("CENTRAL AVANÇADA DE FIIs", Icons.Outlined.AccountBalanceWallet) }
                items(caps.fiiSections, key = { "fii-${it.endpoint}-${it.title}" }) { section ->
                    CapabilitySectionCard(section = section)
                }
            }
        }

        state.portfolioCapabilities?.let { caps ->
            if (caps.sections.isNotEmpty()) {
                item { SectionGroupTitle("CARTEIRA: REBALANCEAMENTO, RISCO E RENDA", Icons.Outlined.AccountBalanceWallet) }
                items(caps.sections, key = { "portfolio-${it.endpoint}-${it.title}" }) { section ->
                    CapabilitySectionCard(section = section)
                }
            }
            if (caps.radarSections.isNotEmpty()) {
                item { SectionGroupTitle("RADAR / WATCHLIST", Icons.Outlined.QueryStats) }
                items(caps.radarSections, key = { "radar-${it.endpoint}-${it.title}" }) { section ->
                    CapabilitySectionCard(section = section)
                }
            }
            if (caps.diagnosticsSections.isNotEmpty()) {
                item { SectionGroupTitle("DIAGNÓSTICO AVANÇADO DOS DADOS", Icons.Outlined.Info) }
                items(caps.diagnosticsSections, key = { "diag-${it.endpoint}-${it.title}" }) { section ->
                    CapabilitySectionCard(section = section)
                }
            }
        }

        if (!state.isLoading && state.assetCapabilities == null && state.portfolioCapabilities == null && state.error.isBlank()) {
            item {
                ProxyNoticeCard("Adicione um ativo ou toque em Atualizar para consultar os módulos avançados disponíveis.")
            }
        }
    }
}

@Composable
private fun ProxyToolsHeader(isLoading: Boolean, lastUpdated: Long, onRefresh: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                    Text("Dados avançados", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    "Recursos avançados para transformar a carteira em uma central analítica.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (lastUpdated > 0L) {
                    Text(
                        "Atualizado ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastUpdated))}",
                        color = TextSecondary.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar", tint = if (isLoading) TextSecondary else GoldPrimary)
            }
        }
    }
}

@Composable
private fun TickerCapabilitySearch(
    value: String,
    onValueChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onOpenAsset: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ativo para Raio-X", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Ex.: PETR4, VALE3, HGLG11") },
                shape = RoundedCornerShape(18.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onAnalyze, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text("Consultar dados")
                }
                OutlinedButton(onClick = onOpenAsset, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text("Abrir ativo")
                }
            }
        }
    }
}

@Composable
private fun FeatureGrid(items: List<Triple<String, String, ImageVector>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (title, subtitle, icon) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.size(36.dp).background(GoldPrimary.copy(alpha = 0.10f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(19.dp))
                            }
                            Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionGroupTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp, start = 2.dp)) {
        Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
        Text(title, color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
private fun CapabilitySectionCard(section: ProxyCapabilitySection) {
    var expanded by remember { mutableStateOf(false) }
    val visibleRows = if (expanded) section.rows else section.rows.take(4)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(section.title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    if (section.subtitle.isNotBlank()) {
                        Text(section.subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp, maxLines = if (expanded) 5 else 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                StatusPill(section.status.ifBlank { "OK" })
            }
            if (section.endpoint.isNotBlank()) {
                Text(section.endpoint, color = TextSecondary.copy(alpha = 0.75f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            visibleRows.forEach { row -> CapabilityRowView(row) }
            section.warnings.take(if (expanded) 6 else 2).forEach { warning ->
                Text("• $warning", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, lineHeight = 15.sp)
            }
            AnimatedVisibility(section.rows.size > 4 || section.warnings.size > 2) {
                Text(
                    text = if (expanded) "Mostrar menos" else "Ver tudo",
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { expanded = !expanded }.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CapabilityRowView(row: ProxyCapabilityRow) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(row.label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (row.value.isNotBlank()) {
                Text(row.value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        val normalizedScore = when {
            row.score in 0.01..1.0 -> row.score.toFloat()
            row.score in 1.01..100.0 -> (row.score / 100.0).toFloat()
            else -> 0f
        }.coerceIn(0f, 1f)
        if (normalizedScore > 0f) {
            LinearProgressIndicator(
                progress = { normalizedScore },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                color = when {
                    normalizedScore >= 0.75f -> SuccessGreen
                    normalizedScore >= 0.45f -> GoldPrimary
                    else -> MaterialTheme.colorScheme.error
                },
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )
        }
        if (row.detail.isNotBlank()) {
            Text(row.detail, color = TextSecondary.copy(alpha = 0.85f), fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    val clean = text.ifBlank { "OK" }
    val color = when {
        clean.contains("erro", true) || clean.contains("fail", true) -> MaterialTheme.colorScheme.error
        clean.contains("parcial", true) || clean.contains("partial", true) || clean.contains("sem", true) -> GoldPrimary
        else -> SuccessGreen
    }
    Surface(color = color.copy(alpha = 0.11f), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, color.copy(alpha = 0.25f))) {
        Text(clean.take(18), color = color, fontWeight = FontWeight.Black, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun ProxyNoticeCard(text: String) {
    Surface(
        color = GoldPrimary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.22f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = GoldPrimary)
            Text(text, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}
