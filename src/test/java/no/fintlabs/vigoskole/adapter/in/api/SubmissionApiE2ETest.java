package no.fintlabs.vigoskole.adapter.in.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import no.fintlabs.vigoskole.TestData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SubmissionApiE2ETest {

  private static HttpServer kodeverkServer;
  private static final Queue<String> queuedResponses = new ArrayDeque<>();

  @LocalServerPort private int port;

  @BeforeAll
  static void setUp() throws IOException {
    ensureInfrastructure();
  }

  @BeforeEach
  void clearQueuedResponses() {
    queuedResponses.clear();
  }

  @AfterAll
  static void tearDown() throws IOException {
    if (kodeverkServer != null) {
      kodeverkServer.stop(0);
    }
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    ensureInfrastructure();
    registry.add(
        "app.vigo-kodeverk.base-url",
        () -> "http://localhost:" + kodeverkServer.getAddress().getPort());
    registry.add("app.storage-directory", SubmissionApiE2ETest::newStorageDirectory);
  }

  @Test
  void shouldAcceptSubmissionAndRejectDuplicate() {
    enqueueResponse(TestData.kodeverkResponse());
    enqueueResponse(TestData.kodeverkResponse());

    HttpResponse<String> firstResponse =
        exchange(TestData.warningSubmissionPayload(), "school-token");
    HttpResponse<String> secondResponse =
        exchange(TestData.validSubmissionPayload(), "school-token");

    assertThat(firstResponse.statusCode()).isEqualTo(201);
    assertThat(firstResponse.body()).contains("ACCEPTED_WITH_WARNINGS");
    assertThat(secondResponse.statusCode()).isEqualTo(403);
    assertThat(secondResponse.body()).contains("godkjent innsending");
  }

  @Test
  void shouldReturnValidationErrorsForRejectedSubmission() {
    enqueueResponse(TestData.kodeverkResponse());

    HttpResponse<String> response = exchange(TestData.invalidSubmissionPayload(), "school-token");

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.body()).contains("VALIDATION_ERROR");
    assertThat(response.body()).contains("Klassekode er påkrevd");
    assertThat(response.body()).contains("Fødselsnummer er ugyldig");
  }

  private HttpResponse<String> exchange(String payload, String token) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create("http://localhost:" + port + "/api/submissions/graduating-students"))
              .header("Authorization", "Bearer " + token)
              .header("Content-Type", "application/ld+json")
              .POST(HttpRequest.BodyPublishers.ofString(payload))
              .build();
      return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static void ensureInfrastructure() {
    try {
      if (kodeverkServer == null) {
        kodeverkServer = HttpServer.create(new java.net.InetSocketAddress(0), 0);
        kodeverkServer.createContext(
            "/api/schools",
            exchange -> {
              byte[] ignoredBody = exchange.getRequestBody().readAllBytes();
              String response = queuedResponses.poll();
              if (response == null) {
                exchange.sendResponseHeaders(500, 0);
                exchange.getResponseBody().close();
                return;
              }
              byte[] body = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
              exchange.getResponseHeaders().add("Content-Type", "application/json");
              exchange.sendResponseHeaders(200, body.length);
              exchange.getResponseBody().write(body);
              exchange.close();
            });
        kodeverkServer.start();
      }
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static void enqueueResponse(String response) {
    queuedResponses.add(response);
  }

  private static String newStorageDirectory() {
    try {
      Path storageDirectory = Files.createTempDirectory("vigoskole-api-test");
      return storageDirectory.toString();
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @TestConfiguration
  static class JwtDecoderConfig {

    @Bean
    @Primary
    JwtDecoder jwtDecoder() {
      return token ->
          new Jwt(
              token,
              Instant.now(),
              Instant.now().plusSeconds(3600),
              Map.of("alg", "none"),
              Map.of(
                  "sub", "api-client",
                  "org_number", TestData.SCHOOL_ORG_NUMBER,
                  "consumer_name", "Test ungdomsskole",
                  "supplier_org_number", "999888777",
                  "supplier_name", "Leverandor AS"));
    }
  }
}
