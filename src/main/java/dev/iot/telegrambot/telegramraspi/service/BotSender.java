package dev.iot.telegrambot.telegramraspi.service;

public interface BotSender {

    void createAndSendMessage(String chatId, String content, String parseMode);

}
