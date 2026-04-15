package br.com.moisescarlos.planify.domain.parser;

import br.com.moisescarlos.planify.domain.enums.ObjectiveType;
import br.com.moisescarlos.planify.domain.model.Objective;
import br.com.moisescarlos.planify.domain.model.Task;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GroqParser {

    private static final Logger log = LoggerFactory.getLogger(GroqParser.class);

    @Value("${groq.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${groq.model.name:llama-3.1-8b-instant}")
    private String modelName;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public GroqParser(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Objective parseWithAI(String userInput) {
        // 📅 GERADOR DE CALENDÁRIO: Cria a tabela dos próximos 7 dias para a IA não errar
        StringBuilder calendarRef = new StringBuilder();
        calendarRef.append("Tabela de Referência (Use para calcular as datas):\n");
        for (int i = 0; i <= 7; i++) {
            LocalDate d = LocalDate.now().plusDays(i);
            String dayName = d.format(DateTimeFormatter.ofPattern("EEEE", new Locale("pt", "BR")));
            calendarRef.append("- ").append(dayName).append(": ").append(d).append("\n");
        }

        String todayName = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE", new Locale("pt", "BR")));

        // ✅ PROMPT ESTRUTURADO COM CALENDÁRIO INJETADO
        String systemPrompt = "Você é o cérebro do Planify. Extraia os dados do usuário e retorne APENAS um objeto JSON válido.\n" +
                "CONTEXTO TEMPORAL:\n" +
                "Hoje é " + todayName + " (" + LocalDate.now() + ").\n" +
                calendarRef.toString() + "\n" +
                "REGRAS VITAIS:\n" +
                "1. TITLE: Nome limpo da tarefa. REMOVA verbos de comando como 'mudar', 'passar', 'apagar' (ex: 'mudar revisar projeto' vira 'revisar projeto').\n" +
                "2. INTENT: 'MOVE' (mudar, alterar, reagendar), 'DELETE' (apagar, remover) ou 'CREATE' (padrão).\n" +
                "3. AMOUNT: Duração em horas (int). O padrão é 1.\n" +
                "4. SUGGESTEDDATE: Retorne OBRIGATORIAMENTE no formato ISO8601 (yyyy-MM-ddTHH:mm:ssZ). Consulte a Tabela de Referência acima para mapear o dia correto!\n\n" +
                "O JSON OBRIGATÓRIO (Mantenha esta estrutura):\n" +
                "{\n" +
                "  \"title\": \"Nome da tarefa\",\n" +
                "  \"intent\": \"CREATE, MOVE ou DELETE\",\n" +
                "  \"amount\": 1,\n" +
                "  \"type\": \"HOURS\",\n" +
                "  \"suggestedDate\": \"2026-04-16T12:00:00Z\",\n" +
                "  \"category\": \"Geral\"\n" +
                "}";

        Map<String, Object> body = Map.of(
                "model", modelName,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userInput)
                ),
                "response_format", Map.of("type", "json_object")
        );

        return executeObjectiveRequest(body, userInput);
    }

    private Objective executeObjectiveRequest(Map<String, Object> body, String userInput) {
        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String aiJson = root.path("choices").get(0).path("message").path("content").asText();

            log.debug("JSON bruto retornado pela IA: {}", aiJson);

            JsonNode data = objectMapper.readTree(aiJson);

            String title = findField(data, "title", "TITLE").asText("Tarefa sem título");
            String intent = extractIntentSmart(data);

            int amount = findField(data, "amount", "AMOUNT").asInt(1);
            if (intent.equals("DELETE")) {
                amount = 1;
            } else if (amount > 4) {
                log.warn("Amount recebido ({}) excede limite. Ajustando para 2.", amount);
                amount = 2;
            }

            LocalDateTime date = parseSmartDate(data, userInput);

            String typeVal = findField(data, "type", "TYPE").asText("HOURS").toUpperCase();
            ObjectiveType objType = typeVal.contains("FREQ") ? ObjectiveType.FREQUENCY : ObjectiveType.HOURS;

            Objective objective = new Objective(
                    title, amount, objType,
                    findField(data, "category", "CATEGORY").asText("Geral"),
                    date, null
            );
            objective.setIntent(intent);
            return objective;

        } catch (Exception e) {
            log.error("Falha ao processar resposta da IA. Erro: {}", e.getMessage(), e);
            return new Objective("Erro na IA", 1, ObjectiveType.HOURS, "Erro",
                    LocalDateTime.now().plusHours(1).withMinute(0), null);
        }
    }

    private JsonNode findField(JsonNode node, String... fieldNames) {
        for (String name : fieldNames) {
            if (node.has(name)) return node.get(name);
        }
        String[] wrappers = {"data", "MOVE", "idPlanos", "planos", "action"};
        for (String wrapper : wrappers) {
            if (node.has(wrapper)) {
                JsonNode subNode = node.get(wrapper);
                for (String name : fieldNames) {
                    if (subNode.has(name)) return subNode.get(name);
                }
            }
        }
        return node.path("non_existent");
    }

    private String extractIntentSmart(JsonNode data) {
        if (data.path("DELETE").asBoolean() || data.path("delete").asBoolean()) return "DELETE";
        if (data.path("MOVE").asBoolean() || data.path("move").asBoolean()) return "MOVE";

        String val = findField(data, "intent", "INTENT", "action", "type", "TYPE").asText("CREATE").toUpperCase();
        if (val.contains("DEL")) return "DELETE";
        if (val.contains("MOV")) return "MOVE";
        return "CREATE";
    }

    private LocalDateTime parseSmartDate(JsonNode data, String userInput) {
        String rawDate = findField(data, "suggestedDate", "suggested_date", "SUGGESTEDDATE").asText("");
        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        if (rawDate.isEmpty()) return now.plusHours(1);

        LocalDateTime date = tryParseDateTime(rawDate, now);

        // Dupla proteção: Se o usuário digitou "amanhã", mas a IA retornou a data de hoje
        if (userInput.toLowerCase().contains("amanhã") && date.toLocalDate().equals(LocalDate.now())) {
            log.debug("Correção de fallback ativada para 'amanhã'.");
            date = date.plusDays(1);
        }

        return date.withMinute(0).withSecond(0).withNano(0);
    }

    private LocalDateTime tryParseDateTime(String rawDate, LocalDateTime fallback) {
        // Limpa a "sujeira" do timezone que o Llama 3 às vezes gera (+0000 vira Z)
        String sanitizedDate = rawDate.replaceAll("\\+0000$", "Z");

        if (sanitizedDate.length() <= 10) {
            try {
                return LocalDate.parse(sanitizedDate.substring(0, 10)).atTime(10, 0);
            } catch (DateTimeParseException e) {
                log.warn("Falha ao parsear data curta '{}': {}", sanitizedDate, e.getMessage());
            }
        }

        try {
            return OffsetDateTime.parse(sanitizedDate).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        try {
            String cleaned = sanitizedDate.split("\\.")[0].replace("Z", "");
            return LocalDateTime.parse(cleaned);
        } catch (DateTimeParseException e) {
            log.warn("Falha ao parsear data '{}'. Usando fallback.", rawDate);
            return fallback.plusHours(1);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public String askForAlternative(Task task, List<String> conflicts) {
        String prompt = String.format("A tarefa '%s' às %s conflita com: %s. Sugira novo horário em 1 frase.",
                task.getTitle(), task.getScheduledDateTime(), String.join(", ", conflicts));

        Map<String, Object> body = Map.of(
                "model", modelName,
                "messages", List.of(
                        Map.of("role", "system", "content", "Você é um assistente de agenda curto e direto."),
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 100
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText("Sugiro tentar 1 hora mais tarde.");
        } catch (Exception e) {
            log.error("Falha buscar alternativa IA '{}': {}", task.getTitle(), e.getMessage());
            return "Sugiro tentar 1 hora mais tarde.";
        }
    }
}