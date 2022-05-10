package dev.iot.telegrambot.telegramraspi.web3;

import com.google.common.collect.Lists;
import dev.iot.telegrambot.telegramraspi.service.dto.profile.Search;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.DefaultGasProvider;
import tech.blockchainers.GnosisSafe;

import java.util.List;

@Slf4j
@Service
public class GnosisSafeOwnerCheck {

    private final Web3j web3j;
    private final CredentialHolder credentials;

    public GnosisSafeOwnerCheck(Web3j web3j, CredentialHolder credentials) {
        this.web3j = web3j;
        this.credentials = credentials;
    }

    public List<String> loadGnosisSafeOwner(String ethereumAddress) {
        try {
            return GnosisSafe.load(ethereumAddress, web3j, credentials.deriveChildKeyPair(0), new DefaultGasProvider()).getOwners().send();
        } catch (Exception ex) {
            log.error("Cannot load owners from Gnosis Safe for " + ethereumAddress);
            return Lists.newArrayList();
        }
        /* also check for others things, like eg. trust given for double check
        BigInteger trusted = hub.limits(gnosisSafe.getContractAddress(), "0x945CaC6047B1f58945ed2aafA5BaeD96A31faa4c").send();
        log.info("limit: {}", trusted.intValue());
        return true;
         */
    }
}