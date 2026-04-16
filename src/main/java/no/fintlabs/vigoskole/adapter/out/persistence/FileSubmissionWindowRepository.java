package no.fintlabs.vigoskole.adapter.out.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import no.fintlabs.vigoskole.application.SubmissionWindowRepository;
import no.fintlabs.vigoskole.config.AppProperties;
import no.fintlabs.vigoskole.domain.model.SubmissionWindow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class FileSubmissionWindowRepository implements SubmissionWindowRepository {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
  private final Path filePath;
  private final AppProperties appProperties;

  public FileSubmissionWindowRepository(AppProperties appProperties) {
    this.appProperties = appProperties;
    this.filePath = appProperties.storageDirectory().resolve("submission-window.properties");
  }

  @Override
  public synchronized SubmissionWindow get() {
    if (!Files.exists(filePath)) {
      return defaultWindow();
    }
    Properties properties = new Properties();
    try (InputStream stream = Files.newInputStream(filePath)) {
      properties.load(stream);
    } catch (IOException exception) {
      return defaultWindow();
    }
    LocalDate from = parseDate(properties.getProperty("from"));
    LocalDate to = parseDate(properties.getProperty("to"));
    if (from == null || to == null) {
      return defaultWindow();
    }
    return new SubmissionWindow(from, to);
  }

  @Override
  public synchronized SubmissionWindow save(SubmissionWindow submissionWindow) {
    try {
      Files.createDirectories(filePath.getParent());
    } catch (IOException exception) {
      throw new IllegalStateException("Kunne ikke opprette lagringskatalog.", exception);
    }
    Properties properties = new Properties();
    properties.setProperty("from", FORMATTER.format(submissionWindow.from()));
    properties.setProperty("to", FORMATTER.format(submissionWindow.to()));
    try (OutputStream stream = Files.newOutputStream(filePath)) {
      properties.store(stream, "submission window");
    } catch (IOException exception) {
      throw new IllegalStateException("Kunne ikke lagre innsendingsperiode.", exception);
    }
    return submissionWindow;
  }

  private SubmissionWindow defaultWindow() {
    SubmissionWindow fallback =
        new SubmissionWindow(
            appProperties.submissionWindow().from(), appProperties.submissionWindow().to());
    save(fallback);
    return fallback;
  }

  private LocalDate parseDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value, FORMATTER);
    } catch (DateTimeParseException exception) {
      return null;
    }
  }
}
