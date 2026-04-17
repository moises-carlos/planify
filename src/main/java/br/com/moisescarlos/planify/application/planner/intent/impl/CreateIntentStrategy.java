package br.com.moisescarlos.planify.application.planner.intent.impl;

import br.com.moisescarlos.planify.application.planner.intent.IntentStrategy;
import br.com.moisescarlos.planify.application.planner.strategy.TaskStrategy;
import br.com.moisescarlos.planify.application.planner.validator.TaskValidator;
import br.com.moisescarlos.planify.domain.model.Goal;
import br.com.moisescarlos.planify.domain.model.Objective;
import br.com.moisescarlos.planify.domain.model.Task;
import br.com.moisescarlos.planify.domain.parser.GroqParser;
import br.com.moisescarlos.planify.exception.BusinessRuleException;
import br.com.moisescarlos.planify.infrastructure.integration.notion.NotionClient;
import br.com.moisescarlos.planify.infrastructure.integration.telegram.TelegramClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CreateIntentStrategy implements IntentStrategy {

    private final List<TaskStrategy> strategies;
    private final List<TaskValidator> validators;
    private final NotionClient notionClient;
    private final TelegramClient telegramClient;
    private final GroqParser groqParser;

    @Override
    public boolean supports(String intent) {
        return "CREATE".equalsIgnoreCase(intent) || intent == null;
    }

    @Override
    public void execute(Objective objective) {
        Goal goal = new Goal(objective.getTitle(), objective.getAmount(), objective.getCategory());
        LocalDateTime start = objective.getSuggestedStartDate() != null ?
                objective.getSuggestedStartDate() :
                LocalDateTime.now().plusHours(1).withMinute(0);

        TaskStrategy strategy = strategies.stream()
                .filter(s -> s.supports(objective)).findFirst()
                .orElseThrow(() -> new BusinessRuleException("Erro: Nenhuma estratégia de geração encontrada para esta meta."));

        List<Task> tasks = strategy.generateTasks(objective, goal, start);
        validateTasks(tasks);
        executePlanCreation(goal, tasks);
    }

    private void executePlanCreation(Goal goal, List<Task> tasks) {
        telegramClient.sendMessage(buildTelegramMessage(goal, tasks));
        for (Task task : tasks) {
            List<String> conflicts = notionClient.getConflictingTaskNames(task.getScheduledDateTime(), 60, task.getTitle());
            if (!conflicts.isEmpty()) {
                String suggestion = groqParser.askForAlternative(task, conflicts);
                telegramClient.sendMessage("⚠️ <b>Conflito!</b> coincide com " + conflicts + ".\n💡 " + suggestion);
                continue;
            }
            String id = notionClient.createTaskPage(task);
            if (id != null) sendTaskToTelegram(task, id);
        }
    }

    private void sendTaskToTelegram(Task task, String pageId) {
        var keyboard = List.of(List.of(Map.of("text", "✅ Concluir", "callback_data", "done:" + pageId)));
        String msg = String.format("📌 <b>Tarefa:</b> %s\n📅 <b>Data:</b> %s",
                task.getTitle(), task.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
        telegramClient.sendWithButtons(msg, keyboard);
    }

    private String buildTelegramMessage(Goal goal, List<Task> tasks) {
        StringBuilder sb = new StringBuilder("✅ <b>Plano Gerado!</b>\n\n🎯 <b>Meta:</b> " + goal.getTitle() + "\n");
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        tasks.forEach(t -> sb.append("🔹 <i>").append(t.getScheduledDateTime().format(f)).append("</i> - ").append(t.getTitle()).append("\n"));
        return sb.toString();
    }

    private void validateTasks(List<Task> tasks) {
        List<Task> processed = new ArrayList<>();
        for (Task t : tasks) {
            validators.forEach(v -> v.validate(t, processed));
            processed.add(t);
        }
    }
}
