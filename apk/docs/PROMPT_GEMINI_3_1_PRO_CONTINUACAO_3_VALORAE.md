# Prompt para Gemini 3.1 Pro — VALORAE Investidor / Portfolio — Continuação 3

Use o ZIP corrigido anexado como projeto principal.

Leia também:

- `RELATORIO_CONTINUACAO_3_VALORAE.md`
- `BUILD_VALIDATION_LOG_CONTINUACAO_3_VALORAE.txt`

## Objetivo

Preservar todas as correções já aplicadas e validar a Continuação 3 da auditoria, com foco em:

- recebimento correto das informações do Valorae Proxy;
- Detalhes do Ativo;
- Análise;
- Comparação de Índices;
- dividendos/proventos;
- Evolução de Proventos;
- Rentabilidade vs IPCA+;
- Equilíbrio de Carteira;
- Agenda de Dividendos;
- performance e estabilidade geral do app.

## Proxy obrigatório

Use somente:

`https://servidor-valorae.vercel.app/api`

Não use:

`https://valorae-proxy.vercel.app`

Não faça scraping direto no app Android.

Todas as informações externas devem vir pelo Valorae Proxy.

## Primeiro passo obrigatório

Antes de compilar, substitua ou regenere o Gradle Wrapper porque o arquivo recebido anteriormente está corrompido:

```text
gradle/wrapper/gradle-wrapper.jar
```

Depois execute:

```bash
./gradlew clean assembleDebug
```

## Validações obrigatórias

Valide no app:

1. PETR4 em Análise.
2. PETR4 em Detalhes do Ativo.
3. VALE3 em Análise e Detalhes.
4. MXRF11 em Análise.
5. MXRF11 em Detalhes do Ativo.
6. Comparação de Índices.
7. Comparação com IBOV para ações.
8. Comparação com IFIX para FIIs.
9. Comparação com CDI/IPCA quando disponível.
10. Filtros de período 1A, 3A, 5A, 10A e MAX.
11. Gráficos com descrições.
12. Histórico de preço.
13. Dividendos/proventos.
14. Evolução de proventos.
15. Rentabilidade vs IPCA+.
16. Equilíbrio de Carteira.
17. Agenda de Dividendos.
18. Estados vazios sem tela branca.
19. Payloads PARTIAL/warnings sem crash.
20. Classificação correta de FIIs.

## Pontos específicos que devem permanecer corrigidos

### Comparação de Índices

O parser deve normalizar séries brutas para retorno percentual.

Exemplo:

- PETR4: 40 → 44 deve virar 0% → 10%
- IBOV: 120000 → 126000 deve virar 0% → 5%

Não compare preço bruto com índice bruto no mesmo eixo.

### `/api/assets`

O parser deve aceitar arrays e objetos indexados por ticker nos caminhos:

- `assets`
- `items`
- `results`
- `data.assets`
- `data.results`

### Dividendos/proventos

Se o bundle principal não trouxer dividendos, use fallback:

`/api/asset/dividends?ticker=PETR4`

Aceitar:

- `events`
- `items`
- `dividends`
- `dividendos`
- `historicoDividendos`
- `proventos`
- `income`
- `data.events`
- `data.items`
- `data.dividends`
- `data.dividendos`
- `data.proventos`

### FIIs

A classificação deve considerar:

1. `liveInfo.isFii` vindo do Proxy;
2. inferência pelo ticker;
3. tipo local salvo;
4. fallback seguro.

### Histórico da carteira

`/api/portfolio/history` deve usar limite proporcional ao range, não limite fixo pequeno para todos os períodos.

### Headers

Manter:

```text
Accept: application/json
X-Valorae-Client-Id: valorae-investidor-android
X-Valorae-Client-Version: versão real do app / BuildConfig.VERSION_NAME
X-Valorae-Environment: production
```

## Regras de arquitetura

- Não adicionar serviços pagos.
- Não adicionar banco externo obrigatório.
- Não adicionar Redis/KV obrigatório.
- Não adicionar WebSocket obrigatório.
- Não adicionar Firebase obrigatório.
- Não reintroduzir scraping direto.
- Não trocar a arquitetura inteira sem necessidade.
- Fazer correções cirúrgicas e seguras.

## Critérios de aceite

A entrega só estará correta quando:

1. O projeto compilar.
2. Um APK debug novo for gerado.
3. Nenhuma referência ativa ao host antigo existir.
4. O app não fizer chamadas diretas a Investidor10, StatusInvest, Yahoo ou Google News.
5. Análise continuar funcionando.
6. Detalhes do Ativo receber dados corretamente.
7. Comparação de Índices funcionar com séries em retorno percentual.
8. Dividendos/proventos aparecerem quando disponíveis.
9. FIIs forem classificados corretamente.
10. Insights funcionarem sem NaN/Infinity/tela branca.
11. O app permanecer compatível com GitHub/Vercel free-only.
