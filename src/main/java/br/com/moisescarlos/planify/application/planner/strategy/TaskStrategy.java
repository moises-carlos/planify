package br.com.moisescarlos.planify.application.planner.strategy;

import br.com.moisescarlos.planify.domain.model.Goal;
import br.com.moisescarlos.planify.domain.model.Objective;
import br.com.moisescarlos.planify.domain.model.Task;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskStrategy {
    boolean supports(Objective objective);

    List<Task> generateTasks(Objective objective, Goal goal, LocalDateTime startDate);
}