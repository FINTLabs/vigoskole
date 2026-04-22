package no.novari.vigoskole.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import no.novari.vigoskole.domain.model.GraduatingStudentsSubmission;
import no.novari.vigoskole.domain.model.SchoolInfo;
import no.novari.vigoskole.domain.model.SchoolYearConfiguration;
import no.novari.vigoskole.domain.model.StudentRecord;
import no.novari.vigoskole.domain.model.Submission;
import no.novari.vigoskole.domain.model.SubmissionWindow;
import no.novari.vigoskole.domain.model.ValidatedStudent;
import no.novari.vigoskole.domain.model.ValidationMessage;
import no.novari.vigoskole.domain.validation.PersonIdentityNumberValidator;
import org.springframework.stereotype.Service;

@Service
public class SubmissionService {

  private final SubmissionRepository submissionRepository;
  private final SchoolYearRepository schoolYearRepository;
  private final SchoolDirectoryPort schoolDirectoryPort;
  private final PersonIdentityNumberValidator identityNumberValidator;
  private final Clock clock;

  public SubmissionService(
      SubmissionRepository submissionRepository,
      SchoolYearRepository schoolYearRepository,
      SchoolDirectoryPort schoolDirectoryPort,
      PersonIdentityNumberValidator identityNumberValidator,
      Clock clock) {
    this.submissionRepository = submissionRepository;
    this.schoolYearRepository = schoolYearRepository;
    this.schoolDirectoryPort = schoolDirectoryPort;
    this.identityNumberValidator = identityNumberValidator;
    this.clock = clock;
  }

  public Submission submitGraduatingStudents(
      GraduatingStudentsSubmission request, SubmitterContext submitterContext) {
    LocalDate today = LocalDate.now(clock);
    String schoolYear = currentSchoolYear(today);
    SchoolYearConfiguration schoolYearConfiguration =
        schoolYearRepository
            .findBySchoolYear(schoolYear)
            .orElseThrow(
                () ->
                    new SchoolYearNotFoundException("Skoleåret er ikke opprettet for innsending."));
    SubmissionWindow submissionWindow =
        schoolYearConfiguration.submissionWindow(Submission.Type.GRADUATING_STUDENTS);
    if (!submissionWindow.includes(today)) {
      throw new SubmissionWindowClosedException(
          "Innsending er kun tillatt innenfor konfigurert periode.");
    }
    SchoolInfo school = verifySchool(requiredSchoolOrgNumber(request));
    List<StudentRecord> students = request == null ? List.of() : request.students();
    if (students.isEmpty()) {
      Submission submission = rejectedSubmission(List.of(), submitterContext, school, today);
      submissionRepository.save(submission);
      throw new SubmissionRejectedException(submission, "Innsendingen må inneholde minst én elev.");
    }

    if (submissionRepository.existsAcceptedSubmission(
        schoolYear, school.orgNumber(), Submission.Type.GRADUATING_STUDENTS)) {
      throw new DuplicateSubmissionException(
          "Det finnes allerede en godkjent innsending for dette skoleåret.");
    }

    List<ValidatedStudent> validatedStudents = validate(students);
    Submission submission =
        buildSubmission(validatedStudents, submitterContext, school, schoolYear);
    submissionRepository.save(submission);

    if (submission.status() == Submission.Status.REJECTED) {
      throw new SubmissionRejectedException(
          submission, "Innsendingen inneholder valideringsfeil og ble avvist.");
    }
    return submission;
  }

  private Submission rejectedSubmission(
      List<ValidatedStudent> validatedStudents,
      SubmitterContext submitterContext,
      SchoolInfo school,
      LocalDate today) {
    return new Submission(
        UUID.randomUUID(),
        Submission.Type.GRADUATING_STUDENTS,
        currentSchoolYear(today),
        Submission.Status.REJECTED,
        school,
        submitterContext.supplier(),
        submitterContext.submitterName(),
        Instant.now(clock),
        validatedStudents);
  }

  private SchoolInfo verifySchool(String orgNumber) {
    return schoolDirectoryPort
        .findLowerSecondarySchool(orgNumber)
        .orElseThrow(
            () ->
                new SchoolVerificationException(
                    "Organisasjonsnummeret tilhører ikke en ungdomsskole i VIGO Kodeverk."));
  }

  private String requiredSchoolOrgNumber(GraduatingStudentsSubmission request) {
    if (request == null
        || request.schoolOrgNumber() == null
        || request.schoolOrgNumber().isBlank()) {
      throw new IllegalArgumentException("Ungdomsskolens organisasjonsnummer er påkrevd.");
    }
    return request.schoolOrgNumber();
  }

  private Submission buildSubmission(
      List<ValidatedStudent> validatedStudents,
      SubmitterContext submitterContext,
      SchoolInfo school,
      String schoolYear) {
    Submission.Status status =
        validatedStudents.stream().anyMatch(ValidatedStudent::hasErrors)
            ? Submission.Status.REJECTED
            : validatedStudents.stream().anyMatch(ValidatedStudent::hasWarnings)
                ? Submission.Status.ACCEPTED_WITH_WARNINGS
                : Submission.Status.ACCEPTED;
    return new Submission(
        UUID.randomUUID(),
        Submission.Type.GRADUATING_STUDENTS,
        schoolYear,
        status,
        school,
        submitterContext.supplier(),
        submitterContext.submitterName(),
        Instant.now(clock),
        validatedStudents);
  }

  private List<ValidatedStudent> validate(List<StudentRecord> students) {
    List<ValidatedStudent> result = new ArrayList<>();
    for (int index = 0; index < students.size(); index++) {
      StudentRecord student = students.get(index);
      List<ValidationMessage> messages = new ArrayList<>();
      PersonIdentityNumberValidator.ValidationResult identityValidation =
          identityNumberValidator.validate(student.personalIdentityNumber());

      if (!identityValidation.valid()) {
        messages.add(
            new ValidationMessage(
                "personalIdentityNumber",
                "INVALID_IDENTITY_NUMBER",
                ValidationMessage.Severity.ERROR,
                "Fødselsnummer er ugyldig."));
      } else if (identityValidation.status() == ValidatedStudent.PersonNumberStatus.SYNTHETIC) {
        messages.add(
            new ValidationMessage(
                "personalIdentityNumber",
                "SYNTHETIC_IDENTITY_NUMBER",
                ValidationMessage.Severity.WARNING,
                "Sjekk om f-nr eller d-nr er tilgjengelig"));
      }

      if (student.classCode() == null || student.classCode().isBlank()) {
        messages.add(
            new ValidationMessage(
                "classCode",
                "MISSING_CLASS_CODE",
                ValidationMessage.Severity.ERROR,
                "Klassekode er påkrevd."));
      }

      if (student.name() == null || student.name().formatted().isBlank()) {
        messages.add(
            new ValidationMessage(
                "name", "MISSING_NAME", ValidationMessage.Severity.WARNING, "Elevnavn mangler."));
      }

      result.add(
          new ValidatedStudent(
              index + 1, student, identityValidation.status(), List.copyOf(messages)));
    }
    return List.copyOf(result);
  }

  static String currentSchoolYear(LocalDate date) {
    int startYear = date.getMonthValue() >= 8 ? date.getYear() : date.getYear() - 1;
    return startYear + "-" + (startYear + 1);
  }
}
