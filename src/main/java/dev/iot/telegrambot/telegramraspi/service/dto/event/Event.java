package dev.iot.telegrambot.telegramraspi.service.dto.event;

import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private BigInteger block_number;
    private Payload payload;

}
