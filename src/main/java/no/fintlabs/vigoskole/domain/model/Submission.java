package no.fintlabs.vigoskole.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Submission(
    UUID id,
    Type type,
    String schoolYear,
    Status status,
    SchoolInfo school,
    SupplierInfo supplier,
    String submittedBy,
    Instant submittedAt,
    List<ValidatedStudent> validatedStudents) {

  public boolean hasErrors() {
    return validatedStudents.stream().anyMatch(ValidatedStudent::hasErrors);
  }

  public boolean hasWarnings() {
    return validatedStudents.stream().anyMatch(ValidatedStudent::hasWarnings);
  }

  public enum Type {
    GRADUATING_STUDENTS
  }

  public enum Status {
    ACCEPTED,
    ACCEPTED_WITH_WARNINGS,
    REJECTED
  }
}
