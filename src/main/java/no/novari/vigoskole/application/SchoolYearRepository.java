package no.novari.vigoskole.application;

import java.util.List;
import java.util.Optional;
import no.novari.vigoskole.domain.model.SchoolYearConfiguration;

public interface SchoolYearRepository {

  List<SchoolYearConfiguration> findAllSchoolYears();

  Optional<SchoolYearConfiguration> findBySchoolYear(String schoolYear);

  SchoolYearConfiguration save(SchoolYearConfiguration schoolYearConfiguration);

  void delete(String schoolYear);
}
