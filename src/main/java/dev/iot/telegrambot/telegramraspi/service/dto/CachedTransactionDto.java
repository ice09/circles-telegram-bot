package dev.iot.telegrambot.telegramraspi.service.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@EqualsAndHashCode
public class CachedTransactionDto {

    private BigInteger startBlock;
    private String to;
    private String toAddr;
    private String from;
    private BigInteger amount;
    private String chatId;

}
