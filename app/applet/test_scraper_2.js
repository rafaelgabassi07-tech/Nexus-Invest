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
            let valRegex = /<span[a-zA-Z\s"=\-]*>([+-]?[\d,.]+[%KMB]?)<\/span>/gi;
            valRegex.lastIndex = labelIdx;
            let match = valRegex.exec(html);

            let divvalRegex = /<div\s+class="[a-zA-Z\s\-]*value[a-zA-Z\s\-]*">([+-]?R\$?\s*[\d,.]+\s*[a-zA-Z]*)<\/div>/gi;
            divvalRegex.lastIndex = labelIdx;
            let divMatch = divvalRegex.exec(html);

            let result1 = match && (match.index - labelIdx < 500) ? match[1] : "TOO FAR";
            let result2 = divMatch && (divMatch.index - labelIdx < 500) ? divMatch[1].trim() : "TOO FAR";

            return `SPAN: ${result1} | DIV: ${result2}`;
        };
        
        console.log("P/L:", extract("P/L<"));
        console.log("P/VP:", extract("P/VP<"));
        console.log("DY:", extract('title="DY"'));
        console.log("VPA:", extract(">VPA<"));
        console.log("ROE:", extract(">ROE <"));
        console.log("ROIC:", extract(">ROIC <"));
        console.log("Liquidez Média Diária:", extract("Liquidez Média Diária"));
        
    } catch(e) {
        console.error("Error", e);
    }
}
test();
