package no.fintlabs.vigoskole.application;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.fintlabs.vigoskole.domain.model.Submission;
import no.fintlabs.vigoskole.domain.model.SubmissionWindow;
import org.springframework.stereotype.Service;

@Service
public class InspectionService {

  private final SubmissionRepository submissionRepository;
  private final SubmissionWindowRepository submissionWindowRepository;

  public InspectionService(
      SubmissionRepository submissionRepository,
      SubmissionWindowRepository submissionWindowRepository) {
    this.submissionRepository = submissionRepository;
    this.submissionWindowRepository = submissionWindowRepository;
  }

  public SubmissionWindow submissionWindow() {
    return submissionWindowRepository.get();
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
