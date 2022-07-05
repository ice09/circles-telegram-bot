package dev.iot.telegrambot.telegramraspi.strategy;

import dev.iot.telegrambot.telegramraspi.CirclesTelegramBot;
import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.service.Web3TransactionChecker;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import dev.iot.telegrambot.telegramraspi.web3.GnosisSafeOwnerCheck;
import dev.iot.telegrambot.telegramraspi.web3.SignatureService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserNameSetter extends AbstractStrategy {

    public UserNameSetter(KeyValueService keyValueService, CirclesAdapter circlesAdapter, String telegramBotName, String telegramBotKey, String circlesSite, Web3TransactionChecker web3TransactionChecker, GnosisSafeOwnerCheck gnosisSafeOwnerCheck, SignatureService signatureService) {
        super(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
    }

    public void setCirclesUsername(String key, String value) {
        keyValueService.setKeyValue(key, value, null);
    }

    @Override
    public void executeCommand(CirclesTelegramBot bot, ExecutionContext context) {
        setCirclesUsername(deriveUniqueHashedId(context.getTelegramName(), context.getTelegramId()), context.getCommandArguments()[1]);
        createAndSendMessage(bot, context.getChatId(), "User *" + context.getTelegramName() + "* set to *" + context.getCommandArguments()[1] + "*.", "Markdown");
    }

    @Override
    public boolean isResponsibleFor(String command, ExecutionContext executionContext) {
        return "set".equals(command);
    }
}
