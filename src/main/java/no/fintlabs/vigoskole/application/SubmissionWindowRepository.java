package no.fintlabs.vigoskole.application;

import no.fintlabs.vigoskole.domain.model.SubmissionWindow;

public interface SubmissionWindowRepository {

  SubmissionWindow get();

  SubmissionWindow save(SubmissionWindow submissionWindow);
}
