package dev.iot.telegrambot.telegramraspi.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import dev.iot.telegrambot.telegramraspi.service.dto.event.EventResponseDto;
import dev.iot.telegrambot.telegramraspi.service.dto.profile.Data;
import dev.iot.telegrambot.telegramraspi.service.dto.profile.Search;
import dev.iot.telegrambot.telegramraspi.service.dto.profile.SearchResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;


@Slf4j
@Service
public class GraphQLClient {

    private final String graphQlUrl;

    public GraphQLClient(@Value("${graphql.url}") String graphQlUrl) {
        this.graphQlUrl = graphQlUrl;
    }

    private HttpResponse callGraphQLService(String url, String query) throws URISyntaxException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);
        URI uri = new URIBuilder(request.getURI())
                .addParameter("query", query)
                .build();
        request.setURI(uri);
        return client.execute(request);
    }

    private EventResponseDto readEventResponse(String serviceUrl, String query) throws URISyntaxException, IOException {
        HttpResponse httpResponse = callGraphQLService(serviceUrl, query);
        String actualResponse = IOUtils.toString(httpResponse.getEntity().getContent(), UTF_8.name());
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        EventResponseDto parsedResponse = objectMapper.readValue(actualResponse, EventResponseDto.class);
        return parsedResponse;
    }

    private SearchResponseDto readSearchResponse(String serviceUrl, String circlesUser, String query) throws URISyntaxException, IOException {
        HttpResponse httpResponse = callGraphQLService(serviceUrl, query);
        String actualResponse = IOUtils.toString(httpResponse.getEntity().getContent(), UTF_8.name());
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SearchResponseDto parsedResponse = objectMapper.readValue(actualResponse, SearchResponseDto.class);
        if ((!parsedResponse.getData().getSearch().isEmpty()) && (parsedResponse.getData().getSearch().size() > 1)) {
            throw new IllegalArgumentException("Cannot uniquely identify Circles user, there are " + parsedResponse.getData().getSearch().size() + " matches for *" + circlesUser + "*.");
        }
        return parsedResponse;
    }

    public EventResponseDto trackAccount(String from) throws URISyntaxException, IOException {
        Resource resource = new ClassPathResource("request.json");
        String query = IOUtils.toString(resource.getInputStream(), UTF_8);
        query = query.replaceAll("FROM", from.toLowerCase()).replaceAll("STARTDATE", LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        return readEventResponse(graphQlUrl, query);
    }

    public SearchResponseDto readProfile(String searchString) throws URISyntaxException, IOException {
        Resource resource = new ClassPathResource("request_profile.json");
        String query = IOUtils.toString(resource.getInputStream(), UTF_8);
        if (searchString.startsWith("\"")) {
            searchString = searchString.substring(1, searchString.length()-1);
        }
        query = query.replaceAll("USERNAME", searchString);
        return readSearchResponse(graphQlUrl, searchString, query);
    }
}
