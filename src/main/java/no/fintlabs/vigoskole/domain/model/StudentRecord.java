package no.fintlabs.vigoskole.domain.model;

public record StudentRecord(String classCode, String personalIdentityNumber, PersonName name) {

  public record PersonName(String firstName, String middleName, String lastName) {

    public String formatted() {
      return java.util.stream.Stream.of(firstName, middleName, lastName)
          .filter(value -> value != null && !value.isBlank())
          .reduce((left, right) -> left + " " + right)
          .orElse("");
    }
  }
}
