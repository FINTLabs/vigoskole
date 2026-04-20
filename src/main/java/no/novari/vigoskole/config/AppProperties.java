package no.novari.vigoskole.config;

import java.nio.file.Path;
import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Path storageDirectory,
    SubmissionWindowsProperties submissionWindows,
    VigoKodeverkProperties vigoKodeverk) {

  public record SubmissionWindowsProperties(
      SubmissionWindowProperties graduatingStudents,
      SubmissionWindowProperties finalGrades,
      SubmissionWindowProperties examGrades) {}

  public record SubmissionWindowProperties(LocalDate from, LocalDate to) {}

  public record VigoKodeverkProperties(String baseUrl) {}
}
