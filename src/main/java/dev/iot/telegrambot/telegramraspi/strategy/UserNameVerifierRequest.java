package dev.iot.telegrambot.telegramraspi.strategy;

import dev.iot.telegrambot.telegramraspi.CirclesTelegramBot;
import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.service.Web3TransactionChecker;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import dev.iot.telegrambot.telegramraspi.web3.GnosisSafeOwnerCheck;
import dev.iot.telegrambot.telegramraspi.web3.SignatureService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserNameVerifierRequest extends AbstractStrategy {

    public UserNameVerifierRequest(KeyValueService keyValueService, CirclesAdapter circlesAdapter, String telegramBotName, String telegramBotKey, String circlesSite, Web3TransactionChecker web3TransactionChecker, GnosisSafeOwnerCheck gnosisSafeOwnerCheck, SignatureService signatureService) {
        super(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
    }

    private void verifyCirclesUser(CirclesTelegramBot bot, String telegramName, String chatId) {
        String telegramNameUpper = telegramName.toUpperCase();
        String chatIdUpper = deriveUniqueHashedId("CRC", chatId).toUpperCase();
        String content = "I AM " + telegramNameUpper + " FROM CHAT " + chatIdUpper;
        createAndSendMessage(bot, chatId, "Please sign this message:\n*" + content + "*", "Markdown");
    }

    @Override
    public void executeCommand(CirclesTelegramBot bot, ExecutionContext context) {
        verifyCirclesUser(bot, context.getTelegramName(), context.getChatId());
    }

    @Override
    public boolean isResponsibleFor(String command, ExecutionContext executionContext) {
        return "verify".equals(command) && executionContext.getCommandArguments().length == 1;
    }
}
