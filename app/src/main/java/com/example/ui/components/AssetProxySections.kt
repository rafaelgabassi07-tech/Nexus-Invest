package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.AssetChartBundle
import com.example.network.B3AssetData
import com.example.network.NewsItem
import com.example.ui.B3UIUtils
import com.example.ui.theme.*
import java.util.Locale

data class ProxyDisplayField(
    val label: String,
    val value: String,
    val description: String = ""
)

private fun String.normalizedKey(): String = lowercase(Locale.ROOT)
    .replace("á", "a")
    .replace("à", "a")
    .replace("ã", "a")
    .replace("â", "a")
    .replace("é", "e")
    .replace("ê", "e")
    .replace("í", "i")
    .replace("ó", "o")
    .replace("õ", "o")
    .replace("ô", "o")
    .replace("ú", "u")
    .replace("ç", "c")
    .replace(Regex("[^a-z0-9]+"), "")

private fun MutableList<ProxyDisplayField>.addUnique(label: String, value: String, description: String = "") {
    val cleanValue = value.trim()
    if (cleanValue.isBlank() || cleanValue == "--" || cleanValue == "—") return
    val key = label.normalizedKey()
    if (any { it.label.normalizedKey() == key }) return
    add(ProxyDisplayField(label, cleanValue, description))
}

private fun MutableList<ProxyDisplayField>.addNumber(
    label: String,
    value: Double,
    description: String = "",
    suffix: String = "",
    prefix: String = "",
    precision: Int = 2
) {
    if (!value.isFinite() || value == 0.0) return
    addUnique(label, B3UIUtils.formatValue(value, suffix = suffix, prefix = prefix, precision = precision), description)
}

private fun MutableList<ProxyDisplayField>.addMoney(label: String, value: Double, description: String = "") {
    if (!value.isFinite() || value == 0.0) return
    addUnique(label, B3UIUtils.formatLargeNumber(value), description)
}

fun buildAssetProxyIndicatorFields(
    assetData: B3AssetData?,
    bundle: AssetChartBundle?,
    isFii: Boolean
): List<ProxyDisplayField> {
    val fields = mutableListOf<ProxyDisplayField>()
    assetData?.let { a ->
        fields.addNumber("Preço Atual", a.price, "Cotação atual recebida do VALORAE Proxy", prefix = "R$ ")
        if (a.changePercent.isFinite() && a.changePercent != 0.0) {
            fields.addUnique("Variação", B3UIUtils.formatValue(a.changePercent, suffix = "%"), "Variação recebida pelo Proxy")
        }
        fields.addNumber("Dividend Yield", a.dy, "Retorno em proventos", suffix = "%")
        fields.addNumber("P/VP", a.pvp, "Preço / Valor Patrimonial")
        if (!isFii) fields.addNumber("P/L", a.pl, "Preço / Lucro anual")
        fields.addNumber("VPA", a.vpa, "Valor patrimonial por ação/cota", prefix = "R$ ")
        if (!isFii) fields.addNumber("LPA", a.lpa, "Lucro por ação", prefix = "R$ ")
        fields.addNumber("Último Provento", a.lastDividend, "Último provento retornado pelo Proxy", prefix = "R$ ")
        fields.addMoney("Valor de Mercado", a.marketCap, "Market cap retornado pelo Proxy")
        fields.addMoney("Liquidez Diária", a.dailyLiquidity, "Volume financeiro/liquidez retornado pelo Proxy")
        fields.addNumber("Mínima 52 Semanas", a.low52, "Menor cotação em 52 semanas", prefix = "R$ ")
        fields.addNumber("Máxima 52 Semanas", a.high52, "Maior cotação em 52 semanas", prefix = "R$ ")

        if (isFii) {
            fields.addNumber("Vacância", a.fiiVacancy, "Vacância física/financeira informada pelo Proxy", suffix = "%", precision = 1)
            if (a.fiiPropertyCount > 0) fields.addUnique("Número de Imóveis", "${a.fiiPropertyCount} imóveis", "Quantidade de ativos físicos informada pelo Proxy")
            fields.addUnique("Segmento", a.fiiSegment, "Segmento do FII")
            fields.addUnique("Cotistas", a.fiiTotalHolders, "Número de cotistas")
            fields.addUnique("Cotas Emitidas", a.fiiIssuedShares, "Quantidade de cotas emitidas")
            fields.addUnique("Taxa de Administração", a.fiiAdminFee, "Taxa informada pelo administrador")
            fields.addUnique("Tipo de Fundo", a.fiiFundType, "Tipo informado no perfil do fundo")
            fields.addUnique("Mandato", a.fiiMandate, "Mandato do fundo")
            fields.addUnique("Público-alvo", a.fiiTargetAudience, "Público-alvo informado")
            fields.addUnique("Tipo de Gestão", a.fiiManagementType, "Gestão ativa/passiva ou equivalente")
            fields.addUnique("Prazo de Duração", a.fiiDuration, "Prazo informado pelo Proxy")
            fields.addNumber("Magic Number", a.magicNumber, "Quantidade de cotas calculada pelo serviço de dados")
        } else {
            fields.addNumber("P/Receita (PSR)", a.priceToSales, "Preço / Receita Líquida")
            fields.addNumber("Margem Líquida", a.margins, "Eficiência líquida", suffix = "%")
            fields.addNumber("Margem Bruta", a.grossMargin, "Eficiência bruta", suffix = "%")
            fields.addNumber("Margem EBIT", a.ebitMargin, "Eficiência EBIT", suffix = "%")
            fields.addNumber("Margem EBITDA", a.ebitdaMargin, "Eficiência EBITDA", suffix = "%")
            fields.addNumber("EV/EBITDA", a.evEbitda, "Valor da firma / EBITDA")
            fields.addNumber("EV/EBIT", a.evEbit, "Valor da firma / EBIT")
            fields.addNumber("P/EBITDA", a.priceEbitda, "Preço / EBITDA")
            fields.addNumber("P/EBIT", a.priceEbit, "Preço / EBIT")
            fields.addNumber("P/Ativo", a.priceAsset, "Preço / Ativo Total")
            fields.addNumber("P/Cap. Giro", a.priceCapGiro, "Preço / Capital de Giro")
            fields.addNumber("P/Ativo Circ. Líq.", a.priceAtivoCircLiq, "Preço / Ativo Circulante Líquido")
            fields.addNumber("Giro Ativos", a.giroAtivos, "Giro de ativos")
            fields.addNumber("ROE", a.roe, "Retorno sobre patrimônio líquido", suffix = "%")
            fields.addNumber("ROIC", a.roic, "Retorno sobre capital investido", suffix = "%")
            fields.addNumber("ROA", a.roa, "Retorno sobre ativos", suffix = "%")
            fields.addNumber("Dívida Líq./Patrimônio", a.divLiqPatrimonio, "Dívida líquida / patrimônio")
            fields.addNumber("Dívida Líq./EBITDA", a.debtEbitda, "Dívida líquida / EBITDA")
            fields.addNumber("Dívida Líq./EBIT", a.divLiqEbit, "Dívida líquida / EBIT")
            fields.addNumber("Dívida Bruta/Patrimônio", a.divBrutaPatrimonio, "Dívida bruta / patrimônio")
            fields.addNumber("Patrimônio/Ativos", a.patrimonioAtivos, "Patrimônio / ativos")
            fields.addNumber("Passivos/Ativos", a.passivosAtivos, "Passivos / ativos")
            fields.addNumber("Liquidez Corrente", a.liquidezCorrente, "Liquidez corrente")
            fields.addNumber("CAGR Receitas 5a", a.cagrRevenue5y, "Crescimento anual composto de receitas", suffix = "%")
            fields.addNumber("CAGR Lucros 5a", a.cagrProfit5y, "Crescimento anual composto de lucros", suffix = "%")
            fields.addNumber("Payout", a.payout, "Parcela do lucro distribuída", suffix = "%")
            fields.addMoney("Valor de Firma", a.firmValue, "Valor de firma retornado pelo Proxy")
            fields.addMoney("Patrimônio Líquido", a.netWorth, "Patrimônio líquido")
            fields.addMoney("Ativos Totais", a.totalAssets, "Ativos totais")
            fields.addMoney("Ativo Circulante", a.currentAssets, "Ativo circulante")
            fields.addMoney("Dívida Bruta", a.grossDebt, "Dívida bruta")
            fields.addMoney("Dívida Líquida", a.netDebt, "Dívida líquida")
            fields.addMoney("Disponibilidade", a.availability, "Disponibilidade financeira")
            fields.addNumber("Free Float", a.freeFloat, "Ações em circulação", suffix = "%")
            fields.addNumber("Tag Along", a.tagAlong, "Tag along informado", suffix = "%")
        }
    }

    bundle?.indicatorCards
        ?.filter { it.label.isNotBlank() && (it.display.isNotBlank() || (it.value.isFinite() && it.value != 0.0)) }
        ?.forEach { point ->
            val valueText = point.display.ifBlank {
                when (point.unit.uppercase(Locale.ROOT)) {
                    "%" -> B3UIUtils.formatValue(point.value, suffix = "%")
                    "BRL", "R$" -> B3UIUtils.formatValue(point.value, prefix = "R$ ")
                    "NUMBER", "NUMERO", "NÚMERO" -> B3UIUtils.formatLargeNumber(point.value).replace("R$ ", "")
                    else -> B3UIUtils.formatValue(point.value)
                }
            }
            fields.addUnique(point.label, valueText, point.source.ifBlank { "Indicador normalizado pelo VALORAE Proxy" })
        }
    return fields
}

fun buildAssetProxyProfileFields(
    assetData: B3AssetData?,
    bundle: AssetChartBundle?,
    isFii: Boolean
): List<ProxyDisplayField> {
    val fields = mutableListOf<ProxyDisplayField>()
    assetData?.let { a ->
        fields.addUnique("Ticker", a.ticker, "Código negociado")
        fields.addUnique("Nome", a.name, "Nome retornado pelo Proxy")
        fields.addUnique("Classe", if (isFii) "FII" else "Ação", "Classe inferida/retornada pelo Proxy")
        fields.addUnique("Fonte", a.source, "Origem do snapshot exibido")
        fields.addUnique("CNPJ", a.cnpj, "Cadastro do ativo")
        fields.addUnique("Setor/Subsetor", a.subSector, "Subsetor de atuação")
        fields.addUnique("Segmento", if (isFii) a.fiiSegment else a.listSegment, "Segmento retornado pelo Proxy")
        fields.addUnique("Ano de Fundação", a.foundationYear, "Ano de fundação")
        fields.addUnique("Estreia na Bolsa", a.listingYear, "Ano de listagem")
        fields.addUnique("Funcionários", a.employeesCount, "Número de funcionários")
        fields.addUnique("Total de Papéis", a.totalPapers, "Quantidade total de ações/cotas")
        if (isFii) {
            fields.addUnique("Cotistas", a.fiiTotalHolders, "Número de cotistas")
            fields.addUnique("Cotas Emitidas", a.fiiIssuedShares, "Cotas emitidas")
            fields.addUnique("Taxa de Administração", a.fiiAdminFee, "Taxa de administração")
            fields.addUnique("Tipo de Fundo", a.fiiFundType, "Tipo do FII")
            fields.addUnique("Mandato", a.fiiMandate, "Mandato do fundo")
            fields.addUnique("Público-alvo", a.fiiTargetAudience, "Público-alvo")
            fields.addUnique("Gestão", a.fiiManagementType, "Tipo de gestão")
            fields.addUnique("Prazo", a.fiiDuration, "Prazo de duração")
        }
    }
    bundle?.fiiPatrimonialInfo
        ?.filter { it.label.isNotBlank() && it.display.isNotBlank() }
        ?.forEach { fields.addUnique(it.label, it.display, it.source.ifBlank { "Informação patrimonial do Proxy" }) }
    return fields
}

@Composable
fun AssetProxyIndicatorSection(
    assetData: B3AssetData?,
    bundle: AssetChartBundle?,
    isFii: Boolean,
    modifier: Modifier = Modifier
) {
    val fields = remember(assetData, bundle, isFii) { buildAssetProxyIndicatorFields(assetData, bundle, isFii) }
    
    if (fields.isEmpty()) {
        ProxyFieldCard(
            title = "INDICADORES GERAIS",
            subtitle = "Renderização dinâmica dos indicadores recebidos pelo VALORAE Proxy. Campos ausentes não são preenchidos com estimativas.",
            fields = emptyList(),
            emptyTitle = "Indicadores indisponíveis",
            emptyMessage = "O Proxy não retornou indicadores suficientes para este ativo no momento.",
            modifier = modifier
        )
        return
    }

    // Classify fields based on normalized label keys to show highly organized structural cards
    val valuationKeys = setOf(
        "precoatual", "variacao", "dividendyield", "pvp", "pl", "vpa", "lpa", "ultimoprovento", 
        "magicnumber", "magicnumbercalculado", "tagalong", "freefloat"
    )
    val efficiencyKeys = setOf(
        "roe", "roic", "roa", "margemliquida", "margembruta", "margemebit", "margemebitda", "payout", "giroativos"
    )
    val debtKeys = setOf(
        "dividaliquidapatrimonio", "dividaliquidaebitda", "dividaliquidaebit", "dividabrutapatrimonio", 
        "passivosativos", "patrimonioativos", "liquidezcorrente"
    )
    val balanceKeys = setOf(
        "valordemercado", "liquidezdiaria", "valordefirma", "patrimonioliquido", "ativostotais", "ativocirculante", 
        "dividabruta", "dividaliquida", "disponibilidade"
    )
    val fiiKeys = setOf(
        "vacancia", "numerodeimoveis", "segmento", "cotistas", "cotasemitidas", 
        "taxadeadministracao", "tipodefundo", "mandato", "publicoalvo", "gestao", "prazodeduracao"
    )

    val valuationList = mutableListOf<ProxyDisplayField>()
    val efficiencyList = mutableListOf<ProxyDisplayField>()
    val debtList = mutableListOf<ProxyDisplayField>()
    val balanceList = mutableListOf<ProxyDisplayField>()
    val fiiList = mutableListOf<ProxyDisplayField>()
    val othersList = mutableListOf<ProxyDisplayField>()

    fields.forEach { field ->
        val key = field.label.normalizedKey()
        when {
            valuationKeys.contains(key) -> valuationList.add(field)
            efficiencyKeys.contains(key) -> efficiencyList.add(field)
            debtKeys.contains(key) -> debtList.add(field)
            balanceKeys.contains(key) -> balanceList.add(field)
            fiiKeys.contains(key) -> fiiList.add(field)
            else -> othersList.add(field)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (valuationList.isNotEmpty()) {
            GroupedProxySectionCard(
                title = "VALUATION & PREÇO",
                subtitle = "Indicadores de valor relativo, múltiplos e proventos vigentes do ativo.",
                fields = valuationList
            )
        }
        if (efficiencyList.isNotEmpty()) {
            GroupedProxySectionCard(
                title = "EFICIÊNCIA & LUCRATIVIDADE",
                subtitle = "Indicadores de margens operacionais e retorno sobre capital empregado.",
                fields = efficiencyList
            )
        }
        if (debtList.isNotEmpty()) {
            GroupedProxySectionCard(
                title = "ENDIVIDAMENTO & LIQUIDEZ",
                subtitle = "Índices de alavancagem financeira, endividamento e liquidez de curto prazo.",
                fields = debtList
            )
        }
        if (balanceList.isNotEmpty()) {
            GroupedProxySectionCard(
                title = "TAMANHO & BALANÇO",
                subtitle = "Dados absolutos de ativos, disponibilidades, dívida e valor corporativo.",
                fields = balanceList
            )
        }
        if (isFii && fiiList.isNotEmpty()) {
            GroupedProxySectionCard(
                title = "DADOS FÍSICOS & IMÓVEIS (FII)",
                subtitle = "Características operacionais, vacância e dados físicos dos ativos imobiliários.",
                fields = fiiList
            )
        }
        if (othersList.isNotEmpty()) {
            GroupedProxySectionCard(
                title = "CONTRATOS & ADICIONAIS",
                subtitle = "Outras métricas normalizadas pelo Proxy de dados do VALORAE.",
                fields = othersList
            )
        }
        AssetIndicatorHistorySection(bundle = bundle)
    }
}

@Composable
private fun AssetIndicatorHistorySection(bundle: AssetChartBundle?) {
    val history = remember(bundle) {
        bundle?.indicatorHistory
            ?.filterValues { points -> points.count { it.value.isFinite() } >= 2 }
            ?.toList()
            ?.sortedBy { it.first }
            ?.take(8)
            ?: emptyList()
    }
    if (history.isEmpty()) return
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "HISTÓRICO DE INDICADORES FUNDAMENTALISTAS",
                color = GoldPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "Séries históricas reais retornadas pelo VALORAE Proxy/Investidor10. Indicadores sem pontos suficientes não são exibidos.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            history.forEach { (name, points) ->
                val sorted = points.sortedBy { it.year.ifBlank { it.period.ifBlank { it.label } } }.takeLast(6)
                val latest = sorted.lastOrNull()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = sorted.joinToString(" • ") { it.year.ifBlank { it.period.ifBlank { it.label } } },
                            color = TextSecondary,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = latest?.display?.takeIf { it.isNotBlank() } ?: latest?.let { B3UIUtils.formatValue(it.value, suffix = it.unit.takeIf { u -> u == "%" }.orEmpty()) }.orEmpty(),
                        color = GoldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                val max = sorted.maxOfOrNull { kotlin.math.abs(it.value) }?.takeIf { it > 0.0 } ?: 1.0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                    sorted.forEach { point ->
                        val height = ((kotlin.math.abs(point.value) / max) * 34.0).coerceIn(4.0, 34.0).dp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(height)
                                .background(GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupedProxySectionCard(
    title: String,
    subtitle: String,
    fields: List<ProxyDisplayField>,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.10f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title, 
                    color = GoldPrimary, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Black, 
                    letterSpacing = 0.8.sp
                )
                Surface(
                    color = GoldPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${fields.size} INDICADORES",
                        color = GoldPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                fields.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { field -> ProxyFieldItem(field, modifier = Modifier.weight(1f)) }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun AssetProxyProfileSection(
    assetData: B3AssetData?,
    bundle: AssetChartBundle?,
    isFii: Boolean,
    newsItems: List<com.example.network.NewsItem> = emptyList(),
    isLoadingNews: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        assetData?.let { data ->
            AssetDataQualityCard(data)
            if (data.assetDescription.isNotBlank()) {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.10f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PERFIL OPERACIONAL", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(data.assetDescription, color = TextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
        }
        val fields = remember(assetData, bundle, isFii) { buildAssetProxyProfileFields(assetData, bundle, isFii) }
        ProxyFieldCard(
            title = "PERFIL & DADOS",
            subtitle = "Dados cadastrais, operacionais e patrimoniais existentes no Proxy para ${assetData?.ticker.orEmpty()}.",
            fields = fields,
            emptyTitle = "Perfil indisponível",
            emptyMessage = "O Proxy não retornou dados de perfil para este ativo no momento."
        )
        AssetNewsSection(ticker = assetData?.ticker.orEmpty(), newsItems = newsItems, isLoading = isLoadingNews)
    }
}

@Composable
private fun AssetDataQualityCard(data: B3AssetData) {
    val filledFields = listOf(
        data.price, data.dy, data.pvp, data.vpa, data.lastDividend, data.marketCap,
        data.roe, data.roic, data.margins, data.dailyLiquidity, data.high52, data.low52
    ).count { it.isFinite() && it != 0.0 } + listOf(
        data.name, data.cnpj, data.assetDescription, data.subSector, data.fiiSegment,
        data.fiiTotalHolders, data.fiiIssuedShares
    ).count { it.isNotBlank() }
    val calculatedCompleteness = ((filledFields / 19.0) * 100.0).coerceIn(0.0, 100.0)
    val completeness = data.extractionCompleteness.takeIf { it > 0.0 } ?: calculatedCompleteness
    val dataStateLabel = when {
        data.fromLocalSnapshot -> "CACHE LOCAL"
        data.isPartial -> "PARCIAL"
        data.proxyStatus.isNotBlank() -> data.proxyStatus.uppercase(Locale.ROOT)
        else -> "OK"
    }
    val dataStateColor = when {
        data.fromLocalSnapshot || data.isPartial -> WarningOrange
        else -> SuccessGreen
    }
    Surface(
        color = GoldPrimary.copy(alpha = 0.06f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.16f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("DADOS RECEBIDOS", color = GoldPrimary, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.8.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = dataStateColor.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                        Text(dataStateLabel, color = dataStateColor, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text("${String.format(Locale.ROOT, "%.0f", completeness)}%", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
            Text(
                text = "Fonte: ${data.source}. Campos ausentes permanecem como indisponíveis para evitar simulação.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            if (data.isPartial || data.fromLocalSnapshot || data.partialDataGuidance.isNotBlank()) {
                Text(
                    text = data.partialDataGuidance.ifBlank { "Resposta parcial: somente campos confirmados pelo Proxy foram renderizados." },
                    color = WarningOrange,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ProxyFieldCard(
    title: String,
    subtitle: String,
    fields: List<ProxyDisplayField>,
    emptyTitle: String,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.10f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            Spacer(modifier = Modifier.height(16.dp))
            if (fields.isEmpty()) {
                EmptyChartState(emptyTitle, emptyMessage)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    fields.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { field -> ProxyFieldItem(field, modifier = Modifier.weight(1f)) }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxyFieldItem(field: ProxyDisplayField, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(field.label.uppercase(Locale.ROOT), color = GoldPrimary, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text(field.value, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (field.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(field.description, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f), fontSize = 8.sp, lineHeight = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AssetNewsSection(ticker: String, newsItems: List<com.example.network.NewsItem>, isLoading: Boolean) {
    val context = LocalContext.current
    val sorted = remember(newsItems) { newsItems.sortedByDescending { it.timestamp } }
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ÚLTIMAS NOTÍCIAS DO ATIVO", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Notícias vinculadas a ${ticker.ifBlank { "este ativo" }}. Sem retorno do Proxy, o app mostra a exata indisponibilidade ao invés de simular manchetes inventadas.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            } else if (sorted.isEmpty()) {
                val explanation = if (ticker.isNotBlank()) {
                    "O VALORAE Proxy não localizou artigos ou atualizações de imprensa para o ativo $ticker nas últimas checagens."
                } else {
                    "Selecione um ativo para buscar notícias específicas registradas no proxy."
                }
                EmptyChartState(
                    title = "Nenhuma notícia retornada",
                    message = explanation
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sorted.take(8).forEach { news ->
                        Surface(
                            color = DarkBackground.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (news.link.isNotBlank()) {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(news.link))) }
                                }
                            }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(news.source.uppercase(Locale.ROOT), color = GoldPrimary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    Text(news.pubDate, color = TextSecondary, fontSize = 8.sp)
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(news.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
