package br.com.moisescarlos.planify.application.planner;

import br.com.moisescarlos.planify.application.planner.usecase.HandlePlannerCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlannerService {

    private final HandlePlannerCommandUseCase handlePlannerCommandUseCase;

    public void handleCommand(String userInput) {
        handlePlannerCommandUseCase.execute(userInput);
    }
}