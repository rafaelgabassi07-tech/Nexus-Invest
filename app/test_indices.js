const fs = require('fs');
async function main() {
  try {
    const res = await fetch("https://servidor-valorae.vercel.app/api/v1/asset?ticker=WEGE3&view=app&profile=max&complete=1");
    const data = await res.json();
    console.log("comparacaoIndices:", JSON.stringify(data.normalized?.comparacaoIndices, null, 2));
    console.log("comparacaoIndices (sections):", JSON.stringify(data.sections?.comparacaoIndices, null, 2));
    console.log("indexComparison length:", data.normalized?.comparacaoIndices?.length);
    console.log("comparacaoIndices:", JSON.stringify(data.normalized?.comparacaoIndices, null, 2));
    const ibovRes = await fetch("https://servidor-valorae.vercel.app/api/v1/asset/history?ticker=IBOV&range=5y");
    const ifixRes = await fetch("https://servidor-valorae.vercel.app/api/v1/asset/history?ticker=IFIX&range=5y");
    const cdiRes = await fetch("https://servidor-valorae.vercel.app/api/v1/asset/history?ticker=CDI&range=5y");
    const ibovData = await ibovRes.json();
    console.log("IBOV points len:", ibovData.points?.length);
    console.log("IBOV point 0:", ibovData.points?.[0]);
    console.log("IFIX status:", ifixRes.status);
    const ipcaRes = await fetch("https://servidor-valorae.vercel.app/api/v1/asset/history?ticker=IPCA&range=5y");
    console.log("IPCA status:", ipcaRes.status);
    const cdiData = await cdiRes.json();
    const indicesRes = await fetch("https://servidor-valorae.vercel.app/api/v1/market/indices");
    console.log("indices status:", indicesRes.status);
    const indicesData = await indicesRes.json();
    console.log("indices items:", indicesData.indices?.map(i => i.ticker || i.symbol || i.name));
  } catch(e) {
    console.error(e);
  }
}
main();
