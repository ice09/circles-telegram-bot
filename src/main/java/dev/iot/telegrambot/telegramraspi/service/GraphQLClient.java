package dev.iot.telegrambot.telegramraspi.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iot.telegrambot.telegramraspi.service.dto.ResponseDto;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class GraphQLClient {

    private HttpResponse callGraphQLService(String url, String query) throws URISyntaxException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);
        URI uri = new URIBuilder(request.getURI())
                .addParameter("query", query)
                .build();
        request.setURI(uri);
        return client.execute(request);
    }

    private ResponseDto readResponse(String serviceUrl, String query) throws URISyntaxException, IOException {
        HttpResponse httpResponse = callGraphQLService(serviceUrl, query);
        String actualResponse = IOUtils.toString(httpResponse.getEntity().getContent(), UTF_8.name());
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ResponseDto parsedResponse = objectMapper.readValue(actualResponse, ResponseDto.class);
        return parsedResponse;
    }

    public ResponseDto trackAccount(String from) throws URISyntaxException, IOException {
        Resource resource = new ClassPathResource("request.json");
        String query = IOUtils.toString(resource.getInputStream(), UTF_8);
        query = query.replaceAll("FROM", from.toLowerCase()).replaceAll("STARTDATE", LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        return readResponse("https://api.circles.land", query);
    }
}
