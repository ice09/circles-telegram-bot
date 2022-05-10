package dev.iot.telegrambot.telegramraspi.service.dto.event;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Data {

    private List<Event> events = new ArrayList<>();
}
