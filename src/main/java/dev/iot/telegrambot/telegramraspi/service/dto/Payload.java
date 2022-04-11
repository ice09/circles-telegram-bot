package dev.iot.telegrambot.telegramraspi.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@Builder
public class Payload {

    private String from;
    private String to;
    private BigInteger flow;

}
