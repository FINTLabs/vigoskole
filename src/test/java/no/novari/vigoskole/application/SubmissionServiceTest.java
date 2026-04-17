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
import no.novari.vigoskole.domain.model.StudentRecord;
import no.novari.vigoskole.domain.model.Submission;
import no.novari.vigoskole.domain.model.SubmissionWindow;
import no.novari.vigoskole.domain.validation.PersonIdentityNumberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmissionServiceTest {

  private InMemorySubmissionRepository submissionRepository;
  private InMemorySubmissionWindowRepository submissionWindowRepository;
  private SubmissionService submissionService;

  @BeforeEach
  void setUp() {
    submissionRepository = new InMemorySubmissionRepository();
    submissionWindowRepository =
        new InMemorySubmissionWindowRepository(
            new SubmissionWindow(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
    submissionService =
        new SubmissionService(
            submissionRepository,
            submissionWindowRepository,
            orgNumber -> Optional.of(TestData.schoolInfo()),
            new PersonIdentityNumberValidator(),
            Clock.fixed(Instant.parse("2026-04-16T10:15:30Z"), ZoneOffset.UTC));
  }

  @Test
  void shouldAcceptSubmissionWithWarnings() {
    Submission submission =
        submissionService.submitGraduatingStudents(
            List.of(TestData.syntheticStudent()),
            new SubmitterContext(TestData.SCHOOL_ORG_NUMBER, "Test ungdomsskole", null));

    assertThat(submission.status()).isEqualTo(Submission.Status.ACCEPTED_WITH_WARNINGS);
    assertThat(submission.validatedStudents().getFirst().messages()).hasSize(1);
    assertThat(submissionRepository.findAll()).hasSize(1);
  }

  @Test
  void shouldRejectSubmissionWithValidationErrorsAndPersistAttempt() {
    StudentRecord invalidStudent =
        new StudentRecord("", "123", new StudentRecord.PersonName(null, null, null));

    assertThatThrownBy(
            () ->
                submissionService.submitGraduatingStudents(
                    List.of(invalidStudent),
                    new SubmitterContext(TestData.SCHOOL_ORG_NUMBER, "Test ungdomsskole", null)))
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
                    List.of(TestData.validStudent()),
                    new SubmitterContext(TestData.SCHOOL_ORG_NUMBER, "Test ungdomsskole", null)))
        .isInstanceOf(DuplicateSubmissionException.class);
  }

  @Test
  void shouldRejectWhenSubmissionWindowIsClosed() {
    submissionService =
        new SubmissionService(
            submissionRepository,
            new InMemorySubmissionWindowRepository(
                new SubmissionWindow(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))),
            orgNumber -> Optional.of(TestData.schoolInfo()),
            new PersonIdentityNumberValidator(),
            Clock.fixed(Instant.parse("2026-04-16T10:15:30Z"), ZoneOffset.UTC));

    assertThatThrownBy(
            () ->
                submissionService.submitGraduatingStudents(
                    List.of(TestData.validStudent()),
                    new SubmitterContext(TestData.SCHOOL_ORG_NUMBER, "Test ungdomsskole", null)))
        .isInstanceOf(SubmissionWindowClosedException.class);
  }

  private static final class InMemorySubmissionRepository implements SubmissionRepository {

    private final List<Submission> submissions = new ArrayList<>();

    @Override
    public Submission save(Submission submission) {
      submissions.removeIf(existing -> existing.id().equals(submission.id()));
      submissions.add(submission);
      return submission;
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
  }

  private record InMemorySubmissionWindowRepository(SubmissionWindow submissionWindow)
      implements SubmissionWindowRepository {

    @Override
    public SubmissionWindow get() {
      return submissionWindow;
    }

    @Override
    public SubmissionWindow save(SubmissionWindow submissionWindow) {
      return submissionWindow;
    }
  }
}
