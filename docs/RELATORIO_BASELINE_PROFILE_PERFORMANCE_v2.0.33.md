# RELATÓRIO — Baseline Profile e Performance v2.0.33

## Objetivo

Aplicar uma otimização segura de runtime/startup no APK VALORAE sem aumentar a espera do build nem depender de execução obrigatória de Macrobenchmark em cada compilação.

## Decisão técnica

Foi aplicada uma estratégia conservadora:

- `ProfileInstaller` explícito no app.
- `baseline-prof.txt` manual em `app/src/main/baselineProfiles/`.
- `startup-prof.txt` manual em `app/src/main/baselineProfiles/`.
- `ReportDrawnWhen` em `MainActivity` para sinalizar a primeira tela utilizável sem aguardar rede.
- Nenhuma geração automática de Baseline Profile no assemble normal.

Essa abordagem reduz risco porque não adiciona um módulo Macrobenchmark obrigatório, não exige emulador/dispositivo para o build comum e não mexe no Proxy.

## Rotas cobertas pelos perfis

- Abertura do app e `MainActivity`.
- Tema e preferências locais.
- Banco Room e repositório de transações.
- `PortfolioViewModel`.
- Dashboard.
- Ativos.
- Detalhes do ativo.
- Gráficos de Desempenho & Índices.
- Gráficos de Finanças & Balanço.
- Análise.
- Insights.
- Componentes de gráficos e seções de Proxy.
- `B3NetworkService` e modelos de gráficos.

## Arquivos alterados

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/baselineProfiles/baseline-prof.txt`
- `app/src/main/baselineProfiles/startup-prof.txt`
- `metadata.json`
- `update.json`
- `index.html`

## O que não foi feito

Não foi criado um módulo Macrobenchmark obrigatório nesta etapa. Motivo: ele exige dependências adicionais, dispositivo/emulador e execução própria para gerar perfis reais. Como o gargalo principal já havia sido corrigido na v2.0.32 com remoção de chamadas duplicadas e `chartfast`, esta etapa adiciona otimização de runtime com baixo risco.

## Como evoluir depois

No Android Studio, é possível criar futuramente um módulo Baseline Profile Generator para gerar perfis reais a partir de jornadas críticas:

1. Abrir app.
2. Entrar em Ativos.
3. Abrir PETR4/HGLG11.
4. Navegar para Desempenho & Índices.
5. Navegar para Finanças & Balanço.
6. Entrar em Insights.

O perfil gerado pode substituir ou complementar os arquivos manuais em `app/src/main/baselineProfiles/`.

## Validação estática

- Perfis criados com sintaxe HRF/wildcard compatível com Baseline Profiles.
- Versão atualizada para `2.0.33`.
- `ProfileInstaller` adicionado via catálogo de versões.
- `ReportDrawnWhen` não espera chamadas de rede, evitando travar a primeira tela utilizável.

## Observação de build

Neste ambiente, o Gradle Wrapper não consegue baixar a distribuição de `services.gradle.org`, portanto a compilação final precisa ser validada no Android Studio/AI Studio ou em ambiente com acesso à internet.
