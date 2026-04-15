package br.com.moisescarlos.planify.application.planner.strategy;

import br.com.moisescarlos.planify.domain.model.Goal;
import br.com.moisescarlos.planify.domain.model.Objective;
import br.com.moisescarlos.planify.domain.model.Task;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskStrategy {
    boolean supports(Objective objective); // Verifica se esta estratégia é a certa para o tipo de objetivo

    List<Task> generateTasks(Objective objective, Goal goal, LocalDateTime startDate);//Retorna a lista de tarefas geradas
}