package no.novari.vigoskole.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.novari.vigoskole.TestData;
import no.novari.vigoskole.domain.model.GraduatingStudentsSubmission;
import no.novari.vigoskole.domain.model.SchoolInfo;
import no.novari.vigoskole.domain.model.SchoolYearConfiguration;
import no.novari.vigoskole.domain.model.StudentRecord;
import no.novari.vigoskole.domain.model.Submission;
import no.novari.vigoskole.domain.validation.PersonIdentityNumberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmissionServiceTest {

  private InMemorySubmissionRepository submissionRepository;
  private InMemorySchoolYearRepository schoolYearRepository;
  private SubmissionService submissionService;

  @BeforeEach
  void setUp() {
    submissionRepository = new InMemorySubmissionRepository();
    schoolYearRepository = new InMemorySchoolYearRepository(TestData.schoolYearConfiguration());
    submissionService =
        new SubmissionService(
            submissionRepository,
            schoolYearRepository,
            schoolDirectoryPort(),
            new PersonIdentityNumberValidator(),
            Clock.fixed(Instant.parse("2026-04-16T10:15:30Z"), ZoneOffset.UTC));
  }

  @Test
  void shouldAcceptSubmissionWithWarnings() {
    Submission submission =
        submissionService.submitGraduatingStudents(
            TestData.syntheticGraduatingStudentsSubmission(),
            new SubmitterContext("999999999", "Ås kommune", null));

    assertThat(submission.status()).isEqualTo(Submission.Status.ACCEPTED_WITH_WARNINGS);
    assertThat(submission.validatedStudents().getFirst().messages()).hasSize(1);
    assertThat(submissionRepository.findAll()).hasSize(1);
    assertThat(submission.submittedBy()).isEqualTo("Ås kommune");
  }

  @Test
  void shouldRejectSubmissionWithValidationErrorsAndPersistAttempt() {
    StudentRecord invalidStudent =
        new StudentRecord("", "123", new StudentRecord.PersonName(null, null, null));

    assertThatThrownBy(
            () ->
                submissionService.submitGraduatingStudents(
                    new GraduatingStudentsSubmission(
                        TestData.SCHOOL_ORG_NUMBER, List.of(invalidStudent)),
                    new SubmitterContext("999999999", "Ås kommune", null)))
        .isInstanceOf(SubmissionRejectedException.class);

    assertThat(submissionRepository.findAll()).hasSize(1);
    assertThat(submissionRepository.findAll().getFirst().status())
        .isEqualTo(Submission.Status.REJECTED);
  }

  @Test
  void shouldRejectDuplicateAcceptedSubmissionForSameSchoolYear() {
    submissionRepository.save(TestData.submission(Submission.Status.ACCEPTED));

    assertThatThrownBy(
            () ->
                submissionService.submitGraduatingStudents(
                    TestData.validGraduatingStudentsSubmission(),
                    new SubmitterContext("999999999", "Ås kommune", null)))
        .isInstanceOf(DuplicateSubmissionException.class);
  }

  @Test
  void shouldRejectWhenSubmissionWindowIsClosed() {
    schoolYearRepository.save(
        new SchoolYearConfiguration(
            "2025-2026",
            new no.novari.vigoskole.domain.model.SubmissionWindows(
                new no.novari.vigoskole.domain.model.SubmissionWindow(
                    LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                TestData.submissionWindows().finalGrades(),
                TestData.submissionWindows().examGrades())));

    assertThatThrownBy(
            () ->
                submissionService.submitGraduatingStudents(
                    TestData.validGraduatingStudentsSubmission(),
                    new SubmitterContext("999999999", "Ås kommune", null)))
        .isInstanceOf(SubmissionWindowClosedException.class);
  }

  @Test
  void shouldRejectWhenSchoolYearIsNotCreated() {
    schoolYearRepository.delete("2025-2026");

    assertThatThrownBy(
            () ->
                submissionService.submitGraduatingStudents(
                    TestData.validGraduatingStudentsSubmission(),
                    new SubmitterContext("999999999", "Ås kommune", null)))
        .isInstanceOf(SchoolYearNotFoundException.class);
  }

  @Test
  void shouldRejectWhenSchoolOrgNumberIsMissingFromPayload() {
    assertThatThrownBy(
            () ->
                submissionService.submitGraduatingStudents(
                    new GraduatingStudentsSubmission(" ", List.of(TestData.validStudent())),
                    new SubmitterContext("999999999", "Ås kommune", null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("organisasjonsnummer");
  }

  private SchoolDirectoryPort schoolDirectoryPort() {
    return new SchoolDirectoryPort() {
      @Override
      public Optional<SchoolInfo> findLowerSecondarySchool(String orgNumber) {
        return Optional.of(TestData.schoolInfo());
      }

      @Override
      public Optional<String> findCountyShortName(String countyNumber) {
        return Optional.of("Akershus");
      }

      @Override
      public Optional<String> findMunicipalityName(String municipalityNumber) {
        return Optional.of("ÅS");
      }
    };
  }

  private static final class InMemorySubmissionRepository implements SubmissionRepository {

    private final List<Submission> submissions = new ArrayList<>();

    @Override
    public void save(Submission submission) {
      submissions.removeIf(existing -> existing.id().equals(submission.id()));
      submissions.add(submission);
    }

    @Override
    public boolean existsAcceptedSubmission(
        String schoolYear, String schoolOrgNumber, Submission.Type type) {
      return submissions.stream()
          .anyMatch(
              submission ->
                  submission.schoolYear().equals(schoolYear)
                      && submission.school().orgNumber().equals(schoolOrgNumber)
                      && submission.type() == type
                      && submission.status() != Submission.Status.REJECTED);
    }

    @Override
    public List<Submission> findAll() {
      return List.copyOf(submissions);
    }

    @Override
    public Optional<Submission> findById(UUID id) {
      return submissions.stream().filter(submission -> submission.id().equals(id)).findFirst();
    }

    @Override
    public void deleteById(UUID id) {
      submissions.removeIf(submission -> submission.id().equals(id));
    }

    @Override
    public void deleteBySchoolYear(String schoolYear) {
      submissions.removeIf(submission -> submission.schoolYear().equals(schoolYear));
    }

    @Override
    public void deleteBySchoolYearAndCountyNumber(String schoolYear, String countyNumber) {
      submissions.removeIf(
          submission ->
              submission.schoolYear().equals(schoolYear)
                  && countyNumber.equals(submission.school().countyNumber()));
    }

    @Override
    public void deleteBySchoolYearAndSchoolOrgNumber(String schoolYear, String schoolOrgNumber) {
      submissions.removeIf(
          submission ->
              submission.schoolYear().equals(schoolYear)
                  && schoolOrgNumber.equals(submission.school().orgNumber()));
    }
  }

  private static final class InMemorySchoolYearRepository implements SchoolYearRepository {

    private final List<SchoolYearConfiguration> schoolYears = new ArrayList<>();

    private InMemorySchoolYearRepository(SchoolYearConfiguration schoolYearConfiguration) {
      schoolYears.add(schoolYearConfiguration);
    }

    @Override
    public List<SchoolYearConfiguration> findAllSchoolYears() {
      return List.copyOf(schoolYears);
    }

    @Override
    public Optional<SchoolYearConfiguration> findBySchoolYear(String schoolYear) {
      return schoolYears.stream()
          .filter(configuration -> configuration.schoolYear().equals(schoolYear))
          .findFirst();
    }

    @Override
    public SchoolYearConfiguration save(SchoolYearConfiguration schoolYearConfiguration) {
      schoolYears.removeIf(
          existing -> existing.schoolYear().equals(schoolYearConfiguration.schoolYear()));
      schoolYears.add(schoolYearConfiguration);
      return schoolYearConfiguration;
    }

    @Override
    public void delete(String schoolYear) {
      schoolYears.removeIf(configuration -> configuration.schoolYear().equals(schoolYear));
    }
  }
}
