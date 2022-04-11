package dev.iot.telegrambot.telegramraspi.service;

import dev.iot.telegrambot.telegramraspi.service.dto.CachedTransactionDto;
import dev.iot.telegrambot.telegramraspi.service.dto.Data;
import dev.iot.telegrambot.telegramraspi.service.dto.Event;
import dev.iot.telegrambot.telegramraspi.service.dto.ResponseDto;
import dev.iot.telegrambot.telegramraspi.web3.Web3Reader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class Web3TransactionChecker {

    //http://localhost:8080/prefill?from=0x249fA3ecD95a53F742707D53688FCafbBd072f33&to=0x4a9aFfA9249F36fd0629f342c182A4e94A13C2e0&amount=100&chatId=1&inviteLink=11
    //http://localhost:8080/prefill?from=0x249fA3ecD95a53F742707D53688FCafbBd072f33&to=0xde374ece6fa50e781e81aac78e811b33d16912c7&amount=100&chatId=1&inviteLink=11
    private Map<String, CachedTransactionDto> watchedTransations;
    private final Web3Reader web3Reader;

    private final GraphQLClient graphQLClient;
    private BotSender botSender;

    public Web3TransactionChecker(Web3Reader web3Reader, GraphQLClient graphQLClient) {
        this.web3Reader = web3Reader;
        this.graphQLClient = graphQLClient;
        this.watchedTransations = new HashMap<>();
    }

    @Scheduled(fixedDelay = 2000)
    public void checkTransactions() throws URISyntaxException, IOException {
        for (Map.Entry<String, CachedTransactionDto> entry : watchedTransations.entrySet()) {
            ResponseDto cTrx = graphQLClient.trackAccount(entry.getKey());
            CachedTransactionDto cTrxDto = watchedTransations.get(entry.getKey());
            log.info("Tracking: " + entry.getKey() + " with startBlock " + cTrxDto.getStartBlock());
            Data trxData = cTrx.getData();
            boolean remove = false;
            for (Event evt : trxData.getEvents()) {
                if (evt.getBlock_number().compareTo(cTrxDto.getStartBlock()) > 0) {
                    if (evt.getPayload().getTo().equalsIgnoreCase(cTrxDto.getTo())) {
                        notify(entry.getValue().getChatId(), evt.getPayload().getFrom() + " to " + evt.getPayload().getTo() + " with amount " + evt.getPayload().getFlow() + " at block " + evt.getBlock_number());
                        remove = true;
                    }
                } else {
                    log.info("Found only older event before startBlock " + cTrxDto.getStartBlock());
                    break;
                }
            }
            //TODO: what about multiple transfers (in one block?)
            boolean watchedMoreThan10Blocks = web3Reader.getCurrentBlock().subtract(cTrxDto.getStartBlock()).intValue() > 10;
            if (remove || watchedMoreThan10Blocks) {
                if (watchedMoreThan10Blocks) {
                    log.error("Aborting tracking " + entry.getKey() + ". No answer for the last 10 blocks.");
                    notify(entry.getValue().getChatId(), "Stopped watching *" + entry.getValue().getFrom() + "* (" + entry.getKey() + "), no transfer for 10 blocks.");
                } else {
                    log.error("Aborting tracking " + entry.getKey() + ". Found recent transfer.");
                }
                watchedTransations.remove(entry.getKey());
            }
        }
    }

    private void notify(String chatId, String message) {
        botSender.createAndSendMessage(chatId, message, "Markdown");
    }


    public void trackAccount(String chatId, String safe, String fromUser, String to, BotSender botSender) {
        BigInteger currentBlock = web3Reader.getCurrentBlock();
        //TODO: Refactor it! Why everytime again?
        this.botSender = botSender;
        watchedTransations.put(safe, CachedTransactionDto.builder().startBlock(currentBlock).from(fromUser).to(to).chatId(chatId).build());
    }
}
