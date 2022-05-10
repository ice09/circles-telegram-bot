package dev.iot.telegrambot.telegramraspi.service.dto.event;

import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payload {

    private String from;
    private String to;
    private BigInteger flow;

}
