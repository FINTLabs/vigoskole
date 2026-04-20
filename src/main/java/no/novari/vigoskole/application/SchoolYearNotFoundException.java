package no.novari.vigoskole.application;

public class SchoolYearNotFoundException extends RuntimeException {

  public SchoolYearNotFoundException(String message) {
    super(message);
  }
}
