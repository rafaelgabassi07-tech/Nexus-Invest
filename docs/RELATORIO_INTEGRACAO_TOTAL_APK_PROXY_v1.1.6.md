# Integração total APK VALORAE v1.1.6 com VALORAE Proxy v21.12.54

Data: 2026-05-30

## Objetivo

Eliminar falhas de recebimento de informações no APK quando o Proxy retorna dados no novo contrato oficial `appPayload`/`appMobileSnapshot` e em contratos compatíveis `normalized`/`results`.

## Correções aplicadas no APK

- O parser do APK agora considera também `legacyAppCompat` como fonte de compatibilidade.
- `mapProxyAsset` e `parseAssetChartBundle` aceitam `legacyAppCompat.results` e `legacyAppCompat.normalized` quando o payload oficial do Proxy estiver usando contrato espelhado.
- O APK preserva leitura de `appPayload`, `appMobileSnapshot`, `assetClassContract`, `assetIndicatorCoverage`, `results`, `normalized`, `financialSummary`, `keyRatios` e `indicadoresAvancados`.
- Adicionado teste `testProxyV211254LegacyCompatContractIsParsed` para cobrir o contrato `21.12.54-total-apk-proxy-contract`.
- O script `verify_valorae_proxy_integration.py` agora verifica `legacyAppCompat`, `officialAppContractVersion`, `21.12.54-total-apk-proxy-contract` e o novo teste de parser.
- A fixture `app/response_petr4.json` foi atualizada com payload `view=app` contendo contrato oficial, `normalized`, `results`, `legacyAppCompat` e `normalizedSummary`.

## Validações executadas

- `python3 scripts/verify_valorae_proxy_integration.py`
- `python3 scripts/verify_valorae_deep_logic_pages_v116.py`
- `python3 scripts/verify_valorae_full_app_functionality.py`
- `python3 scripts/verify_valorae_proxy_capabilities.py`
- Auditoria cruzada de rotas: 57 caminhos `/api` usados pelo APK; 0 ausentes no roteador do Proxy.

## Limitação do ambiente

O build Gradle não pôde ser executado neste ambiente porque o Wrapper tentou baixar `gradle-9.3.1-bin.zip` de `services.gradle.org` e não há acesso externo/DNS disponível. A falha observada foi `UnknownHostException: services.gradle.org`.
