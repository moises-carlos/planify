package br.com.moisescarlos.planify.application.planner.usecase;

import br.com.moisescarlos.planify.application.planner.intent.IntentStrategy;
import br.com.moisescarlos.planify.domain.model.Objective;
import br.com.moisescarlos.planify.domain.parser.GroqParser;
import br.com.moisescarlos.planify.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HandlePlannerCommandUseCase {

    private final GroqParser groqParser;
    private final List<IntentStrategy> intentStrategies;

    public void execute(String userInput) {
        // IA assume o controle total
        Objective objective = groqParser.parseWithAI(userInput);
        String intent = objective.getIntent();

        System.out.println("LOG: Processando " + intent + " para " + objective.getTitle());

        intentStrategies.stream()
                .filter(s -> s.supports(intent))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Erro: Intenção '" + intent + "' não suportada."))
                .execute(objective);
    }
}
