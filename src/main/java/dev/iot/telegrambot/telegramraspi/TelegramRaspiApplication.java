package dev.iot.telegrambot.telegramraspi;

import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class TelegramRaspiApplication implements CommandLineRunner {

    private final KeyValueService keyValueService;
    private final CirclesAdapter circlesAdapter;
    private final String telegramBotName;
    private final String telegramBotKey;

    public TelegramRaspiApplication(KeyValueService keyValueService, CirclesAdapter circlesAdapter, @Value("${telegramBotName}") String telegramBotName, @Value("${telegramBotKey}") String telegramBotKey) {
        this.keyValueService = keyValueService;
        this.circlesAdapter = circlesAdapter;
        this.telegramBotName = telegramBotName;
        this.telegramBotKey = telegramBotKey;
    }

    public static void main(String[] args) {
        SpringApplication.run(TelegramRaspiApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new CirclesTelegramBot(keyValueService, circlesAdapter, telegramBotName, telegramBotKey));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
