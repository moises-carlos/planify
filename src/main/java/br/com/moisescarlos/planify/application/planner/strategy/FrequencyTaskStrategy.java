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
public class FrequencyTaskStrategy implements  TaskStrategy{
    private static final int DEFAULT_DURATION = 60;

    @Override
    public boolean supports(Objective objective) {
        return objective.getType() == ObjectiveType.FREQUENCY;
    }

    @Override
    public List<Task> generateTasks(Objective objective, Goal goal, LocalDateTime startDate) {
        List<Task> tasks = new ArrayList<>();

        int times = objective.getAmount();
        LocalDateTime currentDateTime = startDate;

        for (int i = 0; i < times; i++) {
            Task task = new Task(
                    objective.getTitle(),
                    currentDateTime,
                    DEFAULT_DURATION,
                    goal,
                    objective.getCategory()
            );
            tasks.add(task);

            currentDateTime = currentDateTime.plusDays(1);
        }

        return tasks;
    }
}
