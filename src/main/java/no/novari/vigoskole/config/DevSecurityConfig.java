package no.novari.vigoskole.config;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import no.novari.vigoskole.application.SubmitterContext;
import no.novari.vigoskole.domain.model.SupplierInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("dev")
public class DevSecurityConfig {

  @Bean
  @Order(1)
  SecurityFilterChain devApiSecurityFilterChain(
      HttpSecurity http,
      @Value("${app.maskinporten.expected-scope:" + SecurityConfig.MASKINPORTEN_SCOPE + "}")
          String expectedScope)
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
        .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain devWebSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/actuator/health", "/app.css", "/", "/login")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(formLogin -> formLogin.defaultSuccessUrl("/ui/school-years", true))
        .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
    return http.build();
  }

  @Bean
  JwtDecoder devProfileJwtDecoder(
      @Value("${app.maskinporten.expected-scope:" + SecurityConfig.MASKINPORTEN_SCOPE + "}")
          String expectedScope,
      @Value("${app.maskinporten.test-token.value:local-test-token}") String expectedTokenValue,
      @Value("${app.maskinporten.test-token.consumer-org-number:974603268}")
          String consumerOrgNumber,
      @Value("${app.maskinporten.test-token.consumer-name:Ås ungdomsskole}") String consumerName,
      @Value("${app.maskinporten.test-token.supplier-org-number:}") String supplierOrgNumber,
      @Value("${app.maskinporten.test-token.supplier-name:}") String supplierName,
      @Value("${app.maskinporten.test-token.delegation-source:https://www.altinn.no}")
          String delegationSource) {
    return token -> {
      if (!expectedTokenValue.equals(token)) {
        throw new BadJwtException("Ugyldig dev-token.");
      }
      return createJwt(
          token,
          expectedScope,
          consumerOrgNumber,
          consumerName,
          supplierOrgNumber,
          supplierName,
          delegationSource);
    };
  }

  @Bean
  UserDetailsService devUserDetailsService(
      @Value("${app.dev.user.username:dev}") String username,
      @Value("${app.dev.user.password:devpass}") String password,
      PasswordEncoder passwordEncoder) {
    return new InMemoryUserDetailsManager(
        User.withUsername(username)
            .password(passwordEncoder.encode(password))
            .roles("USER")
            .build());
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  SecurityConfig.JwtSubmitterContextResolver jwtSubmitterContextResolver(
      @Value("${app.maskinporten.test-token.consumer-org-number:974603268}")
          String consumerOrgNumber,
      @Value("${app.maskinporten.test-token.consumer-name:Ås ungdomsskole}") String consumerName,
      @Value("${app.maskinporten.test-token.supplier-org-number:}") String supplierOrgNumber,
      @Value("${app.maskinporten.test-token.supplier-name:}") String supplierName) {
    return new SecurityConfig.JwtSubmitterContextResolver() {
      @Override
      public SubmitterContext resolve(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
          SupplierInfo supplier =
              supplierOrgNumber.isBlank() && supplierName.isBlank()
                  ? null
                  : new SupplierInfo(
                      supplierOrgNumber, supplierName.isBlank() ? supplierOrgNumber : supplierName);
          return new SubmitterContext(consumerOrgNumber, consumerName, supplier);
        }
        return super.resolve(jwt);
      }
    };
  }

  private Jwt createJwt(
      String token,
      String expectedScope,
      String consumerOrgNumber,
      String consumerName,
      String supplierOrgNumber,
      String supplierName,
      String delegationSource) {
    Instant issuedAt = Instant.now();
    Map<String, Object> claims = new HashMap<>();
    claims.put("iss", SecurityConfig.MASKINPORTEN_ISSUER);
    claims.put("client_id", "dev-local-client");
    claims.put("client_amr", "virksomhetssertifikat");
    claims.put("scope", expectedScope);
    claims.put("token_type", "Bearer");
    claims.put("jti", "dev-" + issuedAt.toEpochMilli());
    claims.put("consumer", organizationClaim(consumerOrgNumber));
    claims.put("consumer_name", consumerName);
    if (!supplierOrgNumber.isBlank()) {
      claims.put("supplier", organizationClaim(supplierOrgNumber));
      claims.put("supplier_name", supplierName.isBlank() ? supplierOrgNumber : supplierName);
      claims.put("delegation_source", delegationSource);
    }
    return new Jwt(
        token, issuedAt, issuedAt.plusSeconds(3600), Map.of("alg", "none", "typ", "JWT"), claims);
  }

  private Map<String, Object> organizationClaim(String orgNumber) {
    return Map.of("authority", "iso6523-actorid-upis", "ID", "0192:" + orgNumber);
  }
}
