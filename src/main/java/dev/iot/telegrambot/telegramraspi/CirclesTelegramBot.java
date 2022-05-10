package dev.iot.telegrambot.telegramraspi;

import dev.iot.telegrambot.telegramraspi.service.BotSender;
import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.service.Web3TransactionChecker;
import dev.iot.telegrambot.telegramraspi.service.dto.profile.Search;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueEntity;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import dev.iot.telegrambot.telegramraspi.web3.GnosisSafeOwnerCheck;
import dev.iot.telegrambot.telegramraspi.web3.SignatureService;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.CreateChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.ChatInviteLink;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.web3j.crypto.Hash;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Slf4j
public class CirclesTelegramBot extends TelegramLongPollingBot implements BotSender {

    private final KeyValueService keyValueService;
    private final CirclesAdapter circlesAdapter;
    private final String telegramBotName;
    private final String telegramBotKey;
    private final String circlesSite;
    private final Web3TransactionChecker web3TransactionChecker;
    private final GnosisSafeOwnerCheck gnosisSafeOwnerCheck;
    private final SignatureService signatureService;

    public CirclesTelegramBot(KeyValueService keyValueService, CirclesAdapter circlesAdapter, String telegramBotName, String telegramBotKey, String circlesSite, Web3TransactionChecker web3TransactionChecker, GnosisSafeOwnerCheck gnosisSafeOwnerCheck, SignatureService signatureService) {
        this.keyValueService = keyValueService;
        this.circlesAdapter = circlesAdapter;
        this.telegramBotName = telegramBotName;
        this.telegramBotKey = telegramBotKey;
        this.circlesSite = circlesSite;
        this.web3TransactionChecker = web3TransactionChecker;
        this.gnosisSafeOwnerCheck = gnosisSafeOwnerCheck;
        this.signatureService = signatureService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String telegramName = update.getMessage().getFrom().getFirstName();
            String telegramId = String.valueOf(update.getMessage().getFrom().getId());
            String chatId = update.getMessage().getChatId().toString();
            String derivedHashedId = deriveUniqueHashedId(telegramName, telegramId);
            log.info("Received Message '" + update.getMessage().getText() + "'");
            String[] args = Pattern.compile("\"[^\"]*\"|[^ ]+")
                    .matcher(update.getMessage().getText())
                    .results()
                    .map(MatchResult::group)
                    .toArray(String[]::new);
            String command = args[0];
            try {
                switch (command.substring(1)) {
                    case "verify":
                        verifyCirclesUser(telegramName, chatId, derivedHashedId, args);
                        break;
                    case "set":
                        setCirclesUsername(derivedHashedId, args[1]);
                        // break; should fall through!
                    case "info":
                        readCirclesUserInfo(telegramName, chatId, derivedHashedId);
                        break;
                    case "query":
                        queryCirclesUser(chatId, args);
                        break;
                    case "transfer":
                        initiateCirclesTransfer(chatId, derivedHashedId, args);
                        break;
                }
            } catch (IllegalArgumentException iae) {
                createAndSendMessage(chatId, "Error during processing: " + iae.getMessage(), "Markdown");
            }
        }
    }

    private void initiateCirclesTransfer(String chatId, String derivedHashedId, String[] args) {
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
            String inviteLink = URLEncoder.encode(getInviteLink(chatId), StandardCharsets.UTF_8);
            String toCirclesSafe = circlesAdapter.deriveSafeAddress(toCirclesUser.get()).get();
            web3TransactionChecker.trackAccount(chatId, fromCirclesSafe.get(), fromUser, toCirclesSafe, toUser, this);
            createAndSendMessage(chatId, "Watching *" + fromCirclesUser.get() + "* for outgoing transfer to *" + toUser + "* about " + amount + " € for 10 Blocks.", "Markdown");
            createAndSendMessage(chatId, "Click <a href='" + circlesSite + "/#/banking/send/" + amount + "/" + toCirclesSafe + "/" + inviteLink + "'>here</a> to execute transfer in Circles.", "HTML");
        }
    }

    private void queryCirclesUser(String chatId, String[] args) {
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
            sendPhoto(sendPhoto);
        } else {
            Optional<String> verifiedName = circlesAdapter.verifyCirclesUserName(circlesUser);
            if (verifiedName.isEmpty()) {
                createAndSendMessage(chatId, "*" + circlesUser + "* is not signed up in Circles.", "Markdown");
            } else {
                String safe = circlesAdapter.deriveSafeAddress(verifiedName.get()).get();
                String content = "Circles name is *" + verifiedName.get() + "* with Gnosis Safe address *" + safe + "*";
                loadCompleteDataset(chatId, circlesUser, content);
            }
        }
    }

    private void readCirclesUserInfo(String telegramName, String chatId, String derivedHashedId) {
        String circlesUser = keyValueService.searchValue(derivedHashedId);
        Optional<SendPhoto> found = circlesAdapter.loadCirclesUserAvatar(chatId, circlesUser);
        if (found.isEmpty()) {
            Optional<String> verifiedName = circlesAdapter.verifyCirclesUserName(circlesUser);
            if (verifiedName.isEmpty()) {
                createAndSendMessage(chatId, "Hi *" + telegramName + "* you are not in Circles. Use *" + derivedHashedId + "* as Circles username on signup to help other users to find you with this bot.", "Markdown");
            } else {
                String content = "Hi *" + telegramName + "*, your Circles name is *" + circlesUser + "*";
                if (StringUtils.hasText(derivedHashedId) &&  !circlesUser.equals(derivedHashedId)) {
                    content += "\n_Note: This address was set manually, it is not derived from the Telegram ID._";
                }
                loadCompleteDataset(chatId, circlesUser, content);
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
            sendPhoto(sendPhoto);
        }
    }

    private void verifyCirclesUser(String telegramName, String chatId, String derivedHashedId, String[] args) {
        String telegramNameUpper = telegramName.toUpperCase();
        String chatIdUpper = deriveUniqueHashedId("CRC", chatId).toUpperCase();
        String content = "I AM " + telegramNameUpper + " FROM CHAT " + chatIdUpper;
        if (args.length == 1) {
            createAndSendMessage(chatId, "Please sign this message:\n*" + content + "*", "Markdown");
        } else {
            String circlesUser = keyValueService.searchValue(derivedHashedId);
            Optional<String> verifiedName = circlesAdapter.verifyCirclesUserName(circlesUser);
            if (verifiedName.isEmpty()) {
                createAndSendMessage(chatId, "Hi *" + telegramName + "* you are not in Circles. Use *" + derivedHashedId + "* as Circles username on signup to help other users to find you with this bot.", "Markdown");
            } else {
                Optional<String> safeAddr = circlesAdapter.deriveSafeAddress(circlesUser);
                if (!safeAddr.isEmpty()) {
                    boolean verifiedUserName = verifyUserName(circlesUser, safeAddr.get(), content, args[1]);
                    if (verifiedUserName) {
                        createAndSendMessage(chatId, "*" + telegramName + "* was successfully validated against Circles User *" + circlesUser + "*", "Markdown");
                    } else {
                        createAndSendMessage(chatId, "Error during verification of Signature.", "Markdown");
                    }
                } else {
                    log.error("Cannot derive safe address for " + circlesUser);
                }
            }
        }
    }

    private boolean verifyUserName(String circlesUser, String safeAddr, String content, String signature) {
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

    private void loadCompleteDataset(String chatId, String circlesUser, String content) {
        content = enrichSignatureValidationInfo(circlesUser, content);
        SendMessage sendMessage = SendMessage.builder().parseMode("Markdown").chatId(chatId).text(content).build();
        sendMsg(sendMessage);
    }

    private String enrichSignatureValidationInfo(String circlesUser, String content) {
        KeyValueEntity entity = keyValueService.loadEntityByValue(circlesUser);
        if ((entity != null) && StringUtils.hasText(entity.getExt())) {
            content += "\n_Note: This address has been validated by successful signature verification._";
        }
        return content;
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

    private void sendMsg(SendMessage sendMsg) {
        try {
            execute(sendMsg);
        } catch (TelegramApiException e) {
            log.error("Cannot send message.", e);
        }
    }

    private String deriveUniqueHashedId(String telegramName, String telegramId) {
        Keccak.Digest256 digest256 = new Keccak.Digest256();
        byte[] hashbytes = digest256.digest(telegramId.getBytes(StandardCharsets.UTF_8));
        String sha3Hex = new String(Hex.encode(hashbytes));
        return telegramName + "_" + sha3Hex.substring(0, 8);
    }

    public void setCirclesUsername(String key, String value) {
        keyValueService.setKeyValue(key, value, null);
    }

    public void updateCirclesUsername(String value, String signature) {
        keyValueService.updateValueExt(value, signature);
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