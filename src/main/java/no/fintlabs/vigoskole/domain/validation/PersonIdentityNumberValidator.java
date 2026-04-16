package no.fintlabs.vigoskole.domain.validation;

import java.util.Set;
import no.fintlabs.vigoskole.domain.model.ValidatedStudent;

public class PersonIdentityNumberValidator {

  private static final int[] LOOKUP1 = {3, 7, 6, 1, 8, 9, 4, 5, 2, 0};
  private static final int[] LOOKUP2 = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
  private static final Set<Integer> VALID_2032_REMAINDERS = Set.of(0, 1, 2, 3);

  public ValidationResult validate(String candidate) {
    if (candidate == null || !candidate.matches("\\d{11}")) {
      return ValidationResult.invalid();
    }
    if (!(validateMod11Pre2032(candidate) || validateMod112032(candidate))) {
      return ValidationResult.invalid();
    }

    int day = Integer.parseInt(candidate.substring(0, 2));
    int month = Integer.parseInt(candidate.substring(2, 4));
    if (month < 1 || month > 12) {
      return ValidationResult.invalid();
    }

    if (day >= 1 && day <= 31) {
      return ValidationResult.of(ValidatedStudent.PersonNumberStatus.F_NUMBER);
    }
    if (day >= 41 && day <= 71) {
      int syntheticPrefix = Integer.parseInt(candidate.substring(6, 8));
      if (syntheticPrefix >= 90 && syntheticPrefix <= 99) {
        return ValidationResult.of(ValidatedStudent.PersonNumberStatus.SYNTHETIC);
      }
      return ValidationResult.of(ValidatedStudent.PersonNumberStatus.D_NUMBER);
    }
    return ValidationResult.invalid();
  }

  private boolean validateMod11Pre2032(String socialSecurityNumber) {
    int checksum1 = 0;
    int checksum2 = 0;

    for (int i = 0; i <= 9; i++) {
      int current = socialSecurityNumber.charAt(i) - '0';
      checksum1 += current * LOOKUP1[i];
      checksum2 += current * LOOKUP2[i];
    }

    checksum1 %= 11;
    checksum2 %= 11;

    int checksum1Final = checksum1 == 0 ? 0 : 11 - checksum1;
    int checksum2Final = checksum2 == 0 ? 0 : 11 - checksum2;

    return checksum1Final != 10
        && socialSecurityNumber.charAt(9) - '0' == checksum1Final
        && socialSecurityNumber.charAt(10) - '0' == checksum2Final;
  }

  private boolean validateMod112032(String socialSecurityNumber) {
    int[] digits = socialSecurityNumber.chars().map(character -> character - '0').toArray();
    int givenK1 = digits[9];
    int givenK2 = digits[10];

    int weightedK1 = 0;
    for (int i = 0; i <= 8; i++) {
      weightedK1 += digits[i] * LOOKUP1[i];
    }

    int remainderK1 = (weightedK1 + givenK1) % 11;
    if (!VALID_2032_REMAINDERS.contains(remainderK1)) {
      return false;
    }

    int weightedK2 = 0;
    for (int i = 0; i <= 9; i++) {
      weightedK2 += digits[i] * LOOKUP2[i];
    }
    return (weightedK2 + givenK2) % 11 == 0;
  }

  public record ValidationResult(ValidatedStudent.PersonNumberStatus status) {

    public static ValidationResult invalid() {
      return new ValidationResult(ValidatedStudent.PersonNumberStatus.INVALID);
    }

    public static ValidationResult of(ValidatedStudent.PersonNumberStatus status) {
      return new ValidationResult(status);
    }

    public boolean valid() {
      return status != ValidatedStudent.PersonNumberStatus.INVALID;
    }
  }
}
