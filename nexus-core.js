const liveAssetMetadata = {};

function inferAssetType(ticker) {
  const t = ticker.trim().toUpperCase();
  const etfs = new Set(["BOVA11","IVVB11","SMAL11","DIVO11","FIND11","MATB11","GOVE11","XFIX11","GOLD11","SPXI11","HASH11","BOVB11","BOVS11","BRAP11","BRRJ11","BRAX11","XINA11","EURP11","FIXA11","TCHE11","ECOO11","ACWI11","NASD11","USTK11","NSDQ11","DEFI11","ESGE11","SUST11","AGRI11","IFRA11","BDIV11","BLKB11","BNDX11","BOVV11","BRCO11","CSMO11","VALE11","QUAL11","REIT11","TRET11","WRLD11","XBOV11","PIBB11","SMAC11","MOAT11","PORD11","GLDL11","BITI11","SOLB11","TECC11","HFOF11","BITH11","COIN11","EMAG11","AGRO11","MCHI11","WEGE11","MAGO11","BLOK11","USIG11","SPAB11","CRYP11","ESGB11","SEMI11","RNDP11","FIDC11","ARGT11"]);
  const units = new Set(["TAEE11", "SANB11", "ALUP11", "KLBN11", "BPAC11", "ENGI11", "TIET11", "SULA11", "BIDI11", "SAPR11"]);

  if (etfs.has(t)) return "etfs";
  if (units.has(t)) return "acoes";
  if (/.*3[2-5]$/.test(t)) return "bdrs";
  if (t.endsWith("11")) return "fiis";
  if (/^[A-Z]{1,5}$/.test(t)) return "stocks";
  return "acoes";
}

async function buscarDadosAtivo(ticker) {
  if (liveAssetMetadata[ticker]) {
      updateDOM(liveAssetMetadata[ticker]);
      return liveAssetMetadata[ticker];
  }
  
  try {
    // Tenta primeiro a API normalizada
    const resAsset = await fetch(`/api/asset?ticker=${ticker}`);
    if (resAsset.ok) {
        const obj = await resAsset.json();
        const data = obj.results;
        const normalized = {
            ...obj,
            ...data,
            price: data.precoAtual || data.price || 0,
            dy: data.dy12m || data.dy || 0,
            pl: data.pl || 0,
            pvp: data.pvp || 0,
            roe: data.roe || 0,
            margins: data.margemLiquida || data.margemBruta || data.margins || 0,
            name: data.name || ticker
        };
        liveAssetMetadata[ticker] = normalized;
        updateDOM(normalized);
        return normalized;
    }

    // Fallback p/ scraping legando se a API falhar
    const tipo = inferAssetType(ticker);
    const response = await fetch("/api/scrape", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Cache-Version": "nexus-v16",
      },
      body: JSON.stringify({
        url: `https://investidor10.com.br/${tipo}/${ticker.toLowerCase()}/`,
      }),
    });

    if (!response.ok) {
      throw new Error("Erro de rede nas requisições proxy.");
    }

    const jsonData = await response.json();
    const rawHTML = jsonData.data || jsonData.html;
    if (!rawHTML) return null;

    const parser = new DOMParser();
    const doc = parser.parseFromString(rawHTML, "text/html");

    const getValue = (...labels) => {
      for (const lbl of labels) {
        // Search span[title]
        const span = doc.querySelector(`span[title*="${lbl}"]`);
        if (span) {
           const card = span.closest('._card');
           if (card) {
               const val = card.querySelector('._card-body span');
               if (val && val.textContent.trim()) return val.textContent.trim();
           }
           const cell = span.closest('.cell, .indicator, .table-line');
           if (cell) {
               const val = cell.querySelector('.value span, .destaque, .value');
               if (val && val.textContent.trim()) return val.textContent.trim();
           }
        }
        // Search text exactly
        const all = Array.from(doc.querySelectorAll('span, div, td'));
        for (const el of all) {
            if (el.textContent.trim().toLowerCase() === lbl.toLowerCase()) {
                const parent = el.closest('.cell, .indicator, .desc, tr');
                if (parent) {
                    const val = parent.querySelector('.value span, .destaque, .value, td:last-child');
                    if (val && val.textContent.trim() && val.textContent.trim().toLowerCase() !== lbl.toLowerCase()) {
                        return val.textContent.trim();
                    }
                }
            }
        }
      }
      return "";
    };

    let priceStr = getValue("Cotação");
    if (!priceStr) {
        const pEl = doc.querySelector('div[title="Cotação do Ativo"] .value, ._card.cotacao .value, .value');
        if (pEl) priceStr = pEl.textContent.trim();
    }

    const map = {
        price: priceStr,
        changePercent: getValue("VARIAÇÃO", "Variação (12M)", "VAR 12M"),
        pl: getValue("P/L", "P/Lucro"),
        pvp: getValue("P/VP"),
        dy: getValue("DY", "Dividend") || getValue("Dividend"),
        vpa: getValue("VPA", "Valor Patrimonial por Cota"),
        lpa: getValue("LPA"),
        roe: getValue("ROE"),
        roa: getValue("ROA"),
        roic: getValue("ROIC"),
        grossMargin: getValue("Margem Bruta"),
        ebitMargin: getValue("Margem Ebit", "Margem Operacional"),
        ebitdaMargin: getValue("Margem Ebitda", "Margem EBITDA"),
        margins: getValue("Margem Líquida"),
        evEbitda: getValue("EV/EBITDA"),
        evEbit: getValue("EV/EBIT"),
        priceEbitda: getValue("P/EBITDA"),
        priceEbit: getValue("P/EBIT"),
        priceAsset: getValue("P/Ativo"),
        priceCapGiro: getValue("P/Cap.Giro", "P/Capital de Giro"),
        priceAtivoCircLiq: getValue("P/Ativo Circ"),
        giroAtivos: getValue("Giro"),
        divLiqPatrimonio: getValue("Dívida Líquida / Patrimônio"),
        debtEbitda: getValue("Dívida Líquida / Ebitda"),
        divLiqEbit: getValue("Dívida Líquida / Ebit"),
        divBrutaPatrimonio: getValue("Dívida Bruta / Patrimônio"),
        patrimonioAtivos: getValue("Patrimônio / Ativos"),
        passivosAtivos: getValue("Passivos / Ativos"),
        liquidezCorrente: getValue("Liquidez Corrente"),
        cagrRevenue5y: getValue("CAGR Receitas 5 anos"),
        cagrProfit5y: getValue("CAGR Lucros 5 anos"),
        payout: getValue("Payout"),
        fiiVacancy: getValue("Vacância Física", "Vacância"),
        magicNumber: getValue("Magic Number"),
        fiiTotalHolders: getValue("Número de Cotistas", "Cotistas"),
        fiiIssuedShares: getValue("Cotas Emitidas", "Nº de Cotas"),
        fiiAdminFee: getValue("Taxa de Administração", "Taxa Admin"),
        fiiFundType: getValue("Tipo de Fundo"),
        fiiMandate: getValue("Mandato"),
        fiiTargetAudience: getValue("Público-alvo", "Publico Alvo"),
        fiiManagementType: getValue("Tipo de Gestão", "Gestão"),
        fiiDuration: getValue("Prazo de Duração", "Prazo"),
        fiiSegment: getValue("Segmento"),
        cnpj: getValue("CNPJ"),
        listSegment: getValue("Segmento de Listagem"),
        foundationYear: getValue("Ano de fundação", "Fundação"),
        listingYear: getValue("Ano de estreia na Bolsa", "IPO"),
        employeesCount: getValue("Número de funcionários", "Funcionários"),
        totalPapers: getValue("Nº total de papéis")
    };
    
    // Parse numeric fields and format to build standard metadata
    const parsed = {};
    for (const [k, v] of Object.entries(map)) {
        if (!v || v === "-" || v === "N/A" || v.toLowerCase() === "indisponível") continue;
        let numStr = v.replace("R$", "").replace("%", "").trim();
        if (numStr.includes(",")) numStr = numStr.replace(/\./g, "").replace(",", ".");
        const num = parseFloat(numStr);
        if (!isNaN(num) && k !== "cnpj" && k !== "fiiSegment" && k !== "fiiFundType" && k !== "fiiDuration" && k !== "fiiMandate" && k !== "fiiTargetAudience" && k !== "fiiManagementType" && k !== "fiiAdminFee" && k !== "listSegment" && k !== "foundationYear" && k !== "listingYear" && k !== "employeesCount" && k !== "totalPapers" && k !== "fiiIssuedShares" && k !== "fiiTotalHolders") {
            parsed[k] = num;
        } else {
            parsed[k] = v;
        }
    }
    
    const obj = {
        name: ticker,
        type: tipo === "fiis" ? "FII" : "Ação",
        price: parsed.price || 0,
        dy: parsed.dy || 0,
        pl: parsed.pl || 0,
        pvp: parsed.pvp || 0,
        roe: parsed.roe || 0,
        margins: parsed.margins || parsed.grossMargin || 0,
        lastDividend: parsed.price && parsed.dy ? (parsed.price * (parsed.dy/100)) : 0,
        fiiSegment: parsed.fiiSegment || "",
        fiiTotalHolders: parsed.fiiTotalHolders || "",
        magicNumber: parsed.magicNumber || 0,
        ...parsed
    };
    
    liveAssetMetadata[ticker] = obj;
    updateDOM(obj);
    return obj;
  } catch (err) {
    console.error("Erro ao buscar dados do ativo via Proxy:", err);
    return null;
  }
}

function updateDOM(obj) {
    if (obj.price) {
      const priceDom = document.getElementById("detail-indic-price");
      if (priceDom) priceDom.textContent = "R$ " + obj.price.toFixed(2).replace('.', ',');
    }
    if (obj.pvp) {
      const pvpDom = document.getElementById("detail-indic-pvp");
      if (pvpDom) pvpDom.textContent = obj.pvp.toFixed(2);
    }
    if (obj.dy) {
      const dyDom = document.getElementById("detail-indic-dy");
      if (dyDom) dyDom.textContent = obj.dy.toFixed(2) + "%";
    }
    if (obj.pl) {
      const plDom = document.getElementById("detail-indic-pl");
      if (plDom) plDom.textContent = obj.pl.toFixed(2);
    }

    if (obj.changePercent) {
      const vpaDom = document.getElementById("detail-indic-vpa");
      if (vpaDom) {
        vpaDom.textContent = obj.changePercent.toFixed(2) + "%";
        const vpaLabel = vpaDom.previousElementSibling;
        if (vpaLabel) vpaLabel.textContent = "VARIAÇÃO (DIA)";
      }
    }

    if (obj.price) {
      const simInput = document.getElementById("detail-sim-price-input");
      if (simInput && !isNaN(obj.price)) {
        simInput.value = obj.price;
        if (typeof recomputeSim === "function") {
          recomputeSim();
        }
      }
    }
}
