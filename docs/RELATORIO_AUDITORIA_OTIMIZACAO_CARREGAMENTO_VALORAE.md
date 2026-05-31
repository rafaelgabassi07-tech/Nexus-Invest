# RELATÓRIO — Auditoria de Correções e Otimização de Carregamento do APK VALORAE

## Objetivo

Realizar uma nova auditoria focada em fluidez, redução de chamadas simultâneas ao VALORAE Proxy, carregamento progressivo das informações e prevenção de demora excessiva na abertura do aplicativo.

## Arquivos alterados

- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/network/B3NetworkService.kt`
- `scripts/verify_valorae_proxy_capabilities.py`
- `scripts/verify_valorae_loading_optimization.py`
- `docs/APK_BUILD_ATTEMPT_LOADING_OPTIMIZATION_LOG.txt`
- `docs/RELATORIO_AUDITORIA_OTIMIZACAO_CARREGAMENTO_VALORAE.md`

## Problemas encontrados

### 1. Abertura do app com chamadas demais ao mesmo tempo

A abertura ainda disparava saúde do Proxy, notícias e rankings de mercado praticamente no mesmo momento. Embora fossem assíncronas, isso poderia competir com o carregamento inicial da carteira, especialmente em celulares mais simples ou conexão lenta.

### 2. Insights recarregava rankings globais junto com analytics da carteira

A rotina de Insights buscava análise da carteira, histórico, IPCA, dividendos, ranking da carteira e também rankings globais de ações/FIIs. Isso gerava chamadas repetidas, pois os rankings globais já têm tela própria e cards na Home.

### 3. Botão de atualizar podia disparar Proxy+ pesado sem necessidade

Mesmo com Proxy+ carregando sob demanda, o refresh geral ainda podia forçar a atualização dos módulos avançados quando o usuário ainda não tinha aberto ou usado a tela Proxy+.

### 4. Falta de TTL no ViewModel para evitar chamadas repetidas

Algumas ações de UI ou recomposições podiam chamar novamente saúde, notícias, rankings ou cotações sem uma janela mínima de reuso.

### 5. Batch de ativos podia virar muitas chamadas individuais

Quando `/api/v1/assets` retornava parcialmente, o fallback podia consultar tickers faltantes um a um. Em carteiras grandes, isso poderia gerar atraso desnecessário.

### 6. Proxy+ poderia consultar endpoints demais em uma abertura

A tela Proxy+ era sob demanda, mas ainda podia abrir vários módulos avançados de uma vez, o que pesa no aplicativo e no Proxy.

## Correções e otimizações aplicadas

### Abertura progressiva

O app agora carrega de forma escalonada:

- saúde do Proxy primeiro;
- notícias após pequeno atraso;
- rankings após outro pequeno atraso.

Isso evita que Home, carteira, notícias e rankings disputem rede/CPU no mesmo instante.

### TTLs no ViewModel

Foram adicionadas janelas de reuso para:

- cotações em lote;
- notícias;
- rankings de mercado;
- saúde do Proxy;
- analytics da carteira;
- Proxy+.

Com isso, toques repetidos, recomposições e navegações rápidas não viram novas chamadas desnecessárias.

### Travas contra chamadas concorrentes

Foram adicionadas travas com `Job?.isActive` para:

- notícias;
- saúde do Proxy;
- rankings;
- Proxy+.

Se uma chamada já estiver em andamento, outra chamada igual é ignorada até a anterior terminar.

### Insights mais leve

A atualização de Insights agora não baixa novamente os rankings globais de mercado a cada atualização da carteira. Ela preserva os rankings já carregados pela Home/Rankings e se concentra nos dados realmente dependentes da carteira:

- análise da carteira;
- histórico da carteira;
- IPCA ajustado ao tempo de existência da carteira;
- proventos elegíveis;
- ranking da própria carteira.

### Refresh geral mais seguro

O refresh geral não força Proxy+ se o usuário nunca abriu a tela Proxy+. Se Proxy+ já foi carregado antes, ele pode ser atualizado; caso contrário, permanece sob demanda.

### Rede menos agressiva

O `OkHttp` foi ajustado para uma concorrência mais moderada:

- `maxRequests = 8`;
- `maxRequestsPerHost = 4`;
- timeouts menores e mais compatíveis com uso mobile.

Isso reduz filas longas, travamentos perceptíveis e pressão no deploy gratuito da Vercel.

### Rankings ao vivo com timeout mais curto

O ranking ao vivo, usado principalmente na Home e página Rankings, agora usa timeout menor para não segurar a experiência caso o Proxy demore.

### Fallback de batch otimizado

Quando `/api/v1/assets` retorna parcialmente:

1. o app tenta usar último snapshot local;
2. só depois faz chamadas individuais limitadas;
3. evita transformar uma falha parcial em dezenas de requests.

### Proxy+ mais compacto na abertura da tela

A tela Proxy+ continua funcional, mas agora prioriza os blocos mais importantes por abertura:

- qualidade/cobertura/plano/mapa de fontes;
- principais módulos avançados do ativo;
- principais módulos de FII;
- principais módulos da carteira;
- diagnóstico essencial.

Isso preserva valor analítico sem pesar demais o app.

### Diagnóstico do Proxy com cache

O diagnóstico consolidado do Proxy agora tem cache curto. Abrir configurações/diagnóstico repetidamente não refaz todas as chamadas técnicas imediatamente.

## Validações estáticas executadas

Comandos executados no sandbox:

```bash
python3 scripts/verify_valorae_proxy_integration.py
python3 scripts/verify_valorae_insights_logic.py
python3 scripts/verify_valorae_proxy_capabilities.py
python3 scripts/verify_valorae_proxy_recommendations.py
python3 scripts/verify_valorae_final_consolidation.py
python3 scripts/verify_valorae_loading_optimization.py
```

Resultado: todas passaram.

## Gradle

A tentativa de executar o wrapper Gradle no sandbox falhou antes da compilação por erro externo de DNS:

```text
java.net.UnknownHostException: services.gradle.org
```

Classificação: erro de ambiente do sandbox, não erro comprovado do APK. O log foi salvo em:

```text
docs/APK_BUILD_ATTEMPT_LOADING_OPTIMIZATION_LOG.txt
```

## Comandos recomendados no Android Studio

```bash
./gradlew clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace --info
./gradlew :app:assembleDebug --stacktrace --info
./gradlew :app:lintDebug :app:check --stacktrace --info
```

## Resultado esperado após esta rodada

- Home abre mais leve.
- Carteira carrega primeiro sem disputar com Proxy+ pesado.
- Rankings aparecem progressivamente.
- Notícias não travam a abertura.
- Insights não repete rankings globais sem necessidade.
- Proxy+ continua disponível, mas sob demanda e com carga controlada.
- Menos chamadas simultâneas ao Proxy e menor risco de lentidão em conexão fraca.
- Melhor compatibilidade com Vercel Free.
