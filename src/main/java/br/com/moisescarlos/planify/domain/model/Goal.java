package br.com.moisescarlos.planify.domain.model;
import br.com.moisescarlos.planify.domain.enums.GoalStatus;
import br.com.moisescarlos.planify.exception.BusinessRuleException;
import lombok.Getter;

@Getter
public class Goal {
    private String title;
    private int currentProgress;
    private int targetValue;
    private GoalStatus status;
    private String category;

    public Goal(String title, int targetValue, String category) {
        this.title = title;
        this.targetValue = targetValue;
        this.currentProgress = 0;
        this.status = GoalStatus.IN_PROGRESS;
        this.category = category;
    }

    public void addProgress(int taskValue) {
        if (this.status == GoalStatus.COMPLETED) {
            throw new BusinessRuleException("Não é possível adicionar progresso a uma meta já concluída.");
        }
        this.currentProgress += taskValue;

        if (this.currentProgress >= this.targetValue) {
            this.currentProgress = this.targetValue;
            this.status = GoalStatus.COMPLETED;
        }
    }

}
