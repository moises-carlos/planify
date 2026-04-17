package br.com.moisescarlos.planify.application.planner.intent.impl;

import br.com.moisescarlos.planify.application.planner.intent.IntentStrategy;
import br.com.moisescarlos.planify.domain.model.Objective;
import br.com.moisescarlos.planify.domain.model.Task;
import br.com.moisescarlos.planify.infrastructure.integration.notion.NotionClient;
import br.com.moisescarlos.planify.infrastructure.integration.telegram.TelegramClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MoveIntentStrategy implements IntentStrategy {

    private final NotionClient notionClient;
    private final TelegramClient telegramClient;
    @Lazy private final CreateIntentStrategy createIntentStrategy;

    @Override
    public boolean supports(String intent) {
        return "MOVE".equalsIgnoreCase(intent);
    }

    @Override
    public void execute(Objective objective) {
        List<Task> tasks = notionClient.findTasksByTitle(objective.getTitle());
        tasks.forEach(t -> notionClient.archivePage(t.getId()));
        telegramClient.sendMessage("🔄 Reorganizando: <b>" + objective.getTitle() + "</b>");
        createIntentStrategy.execute(objective);
    }
}
