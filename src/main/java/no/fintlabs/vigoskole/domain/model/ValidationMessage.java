package no.fintlabs.vigoskole.domain.model;

public record ValidationMessage(String field, String code, Severity severity, String message) {

  public enum Severity {
    ERROR,
    WARNING
  }
}
