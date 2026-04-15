package br.com.moisescarlos.planify.application.planner.validator;

import br.com.moisescarlos.planify.domain.model.Task;
import br.com.moisescarlos.planify.exception.BusinessRuleException;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class MinimumIntervalValidator implements TaskValidator {

    private static final int MIN_INTERVAL_MINUTES = 15; // Intervalo padrão de 15 minutos

    @Override
    public void validate(Task newTask, List<Task> existingTasks) {
        for (Task existing : existingTasks) {
            // Verifica se a nova tarefa começa muito colada com o fim de outra no mesmo dia
            long interval = java.time.Duration.between(existing.getScheduledDateTime().plusMinutes(existing.getDurationMinutes()), newTask.getScheduledDateTime()).toMinutes();

            if (interval > 0 && interval < MIN_INTERVAL_MINUTES) {
                throw new BusinessRuleException("Intervalo insuficiente entre tarefas. Mínimo de 15 minutos exigido.");
            }
        }
    }
}