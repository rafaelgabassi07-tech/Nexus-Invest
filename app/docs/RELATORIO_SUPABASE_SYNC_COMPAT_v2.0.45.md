# VALORAE APK v2.0.45 — Compatibilidade Supabase Sync/Snapshots

## Objetivo
Preparar o APK para salvar no Supabase todas as informações persistíveis que ajudam desempenho, continuidade e restauração, sem tornar o Supabase obrigatório e sem quebrar o modo local.

## O que foi implementado

### 1. Camada `CloudSyncManager`
Arquivo: `app/src/main/java/com/example/network/CloudSyncManager.kt`

Suporta:
- teste de conexão Supabase;
- envio de snapshots genéricos;
- restauração de transações;
- criptografia opcional AES-256-GCM antes de enviar dados sensíveis;
- acesso direto ao Supabase via REST/PostgREST usando publishable/anon key;
- ponte segura via VALORAE Proxy `/api/sync` quando o backend possui a chave secreta;
- funcionamento no-op seguro quando o Supabase não está configurado.

### 2. Blocos enviados como snapshot
A ação manual **Sincronizar** salva:
- `portfolio/summary`: resumo, posições, alocação e totais;
- `portfolio/transactions_backup`: movimentações da carteira;
- `portfolio/analytics`: histórico da carteira, IPCA, eventos de proventos e estado dos rankings;
- `market/asset_snapshots`: cotações, indicadores e metadados dos ativos já carregados;
- `market/news`: notícias em cache;
- `system/local_state`: estado de boot, proxy health, notificações e changelogs.

### 3. UI em Configurações > Backup e Dados
A seção agora exibe **Supabase opcional** com:
- status de configuração;
- botão **Testar**;
- botão **Sincronizar**;
- botão **Restaurar transações do Supabase**;
- opção para sobrescrever a carteira local ao restaurar.

### 4. BuildConfig opcional
Novas variáveis aceitas:

```env
SUPABASE_SYNC_ENABLED=true
SUPABASE_URL=https://SEU-PROJETO.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_xxx
SUPABASE_ANON_KEY=
VALORAE_SUPABASE_PROXY_SYNC_ENABLED=true
VALORAE_SUPABASE_SYNC_TOKEN=token-opcional
VALORAE_SUPABASE_AUTO_BACKUP_ENABLED=false
VALORAE_SUPABASE_AUTO_ENCRYPTION_SECRET=
```

## Schema Supabase
Incluído em:

```text
app/supabase/001_valorae_snapshots.sql
```

Execute esse SQL no Supabase antes de sincronizar.

## Segurança recomendada

- Não colocar `service_role` no APK.
- Para uso pessoal sem Supabase Auth, preferir a ponte via Proxy com `SUPABASE_SERVICE_ROLE_KEY` no Vercel e `VALORAE_SUPABASE_SYNC_TOKEN`.
- Para acesso direto do APK, usar apenas `SUPABASE_PUBLISHABLE_KEY`/`SUPABASE_ANON_KEY` e políticas RLS apropriadas.
- A restauração direta pelo APK usa o `user_id` local da instalação, criado em `SharedPreferences`.

## Validação estática
- `CloudSyncManager.kt` substituído por implementação funcional.
- `PortfolioViewModel.kt` ganhou exportação de snapshots e restauração Supabase.
- `SettingsScreen.kt` ganhou UI de sincronização opcional.
- `versionName = 2.0.45` e `versionCode = 55`.
