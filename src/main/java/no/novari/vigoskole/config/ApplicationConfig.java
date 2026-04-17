package no.novari.vigoskole.config;

import java.time.Clock;
import no.novari.vigoskole.domain.validation.PersonIdentityNumberValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

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
  RestClient vigoKodeverkRestClient(AppProperties appProperties) {
    return RestClient.builder().baseUrl(appProperties.vigoKodeverk().baseUrl()).build();
  }
}
