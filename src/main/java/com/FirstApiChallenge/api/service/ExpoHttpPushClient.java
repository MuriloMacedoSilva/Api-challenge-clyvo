package com.FirstApiChallenge.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ExpoHttpPushClient implements ExpoPushClient {

    private final URI endpoint;
    private final Duration requestTimeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ExpoHttpPushClient(
            @Value("${clyvo.push.expo.endpoint:https://exp.host/--/api/v2/push/send}") URI endpoint,
            @Value("${clyvo.push.expo.connect-timeout:3s}") Duration connectTimeout,
            @Value("${clyvo.push.expo.request-timeout:5s}") Duration requestTimeout
    ) {
        this.endpoint = endpoint;
        this.requestTimeout = requestTimeout;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    @Override
    public ExpoPushTicket send(String token, String title, String body, Map<String, Object> data) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("to", token);
        payload.put("title", title);
        payload.put("body", body);
        payload.put("sound", "default");
        payload.put("channelId", "clyvo-default");
        payload.put("data", data);

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Expo Push Service respondeu HTTP " + response.statusCode());
        }

        JsonNode ticket = objectMapper.readTree(response.body()).path("data");
        if ("ok".equals(ticket.path("status").asText())) {
            return ExpoPushTicket.success();
        }
        return ExpoPushTicket.rejected(
                ticket.path("details").path("error").asText(null),
                ticket.path("message").asText("Expo rejeitou a notificação")
        );
    }
}
