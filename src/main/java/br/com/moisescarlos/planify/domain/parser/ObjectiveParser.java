package br.com.moisescarlos.planify.domain.parser;
import br.com.moisescarlos.planify.domain.model.Objective;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ObjectiveParser {
    Objective parse(String input);

}
