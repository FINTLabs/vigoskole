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
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import no.novari.vigoskole.TestData;
import no.novari.vigoskole.config.SecurityConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.main.allow-bean-definition-overriding=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SubmissionApiE2ETest {

  private static HttpServer kodeverkServer;
  private static final Queue<String> queuedResponses = new ArrayDeque<>();
  private static volatile String lastKodeverkRequestBody;

  @LocalServerPort private int port;

  @BeforeAll
  static void setUp() throws IOException {
    ensureInfrastructure();
  }

  @BeforeEach
  void clearQueuedResponses() {
    queuedResponses.clear();
    lastKodeverkRequestBody = null;
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
    assertThat(firstResponse.body()).contains("Ås ungdomsskole");
    assertThat(firstResponse.body()).contains("991825827");
    assertThat(lastKodeverkRequestBody).contains("974603268");
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

  @Test
  void shouldRejectSubmissionWhenConsumerOrgNumberIsNotVerifiedAsLowerSecondarySchool() {
    enqueueResponse(TestData.kodeverkResponseWithoutMatchingSchool());

    HttpResponse<String> response = exchange(TestData.validSubmissionPayload(), "school-token");

    assertThat(response.statusCode()).isEqualTo(403);
    assertThat(response.body()).contains("tilhører ikke en ungdomsskole");
    assertThat(lastKodeverkRequestBody).contains("974603268");
  }

  @Test
  void shouldRejectTokenWhenConsumerClaimIsMissing() {
    HttpResponse<String> response =
        exchange(TestData.validSubmissionPayload(), "missing-consumer-token");

    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  void shouldRejectTokenIssuedFromSkyporten() {
    HttpResponse<String> response = exchange(TestData.validSubmissionPayload(), "skyporten-token");

    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  void shouldRejectEndUserRestrictedToken() {
    HttpResponse<String> response = exchange(TestData.validSubmissionPayload(), "enduser-token");

    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  void shouldRejectTokenWithUnexpectedScope() {
    HttpResponse<String> response =
        exchange(TestData.validSubmissionPayload(), "wrong-scope-token");

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
              lastKodeverkRequestBody =
                  new String(
                      exchange.getRequestBody().readAllBytes(),
                      java.nio.charset.StandardCharsets.UTF_8);
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
    JwtDecoder jwtDecoder(OAuth2TokenValidator<Jwt> maskinportenTokenValidator) {
      return token -> {
        Jwt jwt = createJwt(token);
        var validationResult = maskinportenTokenValidator.validate(jwt);
        if (validationResult.hasErrors()) {
          throw new JwtValidationException(
              "Maskinporten token validation failed", validationResult.getErrors());
        }
        return jwt;
      };
    }

    private Jwt createJwt(String token) {
      Map<String, Object> claims =
          switch (token) {
            case "missing-consumer-token" -> baseClaimsWithoutConsumer();
            case "skyporten-token" -> claimsWithSub();
            case "enduser-token" -> claimsWithPid();
            case "wrong-scope-token" -> claimsWithWrongScope();
            default -> baseClaims();
          };
      return new Jwt(
          token,
          Instant.now(),
          Instant.now().plusSeconds(3600),
          Map.of("alg", "RS256", "kid", "test-key"),
          claims);
    }

    private Map<String, Object> baseClaims() {
      Map<String, Object> claims = baseClaimsWithoutConsumer();
      claims.put("consumer", Map.of("authority", "iso6523-actorid-upis", "ID", "0192:974603268"));
      return claims;
    }

    private Map<String, Object> baseClaimsWithoutConsumer() {
      return new java.util.HashMap<>(
          Map.of(
              "iss", SecurityConfig.MASKINPORTEN_ISSUER,
              "client_id", "vigoskole-client",
              "client_amr", "virksomhetssertifikat",
              "scope", SecurityConfig.MASKINPORTEN_SCOPE,
              "token_type", "Bearer",
              "jti", "jwt-id-123",
              "supplier", Map.of("authority", "iso6523-actorid-upis", "ID", "0192:991825827"),
              "delegation_source", "https://www.altinn.no"));
    }

    private Map<String, Object> claimsWithSub() {
      Map<String, Object> claims = new java.util.HashMap<>(baseClaims());
      claims.put("sub", "0192:310175838;novari:vigoskole.write");
      return claims;
    }

    private Map<String, Object> claimsWithPid() {
      Map<String, Object> claims = new java.util.HashMap<>(baseClaims());
      claims.put("pid", "01010199999");
      return claims;
    }

    private Map<String, Object> claimsWithWrongScope() {
      Map<String, Object> claims = new java.util.HashMap<>(baseClaims());
      claims.put("scope", "novari:wrong-scope");
      return claims;
    }
  }
}
