package no.fintlabs.vigoskole.config;

import no.fintlabs.vigoskole.application.SubmitterContext;
import no.fintlabs.vigoskole.domain.model.SupplierInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!dev")
public class SecurityConfig {

  @Bean
  @Order(1)
  SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**")
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(HttpMethod.POST, "/api/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .csrf(csrf -> csrf.disable())
        .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/actuator/health", "/app.css", "/")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(Customizer.withDefaults())
        .logout(logout -> logout.logoutSuccessUrl("/"));
    return http.build();
  }

  @Bean
  JwtSubmitterContextResolver jwtSubmitterContextResolver() {
    return new JwtSubmitterContextResolver();
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
          claim(jwt, "org_number", "orgnr", "consumer_org_number", "organization_number");
      if (orgNumber == null || orgNumber.isBlank()) {
        throw new IllegalArgumentException("Maskinporten-token mangler organisasjonsnummer.");
      }
      String displayName =
          firstNonBlank(claim(jwt, "consumer_name", "organization_name", "client_name"), orgNumber);
      String supplierOrg = claim(jwt, "supplier_org_number", "supplier_orgnr");
      String supplierName = claim(jwt, "supplier_name");
      SupplierInfo supplier =
          supplierOrg == null && supplierName == null
              ? null
              : new SupplierInfo(supplierOrg, supplierName);
      return new SubmitterContext(orgNumber, displayName, supplier);
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
}
