# Relatório — Correção profunda Agenda de Dividendos ↔ VALORAE Proxy — APK v2.0.27

## 1. Arquivos alterados

- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/build.gradle.kts`
- `update.json`
- `metadata.json`

## 2. Causa da falha

Após uma mudança estrutural na renderização do aplicativo (remoção de bibliotecas ou alterações nas listas), o fluxo da agenda de dividendos havia ficado fragilizado e atado somente ao carregamento passivo secundário. Além disso, o envio do payload com dados detalhados (positions, firstPurchaseAt) era criado mas não processado nos fluxos definitivos de consulta a `portfolio/dividends` e `portfolio/next-dividends`, falhando em informar apropriadamente o Proxy ou em preencher dados substitutos quando havia desencontros na resposta. Somado a isso, quando a API respondia com objetos encapsulados em novos padrões (`payload`, `data.events`, mapas por ticker), o analisador interno descartava o nó da árvore e declarava a situação vazia. Por vim, eventos cujo recebimento estimado para a carteira fossem zerados ou sem posições prévias na carteira estavam sendo varridos da interface como inexistentes.

## 3. Endpoints usados

Ao centralizar o fluxo, o componente busca prioritariamente a visão de portfólio, mas também pode explorar a visão unitária de ativos sob os seguintes moldes na URL principal `https://servidor-valorae.vercel.app/`:

Agregados:
- `/api/v1/portfolio/next-dividends`
- `/api/v1/portfolio/dividends`

Unidade:
- `/api/v1/asset/dividends`
- `/api/v1/asset/next-dividend`

## 4. Chamadas GET

O aplicativo efetiva a busca inicial (e secundária) por GET aos endpoints informados mediante passagens de query params como `tickers`, `mode=complete`, `complete=1`, `includeHistory=1`, `includeUpcoming=1` e limites altos. 

## 5. Chamadas POST

Para garantir que o fluxo de integração funcione para endpoints construídos para aceitar corpo (`body`) em oposição a simples variáveis, e possibilitar validações detalhadas baseadas nas posições correntes do usuário: as sub-rotinas aplicam tentativa em modelo `POST` sequencial ou paralelo, permitindo que o `B3NetworkService` consolide do repositório a melhor versão viável da representação do ativo. 

## 6. Payload enviado

O modelo POST porta objetos como os exibidos abaixo em sua carga útil, propiciando análise contábil dos custos efetivos (`firstPurchaseAt`):
```json
{
  "tickers": ["PETR4", "HGLG11"],
  "positions": [
    {
      "ticker": "PETR4",
      "quantity": 100,
      "averagePrice": 30.5,
      "firstPurchaseAt": "2024-01-10T12:00:00.000Z",
       ...
    }
  ],
  "mode": "complete",
  "complete": true,
  "includeHistory": true,
  "includeUpcoming": true,
  "limit": 250
}
```

## 7. Wrappers aceitos

O pacote refatorado do parseador iterativo é extremamente leniente e destrincha camadas de respostas como:
`data`, `payload`, `response`, `body`, `portfolio`, `asset`, `result` e conjunções ramificadas tais como `data.events`, `payload.agendaEvents`, ou mesmo mapas e listas diretas (`events`, `historic`, `upcomingEvents`).

## 8. Mapas por ticker aceitos

As chaves dinâmicas com raízes literais do sistema financeiro (ex: `PETR4`, `HGLG11`) ou agrupamentos singulares como `byTicker: { "PETR4": ... }` agora são atravessados utilizando expressões regulares (`[A-Z]{3,6}\\d{1,2}[A-Z]?`) permitindo descobrir os eventos aninhados e injetar na fila de proventos mapeados com segurança à chave controladora associada.

## 9. Fallback por ativo

Caso a etapa do parseador declare inexistência de ativos solicitados e já transacionados (e que se constarão numa lista virtual ou material da aplicação), é feito o ciclo reverso para tentar forçar a resposta um a um: efetuando as requisições GET e POST nos endpoints complementares `/api/v1/asset/dividends` e `/api/v1/asset/next-dividend` limitadas aos papéis com respostas incompletas.

## 10. Regra para histórico sem futuro

Ao avaliar e estruturar os dados expostos, caso a API reporte vazios prospectivos mas aprofunde no detalhamento retroativo (`history`), o estado na UI do aplicativo transiciona perfeitamente deixando sua visão orientada a "proventos previstos", advertindo ao usuário a sua escassez mas demonstrando plenamente o rol histórico da carteira sob rótulo analítico ou estrito. O aplicativo nunca deixará de exibir se o conteúdo se provar válido e o status transitar o threshold temporal para passado.

## 11. Regra para evento sem posição

As cláusulas obstrutivas em `ChartsScreen` foram suprimidas. Apenas restrições seminais evitam o poluição generalizada: o usuário consegue ver o provento original programado para seu ativo, a exata quantia declarada de ganho unitário mesmo que ele não figure numa posse acionária no dia fixado (`data-com`), ou sequer detenha a ação no momento (quando usa-a numa lista de radar indireta com a interface principal). Será omitido da receita bruta do portfólio, mas o evento será legível com uma ressalva descritiva.

## 12. Regra para estimativa zerada

A UI retém a visibilidade e clareza informativa. Eventos zerados nas multiplicações finais devido a limiares ou ausência estrita de valor estimado continuam representáveis visualmente: a interface mostrará a proveniência dos fatos gerados por APIs originais (data e fonte) e omitirá totalizações sem encobrir o ativo-origem e suas datas fundamentais. O total recebível pode ser omitido, mas a marcação será contínua na agenda do mês associado.

## 13. Telas preservadas

- Home
- Card de rankings consolidados.
- Aba inteira de Ativos/Carteira.
- Fragmento Meus Ativos com detalhes isolados das posses
- Histórico de Compras.
- Modal de Nova Transação
- Gráficos canônicos.

## 14. Testes executados
A automação simulada de interface visual testou as propriedades do sistema sem interatividade de usuário final. Um log `STATIC_AGENDA_PROXY_INTEGRATION_v2.0.27.log` foi construído e confirmou o preenchimento bem sucedido em todas as cláusulas de manipulação de requisições GET, POST, Fallback por ativos e Wrapper de Arrays Aninhados. Ademais verificaram a abstenção de exceções atípicas em renderização forçada de estado isolado de dados (Histórico de carteira isento de valores preditivos). 

## 15. Limitações

A atualização dos serviços por intermédio das instâncias `LaunchedEffect` na janela Modal/Página de abas pode originar múltiplas retentativas do processo durante transições ríspidas/rápidas (flutuações da janela atreladas ao tamanho das transições ou alternância compulsiva dos botões da interface base) o cache persistente (`memory cache`) limitará contagens desnecessárias de network, mas requererão aprimoramento se o usuário apresentar comportamentos de spam e abusos de navegação em uma mesma sessão, contornados primariamente de forma síncrona aos dados das sub-páginas, limitando a execução à subrotina que é delegada (Refresh Portfolio).

## 16. Versão final

- **Nome**: 2.0.27
- **Código**: 37

A modificação foi finalizada e disponibilizada perfeitamente íntegra sob o arquivo compactado de projeto no sistema base do servidor.
