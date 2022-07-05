package dev.iot.telegrambot.telegramraspi;

import dev.iot.telegrambot.telegramraspi.service.CirclesAdapter;
import dev.iot.telegrambot.telegramraspi.service.Web3TransactionChecker;
import dev.iot.telegrambot.telegramraspi.storage.KeyValueService;
import dev.iot.telegrambot.telegramraspi.strategy.*;
import dev.iot.telegrambot.telegramraspi.web3.GnosisSafeOwnerCheck;
import dev.iot.telegrambot.telegramraspi.web3.SignatureService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@EnableScheduling
public class TelegramRaspiApplication implements CommandLineRunner {

    private final KeyValueService keyValueService;
    private final CirclesAdapter circlesAdapter;
    private final String telegramBotName;
    private final String telegramBotKey;
    private final String circlesSite;
    private final Web3TransactionChecker web3TransactionChecker;
    private final GnosisSafeOwnerCheck gnosisSafeOwnerCheck;
    private final SignatureService signatureService;

    public TelegramRaspiApplication(KeyValueService keyValueService, CirclesAdapter circlesAdapter, @Value("${telegramBotName}") String telegramBotName, @Value("${telegramBotKey}") String telegramBotKey, @Value("${circles.site}") String circlesSite, Web3TransactionChecker web3TransactionChecker, GnosisSafeOwnerCheck gnosisSafeOwnerCheck, SignatureService signatureService) {
        this.keyValueService = keyValueService;
        this.circlesAdapter = circlesAdapter;
        this.telegramBotName = telegramBotName;
        this.telegramBotKey = telegramBotKey;
        this.circlesSite = circlesSite;
        this.web3TransactionChecker = web3TransactionChecker;
        this.gnosisSafeOwnerCheck = gnosisSafeOwnerCheck;
        this.signatureService = signatureService;
    }

    public static void main(String[] args) {
        SpringApplication.run(TelegramRaspiApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            CirclesTransfer transferStrategy = new CirclesTransfer(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
            CirclesUserInfo userInfoStrategy = new CirclesUserInfo(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
            CirclesUserReader userReaderStrategy = new CirclesUserReader(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
            CreateAndSendMessage createSendeStrategy = new CreateAndSendMessage(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
            UserNameSetter userNameSetterStrategy = new UserNameSetter(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
            UserNameVerifier userNameVerifier = new UserNameVerifier(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
            UserNameVerifierRequest userNameVerifierRequest = new UserNameVerifierRequest(keyValueService, circlesAdapter, telegramBotName, telegramBotKey, circlesSite, web3TransactionChecker, gnosisSafeOwnerCheck, signatureService);
            AbstractStrategy[] commands = new AbstractStrategy[]{transferStrategy, userInfoStrategy, userReaderStrategy, createSendeStrategy, userNameSetterStrategy, userNameVerifierRequest, userNameVerifier};
            CirclesTelegramBot bot = new CirclesTelegramBot(telegramBotName, telegramBotKey, commands);
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
