# Ponte Segura Vercel ↔️ Supabase para Backup

Este diretório contém o código do Servidor Serverless (API Route) pronto para implantação no **Vercel** que resolve o problema de segurança de chaves expostas no APK do Android.

---

## 🔒 Por que esta arquitetura é 100% segura e recomendada?

1. **Chaves Secretas Ocultas**: As chaves `SUPABASE_URL` e `SUPABASE_ANON_KEY` ficam gravadas **apenas** no painel administrativo do Vercel e são acessadas de forma server-side (no servidor). Elas nunca são empacotadas ou injetadas na compilação do APK Android do aplicativo.
2. **Criptografia Zero-Knowledge no Cliente**: O aplicativo Android já realiza a criptografia simétrica com algoritmo **AES-256-GCM** localmente no celular utilizando a sua senha ou PIN pessoal antes de enviar. Logo, mesmo que alguém intercepte o tráfego da rede, ou mesmo que o Vercel ou o Supabase fossem invadidos, os seus dados de investimentos permanecem ilegíveis e totalmente privados.

---

## 🚀 Como Implantar no Vercel (Passo a Passo)

### Passo 1: Configurar a Estrutura do Projeto Vercel
1. No seu computador, crie uma pasta vazia para o projeto de backend.
2. Crie uma subpasta chamada `api/`.
3. Copie o arquivo `sync.js` (que criamos neste diretório) e salve-o como `api/sync.js`.

A estrutura final deve ser:
```text
meu-backend-vercel/
├── api/
│   └── sync.js
```

### Passo 2: Publicar no Vercel (Gratuitamente)
Você pode fazer a implantação de duas formas simples:

#### Opção A: Pelo Terminal (Vercel CLI)
1. Abra um terminal/cmd na pasta raiz do seu projeto `meu-backend-vercel`.
2. Instale a CLI do Vercel caso não tenha: `npm install -g vercel`
3. Digite `vercel` para fazer login e criar o projeto. Responda às perguntas rápidas escolhendo as opções padrão.
4. Após o deploy de desenvolvimento, publique em produção rodando `vercel --prod` para gerar a URL definitiva do seu app.

#### Opção B: Pelo GitHub (Mais recomendada para atualizar automático)
1. Crie um repositório privado no GitHub e suba a pasta contendo `api/sync.js`.
2. Acesse o painel da [Vercel](https://vercel.com/) e clique em **Add New > Project**.
3. Importe o repositório que você acabou de criar.
4. Clique em **Deploy**.

---

### Passo 3: Adicionar as Variáveis de Ambiente no Painel do Vercel
Durante ou após o deploy na Vercel:
1. Vá nas configurações do seu projeto na Vercel (**Settings > Environment Variables**).
2. Adicione as seguintes chaves com os valores correspondentes que você pegou da sua conta no Supabase:
   * **`SUPABASE_URL`**: `https://xxxxxxxxxxxxxxxxxxxx.supabase.co`
   * **`SUPABASE_ANON_KEY`**: A chave anônima (composta por uma string longa JWT).
3. Salve as variáveis e faça um "Redeploy" se necessário para as alterações entrarem em vigor.

---

## 📱 Passo 4: Conectar o Aplicativo Android à sua Ponte Vercel

Uma vez que sua API tenha sido publicada, a Vercel gera uma URL do tipo `https://nome-do-seu-projeto.vercel.app`.

Tudo o que você precisa fazer agora é configurar o aplicativo Android para apontar para o Vercel:
1. No painel de **Secrets (Segredos) do AI Studio**, adicione a variável:
   * **`VERCEL_BACKEND_URL`**: `https://nome-do-seu-projeto.vercel.app`
2. **Deixe em branco ou limpe** as variáveis `SUPABASE_URL` e `SUPABASE_ANON_KEY` no painel do AI Studio. 
3. Quando as variáveis do Supabase estão vazias e a do Vercel está preenchida, o aplicativo do Android detecta isso na hora e direciona toda a sincronização (Backup e Restauração) de forma automática e transparente pela sua ponte do Vercel!

---

💡 **Nota sobre Custo**: O plano Hobby do Vercel é **100% gratuito** de forma vitalícia e possui uma cota de 100.000 requisições diárias sem custos adicionais, o que é mais do que suficiente para uso corporativo, prototipagem ou uso pessoal confortável do seu app!
