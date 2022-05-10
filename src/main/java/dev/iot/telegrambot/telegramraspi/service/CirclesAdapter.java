package dev.iot.telegrambot.telegramraspi.service;

import dev.iot.telegrambot.telegramraspi.service.dto.profile.Search;
import dev.iot.telegrambot.telegramraspi.service.dto.profile.SearchResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CirclesAdapter {

    private final GraphQLClient graphQLClient;

    public CirclesAdapter(GraphQLClient graphQLClient) {
        this.graphQLClient = graphQLClient;
    }

    public Optional<SendPhoto> loadCirclesUserAvatar(String chatId, String sCirclesUser) {
        try {
            SearchResponseDto circlesUser = graphQLClient.readProfile(sCirclesUser);
            if (!circlesUser.getData().getSearch().isEmpty()) {
                if (!StringUtils.hasText(circlesUser.getData().getSearch().get(0).getAvatarUrl())) {
                    log.error("User has no avatar: " + sCirclesUser);
                    return Optional.empty();
                } else {
                    SendPhoto photo = new SendPhoto(chatId, new InputFile(new URL(circlesUser.getData().getSearch().get(0).getAvatarUrl()).openStream(), "avatar.jpg"));
                    return Optional.of(photo);
                }
            } else {
                log.error("Cannot find user avatar for " + sCirclesUser);
                return Optional.empty();
            }
        } catch (RuntimeException ex) {
            log.error("Cannot load user data from Circles.", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Cannot load user data from Circles.", ex);
            return Optional.empty();
        }
    }

    public Optional<String> verifyCirclesUserName(String circlesUsername) {
        try {
            List<Search> searchResponseDto = graphQLClient.readProfile(circlesUsername).getData().getSearch();
            if (searchResponseDto.isEmpty()) {
                log.error("Cannot load user profile for " + circlesUsername);
                return Optional.empty();
            } else {
                return Optional.of(getFullName(searchResponseDto.get(0)));
            }
        } catch (Exception ex) {
            log.error("Cannot load user data from Circles.", ex);
            return Optional.empty();
        }
    }

    public Optional<String> deriveSafeAddress(String circlesUsername) {
        try {
            List<Search> circlesSafe = graphQLClient.readProfile(circlesUsername).getData().getSearch();
            if (circlesSafe.isEmpty()) {
                log.error("Cannot load user safe address for " + circlesUsername);
                return Optional.empty();
            } else {
                return Optional.of(circlesSafe.get(0).getCirclesAddress());
            }
        } catch (Exception ex) {
            log.error("Cannot load user data from Circles.", ex);
            return Optional.empty();
        }
    }

    public SendPhoto addCaptionForQuery(SendPhoto sendPhoto, String circlesUsername) throws URISyntaxException, IOException {
        List<Search> circlesUser = graphQLClient.readProfile(circlesUsername).getData().getSearch();
        if (circlesUser.isEmpty()) {
            String caption = "Cannot find Circles profile for *" + circlesUsername + "*";
            sendPhoto.setParseMode("Markdown");
            sendPhoto.setCaption(caption);
            return sendPhoto;
        } else {
            String safe = circlesUser.get(0).getCirclesAddress();
            String name = getFullName(circlesUser.get(0));
            String caption = "Circles name is *" + name + "* with Gnosis Safe address *" + safe + "*";
            sendPhoto.setParseMode("Markdown");
            sendPhoto.setCaption(caption);
            return sendPhoto;
        }
    }

    public SendPhoto addCaptionForInfo(SendPhoto sendPhoto, String circlesUsername, String telegramName, String derivedHashId) throws URISyntaxException, IOException {
        List<Search> circlesUser = graphQLClient.readProfile(circlesUsername).getData().getSearch();
        if (circlesUser.isEmpty()) {
            String caption = "Cannot find Circles profile for *" + circlesUsername + "*";
            sendPhoto.setParseMode("Markdown");
            sendPhoto.setCaption(caption);
            return sendPhoto;
        } else {
            String caption = "Hi *" + telegramName + "*, your Circles name is *" + getFullName(circlesUser.get(0)) + "*";
            if (StringUtils.hasText(derivedHashId) && !getFullName(circlesUser.get(0)).equals(derivedHashId)) {
                caption += "\n_Note: This address was set manually, it is not derived from the Telegram ID._";
            }
            sendPhoto.setParseMode("Markdown");
            sendPhoto.setCaption(caption);
            return sendPhoto;
        }
    }

    private String getFullName(Search searchDto) {
        String name = searchDto.getFirstName();
        if (StringUtils.hasText(searchDto.getLastName())) {
            name = "\"" + name + " " + searchDto.getLastName() + "\"";
        }
        return name;
    }

}
