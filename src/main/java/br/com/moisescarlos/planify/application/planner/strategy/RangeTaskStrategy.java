package br.com.moisescarlos.planify.application.planner.strategy;

import br.com.moisescarlos.planify.domain.enums.ObjectiveType;
import br.com.moisescarlos.planify.domain.model.Goal;
import br.com.moisescarlos.planify.domain.model.Objective;
import br.com.moisescarlos.planify.domain.model.Task;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class RangeTaskStrategy implements TaskStrategy {

    @Override
    public List<Task> generateTasks(Objective objective, Goal goal, LocalDateTime startDate) {
        List<Task> tasks = new ArrayList<>();
        List<DayOfWeek> allowedDays = objective.getAllowedDays();

        int remainingHours = objective.getAmount();
        LocalDateTime current = startDate;

        while (remainingHours > 0) {
            if (allowedDays.contains(current.getDayOfWeek())) {
                int hoursForToday = Math.min(remainingHours, 2);
                tasks.add(new Task(objective.getTitle(), current, hoursForToday * 60, goal, objective.getCategory()));
                remainingHours -= hoursForToday;
            }
            current = current.plusDays(1).withHour(8).withMinute(0);
        }
        return tasks;
    }
    @Override
    public boolean supports(Objective objective) {
        return objective.getType() == ObjectiveType.RANGE;
    }
}