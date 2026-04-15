package br.com.moisescarlos.planify.integration.notion.dto;

import java.util.List;
import java.util.Map;

// Representa a estrutura básica para criar uma página em uma base de dados
public record NotionPageRequest(
        Parent parent,
        Map<String, Object> properties
) {
    public record Parent(String database_id) {}

    // Helpers para criar os campos comuns (Texto, Data, Checkbox)
    public static Map<String, Object> textField(String content) {
        return Map.of("title", List.of(Map.of("text", Map.of("content", content))));
    }

    public static Map<String, Object> dateField(String isoDate) {
        return Map.of("date", Map.of("start", isoDate));
    }
}