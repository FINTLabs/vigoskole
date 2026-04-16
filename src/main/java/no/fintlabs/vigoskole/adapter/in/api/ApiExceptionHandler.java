package no.fintlabs.vigoskole.adapter.in.api;

import java.time.LocalDateTime;
import no.fintlabs.vigoskole.application.DuplicateSubmissionException;
import no.fintlabs.vigoskole.application.SchoolVerificationException;
import no.fintlabs.vigoskole.application.SubmissionRejectedException;
import no.fintlabs.vigoskole.application.SubmissionWindowClosedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = SubmissionController.class)
public class ApiExceptionHandler {

  @ExceptionHandler(SubmissionRejectedException.class)
  ResponseEntity<ErrorResponse> handleRejected(SubmissionRejectedException exception) {
    return ResponseEntity.badRequest()
        .body(
            new ErrorResponse(
                "VALIDATION_ERROR",
                exception.getMessage(),
                LocalDateTime.now(),
                SubmissionController.SubmissionResponse.from(exception.submission())));
  }

  @ExceptionHandler({
    DuplicateSubmissionException.class,
    SubmissionWindowClosedException.class,
    SchoolVerificationException.class
  })
  ResponseEntity<ErrorResponse> handleForbidden(RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse("FORBIDDEN", exception.getMessage(), LocalDateTime.now(), null));
  }

  @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
  ResponseEntity<ErrorResponse> handleBadRequest(Exception exception) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse("BAD_REQUEST", exception.getMessage(), LocalDateTime.now(), null));
  }

  record ErrorResponse(
      String errorCode,
      String message,
      LocalDateTime timestamp,
      SubmissionController.SubmissionResponse submission) {}
}
