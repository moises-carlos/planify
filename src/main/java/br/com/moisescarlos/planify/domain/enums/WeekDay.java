package br.com.moisescarlos.planify.domain.enums;

import lombok.Getter;

import java.time.DayOfWeek;
import java.util.Arrays;

public enum WeekDay {
    SEGUNDA(DayOfWeek.MONDAY, "segunda", "seg"),
    TERCA(DayOfWeek.TUESDAY, "terça", "ter"),
    QUARTA(DayOfWeek.WEDNESDAY, "quarta", "qua"),
    QUINTA(DayOfWeek.THURSDAY, "quinta", "qui"),
    SEXTA(DayOfWeek.FRIDAY, "sexta", "sex"),
    SABADO(DayOfWeek.SATURDAY, "sábado", "sab"),
    DOMINGO(DayOfWeek.SUNDAY, "domingo", "dom");

    @Getter
    private final DayOfWeek dayOfWeek;
    private final String[] aliases;

    WeekDay(DayOfWeek dayOfWeek, String... aliases) {
        this.dayOfWeek = dayOfWeek;
        this.aliases = aliases;
    }
    public static WeekDay fromText(String text) {
        String lowerText = text.toLowerCase();
        for (WeekDay day : values()) {
            // Verifica se o texto contém algum dos apelidos (ex: "seg" ou "segunda")
            if (Arrays.stream(day.aliases).anyMatch(lowerText::contains)) {
                return day;
            }
        }
        return null;
    }
}