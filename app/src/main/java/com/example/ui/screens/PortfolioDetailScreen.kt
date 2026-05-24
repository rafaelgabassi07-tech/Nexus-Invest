package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.PortfolioSummary
import com.example.viewmodel.AssetSummary
import com.example.ui.components.SegmentedAllocationBar

@Composable
fun PortfolioDetailScreen(
    summary: PortfolioSummary,
    assets: List<AssetSummary>,
    hideValues: Boolean = false,
    onBack: () -> Unit
) {
    val sortedAssets = remember(assets) {
        assets.sortedByDescending { it.totalCurrentValue }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Sticky/Fixed standardized header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground)
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 12.dp),
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
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Patrimônio Consolidado",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Visão unificada do seu portfólio e performance",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Main Value Display
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VALOR TOTAL DA CARTEIRA",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (hideValues) "R$ •••••••" else "R$ ${String.format("%,.2f", summary.totalCurrentValue)}",
                    color = TextPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val retPct = summary.returnPercent
                val isPositive = retPct >= 0
                Surface(
                    color = (if (isPositive) SuccessGreen else DangerRed).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, (if (isPositive) SuccessGreen else DangerRed).copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = if (isPositive) SuccessGreen else DangerRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hideValues) "••%" else String.format("%+.2f%%", retPct),
                            color = if (isPositive) SuccessGreen else DangerRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Unified Portfolio Stats Card
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val avgDy = if (summary.totalCurrentValue > 0) {
                        (assets.sumOf { it.totalCurrentValue * it.dividendYield } / summary.totalCurrentValue)
                    } else 0.0

                    // Row 1: Total Investido & Retorno Real
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.1f)) {
                            Text(
                                text = "TOTAL INVESTIDO",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (hideValues) "R$ •••••••" else "R$ ${String.format("%,.2f", summary.totalInvested)}",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column(
                            modifier = Modifier.weight(0.9f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "RETORNO REAL",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (hideValues) "R$ •••••••" else "R$ ${String.format("%,.2f", summary.totalReturn)}",
                                color = if (summary.totalReturn >= 0) SuccessGreen else DangerRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.08f), thickness = 1.dp)

                    // Row 2: Yield Médio & Ativos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.1f)) {
                            Text(
                                text = "YIELD MÉDIO",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format("%.2f%%", avgDy),
                                color = GoldPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column(
                            modifier = Modifier.weight(0.9f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "ATIVOS EM CARTEIRA",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${assets.size} papéis",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Allocation Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "DISTRIBUIÇÃO DE CATEGORIAS",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                SegmentedAllocationBar(
                    stockValue = if (summary.totalCurrentValue > 0) summary.totalStocksCurrent else summary.totalStocksInvested,
                    fiiValue = if (summary.totalCurrentValue > 0) summary.totalFiisCurrent else summary.totalFiisInvested,
                    modifier = Modifier.height(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    AllocationLegendItem(
                        label = "Ações",
                        value = if (hideValues) "R$ •••" else "R$ ${String.format("%,.2f", summary.totalStocksCurrent)}",
                        percent = (summary.sharesRatioStock * 100).toInt(),
                        color = GoldPrimary
                    )
                    AllocationLegendItem(
                        label = "FIIs",
                        value = if (hideValues) "R$ •••" else "R$ ${String.format("%,.2f", summary.totalFiisCurrent)}",
                        percent = (summary.sharesRatioFii * 100).toInt(),
                        color = SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ANALYTICAL INSIGHT PANEL: Advanced performance summary
            val profitableCount = assets.count { it.totalReturn >= 0 }
            val losingCount = assets.count { it.totalReturn < 0 }
            
            val biggestAsset = assets.maxByOrNull { it.totalCurrentValue }
            val biggestAssetPercent = biggestAsset?.let { 
                if (summary.totalCurrentValue > 0) (it.totalCurrentValue / summary.totalCurrentValue) * 100 else 0.0 
            } ?: 0.0
            val biggestAssetLabel = biggestAsset?.let { "${it.ticker} (${String.format("%.1f%%", biggestAssetPercent)})" } ?: "Nenhum"
            
            val highestYieldAsset = assets.maxByOrNull { it.dividendYield }
            val highestYieldLabel = highestYieldAsset?.let { "${it.ticker} (${String.format("%.2f%%", it.dividendYield * 100)})" } ?: "Nenhum"
            
            val bestReturnAsset = assets.maxByOrNull { it.totalReturn }
            val bestReturnLabel = bestReturnAsset?.let { 
                "${it.ticker} (${if (hideValues) "R$ •••" else "R$ " + String.format("%,.0f", it.totalReturn)})" 
            } ?: "Nenhum"

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "MÉTRICAS METODOLÓGICAS",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Ratio de Acerto (Ativos)", fontSize = 12.sp, color = TextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.background(SuccessGreen.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("$profitableCount em alta", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.background(DangerRed.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("$losingCount em baixa", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Concentração Principal", fontSize = 12.sp, color = TextSecondary)
                            Text(biggestAssetLabel, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Líder de Dividend Yield", fontSize = 12.sp, color = TextSecondary)
                            Text(highestYieldLabel, fontSize = 12.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Melhor Retorno Nominal", fontSize = 12.sp, color = TextSecondary)
                            Text(bestReturnLabel, fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 2: PORTFOLIO COMPOSITION COMPARISON snapshot table
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "DESEMPENHO DETALHADO POR ATIVO",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ativo", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("Peso / Valor", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                            Text("Rentab.", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                            Text("Yield", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                        }
                        
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.dp)
                        
                        if (assets.isEmpty()) {
                            Text(
                                text = "Nenhum ativo de custódia disponível",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            sortedAssets.forEachIndexed { index, asset ->
                                val weight = if (summary.totalCurrentValue > 0) (asset.totalCurrentValue / summary.totalCurrentValue) * 100 else 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(asset.ticker, color = GoldPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                        Text(if (asset.type == "ACAO") "Ação" else "FII", color = TextSecondary, fontSize = 10.sp)
                                    }
                                    
                                    Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                                        Text(String.format("%.1f%%", weight), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(if (hideValues) "R$ •••" else "R$ ${String.format("%,.0f", asset.totalCurrentValue)}", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    
                                    val assetReturn = asset.totalReturn
                                    val isAssetPositive = assetReturn >= 0
                                    Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (hideValues) "••%" else String.format("%+.1f%%", asset.returnPercent),
                                            color = if (isAssetPositive) SuccessGreen else DangerRed,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (hideValues) "R$ •••" else "R$ ${String.format("%,.0f", assetReturn)}",
                                            color = (if (isAssetPositive) SuccessGreen else DangerRed).copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    
                                    Text(
                                        text = String.format("%.1f%%", asset.dividendYield * 100),
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(0.8f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                if (index < assets.size - 1) {
                                    HorizontalDivider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Helpful Insight Card
            Surface(
                color = GoldPrimary.copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Sua carteira está diversificada em ${assets.size} ativos. O equilíbrio entre Ações (${(summary.sharesRatioStock * 100).toInt()}%) e FIIs (${(summary.sharesRatioFii * 100).toInt()}%) ajuda na resiliência do portfólio.",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Footer Info
            Text(
                text = "VALORAE ANALYTICS • 2026",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = TextSecondary.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun DetailStatCard(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.12f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun AllocationLegendItem(
    label: String,
    value: String,
    percent: Int,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = "$label ($percent%)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}
