package com.agnitio.infrastructure.client.vision;

import com.agnitio.application.dto.FaceMatchDto;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real HTTP client (no mocks) against a local server to verify what goes over
 * the wire. Regression test: the JDK client used to send "Upgrade: h2c" with a chunked body,
 * which uvicorn (vision-service) drops, resulting in a 422 "Field required" response.
 */
class VisionServiceHttpClientWireTest {

    private HttpServer server;
    private final AtomicReference<String> upgradeHeader = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/faces/search", exchange -> {
            upgradeHeader.set(exchange.getRequestHeaders().getFirst("Upgrade"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));

            byte[] response = "{\"totalMatches\":0,\"matches\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    @DisplayName("should send a plain HTTP/1.1 multipart request with all form fields")
    void shouldSendMultipartFieldsWithoutHttp2Upgrade() {
        UUID eventId = UUID.randomUUID();
        VisionServiceHttpClient client = new VisionServiceHttpClient(
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-key");

        List<FaceMatchDto> matches = client.searchFacesBySelfie(eventId, new byte[]{1, 2, 3}, "selfie.jpg", 0.6, 50);

        assertThat(matches).isEmpty();
        assertThat(upgradeHeader.get()).isNull();
        assertThat(contentType.get()).startsWith("multipart/form-data");
        assertThat(body.get())
                .contains("name=\"eventId\"")
                .contains(eventId.toString())
                .contains("name=\"selfie\"; filename=\"selfie.jpg\"")
                .contains("name=\"threshold\"")
                .contains("name=\"limit\"");
    }
}
