# RELATÓRIO — VALORAE Web App Index parity v2.0.31

## Escopo entregue
- Substituição do `index.html` por uma versão web autônoma em arquivo único, sem dependência obrigatória de Tailwind/FontAwesome/CDN.
- Visual inspirado no APK Android Kotlin/Compose: tema Carbon, superfícies elevadas, ouro VALORAE, cards arredondados, top bar, bottom navigation e FAB.
- Navegação principal equivalente ao APK: Início, Ativos, Análise, Insights e Notícias.
- Lógica local de carteira com `localStorage`: compras, vendas, proventos recebidos, preço médio, patrimônio, resultado, DY realizado, concentração e ranking.
- Tela de Ativos com cards, filtros por classe, histórico editável e importação/exportação JSON.
- Tela de Análise com consulta ao VALORAE Proxy usando múltiplos contratos candidatos e normalização de resposta para evitar JSON bruto ao usuário final.
- Tela de Insights com agenda de proventos, evolução mensal, rankings e diagnóstico de compatibilidade com Proxy.
- Agenda de proventos com suporte a aliases amplos: `events`, `items`, `rows`, `dividends`, `dividendos`, `proventos`, `agenda`, `agendaEvents`, `upcomingEvents`, `nextDividends`, `futureEvents`, `schedule`, `calendar`, `calendario`, `payload`, `result`, `response`, `portfolio` e `asset`.
- Compatibilidade com `/api/v1/portfolio/events`, `/api/v1/portfolio/dividends`, `/api/v1/asset/dividends`, `/api/v1/asset/next-dividend`, `/api/v1/asset`, `/api/v1/news`, `/api/v1/ready`, `/api/v1/health` e `/api/sync`.
- Sincronia em nuvem via `/api/sync`, com suporte a criptografia client-side AES-GCM quando PIN/senha é informado.
- Tema claro/escuro e opção de ocultar valores.

## Arquivos alterados
- `index.html`
- `app/build.gradle.kts` (`versionCode=41`, `versionName=2.0.31`)
- `metadata.json` (`versionCode=41`, `versionName=2.0.31`)

## Observação
Não foi alterada a lógica nativa Kotlin/Compose do APK. A atualização se concentra na versão web raiz (`index.html`) e no versionamento do projeto para entrega ao AI Studio.
