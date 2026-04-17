package br.com.moisescarlos.planify.application.planner.validator;

import br.com.moisescarlos.planify.domain.model.Task;
import br.com.moisescarlos.planify.exception.BusinessRuleException;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class NoOverlapValidator implements TaskValidator {

    @Override
    public void validate(Task newTask, List<Task> existingTasks) {
        for (Task existing : existingTasks) {
            if (newTask.getScheduledDateTime().isBefore(existing.getScheduledDateTime().plusMinutes(existing.getDurationMinutes())) &&
                    existing.getScheduledDateTime().isBefore(newTask.getScheduledDateTime().plusMinutes(newTask.getDurationMinutes()))) {
                throw new BusinessRuleException("Conflito de horário: Já existe uma tarefa agendada neste período."); 
            }
        }
    }
}