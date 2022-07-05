package dev.iot.telegrambot.telegramraspi.strategy;

import dev.iot.telegrambot.telegramraspi.CirclesTelegramBot;
import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.service.Web3TransactionChecker;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import dev.iot.telegrambot.telegramraspi.web3.GnosisSafeOwnerCheck;
import dev.iot.telegrambot.telegramraspi.web3.SignatureService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class UserNameVerifier extends AbstractStrategy {

    public UserNameVerifier(KeyValueService keyValueService, CirclesAdapter circlesAdapter, String telegramBotName, String telegramBotKey, String circlesSite, Web3TransactionChecker web3TransactionChecker, GnosisSafeOwnerCheck gnosisSafeOwnerCheck, SignatureService signatureService) {
        super(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
    }

    private void verifyCirclesUser(CirclesTelegramBot bot, String telegramName, String chatId, String derivedHashedId, String signature) {
        String telegramNameUpper = telegramName.toUpperCase();
        String chatIdUpper = deriveUniqueHashedId("CRC", chatId).toUpperCase();
        String content = "I AM " + telegramNameUpper + " FROM CHAT " + chatIdUpper;
        String circlesUser = keyValueService.searchValue(derivedHashedId);
        Optional<String> verifiedName = circlesAdapter.verifyCirclesUserName(circlesUser);
        if (verifiedName.isEmpty()) {
            createAndSendMessage(bot, chatId, "Hi *" + telegramName + "* you are not in Circles. Use *" + derivedHashedId + "* as Circles username on signup to help other users to find you with this bot.", "Markdown");
        } else {
            Optional<String> safeAddr = circlesAdapter.deriveSafeAddress(circlesUser);
            if (safeAddr.isPresent()) {
                boolean verifiedUserName = verifyUserName(circlesUser, safeAddr.get(), content, signature);
                if (verifiedUserName) {
                    createAndSendMessage(bot, chatId, "*" + telegramName + "* was successfully validated against Circles User *" + circlesUser + "*", "Markdown");
                } else {
                    createAndSendMessage(bot, chatId, "Error during verification of Signature.", "Markdown");
                }
            } else {
                log.error("Cannot derive safe address for " + circlesUser);
            }
        }
    }

    @Override
    public void executeCommand(CirclesTelegramBot bot, ExecutionContext context) {
        verifyCirclesUser(bot, context.getTelegramName(), context.getChatId(), deriveUniqueHashedId(context.getTelegramName(), context.getTelegramId()), context.getCommandArguments()[1]);
    }

    @Override
    public boolean isResponsibleFor(String command, ExecutionContext executionContext) {
        return "verify".equals(command) && executionContext.getCommandArguments().length == 2;
    }
}
