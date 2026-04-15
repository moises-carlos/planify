package br.com.moisescarlos.planify.integration.notion;

import br.com.moisescarlos.planify.domain.model.Goal;
import br.com.moisescarlos.planify.domain.model.Task;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class NotionClient {

    private static final Logger log = LoggerFactory.getLogger(NotionClient.class);
    private final RestTemplate restTemplate;

    @Value("${notion.api.token}")
    private String apiKey;

    @Value("${notion.database.id}")
    private String databaseId;

    public NotionClient() {
        // Factory necessária para habilitar o método PATCH (usado em update e archive)
        var factory = new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault());
        this.restTemplate = new RestTemplate(factory);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + apiKey);
        h.set("Notion-Version", "2022-06-28");
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    public String createTaskPage(Task task) {
        String url = "https://api.notion.com/v1/pages";
        ZonedDateTime zonedDateTime = task.getScheduledDateTime().atZone(ZoneId.of("America/Sao_Paulo"));

        Map<String, Object> properties = new HashMap<>();
        properties.put("Nome", Map.of("title", List.of(Map.of("text", Map.of("content", task.getTitle())))));
        properties.put("Data", Map.of("date", Map.of("start", zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))));
        properties.put("Status", Map.of("status", Map.of("name", "Não iniciada")));
        properties.put("Categoria", Map.of("select", Map.of("name", task.getCategory())));

        Map<String, Object> body = Map.of("parent", Map.of("database_id", databaseId), "properties", properties);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, createHeaders()), Map.class);
            String id = (String) response.getBody().get("id");
            log.info("Sucesso ao criar página no Notion: {} (ID: {})", task.getTitle(), id);
            return id;
        } catch (Exception e) {
            log.error("Falha crítica ao criar página no Notion para '{}': {}", task.getTitle(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Busca tarefas por título (Contém). Essencial para MOVE e DELETE.
     */
    public List<Task> findTasksByTitle(String title) {
        String url = "https://api.notion.com/v1/databases/" + databaseId + "/query";

        // Filtro: Título contém a string informada AND Status não está concluído
        Map<String, Object> requestBody = Map.of("filter", Map.of("and", List.of(
                Map.of("property", "Nome", "rich_text", Map.of("contains", title)),
                Map.of("property", "Status", "status", Map.of("does_not_equal", "Concluído"))
        )));

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(requestBody, createHeaders()), Map.class);
            List<Map> results = (List<Map>) response.getBody().get("results");
            List<Task> tasks = new ArrayList<>();

            if (results != null) {
                for (Map result : results) {
                    Map<String, Object> props = (Map<String, Object>) result.get("properties");
                    List<Map> titleList = (List<Map>) ((Map) props.get("Nome")).get("title");
                    String taskName = titleList.isEmpty() ? "Sem nome" : (String) titleList.get(0).get("plain_text");

                    Task t = new Task(taskName, LocalDateTime.now(), 0, new Goal("Busca", 1, "Sistema"), "Geral");
                    t.setId((String) result.get("id"));
                    tasks.add(t);
                }
            }
            log.debug("Encontradas {} tarefas com o título '{}'", tasks.size(), title);
            return tasks;
        } catch (Exception e) {
            log.error("Erro ao buscar tarefas por título ({}): {}", title, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Atualiza o Status para 'Concluído' ou arquiva a página (Delete).
     */
    public void updateTaskStatus(String pageId, String newStatus) {
        String url = "https://api.notion.com/v1/pages/" + pageId;
        Map<String, Object> body = Map.of("properties", Map.of("Status", Map.of("status", Map.of("name", newStatus))));
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body, createHeaders()), String.class);
            log.info("Status da página {} atualizado para {}", pageId, newStatus);
        } catch (Exception e) {
            log.error("Erro ao atualizar status da página {}: {}", pageId, e.getMessage(), e);
        }
    }

    public void archivePage(String pageId) {
        String url = "https://api.notion.com/v1/pages/" + pageId;
        Map<String, Object> body = Map.of("archived", true);
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body, createHeaders()), String.class);
            log.info("Página {} arquivada com sucesso", pageId);
        } catch (Exception e) {
            log.error("Erro ao arquivar página {}: {}", pageId, e.getMessage(), e);
        }
    }

    /**
     * Métodos de Verificação de Conflito e Listagem
     */
    public List<String> getConflictingTaskNames(LocalDateTime start, int duration, String currentTaskTitle) {
        List<Task> overlapping = findTasksInTimeRange(start, start.plusMinutes(duration));
        String baseTitle = currentTaskTitle.split("\\(")[0].trim().toLowerCase();

        return overlapping.stream()
                .map(Task::getTitle)
                .filter(title -> !title.toLowerCase().contains(baseTitle))
                .distinct().toList();
    }

    public List<Task> findTasksInTimeRange(LocalDateTime start, LocalDateTime end) {
        String url = "https://api.notion.com/v1/databases/" + databaseId + "/query";
        String isoStart = start.atZone(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String isoEnd = end.atZone(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        Map<String, Object> body = Map.of("filter", Map.of("and", List.of(
                Map.of("property", "Data", "date", Map.of("on_or_after", isoStart)),
                Map.of("property", "Data", "date", Map.of("on_or_before", isoEnd)),
                Map.of("property", "Status", "status", Map.of("does_not_equal", "Concluído"))
        )));

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, createHeaders()), Map.class);
            List<Map> results = (List<Map>) response.getBody().get("results");
            List<Task> tasks = new ArrayList<>();
            if (results != null) {
                Goal dummyGoal = new Goal("Intervalo", 1, "Sistema");
                for (Map result : results) {
                    Map<String, Object> props = (Map<String, Object>) result.get("properties");
                    List<Map> tL = (List<Map>) ((Map) props.get("Nome")).get("title");
                    String t = tL.isEmpty() ? "Tarefa" : (String) tL.get(0).get("plain_text");
                    
                    Task task = new Task(t, start, 0, dummyGoal, "Geral");
                    task.setId((String) result.get("id"));
                    tasks.add(task);
                }
            }
            return tasks;
        } catch (Exception e) {
            log.error("Erro ao buscar tarefas no intervalo: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, String>> getTasksForToday() {
        String url = "https://api.notion.com/v1/databases/" + databaseId + "/query";
        Map<String, Object> body = Map.of("filter", Map.of("and", List.of(
                Map.of("property", "Data", "date", Map.of("equals", LocalDate.now().toString())),
                Map.of("property", "Status", "status", Map.of("does_not_equal", "Concluído"))
        )));
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, createHeaders()), Map.class);
            List<Map> results = (List<Map>) response.getBody().get("results");
            List<Map<String, String>> tasks = new ArrayList<>();
            if (results != null) {
                for (Map r : results) {
                    Map<String, Object> p = (Map<String, Object>) r.get("properties");
                    List<Map> tL = (List<Map>) ((Map) p.get("Nome")).get("title");
                    tasks.add(Map.of("id", (String) r.get("id"), "name", (String) tL.get(0).get("plain_text")));
                }
            }
            return tasks;
        } catch (Exception e) { return List.of(); }
    }
    public List<Task> findTasksByTime(LocalDateTime targetTime) {
        String url = "https://api.notion.com/v1/databases/" + databaseId + "/query";

        // Formata para o padrão ISO que o Notion entende (Ex: 2026-04-15T15:00:00-03:00)
        String isoDate = targetTime.withSecond(0).withNano(0)
                .atZone(ZoneId.of("America/Sao_Paulo"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // Filtro: Data EXATAMENTE igual ao targetTime AND Status não concluído
        Map<String, Object> body = Map.of(
                "filter", Map.of(
                        "and", List.of(
                                Map.of("property", "Data", "date", Map.of("equals", isoDate)),
                                Map.of("property", "Status", "status", Map.of("does_not_equal", "Concluído"))
                        )
                )
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, createHeaders()), Map.class);
            List<Map> results = (List<Map>) response.getBody().get("results");

            List<Task> tasks = new ArrayList<>();
            if (results != null) {
                Goal dummyGoal = new Goal("Notificação", 1, "Sistema");
                for (Map result : results) {
                    Map<String, Object> props = (Map<String, Object>) result.get("properties");

                    // Extração segura do título
                    List<Map> titleList = (List<Map>) ((Map) props.get("Nome")).get("title");
                    String title = titleList.isEmpty() ? "Tarefa" : (String) titleList.get(0).get("plain_text");

                    // Extração da categoria
                    String category = "Geral";
                    Map<String, Object> select = (Map<String, Object>) ((Map) props.get("Categoria")).get("select");
                    if (select != null) {
                        category = (String) select.get("name");
                    }

                    tasks.add(new Task(title, targetTime, 0, dummyGoal, category));
                }
            }
            return tasks;
        } catch (Exception e) {
            System.err.println("Erro ao buscar tarefas por horário: " + e.getMessage());
            return List.of();
        }
    }

    public void updateStatus(String pageId, String newStatusName) {
        // A URL para atualizar uma página termina com o ID dela
        String url = "https://api.notion.com/v1/pages/" + pageId;

        // Estrutura: properties -> Status -> status -> name
        Map<String, Object> body = Map.of(
                "properties", Map.of(
                        "Status", Map.of(
                                "status", Map.of("name", newStatusName)
                        )
                )
        );

        try {
            // Notion exige PATCH para atualizações parciais
            restTemplate.patchForObject(url, new HttpEntity<>(body, createHeaders()), String.class);
            log.info("Sucesso! Página {} movida para o status: {}", pageId, newStatusName);
        } catch (Exception e) {
            log.error("Falha ao atualizar status no Notion para a página {}: {}", pageId, e.getMessage());
        }
    }

    public List<Task> getCompletedTasksLast7Days() {
        String url = "https://api.notion.com/v1/databases/" + databaseId + "/query";
        String sevenDaysAgo = LocalDate.now().minusDays(7).toString();

        // Filtro: Status == 'Concluído' AND Data >= 7 dias atrás
        Map<String, Object> body = Map.of("filter", Map.of("and", List.of(
                Map.of("property", "Status", "status", Map.of("equals", "Concluído")),
                Map.of("property", "Data", "date", Map.of("on_or_after", sevenDaysAgo))
        )));

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, createHeaders()), Map.class);
            List<Map> results = (List<Map>) response.getBody().get("results");
            List<Task> completedTasks = new ArrayList<>();

            if (results != null) {
                for (Map result : results) {
                    Map<String, Object> props = (Map<String, Object>) result.get("properties");

                    // Extração do Nome
                    List<Map> titleList = (List<Map>) ((Map) props.get("Nome")).get("title");
                    String title = titleList.isEmpty() ? "Tarefa" : (String) titleList.get(0).get("plain_text");

                    // Extração da Categoria
                    String category = "Geral";
                    Map<String, Object> select = (Map<String, Object>) ((Map) props.get("Categoria")).get("select");
                    if (select != null) category = (String) select.get("name");

                    completedTasks.add(new Task(title, LocalDateTime.now(), 60, null, category));
                }
            }
            return completedTasks;
        } catch (Exception e) {
            log.error("Erro ao buscar estatísticas: {}", e.getMessage());
            return List.of();
        }
    }
}