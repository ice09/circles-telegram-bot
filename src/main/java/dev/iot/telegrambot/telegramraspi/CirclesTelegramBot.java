package dev.iot.telegrambot.telegramraspi;

import dev.iot.telegrambot.telegramraspi.service.BotSender;
import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.service.Web3TransactionChecker;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.CreateChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.ChatInviteLink;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Slf4j
public class CirclesTelegramBot extends TelegramLongPollingBot implements BotSender {

    private final KeyValueService keyValueService;
    private final CirclesAdapter circlesAdapter;
    private final String telegramBotName;
    private final String telegramBotKey;
    private final Web3TransactionChecker web3TransactionChecker;

    public CirclesTelegramBot(KeyValueService keyValueService, CirclesAdapter circlesAdapter, @Value("${telegramBotName}") String telegramBotName, @Value("${telegramBotKey}") String telegramBotKey, Web3TransactionChecker web3TransactionChecker) {
        this.keyValueService = keyValueService;
        this.circlesAdapter = circlesAdapter;
        this.telegramBotName = telegramBotName;
        this.telegramBotKey = telegramBotKey;
        this.web3TransactionChecker = web3TransactionChecker;
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
                            createAndSendMessage(chatId, "Hi *" + telegramName + "* you are not in Circles. Use *" + derivedHashedId + "* as Circles username on signup to help other users to find you with this bot.", "Markdown");
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
                            createAndSendMessage(chatId, "*" + circlesUser + "* is not signed up in Circles.", "Markdown");
                        }
                    }
                    break;
                case "transfer":
                    {
                        String toUser = args[1];
                        String fromUser = keyValueService.searchValue(derivedHashedId);
                        Optional<String> fromCirclesUser = circlesAdapter.verifyCirclesUserName(fromUser);
                        Optional<String> fromCirclesSafe = circlesAdapter.deriveSafeAddress(fromUser);
                        Optional<String> toCirclesUser = circlesAdapter.verifyCirclesUserName(toUser);
                        if (toCirclesUser.isEmpty()) {
                            createAndSendMessage(chatId, "Receiver *" + toUser + "* is not a valid Circles User.", "Markdown");
                        } else if (fromCirclesUser.isEmpty()) {
                            createAndSendMessage(chatId, "Sender *" + fromUser + "* is not a valid Circles User.", "Markdown");
                        } else {
                            BigDecimal amount = new BigDecimal(args[2]);
                            String inviteLink = Base64.getEncoder().encodeToString(getInviteLink(chatId).getBytes(StandardCharsets.UTF_8));
                            // "http://...?to,amount,invite,chatId";
                            String toCirclesSafe = circlesAdapter.deriveSafeAddress(toCirclesUser.get()).get();
                            web3TransactionChecker.trackAccount(chatId, fromCirclesSafe.get(), fromUser, toCirclesSafe, toUser, this);
                            createAndSendMessage(chatId, "Watching *" + fromCirclesUser.get() + "* for outgoing transfer to *" + toUser + "* about " + amount + " € for 10 Blocks.", "Markdown");
                            createAndSendMessage(chatId, "Click <a href='http://8c84-2003-ce-7f1d-ecb7-dd31-5709-bc5a-d96f.ngrok.io/prefill?to=" + toUser + "&amount=" + amount + "&chatId=" + chatId + "&inviteLink=" + inviteLink + "'>here</a> to execute transfer in Circles.", "HTML");
                        }
                    }
                    break;
            }
        }
    }

    private String getInviteLink(String chatId) {
        try {
            ChatInviteLink res = sendApiMethod(CreateChatInviteLink.builder().chatId(chatId).name("newnameyeah").build());
            return res.getInviteLink();
        } catch (Exception ex) {
            return "<ERROR>";
        }
    }

    public void createAndSendMessage(String chatId, String content, String parseMode) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(content);
        message.setParseMode(parseMode);
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