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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class Web3TransactionChecker {

    //http://localhost:8080/prefill?from=0x249fA3ecD95a53F742707D53688FCafbBd072f33&to=0x4a9aFfA9249F36fd0629f342c182A4e94A13C2e0&amount=100&chatId=1&inviteLink=11
    //http://localhost:8080/prefill?from=0x249fA3ecD95a53F742707D53688FCafbBd072f33&to=0xde374ece6fa50e781e81aac78e811b33d16912c7&amount=100&chatId=1&inviteLink=11
    private Map<String, List<CachedTransactionDto>> watchedTransactions;
    private final Web3Reader web3Reader;

    private final GraphQLClient graphQLClient;
    private BotSender botSender;

    public Web3TransactionChecker(Web3Reader web3Reader, GraphQLClient graphQLClient) {
        this.web3Reader = web3Reader;
        this.graphQLClient = graphQLClient;
        this.watchedTransactions = new HashMap<>();
    }

    @Scheduled(fixedDelay = 2000)
    public void checkTransactions() throws URISyntaxException, IOException {
        for (Map.Entry<String, List<CachedTransactionDto>> entries : watchedTransactions.entrySet()) {
            ResponseDto cTrx = graphQLClient.trackAccount(entries.getKey());
            List<CachedTransactionDto> copyList = new ArrayList<>();
            for (CachedTransactionDto entry : entries.getValue()) {
                log.info("Tracking: " + entries.getKey() + " with startBlock " + entry.getStartBlock());
                Data trxData = cTrx.getData();
                copyList.add(entry);
                for (Event evt : trxData.getEvents()) {
                    if (evt.getBlock_number().compareTo(entry.getStartBlock()) > 0) {
                        if (evt.getPayload().getTo().equalsIgnoreCase(entry.getToAddr())) {
                            notify(entry.getChatId(), "Received transfer from *" + entry.getFrom() + "* to *" + entry.getTo() + "* with amount " + BigDecimal.valueOf(evt.getPayload().getFlow().longValue()).divide(BigDecimal.valueOf(1_000_000_000_000_000_000l)).setScale(2, RoundingMode.DOWN) + " CRC at block " + evt.getBlock_number());
                            copyList.remove(entry);
                        }
                    } else {
                        log.debug("Found only older event before startBlock " + entry.getStartBlock());
                        break;
                    }
                }
                boolean watchedMoreThan10Blocks = web3Reader.getCurrentBlock().subtract(entry.getStartBlock()).intValue() > 10;
                if (watchedMoreThan10Blocks && copyList.contains(entry)) {
                    log.error("Aborting tracking " + entries.getKey() + ". No answer for the last 10 blocks.");
                    notify(entry.getChatId(), "Stopped watching *" + entry.getFrom() + "* (" + entries.getKey() + ") to *" + entry.getTo() + "*, no transfer for 10 blocks.");
                    copyList.remove(entry);
                }
                entries.setValue(copyList);
            }
        }
        Map<String, List<CachedTransactionDto>> copyWatchedTransations = new HashMap<>();
        for (Map.Entry<String, List<CachedTransactionDto>> entries : watchedTransactions.entrySet()) {
            if (!entries.getValue().isEmpty()) {
                copyWatchedTransations.put(entries.getKey(), entries.getValue());
            }
        }
        watchedTransactions = new HashMap<>(copyWatchedTransations);
    }

    private void notify(String chatId, String message) {
        botSender.createAndSendMessage(chatId, message, "Markdown");
    }


    public void trackAccount(String chatId, String safe, String fromUser, String toAddr, String to, BotSender botSender) {
        BigInteger currentBlock = web3Reader.getCurrentBlock();
        //TODO: Refactor it! Why everytime again?
        this.botSender = botSender;
        CachedTransactionDto newCacheDto = CachedTransactionDto.builder().startBlock(currentBlock).from(fromUser).to(to).toAddr(toAddr).chatId(chatId).build();
        List<CachedTransactionDto> cachedTrxs;
        if (watchedTransactions.containsKey(safe)) {
            cachedTrxs = watchedTransactions.get(safe);
        } else {
            cachedTrxs = new ArrayList<>();
        }
        cachedTrxs.add(newCacheDto);
        watchedTransactions.put(safe, cachedTrxs);
        log.info("Start watching " + fromUser + "(" + safe + ") to " + to);
    }
}