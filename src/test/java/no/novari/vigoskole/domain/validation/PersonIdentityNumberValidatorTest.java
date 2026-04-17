package no.novari.vigoskole.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;

import no.novari.vigoskole.TestData;
import no.novari.vigoskole.domain.model.ValidatedStudent;
import org.junit.jupiter.api.Test;

class PersonIdentityNumberValidatorTest {

  private final PersonIdentityNumberValidator validator = new PersonIdentityNumberValidator();

  @Test
  void shouldAcceptRegularNationalIdentityNumber() {
    assertThat(validator.validate(TestData.VALID_F_NUMBER).status())
        .isEqualTo(ValidatedStudent.PersonNumberStatus.F_NUMBER);
  }

  @Test
  void shouldAcceptDNumber() {
    assertThat(validator.validate(TestData.VALID_D_NUMBER).status())
        .isEqualTo(ValidatedStudent.PersonNumberStatus.D_NUMBER);
  }

  @Test
  void shouldMarkSyntheticIdentityNumberAsWarningType() {
    assertThat(validator.validate(TestData.VALID_SYNTHETIC_NUMBER).status())
        .isEqualTo(ValidatedStudent.PersonNumberStatus.SYNTHETIC);
  }

  @Test
  void shouldRejectInvalidIdentityNumber() {
    assertThat(validator.validate("123").status())
        .isEqualTo(ValidatedStudent.PersonNumberStatus.INVALID);
  }
}
