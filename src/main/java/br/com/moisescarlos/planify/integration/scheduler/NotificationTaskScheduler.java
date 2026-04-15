package br.com.moisescarlos.planify.integration.scheduler;

import br.com.moisescarlos.planify.domain.model.Task;
import br.com.moisescarlos.planify.integration.notion.NotionClient;
import br.com.moisescarlos.planify.integration.telegram.TelegramClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationTaskScheduler.class);

    private final NotionClient notionClient;
    private final TelegramClient telegramClient;

    // ✅ FIX 3: Controle de tarefas já notificadas para evitar duplicatas
    private final Set<String> notifiedTaskIds = ConcurrentHashMap.newKeySet();

    public NotificationTaskScheduler(NotionClient notionClient, TelegramClient telegramClient) {
        this.notionClient = notionClient;
        this.telegramClient = telegramClient;
    }

    // ✅ FIX 6: Cron configurável via properties com fallback padrão (a cada 1 minuto)
    @Scheduled(cron = "${planify.scheduler.cron:0 * * * * *}")
    public void checkUpcomingTasks() {
        LocalDateTime now = LocalDateTime.now();

        // ✅ FIX 2: Busca em janela de tempo (agora+9min até agora+11min)
        // Evita dependência de precisão de segundos
        LocalDateTime windowStart = now.plusMinutes(9);
        LocalDateTime windowEnd = now.plusMinutes(11);

        log.debug("Verificando tarefas entre {} e {}", windowStart, windowEnd);

        try {
            // ✅ FIX 2: Método agora recebe range ao invés de instante exato
            List<Task> upcomingTasks = notionClient.findTasksInTimeRange(windowStart, windowEnd);

            for (Task task : upcomingTasks) {
                // ✅ FIX 3: Pula tarefas já notificadas
                if (notifiedTaskIds.contains(task.getId())) {
                    log.debug("Tarefa '{}' já notificada, ignorando.", task.getTitle());
                    continue;
                }

                // ✅ FIX 5: Try/catch por tarefa — falha em uma não bloqueia as demais
                try {
                    String alert = String.format(
                            "⏰ <b>Atenção!</b>\nSua tarefa <i>\"%s\"</i> começa em 10 minutos!",
                            task.getTitle()
                    );
                    telegramClient.sendMessage(alert);
                    notifiedTaskIds.add(task.getId());
                    log.info("Notificação enviada para tarefa '{}'", task.getTitle());

                } catch (Exception e) {
                    log.error("Falha ao enviar notificação para tarefa '{}': {}", task.getTitle(), e.getMessage(), e);
                }
            }

            // ✅ Limpeza: remove IDs de tarefas que já passaram para evitar vazamento de memória
            cleanupNotifiedIds(now);

        } catch (Exception e) {
            log.error("Falha ao buscar tarefas no Notion: {}", e.getMessage(), e);
        }
    }

    // Remove tarefas já passadas do Set para não crescer indefinidamente
    private void cleanupNotifiedIds(LocalDateTime now) {
        try {
            List<Task> pastTasks = notionClient.findTasksInTimeRange(
                    now.minusMinutes(30), now.minusMinutes(1)
            );
            pastTasks.stream()
                    .map(Task::getId)
                    .forEach(notifiedTaskIds::remove);
        } catch (Exception e) {
            log.warn("Falha na limpeza de IDs notificados: {}", e.getMessage());
        }
    }
}