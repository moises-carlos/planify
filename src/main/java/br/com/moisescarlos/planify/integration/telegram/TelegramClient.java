package br.com.moisescarlos.planify.integration.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class TelegramClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    public void sendMessage(String message) {
        sendWithButtons(message, null);
    }

    public void sendWithButtons(String message, List<List<Map<String, String>>> buttons) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", message);
        body.put("parse_mode", "HTML");

        if (buttons != null) {
            body.put("reply_markup", Map.of("inline_keyboard", buttons));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }
}