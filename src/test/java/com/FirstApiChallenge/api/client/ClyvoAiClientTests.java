package com.FirstApiChallenge.api.client;

import com.FirstApiChallenge.api.dto.AiAnimalContextDTO;
import com.FirstApiChallenge.api.dto.ClyvoAiRequestDTO;
import com.FirstApiChallenge.api.exception.CustomException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class ClyvoAiClientTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void deserializesValidResponseAndPostsExpectedContract() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes()));
            respond(exchange, 200, "{\"answer\":\"Resposta\",\"disclaimer\":\"Aviso\",\"model\":\"gemma3:4b\"}");
        });

        var response = client(Duration.ofSeconds(1)).chat(request());

        assertEquals("Resposta", response.answer());
        assertEquals("gemma3:4b", response.model());
        assertEquals("TUTOR", objectMapper.readTree(requestBody.get()).get("actorType").asText());
        assertEquals("Luna", objectMapper.readTree(requestBody.get()).get("animal").get("name").asText());
    }

    @Test
    void mapsBadRequestToSafeBadGateway() throws Exception {
        startServer(exchange -> respond(exchange, 400, "{}"));

        CustomException error = assertThrows(CustomException.class, () -> client(Duration.ofSeconds(1)).chat(request()));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
    }

    @Test
    void mapsBadGateway() throws Exception {
        startServer(exchange -> respond(exchange, 502, "{}"));

        CustomException error = assertThrows(CustomException.class, () -> client(Duration.ofSeconds(1)).chat(request()));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
    }

    @Test
    void mapsServiceUnavailable() throws Exception {
        startServer(exchange -> respond(exchange, 503, "{}"));

        CustomException error = assertThrows(CustomException.class, () -> client(Duration.ofSeconds(1)).chat(request()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
        assertEquals("Serviço de inteligência artificial temporariamente indisponível", error.getMessage());
    }

    @Test
    void mapsGatewayTimeout() throws Exception {
        startServer(exchange -> respond(exchange, 504, "{}"));

        CustomException error = assertThrows(CustomException.class, () -> client(Duration.ofSeconds(1)).chat(request()));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, error.getStatus());
    }

    @Test
    void mapsJavaHttpTimeout() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(300);
                respond(exchange, 200, "{}");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });

        CustomException error = assertThrows(CustomException.class,
                () -> client(Duration.ofMillis(50)).chat(request()));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, error.getStatus());
    }

    @Test
    void mapsInvalidJsonResponse() throws Exception {
        startServer(exchange -> respond(exchange, 200, "not-json"));

        CustomException error = assertThrows(CustomException.class, () -> client(Duration.ofSeconds(1)).chat(request()));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
    }

    @Test
    void mapsConnectionRefusedToServiceUnavailable() throws Exception {
        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }
        ClyvoAiClient client = new ClyvoAiClient(
                objectMapper,
                "http://localhost:" + unusedPort,
                Duration.ofSeconds(1),
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(200)).build()
        );

        CustomException error = assertThrows(CustomException.class, () -> client.chat(request()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
    }

    private ClyvoAiClient client(Duration requestTimeout) {
        return new ClyvoAiClient(
                objectMapper,
                "http://localhost:" + server.getAddress().getPort(),
                requestTimeout,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()
        );
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/v1/ai/chat", exchange -> handler.handle(exchange));
        server.start();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private ClyvoAiRequestDTO request() {
        return new ClyvoAiRequestDTO(
                "Resuma o histórico recente",
                "TUTOR",
                new AiAnimalContextDTO(10L, "Luna", "Cachorro", "SRD", 4, 9.5f),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
