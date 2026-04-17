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
    VigoKodeverkSchoolDirectoryAdapter adapter =
        new VigoKodeverkSchoolDirectoryAdapter(
            HttpClient.newHttpClient(),
            new ObjectMapper(),
            new AppProperties(
                Path.of(".data/eclipsestore"),
                new AppProperties.SubmissionWindowProperties(
                    java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 12, 31)),
                new AppProperties.VigoKodeverkProperties("https://kodeverk.vigo.no")));

    JsonNode response = adapter.executeLookup("974603268");

    assertThat(response).isNotNull();
    assertThat(response.has("content")).isTrue();
    assertThat(response.get("content").isArray()).isTrue();
    assertThat(response.get("content"))
        .anySatisfy(
            entry -> {
              assertThat(entry.path("orgNr").asText()).isEqualTo("974603268");
              assertThat(entry.path("type").asText()).isEqualTo("G");
              assertThat(entry.path("name").asText()).isEqualTo("Ås ungdomsskole");
            });
  }
}
