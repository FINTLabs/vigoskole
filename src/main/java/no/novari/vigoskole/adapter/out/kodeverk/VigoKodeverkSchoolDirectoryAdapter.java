package no.novari.vigoskole.adapter.out.kodeverk;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.novari.vigoskole.application.SchoolDirectoryPort;
import no.novari.vigoskole.application.SchoolVerificationException;
import no.novari.vigoskole.config.AppProperties;
import no.novari.vigoskole.domain.model.SchoolInfo;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class VigoKodeverkSchoolDirectoryAdapter implements SchoolDirectoryPort {

  static final String SCHOOL_LOOKUP_PATH = "/api/schools?page=0&size=10";
  static final String COUNTY_LOOKUP_PATH = "/api/counties?page=0&size=10&sort=countyNr,asc";
  static final String MUNICIPALITY_LOOKUP_PATH =
      "/api/municipalities?page=0&size=10&sort=municipalityNr,asc";

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final URI schoolLookupUri;
  private final URI countyLookupUri;
  private final URI municipalityLookupUri;

  public VigoKodeverkSchoolDirectoryAdapter(
      HttpClient httpClient, ObjectMapper objectMapper, AppProperties appProperties) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.schoolLookupUri =
        URI.create(appProperties.vigoKodeverk().baseUrl()).resolve(SCHOOL_LOOKUP_PATH);
    this.countyLookupUri =
        URI.create(appProperties.vigoKodeverk().baseUrl()).resolve(COUNTY_LOOKUP_PATH);
    this.municipalityLookupUri =
        URI.create(appProperties.vigoKodeverk().baseUrl()).resolve(MUNICIPALITY_LOOKUP_PATH);
  }

  @Override
  public Optional<SchoolInfo> findLowerSecondarySchool(String orgNumber) {
    JsonNode response = executeSchoolLookup(orgNumber);

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

  @Override
  public Optional<String> findCountyShortName(String countyNumber) {
    JsonNode response = executeCountyLookup(countyNumber);

    if (response == null || !response.has("content") || !response.get("content").isArray()) {
      return Optional.empty();
    }

    for (JsonNode entry : response.get("content")) {
      String candidateCountyNumber = findValue(entry, "countyNr");
      if (countyNumber.equals(candidateCountyNumber)) {
        return Optional.ofNullable(findValue(entry, "shortName"));
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> findMunicipalityName(String municipalityNumber) {
    JsonNode response = executeMunicipalityLookup(municipalityNumber);

    if (response == null || !response.has("content") || !response.get("content").isArray()) {
      return Optional.empty();
    }

    for (JsonNode entry : response.get("content")) {
      String candidateMunicipalityNumber = findValue(entry, "municipalityNr");
      if (municipalityNumber.equals(candidateMunicipalityNumber)) {
        return Optional.ofNullable(findValue(entry, "name"));
      }
    }
    return Optional.empty();
  }

  JsonNode executeSchoolLookup(String orgNumber) {
    return executeLookup(schoolLookupUri, "orgNr", orgNumber);
  }

  JsonNode executeCountyLookup(String countyNumber) {
    return executeLookup(countyLookupUri, "countyNr", countyNumber);
  }

  JsonNode executeMunicipalityLookup(String municipalityNumber) {
    return executeLookup(municipalityLookupUri, "municipalityNr", municipalityNumber);
  }

  private JsonNode executeLookup(URI lookupUri, String key, String value) {
    String requestBody = serializeRequest(key, value);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(lookupUri)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new SchoolVerificationException(
            "Oppslag mot VIGO Kodeverk feilet med HTTP " + response.statusCode() + ".");
      }
      return objectMapper.readTree(response.body());
    } catch (IOException exception) {
      throw new SchoolVerificationException(
          "Oppslag mot VIGO Kodeverk feilet på grunn av I/O-feil.");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new SchoolVerificationException("Oppslag mot VIGO Kodeverk ble avbrutt.");
    }
  }

  private String serializeRequest(String key, String value) {
    return objectMapper.writeValueAsString(
        List.of(Map.of("key", key, "value", value, "operation", "EQUAL")));
  }

  private String findValue(JsonNode node, String key) {
    if (node.hasNonNull(key)) {
      return node.get(key).asString();
    }
    JsonNode values = node.get("values");
    if (values != null && values.isArray()) {
      for (JsonNode item : values) {
        if (key.equals(item.path("key").asString())) {
          return item.path("value").asString(null);
        }
      }
    }
    JsonNode fields = node.get("fields");
    if (fields != null && fields.isArray()) {
      for (JsonNode item : fields) {
        if (key.equals(item.path("key").asString())) {
          return item.path("value").asString(null);
        }
      }
    }
    return null;
  }
}
