package no.novari.vigoskole.domain.model;

public record SubmissionWindows(
    SubmissionWindow graduatingStudents,
    SubmissionWindow finalGrades,
    SubmissionWindow examGrades) {

  public SubmissionWindow windowFor(Submission.Type type) {
    return switch (type) {
      case GRADUATING_STUDENTS -> graduatingStudents;
      case FINAL_GRADES -> finalGrades;
      case EXAM_GRADES -> examGrades;
    };
  }
}
