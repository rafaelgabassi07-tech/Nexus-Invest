# Prompt para Gemini 3.5 Flash — VALORAE Continuação 9

Use o ZIP anexado como projeto principal do app VALORAE Investidor/Portfolio. Preserve todas as correções já aplicadas e continue a partir desta versão.

## Objetivo

Compilar o app, corrigir qualquer erro restante e gerar um APK debug novo. Valide principalmente a chegada de dados, indicadores fundamentalistas, FIIs e gráficos.

## Proxy obrigatório

Use somente:

```text
https://servidor-valorae.vercel.app/api
```

Não use:

```text
https://valorae-proxy.vercel.app
```

Não faça scraping direto no app Android. Não acesse diretamente Investidor10, StatusInvest, Yahoo, Google Finance ou Google News. Todos os dados externos devem vir pelo Valorae Proxy.

## Primeiro passo de build

Nesta versão o `gradle-wrapper.jar` corrompido foi substituído por um JAR válido. Mesmo assim, se o Android Studio preferir regenerar o wrapper oficial, faça isso antes de compilar.

Execute:

```bash
./gradlew clean assembleDebug
```

Se houver erro de download do Gradle por rede, configure internet/proxy no Studio ou regenere o wrapper oficial com Gradle instalado.

## Correções que devem ser preservadas

1. `B3NetworkService.mergedObject()` deve combinar `root.normalized`, `results.normalized` e outros objetos parciais.
2. `canonicalKey()` deve normalizar acentos, espaços, barras, hífens e underlines para reconhecer aliases de indicadores.
3. O parser deve aceitar:
   - `results.indicadores`
   - `results.normalized`
   - `root.normalized`
   - `results.financialSummary`
   - `financialSummary.ratiosChave`
   - `financialSummary.keyRatios`
   - `financialSummary.ratios`
   - `results.indicadoresFundamentalistas.semComparativos`
   - `results.indicadoresFundamentalistas.comComparativos`
   - `results.indicadoresFundamentalistas.comparativos`
   - `results.fundamentalistIndicators`
   - `results.keyValues`
   - `results.informacoesFundo`
   - `results.dadosFundo`
   - `results.fund`
   - `results.valorPatrimonial`
   - `results.revenueGeography`
   - `results.revenueSegment`
4. Valores zero explícitos devem ser preservados quando forem informação válida, especialmente `Vacância Física = 0%`.
5. `AssetDetailModal` deve completar o grid de indicadores com `chartBundle.indicatorCards`.
6. FIIs como `MXRF11`, `HGLG11` e outros terminados em `11` devem carregar dados em Análise e Detalhes.
7. Todos os gráficos devem mostrar título, descrição e estado vazio amigável.
8. Nenhuma tela pode ficar branca quando o Proxy retornar `PARTIAL`, `warnings` ou campos ausentes.

## Validação obrigatória

Teste no app:

- PETR4 em Análise
- PETR4 em Detalhes do Ativo
- VALE3 em Análise
- MXRF11 em Análise
- MXRF11 em Detalhes do Ativo
- HGLG11 ou KNRI11 em Análise/Detalhes se possível
- Comparação de Índices
- Gráficos de FIIs
- Indicadores fundamentalistas dos FIIs
- Dividendos/proventos
- Evolução de Proventos
- Rentabilidade vs IPCA+
- Equilíbrio de Carteira
- Agenda de Dividendos

## Critérios finais

A entrega só está correta quando:

1. O app compilar sem erros.
2. Um APK debug novo for gerado.
3. Detalhes do Ativo receber os mesmos dados centrais da Análise.
4. Indicadores fundamentalistas aparecerem quando vierem em `normalized`, `financialSummary`, `indicadores`, `keyRatios`, `informacoesFundo` ou `indicadoresFundamentalistas`.
5. Gráficos de ações e FIIs renderizarem quando houver dados.
6. Estados vazios forem amigáveis, sem tela branca.
7. Não houver host antigo nem scraping direto.
8. O app continuar compatível com plano gratuito GitHub/Vercel.

Ao final, entregue:

1. APK debug novo.
2. Lista de arquivos alterados.
3. Resultado de `./gradlew clean assembleDebug`.
4. Resumo do que foi corrigido.
