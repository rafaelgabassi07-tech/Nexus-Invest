# RELATÓRIO — INDEX.HTML WEB DESKTOP APK PARITY v2.0.32

## Objetivo
Refazer a versão web localizada em `index.html` para ficar mais próxima do APK VALORAE nativo, com foco em uso desktop, organização visual, cards Material 3 escuros, navegação consistente, animações e compatibilidade com o VALORAE Proxy.

## Alterações aplicadas
- `index.html` refeito como app web autônomo em arquivo único, sem CDN e sem dependências externas.
- Layout desktop-first com rail lateral, top bar, FAB, cards premium, modais, drawers e responsividade para mobile com bottom navigation semelhante ao APK.
- Telas principais espelhando a navegação nativa: Início, Ativos, Análise, Insights e Notícias.
- Home renomeada e organizada como no APK: `Meus Investimentos`, patrimônio consolidado, total investido, rentabilidade, retorno total, divisão ações/FIIs, top posições, agenda e evolução de proventos.
- Página de Ativos com abas `Ativos` e `Histórico de Compras`, lista por classe e painel de detalhe.
- Página de Análise com busca por ticker e renderização amigável de dados do Proxy, sem exibir JSON cru ao usuário.
- Página de Insights com Agenda de Dividendos, Evolução de Proventos, Rankings e Risco/Alocação.
- Página de Notícias com cards visuais e fallback local.
- Compatibilidade com Proxy oficial `https://servidor-valorae.vercel.app` via endpoints `/api/v1/health`, `/api/v1/asset`, `/api/v1/asset/history`, `/api/v1/asset/dividends`, `/api/v1/portfolio/events`, `/api/v1/portfolio/next-dividends`, `/api/v1/market/rankings` e `/api/v1/news`.
- Cabeçalhos compatíveis com o contrato do APK: `x-valorae-app`, `x-valorae-client`, `x-valorae-build`, `x-valorae-platform`, `X-Valorae-Client-Version` e `X-Valorae-Environment`.
- Persistência local robusta em `localStorage`, com fallback em memória caso o navegador bloqueie armazenamento.
- Nova versão do app definida em `versionName = 2.0.32` e `versionCode = 42`.

## Observação de build
O projeto fonte foi atualizado. A compilação Gradle pode depender do ambiente do Google AI Studio/Android Studio para baixar o Gradle/Android Plugin caso não estejam em cache local.
