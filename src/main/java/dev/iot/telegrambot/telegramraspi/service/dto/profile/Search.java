package dev.iot.telegrambot.telegramraspi.service.dto.profile;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Search {

    private String firstName;
    private String lastName;
    private String circlesAddress;
    private String avatarUrl;

}
