package no.fintlabs.vigoskole.domain.model;

import java.time.LocalDate;

public record SubmissionWindow(LocalDate from, LocalDate to) {

  public boolean includes(LocalDate date) {
    return !date.isBefore(from) && !date.isAfter(to);
  }
}
