package no.novari.vigoskole.adapter.out.kodeverk;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.novari.vigoskole.application.SchoolDirectoryPort;
import no.novari.vigoskole.domain.model.SchoolInfo;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class VigoKodeverkSchoolDirectoryAdapter implements SchoolDirectoryPort {

  private final RestClient restClient;

  public VigoKodeverkSchoolDirectoryAdapter(RestClient vigoKodeverkRestClient) {
    this.restClient = vigoKodeverkRestClient;
  }

  @Override
  public Optional<SchoolInfo> findLowerSecondarySchool(String orgNumber) {
    JsonNode response =
        restClient
            .method(HttpMethod.GET)
            .uri("/api/schools?page=0&size=10")
            .contentType(MediaType.APPLICATION_JSON)
            .body(List.of(Map.of("key", "orgNr", "value", orgNumber, "operation", "EQUAL")))
            .retrieve()
            .body(JsonNode.class);

    if (response == null || !response.has("content") || !response.get("content").isArray()) {
      return Optional.empty();
    }

    for (JsonNode entry : response.get("content")) {
      String type = findValue(entry, "type");
      String candidateOrgNumber = findValue(entry, "orgNr");
      if ("G".equals(type) && orgNumber.equals(candidateOrgNumber)) {
        return Optional.of(
            new SchoolInfo(
                candidateOrgNumber,
                findValue(entry, "name"),
                findValue(entry, "municipalityNr"),
                findValue(entry, "countyNr"),
                findValue(entry, "number")));
      }
    }
    return Optional.empty();
  }

  private String findValue(JsonNode node, String key) {
    if (node.hasNonNull(key)) {
      return node.get(key).asText();
    }
    JsonNode values = node.get("values");
    if (values != null && values.isArray()) {
      for (JsonNode item : values) {
        if (key.equals(item.path("key").asText())) {
          return item.path("value").asText(null);
        }
      }
    }
    JsonNode fields = node.get("fields");
    if (fields != null && fields.isArray()) {
      for (JsonNode item : fields) {
        if (key.equals(item.path("key").asText())) {
          return item.path("value").asText(null);
        }
      }
    }
    return null;
  }
}
