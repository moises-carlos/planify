package br.com.moisescarlos.planify.domain.model;

import br.com.moisescarlos.planify.domain.enums.ObjectiveType;
import br.com.moisescarlos.planify.exception.BusinessRuleException;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class Objective {
    private String title;
    private int amount;
    private ObjectiveType type;
    private String category;
    private LocalDateTime suggestedStartDate;
    private List<DayOfWeek> allowedDays;

    @Setter
    private String intent;

    public Objective(String title, int amount, ObjectiveType type, String category, LocalDateTime suggestedStartDate, List<DayOfWeek> allowedDays) {
        if (amount <= 0) {
            throw new BusinessRuleException("A quantidade estipulada (horas ou frequência) do objetivo deve ser maior que zero.");
        }
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.suggestedStartDate = suggestedStartDate;
        this.allowedDays = allowedDays;
        this.intent = "CREATE";
    }
}