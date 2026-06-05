const fs = require('fs');
async function main() {
  try {
    const assetRes = await fetch("https://servidor-valorae.vercel.app/api/v1/asset?ticker=WEGE3&complete=1");
    const assetData = await assetRes.json().catch(()=>({}));
    
    function searchKeys(obj, path = "") {
        if (!obj || typeof obj !== 'object') return;
        for (let k in obj) {
            const currentPath = path ? `${path}.${k}` : k;
            if (k.toLowerCase().includes('revenue') || 
                k.toLowerCase().includes('geo') || 
                k.toLowerCase().includes('country') ||
                k.toLowerCase().includes('fatur')) {
                console.log("FOUND KEY:", currentPath);
            } else if (typeof obj[k] === 'object' && !Array.isArray(obj[k])) {
                searchKeys(obj[k], currentPath);
            }
        }
    }
    searchKeys(assetData);
  } catch(e) {
    console.error(e);
  }
}
main();
