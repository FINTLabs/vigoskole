package no.novari.vigoskole.application;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.novari.vigoskole.config.AppProperties;
import no.novari.vigoskole.domain.model.SchoolYearConfiguration;
import no.novari.vigoskole.domain.model.Submission;
import no.novari.vigoskole.domain.model.SubmissionWindow;
import no.novari.vigoskole.domain.model.SubmissionWindows;
import org.springframework.stereotype.Service;

@Service
public class InspectionService {

  private final SubmissionRepository submissionRepository;
  private final SchoolYearRepository schoolYearRepository;
  private final AppProperties appProperties;

  public InspectionService(
      SubmissionRepository submissionRepository,
      SchoolYearRepository schoolYearRepository,
      AppProperties appProperties) {
    this.submissionRepository = submissionRepository;
    this.schoolYearRepository = schoolYearRepository;
    this.appProperties = appProperties;
  }

  public SubmissionWindows defaultSubmissionWindows() {
    if (appProperties.submissionWindows() == null) {
      return new SubmissionWindows(
          new SubmissionWindow(
              java.time.LocalDate.of(2026, 1, 2), java.time.LocalDate.of(2026, 5, 31)),
          new SubmissionWindow(
              java.time.LocalDate.of(2026, 5, 15), java.time.LocalDate.of(2026, 6, 20)),
          new SubmissionWindow(
              java.time.LocalDate.of(2026, 6, 1), java.time.LocalDate.of(2026, 6, 30)));
    }
    return new SubmissionWindows(
        new SubmissionWindow(
            appProperties.submissionWindows().graduatingStudents().from(),
            appProperties.submissionWindows().graduatingStudents().to()),
        new SubmissionWindow(
            appProperties.submissionWindows().finalGrades().from(),
            appProperties.submissionWindows().finalGrades().to()),
        new SubmissionWindow(
            appProperties.submissionWindows().examGrades().from(),
            appProperties.submissionWindows().examGrades().to()));
  }

  public List<SchoolYearConfiguration> schoolYears() {
    return schoolYearRepository.findAllSchoolYears().stream()
        .sorted(Comparator.comparing(SchoolYearConfiguration::schoolYear).reversed())
        .toList();
  }

  public Optional<SchoolYearConfiguration> schoolYear(String schoolYear) {
    return schoolYearRepository.findBySchoolYear(schoolYear);
  }

  public SchoolYearConfiguration createSchoolYear(
      String schoolYear, SubmissionWindows submissionWindows) {
    return schoolYearRepository.save(new SchoolYearConfiguration(schoolYear, submissionWindows));
  }

  public SchoolYearConfiguration updateSchoolYear(
      String schoolYear, SubmissionWindows submissionWindows) {
    return schoolYearRepository.save(new SchoolYearConfiguration(schoolYear, submissionWindows));
  }

  public void deleteSchoolYear(String schoolYear) {
    schoolYearRepository.delete(schoolYear);
    submissionRepository.deleteBySchoolYear(schoolYear);
  }

  public void deleteCountySubmissions(String schoolYear, String countyNumber) {
    submissionRepository.deleteBySchoolYearAndCountyNumber(schoolYear, countyNumber);
  }

  public void deleteSchoolSubmissions(String schoolYear, String schoolOrgNumber) {
    submissionRepository.deleteBySchoolYearAndSchoolOrgNumber(schoolYear, schoolOrgNumber);
  }

  public void deleteSubmission(UUID submissionId) {
    submissionRepository.deleteById(submissionId);
  }

  public List<Submission> submissions() {
    return submissionRepository.findAll().stream()
        .sorted(Comparator.comparing(Submission::submittedAt).reversed())
        .toList();
  }

  public Optional<Submission> submission(UUID id) {
    return submissionRepository.findById(id);
  }
}
