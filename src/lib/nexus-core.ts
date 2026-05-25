import { z } from 'zod';

// ════════════════════════════════════════════════════════════════════════════
// 1. TIPAGENS E CONTRATOS
// ════════════════════════════════════════════════════════════════════════════

export interface GenericRule {
  name: string;
  anchors: string[];
  extractRegex: RegExp;
  formatter?: (raw: any) => any;
  multiple?: boolean;
  extractGroups?: boolean;
  chunkSize?: number;
}

export interface ExtractorTemplate<T = any> {
  name: string;
  rules: GenericRule[];
  schema: z.ZodSchema<T>;
}

export interface ScrapeSource<T = any> {
  url: string;
  template: ExtractorTemplate<T>;
  requireStealth?: boolean;
}

export type ExtendedAssetType = 'ACAO' | 'FII' | 'BDR' | 'ETF' | 'STOCK';

export interface NewsItem {
  title: string;
  link: string;
  pubDate?: Date;
  source?: string;
}

export interface DividendItem {
  tipo: string;
  dataCom: string;
  dataPagamento: string;
  valor: number;
}

export interface NexusEngineOptions {
  cacheTtlMs?: number;
  cacheStaleMs?: number;
  maxRetries?: number;
  retryBaseDelay?: number;
  fetchTimeoutMs?: number;
  concurrencyLimit?: number;
  domainRps?: number;
  domainBurst?: number;
  useNexusProxy?: boolean;
  nexusProxyUrl?: string;
  nexusProxyBatchUrl?: string;
  nexusProxyTimeoutMs?: number;
  nexusProxyRetries?: number;
  fetchDispatcher?: any;
}

// ════════════════════════════════════════════════════════════════════════════
// 2. CONSTANTES PRÉ-COMPILADAS DE MÓDULO
// ════════════════════════════════════════════════════════════════════════════

const RE_MOEDA   = /[R$\s]/g;
const RE_MILHAR  = /\./g;
const RE_DECIMAL = /,/;
const RE_SA      = /\.SA$/i;
const RE_TICKER  = /^(?:[A-Z]{4}\d{1,2}F?|[A-Z]{1,5})$/;
const RE_ESPACO  = /\s+/g;

export const VALORES_INVALIDOS = new Set([
  '-', '—', '–', 'N/A', 'n/a', 'nd', '', 'null', 'undefined',
  '--', '---', '--%', '0%', '0,00', '0.00', 'n.d.', 'N.D.', 'NaN', 'Inf', '#', '?',
  'Indisponível', 'indisponível', 'Bloqueado', 'bloqueado', 'PRO', 'N.I.', '...',
  'Lock', 'lock', '--%',
]);

const USER_AGENTS = [
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36',
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36',
  'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36',
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:138.0) Gecko/20100101 Firefox/138.0',
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 14.7; rv:138.0) Gecko/20100101 Firefox/138.0',
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36 Edg/136.0.0.0',
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36',
];

const YAHOO_HOSTS = ['query1', 'query2'] as const;

const ETFS_CONHECIDOS = new Set([
  'BOVA11','IVVB11','SMAL11','DIVO11','FIND11','MATB11','GOVE11','XFIX11',
  'GOLD11','SPXI11','HASH11','BOVB11','BOVS11','BRAP11','BRRJ11','BRAX11',
  'XINA11','EURP11','FIXA11','TCHE11','ECOO11','ACWI11','NASD11',
  'USTK11','NSDQ11','DEFI11','ESGE11','SUST11','AGRI11','IFRA11',
  'BDIV11','BLKB11','BNDX11','BOVV11','BRCO11','CSMO11','VALE11','QUAL11',
  'REIT11','TRET11','WRLD11','XBOV11','PIBB11','SMAC11','MOAT11','PORD11',
  'GLDL11','BITI11','SOLB11','TECC11','HFOF11','BITH11','COIN11',
  'EMAG11','AGRO11','MCHI11','WEGE11','MAGO11','BLOK11','USIG11',
  'SPAB11','CRYP11','ESGB11','SEMI11','RNDP11','FIDC11','ARGT11',
]);

const DIAS_POR_PERIODO: Readonly<Record<string, number>> = {
  '1mo': 30, '3mo': 90, '6mo': 180, '1y': 365, '5y': 1825, 'max': 10950,
};

const NEXUS_PROXY_CACHE_VERSION = '2026-05-23-nexus-v16';

// ════════════════════════════════════════════════════════════════════════════
// 4. UTILITÁRIOS
// ════════════════════════════════════════════════════════════════════════════

export function normalizeBRNumber(raw: string): number | string {
  if (!raw) return '';
  let limpo = raw.replace(RE_MOEDA, '').toUpperCase().trim();
  if (limpo.includes('%')) return limpo;

  let mult = 1;
  const wordIdx = limpo.search(/BILH|TRILH|MILH(?!AR)|MIL\b/);
  if (wordIdx > 0) {
    const suffix = limpo.slice(wordIdx);
    if      (suffix.startsWith('BILH'))  mult = 1e9;
    else if (suffix.startsWith('TRILH')) mult = 1e12;
    else if (suffix.startsWith('MILH'))  mult = 1e6;
    else if (suffix.startsWith('MIL'))   mult = 1e3;
    limpo = limpo.slice(0, wordIdx).trim();
  } else {
    const ult = limpo[limpo.length - 1];
    if      (ult === 'K') { mult = 1_000;         limpo = limpo.slice(0, -1); }
    else if (ult === 'M') { mult = 1_000_000;     limpo = limpo.slice(0, -1); }
    else if (ult === 'B') { mult = 1_000_000_000; limpo = limpo.slice(0, -1); }
  }

  limpo = limpo.replace(RE_MILHAR, '').replace(RE_DECIMAL, '.');
  const num = parseFloat(limpo);
  return isNaN(num) ? raw.trim() : num * mult;
}

export function inferAssetType(ticker: string): ExtendedAssetType {
  const t = ticker.trim().toUpperCase();
  if (ETFS_CONHECIDOS.has(t)) return 'ETF';
  if (RE_BDR.test(t)) return 'BDR';
  if (t.endsWith('11')) return 'FII';
  if (/^[A-Z]{1,5}$/.test(t)) return 'STOCK';
  return 'ACAO';
}

export class NexusEngineUltra {
  private static _options: Required<NexusEngineOptions> = {
    cacheTtlMs:          24 * 60 * 60 * 1_000,
    cacheStaleMs:        5  * 60 * 1_000,
    maxRetries:          3,
    retryBaseDelay:      500,
    fetchTimeoutMs:      15_000,
    concurrencyLimit:    5,
    domainRps:           2,
    domainBurst:         5,
    /** NOVO v16 */
    useNexusProxy:       true,
    nexusProxyUrl:       '',
    nexusProxyBatchUrl:  '',
    nexusProxyTimeoutMs: 12_000,
    nexusProxyRetries:   2,
    /** NOVO v17 */
    fetchDispatcher:     undefined,
  };

  static configure(opts: NexusEngineOptions): void {
    this._options = { ...this._options, ...opts } as Required<NexusEngineOptions>;
  }

  static async fetchAtivo(ticker: string) {
      // Implementação simplificada para o Vercel / Proxy
      return { ticker, success: true };
  }
}
