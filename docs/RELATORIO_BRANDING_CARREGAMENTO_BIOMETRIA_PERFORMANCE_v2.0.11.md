# RELATÓRIO — VALORAE APK v2.0.11

## Objetivo
Aprimorar a tela de carregamento inicial e a tela de desbloqueio por biometria/digital, garantindo que o logotipo do aplicativo apareça acompanhado do nome **Valorae** abaixo. Também foram aplicadas melhorias de desempenho, performance e responsividade em pontos seguros da aplicação.

## Arquivos alterados

- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/viewmodel/PortfolioViewModel.kt`
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/build.gradle.kts`
- `scripts/verify_valorae_branding_performance_v211.py`

## Melhorias visuais aplicadas

### Tela de carregamento inicial
Criada uma tela própria de carregamento com:

- logotipo do Valorae;
- nome **Valorae** logo abaixo do logotipo;
- subtítulo `CARTEIRA DE INVESTIMENTOS`;
- barra de progresso discreta;
- mensagem de inicialização amigável;
- fundo escuro com gradiente para aparência mais premium.

A tela aparece enquanto as preferências de segurança/biometria ainda estão carregando. Isso evita piscar rapidamente a tela de bloqueio quando a biometria está desativada.

### Tela de desbloqueio biométrico/digital
A tela de desbloqueio agora reutiliza o mesmo bloco visual de marca:

- logotipo do app;
- nome **Valorae** abaixo;
- subtítulo `ACESSO SEGURO`;
- texto de status mais claro para digital/PIN/senha/padrão;
- prompt nativo do Android com título `Valorae`.

## Melhorias de responsividade/performance

### Inicialização mais suave
- O app agora diferencia estado `carregando preferências` de estado `bloqueado`.
- A Home não tenta renderizar antes de saber se a biometria está ativa ou não.
- O teclado usa `SOFT_INPUT_ADJUST_RESIZE`, reduzindo sobreposição de campos em telas com formulário.

### Menos recomposições e emissões repetidas
Foram adicionados `distinctUntilChanged()` em fluxos derivados importantes:

- resumo de ativos;
- resumo DARF;
- resumo consolidado da carteira.

Isso reduz recomposições quando o conteúdo calculado não mudou.

### Timeouts de rede delimitados
Chamadas que poderiam manter estados de carregamento por muito tempo agora têm limites explícitos:

- diagnóstico do Proxy: `3.500ms`;
- notícias: `5.500ms`;
- bundle de gráficos: `12.000ms`;
- rankings completos da Home: `14.000ms`/`18.000ms` conforme o modo.

### Home mais estável
- Itens principais do `LazyColumn` receberam chaves estáveis.
- Removido `AnimatedVisibility(visible = true)` redundante na composição da carteira.
- Contagem de ativos por classe agora usa `remember(assets)`.

## Versionamento

- `versionName = "2.0.11"`
- `versionCode = 21`
- `app_name = "Valorae"`

## Validações realizadas

Executado:

```bash
python3 scripts/verify_valorae_branding_performance_v211.py
```

Resultado:

```text
STATIC_BRANDING_PERFORMANCE_CHECK_OK
```

Executado:

```bash
python3 scripts/verify_valorae_loading_optimization.py
```

Resultado:

```text
Valorae loading optimization audit OK
```

Executado:

```bash
python3 scripts/verify_valorae_deep_final_audit.py
```

Resultado:

```text
Valorae deep final audit OK
```

## Observação sobre build Gradle
O build completo com Gradle não pôde ser executado neste ambiente porque o wrapper tentou baixar a distribuição em `services.gradle.org`, mas o ambiente atual está sem acesso externo. As verificações estáticas e estruturais foram executadas no código-fonte.
