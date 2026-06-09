# RELATÓRIO — Identidade visual VALORAE opção 3 — APK v2.0.43

## Objetivo

Aplicar a identidade visual escolhida pelo usuário, baseada na opção 3: símbolo geométrico em formato de diamante/bússola, com a letra V integrada, acentos em dourado, corpo em verde profundo e facetas prateadas. A proposta visual comunica valor, direção, estratégia, confiança e precisão.

## Locais rastreados com logotipo ou marca

### Código Compose nativo

- `app/src/main/java/com/example/MainActivity.kt`
  - TopAppBar principal: `R.drawable.valorae_logo_vector`.
  - Componente `ValoraeBrandLockup`: usado nas telas de carregamento e bloqueio.
  - `BrandedLoadingScreen`: abertura/carregamento inicial.
  - `LockScreen`: tela de acesso seguro/biometria.

- `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`
  - Página Sobre: `R.drawable.valorae_logo_vector`.

### Recursos Android ativos

- `app/src/main/res/drawable/valorae_logo_vector.xml`
  - Vetor principal usado no topo, splash, tela de loading, bloqueio e página Sobre.

- `app/src/main/res/drawable/ic_launcher_foreground.xml`
  - Foreground do adaptive icon Android.

- `app/src/main/res/drawable/ic_launcher_background.xml`
  - Background do adaptive icon Android.

- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  - Ícone launcher principal.

- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
  - Ícone launcher redondo.

- `app/src/main/res/drawable/valorae_splash_background.xml`
  - Splash nativo/`windowBackground`.

- `app/src/main/res/values/themes.xml`
  - Usa `@drawable/valorae_splash_background` como fundo inicial da janela.

### Recursos legados e auxiliares substituídos

Todos foram recriados para manter consistência visual, mesmo quando não estavam referenciados diretamente no código atual:

- `app_icon_foreground_1779471656384.png` — 512x512, foreground transparente.
- `img_app_icon_valorae_1779375113392.png` — 512x512, ícone completo.
- `investidor_icon_1779300657643.png` — 512x512, ícone completo.
- `valorae_app_icon.png` — 512x512, ícone completo.
- `valorae_logo_mark_square.png` — 512x512, marca quadrada.
- `valorae_logo_square_1779371909019.png` — 512x512, marca quadrada.
- `valorae_minimalist_logo_1779327110823.png` — 512x512, marca simplificada transparente.
- `valorae_launcher_foreground_safe.png` — 432x432, foreground seguro para launcher.
- `valorae_header_logo.png` — 1200x320, lockup horizontal.
- `valorae_splash_logo.png` — 900x260, lockup para splash/apresentação.
- `valorae_logo_mark.png` — 1024x1024, marca principal em alta resolução.
- `valorae_logo_1779326010183.png` — 1600x900, apresentação clara.
- `valorae_premium_logo_1779327613624.png` — 1600x900, apresentação escura premium.
- `valorae_wordmark_horizontal.png` — 1600x900, lockup horizontal.

### Versão web embutida na raiz do APK

- `index.html`
  - Logo lateral desktop.
  - Logo mobile/header.
  - Paleta principal ajustada para verde profundo + dourado premium.
  - Texto de marca alterado para `Direção & Valor`.

## Melhorias aplicadas na experiência visual

1. Substituição do antigo V simples por símbolo premium de diamante/bússola.
2. Remoção de recorte circular inadequado para o novo símbolo no topo do app.
3. Criação de tile premium com gradiente verde profundo e borda dourada para loading, bloqueio e Sobre.
4. Recriação do splash nativo com fundo escuro esverdeado e marca central maior.
5. Atualização do launcher adaptive icon para manter boa leitura em launchers Android.
6. Harmonização da versão web embutida com a nova identidade.
7. Atualização de manifesto de versão para `2.0.43` / `versionCode 53`.

## Paleta visual usada

- Verde profundo: `#073A2E`.
- Verde escuro: `#051612`.
- Dourado premium: `#E7C980`.
- Dourado base: `#C6A25A`.
- Prata/mist: `#E6ECE7`.
- Grafite: `#1C1F21`.

## Validação

- XMLs de recursos Android validados com parser XML: OK.
- Dimensões dos PNGs conferidas: OK.
- ZIP final testado com `unzip -t`: OK após empacotamento.
- Build Android completa não pôde ser executada neste ambiente porque o Gradle Wrapper tentou baixar `gradle-9.3.1-bin.zip` de `services.gradle.org`, mas o ambiente não possui resolução/acesso externo. O erro foi de rede, não de código.
