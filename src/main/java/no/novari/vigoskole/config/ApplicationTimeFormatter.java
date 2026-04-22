package no.novari.vigoskole.config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component("applicationTimeFormatter")
public class ApplicationTimeFormatter {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final ZoneId zoneId;

  public ApplicationTimeFormatter(ZoneId zoneId) {
    this.zoneId = zoneId;
  }

  public String format(Instant instant) {
    return DATE_TIME_FORMATTER.format(instant.atZone(zoneId));
  }

  public LocalDate toLocalDate(Instant instant) {
    return instant.atZone(zoneId).toLocalDate();
  }
}
