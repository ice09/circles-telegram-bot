package dev.iot.telegrambot.telegramraspi.strategy;

import dev.iot.telegrambot.telegramraspi.CirclesTelegramBot;
import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.service.Web3TransactionChecker;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import dev.iot.telegrambot.telegramraspi.web3.GnosisSafeOwnerCheck;
import dev.iot.telegrambot.telegramraspi.web3.SignatureService;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class CirclesTransfer extends AbstractStrategy {

    public CirclesTransfer(KeyValueService keyValueService, CirclesAdapter circlesAdapter, String telegramBotName, String telegramBotKey, String circlesSite, Web3TransactionChecker web3TransactionChecker, GnosisSafeOwnerCheck gnosisSafeOwnerCheck, SignatureService signatureService) {
        super(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
    }

    private void initiateCirclesTransfer(CirclesTelegramBot bot, String chatId, String derivedHashedId, String[] args) {
        String toUser = args[1];
        String fromUser = keyValueService.searchValue(derivedHashedId);
        Optional<String> fromCirclesUser = circlesAdapter.verifyCirclesUserName(fromUser);
        Optional<String> fromCirclesSafe = circlesAdapter.deriveSafeAddress(fromUser);
        Optional<String> toCirclesUser = circlesAdapter.verifyCirclesUserName(toUser);
        if (toCirclesUser.isEmpty()) {
            createAndSendMessage(bot, chatId, "Receiver *" + toUser + "* is not a valid Circles User.", "Markdown");
        } else if (fromCirclesUser.isEmpty()) {
            createAndSendMessage(bot, chatId, "Sender *" + fromUser + "* is not a valid Circles User.", "Markdown");
        } else {
            BigDecimal amount = new BigDecimal(args[2]);
            String inviteLink = URLEncoder.encode(getInviteLink(bot, chatId), StandardCharsets.UTF_8);
            String toCirclesSafe = circlesAdapter.deriveSafeAddress(toCirclesUser.get()).get();
            web3TransactionChecker.trackAccount(bot, chatId, fromCirclesSafe.get(), fromUser, toCirclesSafe, toUser);
            createAndSendMessage(bot, chatId, "Watching *" + fromCirclesUser.get() + "* for outgoing transfer to *" + toUser + "* about " + amount + " € for 10 Blocks.", "Markdown");
            createAndSendMessage(bot, chatId, "Click <a href='" + circlesSite + "/#/banking/send/" + amount + "/" + toCirclesSafe + "/" + inviteLink + "'>here</a> to execute transfer in Circles.", "HTML");
        }
    }

    @Override
    public void executeCommand(CirclesTelegramBot bot, ExecutionContext context) {
        initiateCirclesTransfer(bot, context.getChatId(), deriveUniqueHashedId(context.getTelegramName(), context.getTelegramId()), context.getCommandArguments());

    }

    @Override
    public boolean isResponsibleFor(String command, ExecutionContext executionContext) {
        return "transfer".equals(command);
    }

}
