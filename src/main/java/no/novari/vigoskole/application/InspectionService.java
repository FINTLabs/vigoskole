package no.novari.vigoskole.application;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.novari.vigoskole.config.AppProperties;
import no.novari.vigoskole.domain.model.Submission;
import no.novari.vigoskole.domain.model.SubmissionWindow;
import org.springframework.stereotype.Service;

@Service
public class InspectionService {

  private final SubmissionRepository submissionRepository;
  private final SubmissionWindowRepository submissionWindowRepository;
  private final AppProperties appProperties;

  public InspectionService(
      SubmissionRepository submissionRepository,
      SubmissionWindowRepository submissionWindowRepository,
      AppProperties appProperties) {
    this.submissionRepository = submissionRepository;
    this.submissionWindowRepository = submissionWindowRepository;
    this.appProperties = appProperties;
  }

  public SubmissionWindow submissionWindow() {
    SubmissionWindow current = submissionWindowRepository.get();
    if (current != null && current.from() != null && current.to() != null) {
      return current;
    }
    SubmissionWindow fallback =
        new SubmissionWindow(
            appProperties.submissionWindow().from(), appProperties.submissionWindow().to());
    submissionWindowRepository.save(fallback);
    return fallback;
  }

  public SubmissionWindow updateSubmissionWindow(SubmissionWindow submissionWindow) {
    return submissionWindowRepository.save(submissionWindow);
  }

  public List<String> schoolYears() {
    return submissionRepository.findAll().stream()
        .map(Submission::schoolYear)
        .distinct()
        .sorted(Comparator.reverseOrder())
        .toList();
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
