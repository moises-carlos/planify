package br.com.moisescarlos.planify.domain.model;

import br.com.moisescarlos.planify.domain.enums.TaskStatus;
import br.com.moisescarlos.planify.exception.BusinessRuleException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class Task {
    @Setter private String id;
    private String title;
    private LocalDateTime scheduledDateTime;
    private int durationMinutes;
    private TaskStatus status;
    private Goal associatedGoal;
    private String category;

    public Task(String title, LocalDateTime scheduledDateTime, int durationMinutes, Goal associatedGoal, String category) {
        if (associatedGoal == null) {
            throw new BusinessRuleException("Every task must be linked to a goal.");
        }
        this.title = title;
        this.scheduledDateTime = scheduledDateTime;
        this.durationMinutes = durationMinutes;
        this.associatedGoal = associatedGoal;
        this.status = TaskStatus.PENDING;
        this.category = category;
    }

    public void complete() {
        if (this.status == TaskStatus.COMPLETED) return;
        this.status = TaskStatus.COMPLETED;
        this.associatedGoal.addProgress(this.durationMinutes);
    }
}