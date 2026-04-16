package no.fintlabs.vigoskole.domain.model;

import java.util.List;

public record ValidatedStudent(
    int lineNumber,
    StudentRecord student,
    PersonNumberStatus personNumberStatus,
    List<ValidationMessage> messages) {

  public boolean hasErrors() {
    return messages.stream()
        .anyMatch(message -> message.severity() == ValidationMessage.Severity.ERROR);
  }

  public boolean hasWarnings() {
    return messages.stream()
        .anyMatch(message -> message.severity() == ValidationMessage.Severity.WARNING);
  }

  public enum PersonNumberStatus {
    F_NUMBER("F-nr"),
    D_NUMBER("D-nr"),
    SYNTHETIC("Fiktivt nr"),
    INVALID("Ugyldig");

    private final String displayValue;

    PersonNumberStatus(String displayValue) {
      this.displayValue = displayValue;
    }

    public String displayValue() {
      return displayValue;
    }
  }
}
