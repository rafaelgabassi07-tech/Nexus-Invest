const fs = require('fs');

async function test() {
    try {
        const res = await fetch("https://investidor10.com.br/acoes/grnd3/", {
            headers: {
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
            }
        });
        const html = await res.text();
        const htmlLower = html.toLowerCase();
        
        const extract = (label) => {
            let labelIdx = htmlLower.indexOf(label.toLowerCase());
            if (labelIdx === -1) return "NOT FOUND";
            let valRegex = /<span[^>]*>\s*([+-]?[\d,.]+[kmbKMB]*\s*%?)\s*<\/span>/gi;
            valRegex.lastIndex = labelIdx;
            let match = valRegex.exec(html);

            let divvalRegex = /<div\s+class="[a-zA-Z\s\-]*value[a-zA-Z\s\-]*"[^>]*>\s*(?:<div[^>]*>\s*)?([+-]?R?\$?\s*[\d,.]+\s*[kmbKMBa-zA-Z]*\s*%?)\s*(?:<\/div>\s*)?<\/div>/gi;
            divvalRegex.lastIndex = labelIdx;
            let divMatch = divvalRegex.exec(html);

            let result1 = match && (match.index - labelIdx < 800) ? match[1] : null;
            let result2 = divMatch && (divMatch.index - labelIdx < 800) ? divMatch[1].trim() : null;

            return result1 || result2 || "NOT_FOUND";
        };
        
        console.log("P/L:", extract("P/L<"));
        console.log("P/VP:", extract("P/VP<"));
        console.log("DY:", extract('title="DY"'));
        console.log("VPA:", extract(">VPA<"));
        console.log("ROE:", extract(">ROE <"));
        console.log("Liquidez:", extract("Liquidez Média Diária"));
        console.log("Patrimônio:", extract("Patrimônio Líquido<"));
        
    } catch(e) {
        console.error("Error", e);
    }
}
test();
