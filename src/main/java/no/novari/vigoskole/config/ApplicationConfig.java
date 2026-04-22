package no.novari.vigoskole.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.net.http.HttpClient;
import java.time.Clock;
import no.novari.vigoskole.domain.validation.PersonIdentityNumberValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
@OpenAPIDefinition(
    info =
        @Info(
            title = "Vigo Skole API",
            description = "API for innsending og inspeksjon av avgangselever.",
            version = "v1"))
@SecurityScheme(
    name = "maskinporten-jwt",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class ApplicationConfig {

  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }

  @Bean
  PersonIdentityNumberValidator personIdentityNumberValidator() {
    return new PersonIdentityNumberValidator();
  }

  @Bean
  HttpClient httpClient() {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  }
}
