package no.novari.vigoskole.application;

public class DuplicateSubmissionException extends RuntimeException {

  public DuplicateSubmissionException(String message) {
    super(message);
  }
}
