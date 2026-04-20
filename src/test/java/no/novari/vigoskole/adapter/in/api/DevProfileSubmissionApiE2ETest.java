package no.novari.vigoskole.adapter.in.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Queue;
import no.novari.vigoskole.TestData;
import no.novari.vigoskole.application.SchoolYearRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DevProfileSubmissionApiE2ETest {

  private static HttpServer kodeverkServer;
  private static final Queue<String> queuedResponses = new ArrayDeque<>();
  private static volatile String lastKodeverkRequestMethod;
  private static volatile String lastKodeverkRequestPath;

  @LocalServerPort private int port;

  @org.springframework.beans.factory.annotation.Autowired
  private SchoolYearRepository schoolYearRepository;

  @BeforeAll
  static void setUp() throws IOException {
    ensureInfrastructure();
  }

  @BeforeEach
  void clearQueuedResponses() {
    queuedResponses.clear();
    lastKodeverkRequestMethod = null;
    lastKodeverkRequestPath = null;
    schoolYearRepository.save(TestData.schoolYearConfiguration());
  }

  @AfterAll
  static void tearDown() {
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
    registry.add("app.storage-directory", DevProfileSubmissionApiE2ETest::newStorageDirectory);
  }

  @Test
  void shouldAcceptConfiguredFakeTokenInDevProfile() {
    enqueueResponse(TestData.kodeverkResponse());

    HttpResponse<String> response = exchange(TestData.validSubmissionPayload(), "local-test-token");

    assertThat(response.statusCode()).isEqualTo(201);
    assertThat(response.body()).contains("Ås ungdomsskole");
    assertThat(lastKodeverkRequestMethod).isEqualTo("POST");
    assertThat(lastKodeverkRequestPath).isEqualTo("/api/schools?page=0&size=10");
  }

  @Test
  void shouldRejectUnknownFakeTokenInDevProfile() {
    HttpResponse<String> response = exchange(TestData.validSubmissionPayload(), "wrong-token");

    assertThat(response.statusCode()).isEqualTo(401);
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
              lastKodeverkRequestMethod = exchange.getRequestMethod();
              lastKodeverkRequestPath = exchange.getRequestURI().toString();
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
      Path storageDirectory = Files.createTempDirectory("vigoskole-dev-profile-api-test");
      return storageDirectory.toString();
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
