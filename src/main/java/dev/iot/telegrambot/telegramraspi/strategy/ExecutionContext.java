package dev.iot.telegrambot.telegramraspi.strategy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutionContext {

    private String telegramId;
    private String telegramName;
    private String chatId;
    private String[] commandArguments;

}
