# Relatório Agenda de Dividendos APK

## Arquivos Alterados
- `B3NetworkService.kt`: Modificada lógica de parsing de nomes de rankings (removendo pontuações `(+,%)` de forma `clean` e aceitando variações); adicionados parâmetros `includeHistory=1` e `includeUpcoming=1` em requisições de dividendos se não estavam. Adicionados alias na listagem de dividendos e proventos.
- `ChartsScreen.kt`: Exibição visual dos eventos corrigida. Foi adicionada uma regra especial que permite a exibição do evento na aba de "Agenda de Dividendos" e "Proventos" mesmo quando o usuário não tinha uma quantidade elegível ao tempo do anúncio, evitando ocultar o evento do calendário oficial. Nos componentes `DividendEventsList`, mostramos a mensagem `Valor estimado indisponível para sua carteira` caso não seja possível calcular o saldo ou caso a eligibilidade esteja zerada. 
- `DashboardScreen.kt`: Ajustados a responsividade limitando que caracteres não-numerais gerassem distorções visuais para preços e custos na tela de nova transação; corrigidos os contrastes (`0xFF444444`) e campos de input.
- `SettingsScreen.kt`: Foi modificado o sistema falho de exportar e ler transações em um gerenciador de arquivos (fallback que travava se o usuário não possuísse um sistema de arquivos local apto), criando flags e popups de erros seguros.
- `PortfolioViewModel.kt`: Corrigido a string de busca de notícias globais, retirando o pareamento compulsório com primeiro ticker para exibir Notícias Gerais e Macroeconomia.

## Endpoints e Parâmetros
O uso de endpoints da Agenda e Histórico agora mandatórios:
- `includeHistory=1`
- `includeUpcoming=1`
- `mode=complete`
- `limit=250`

## Regra para eventos sem posição
Um evento continuará visível. Durante a listagem local e filtragem, evitamos descartar o evento caso `estimated` seja zero se ele ainda for válido. Na tela, se o `eligibleShares` ou o cálculo for invisível, exibe a mensagem de aviso mas preenche ao lado o ticker, data do pagamento, e `Valor por Ação/Cota`. O espaço "Sua estimativa / Total" é marcado como `R$ --`.

## Versionamento
A versão foi verificada, seguindo acima de 2.0.24, em fase de testes. A compatibilidade e fallbacks antigos não foram removidos.
Nenhum json bruto listado nas páginas. Ranking, Carteiras, Histórico de transações seguem mantidos com os mesmos designs fundamentais da identidade Visual da Valorae.
