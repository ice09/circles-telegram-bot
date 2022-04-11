package dev.iot.telegrambot.telegramraspi.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@Builder
public class Event {

    private BigInteger block_number;
    private Payload payload;

}
