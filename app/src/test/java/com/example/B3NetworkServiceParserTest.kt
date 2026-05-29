package com.example

import com.example.network.B3NetworkService
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Test
import java.io.File

@RunWith(RobolectricTestRunner::class)
class B3NetworkServiceParserTest {

    @Test
    fun testParseLocaleFinancialNumber() {
        val parser = B3NetworkService

        assertEquals(4.32 * 1_000_000_000, parser.parseLocaleFinancialNumber("R\$ 4,32 Bilhões"), 0.01)
        assertEquals(4.32 * 1_000_000_000, parser.parseLocaleFinancialNumber("4,32 Bilhões"), 0.01)
        assertEquals(452.42 * 1_000_000, parser.parseLocaleFinancialNumber("452,42 Milhões"), 0.01)
        assertEquals(1754.29, parser.parseLocaleFinancialNumber("1.754,29%"), 0.01) // Depending on implementation it might strip % and keep 1754.29 or 17.5429. Our implementation strips % and uses double.
        assertEquals(0.10000000, parser.parseLocaleFinancialNumber("0,10000000"), 0.000001)
        assertEquals(0.0, parser.parseLocaleFinancialNumber("-"), 0.0)
        assertEquals(0.0, parser.parseLocaleFinancialNumber("--"), 0.0)
    }

    @Test
    fun testResponsePetr4Json() {
        val file = File("../response_petr4.json")
        if (!file.exists()) {
            println("File ../response_petr4.json not found, skipping PETR4 full parse test.")
            return
        }

        val jsonString = file.readText()
        val root = JSONObject(jsonString)

        val bundle = B3NetworkService.parseAssetChartBundle("PETR4", false, root, emptyList())

        // Basic assertions based on the prompt's structural hints
        assertEquals("PETR4", bundle.ticker)
        assertTrue(bundle.profitability.isNotEmpty() || bundle.realProfitability.isNotEmpty())
        assertTrue(bundle.indicatorCards.isNotEmpty())
        assertTrue(bundle.dividendEvents.isNotEmpty() || bundle.dividendYearly.isNotEmpty())
        // Should parse results.chartsFinanceiros.receitasLucros or sections.demonstrativos.receitasLucros
        assertTrue(bundle.revenueProfit.isNotEmpty())
        assertTrue(bundle.profitVsQuote.isNotEmpty())
        // Should parse results.chartsFinanceiros.evolucaoPatrimonio or sections.demonstrativos.evolucaoPatrimonio
        assertTrue(bundle.equityEvolution.isNotEmpty())
        assertTrue(bundle.payoutHistory.isNotEmpty())
        assertTrue(bundle.revenueByRegion.isNotEmpty())
        assertTrue(bundle.revenueByBusiness.isNotEmpty())
    }

    @Test
    fun testPartialAndEmptyArrays() {
        val jsonString = """
            {
                "status": "PARTIAL",
                "warnings": ["Data is partial"],
                "results": {
                    "sections": {
                        "demonstrativos": {
                            "receitasLucros": [],
                            "evolucaoPatrimonio": [],
                            "payoutHistorico": {
                                "years": [],
                                "payOutCompanyIndicators": [],
                                "dyTickerIndicators": []
                            }
                        },
                        "empresa": {
                            "regioesReceita": {},
                            "negociosReceita": {}
                        }
                    },
                    "historicoDividendos": []
                }
            }
        """.trimIndent()

        val root = JSONObject(jsonString)
        val bundle = B3NetworkService.parseAssetChartBundle("TEST3", false, root, emptyList())

        assertTrue(bundle.warnings.contains("Data is partial"))
        assertTrue(bundle.revenueProfit.isEmpty())
        assertTrue(bundle.equityEvolution.isEmpty())
        assertTrue(bundle.payoutHistory.isEmpty())
        assertTrue(bundle.dividendEvents.isEmpty())
        assertTrue(bundle.revenueByRegion.isEmpty())
        assertTrue(bundle.revenueByBusiness.isEmpty())
    }
    @Test
    fun testIndexComparisonParserAcceptsMultipleShapes() {
        val jsonString = """
            {
                "status": "OK",
                "ticker": "PETR4",
                "results": {
                    "sections": {
                        "comparacaoIndices": {
                            "series": [
                                {"name": "PETR4", "points": [{"date": "2025-01", "value": 0}, {"date": "2025-02", "value": 4.5}]},
                                {"name": "IBOVESPA", "points": [{"date": "2025-01", "value": 0}, {"date": "2025-02", "value": 2.1}]},
                                {"name": "IPCA", "data": [{"x": "2025-01", "y": 0}, {"x": "2025-02", "y": 0.9}]}
                            ]
                        }
                    }
                }
            }
        """.trimIndent()

        val bundle = B3NetworkService.parseAssetChartBundle("PETR4", false, JSONObject(jsonString), emptyList())

        assertTrue(bundle.indexComparison.any { it.name == "PETR4" && it.points.size == 2 })
        assertTrue(bundle.indexComparison.any { it.name == "IBOV" && it.points.last().value == 2.1 })
        assertTrue(bundle.indexComparison.any { it.name == "IPCA" && it.points.last().value == 0.9 })
    }

    @Test
    fun testIndexComparisonParserNormalizesRawPriceLevels() {
        val jsonString = """
            {
                "status": "OK",
                "ticker": "PETR4",
                "results": {
                    "sections": {
                        "comparacaoIndices": {
                            "series": [
                                {"name": "PETR4", "points": [{"date": "2025-01", "price": 40.0}, {"date": "2025-02", "price": 44.0}]},
                                {"name": "IBOVESPA", "points": [{"date": "2025-01", "value": 120000.0}, {"date": "2025-02", "value": 126000.0}]}
                            ]
                        }
                    }
                }
            }
        """.trimIndent()

        val bundle = B3NetworkService.parseAssetChartBundle("PETR4", false, JSONObject(jsonString), emptyList())
        val petr = bundle.indexComparison.first { it.name == "PETR4" }
        val ibov = bundle.indexComparison.first { it.name == "IBOV" }

        assertEquals(0.0, petr.points.first().value, 0.001)
        assertEquals(10.0, petr.points.last().value, 0.001)
        assertEquals(5.0, ibov.points.last().value, 0.001)
    }

    @Test
    fun testFiiNormalizedFieldsBecomeIndicatorCards() {
        val jsonString = """
            {
                "status": "OK",
                "ticker": "MXRF11",
                "type": "FII",
                "normalized": {
                    "precoAtual": {"value": 9.75, "display": "R$ 9,75"},
                    "dividendYield": {"value": 12.4, "display": "12,40%"},
                    "yield1m": {"value": 0.95},
                    "yield3m": {"value": 2.8},
                    "yield6m": {"value": 5.9},
                    "yield12m": {"value": 12.4},
                    "pvp": {"value": 0.98},
                    "valorPatrimonialCota": {"value": 10.05},
                    "patrimonioLiquido": {"value": 2500000000},
                    "liquidezMediaDiaria": {"value": 15000000},
                    "vacanciaFisica": {"value": 0.0}
                },
                "results": {
                    "segmentoFii": "Papel",
                    "informacoesFundo": {
                        "numeroCotistas": "1.000.000",
                        "cotasEmitidas": "250.000.000",
                        "tipoFundo": "Fundo de Papel"
                    }
                }
            }
        """.trimIndent()

        val bundle = B3NetworkService.parseAssetChartBundle("MXRF11", true, JSONObject(jsonString), emptyList())

        assertEquals("FII", bundle.type)
        assertTrue(bundle.indicatorCards.any { it.label == "Dividend Yield" && it.value == 12.4 })
        assertTrue(bundle.indicatorCards.any { it.label == "P/VP" && it.value == 0.98 })
        assertTrue(bundle.indicatorCards.any { it.label == "VPA" && it.value == 10.05 })
        assertTrue(bundle.indicatorCards.any { it.label == "Patrimônio Líquido" && it.value == 2500000000.0 })
        assertTrue(bundle.indicatorCards.any { it.label == "Yield 12M" && it.value == 12.4 })
        assertTrue(bundle.fiiDistribution12m.any { it.label == "Yield 12M" && it.value == 12.4 })
        assertTrue(bundle.fiiAssetDistribution["Ativos"]?.any { it.name == "Fundo de Papel" || it.name == "Papel" } == true)
    }

    @Test
    fun testFiiDividendObjectBuildsMonthlyAndYieldCharts() {
        val jsonString = """
            {
                "status": "OK",
                "ticker": "MXRF11",
                "type": "FII",
                "normalized": {
                    "precoAtual": {"value": 10.0},
                    "dividendYield": {"value": 12.0},
                    "pvp": {"value": 1.01},
                    "valorPatrimonialCota": {"value": 9.90}
                },
                "results": {
                    "dividendos": {
                        "items": [
                            {"dataCom": "31/01/2026", "dataPagamento": "14/02/2026", "valor": "0,10", "tipo": "Rendimento"},
                            {"dataCom": "28/02/2026", "dataPagamento": "14/03/2026", "valor": "0,10", "tipo": "Rendimento"}
                        ]
                    },
                    "informacoesFundo": {
                        "segmento": "Recebíveis",
                        "numeroCotistas": "1.453.148",
                        "cotasEmitidas": "460.269.531"
                    }
                }
            }
        """.trimIndent()

        val bundle = B3NetworkService.parseAssetChartBundle("MXRF11", true, JSONObject(jsonString), emptyList())

        assertTrue(bundle.dividendEvents.size == 2)
        assertTrue(bundle.dividendMonthly.isNotEmpty())
        assertTrue(bundle.dividendYearly.isNotEmpty())
        assertTrue(bundle.dividendYieldHistory.isNotEmpty())
        assertTrue(bundle.indicatorCards.any { it.label == "Preço Atual" && it.value == 10.0 })
        assertTrue(bundle.indicatorCards.any { it.label == "Cotistas" })
    }

    @Test
    fun testGenericNormalizedIndicatorsAreNotDropped() {
        val jsonString = """
            {
                "status": "OK",
                "ticker": "ABCD11",
                "type": "FII",
                "normalized": {
                    "precoAtual": {"display": "R$ 95,20", "value": 95.2},
                    "variacao12m": {"display": "8,50%", "value": 8.5},
                    "liquidezMediaDiaria": {"display": "R$ 2,10 mi", "value": 2100000},
                    "vacanciaFisica": {"display": "4,20%", "value": 4.2}
                },
                "results": {}
            }
        """.trimIndent()

        val bundle = B3NetworkService.parseAssetChartBundle("ABCD11", true, JSONObject(jsonString), emptyList())

        assertTrue(bundle.indicatorCards.any { it.label == "Preço Atual" && it.display.contains("95") })
        assertTrue(bundle.indicatorCards.any { it.label == "Variação 12M" && it.value == 8.5 })
        assertTrue(bundle.indicatorCards.any { it.label == "Liquidez Média Diária" && it.value == 2100000.0 })
        assertTrue(bundle.indicatorCards.any { it.label == "Vacância Física" && it.value == 4.2 })
    }

    @Test
    fun testMergedRootAndResultsNormalizedForFii() {
        val jsonString = """
            {
                "status": "OK",
                "ticker": "HGLG11",
                "type": "FII",
                "normalized": {
                    "precoAtual": {"value": 165.20, "display": "R$ 165,20"},
                    "pvp": {"value": 0.94}
                },
                "results": {
                    "normalized": {
                        "dividendYield": {"value": 8.7, "display": "8,70%"},
                        "vacanciaFisica": {"value": 0.0, "display": "0,00%"},
                        "valorPatrimonialCota": {"value": 175.0}
                    },
                    "financialSummary": {
                        "keyRatios": {
                            "liquidezMediaDiaria": {"value": 12000000}
                        }
                    }
                }
            }
        """.trimIndent()

        val bundle = B3NetworkService.parseAssetChartBundle("HGLG11", true, JSONObject(jsonString), emptyList())

        assertTrue(bundle.indicatorCards.any { it.label == "Preço Atual" && it.value == 165.2 })
        assertTrue(bundle.indicatorCards.any { it.label == "Dividend Yield" && it.value == 8.7 })
        assertTrue(bundle.indicatorCards.any { it.label == "P/VP" && it.value == 0.94 })
        assertTrue(bundle.indicatorCards.any { it.label == "Vacância Física" && it.value == 0.0 })
        assertTrue(bundle.indicatorCards.any { it.label == "VPA" && it.value == 175.0 })
    }

    @Test
    fun testFundamentalistIndicatorAlternativeShapesAreParsed() {
        val jsonString = """
            {
                "status": "OK",
                "ticker": "TEST3",
                "results": {
                    "indicadoresFundamentalistas": {
                        "comComparativos": {
                            "p_l": "7,50",
                            "p_vp": "1,20",
                            "dividend_yield": "6,10%"
                        }
                    },
                    "fundamentalistIndicators": [
                        {"label": "ROE", "value": "18,5%"},
                        {"label": "Margem Líquida", "value": "22,1%"}
                    ]
                }
            }
        """.trimIndent()

        val bundle = B3NetworkService.parseAssetChartBundle("TEST3", false, JSONObject(jsonString), emptyList())

        assertTrue(bundle.indicatorCards.any { it.label == "P/L" && it.value == 7.5 })
        assertTrue(bundle.indicatorCards.any { it.label == "P/VP" && it.value == 1.2 })
        assertTrue(bundle.indicatorCards.any { it.label == "Dividend Yield" && it.value == 6.1 })
        assertTrue(bundle.indicatorCards.any { it.label == "ROE" && it.value == 18.5 })
        assertTrue(bundle.indicatorCards.any { it.label == "Margem Líquida" && it.value == 22.1 })
    }

}
