package no.novari.vigoskole.adapter.out.kodeverk;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.nio.file.Path;
import no.novari.vigoskole.config.AppProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class VigoKodeverkSchoolDirectoryAdapterTest {

  @Test
  void executeLookupShouldReturnAsUngdomsskoleFromRealVigoKodeverk() {
    VigoKodeverkSchoolDirectoryAdapter adapter = adapter();

    JsonNode response = adapter.executeSchoolLookup("974603268");

    assertThat(response).isNotNull();
    assertThat(response.has("content")).isTrue();
    assertThat(response.get("content").isArray()).isTrue();
    assertThat(response.get("content"))
        .anySatisfy(
            entry -> {
              assertThat(entry.path("orgNr").asString()).isEqualTo("974603268");
              assertThat(entry.path("type").asString()).isEqualTo("G");
              assertThat(entry.path("name").asString()).isEqualTo("Ås ungdomsskole");
            });
  }

  @Test
  void findCountyShortNameShouldReturnShortNameFromRealVigoKodeverk() {
    VigoKodeverkSchoolDirectoryAdapter adapter = adapter();

    assertThat(adapter.findCountyShortName("32")).contains("Akershus");
  }

  @Test
  void findMunicipalityNameShouldReturnNameFromRealVigoKodeverk() {
    VigoKodeverkSchoolDirectoryAdapter adapter = adapter();

    assertThat(adapter.findMunicipalityName("3218")).contains("ÅS");
  }

  private VigoKodeverkSchoolDirectoryAdapter adapter() {
    return new VigoKodeverkSchoolDirectoryAdapter(
        HttpClient.newHttpClient(),
        new ObjectMapper(),
        new AppProperties(
            Path.of(".data/eclipsestore"),
            new AppProperties.SubmissionWindowsProperties(
                new AppProperties.SubmissionWindowProperties(
                    java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 12, 31)),
                new AppProperties.SubmissionWindowProperties(
                    java.time.LocalDate.of(2026, 5, 1), java.time.LocalDate.of(2026, 6, 30)),
                new AppProperties.SubmissionWindowProperties(
                    java.time.LocalDate.of(2026, 6, 1), java.time.LocalDate.of(2026, 7, 15))),
            new AppProperties.VigoKodeverkProperties("https://kodeverk.vigo.no")));
  }
}
