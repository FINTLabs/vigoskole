package no.novari.vigoskole.adapter.in.api;

import java.time.LocalDate;
import java.util.List;
import no.novari.vigoskole.application.SubmissionService;
import no.novari.vigoskole.config.SecurityConfig.JwtSubmitterContextResolver;
import no.novari.vigoskole.domain.model.StudentRecord;
import no.novari.vigoskole.domain.model.Submission;
import no.novari.vigoskole.domain.model.ValidatedStudent;
import no.novari.vigoskole.domain.model.ValidationMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

  private final SubmissionService submissionService;
  private final JwtSubmitterContextResolver jwtSubmitterContextResolver;
  private final ObjectMapper objectMapper;

  public SubmissionController(
      SubmissionService submissionService,
      JwtSubmitterContextResolver jwtSubmitterContextResolver,
      ObjectMapper objectMapper) {
    this.submissionService = submissionService;
    this.jwtSubmitterContextResolver = jwtSubmitterContextResolver;
    this.objectMapper = objectMapper;
  }

  @PostMapping(
      path = "/graduating-students",
      consumes = {MediaType.APPLICATION_JSON_VALUE, "application/ld+json"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> submitGraduatingStudents(
      @RequestBody GraduatingStudentsSubmissionRequest request, @AuthenticationPrincipal Jwt jwt) {
    Submission submission =
        submissionService.submitGraduatingStudents(
            request.toDomain(), jwtSubmitterContextResolver.resolve(jwt));
    return ResponseEntity.status(HttpStatus.CREATED)
        .contentType(MediaType.APPLICATION_JSON)
        .body(toPrettyJson(SubmissionResponse.from(submission)));
  }

  String toPrettyJson(Object body) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Kunne ikke serialisere API-respons.", exception);
    }
  }

  public record GraduatingStudentsSubmissionRequest(List<StudentRequest> students) {

    List<StudentRecord> toDomain() {
      return students == null
          ? List.of()
          : students.stream().map(StudentRequest::toDomain).toList();
    }
  }

  public record StudentRequest(String classCode, String personalIdentityNumber, NameRequest name) {

    StudentRecord toDomain() {
      return new StudentRecord(
          classCode, personalIdentityNumber, name == null ? null : name.toDomain());
    }
  }

  public record NameRequest(String firstName, String middleName, String lastName) {

    StudentRecord.PersonName toDomain() {
      return new StudentRecord.PersonName(firstName, middleName, lastName);
    }
  }

  public record SubmissionResponse(
      String submissionId,
      String schoolYear,
      String status,
      SchoolResponse school,
      SupplierResponse supplier,
      String submittedBy,
      LocalDate receivedDate,
      List<ValidatedStudentResponse> validationResults) {

    static SubmissionResponse from(Submission submission) {
      return new SubmissionResponse(
          submission.id().toString(),
          submission.schoolYear(),
          submission.status().name(),
          new SchoolResponse(
              submission.school().orgNumber(),
              submission.school().name(),
              submission.school().municipalityNumber(),
              submission.school().countyNumber(),
              submission.school().schoolNumber()),
          submission.supplier() == null
              ? null
              : new SupplierResponse(
                  submission.supplier().orgNumber(), submission.supplier().name()),
          submission.submittedBy(),
          submission.submittedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
          submission.validatedStudents().stream().map(ValidatedStudentResponse::from).toList());
    }
  }

  public record SchoolResponse(
      String orgNumber,
      String name,
      String municipalityNumber,
      String countyNumber,
      String schoolNumber) {}

  public record SupplierResponse(String orgNumber, String name) {}

  public record ValidatedStudentResponse(
      int lineNumber,
      String classCode,
      String personalIdentityNumberStatus,
      String name,
      List<MessageResponse> messages) {

    static ValidatedStudentResponse from(ValidatedStudent student) {
      return new ValidatedStudentResponse(
          student.lineNumber(),
          student.student().classCode(),
          student.personNumberStatus().displayValue(),
          student.student().name() == null ? "" : student.student().name().formatted(),
          student.messages().stream().map(MessageResponse::from).toList());
    }
  }

  public record MessageResponse(String field, String code, String severity, String message) {

    static MessageResponse from(ValidationMessage message) {
      return new MessageResponse(
          message.field(), message.code(), message.severity().name(), message.message());
    }
  }
}
