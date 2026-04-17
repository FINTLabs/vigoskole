package no.novari.vigoskole.domain.model;

public record StudentRecord(String classCode, String personalIdentityNumber, PersonName name) {

  public String formattedPersonalIdentityNumber() {
    if (personalIdentityNumber == null || personalIdentityNumber.length() != 11) {
      return personalIdentityNumber == null ? "" : personalIdentityNumber;
    }
    return personalIdentityNumber.substring(0, 6) + " " + personalIdentityNumber.substring(6);
  }

  public record PersonName(String firstName, String middleName, String lastName) {

    public String formatted() {
      return java.util.stream.Stream.of(firstName, middleName, lastName)
          .filter(value -> value != null && !value.isBlank())
          .reduce((left, right) -> left + " " + right)
          .orElse("");
    }
  }
}
