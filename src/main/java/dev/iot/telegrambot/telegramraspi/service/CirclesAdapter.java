package dev.iot.telegrambot.telegramraspi.service;

import com.jayway.jsonpath.JsonPath;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

@Slf4j
@Service
public class CirclesAdapter {

    public static final String CIRCLES_GARDEN_API_USERS = "https://api.circles.garden/api/users/";

    public Optional<SendPhoto> loadCirclesUserAvatar(String chatId, String sCirclesUser) {
        try {
            String circlesUser = Unirest.get(CIRCLES_GARDEN_API_USERS + sCirclesUser).asString().getBody();
            Object oAvatar = JsonPath.parse(circlesUser).read("$['data']['avatarUrl']");
            String sAvatar = oAvatar.toString();
            SendPhoto photo = new SendPhoto(chatId, new InputFile(new URL(sAvatar).openStream(), "avatar.jpg"));
            return Optional.of(photo);
        } catch (Exception ex) {
            log.error("Cannot load user data from Circles.", ex);
            return Optional.empty();
        }
    }

    public SendPhoto addCaptionForQuery(SendPhoto sendPhoto, String circlesUsername) {
        String circlesUser = Unirest.get(CIRCLES_GARDEN_API_USERS + circlesUsername).asString().getBody();
        Object oSafe = JsonPath.parse(circlesUser).read("$['data']['safeAddress']");
        Object oName = JsonPath.parse(circlesUser).read("$['data']['username']");
        String sSafe = oSafe.toString();
        String sName = oName.toString();
        String caption = "Circles name is *" + sName + "* with Gnosis Safe address *" + sSafe + "*";
        sendPhoto.setParseMode("Markdown");
        sendPhoto.setCaption(caption);
        return sendPhoto;
    }

    public SendPhoto addCaptionForInfo(SendPhoto sendPhoto, String circlesUsername, String telegramName, String derivedHashId) {
        String circlesUser = Unirest.get(CIRCLES_GARDEN_API_USERS + circlesUsername).asString().getBody();
        Object oName = JsonPath.parse(circlesUser).read("$['data']['username']");
        String sName = oName.toString();
        String caption = "Hi *" + telegramName + "*, your Circles name is *" + sName + "*";
        if (StringUtils.hasText(derivedHashId) &&  !sName.equals(derivedHashId)) {
            caption += "\n_Note: This address was set manually, it is not derived from the Telegram ID._";
        }
        sendPhoto.setParseMode("Markdown");
        sendPhoto.setCaption(caption);
        return sendPhoto;
    }

}
