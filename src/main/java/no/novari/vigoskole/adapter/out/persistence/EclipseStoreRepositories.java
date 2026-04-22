package no.novari.vigoskole.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.novari.vigoskole.application.SchoolYearRepository;
import no.novari.vigoskole.application.SubmissionRepository;
import no.novari.vigoskole.domain.model.SchoolYearConfiguration;
import no.novari.vigoskole.domain.model.Submission;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class EclipseStoreRepositories implements SubmissionRepository, SchoolYearRepository {

  private final EmbeddedStorageManager storageManager;
  private final EclipseStoreState state;

  public EclipseStoreRepositories(EmbeddedStorageManager storageManager, EclipseStoreState state) {
    this.storageManager = storageManager;
    this.state = state;
  }

  @Override
  public synchronized void save(Submission submission) {
    EclipseStoreState currentState = currentState();
    currentState.submissions().removeIf(existing -> existing.id().equals(submission.id()));
    currentState.submissions().add(submission);
    storageManager.store(currentState.submissions());
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

  @Override
  public synchronized void deleteById(UUID id) {
    EclipseStoreState currentState = currentState();
    if (currentState.submissions().removeIf(submission -> submission.id().equals(id))) {
      storageManager.store(currentState.submissions());
    }
  }

  @Override
  public synchronized void deleteBySchoolYear(String schoolYear) {
    deleteMatching(submission -> submission.schoolYear().equals(schoolYear));
  }

  @Override
  public synchronized void deleteBySchoolYearAndCountyNumber(
      String schoolYear, String countyNumber) {
    deleteMatching(
        submission ->
            submission.schoolYear().equals(schoolYear)
                && countyNumber.equals(submission.school().countyNumber()));
  }

  @Override
  public synchronized void deleteBySchoolYearAndSchoolOrgNumber(
      String schoolYear, String schoolOrgNumber) {
    deleteMatching(
        submission ->
            submission.schoolYear().equals(schoolYear)
                && schoolOrgNumber.equals(submission.school().orgNumber()));
  }

  @Override
  public synchronized List<SchoolYearConfiguration> findAllSchoolYears() {
    return List.copyOf(currentState().schoolYears());
  }

  @Override
  public synchronized Optional<SchoolYearConfiguration> findBySchoolYear(String schoolYear) {
    return currentState().schoolYears().stream()
        .filter(configuration -> configuration.schoolYear().equals(schoolYear))
        .findFirst();
  }

  @Override
  public synchronized SchoolYearConfiguration save(
      SchoolYearConfiguration schoolYearConfiguration) {
    EclipseStoreState currentState = currentState();
    currentState
        .schoolYears()
        .removeIf(existing -> existing.schoolYear().equals(schoolYearConfiguration.schoolYear()));
    currentState.schoolYears().add(schoolYearConfiguration);
    storageManager.store(currentState.schoolYears());
    return schoolYearConfiguration;
  }

  @Override
  public synchronized void delete(String schoolYear) {
    EclipseStoreState currentState = currentState();
    if (currentState
        .schoolYears()
        .removeIf(configuration -> configuration.schoolYear().equals(schoolYear))) {
      storageManager.store(currentState.schoolYears());
    }
  }

  private void deleteMatching(java.util.function.Predicate<Submission> predicate) {
    EclipseStoreState currentState = currentState();
    if (currentState.submissions().removeIf(predicate)) {
      storageManager.store(currentState.submissions());
    }
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
