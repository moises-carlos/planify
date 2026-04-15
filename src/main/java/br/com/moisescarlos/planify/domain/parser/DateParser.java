package br.com.moisescarlos.planify.domain.parser;

import br.com.moisescarlos.planify.domain.enums.WeekDay;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateParser {

    // Extrai o Horário (ex: 14:30, 14h30, as 14h)
    public static LocalTime parseTime(String input) {
        String text = input.toLowerCase();
        // Procura por "as 10h", "às 10:30" ou o formato de relógio "10:30"
        Matcher timeMatcher = Pattern.compile("(?:as|às|at)\\s+(\\d{1,2})(?:[:h](\\d{2})?)?|(\\d{1,2}:\\d{2})").matcher(text);

        if (timeMatcher.find()) {
            int hour;
            int minute = 0;

            if (timeMatcher.group(3) != null) { // Formato 14:30
                String[] parts = timeMatcher.group(3).split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } else { // Formato "as 14h" ou "as 14:30"
                hour = Integer.parseInt(timeMatcher.group(1));
                if (timeMatcher.group(2) != null) {
                    minute = Integer.parseInt(timeMatcher.group(2));
                }
            }

            if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                return LocalTime.of(hour, minute);
            }
        }
        return null;
    }

    public static LocalDateTime parse(String input) {
        String text = input.toLowerCase();
        if (text.contains("amanhã")) return LocalDate.now().plusDays(1).atTime(8, 0);

        WeekDay identifiedDay = WeekDay.fromText(text);
        if (identifiedDay != null) {
            LocalDate nextDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(identifiedDay.getDayOfWeek()));
            return nextDate.atTime(8, 0);
        }

        Matcher dateMatcher = Pattern.compile("(\\d{1,2})/(\\d{1,2})").matcher(text);
        if (dateMatcher.find()) {
            int day = Integer.parseInt(dateMatcher.group(1));
            int month = Integer.parseInt(dateMatcher.group(2));
            return LocalDate.of(LocalDate.now().getYear(), month, day).atTime(8, 0);
        }
        return null;
    }

    public static List<DayOfWeek> parseDayRange(String input) {
        String text = input.toLowerCase();
        Matcher rangeMatcher = Pattern.compile("(seg|ter|qua|qui|sex|sab|dom).*? a .*?(seg|ter|qua|qui|sex|sab|dom)").matcher(text);

        if (rangeMatcher.find()) {
            WeekDay startDay = WeekDay.fromText(rangeMatcher.group(1));
            WeekDay endDay = WeekDay.fromText(rangeMatcher.group(2));
            if (startDay != null && endDay != null) {
                List<DayOfWeek> days = new ArrayList<>();
                int start = startDay.getDayOfWeek().getValue();
                int end = endDay.getDayOfWeek().getValue();
                if (start <= end) {
                    for (int i = start; i <= end; i++) days.add(DayOfWeek.of(i));
                } else {
                    for (int i = start; i <= 7; i++) days.add(DayOfWeek.of(i));
                    for (int i = 1; i <= end; i++) days.add(DayOfWeek.of(i));
                }
                return days;
            }
        }
        return Collections.emptyList();
    }
}