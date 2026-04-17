package no.novari.vigoskole.application;

import java.util.Optional;
import no.novari.vigoskole.domain.model.SchoolInfo;

public interface SchoolDirectoryPort {

  Optional<SchoolInfo> findLowerSecondarySchool(String orgNumber);

  Optional<String> findCountyShortName(String countyNumber);

  Optional<String> findMunicipalityName(String municipalityNumber);
}
