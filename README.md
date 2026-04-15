# 🚀 Planify: AI-Powered Task Orchestrator

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.0+-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Notion API](https://img.shields.io/badge/Notion_API-Black?style=for-the-badge&logo=notion&logoColor=white)
![Telegram API](https://img.shields.io/badge/Telegram_Bot-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)

O **Planify** é um assistente autônomo de gestão de tempo que atua como uma ponte inteligente entre o Telegram e o Notion. Em vez de depender de comandos estritos (`/add`, `/move`), o Planify utiliza a API da Groq (Llama 3) para interpretar linguagem natural, deduzir intenções e orquestrar o banco de dados do Notion de forma invisível.

## Por que o Planify é diferente?

Assistentes baseados em LLM frequentemente sofrem com "alucinações" de datas e quebras de formato JSON. O Planify resolve isso através de Engenharia de Software e Prompting avançado:

* **Injeção de Contexto Temporal:** O backend gera uma tabela de referência de calendário dinamicamente e injeta no *System Prompt*, garantindo que a IA traduza perfeitamente "próxima terça-feira" para o formato `ISO8601` exato.
* **Parser JSON Resiliente (Deep Search):** Se a IA aninhar dados incorretamente ou alterar o *case* das chaves (ex: `TITLE` em vez de `title`), o algoritmo de busca profunda do `GroqParser` varre a árvore JSON (`JsonNode`) para resgatar os dados sem gerar exceções.
* **Sanitização de Fuso Horário:** Tratamento automático de retornos malformados da LLM (como `+0000` em vez de `Z`), utilizando `OffsetDateTime` para garantir parse seguro no Java Time API.
* **Orquestração CRUD:** Intenções de `MOVE` realizam buscas no Notion, arquivam a tarefa antiga e recriam no novo horário de forma transacional.

##  Arquitetura

1. **`TelegramClient`**: Escuta eventos (Webhook/Polling) em linguagem natural.
2. **`GroqParser`**: Atua como o "cérebro" do sistema. Limpa a entrada, aplica o esqueleto JSON no prompt, injeta o calendário e retorna um objeto `Objective` estritamente tipado.
3. **`PlannerService`**: O coração do domínio. Recebe o `Objective`, identifica a intenção (`CREATE`, `MOVE`, `DELETE`), valida regras de negócio e aciona integrações.
4. **`NotionClient`**: Executa as mutações (REST API) no banco de dados do Notion do usuário.

## Como executar localmente

### 1. Pré-requisitos
- Java 17 ou superior instalado.
- Maven 3.8+
- Chaves de API ativas: [Groq Cloud](https://console.groq.com/), [Notion Integration](https://www.notion.so/my-integrations) e [Telegram BotFather](https://t.me/BotFather).

### 2. Configuração de Variáveis de Ambiente
Crie um arquivo `application.properties` (ou `.yml`) no diretório `src/main/resources` com as seguintes credenciais:

```properties
# Telegram Bot
telegram.bot.token=SEU_TOKEN_DO_TELEGRAM

# Notion API
notion.api.key=SECRET_DA_INTEGRACAO_NOTION
notion.database.id=ID_DO_DATABASE_DE_TAREFAS

# Groq API (Llama 3)
groq.api.key=SUA_CHAVE_API_GROQ
groq.model.name=llama-3.1-8b-instant