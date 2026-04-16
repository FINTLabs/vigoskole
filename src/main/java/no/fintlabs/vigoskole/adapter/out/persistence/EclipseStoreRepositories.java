package no.fintlabs.vigoskole.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.fintlabs.vigoskole.application.SubmissionRepository;
import no.fintlabs.vigoskole.domain.model.Submission;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class EclipseStoreRepositories implements SubmissionRepository {

  private final EmbeddedStorageManager storageManager;
  private final EclipseStoreState state;

  public EclipseStoreRepositories(EmbeddedStorageManager storageManager, EclipseStoreState state) {
    this.storageManager = storageManager;
    this.state = state;
  }

  @Override
  public synchronized Submission save(Submission submission) {
    EclipseStoreState currentState = currentState();
    currentState.submissions().removeIf(existing -> existing.id().equals(submission.id()));
    currentState.submissions().add(submission);
    storageManager.store(currentState.submissions());
    return submission;
  }

  @Override
  public synchronized boolean existsAcceptedSubmission(
      String schoolYear, String schoolOrgNumber, Submission.Type type) {
    return currentState().submissions().stream()
        .anyMatch(
            submission ->
                submission.schoolYear().equals(schoolYear)
                    && submission.school().orgNumber().equals(schoolOrgNumber)
                    && submission.type() == type
                    && submission.status() != Submission.Status.REJECTED);
  }

  @Override
  public synchronized List<Submission> findAll() {
    return List.copyOf(currentState().submissions());
  }

  @Override
  public synchronized Optional<Submission> findById(UUID id) {
    return currentState().submissions().stream()
        .filter(submission -> submission.id().equals(id))
        .findFirst();
  }

  private EclipseStoreState currentState() {
    Object root = storageManager.root();
    if (root instanceof EclipseStoreState loadedState) {
      return loadedState;
    }
    storageManager.setRoot(state);
    return state;
  }
}
