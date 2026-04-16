package no.fintlabs.vigoskole.config;

import no.fintlabs.vigoskole.application.SubmitterContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("dev")
public class DevSecurityConfig {

  @Bean
  SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .csrf(csrf -> csrf.disable());
    return http.build();
  }

  @Bean
  SecurityConfig.JwtSubmitterContextResolver jwtSubmitterContextResolver() {
    return new DevJwtSubmitterContextResolver();
  }

  static final class DevJwtSubmitterContextResolver
      extends SecurityConfig.JwtSubmitterContextResolver {

    @Override
    public SubmitterContext resolve(@AuthenticationPrincipal Jwt jwt) {
      if (jwt == null) {
        return new SubmitterContext("000000000", "Dev user", null);
      }
      return super.resolve(jwt);
    }
  }
}
