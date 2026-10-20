package com.FirstApiChallenge.api.client;

import com.FirstApiChallenge.api.dto.ClyvoAiRequestDTO;
import com.FirstApiChallenge.api.dto.ClyvoAiResponseDTO;
import com.FirstApiChallenge.api.exception.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ClyvoAiClient {

    private static final String CHAT_PATH = "/api/v1/ai/chat";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI chatUri;
    private final Duration requestTimeout;

    @Autowired
    public ClyvoAiClient(
            ObjectMapper objectMapper,
            @Value("${clyvo.ai.url:http://localhost:5283}") String baseUrl,
            @Value("${clyvo.ai.connect-timeout:5s}") Duration connectTimeout,
            @Value("${clyvo.ai.request-timeout:130s}") Duration requestTimeout) {
        this(objectMapper, baseUrl, requestTimeout, HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build());
    }

    ClyvoAiClient(
            ObjectMapper objectMapper,
            String baseUrl,
            Duration requestTimeout,
            HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.chatUri = URI.create(baseUrl.replaceAll("/+$", "") + CHAT_PATH);
        this.requestTimeout = requestTimeout;
    }

    public ClyvoAiResponseDTO chat(ClyvoAiRequestDTO request) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(chatUri)
                    .header("Content-Type", "application/json")
                    .timeout(requestTimeout)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw statusException(response.statusCode());
            }

            return objectMapper.readValue(response.body(), ClyvoAiResponseDTO.class);
        } catch (HttpTimeoutException exception) {
            throw new CustomException("Tempo limite do serviço de inteligência artificial excedido", HttpStatus.GATEWAY_TIMEOUT);
        } catch (ConnectException exception) {
            throw unavailableException();
        } catch (JacksonException exception) {
            throw new CustomException("Resposta inválida do serviço de inteligência artificial", HttpStatus.BAD_GATEWAY);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailableException();
        } catch (IOException exception) {
            throw unavailableException();
        }
    }

    private CustomException statusException(int statusCode) {
        return switch (statusCode) {
            case 400 -> new CustomException("Requisição inválida enviada ao serviço de inteligência artificial", HttpStatus.BAD_GATEWAY);
            case 502 -> new CustomException("Resposta inválida do serviço de inteligência artificial", HttpStatus.BAD_GATEWAY);
            case 503 -> unavailableException();
            case 504 -> new CustomException("Tempo limite do serviço de inteligência artificial excedido", HttpStatus.GATEWAY_TIMEOUT);
            default -> new CustomException("Falha na comunicação com o serviço de inteligência artificial", HttpStatus.BAD_GATEWAY);
        };
    }

    private CustomException unavailableException() {
        return new CustomException(
                "Serviço de inteligência artificial temporariamente indisponível",
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }
}
