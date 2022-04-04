package dev.iot.telegrambot.telegramraspi;

import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
public class CirclesTelegramBot extends TelegramLongPollingBot {

    private final KeyValueService keyValueService;
    private final CirclesAdapter circlesAdapter;
    private final String telegramBotName;
    private final String telegramBotKey;


    public CirclesTelegramBot(KeyValueService keyValueService, CirclesAdapter circlesAdapter, @Value("${telegramBotName}") String telegramBotName, @Value("${telegramBotKey}") String telegramBotKey) {
        this.keyValueService = keyValueService;
        this.circlesAdapter = circlesAdapter;
        this.telegramBotName = telegramBotName;
        this.telegramBotKey = telegramBotKey;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String telegramName = update.getMessage().getFrom().getFirstName();
            String telegramId = String.valueOf(update.getMessage().getFrom().getId());
            String chatId = update.getMessage().getChatId().toString();
            String derivedHashedId = deriveUniqueHashedId(telegramName, telegramId);
            log.info("Received Message '" + update.getMessage().getText() + "'");
            String[] args = update.getMessage().getText().split(" ");
            String command = args[0];
            switch (command.substring(1)) {
                case "set":
                    {
                        setCirclesUsername(derivedHashedId, args[1]);
                    }
                    // break; should fall through!
                case "info":
                    {
                        String circlesUser = keyValueService.searchValue(derivedHashedId);
                        Optional<SendPhoto> found = circlesAdapter.loadCirclesUserAvatar(chatId, circlesUser);
                        if (found.isEmpty()) {
                            createAndSendMessage(chatId, "Hi *" + telegramName + "* you are not in Circles. Use *" + derivedHashedId + "* as Circles username on signup to help other users to find you with this bot.");
                        } else {
                            SendPhoto sendPhoto = found.get();
                            circlesAdapter.addCaptionForInfo(sendPhoto, circlesUser, telegramName, derivedHashedId);
                            sendPhoto(sendPhoto);
                        }
                    }
                    break;
                case "query":
                    {
                        String circlesUser = args[1];
                        Optional<SendPhoto> found = circlesAdapter.loadCirclesUserAvatar(chatId, circlesUser);
                        if (found.isPresent()) {
                            SendPhoto sendPhoto = found.get();
                            circlesAdapter.addCaptionForQuery(sendPhoto, circlesUser);
                            sendPhoto(sendPhoto);
                        } else {
                            createAndSendMessage(chatId, "*" + circlesUser + "* is not signed up in Circles.");
                        }
                    }
                    break;
            }
        }
    }

    private void createAndSendMessage(String chatId, String content) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(content);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Cannot send message.", e);
        }
    }

    private void sendPhoto(SendPhoto sendPhoto) {
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("Cannot send photo.", e);
        }
    }

    private String deriveUniqueHashedId(String telegramName, String telegramId) {
        Keccak.Digest256 digest256 = new Keccak.Digest256();
        byte[] hashbytes = digest256.digest(telegramId.getBytes(StandardCharsets.UTF_8));
        String sha3Hex = new String(Hex.encode(hashbytes));
        return telegramName + "_" + sha3Hex.substring(0, 8);
    }

    public void setCirclesUsername(String key, String value) {
        keyValueService.setKeyValue(key, value);
    }

    @Override
    public String getBotUsername() {
        return telegramBotName;
    }

    @Override
    public String getBotToken() {
        return telegramBotKey;
    }
}