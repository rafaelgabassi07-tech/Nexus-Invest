import { NexusEngineUltra, inferAssetType, canonicalizeTicker, validarTicker } from './lib/nexus-engine.js';

export default async function handler(req, res) {
  try {
    // CORS Headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') return res.status(200).end();

    const tickerRaw = req.query.ticker;
    
    // Tratamento para caso o ticker venha como array
    const ticker = Array.isArray(tickerRaw) ? tickerRaw[0] : tickerRaw;

    if (!ticker) {
      return res.status(400).json({ error: 'Envie o ticker via query: ?ticker=PETR4' });
    }

    // Normaliza e valida antes de qualquer coisa
    const clean = canonicalizeTicker(ticker as string);
    const erro  = validarTicker(clean);

    if (erro) {
      return res.status(400).json({
        error: erro,
        hint: '^IFIX e outros índices não são ativos negociáveis — use tickers como PETR4, VISC11, BOVA11'
      });
    }

    try {
      const type   = inferAssetType(clean);
      const result = await NexusEngineUltra.fetchAtivo(clean, type);
      
      // Headers de cache (SWR) para performance
      res.setHeader('Cache-Control', 's-maxage=3600, stale-while-revalidate');

      // Retorna o resultado com 'data' para compatibilidade retroativa e 'results' nativo do engine
      return res.status(200).json({
        ...result,
        data: result.results
      });
    } catch (engineError: any) {
      console.error(`Engine error fetching ${clean}:`, engineError);
      return res.status(500).json({ 
        error: 'Erro no motor de busca: ' + (engineError.message || 'Erro desconhecido'),
        ticker: clean 
      });
    }
  } catch (globalError: any) {
    console.error('Crash global no handler:', globalError);
    return res.status(500).json({ 
      error: 'Crash interno no servidor: ' + (globalError.message || 'Falha catastrófica'),
      stack: process.env.NODE_ENV === 'development' ? globalError.stack : undefined
    });
  }
}
