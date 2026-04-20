package no.novari.vigoskole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.novari.vigoskole.domain.model.SchoolInfo;
import no.novari.vigoskole.domain.model.SchoolYearConfiguration;
import no.novari.vigoskole.domain.model.StudentRecord;
import no.novari.vigoskole.domain.model.Submission;
import no.novari.vigoskole.domain.model.SubmissionWindow;
import no.novari.vigoskole.domain.model.SubmissionWindows;
import no.novari.vigoskole.domain.model.SupplierInfo;
import no.novari.vigoskole.domain.model.ValidatedStudent;
import no.novari.vigoskole.domain.model.ValidationMessage;

public final class TestData {

  public static final String SCHOOL_ORG_NUMBER = "974603268";
  public static final String VALID_F_NUMBER = "15049100008";
  public static final String VALID_D_NUMBER = "41128500071";
  public static final String VALID_SYNTHETIC_NUMBER = "42059190061";

  private TestData() {}

  public static StudentRecord validStudent() {
    return new StudentRecord(
        "10A", VALID_F_NUMBER, new StudentRecord.PersonName("Ola", null, "Nordmann"));
  }

  public static StudentRecord syntheticStudent() {
    return new StudentRecord(
        "10A", VALID_SYNTHETIC_NUMBER, new StudentRecord.PersonName("Kari", null, "Nordmann"));
  }

  public static SchoolInfo schoolInfo() {
    return new SchoolInfo(SCHOOL_ORG_NUMBER, "Ås ungdomsskole", "3218", "32", "1540");
  }

  public static SubmissionWindows submissionWindows() {
    return new SubmissionWindows(
        new SubmissionWindow(
            java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 12, 31)),
        new SubmissionWindow(
            java.time.LocalDate.of(2026, 5, 1), java.time.LocalDate.of(2026, 6, 30)),
        new SubmissionWindow(
            java.time.LocalDate.of(2026, 6, 1), java.time.LocalDate.of(2026, 7, 15)));
  }

  public static SchoolYearConfiguration schoolYearConfiguration() {
    return new SchoolYearConfiguration("2025-2026", submissionWindows());
  }

  public static Submission submission(Submission.Status status) {
    return new Submission(
        UUID.randomUUID(),
        Submission.Type.GRADUATING_STUDENTS,
        "2025-2026",
        status,
        schoolInfo(),
        new SupplierInfo("999888777", "Leverandor AS"),
        "Fylkesbruker",
        Instant.parse("2026-04-16T10:15:30Z"),
        List.of(
            new ValidatedStudent(
                1,
                validStudent(),
                ValidatedStudent.PersonNumberStatus.F_NUMBER,
                List.of(
                    new ValidationMessage(
                        "name",
                        "MISSING_NAME",
                        ValidationMessage.Severity.WARNING,
                        "Elevnavn mangler.")))));
  }

  public static String validSubmissionPayload() {
    return """
                {
                  "@context": {
                    "fint": "https://novari.no/ontology/fint.ttl#",
                    "vigo": "https://novari.no/ontology/vigo-skole#"
                  },
                  "@type": "vigo:graduatingStudentSubmission",
                  "students": [
                    {
                      "classCode": "10A",
                      "personalIdentityNumber": "15049100008",
                      "name": {
                        "firstName": "Ola",
                        "lastName": "Nordmann"
                      }
                    }
                  ]
                }
                """;
  }

  public static String warningSubmissionPayload() {
    return """
                {
                  "@context": {
                    "fint": "https://novari.no/ontology/fint.ttl#",
                    "vigo": "https://novari.no/ontology/vigo-skole#"
                  },
                  "@type": "vigo:graduatingStudentSubmission",
                  "students": [
                    {
                      "classCode": "10A",
                      "personalIdentityNumber": "42059190061",
                      "name": {
                        "firstName": "Kari",
                        "lastName": "Nordmann"
                      }
                    }
                  ]
                }
                """;
  }

  public static String invalidSubmissionPayload() {
    return """
                {
                  "@context": {
                    "fint": "https://novari.no/ontology/fint.ttl#",
                    "vigo": "https://novari.no/ontology/vigo-skole#"
                  },
                  "@type": "vigo:graduatingStudentSubmission",
                  "students": [
                    {
                      "classCode": "",
                      "personalIdentityNumber": "123",
                      "name": {}
                    }
                  ]
                }
                """;
  }

  public static String kodeverkResponse() {
    return """
                {
                  "content": [
                    {
                      "type": "G",
                      "orgNr": "974603268",
                      "name": "Ås ungdomsskole",
                      "municipalityNr": "3218",
                      "countyNr": "32",
                      "number": "1540"
                    }
                  ]
                }
                """;
  }

  public static String kodeverkResponseWithoutMatchingSchool() {
    return """
                {
                  "content": [
                    {
                      "type": "V",
                      "orgNr": "974603268",
                      "name": "Ikke ungdomsskole",
                      "municipalityNr": "3218",
                      "countyNr": "32",
                      "number": "1540"
                    }
                  ]
                }
                """;
  }
}
