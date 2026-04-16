package no.fintlabs.vigoskole.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.fintlabs.vigoskole.application.SubmissionRepository;
import no.fintlabs.vigoskole.application.SubmissionWindowRepository;
import no.fintlabs.vigoskole.config.AppProperties;
import no.fintlabs.vigoskole.domain.model.Submission;
import no.fintlabs.vigoskole.domain.model.SubmissionWindow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("test")
public class InMemoryRepositories implements SubmissionRepository, SubmissionWindowRepository {

  private final List<Submission> submissions = new ArrayList<>();
  private SubmissionWindow submissionWindow;

  public InMemoryRepositories(AppProperties appProperties) {
    this.submissionWindow =
        new SubmissionWindow(
            appProperties.submissionWindow().from(), appProperties.submissionWindow().to());
  }

  @Override
  public synchronized Submission save(Submission submission) {
    submissions.removeIf(existing -> existing.id().equals(submission.id()));
    submissions.add(submission);
    return submission;
  }

  @Override
  public synchronized boolean existsAcceptedSubmission(
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
  public synchronized List<Submission> findAll() {
    return List.copyOf(submissions);
  }

  @Override
  public synchronized Optional<Submission> findById(UUID id) {
    return submissions.stream().filter(submission -> submission.id().equals(id)).findFirst();
  }

  @Override
  public synchronized SubmissionWindow get() {
    return submissionWindow;
  }

  @Override
  public synchronized SubmissionWindow save(SubmissionWindow submissionWindow) {
    this.submissionWindow = submissionWindow;
    return submissionWindow;
  }
}
