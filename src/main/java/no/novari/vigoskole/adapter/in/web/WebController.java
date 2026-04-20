package no.novari.vigoskole.adapter.in.web;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import no.novari.vigoskole.application.InspectionService;
import no.novari.vigoskole.application.SchoolDirectoryPort;
import no.novari.vigoskole.domain.model.SchoolYearConfiguration;
import no.novari.vigoskole.domain.model.Submission;
import no.novari.vigoskole.domain.model.SubmissionWindow;
import no.novari.vigoskole.domain.model.SubmissionWindows;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/ui")
public class WebController {

  private static final Pattern SCHOOL_YEAR_PATTERN = Pattern.compile("\\d{4}-\\d{4}");

  private final InspectionService inspectionService;
  private final SchoolDirectoryPort schoolDirectoryPort;

  public WebController(
      InspectionService inspectionService, SchoolDirectoryPort schoolDirectoryPort) {
    this.inspectionService = inspectionService;
    this.schoolDirectoryPort = schoolDirectoryPort;
  }

  @GetMapping("/school-years")
  public String schoolYears(Model model, Authentication authentication) {
    populateSchoolYearsModel(model, authentication);
    return "school-years";
  }

  @GetMapping("/school-years/new")
  public String newSchoolYear(Model model, Authentication authentication) {
    return schoolYearForm(
        model,
        authentication,
        "Opprett skoleår",
        "Sett opp et nytt skoleår og konfigurer innsendingsvinduer for hver type innsending.",
        "Opprett skoleår",
        "/ui/school-years",
        "Skoleår",
        null,
        false,
        toWindowForm(inspectionService.defaultSubmissionWindows()),
        "/ui/school-years",
        "Avbryt");
  }

  @GetMapping("/school-years/{schoolYear}/edit")
  public String editSchoolYear(
      @PathVariable String schoolYear, Model model, Authentication authentication) {
    SchoolYearConfiguration schoolYearConfiguration =
        inspectionService
            .schoolYear(schoolYear)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return schoolYearForm(
        model,
        authentication,
        "Rediger skoleår",
        "Oppdater innsendingsvinduer for valgt skoleår.",
        "Lagre endringer",
        "/ui/school-years/" + schoolYear + "/edit",
        schoolYear,
        schoolYear,
        true,
        toWindowForm(schoolYearConfiguration.submissionWindows()),
        "/ui/school-years/" + schoolYear,
        "Tilbake til skoleåret");
  }

  @PostMapping("/school-years")
  public String createSchoolYear(
      @RequestParam String schoolYear,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate graduatingStudentsFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate graduatingStudentsTo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate finalGradesFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate finalGradesTo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate examGradesFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate examGradesTo,
      Model model,
      Authentication authentication) {
    if (!isValidSchoolYear(schoolYear)) {
      return schoolYearError(
          model,
          authentication,
          "Skoleår er ugyldig.",
          "Bruk formatet ÅÅÅÅ-ÅÅÅÅ, og sørg for at sluttsåret er ett år etter startåret.",
          "/ui/school-years/new",
          "Tilbake til opprettelse av skoleår");
    }
    if (inspectionService.schoolYear(schoolYear).isPresent()) {
      return schoolYearError(
          model,
          authentication,
          "Skoleåret finnes allerede.",
          "Velg et skoleår som ikke er opprettet fra før.",
          "/ui/school-years/new",
          "Tilbake til opprettelse av skoleår");
    }
    SubmissionWindows submissionWindows =
        submissionWindows(
            graduatingStudentsFrom,
            graduatingStudentsTo,
            finalGradesFrom,
            finalGradesTo,
            examGradesFrom,
            examGradesTo,
            "/ui/school-years/new",
            "Tilbake til opprettelse av skoleår",
            model,
            authentication);
    if (submissionWindows == null) {
      return "school-year-form-error";
    }
    inspectionService.createSchoolYear(schoolYear, submissionWindows);
    return "redirect:/ui/school-years";
  }

  @PostMapping("/school-years/{schoolYear}/edit")
  public String updateSchoolYearWindows(
      @PathVariable String schoolYear,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate graduatingStudentsFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate graduatingStudentsTo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate finalGradesFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate finalGradesTo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate examGradesFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate examGradesTo,
      Model model,
      Authentication authentication) {
    if (inspectionService.schoolYear(schoolYear).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    SubmissionWindows submissionWindows =
        submissionWindows(
            graduatingStudentsFrom,
            graduatingStudentsTo,
            finalGradesFrom,
            finalGradesTo,
            examGradesFrom,
            examGradesTo,
            "/ui/school-years/" + schoolYear + "/edit",
            "Tilbake til redigering av skoleår",
            model,
            authentication);
    if (submissionWindows == null) {
      return "school-year-form-error";
    }
    inspectionService.updateSchoolYear(schoolYear, submissionWindows);
    return "redirect:/ui/school-years/" + schoolYear;
  }

  @PostMapping("/school-years/{schoolYear}/delete")
  public String deleteSchoolYear(@PathVariable String schoolYear) {
    inspectionService.deleteSchoolYear(schoolYear);
    return "redirect:/ui/school-years";
  }

  @PostMapping("/school-years/{schoolYear}/counties/{countyNumber}/delete")
  public String deleteCountySubmissions(
      @PathVariable String schoolYear, @PathVariable String countyNumber) {
    inspectionService.deleteCountySubmissions(schoolYear, countyNumber);
    return "redirect:/ui/school-years/" + schoolYear;
  }

  @PostMapping("/school-years/{schoolYear}/counties/{countyNumber}/schools/{orgNumber}/delete")
  public String deleteSchoolSubmissions(
      @PathVariable String schoolYear,
      @PathVariable String countyNumber,
      @PathVariable String orgNumber) {
    inspectionService.deleteSchoolSubmissions(schoolYear, orgNumber);
    return "redirect:/ui/school-years/" + schoolYear + "/counties/" + countyNumber;
  }

  @GetMapping("/school-years/{schoolYear}")
  public String schoolYear(
      @PathVariable String schoolYear, Model model, Authentication authentication) {
    List<Submission> submissions = submissionsForSchoolYear(schoolYear);
    inspectionService
        .schoolYear(schoolYear)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Map<String, List<Submission>> counties =
        submissions.stream()
            .collect(Collectors.groupingBy(submission -> submission.school().countyNumber()));
    model.addAttribute("schoolYear", schoolYear);
    model.addAttribute(
        "counties",
        counties.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(
                entry ->
                    new CountyView(
                        entry.getKey(), countyName(entry.getKey()), entry.getValue().size()))
            .toList());
    model.addAttribute("userDisplayName", displayName(authentication));
    return "school-year";
  }

  @GetMapping("/school-years/{schoolYear}/counties/{countyNumber}")
  public String county(
      @PathVariable String schoolYear,
      @PathVariable String countyNumber,
      Model model,
      Authentication authentication) {
    if (inspectionService.schoolYear(schoolYear).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    List<Submission> submissions =
        submissionsForSchoolYear(schoolYear).stream()
            .filter(submission -> countyNumber.equals(submission.school().countyNumber()))
            .toList();
    Map<String, List<Submission>> schools =
        submissions.stream()
            .collect(Collectors.groupingBy(submission -> submission.school().orgNumber()));
    model.addAttribute("schoolYear", schoolYear);
    model.addAttribute("countyNumber", countyNumber);
    model.addAttribute("countyName", countyName(countyNumber));
    model.addAttribute(
        "schools",
        schools.values().stream()
            .map(this::toSchoolView)
            .sorted(Comparator.comparing(SchoolView::schoolNumber))
            .toList());
    model.addAttribute("userDisplayName", displayName(authentication));
    return "county";
  }

  @GetMapping("/school-years/{schoolYear}/counties/{countyNumber}/schools/{orgNumber}")
  public String school(
      @PathVariable String schoolYear,
      @PathVariable String countyNumber,
      @PathVariable String orgNumber,
      Model model,
      Authentication authentication) {
    if (inspectionService.schoolYear(schoolYear).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    List<Submission> submissions =
        submissionsForSchoolYear(schoolYear).stream()
            .filter(submission -> countyNumber.equals(submission.school().countyNumber()))
            .filter(submission -> orgNumber.equals(submission.school().orgNumber()))
            .sorted(Comparator.comparing(Submission::submittedAt).reversed())
            .toList();
    if (submissions.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    model.addAttribute("schoolYear", schoolYear);
    model.addAttribute("countyNumber", countyNumber);
    model.addAttribute("countyName", countyName(countyNumber));
    model.addAttribute("school", submissions.getFirst().school());
    model.addAttribute("submissions", submissions);
    model.addAttribute("userDisplayName", displayName(authentication));
    return "school";
  }

  @GetMapping("/submissions/{submissionId}")
  public String submission(
      @PathVariable UUID submissionId, Model model, Authentication authentication) {
    Submission submission =
        inspectionService
            .submission(submissionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    model.addAttribute("submission", submission);
    model.addAttribute("countyName", countyName(submission.school().countyNumber()));
    model.addAttribute(
        "municipalityName", municipalityName(submission.school().municipalityNumber()));
    model.addAttribute("userDisplayName", displayName(authentication));
    return "submission";
  }

  @PostMapping("/submissions/{submissionId}/delete")
  public String deleteSubmission(@PathVariable UUID submissionId) {
    Submission submission =
        inspectionService
            .submission(submissionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    inspectionService.deleteSubmission(submissionId);
    return "redirect:/ui/school-years/"
        + submission.schoolYear()
        + "/counties/"
        + submission.school().countyNumber()
        + "/schools/"
        + submission.school().orgNumber();
  }

  private List<Submission> submissionsForSchoolYear(String schoolYear) {
    return inspectionService.submissions().stream()
        .filter(submission -> schoolYear.equals(submission.schoolYear()))
        .toList();
  }

  private void populateSchoolYearsModel(Model model, Authentication authentication) {
    List<Submission> submissions = inspectionService.submissions();
    Map<String, Long> counts =
        submissions.stream()
            .collect(Collectors.groupingBy(Submission::schoolYear, Collectors.counting()));
    model.addAttribute(
        "schoolYears",
        inspectionService.schoolYears().stream()
            .map(
                schoolYear ->
                    new SchoolYearView(
                        schoolYear.schoolYear(), counts.getOrDefault(schoolYear.schoolYear(), 0L)))
            .toList());
    model.addAttribute("userDisplayName", displayName(authentication));
  }

  private SubmissionWindowForm toWindowForm(SubmissionWindows submissionWindows) {
    return new SubmissionWindowForm(
        submissionWindows.graduatingStudents().from(),
        submissionWindows.graduatingStudents().to(),
        submissionWindows.finalGrades().from(),
        submissionWindows.finalGrades().to(),
        submissionWindows.examGrades().from(),
        submissionWindows.examGrades().to());
  }

  private SubmissionWindows submissionWindows(
      LocalDate graduatingStudentsFrom,
      LocalDate graduatingStudentsTo,
      LocalDate finalGradesFrom,
      LocalDate finalGradesTo,
      LocalDate examGradesFrom,
      LocalDate examGradesTo,
      String backHref,
      String backLabel,
      Model model,
      Authentication authentication) {
    SubmissionWindow graduatingStudents =
        validateWindow(
            graduatingStudentsFrom,
            graduatingStudentsTo,
            "Liste over avgangselever",
            backHref,
            backLabel,
            model,
            authentication);
    SubmissionWindow finalGrades =
        validateWindow(
            finalGradesFrom,
            finalGradesTo,
            "Standpunktkarakterer",
            backHref,
            backLabel,
            model,
            authentication);
    SubmissionWindow examGrades =
        validateWindow(
            examGradesFrom,
            examGradesTo,
            "Eksamenskarakterer",
            backHref,
            backLabel,
            model,
            authentication);
    if (graduatingStudents == null || finalGrades == null || examGrades == null) {
      return null;
    }
    return new SubmissionWindows(graduatingStudents, finalGrades, examGrades);
  }

  private SubmissionWindow validateWindow(
      LocalDate from,
      LocalDate to,
      String label,
      String backHref,
      String backLabel,
      Model model,
      Authentication authentication) {
    if (from == null || to == null) {
      schoolYearError(
          model,
          authentication,
          "Innsendingsperiode mangler datoer.",
          "Angi både fra-dato og til-dato for " + label + ".",
          backHref,
          backLabel);
      return null;
    }
    if (from.isAfter(to)) {
      schoolYearError(
          model,
          authentication,
          "Innsendingsperiode er ugyldig.",
          "Korriger " + label + " slik at fra-dato er lik eller tidligere enn til-dato.",
          backHref,
          backLabel);
      return null;
    }
    return new SubmissionWindow(from, to);
  }

  private String schoolYearError(
      Model model,
      Authentication authentication,
      String explanation,
      String correction,
      String backHref,
      String backLabel) {
    model.addAttribute("userDisplayName", displayName(authentication));
    model.addAttribute("errorExplanation", explanation);
    model.addAttribute("errorCorrection", correction);
    model.addAttribute("backHref", backHref);
    model.addAttribute("backLabel", backLabel);
    return "school-year-form-error";
  }

  private boolean isValidSchoolYear(String schoolYear) {
    if (schoolYear == null || !SCHOOL_YEAR_PATTERN.matcher(schoolYear).matches()) {
      return false;
    }
    String[] parts = schoolYear.split("-");
    int startYear = Integer.parseInt(parts[0]);
    int endYear = Integer.parseInt(parts[1]);
    return endYear == startYear + 1;
  }

  private String schoolYearForm(
      Model model,
      Authentication authentication,
      String pageTitle,
      String subtitle,
      String submitLabel,
      String formAction,
      String schoolYearTitle,
      String schoolYearValue,
      boolean schoolYearReadonly,
      SubmissionWindowForm windowForm,
      String cancelHref,
      String cancelLabel) {
    model.addAttribute("pageTitle", pageTitle);
    model.addAttribute("pageSubtitle", subtitle);
    model.addAttribute("submitLabel", submitLabel);
    model.addAttribute("formAction", formAction);
    model.addAttribute("schoolYearTitle", schoolYearTitle);
    model.addAttribute("schoolYearValue", schoolYearValue);
    model.addAttribute("schoolYearReadonly", schoolYearReadonly);
    model.addAttribute("windowForm", windowForm);
    model.addAttribute("cancelHref", cancelHref);
    model.addAttribute("cancelLabel", cancelLabel);
    model.addAttribute("userDisplayName", displayName(authentication));
    return "school-year-new";
  }

  private SchoolView toSchoolView(List<Submission> submissions) {
    Submission latest =
        submissions.stream().max(Comparator.comparing(Submission::submittedAt)).orElseThrow();
    return new SchoolView(
        latest.school().orgNumber(),
        latest.school().schoolNumber(),
        latest.school().name(),
        latest.status().name(),
        submissions.size());
  }

  private String displayName(Principal principal) {
    return principal == null ? "Ikke innlogget" : principal.getName();
  }

  private String countyName(String countyNumber) {
    return schoolDirectoryPort.findCountyShortName(countyNumber).orElse(countyNumber);
  }

  private String municipalityName(String municipalityNumber) {
    return schoolDirectoryPort.findMunicipalityName(municipalityNumber).orElse(municipalityNumber);
  }

  public record CountyView(String countyNumber, String countyName, int submissions) {}

  public record SubmissionWindowForm(
      LocalDate graduatingStudentsFrom,
      LocalDate graduatingStudentsTo,
      LocalDate finalGradesFrom,
      LocalDate finalGradesTo,
      LocalDate examGradesFrom,
      LocalDate examGradesTo) {}

  public record SchoolYearView(String schoolYear, long submissions) {}

  public record SchoolView(
      String orgNumber, String schoolNumber, String schoolName, String status, int submissions) {}
}
