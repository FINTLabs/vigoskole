package no.novari.vigoskole.application;

import no.novari.vigoskole.domain.model.SubmissionWindow;

public interface SubmissionWindowRepository {

  SubmissionWindow get();

  SubmissionWindow save(SubmissionWindow submissionWindow);
}
