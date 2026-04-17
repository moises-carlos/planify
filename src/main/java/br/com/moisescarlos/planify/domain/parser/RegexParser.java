package br.com.moisescarlos.planify.domain.parser;

import br.com.moisescarlos.planify.domain.enums.ObjectiveType;
import br.com.moisescarlos.planify.domain.model.Objective;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegexParser implements ObjectiveParser {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?<!as\\s|às\\s)(\\d+)(?:h|hrs|\\s?horas)");

    private static final Pattern CATEGORY_PATTERN = Pattern.compile("#(\\w+)");

    @Override
    public Objective parse(String input) {
        if (input.contains("mano") || input.contains("pensei") || input.split(" ").length > 6) {
            throw new RuntimeException("Frase complexa. Usando IA.");
        }

        Matcher amountMatcher = AMOUNT_PATTERN.matcher(input);
        int amount;

        if (amountMatcher.find()) {
            amount = Integer.parseInt(amountMatcher.group(1));

            if (amount > 8) {
                throw new RuntimeException("Duração suspeita (" + amount + "h). Validando com IA.");
            }
        } else {
            throw new RuntimeException("Nenhuma duração explícita. Deixando IA decidir.");
        }

        Matcher catMatcher = CATEGORY_PATTERN.matcher(input);
        String category = catMatcher.find() ? catMatcher.group(1) : "Geral";

        String title = input.replaceAll("#\\w+", "")
                .replaceAll("(?<!as\\s|às\\s)\\d+.*", "") // Não remove o horário do título se for "as 14h"
                .trim();

        return new Objective(title, amount, ObjectiveType.HOURS, category, null, null);
    }
}