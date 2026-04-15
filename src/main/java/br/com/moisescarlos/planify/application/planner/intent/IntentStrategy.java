package br.com.moisescarlos.planify.application.planner.intent;

import br.com.moisescarlos.planify.domain.model.Objective;

public interface IntentStrategy {
    boolean supports(String intent);
    void execute(Objective objective);
}
