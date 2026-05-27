package com.example.network

import org.json.JSONObject
import org.json.JSONArray

data class AssetChartBundle(
    val ticker: String,
    val type: String,
    val priceHistory: List<ChartPoint> = emptyList(),
    val profitability: List<AssetPeriodReturn> = emptyList(),
    val realProfitability: List<AssetPeriodReturn> = emptyList(),
    val indicatorCards: List<AssetIndicatorPoint> = emptyList(),
    val indicatorHistory: Map<String, List<AssetIndicatorPoint>> = emptyMap(),
    val dividendEvents: List<DividendEvent> = emptyList(),
    val dividendMonthly: List<AssetIndicatorPoint> = emptyList(),
    val dividendYearly: List<AssetIndicatorPoint> = emptyList(),
    val dividendYieldHistory: List<AssetIndicatorPoint> = emptyList(),
    val indexComparison: List<AssetComparisonSeries> = emptyList(),
    val commodityComparison: List<AssetComparisonSeries> = emptyList(),
    val revenueProfit: List<FinancialStatementPoint> = emptyList(),
    val profitVsQuote: List<AssetComparisonPoint> = emptyList(),
    val equityEvolution: List<FinancialStatementPoint> = emptyList(),
    val payoutHistory: List<AssetIndicatorPoint> = emptyMap<String, List<AssetIndicatorPoint>>().values.flatten(), // Allow List
    val revenueByRegion: Map<String, List<AssetBreakdownPoint>> = emptyMap(),
    val revenueByBusiness: Map<String, List<AssetBreakdownPoint>> = emptyMap(),
    val fiiDistribution12m: List<AssetIndicatorPoint> = emptyList(),
    val fiiPeerAverage: List<AssetComparisonPoint> = emptyList(),
    val fiiPatrimonialInfo: List<AssetIndicatorPoint> = emptyList(),
    val fiiAssetDistribution: Map<String, List<AssetBreakdownPoint>> = emptyMap(),
    val warnings: List<String> = emptyList(),
    val source: String = "Valorae Proxy / Investidor10"
)

data class AssetPeriodReturn(
    val period: String,
    val valuePercent: Double,
    val label: String = period,
    val kind: String = "nominal"
)

data class AssetIndicatorPoint(
    val label: String,
    val value: Double,
    val display: String = "",
    val unit: String = "",
    val year: String = "",
    val period: String = "",
    val source: String = "Valorae Proxy"
)

data class AssetComparisonSeries(
    val name: String,
    val points: List<AssetComparisonPoint>
)

data class AssetComparisonPoint(
    val label: String,
    val value: Double,
    val secondaryValue: Double = 0.0,
    val dateMillis: Long = 0L
)

data class FinancialStatementPoint(
    val label: String,
    val year: String,
    val quarter: String = "",
    val netRevenue: Double = 0.0,
    val cost: Double = 0.0,
    val grossProfit: Double = 0.0,
    val ebitda: Double = 0.0,
    val ebit: Double = 0.0,
    val netProfit: Double = 0.0,
    val netWorth: Double = 0.0,
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0
)

data class AssetBreakdownPoint(
    val name: String,
    val valuePercent: Double,
    val displayValue: String = "",
    val year: String = ""
)
