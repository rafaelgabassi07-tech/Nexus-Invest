# RELATÓRIO — Gráficos de Ativos End-to-End v2.0.31

## Objetivo
Corrigir o consumo e renderização dos gráficos das telas de Detalhes do Ativo e Análise no APK VALORAE.

## Correções principais no APK
- `B3NetworkService.kt` agora aceita histórico de dividendos vindo de `assetChartsCanonical`, `company.dividendHistory` e `fii.dividendHistory`.
- Corrigida agregação de proventos por ano/mês quando as datas vêm em ISO `yyyy-MM-dd`, além do formato brasileiro `dd/MM/yyyy`.
- A sazonalidade mensal deixou de ficar presa a 24 meses e ganhou filtros `24M`, `60M` e `MAX`.
- Os indicadores capturados do ativo passaram a ser exibidos como cards/lista no painel de gráficos, reduzindo a sensação de tela vazia quando uma série gráfica específica ainda não existe.
- O gráfico de Balanço Patrimonial agora aceita pelo menos duas séries reais entre Ativo, PL e Passivo, em vez de esconder tudo caso uma delas falte.
- Mantida compatibilidade com o Proxy v21.12.68 e aliases antigos.

## Validação
- `./gradlew --no-daemon assembleDebug` foi tentado, mas o ambiente isolado não conseguiu resolver `services.gradle.org` (`UnknownHostException`).
- Foi feita validação estática dos arquivos alterados e do contrato esperado pelo Proxy.

## Limites conhecidos
- O app não fabrica séries quando o Proxy declara que o Investidor10 não entregou pontos reais suficientes.
- Gráficos financeiros dependentes de APIs internas dinâmicas continuam sujeitos ao que o Proxy conseguir capturar no HTML/API disponível.
