package br.com.moisescarlos.planify.integration.telegram;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
@Configuration
public class BotConfig {

    // Esse código força o Spring a conectar o seu bot nos servidores do Telegram assim que a aplicação liga
    @Bean
    public TelegramBotsApi telegramBotsApi(PlanifyBotListener planifyBotListener) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(planifyBotListener);
        return api;
    }
}