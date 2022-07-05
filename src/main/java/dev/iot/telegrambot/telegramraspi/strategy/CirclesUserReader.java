package dev.iot.telegrambot.telegramraspi.strategy;

import dev.iot.telegrambot.telegramraspi.CirclesTelegramBot;
import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.service.Web3TransactionChecker;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import dev.iot.telegrambot.telegramraspi.web3.GnosisSafeOwnerCheck;
import dev.iot.telegrambot.telegramraspi.web3.SignatureService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import java.util.Optional;

@Slf4j
public class CirclesUserReader extends AbstractStrategy {


    public CirclesUserReader(KeyValueService keyValueService, CirclesAdapter circlesAdapter, String telegramBotName, String telegramBotKey, String circlesSite, Web3TransactionChecker web3TransactionChecker, GnosisSafeOwnerCheck gnosisSafeOwnerCheck, SignatureService signatureService) {
        super(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
    }

    private void queryCirclesUser(CirclesTelegramBot bot, String chatId, String[] args) {
        String circlesUser = args[1];
        Optional<SendPhoto> found = circlesAdapter.loadCirclesUserAvatar(chatId, circlesUser);
        if (found.isPresent()) {
            SendPhoto sendPhoto = found.get();
            try {
                circlesAdapter.addCaptionForQuery(sendPhoto, circlesUser);
                sendPhoto.setCaption(enrichSignatureValidationInfo(circlesUser, sendPhoto.getCaption()));
            } catch (Exception ex) {
                log.error(ex.getMessage());
                sendPhoto.setCaption("An error occurred while processing Circles data.");
            }
            sendPhoto(bot, sendPhoto);
        } else {
            Optional<String> verifiedName = circlesAdapter.verifyCirclesUserName(circlesUser);
            if (verifiedName.isEmpty()) {
                createAndSendMessage(bot, chatId, "*" + circlesUser + "* is not signed up in Circles.", "Markdown");
            } else {
                String safe = circlesAdapter.deriveSafeAddress(verifiedName.get()).get();
                String content = "Circles name is *" + verifiedName.get() + "* with Gnosis Safe address *" + safe + "*";
                loadCompleteDataset(bot, chatId, circlesUser, content);
            }
        }
    }

    @Override
    public void executeCommand(CirclesTelegramBot bot, ExecutionContext context) {
        queryCirclesUser(bot, context.getChatId(), context.getCommandArguments());
    }

    @Override
    public boolean isResponsibleFor(String command, ExecutionContext executionContext) {
        return "query".equals(command);
    }
}
