package dev.iot.telegrambot.telegramraspi.web3;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class MonitorConfig {

    private final String ethereumRpcUrl;
    private final String mnemonic;

    public MonitorConfig(@Value("${web3.ethereum.rpc.url}") String ethereumRpcUrl, @Value("${web3.mnemonic}") String mnemonic) {
        this.ethereumRpcUrl = ethereumRpcUrl;
        this.mnemonic = mnemonic;
    }

    @Bean
    public CredentialHolder createCredentials() {
        return new CredentialHolder(createMasterKeyPair());
    }

    public Bip32ECKeyPair createMasterKeyPair() {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");
        return Bip32ECKeyPair.generateKeyPair(seed);
    }

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(ethereumRpcUrl, createOkHttpClient()));
    }

    private OkHttpClient createOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        configureTimeouts(builder);
        return builder.build();
    }

    private void configureTimeouts(OkHttpClient.Builder builder) {
        long tos = 800000L;
        builder.connectTimeout(tos, TimeUnit.SECONDS);
        builder.readTimeout(tos, TimeUnit.SECONDS);  // Sets the socket timeout too
        builder.writeTimeout(tos, TimeUnit.SECONDS);
    }

}