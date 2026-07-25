package com.digitalheroes.pagepulse;

import com.digitalheroes.pagepulse.dto.AuditRequest;
import com.digitalheroes.pagepulse.dto.AuditResponse;
import com.digitalheroes.pagepulse.dto.ErrorResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Full request/response cycle tests: real HTTP calls into a running Spring Boot
 * instance, against a local HttpServer standing in for the audited page, so no
 * real internet access is required and every scenario is fully controlled.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditIntegrationTest {

    private static HttpServer targetServer;
    private static String targetBaseUrl;

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @BeforeAll
    static void startTargetServer() throws IOException {
        targetServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        targetServer.setExecutor(Executors.newCachedThreadPool());

        targetServer.createContext("/happy", exchange -> {
            String html = """
                    <html>
                    <head>
                        <title>Integration Test Page</title>
                        <meta name="description" content="A page served locally for integration testing.">
                    </head>
                    <body>
                        <h1>Home</h1>
                        <h2>About</h2>
                        <h2>Contact</h2>
                        <p>Welcome to our test page today.</p>
                    </body>
                    </html>
                    """;
            respond(exchange, 200, "text/html; charset=utf-8", html);
        });

        targetServer.createContext("/non-html", exchange ->
                respond(exchange, 200, "application/json", "{}"));

        targetServer.createContext("/slow", exchange -> {
            try {
                Thread.sleep(7000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "text/html", "<html></html>");
        });

        targetServer.start();
        targetBaseUrl = "http://localhost:" + targetServer.getAddress().getPort();
    }

    @AfterAll
    static void stopTargetServer() {
        targetServer.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status,
                                 String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Test
    void happyPath_returnsFullMetricsThroughTheRealHttpPipeline() {
        ResponseEntity<AuditResponse> response = restTemplate.postForEntity(
                auditUrl(),
                new AuditRequest(targetBaseUrl + "/happy"),
                AuditResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuditResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(200, body.status());
        assertEquals("Integration Test Page", body.title());
        assertEquals("A page served locally for integration testing.", body.metaDescription());
        assertEquals(1, body.h1Count());
        assertEquals(9, body.wordCount());
        assertEquals(2, body.headingCounts().h2());
        assertEquals(0, body.images().total());
    }

    @Test
    void invalidUrl_returns400() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                auditUrl(),
                new AuditRequest("not-a-url"),
                ErrorResponse.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid URL", response.getBody().error());
    }

    @Test
    void connectionRefused_returns502() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                auditUrl(),
                new AuditRequest("http://127.0.0.1:" + closedPort + "/"),
                ErrorResponse.class
        );

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("Unable to fetch URL", response.getBody().error());
    }

    @Test
    void nonHtmlResponse_returns415() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                auditUrl(),
                new AuditRequest(targetBaseUrl + "/non-html"),
                ErrorResponse.class
        );

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertEquals("URL does not point to an HTML page", response.getBody().error());
    }

    @Test
    void requestTimeout_returns504() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                auditUrl(),
                new AuditRequest(targetBaseUrl + "/slow"),
                ErrorResponse.class
        );

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertEquals("Request timed out", response.getBody().error());
    }

    private String auditUrl() {
        return "http://localhost:" + port + "/api/audit";
    }
}
