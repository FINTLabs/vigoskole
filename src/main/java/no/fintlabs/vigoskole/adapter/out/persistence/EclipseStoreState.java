package no.fintlabs.vigoskole.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import no.fintlabs.vigoskole.domain.model.Submission;
import no.fintlabs.vigoskole.domain.model.SubmissionWindow;

public class EclipseStoreState {

  private SubmissionWindow submissionWindow;
  private final List<Submission> submissions = new ArrayList<>();

  public EclipseStoreState(SubmissionWindow submissionWindow) {
    this.submissionWindow = submissionWindow;
  }

  public SubmissionWindow submissionWindow() {
    return submissionWindow;
  }

  public void submissionWindow(SubmissionWindow submissionWindow) {
    this.submissionWindow = submissionWindow;
  }

  public List<Submission> submissions() {
    return submissions;
  }
}
