import { NexusEngineUltra, inferAssetType, canonicalizeTicker, validarTicker } from './lib/nexus-engine.js';

export default async function handler(req: any, res: any) {
  try {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
      return res.status(200).end();
    }

    const { tickers } = req.body || {};
    if (!tickers || !Array.isArray(tickers)) {
      return res.status(400).json({ error: 'Send a tickers array: {"tickers": ["PETR4", "VALE3"]}' });
    }

    const results = await Promise.all(tickers.map(async (ticker) => {
      try {
        const clean = canonicalizeTicker(ticker);
        const erro  = validarTicker(clean);
        
        if (erro) {
          return { ticker: clean, success: false, error: erro, isInvalid: true };
        }

        const type = inferAssetType(clean);
        const data = await NexusEngineUltra.fetchAtivo(clean, type);
        return { ticker: clean, success: true, ...data };
      } catch (e: any) {
        return { ticker: String(ticker), success: false, error: e.message || 'Erro desconhecido' };
      }
    }));

    return res.status(200).json({ results });
  } catch (error: any) {
    console.error('Batch crash:', error);
    return res.status(500).json({
      error: 'Batch processing error: ' + (error.message || 'Erro desconhecido')
    });
  }
}
