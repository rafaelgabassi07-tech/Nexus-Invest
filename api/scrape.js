export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  const body = req.body;
  if (!body?.url) {
    return res.status(400).json({ error: 'Envie a URL no formato: {"url": "https:..."}' });
  }

  // -- Validação da URL (Segurança) --
  try {
    const parsedUrl = new URL(body.url);
    if (parsedUrl.protocol !== 'https:') {
      return res.status(400).json({ error: 'Apenas URLs HTTPS são permitidas.' });
    }
    const allowedHosts = [
      'investidor10.com.br',
      'www.investidor10.com.br',
      'statusinvest.com.br',
      'www.statusinvest.com.br'
    ];
    if (!allowedHosts.includes(parsedUrl.hostname)) {
      return res.status(403).json({ error: 'Domínio não permitido pela proxy.' });
    }
  } catch (e) {
    return res.status(400).json({ error: 'URL inválida.' });
  }

  // -- Lê headers enviados pelo Nexus Engine (Repasse de UA stealth etc.) --
  const forwardedHeaders = (body.headers && typeof body.headers === 'object') ? body.headers : {};
  
  // Remove headers internos que não devem ir para o site-alvo (X-Cache-Version etc.)
  const { 'X-Cache-Version': _cv, ...safeForwardedHeaders } = forwardedHeaders;
  
  const userAgent = safeForwardedHeaders['User-Agent'] || 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

  const startMs = Date.now();

  try {
    const fetchRes = await fetch(body.url, {
      headers: {
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
        'Accept-Language': 'pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7',
        'Accept-Encoding': 'gzip, deflate, br',
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache',
        ...safeForwardedHeaders,
        'User-Agent': userAgent,
      },
    });

    const html = await fetchRes.text();
    const elapsedMs = Date.now() - startMs;

    return res.status(200).json({
      html,
      data: html,
      metrics: {
        cacheStatus: 'MISS',
        statusCode: fetchRes.status,
        elapsedMs,
        contentLength: html.length,
      },
    });
  } catch (error) {
    const elapsedMs = Date.now() - startMs;
    return res.status(500).json({
      error: 'Erro no proxy: ' + error.message,
      metrics: { elapsedMs },
    });
  }
}
