package dev.iot.telegrambot.telegramraspi.strategy;

import dev.iot.telegrambot.telegramraspi.CirclesTelegramBot;
import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.service.Web3TransactionChecker;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import dev.iot.telegrambot.telegramraspi.web3.GnosisSafeOwnerCheck;
import dev.iot.telegrambot.telegramraspi.web3.SignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import java.util.Optional;

@Slf4j
public class CirclesUserInfo extends AbstractStrategy {

    public CirclesUserInfo(KeyValueService keyValueService, CirclesAdapter circlesAdapter, String telegramBotName, String telegramBotKey, String circlesSite, Web3TransactionChecker web3TransactionChecker, GnosisSafeOwnerCheck gnosisSafeOwnerCheck, SignatureService signatureService) {
        super(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
    }

    private void readCirclesUserInfo(CirclesTelegramBot bot, String telegramName, String chatId, String derivedHashedId) {
        String circlesUser = keyValueService.searchValue(derivedHashedId);
        Optional<SendPhoto> found = circlesAdapter.loadCirclesUserAvatar(chatId, circlesUser);
        if (found.isEmpty()) {
            Optional<String> verifiedName = circlesAdapter.verifyCirclesUserName(circlesUser);
            if (verifiedName.isEmpty()) {
                createAndSendMessage(bot, chatId, "Hi *" + telegramName + "* you are not in Circles. Use *" + derivedHashedId + "* as Circles username on signup to help other users to find you with this bot.", "Markdown");
            } else {
                String content = "Hi *" + telegramName + "*, your Circles name is *" + circlesUser + "*";
                if (StringUtils.hasText(derivedHashedId) &&  !circlesUser.equals(derivedHashedId)) {
                    content += "\n_Note: This address was set manually, it is not derived from the Telegram ID._";
                }
                loadCompleteDataset(bot, chatId, circlesUser, content);
            }
        } else {
            SendPhoto sendPhoto = found.get();
            try {
                circlesAdapter.addCaptionForInfo(sendPhoto, circlesUser, telegramName, derivedHashedId);
                sendPhoto.setCaption(enrichSignatureValidationInfo(circlesUser, sendPhoto.getCaption()));
            } catch (Exception ex) {
                log.error(ex.getMessage());
                sendPhoto.setCaption("An error occurred while processing Circles data.");
            }
            sendPhoto(bot, sendPhoto);
        }
    }

    @Override
    public void executeCommand(CirclesTelegramBot bot, ExecutionContext context) {
        readCirclesUserInfo(bot, context.getTelegramName(), context.getChatId(), deriveUniqueHashedId(context.getTelegramName(), context.getTelegramId()));
    }

    @Override
    public boolean isResponsibleFor(String command, ExecutionContext executionContext) {
        return "info".equals(command);
    }
}
