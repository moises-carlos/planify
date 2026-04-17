package br.com.moisescarlos.planify.infrastructure.integration.notion.dto;

import java.util.List;
import java.util.Map;

public record NotionPageRequest(
        Parent parent,
        Map<String, Object> properties
) {
    public record Parent(String database_id) {}

    public static Map<String, Object> textField(String content) {
        return Map.of("title", List.of(Map.of("text", Map.of("content", content))));
    }

    public static Map<String, Object> dateField(String isoDate) {
        return Map.of("date", Map.of("start", isoDate));
    }
}