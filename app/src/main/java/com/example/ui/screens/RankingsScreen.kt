package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.B3AssetData
import com.example.network.MarketRankingItem
import com.example.network.MarketRankingSnapshot
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.PortfolioAnalyticsState
import com.example.viewmodel.PortfolioViewModel
import java.util.Locale
import kotlin.math.abs

private data class RankingCategoryUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String = "",
    val items: List<MarketRankingItem>,
    val positive: Boolean? = null,
    val unavailableMessage: String = "O Valorae Proxy ainda não retornou dados suficientes para este ranking."
)

@Composable
fun RankingsScreen(
    viewModel: PortfolioViewModel,
    onAssetClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val analytics by viewModel.portfolioAnalytics.collectAsStateWithLifecycle()
    val summaries by viewModel.assetSummaries.collectAsStateWithLifecycle()
    val cachedAssetData by viewModel.cachedAssetData.collectAsStateWithLifecycle()

    LaunchedEffect(summaries.size) {
        viewModel.refreshLiveMarketRankings(force = false, full = true)
        if (summaries.isNotEmpty()) {
            viewModel.refreshPortfolioAnalytics(force = false)
        }
    }

    val categories = remember(analytics) { buildRankingCategories(analytics) }
    var selectedId by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(categories) {
        val selected = categories.firstOrNull { it.id == selectedId }
        if (selectedId.isBlank() || selected == null || (selected.items.isEmpty() && categories.any { it.items.isNotEmpty() })) {
            selectedId = categories.firstOrNull { it.items.isNotEmpty() }?.id ?: categories.firstOrNull()?.id.orEmpty()
        }
    }
    val selectedCategory = categories.firstOrNull { it.id == selectedId } ?: categories.firstOrNull()
    val hasAnyRanking = categories.any { it.items.isNotEmpty() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            RankingsHeader(
                isLoading = analytics.isLoading,
                source = analytics.source,
                onRefresh = {
                    viewModel.refreshLiveMarketRankings(force = true, full = true)
                    viewModel.refreshPortfolioAnalytics(force = true)
                }
            )
        }

        item { RankingsInfoBanner() }

        item {
            Text(
                text = "CATEGORIAS DE RANKING",
                color = GoldPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp)
            )
        }

        items(count = (categories.size + 1) / 2, key = { row -> "ranking-grid-row-$row" }) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val left = categories.getOrNull(row * 2)
                val right = categories.getOrNull(row * 2 + 1)
                if (left != null) {
                    RankingCategoryCard(
                        category = left,
                        selected = selectedId == left.id,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedId = left.id }
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (right != null) {
                    RankingCategoryCard(
                        category = right,
                        selected = selectedId == right.id,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedId = right.id }
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            if (selectedCategory != null) {
                RankingDetailsCard(
                    category = selectedCategory,
                    assetData = cachedAssetData,
                    onAssetClick = onAssetClick
                )
            }
        }

        item {
            MarketMoversDuo(
                ranking = analytics.liveMarketRanking,
                assetData = cachedAssetData,
                onAssetClick = onAssetClick,
                compact = false
            )
        }

        if (!hasAnyRanking) {
            item { RankingsEmptyState() }
        }

        val portfolioRanking = analytics.portfolioRanking
        if (portfolioRanking != null) {
            item {
                Text(
                    text = "INTELIGÊNCIA DA CARTEIRA",
                    color = GoldPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 4.dp)
                )
            }
            item { ProxyActionPlanSection(analytics.analysis) }
        }

        val warnings = buildList {
            addAll(analytics.portfolioRanking?.warnings.orEmpty())
            addAll(analytics.liveMarketRanking?.warnings.orEmpty())
            addAll(analytics.stockMarketRanking?.warnings.orEmpty())
            addAll(analytics.fiiMarketRanking?.warnings.orEmpty())
        }.distinct()
        if (warnings.isNotEmpty()) {
            item { RankingWarningsCard(warnings) }
        }
    }
}

private fun buildRankingCategories(analytics: PortfolioAnalyticsState): List<RankingCategoryUi> {
    val live = analytics.liveMarketRanking
    val stock = analytics.stockMarketRanking ?: analytics.liveMarketRanking
    val fii = analytics.fiiMarketRanking
    val portfolio = analytics.portfolioRanking

    return listOf(
        RankingCategoryUi("altas", "Maiores Altas", "Movimentos positivos do dia", "Hoje", live?.highs.orEmpty(), positive = true),
        RankingCategoryUi("baixas", "Maiores Baixas", "Movimentos negativos do dia", "Hoje", live?.lows.orEmpty(), positive = false),
        RankingCategoryUi("score", "Score Valorae", "Melhor combinação de qualidade e dados", "Proxy", stock?.score.orEmpty()),
        RankingCategoryUi("dy", "Dividend Yield", "Maiores yields no recorte do Proxy", "DY", stock?.dividendYield.orEmpty()),
        RankingCategoryUi("baratas-pvp", "Mais Baratas", "Menores P/VP entre os comparados", "P/VP", stock?.pvp.orEmpty().ifEmpty { stock?.value.orEmpty() }),
        RankingCategoryUi("baratas-pl", "Menores P/Ls", "Empresas com menor P/L positivo", "P/L", stock?.pl.orEmpty()),
        RankingCategoryUi("roe", "Maiores ROEs", "Retorno sobre patrimônio", "ROE", stock?.roe.orEmpty()),
        RankingCategoryUi("roic", "Maiores ROICs", "Retorno sobre capital investido", "ROIC", stock?.roic.orEmpty()),
        RankingCategoryUi("quality", "Qualidade dos Dados", "Cobertura e confiabilidade do Proxy", "Qualidade", stock?.quality.orEmpty()),
        RankingCategoryUi("buy-hold", "Buy And Hold", "Perfil conservador do Proxy", "Conserv.", stock?.conservative.orEmpty()),
        RankingCategoryUi("growth", "Crescimento", "Perfil crescimento por ROE/ROIC/score", "Cresc.", stock?.growth.orEmpty()),
        RankingCategoryUi("dividendos", "Perfil Dividendos", "Ranking composto de renda", "Renda", stock?.dividendsProfile.orEmpty()),
        RankingCategoryUi("valor", "Valor", "Ranking de preço relativo", "Valor", stock?.valueProfile.orEmpty()),
        RankingCategoryUi("fii-renda", "FIIs Renda", "Ranking de renda para fundos imobiliários", "FII", fii?.incomeFii.orEmpty()),
        RankingCategoryUi("fii-dy", "FIIs Dividend Yield", "Maiores yields entre FIIs comparados", "FII DY", fii?.dividendYield.orEmpty()),
        RankingCategoryUi("fii-baratos", "FIIs Mais Baratos", "Menores P/VP entre FIIs", "FII P/VP", fii?.pvp.orEmpty().ifEmpty { fii?.value.orEmpty() }),
        RankingCategoryUi("carteira-score", "Carteira Score", "Ranking dos ativos que você possui", "Carteira", portfolio?.score.orEmpty()),
        RankingCategoryUi("carteira-dy", "Carteira Dividend Yield", "DY dos ativos da carteira", "Carteira", portfolio?.dividendYield.orEmpty())
    )
}

@Composable
private fun RankingCategoryCard(
    category: RankingCategoryUi,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val hasData = category.items.isNotEmpty()
    val accent = when (category.positive) {
        true -> SuccessGreen
        false -> DangerRed
        null -> GoldPrimary
    }
    Surface(
        color = if (selected) DarkSurfaceElevated else DarkSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.42f) else BorderColor.copy(alpha = 0.07f)),
        modifier = modifier
            .heightIn(min = 124.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = if (selected) 0.16f else 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (category.positive) {
                        true -> Icons.AutoMirrored.Outlined.TrendingUp
                        false -> Icons.AutoMirrored.Outlined.TrendingDown
                        null -> Icons.Filled.Leaderboard
                    },
                    contentDescription = null,
                    tint = accent.copy(alpha = if (hasData) 1f else 0.55f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = category.title,
                color = if (hasData) TextPrimary else TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (category.badge.isNotBlank()) {
                Spacer(modifier = Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(category.badge, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasData) "${category.items.size} ativos" else "Aguardando dados",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RankingDetailsCard(
    category: RankingCategoryUi,
    assetData: Map<String, B3AssetData>,
    onAssetClick: (String) -> Unit
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (category.positive) {
                        true -> Icons.AutoMirrored.Outlined.TrendingUp
                        false -> Icons.AutoMirrored.Outlined.TrendingDown
                        null -> Icons.Filled.Leaderboard
                    },
                    contentDescription = null,
                    tint = when (category.positive) {
                        true -> SuccessGreen
                        false -> DangerRed
                        null -> GoldPrimary
                    },
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(category.subtitle, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (category.items.isEmpty()) {
                RankingsInlineNotice(
                    title = "Ranking indisponível agora",
                    message = category.unavailableMessage,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                category.items.take(12).forEachIndexed { index, item ->
                    RankingItemRow(
                        item = item,
                        assetData = assetData,
                        positive = category.positive,
                        onClick = { onAssetClick(item.ticker) }
                    )
                    if (index < category.items.take(12).lastIndex) {
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.08f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingItemRow(
    item: MarketRankingItem,
    assetData: Map<String, B3AssetData>,
    positive: Boolean?,
    onClick: () -> Unit
) {
    val ticker = item.ticker.ifBlank { "—" }
    val accent = when (positive) {
        true -> SuccessGreen
        false -> DangerRed
        null -> GoldPrimary
    }
    val asset = assetData[ticker]
    val value = rankingValueText(item, positive)
    val price = marketMoverPriceText(item, asset)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                .border(1.dp, BorderColor.copy(alpha = 0.07f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.rank.takeIf { it > 0 }?.toString() ?: "#",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(ticker, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = item.name.ifBlank { item.explanation }.ifBlank { item.grade }.ifBlank { item.source }
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(value, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
            if (price != "—") {
                Text(price, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RankingsHeader(
    isLoading: Boolean,
    source: String,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Leaderboard, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Rankings",
                    color = TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isLoading) "Atualizando rankings pelo Valorae Proxy..." else source.ifBlank { "Valorae Proxy" },
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar rankings", tint = GoldPrimary)
        }
    }
}

@Composable
private fun RankingsInfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GoldPrimary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .border(1.dp, GoldPrimary.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Categorias no estilo Investidor10, alimentadas pelo Valorae Proxy: altas/baixas ao vivo, fundamentos, perfis de qualidade, dividendos, valor, crescimento, FIIs e rankings da carteira. Eles não alteram proventos históricos, IPCA ou a linha do tempo real da sua carteira.",
            color = TextPrimary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
fun HomeMarketMoversPreview(
    ranking: MarketRankingSnapshot?,
    assetData: Map<String, B3AssetData> = emptyMap(),
    onOpenRankings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highs = ranking?.highs.orEmpty()
    val lows = ranking?.lows.orEmpty()
    if (highs.isEmpty() && lows.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MOVIMENTOS DO DIA",
                color = GoldPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Ver rankings",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onOpenRankings)
            )
        }
        MarketMoversDuo(
            ranking = ranking,
            assetData = assetData,
            onAssetClick = { onOpenRankings() },
            compact = true,
            modifier = Modifier.clickable(onClick = onOpenRankings)
        )
    }
}

@Composable
fun MarketMoversDuo(
    ranking: MarketRankingSnapshot?,
    assetData: Map<String, B3AssetData> = emptyMap(),
    onAssetClick: (String) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val highs = ranking?.highs.orEmpty()
    val lows = ranking?.lows.orEmpty()
    if (highs.isEmpty() && lows.isEmpty()) {
        RankingsInlineNotice(
            title = "Altas e baixas indisponíveis",
            message = "O Proxy pode retornar ranking por Score/DY quando a fonte ao vivo bloqueia altas e baixas. Use as categorias acima para consultar os dados disponíveis.",
            modifier = modifier
        )
        return
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (highs.isNotEmpty()) {
            MarketMoversCard(
                title = "Maiores Altas",
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                items = highs,
                assetData = assetData,
                positive = true,
                compact = compact,
                onAssetClick = onAssetClick
            )
        }
        if (lows.isNotEmpty()) {
            MarketMoversCard(
                title = "Maiores Baixas",
                icon = Icons.AutoMirrored.Outlined.TrendingDown,
                items = lows,
                assetData = assetData,
                positive = false,
                compact = compact,
                onAssetClick = onAssetClick
            )
        }
    }
}

@Composable
fun MarketMoversCard(
    title: String,
    icon: ImageVector,
    items: List<MarketRankingItem>,
    assetData: Map<String, B3AssetData> = emptyMap(),
    positive: Boolean,
    compact: Boolean,
    onAssetClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (positive) SuccessGreen else DangerRed
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.06f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(if (compact) 18.dp else 22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = if (compact) 15.sp else 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(if (compact) 10.dp else 16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items.take(if (compact) 4 else 8).forEachIndexed { index, item ->
                    MarketMoverRow(
                        item = item,
                        asset = assetData[item.ticker],
                        positive = positive,
                        compact = compact,
                        onClick = { onAssetClick(item.ticker) }
                    )
                    if (index < items.take(if (compact) 4 else 8).lastIndex) {
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.08f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketMoverRow(
    item: MarketRankingItem,
    asset: B3AssetData?,
    positive: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val accent = if (positive) SuccessGreen else DangerRed
    val ticker = item.ticker.ifBlank { "—" }
    val badge = remember(ticker) { ticker.take(2).uppercase(Locale.ROOT) }
    val priceText = marketMoverPriceText(item, asset)
    val changeText = marketMoverChangeText(item, positive)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = if (compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 34.dp else 42.dp)
                .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                .border(1.dp, BorderColor.copy(alpha = 0.07f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge,
                color = TextPrimary,
                fontSize = if (compact) 11.sp else 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ticker,
                color = TextPrimary,
                fontSize = if (compact) 15.sp else 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = item.name.ifBlank { item.explanation }.ifBlank { item.source }
            if (!compact && subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = priceText,
            color = TextPrimary,
            fontSize = if (compact) 12.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 70.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = changeText,
            color = accent,
            fontSize = if (compact) 13.sp else 16.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 72.dp)
        )
    }
}

private fun marketMoverPriceText(item: MarketRankingItem, asset: B3AssetData? = null): String {
    val raw = item.priceDisplay.ifBlank { item.displayValue.takeIf { it.contains("R$", ignoreCase = true) }.orEmpty() }
    return when {
        raw.startsWith("R$", ignoreCase = true) -> raw
        item.price > 0.0 -> "R$ ${String.format(Locale("pt", "BR"), "%.2f", item.price)}"
        asset?.price?.let { it > 0.0 } == true -> "R$ ${String.format(Locale("pt", "BR"), "%.2f", asset.price)}"
        raw.contains("R$", ignoreCase = true) -> raw
        else -> "—"
    }
}

private fun marketMoverChangeText(item: MarketRankingItem, positive: Boolean): String {
    val raw = item.changeDisplay.ifBlank { item.displayValue.takeIf { it.contains("%") }.orEmpty() }
    val cleaned = raw.trim()
    if (cleaned.isNotBlank()) {
        val withoutSign = cleaned.removePrefix("▲").removePrefix("▼").removePrefix("+").removePrefix("-").trim()
        return if (positive) "▲ $withoutSign" else "▼ $withoutSign"
    }
    val value = when {
        item.changePercent != 0.0 -> abs(item.changePercent)
        item.value != 0.0 && (item.direction.equals("alta", true) || item.direction.equals("baixa", true)) -> abs(item.value)
        else -> 0.0
    }
    return if (value > 0.0) {
        val text = String.format(Locale("pt", "BR"), "%.2f%%", value)
        if (positive) "▲ $text" else "▼ $text"
    } else {
        if (positive) "▲ —" else "▼ —"
    }
}

private fun rankingValueText(item: MarketRankingItem, positive: Boolean?): String {
    if (positive != null) return marketMoverChangeText(item, positive)
    val display = item.displayValue.trim()
    if (display.isNotBlank() && display != "—") return display
    val value = item.value
    return when {
        item.grade.isNotBlank() -> item.grade
        value == 0.0 -> "—"
        abs(value) <= 1.0 -> String.format(Locale("pt", "BR"), "%.2f", value)
        abs(value) <= 100.0 -> String.format(Locale("pt", "BR"), "%.2f", value)
        else -> String.format(Locale("pt", "BR"), "%.0f", value)
    }
}

@Composable
fun RankingsInlineNotice(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(16.dp))
            .border(1.dp, BorderColor.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(message, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun RankingsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(22.dp))
            .border(1.dp, BorderColor.copy(alpha = 0.06f), RoundedCornerShape(22.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(GoldPrimary.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Leaderboard, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Rankings ainda não carregados", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Atualize a tela ou aguarde a resposta do Valorae Proxy. O app mantém a carteira, cache local e Insights funcionando mesmo com ranking parcial.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RankingWarningsCard(warnings: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(18.dp))
            .border(1.dp, BorderColor.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text("AVISOS DO PROXY", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(8.dp))
        warnings.take(6).forEach { warning ->
            Text("• $warning", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(bottom = 5.dp))
        }
    }
}
