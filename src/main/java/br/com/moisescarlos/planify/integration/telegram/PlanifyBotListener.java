package br.com.moisescarlos.planify.integration.telegram;

import br.com.moisescarlos.planify.application.planner.PlannerService;
import br.com.moisescarlos.planify.integration.notion.NotionClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

@Component
public class PlanifyBotListener extends TelegramLongPollingBot {

    private final String botUsername;
    private final String allowedChatId;
    private final PlannerService plannerService;
    private final NotionClient notionClient;
    private final TelegramClient telegramClient;

    public PlanifyBotListener(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.chat.id}") String allowedChatId,
            @Lazy PlannerService plannerService,
            NotionClient notionClient,
            TelegramClient telegramClient) {

        super(botToken);
        this.botUsername = botUsername;
        this.allowedChatId = allowedChatId;
        this.plannerService = plannerService;
        this.notionClient = notionClient;
        this.telegramClient = telegramClient;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            if (!chatId.equals(allowedChatId)) return;

            // 1. Comandos Fixos
            if (text.equalsIgnoreCase("/listar")) {
                processTaskList();
            }
            else if (text.equalsIgnoreCase("/help") || text.equalsIgnoreCase("/ajuda")) {
                processHelp();
            }
            else if (text.equalsIgnoreCase("/status")) {
                processStatus();
            }
            // 2. Processamento de Linguagem Natural (IA + Regex)
            else {
                // Trocamos 'generatePlan' por 'handleCommand' para suportar MOVE e DELETE
                plannerService.handleCommand(text);
            }
        }
        else if (update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }

    private void processTaskList() {
        List<Map<String, String>> tasks = notionClient.getTasksForToday();

        if (tasks.isEmpty()) {
            telegramClient.sendMessage("🙌 <b>Nenhuma tarefa pendente para hoje!</b>");
            return;
        }

        telegramClient.sendMessage("📋 <b>Suas tarefas de hoje:</b>");
        for (Map<String, String> task : tasks) {
            String messageText = "📌 " + task.get("name");

            // Texto do botão em PT-BR, Data em EN
            var buttons = List.of(List.of(
                    Map.of("text", "✅ Concluir", "callback_data", "done:" + task.get("id"))
            ));

            telegramClient.sendWithButtons(messageText, buttons);
        }
    }

    private void handleCallback(Update update) {
        String callbackData = update.getCallbackQuery().getData();

        if (callbackData.startsWith("done:")) {
            String pageId = callbackData.split(":")[1];

            // Ação no Notion
            notionClient.updateTaskStatus(pageId, "Concluído");

            // Resposta no Telegram em PT-BR
            telegramClient.sendMessage("Tarefa concluída com sucesso! 🚀");
        }
    }

    private void processHelp() {
        String helpText = """
                🤖 <b>Central de Ajuda Planify</b>
                
                Eu ajudo você a organizar suas metas diretamente no <b>Notion</b> usando linguagem natural.
                
                <b>Comandos:</b>
                /listar - Lista suas tarefas de hoje com botões de conclusão.
                /status - Mostra um resumo do seu dia.
                /ajuda - Mostra esta mensagem.
                
                <b>Exemplos de como falar comigo:</b>
                • <i>"Quero estudar Java por 2 horas começando as 14h"</i>
                • <i>"Vou ler 50 páginas de um livro hoje"</i>
                • <i>"Remover as tarefas de 'Estudar Java'"</i>
                • <i>"Mover meus treinos para amanhã de manhã"</i>
                
                💡 <b>Dica:</b> Tente ser específico com horários para evitar conflitos!
                """;
        telegramClient.sendMessage(helpText);
    }

    private void processStatus() {
        List<Map<String, String>> tasks = notionClient.getTasksForToday();
        int pending = tasks.size();
        
        String statusText = String.format("""
                📊 <b>Resumo do Dia</b>
                
                📅 <b>Data:</b> %s
                📝 <b>Tarefas Pendentes:</b> %d
                
                %s
                
                ✨ <i>Mantenha o foco e produtividade!</i>
                """, 
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                pending,
                pending == 0 ? "✅ Você está em dia com suas metas!" : "🎯 Você ainda tem compromissos hoje. Use /listar para vê-los."
        );
        telegramClient.sendMessage(statusText);
    }
}