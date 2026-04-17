package br.com.moisescarlos.planify.application.planner.intent.impl;

import br.com.moisescarlos.planify.application.planner.intent.IntentStrategy;
import br.com.moisescarlos.planify.domain.model.Objective;
import br.com.moisescarlos.planify.domain.model.Task;
import br.com.moisescarlos.planify.infrastructure.integration.notion.NotionClient;
import br.com.moisescarlos.planify.infrastructure.integration.telegram.TelegramClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeleteIntentStrategy implements IntentStrategy {

    private final NotionClient notionClient;
    private final TelegramClient telegramClient;

    @Override
    public boolean supports(String intent) {
        return "DELETE".equalsIgnoreCase(intent);
    }

    @Override
    public void execute(Objective objective) {
        List<Task> tasks = notionClient.findTasksByTitle(objective.getTitle());
        if (tasks.isEmpty()) {
            telegramClient.sendMessage("⚠️ Nada para remover de '<b>" + objective.getTitle() + "</b>'.");
            return;
        }
        tasks.forEach(t -> notionClient.archivePage(t.getId()));
        telegramClient.sendMessage("🗑️ Removi as tarefas de '<b>" + objective.getTitle() + "</b>'.");
    }
}
