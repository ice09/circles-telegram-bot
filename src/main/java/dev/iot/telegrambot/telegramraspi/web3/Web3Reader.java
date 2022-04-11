package dev.iot.telegrambot.telegramraspi.web3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;

@Service
@Slf4j
public class Web3Reader {

    private final Web3j httpWeb3j;

    public Web3Reader(Web3j httpWeb3j) {
        this.httpWeb3j = httpWeb3j;
    }

    public BigInteger getCurrentBlock() {
        try {
            return httpWeb3j.ethBlockNumber().send().getBlockNumber();
        } catch (Exception e) {
            log.error("Cannot read current block number: {}", e.getMessage());
            return BigInteger.ZERO;
        }
    }

}
