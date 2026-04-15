package br.com.moisescarlos.planify.application.planner.strategy;

import br.com.moisescarlos.planify.domain.enums.ObjectiveType;
import br.com.moisescarlos.planify.domain.model.Goal;
import br.com.moisescarlos.planify.domain.model.Objective;
import br.com.moisescarlos.planify.domain.model.Task;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class HoursTaskStrategy implements TaskStrategy {

    private static final int TASK_DURATION_MINUTES = 60;
    private static final int BREAK_DURATION_MINUTES = 15;

    @Override
    public boolean supports(Objective objective) {
        return objective.getType() == ObjectiveType.HOURS;
    }

    @Override
    public List<Task> generateTasks(Objective objective, Goal goal, LocalDateTime startDate) {
        List<Task> generatedTasks = new ArrayList<>();

        int totalHours = objective.getAmount();
        int totalMinutesToPlan = totalHours * 60;
        int plannedMinutes = 0;

        LocalDateTime currentTaskDate = startDate;
        int part = 1;

        // ✅ Cálculo prévio para saber se haverá mais de uma parte
        int totalParts = (int) Math.ceil((double) totalMinutesToPlan / TASK_DURATION_MINUTES);

        while (plannedMinutes < totalMinutesToPlan) {
            int currentBlockMinutes = Math.min(TASK_DURATION_MINUTES, totalMinutesToPlan - plannedMinutes);

            // ✅ LÓGICA REFORMULADA:
            // Só adiciona "(Part X)" se o objetivo total for maior que um único bloco (60 min)
            String finalTitle = objective.getTitle();
            if (totalParts > 1) {
                finalTitle += " (Part " + part + ")";
            }

            Task task = new Task(
                    finalTitle,
                    currentTaskDate,
                    currentBlockMinutes,
                    goal,
                    objective.getCategory()
            );

            generatedTasks.add(task);

            plannedMinutes += currentBlockMinutes;
            part++;

            currentTaskDate = currentTaskDate
                    .plusMinutes(currentBlockMinutes)
                    .plusMinutes(BREAK_DURATION_MINUTES);
        }

        return generatedTasks;
    }
}