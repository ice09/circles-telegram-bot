package dev.iot.telegrambot.telegramraspi;

import dev.iot.telegrambot.telegramraspi.strategy.AbstractStrategy;
import dev.iot.telegrambot.telegramraspi.strategy.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.CreateChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatInviteLink;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Slf4j
public class CirclesTelegramBot extends TelegramLongPollingBot {

    private final String telegramBotName;
    private final String telegramBotKey;
    private AbstractStrategy[] commands;

    public CirclesTelegramBot(String telegramBotName, String telegramBotKey, AbstractStrategy[] commands) {
        this.telegramBotName = telegramBotName;
        this.telegramBotKey = telegramBotKey;
        this.commands = commands;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String telegramName = update.getMessage().getFrom().getFirstName();
            String telegramId = String.valueOf(update.getMessage().getFrom().getId());
            String chatId = update.getMessage().getChatId().toString();
            log.info("Received Message '" + update.getMessage().getText() + "'");
            String[] args = Pattern.compile("\"[^\"]*\"|[^ ]+")
                    .matcher(update.getMessage().getText())
                    .results()
                    .map(MatchResult::group)
                    .toArray(String[]::new);
            ExecutionContext executionContext = ExecutionContext.builder()
                    .chatId(chatId)
                    .telegramName(telegramName)
                    .telegramId(telegramId)
                    .commandArguments(args).build();
            String command = args[0].substring(1);
            try {
                execute(executionContext, command);
            } catch (IllegalArgumentException iae) {
                String[] cmdArgs = {"Error during processing: " + iae.getMessage(), "Markdown"};
                ExecutionContext error = ExecutionContext.builder()
                        .chatId(chatId)
                        .commandArguments(cmdArgs).build();
                execute(error, "sendMessage");
            }
        }
    }

    private void execute(ExecutionContext executionContext, String command) {
        for (AbstractStrategy strategy : commands) {
            if (strategy.isResponsibleFor(command, executionContext)) {
                strategy.executeCommand(this, executionContext);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return telegramBotName;
    }

    @Override
    public String getBotToken() {
        return telegramBotKey;
    }

    public ChatInviteLink sendApiMethodDelegate(CreateChatInviteLink method) throws TelegramApiException {
        return sendApiMethod(method);
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

}