package br.com.moisescarlos.planify.infrastructure.integration.telegram;

import br.com.moisescarlos.planify.application.planner.PlannerService;
import br.com.moisescarlos.planify.domain.model.Task;
import br.com.moisescarlos.planify.infrastructure.integration.notion.NotionClient;
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

            if (text.equalsIgnoreCase("/listar")) {
                processTaskList();
            }
            else if (text.equalsIgnoreCase("/help") || text.equalsIgnoreCase("/ajuda")) {
                processHelp();
            }
            else if (text.equalsIgnoreCase("/status")) {
                processStatus();
            }
            else {
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

            var buttons = List.of(List.of(
                    Map.of("text", "✅ Concluir", "callback_data", "done:" + task.get("id"))
            ));

            telegramClient.sendWithButtons(messageText, buttons);
        }
    }

    private void handleCallback(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        if (callbackData.startsWith("done:")) {
            String pageId = callbackData.split(":")[1];

            notionClient.updateTaskStatus(pageId, "Concluído");

            String originalText = update.getCallbackQuery().getMessage().getText();
            String feedbackText = "✅ <s>" + originalText + "</s>\n<i>Tarefa concluída no Notion!</i>";

            telegramClient.editMessage(chatId, messageId, feedbackText);
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
        List<Task> completedTasks = notionClient.getCompletedTasksLast7Days();

        if (completedTasks.isEmpty()) {
            telegramClient.sendMessage("📊 <b>Ainda não tenho dados suficientes para sua semana.</b>\nConclua algumas tarefas no Notion primeiro!");
            return;
        }

        Map<String, Long> stats = completedTasks.stream()
                .collect(java.util.stream.Collectors.groupingBy(Task::getCategory, java.util.stream.Collectors.counting()));

        int total = completedTasks.size();
        StringBuilder report = new StringBuilder("📊 <b>Seu Resumo Semanal</b>\n\n");
        report.append("✅ <b>Total Concluído:</b> ").append(total).append(" tarefas\n\n");
        report.append("🔥 <b>Foco por Categoria:</b>\n");

        stats.forEach((category, count) -> {
            double percent = (count.doubleValue() / total) * 100;
            String progress = "▓".repeat((int) percent / 10) + "░".repeat(10 - (int) percent / 10);
            report.append(String.format("<b>%s</b>: %.0f%%\n<code>%s</code> (%d)\n", category, percent, progress, count));
        });

        report.append("\n✨ <i>Continue evoluindo, Moisés!</i>");
        telegramClient.sendMessage(report.toString());
    }
}