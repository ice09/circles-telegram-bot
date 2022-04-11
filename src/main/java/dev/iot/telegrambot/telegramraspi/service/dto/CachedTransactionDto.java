package dev.iot.telegrambot.telegramraspi.service.dto;

import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CachedTransactionDto {

    private BigInteger startBlock;
    private String to;
    private String toAddr;
    private String from;
    private BigInteger amount;
    private String chatId;

}
