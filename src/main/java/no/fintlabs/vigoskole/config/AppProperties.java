package no.fintlabs.vigoskole.config;

import java.nio.file.Path;
import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Path storageDirectory,
    SubmissionWindowProperties submissionWindow,
    VigoKodeverkProperties vigoKodeverk) {

  public record SubmissionWindowProperties(LocalDate from, LocalDate to) {}

  public record VigoKodeverkProperties(String baseUrl) {}
}
