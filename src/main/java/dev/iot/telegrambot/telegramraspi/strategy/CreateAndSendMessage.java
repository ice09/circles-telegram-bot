package dev.iot.telegrambot.telegramraspi.strategy;

import dev.iot.telegrambot.telegramraspi.CirclesTelegramBot;
import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.service.Web3TransactionChecker;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import dev.iot.telegrambot.telegramraspi.web3.GnosisSafeOwnerCheck;
import dev.iot.telegrambot.telegramraspi.web3.SignatureService;

public class CreateAndSendMessage extends AbstractStrategy {

    public CreateAndSendMessage(KeyValueService keyValueService, CirclesAdapter circlesAdapter, String telegramBotName, String telegramBotKey, String circlesSite, Web3TransactionChecker web3TransactionChecker, GnosisSafeOwnerCheck gnosisSafeOwnerCheck, SignatureService signatureService) {
        super(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
    }

    @Override
    public void executeCommand(CirclesTelegramBot bot, ExecutionContext context) {
        createAndSendMessage(bot, context.getChatId(), context.getCommandArguments()[0], context.getCommandArguments()[1]);
    }

    @Override
    public boolean isResponsibleFor(String command, ExecutionContext executionContext) {
        return command.equals("sendMessage");
    }
}
