# Correção da proporção do ícone Android

Ajuste aplicado:
- O logotipo do launcher foi reduzido para aproximadamente 58–60% da área do ícone.
- Foi criada uma imagem de foreground com fundo transparente: `valorae_launcher_foreground_safe.png`.
- O adaptive icon usa fundo navy separado e foreground seguro.
- Os PNGs de fallback em `mipmap-mdpi` até `mipmap-xxxhdpi` também foram recriados com margem segura.

Objetivo:
- Evitar que o símbolo fique cortado ou grande demais no atalho da tela inicial do Android.
