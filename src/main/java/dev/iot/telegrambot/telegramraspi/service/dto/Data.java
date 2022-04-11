package dev.iot.telegrambot.telegramraspi.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Data {

    private List<Event> events = new ArrayList<>();
}
