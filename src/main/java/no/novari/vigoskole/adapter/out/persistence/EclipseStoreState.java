package no.novari.vigoskole.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import no.novari.vigoskole.domain.model.SchoolYearConfiguration;
import no.novari.vigoskole.domain.model.Submission;

public class EclipseStoreState {

  private final List<SchoolYearConfiguration> schoolYears = new ArrayList<>();
  private final List<Submission> submissions = new ArrayList<>();

  public List<SchoolYearConfiguration> schoolYears() {
    return schoolYears;
  }

  public List<Submission> submissions() {
    return submissions;
  }
}
