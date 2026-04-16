package no.fintlabs.vigoskole.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.fintlabs.vigoskole.domain.model.Submission;

public interface SubmissionRepository {

  Submission save(Submission submission);

  boolean existsAcceptedSubmission(String schoolYear, String schoolOrgNumber, Submission.Type type);

  List<Submission> findAll();

  Optional<Submission> findById(UUID id);
}
