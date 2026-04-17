package no.novari.vigoskole.config;

import java.net.http.HttpClient;
import java.time.Clock;
import no.novari.vigoskole.domain.validation.PersonIdentityNumberValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
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
