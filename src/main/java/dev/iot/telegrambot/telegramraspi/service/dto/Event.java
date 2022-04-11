package dev.iot.telegrambot.telegramraspi.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
public class Event {

    private BigInteger block_number;
    private Payload payload;

}
