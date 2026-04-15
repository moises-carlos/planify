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

    // Cada bloco de estudo/trabalho terá 60 minutos (1h)
    private static final int TASK_DURATION_MINUTES = 60;
    // Intervalo de descanso entre os blocos
    private static final int BREAK_DURATION_MINUTES = 15;

    @Override
    public boolean supports(Objective objective) {
        return objective.getType() == ObjectiveType.HOURS;
    }

    @Override
    public List<Task> generateTasks(Objective objective, Goal goal, LocalDateTime startDate) {
        List<Task> generatedTasks = new ArrayList<>();

        // Quantidade total de horas solicitada (ex: 2h)
        int totalHours = objective.getAmount();
        int totalMinutesToPlan = totalHours * 60;
        int plannedMinutes = 0;

        LocalDateTime currentTaskDate = startDate;
        int part = 1;

        while (plannedMinutes < totalMinutesToPlan) {
            // Se restarem menos de 60 min, pega o restante, senão pega 60 min
            int currentBlockMinutes = Math.min(TASK_DURATION_MINUTES, totalMinutesToPlan - plannedMinutes);

            // Adicionamos "(Part X)" ao título para você saber a ordem no Notion/Telegram
            String titleWithPart = objective.getTitle() + " (Part " + part + ")";

            Task task = new Task(
                    titleWithPart,
                    currentTaskDate,
                    currentBlockMinutes,
                    goal,
                    objective.getCategory()
            );

            generatedTasks.add(task);

            plannedMinutes += currentBlockMinutes;
            part++;

            // CÁLCULO DO PRÓXIMO HORÁRIO:
            // Horário de início da tarefa anterior + duração da tarefa + intervalo
            currentTaskDate = currentTaskDate
                    .plusMinutes(currentBlockMinutes)
                    .plusMinutes(BREAK_DURATION_MINUTES);
        }

        return generatedTasks;
    }
}