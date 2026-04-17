package no.novari.vigoskole.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class SecurityConfigJwtDecoderTest {

  private final SecurityConfig securityConfig = new SecurityConfig();

  private MockWebServer mockWebServer;
  private RSAKey publishedKey;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    publishedKey = new RSAKeyGenerator(2048).keyID("maskinporten-key").generate();
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.close();
  }

  @Test
  void shouldAcceptSignedTokenAndCacheJwksForRepeatedValidation() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(new JWKSet(publishedKey.toPublicJWK()).toString()));

    JwtDecoder decoder = jwtDecoder();

    Jwt firstJwt = decoder.decode(signedToken(publishedKey, "jwt-1"));
    Jwt secondJwt = decoder.decode(signedToken(publishedKey, "jwt-2"));

    assertThat(firstJwt.getIssuer().toString()).isEqualTo(SecurityConfig.MASKINPORTEN_ISSUER);
    assertThat(secondJwt.getClaimAsString("scope")).isEqualTo(SecurityConfig.MASKINPORTEN_SCOPE);
    assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
  }

  @Test
  void shouldRejectUnsignedToken() {
    JwtDecoder decoder = jwtDecoder();

    assertThatThrownBy(() -> decoder.decode(unsignedToken()))
        .isInstanceOf(JwtException.class)
        .hasMessageContaining("Unsupported algorithm");
  }

  @Test
  void shouldRejectLocalTestTokenOutsideTestProfile() {
    JwtDecoder decoder = jwtDecoder();

    assertThatThrownBy(() -> decoder.decode("local-test-token")).isInstanceOf(JwtException.class);
  }

  @Test
  void shouldRejectTokenSignedWithKeyNotPublishedByMaskinporten() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(new JWKSet(publishedKey.toPublicJWK()).toString()));

    RSAKey wrongSigningKey = new RSAKeyGenerator(2048).keyID(publishedKey.getKeyID()).generate();
    JwtDecoder decoder = jwtDecoder();

    assertThatThrownBy(() -> decoder.decode(signedToken(wrongSigningKey, "jwt-3")))
        .isInstanceOf(JwtException.class);
    assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
  }

  private JwtDecoder jwtDecoder() {
    Cache cache = securityConfig.maskinportenJwkSetCache(Duration.ofHours(24));
    return securityConfig.jwtDecoder(
        mockWebServer.url("/jwk").toString(),
        cache,
        securityConfig.maskinportenTokenValidator(SecurityConfig.MASKINPORTEN_SCOPE));
  }

  private String signedToken(RSAKey signingKey, String jwtId) throws Exception {
    JWTClaimsSet claims = baseClaims(jwtId);
    SignedJWT signedJwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(signingKey.getKeyID())
                .type(JOSEObjectType.JWT)
                .build(),
            claims);
    signedJwt.sign(new RSASSASigner(signingKey.toPrivateKey()));
    return signedJwt.serialize();
  }

  private String unsignedToken() {
    return new PlainJWT(baseClaims("plain-jwt")).serialize();
  }

  private JWTClaimsSet baseClaims(String jwtId) {
    Instant now = Instant.now();
    return new JWTClaimsSet.Builder()
        .issuer(SecurityConfig.MASKINPORTEN_ISSUER)
        .issueTime(Date.from(now))
        .expirationTime(Date.from(now.plusSeconds(600)))
        .jwtID(jwtId)
        .claim("client_id", "vigoskole-client")
        .claim("client_amr", "virksomhetssertifikat")
        .claim("scope", SecurityConfig.MASKINPORTEN_SCOPE)
        .claim("token_type", "Bearer")
        .claim("consumer", Map.of("authority", "iso6523-actorid-upis", "ID", "0192:974603268"))
        .build();
  }
}
