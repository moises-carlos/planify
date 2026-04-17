package br.com.moisescarlos.planify.application.planner.validator;

import br.com.moisescarlos.planify.domain.model.Task;
import java.util.List;

public interface TaskValidator {
    void validate(Task newTask, List<Task> existingTasks);
}