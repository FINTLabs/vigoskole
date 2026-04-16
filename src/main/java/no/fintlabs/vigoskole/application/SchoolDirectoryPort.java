package no.fintlabs.vigoskole.application;

import java.util.Optional;
import no.fintlabs.vigoskole.domain.model.SchoolInfo;

public interface SchoolDirectoryPort {

  Optional<SchoolInfo> findLowerSecondarySchool(String orgNumber);
}
