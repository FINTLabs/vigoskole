package no.novari.vigoskole.application;

import no.novari.vigoskole.domain.model.Submission;

public class SubmissionRejectedException extends RuntimeException {

  private final Submission submission;

  public SubmissionRejectedException(Submission submission, String message) {
    super(message);
    this.submission = submission;
  }

  public Submission submission() {
    return submission;
  }
}
