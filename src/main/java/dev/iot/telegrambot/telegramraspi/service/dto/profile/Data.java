package dev.iot.telegrambot.telegramraspi.service.dto.profile;

import dev.iot.telegrambot.telegramraspi.service.dto.event.Event;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Data {

    private List<Search> search = new ArrayList<>();
}
