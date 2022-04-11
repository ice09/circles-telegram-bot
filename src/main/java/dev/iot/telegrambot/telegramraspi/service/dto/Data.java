package dev.iot.telegrambot.telegramraspi.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class Data {

    private List<Event> events = new ArrayList<>();
}
