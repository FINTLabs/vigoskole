package no.fintlabs.vigoskole.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import no.fintlabs.vigoskole.domain.model.SchoolInfo;
import no.fintlabs.vigoskole.domain.model.StudentRecord;
import no.fintlabs.vigoskole.domain.model.Submission;
import no.fintlabs.vigoskole.domain.model.SubmissionWindow;
import no.fintlabs.vigoskole.domain.model.ValidatedStudent;
import no.fintlabs.vigoskole.domain.model.ValidationMessage;
import no.fintlabs.vigoskole.domain.validation.PersonIdentityNumberValidator;
import org.springframework.stereotype.Service;

@Service
public class SubmissionService {

  private final SubmissionRepository submissionRepository;
  private final SubmissionWindowRepository submissionWindowRepository;
  private final SchoolDirectoryPort schoolDirectoryPort;
  private final PersonIdentityNumberValidator identityNumberValidator;
  private final Clock clock;

  public SubmissionService(
      SubmissionRepository submissionRepository,
      SubmissionWindowRepository submissionWindowRepository,
      SchoolDirectoryPort schoolDirectoryPort,
      PersonIdentityNumberValidator identityNumberValidator,
      Clock clock) {
    this.submissionRepository = submissionRepository;
    this.submissionWindowRepository = submissionWindowRepository;
    this.schoolDirectoryPort = schoolDirectoryPort;
    this.identityNumberValidator = identityNumberValidator;
    this.clock = clock;
  }

  public Submission submitGraduatingStudents(
      List<StudentRecord> students, SubmitterContext submitterContext) {
    SubmissionWindow submissionWindow = submissionWindowRepository.get();
    LocalDate today = LocalDate.now(clock);
    if (!submissionWindow.includes(today)) {
      throw new SubmissionWindowClosedException(
          "Innsending er kun tillatt innenfor konfigurert periode.");
    }
    if (students == null || students.isEmpty()) {
      Submission submission =
          rejectedSubmission(
              List.of(), submitterContext, verifySchool(submitterContext.orgNumber()), today);
      submissionRepository.save(submission);
      throw new SubmissionRejectedException(submission, "Innsendingen må inneholde minst én elev.");
    }

    SchoolInfo school = verifySchool(submitterContext.orgNumber());
    String schoolYear = currentSchoolYear(today);
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
        submitterContext.displayName(),
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
        submitterContext.displayName(),
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
                "Fiktivt fødselsnummer er tillatt, men markeres som advarsel."));
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
