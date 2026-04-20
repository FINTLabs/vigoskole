package no.novari.vigoskole.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import no.novari.vigoskole.application.SubmitterContext;
import no.novari.vigoskole.domain.model.SupplierInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!dev")
public class SecurityConfig {

  public static final String MASKINPORTEN_ISSUER = "https://maskinporten.no/";
  public static final String MASKINPORTEN_SCOPE = "novari:fakevigoskole";

  @Bean
  @Order(1)
  SecurityFilterChain apiSecurityFilterChain(
      HttpSecurity http,
      @Value("${app.maskinporten.expected-scope:" + MASKINPORTEN_SCOPE + "}") String expectedScope)
      throws Exception {
    http.securityMatcher("/api/**")
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(HttpMethod.POST, "/api/**")
                    .hasAuthority("SCOPE_" + expectedScope)
                    .anyRequest()
                    .denyAll())
        .csrf(AbstractHttpConfigurer::disable)
        .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny));
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/actuator/health", "/app.css", "/", "/login")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(oauth2Login -> oauth2Login.loginPage("/login"))
        .logout(logout -> logout.logoutSuccessUrl("/"))
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny))
        .addFilterAfter(
            new ContentSecurityPolicyFilter(),
            org.springframework.security.web.context.SecurityContextHolderFilter.class);
    return http.build();
  }

  @Bean
  JwtSubmitterContextResolver jwtSubmitterContextResolver() {
    return new JwtSubmitterContextResolver();
  }

  @Bean
  OAuth2TokenValidator<Jwt> maskinportenTokenValidator(
      @Value("${app.maskinporten.expected-scope:" + MASKINPORTEN_SCOPE + "}")
          String expectedScope) {
    OAuth2TokenValidator<Jwt> issuerValidator =
        JwtValidators.createDefaultWithIssuer(MASKINPORTEN_ISSUER);
    OAuth2TokenValidator<Jwt> claimsValidator =
        jwt -> {
          ValidationErrors errors = new ValidationErrors();
          requireNonBlank(jwt, "client_id", errors);
          requireNonBlankOrCollection(jwt, "client_amr", errors);
          requireNonBlankOrCollection(jwt, "scope", errors);
          requireNonBlank(jwt, "jti", errors);
          requireEquals(jwt, "token_type", "Bearer", errors);
          requireScope(jwt, expectedScope, errors);
          requireOrganizationClaim(jwt, "consumer", errors);
          rejectIfPresent(
              jwt, "sub", "Skyporten-claimet 'sub' støttes ikke for dette API-et.", errors);
          rejectIfPresent(
              jwt,
              "pid",
              "Endebruker-bundne Maskinporten-tokens støttes ikke for dette API-et.",
              errors);
          if (jwt.getClaims().containsKey("supplier")) {
            requireOrganizationClaim(jwt, "supplier", errors);
            requireNonBlank(jwt, "delegation_source", errors);
          }
          return errors.result();
        };
    return jwt -> {
      OAuth2TokenValidatorResult issuerResult = issuerValidator.validate(jwt);
      return issuerResult.hasErrors() ? issuerResult : claimsValidator.validate(jwt);
    };
  }

  @Bean
  Cache maskinportenJwkSetCache(
      @Value("${app.maskinporten.jwk-cache-duration:PT24H}") Duration cacheDuration) {
    return new ExpiringMapCache("maskinporten-jwk-set-cache", cacheDuration);
  }

  @Bean
  @ConditionalOnMissingBean(JwtDecoder.class)
  JwtDecoder jwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
      Cache maskinportenJwkSetCache,
      OAuth2TokenValidator<Jwt> maskinportenTokenValidator) {
    NimbusJwtDecoder jwtDecoder =
        NimbusJwtDecoder.withJwkSetUri(jwkSetUri).cache(maskinportenJwkSetCache).build();
    jwtDecoder.setJwtValidator(maskinportenTokenValidator);
    return jwtDecoder;
  }

  @Bean
  @ConditionalOnMissingBean(ClientRegistrationRepository.class)
  ClientRegistrationRepository clientRegistrationRepository() {
    ClientRegistration clientRegistration =
        ClientRegistration.withRegistrationId("idporten")
            .clientId("vigoskole")
            .clientSecret("change-me")
            .authorizationGrantType(
                org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile")
            .authorizationUri("https://example.invalid/authorize")
            .tokenUri("https://example.invalid/token")
            .jwkSetUri("https://example.invalid/jwks")
            .userInfoUri("https://example.invalid/userinfo")
            .userNameAttributeName("sub")
            .clientName("ID-porten")
            .build();
    return new InMemoryClientRegistrationRepository(clientRegistration);
  }

  @Bean
  @ConditionalOnMissingBean(OAuth2AuthorizedClientService.class)
  OAuth2AuthorizedClientService authorizedClientService(
      ClientRegistrationRepository clientRegistrationRepository) {
    return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
  }

  public static class JwtSubmitterContextResolver {

    public SubmitterContext resolve(@AuthenticationPrincipal Jwt jwt) {
      if (jwt == null) {
        throw new IllegalArgumentException("Mangler Maskinporten-token.");
      }
      String orgNumber =
          firstNonBlank(
              consumerOrgNumber(jwt),
              claim(jwt, "org_number", "orgnr", "consumer_org_number", "organization_number"));
      if (orgNumber == null || orgNumber.isBlank()) {
        throw new IllegalArgumentException("Maskinporten-token mangler organisasjonsnummer.");
      }
      String displayName =
          firstNonBlank(claim(jwt, "consumer_name", "organization_name", "client_name"), orgNumber);
      String supplierOrg =
          firstNonBlank(
              organizationClaimValue(jwt, "supplier"),
              claim(jwt, "supplier_org_number", "supplier_orgnr"));
      String supplierName = firstNonBlank(claim(jwt, "supplier_name"), supplierOrg);
      SupplierInfo supplier =
          supplierOrg == null && supplierName == null
              ? null
              : new SupplierInfo(supplierOrg, supplierName);
      return new SubmitterContext(orgNumber, displayName, supplier);
    }

    private String consumerOrgNumber(Jwt jwt) {
      return organizationClaimValue(jwt, "consumer");
    }

    private String organizationClaimValue(Jwt jwt, String claimName) {
      Object consumer = jwt.getClaims().get(claimName);
      if (!(consumer instanceof Map<?, ?> consumerClaims)) {
        return null;
      }
      Object id = consumerClaims.get("ID");
      if (!(id instanceof String consumerId) || consumerId.isBlank()) {
        return null;
      }
      int separatorIndex = consumerId.lastIndexOf(':');
      return separatorIndex >= 0 ? consumerId.substring(separatorIndex + 1) : consumerId;
    }

    private String claim(Jwt jwt, String... names) {
      for (String name : names) {
        Object value = jwt.getClaims().get(name);
        if (value instanceof String string && !string.isBlank()) {
          return string;
        }
      }
      return null;
    }

    private String firstNonBlank(String first, String fallback) {
      return first != null && !first.isBlank() ? first : fallback;
    }
  }

  private static void requireNonBlank(Jwt jwt, String claimName, ValidationErrors errors) {
    Object value = jwt.getClaims().get(claimName);
    if (!(value instanceof String string) || string.isBlank()) {
      errors.add(claimName, "Maskinporten-token mangler claimet '" + claimName + "'.");
    }
  }

  private static void requireNonBlankOrCollection(
      Jwt jwt, String claimName, ValidationErrors errors) {
    Object value = jwt.getClaims().get(claimName);
    if (value instanceof String string && !string.isBlank()) {
      return;
    }
    if (value instanceof Collection<?> collection && !collection.isEmpty()) {
      return;
    }
    errors.add(claimName, "Maskinporten-token mangler claimet '" + claimName + "'.");
  }

  private static void requireEquals(
      Jwt jwt, String claimName, String expectedValue, ValidationErrors errors) {
    Object value = jwt.getClaims().get(claimName);
    if (!(value instanceof String string) || !expectedValue.equals(string)) {
      errors.add(claimName, "Maskinporten-token må ha '" + claimName + "=" + expectedValue + "'.");
    }
  }

  private static void requireOrganizationClaim(Jwt jwt, String claimName, ValidationErrors errors) {
    Object value = jwt.getClaims().get(claimName);
    if (!(value instanceof Map<?, ?> organizationClaim)) {
      errors.add(claimName, "Maskinporten-token mangler gyldig claim '" + claimName + "'.");
      return;
    }
    Object authority = organizationClaim.get("authority");
    Object id = organizationClaim.get("ID");
    if (!(authority instanceof String authorityString) || authorityString.isBlank()) {
      errors.add(claimName, "Maskinporten-token mangler authority i claim '" + claimName + "'.");
    }
    if (!(id instanceof String idString) || idString.isBlank()) {
      errors.add(claimName, "Maskinporten-token mangler ID i claim '" + claimName + "'.");
    }
  }

  private static void requireScope(Jwt jwt, String expectedScope, ValidationErrors errors) {
    Object scope = jwt.getClaims().get("scope");
    if (scope instanceof String scopeString) {
      boolean matches =
          java.util.Arrays.stream(scopeString.split("\\s+")).anyMatch(expectedScope::equals);
      if (matches) {
        return;
      }
    }
    if (scope instanceof Collection<?> scopes
        && scopes.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .anyMatch(expectedScope::equals)) {
      return;
    }
    errors.add("scope", "Maskinporten-token mangler forventet scope '" + expectedScope + "'.");
  }

  private static void rejectIfPresent(
      Jwt jwt, String claimName, String message, ValidationErrors errors) {
    Object value = jwt.getClaims().get(claimName);
    if (value == null) {
      return;
    }
    if (value instanceof String string && string.isBlank()) {
      return;
    }
    errors.add(claimName, message);
  }

  private static final class ValidationErrors {

    private final java.util.List<OAuth2Error> errors = new java.util.ArrayList<>();

    void add(String claimName, String description) {
      errors.add(new OAuth2Error("invalid_token", description, claimName));
    }

    OAuth2TokenValidatorResult result() {
      return errors.isEmpty()
          ? OAuth2TokenValidatorResult.success()
          : OAuth2TokenValidatorResult.failure(errors);
    }
  }

  static final class ExpiringMapCache implements Cache {

    private final String name;
    private final Duration ttl;
    private final ConcurrentMap<Object, CacheEntry> store = new ConcurrentHashMap<>();

    ExpiringMapCache(String name, Duration ttl) {
      this.name = name;
      this.ttl = ttl;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Object getNativeCache() {
      return store;
    }

    @Override
    public ValueWrapper get(Object key) {
      CacheEntry entry = getValidEntry(key);
      return entry == null ? null : () -> entry.value();
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
      CacheEntry entry = getValidEntry(key);
      if (entry == null) {
        return null;
      }
      Object value = entry.value();
      return type == null || type.isInstance(value) ? type.cast(value) : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
      CacheEntry entry = getValidEntry(key);
      if (entry != null) {
        return (T) entry.value();
      }
      try {
        T value = valueLoader.call();
        put(key, value);
        return value;
      } catch (Exception exception) {
        throw new IllegalStateException("Kunne ikke laste verdi til cache.", exception);
      }
    }

    @Override
    public void put(Object key, Object value) {
      if (value == null) {
        evict(key);
        return;
      }
      store.put(key, new CacheEntry(value, Instant.now().plus(ttl)));
    }

    @Override
    public void evict(Object key) {
      store.remove(key);
    }

    @Override
    public void clear() {
      store.clear();
    }

    private CacheEntry getValidEntry(Object key) {
      CacheEntry entry = store.get(key);
      if (entry == null) {
        return null;
      }
      if (entry.expiresAt().isAfter(Instant.now())) {
        return entry;
      }
      store.remove(key, entry);
      return null;
    }

    private record CacheEntry(Object value, Instant expiresAt) {}
  }
}
