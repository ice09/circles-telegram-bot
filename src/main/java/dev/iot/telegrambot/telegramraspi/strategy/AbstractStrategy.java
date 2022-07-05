package dev.iot.telegrambot.telegramraspi.strategy;

import dev.iot.telegrambot.telegramraspi.CirclesTelegramBot;
import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.service.Web3TransactionChecker;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueEntity;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import dev.iot.telegrambot.telegramraspi.web3.GnosisSafeOwnerCheck;
import dev.iot.telegrambot.telegramraspi.web3.SignatureService;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.groupadministration.CreateChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.ChatInviteLink;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.web3j.crypto.Hash;

import java.nio.charset.StandardCharsets;

@Slf4j
public abstract class AbstractStrategy {

    protected final KeyValueService keyValueService;
    protected final CirclesAdapter circlesAdapter;
    protected final String telegramBotName;
    protected final String telegramBotKey;
    protected final String circlesSite;
    protected final Web3TransactionChecker web3TransactionChecker;
    protected final GnosisSafeOwnerCheck gnosisSafeOwnerCheck;
    protected final SignatureService signatureService;

    public AbstractStrategy(KeyValueService keyValueService, CirclesAdapter circlesAdapter, String telegramBotName, String telegramBotKey, String circlesSite, Web3TransactionChecker web3TransactionChecker, GnosisSafeOwnerCheck gnosisSafeOwnerCheck, SignatureService signatureService) {
        this.keyValueService = keyValueService;
        this.circlesAdapter = circlesAdapter;
        this.telegramBotName = telegramBotName;
        this.telegramBotKey = telegramBotKey;
        this.circlesSite = circlesSite;
        this.web3TransactionChecker = web3TransactionChecker;
        this.gnosisSafeOwnerCheck = gnosisSafeOwnerCheck;
        this.signatureService = signatureService;
    }

    public abstract void executeCommand(CirclesTelegramBot bot, ExecutionContext context);

    public abstract boolean isResponsibleFor(String command, ExecutionContext executionContext);

    protected void updateCirclesUsername(String value, String signature) {
        keyValueService.updateValueExt(value, signature);
    }

    protected boolean verifyUserName(String circlesUser, String safeAddr, String content, String signature) {
        byte[] proof = Hash.sha3(signatureService.createProof(content.getBytes(StandardCharsets.UTF_8)));
        for (String owner : gnosisSafeOwnerCheck.loadGnosisSafeOwner(safeAddr)) {
            if (owner.substring(2).equalsIgnoreCase(signatureService.ecrecoverAddress(proof, Hex.decode(signature.substring(2)), owner))) {
                updateCirclesUsername(circlesUser, signature);
                log.info("Signature is valid for address " + safeAddr + ", '" + content + "' and signature " + signature);
                return true;
            }
        }
        log.error("Signature is not valid for address " + safeAddr + ", '" + content + "' and signature " + signature);
        return false;
    }

    protected void loadCompleteDataset(CirclesTelegramBot bot, String chatId, String circlesUser, String content) {
        content = enrichSignatureValidationInfo(circlesUser, content);
        SendMessage sendMessage = SendMessage.builder().parseMode("Markdown").chatId(chatId).text(content).build();
        sendMsg(bot, sendMessage);
    }

    protected String enrichSignatureValidationInfo(String circlesUser, String content) {
        KeyValueEntity entity = keyValueService.loadEntityByValue(circlesUser);
        if ((entity != null) && StringUtils.hasText(entity.getExt())) {
            content += "\n_Note: This address has been validated by successful signature verification._";
        }
        return content;
    }

    protected String getInviteLink(CirclesTelegramBot bot, String chatId) {
        try {
            ChatInviteLink res = bot.sendApiMethodDelegate(CreateChatInviteLink.builder().chatId(chatId).name("newnameyeah").build());
            return res.getInviteLink();
        } catch (Exception ex) {
            log.error("Invite Link Creation: " + ex.getMessage());
            return "<ERROR>";
        }
    }

    public void createAndSendMessage(CirclesTelegramBot bot, String chatId, String content, String parseMode) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(content);
        message.setParseMode(parseMode);
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Cannot send message.", e);
        }
    }

    protected void sendPhoto(CirclesTelegramBot bot, SendPhoto sendPhoto) {
        try {
            bot.execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("Cannot send photo.", e);
        }
    }

    protected void sendMsg(CirclesTelegramBot bot, SendMessage sendMsg) {
        try {
            bot.execute(sendMsg);
        } catch (TelegramApiException e) {
            log.error("Cannot send message.", e);
        }
    }

    protected String deriveUniqueHashedId(String telegramName, String telegramId) {
        Keccak.Digest256 digest256 = new Keccak.Digest256();
        byte[] hashbytes = digest256.digest(telegramId.getBytes(StandardCharsets.UTF_8));
        String sha3Hex = new String(Hex.encode(hashbytes));
        return telegramName + "_" + sha3Hex.substring(0, 8);
    }

}
