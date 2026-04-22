package no.novari.vigoskole.domain.model;

import java.util.List;

public record GraduatingStudentsSubmission(String schoolOrgNumber, List<StudentRecord> students) {

  public GraduatingStudentsSubmission {
    students = students == null ? List.of() : List.copyOf(students);
  }
}
