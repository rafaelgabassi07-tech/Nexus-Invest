const fs = require('fs');
async function main() {
    const resComp = await fetch("https://servidor-valorae.vercel.app/api/v1/compare?tickers=WEGE3,IBOV,IFIX,CDI,IPCA&range=10Y&view=standard&profile=fast");
    const dataComp = await resComp.json();
    console.log("compare ranking length:", dataComp.ranking?.length);
    console.log("compare rankings keys:", Object.keys(dataComp.rankings || {}));
}
main();
