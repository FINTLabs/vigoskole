package no.novari.vigoskole.domain.model;

public record SchoolYearConfiguration(String schoolYear, SubmissionWindows submissionWindows) {

  public SubmissionWindow submissionWindow(Submission.Type type) {
    return submissionWindows.windowFor(type);
  }
}
