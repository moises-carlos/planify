package br.com.moisescarlos.planify.domain.parser;

import br.com.moisescarlos.planify.domain.model.Objective;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class SmartObjectiveParser implements ObjectiveParser {

    private final RegexParser regexParser;
    private final GroqParser groqParser;

    public SmartObjectiveParser(RegexParser regexParser, GroqParser groqParser) {
        this.regexParser = regexParser;
        this.groqParser = groqParser;
    }

    @Override
    public Objective parse(String input) {
        try {
            // Só aceita o Regex se for um comando curto e ultra específico
            return regexParser.parse(input);
        } catch (Exception e) {
            System.out.println("Regex falhou ou frase complexa. Ativando cérebro da IA...");
            return groqParser.parseWithAI(input);
        }
    }
}