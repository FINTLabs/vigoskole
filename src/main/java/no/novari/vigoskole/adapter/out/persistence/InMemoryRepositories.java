package no.novari.vigoskole.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.novari.vigoskole.application.SchoolYearRepository;
import no.novari.vigoskole.application.SubmissionRepository;
import no.novari.vigoskole.domain.model.SchoolYearConfiguration;
import no.novari.vigoskole.domain.model.Submission;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("test")
public class InMemoryRepositories implements SubmissionRepository, SchoolYearRepository {

  private final List<Submission> submissions = new ArrayList<>();
  private final List<SchoolYearConfiguration> schoolYears = new ArrayList<>();

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
  public synchronized void deleteById(UUID id) {
    submissions.removeIf(submission -> submission.id().equals(id));
  }

  @Override
  public synchronized void deleteBySchoolYear(String schoolYear) {
    submissions.removeIf(submission -> submission.schoolYear().equals(schoolYear));
  }

  @Override
  public synchronized void deleteBySchoolYearAndCountyNumber(
      String schoolYear, String countyNumber) {
    submissions.removeIf(
        submission ->
            submission.schoolYear().equals(schoolYear)
                && countyNumber.equals(submission.school().countyNumber()));
  }

  @Override
  public synchronized void deleteBySchoolYearAndSchoolOrgNumber(
      String schoolYear, String schoolOrgNumber) {
    submissions.removeIf(
        submission ->
            submission.schoolYear().equals(schoolYear)
                && schoolOrgNumber.equals(submission.school().orgNumber()));
  }

  @Override
  public synchronized List<SchoolYearConfiguration> findAllSchoolYears() {
    return List.copyOf(schoolYears);
  }

  @Override
  public synchronized Optional<SchoolYearConfiguration> findBySchoolYear(String schoolYear) {
    return schoolYears.stream()
        .filter(configuration -> configuration.schoolYear().equals(schoolYear))
        .findFirst();
  }

  @Override
  public synchronized SchoolYearConfiguration save(
      SchoolYearConfiguration schoolYearConfiguration) {
    schoolYears.removeIf(
        existing -> existing.schoolYear().equals(schoolYearConfiguration.schoolYear()));
    schoolYears.add(schoolYearConfiguration);
    return schoolYearConfiguration;
  }

  @Override
  public synchronized void delete(String schoolYear) {
    schoolYears.removeIf(configuration -> configuration.schoolYear().equals(schoolYear));
  }
}
