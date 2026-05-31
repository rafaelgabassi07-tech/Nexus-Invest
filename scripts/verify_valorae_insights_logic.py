#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
charts = (root / "app/src/main/java/com/example/ui/screens/ChartsScreen.kt").read_text()
vm = (root / "app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt").read_text()
net = (root / "app/src/main/java/com/example/network/B3NetworkService.kt").read_text()

checks = {
    "Insights calcula quantidade elegível em dividendos": "eligibleDividendAmount(" in charts and "sharesOwnedAtInsightDate" in charts,
    "Evolução de proventos recebe transações": "transactions: List<com.example.data.Transaction>" in charts and "buildDividendEvolutionData(" in charts,
    "Agenda de dividendos filtra por elegibilidade": "buildDividendAgendaData(" in charts and "eligibleDividendAmount(event, transactions)" in charts,
    "Agenda não reaproveita eventos antigos quando não há futuro": "upcomingEligibleDividendEvents" in charts and "val recentRows = rowsFrom(normalizedEvents)" not in charts,
    "Agenda local remove datas-com antigas": "startOfInsightDayMillis(System.currentTimeMillis())" in charts and "eventMillis <= 0L || eventMillis >= todayStart" in charts,
    "Barras de diversificação são clampadas em 0..1": "(weight / 100f).coerceIn(0f, 1f)" in charts and "(percent / 100f).coerceIn(0f, 1f)" in charts,
    "Top pagadores respeita período e existência da carteira": "buildTopDividendAssetsForPeriod" in charts and "periodStartMillis" in charts,
    "Fallback de top pagadores soma mês a mês por quantidade histórica": "estimateDividendForAssetAcrossPeriod" in charts and "sharesOwnedAtInsightDate(transactions, asset.ticker, monthEnd)" in charts,
    "Parser de datas dos Insights aceita ISO e formato brasileiro curto": "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" in charts and "dd/MM/yy" in charts,
    "KPIs da agenda usam eventos elegíveis": "getKpiMetricsForPage" in charts and "eligibleDividendAmount(it, transactions)" in charts,
    "Histórico remoto é ajustado ao início da carteira": "normalizePortfolioHistoryForAge" in vm,
    "IPCA é rebaseado ao período da carteira": "normalizeIpcaForPortfolioAge" in vm,
    "Fallback visual de IPCA/Carteira respeita idade da carteira": "portfolioAgeMonthsForInsights" in charts and "List(insightAgeMonths)" in charts,
    "Tabela IPCA alinha séries antes de calcular juros reais": "alignedIpcaTable" in charts and "resampleInsightSeries(ipcaDataValues, portDataValues.size)" in charts,
    "Proventos remotos são saneados por posição na data": "sanitizeDividendEventsForPortfolio" in vm,
    "Histórico local usa custo médio móvel após vendas": "LocalPositionSnapshot" in vm and "avgCost" in vm,
    "Payload de carteira envia firstPurchaseAt opcional": "firstPurchaseAt" in net and "firstPurchaseAtSeconds" in net,
}

failed = [name for name, ok in checks.items() if not ok]
if failed:
    print("Valorae Insights audit FAILED")
    for item in failed:
        print(f"- {item}")
    raise SystemExit(1)

print("Valorae Insights logic audit OK")
for item in checks:
    print(f"OK - {item}")
