# RELATÓRIO — NOVA AUDITORIA MINUCIOSA DO APK VALORAE

Data: 2026-05-30  
Projeto auditado: APK VALORAE — versão pós-correção de Insights  
Proxy alvo: VALORAE Proxy v21.12.51/v21.12.52  
URL pública esperada: `https://servidor-valorae.vercel.app/`

## 1. Escopo da auditoria

A nova auditoria revisou, de forma minuciosa, os seguintes blocos do APK:

- Integração do APK com o VALORAE Proxy.
- Endpoints `/api/v1/...` usados pelo app.
- Página **Insights** e suas subpáginas.
- Lógica de dividendos passados e futuros.
- Respeito ao tempo real de existência da carteira.
- Agenda de dividendos.
- IPCA+ e comparação por período da carteira.
- Diversificação e gráficos de barras/progresso.
- Testes unitários e contratos usados nos testes.
- Validação de URL do Proxy.
- Segurança contra uso de HTTP/local host em build de produção.
- Dependências ou serviços externos opcionais.

A auditoria não removeu funcionalidades existentes, não adicionou serviços pagos e manteve o Proxy como backend central do APK.

---

## 2. Resultado geral

Status final da auditoria estática: **APROVADO**.

Foram encontradas e corrigidas falhas reais, principalmente em:

1. Agenda de dividendos usando eventos antigos quando não havia eventos futuros.
2. Teste unitário ainda usando contrato antigo `/api/asset`.
3. Validação de base URL aceitando `http://`.
4. Gráficos/progressos sem clamp de valores, podendo quebrar visualmente com dados extremos ou parciais.
5. Sincronização externa opcional com validação fraca de URL/chave.
6. Nome genérico do projeto no Gradle.

Após os ajustes, as auditorias estáticas retornaram:

```text
Valorae Proxy integration audit OK
Valorae Insights logic audit OK
```

---

## 3. Arquitetura confirmada

O APK continua sendo um projeto Android nativo com:

- Kotlin.
- Jetpack Compose.
- Room/local persistence.
- ViewModel/StateFlow.
- OkHttp/JSON manual via `JSONObject`.
- Camada centralizada de rede em `B3NetworkService.kt`.
- Página de Insights implementada em `ChartsScreen.kt`.

A integração com o VALORAE Proxy permanece centralizada no APK, sem transformar o aplicativo em scraper direto.

---

## 4. Endpoints do Proxy confirmados no APK

A camada de rede usa contratos v1 do Proxy para as funções principais:

- `/api/v1/asset`
- `/api/v1/assets`
- `/api/v1/asset/history`
- `/api/v1/asset/dividends`
- `/api/v1/news`
- `/api/v1/market/indices`
- `/api/v1/portfolio/analyze`
- `/api/v1/portfolio/history`
- `/api/v1/market/ipca`
- `/api/v1/portfolio/next-dividends`
- `/api/v1/ready`
- `/api/v1/release/readiness`
- `/api/v1/source/status`
- `/api/v1/integration/manifest`

Rotas técnicas mantidas por compatibilidade:

- `/api/server/metrics`
- `/api/observability`
- `/api/fields`
- `/api/openapi`

Não foram encontradas chamadas inseguras de scraping direto no fluxo principal do APK.

---

## 5. Correções realizadas nesta nova auditoria

### 5.1. Agenda de dividendos corrigida

Problema encontrado:

A subpágina de Agenda podia reutilizar eventos antigos quando não havia evento futuro disponível. Isso poderia exibir dividendos já passados como se fossem agenda futura.

Correção aplicada:

- Adicionado cálculo de início do dia para comparações estáveis.
- Criado filtro de eventos futuros/elegíveis.
- Agenda agora usa apenas eventos de hoje ou futuros.
- Eventos antigos não são mais reutilizados como fallback.
- Fallback local também remove datas-com antigas.

Arquivos alterados:

- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `scripts/verify_valorae_insights_logic.py`

Validação adicionada:

- `Agenda não reaproveita eventos antigos quando não há futuro`
- `Agenda local remove datas-com antigas`
- `KPIs da agenda usam eventos elegíveis`

---

### 5.2. Dividendos continuam respeitando a existência real da carteira

A auditoria confirmou que a lógica de proventos segue respeitando:

- Data de compra.
- Data-com, quando disponível.
- Data de pagamento, quando aplicável.
- Quantidade elegível na data do evento.
- Não inclusão automática de proventos anteriores à existência da posição.

Também foi confirmado que o payload opcional enviado ao Proxy preserva `firstPurchaseAt`, permitindo que o backend consiga ajustar análises ao período real da carteira.

---

### 5.3. IPCA+ continua respeitando o período da carteira

A auditoria confirmou que o IPCA remoto é rebaseado para o início real da carteira, evitando comparação contra meses anteriores à existência do portfólio.

Também foi confirmado que o histórico local usa custo médio móvel após vendas, evitando distorções em carteiras com compra/venda parcial.

---

### 5.4. Gráficos e barras protegidos contra valores extremos

Problema encontrado:

Alguns componentes visuais usavam percentuais diretamente em `LinearProgressIndicator` ou `fillMaxWidth`, sem limitar a faixa válida. Respostas parciais, valores extremos ou percentuais acima de 100 poderiam causar comportamento visual incorreto.

Correção aplicada:

- Percentuais e pesos visuais agora usam `coerceIn(0f, 1f)`.
- A UI fica mais robusta contra dados parciais, inválidos ou extremos.

Arquivo alterado:

- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`

Validação adicionada:

- `Barras de diversificação são clampadas em 0..1`

---

### 5.5. Teste unitário migrado para contrato `/api/v1/asset`

Problema encontrado:

Um teste unitário ainda referenciava a rota antiga:

```text
/api/asset?ticker=PETR4
```

Correção aplicada:

O teste foi convertido para validação offline do contrato correto:

```text
/api/v1/asset?ticker=PETR4&view=app&profile=turbo
```

Agora o teste valida:

- Uso de HTTPS.
- Path `/api/v1/asset`.
- Parâmetros `ticker`, `view` e `profile`.
- Headers `x-valorae-app` e `x-valorae-platform`.

Arquivo alterado:

- `app/src/test/java/com/example/ExampleUnitTest.kt`

---

### 5.6. Base URL do Proxy endurecida para HTTPS

Problema encontrado:

A validação aceitava URLs com `http://`. Para o APK final, isso não é desejável.

Correção aplicada:

- `safeValoraeProxyUrl` agora exige `https://`.
- `B3NetworkService.isUsableProxyUrl` agora exige `https://`.
- Continuam bloqueados `localhost`, `127.0.0.1`, `10.0.2.2`, placeholders e host legado.

Arquivos alterados:

- `app/build.gradle.kts`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `scripts/verify_valorae_proxy_integration.py`

---

### 5.7. Sincronização externa opcional endurecida

Foi encontrada uma camada opcional de sync externo já existente no projeto. Ela não é necessária para a integração com o Proxy e não foi usada como dependência do APK.

Correção aplicada:

- `CloudSyncManager.isCloudConfigured()` agora exige URL HTTPS.
- Bloqueia placeholders.
- Bloqueia `localhost`, `127.0.0.1` e `10.0.2.2`.
- Exige chave minimamente válida.

Arquivo alterado:

- `app/src/main/java/com/example/network/CloudSyncManager.kt`

Observação:

Essa camada continua opcional e desativada se não configurada. Se a regra desejada for eliminar completamente qualquer possibilidade de sync externo, a próxima etapa recomendada é remover ou ocultar essa funcionalidade opcional.

---

### 5.8. Nome do projeto ajustado

Problema encontrado:

O projeto ainda tinha nome genérico no Gradle.

Correção aplicada:

- `rootProject.name` alterado para `VALORAE`.

Arquivo alterado:

- `settings.gradle.kts`

---

## 6. Subpáginas de Insights revisadas

### 6.1. Proventos

Status: **corrigido/validado**.

Critérios verificados:

- Proventos passados respeitam existência da carteira.
- Proventos futuros respeitam quantidade elegível.
- Eventos anteriores à compra não entram como recebidos.
- Ranking de pagadores respeita período e posição.
- Top pagadores não usa multiplicador fixo indevido.

### 6.2. Agenda

Status: **corrigido/validado**.

Critérios verificados:

- Não reaproveita eventos antigos.
- Mostra apenas hoje/futuro.
- Datas antigas do fallback local são filtradas.
- KPIs usam eventos elegíveis.

### 6.3. IPCA+

Status: **validado**.

Critérios verificados:

- IPCA remoto rebaseado ao início da carteira.
- Histórico remoto ajustado ao período da carteira.
- Histórico local respeita custo médio móvel.

### 6.4. Diversificação

Status: **corrigido/validado**.

Critérios verificados:

- Barras e progressos limitados em `0..1`.
- Evita quebra visual por dados extremos/parciais.
- Mantém leitura estável da composição por ativo/setor/tipo.

---

## 7. Validações executadas

Comandos executados no sandbox:

```bash
python3 scripts/verify_valorae_proxy_integration.py
python3 scripts/verify_valorae_insights_logic.py
```

Resultado:

```text
Valorae Proxy integration audit OK
Valorae Insights logic audit OK
OK - Insights calcula quantidade elegível em dividendos
OK - Evolução de proventos recebe transações
OK - Agenda de dividendos filtra por elegibilidade
OK - Agenda não reaproveita eventos antigos quando não há futuro
OK - Agenda local remove datas-com antigas
OK - Barras de diversificação são clampadas em 0..1
OK - Top pagadores respeita período e existência da carteira
OK - KPIs da agenda usam eventos elegíveis
OK - Histórico remoto é ajustado ao início da carteira
OK - IPCA é rebaseado ao período da carteira
OK - Proventos remotos são saneados por posição na data
OK - Histórico local usa custo médio móvel após vendas
OK - Payload de carteira envia firstPurchaseAt opcional
```

---

## 8. Gradle

Foi tentado rodar Gradle no sandbox:

```bash
./gradlew :app:compileDebugKotlin --stacktrace --info
```

Resultado:

```text
Baixando Gradle: https://services.gradle.org/distributions/gradle-9.3.1-bin.zip
Exception in thread "main" java.net.UnknownHostException: services.gradle.org
```

Conclusão:

A falha ocorreu antes da compilação do código, durante o download da distribuição Gradle pelo wrapper. Portanto, neste ambiente ela continua sendo uma falha de rede/DNS do sandbox, não uma falha comprovada de Kotlin, Compose, Manifest ou dependência do APK.

Como você já conseguiu contornar esse mesmo problema no Android Studio usando Gradle nativo, recomenda-se validar esta versão final no mesmo ambiente.

---

## 9. Comandos recomendados no Android Studio

Na raiz do projeto, execute:

```bash
./gradlew clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
./gradlew :app:lintDebug :app:check --stacktrace --info
```

Se quiser uma bateria mais completa:

```bash
./gradlew --version
./gradlew clean --stacktrace --info
./gradlew :app:compileDebugKotlin --stacktrace --info
./gradlew :app:compileDebugJavaWithJavac --stacktrace --info
./gradlew :app:processDebugManifest --stacktrace --info
./gradlew :app:mergeDebugResources --stacktrace --info
./gradlew :app:testDebugUnitTest --stacktrace --info
./gradlew :app:lintDebug --stacktrace --info
./gradlew :app:check --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
```

---

## 10. Arquivos alterados

- `app/build.gradle.kts`
- `settings.gradle.kts`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `app/src/main/java/com/example/network/CloudSyncManager.kt`
- `app/src/main/java/com/example/ui/screens/ChartsScreen.kt`
- `app/src/test/java/com/example/ExampleUnitTest.kt`
- `scripts/verify_valorae_proxy_integration.py`
- `scripts/verify_valorae_insights_logic.py`
- `docs/RELATORIO_NOVA_AUDITORIA_MINUCIOSA_VALORAE_APK.md`

---

## 11. Critérios de aceite reavaliados

| Critério | Status |
|---|---:|
| APK usa Proxy como backend central | OK |
| APK usa `/api/v1/ready` | OK |
| APK usa `/api/v1/assets` para carteira | OK |
| APK usa `/api/v1/asset` para detalhe | OK |
| Rotas antigas principais removidas do código-fonte/testes | OK |
| `PARTIAL` não deve apagar snapshot bom | OK pela lógica auditada |
| Insights respeita existência real da carteira | OK |
| Proventos passados respeitam posição/data | OK |
| Dividendos futuros respeitam elegibilidade | OK |
| Agenda não mostra eventos antigos como futuros | Corrigido/OK |
| IPCA+ compara só o período da carteira | OK |
| Gráficos protegidos contra valores extremos | Corrigido/OK |
| Base URL do Proxy configurável | OK |
| Base URL exige HTTPS | Corrigido/OK |
| Sem scraping direto inseguro no fluxo principal | OK |
| Sem novas dependências pagas | OK |
| Gradle no sandbox | Bloqueado por DNS externo |
| Gradle no Android Studio | Revalidar após estes patches |

---

## 12. Pendências reais

1. Reexecutar Gradle no Android Studio após esta nova rodada de patches.
2. Testar manualmente em dispositivo/emulador:
   - Carteira com compras antigas.
   - Carteira recém-criada.
   - Carteira com vendas parciais.
   - Ativo com dividendos passados.
   - Ativo com dividendos futuros.
   - Proxy offline.
   - Proxy retornando `PARTIAL`.
3. Decidir se a camada opcional de sync externo deve ser removida completamente ou apenas permanecer endurecida/desativada sem configuração.

---

## 13. Conclusão

A nova auditoria encontrou falhas reais e as corrigiu. O ponto mais crítico era a Agenda de dividendos, que podia reaproveitar eventos antigos como fallback. Esse comportamento foi removido.

A integração com o VALORAE Proxy está coerente com os contratos v1, a validação de URL ficou mais segura, os testes unitários não usam mais rota antiga, e a página Insights está mais alinhada ao tempo real de existência da carteira.

O próximo passo obrigatório é rodar a bateria Gradle no Android Studio, porque o sandbox continua bloqueado por `UnknownHostException` ao baixar o Gradle wrapper.
