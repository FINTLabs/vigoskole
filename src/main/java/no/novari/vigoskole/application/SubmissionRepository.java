package no.novari.vigoskole.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.novari.vigoskole.domain.model.Submission;

public interface SubmissionRepository {

  void save(Submission submission);

  boolean existsAcceptedSubmission(String schoolYear, String schoolOrgNumber, Submission.Type type);

  List<Submission> findAll();

  Optional<Submission> findById(UUID id);

  void deleteById(UUID id);

  void deleteBySchoolYear(String schoolYear);

  void deleteBySchoolYearAndCountyNumber(String schoolYear, String countyNumber);

  void deleteBySchoolYearAndSchoolOrgNumber(String schoolYear, String schoolOrgNumber);
}
