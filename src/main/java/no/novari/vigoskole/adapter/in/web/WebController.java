package no.novari.vigoskole.adapter.in.web;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import no.novari.vigoskole.application.InspectionService;
import no.novari.vigoskole.domain.model.Submission;
import no.novari.vigoskole.domain.model.SubmissionWindow;
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

  private final InspectionService inspectionService;

  public WebController(InspectionService inspectionService) {
    this.inspectionService = inspectionService;
  }

  @GetMapping("/school-years")
  public String schoolYears(Model model, Authentication authentication) {
    populateSchoolYearsModel(model, authentication);
    return "school-years";
  }

  @PostMapping("/submission-window")
  public String updateSubmissionWindow(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      Model model,
      Authentication authentication) {
    if (from == null || to == null) {
      return submissionWindowError(
          model,
          authentication,
          "Innsendingsperioden er ugyldig fordi startdato og sluttdato må være satt.",
          "Angi både fra-dato og til-dato før perioden kan lagres.");
    }

    if (from.isAfter(to)) {
      return submissionWindowError(
          model,
          authentication,
          "Innsendingsperioden er ugyldig fordi fra-dato er satt etter til-dato.",
          "Korriger perioden slik at fra-dato er lik eller tidligere enn til-dato.");
    }

    inspectionService.updateSubmissionWindow(new SubmissionWindow(from, to));
    return "redirect:/ui/school-years";
  }

  @GetMapping("/school-years/{schoolYear}")
  public String schoolYear(
      @PathVariable String schoolYear, Model model, Authentication authentication) {
    List<Submission> submissions = submissionsForSchoolYear(schoolYear);
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
                        entry.getKey(),
                        entry.getValue().getFirst().school().countyNumber(),
                        entry.getValue().size()))
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
    List<Submission> submissions =
        submissionsForSchoolYear(schoolYear).stream()
            .filter(submission -> countyNumber.equals(submission.school().countyNumber()))
            .toList();
    Map<String, List<Submission>> schools =
        submissions.stream()
            .collect(Collectors.groupingBy(submission -> submission.school().orgNumber()));
    model.addAttribute("schoolYear", schoolYear);
    model.addAttribute("countyNumber", countyNumber);
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
    model.addAttribute("userDisplayName", displayName(authentication));
    return "submission";
  }

  private List<Submission> submissionsForSchoolYear(String schoolYear) {
    return inspectionService.submissions().stream()
        .filter(submission -> schoolYear.equals(submission.schoolYear()))
        .toList();
  }

  private String submissionWindowError(
      Model model, Authentication authentication, String explanation, String correction) {
    model.addAttribute("submissionWindow", inspectionService.submissionWindow());
    model.addAttribute("userDisplayName", displayName(authentication));
    model.addAttribute("errorExplanation", explanation);
    model.addAttribute("errorCorrection", correction);
    return "submission-window-error";
  }

  private void populateSchoolYearsModel(Model model, Authentication authentication) {
    List<Submission> submissions = inspectionService.submissions();
    Map<String, Long> counts =
        submissions.stream()
            .collect(Collectors.groupingBy(Submission::schoolYear, Collectors.counting()));
    model.addAttribute("schoolYears", inspectionService.schoolYears());
    model.addAttribute("submissionCounts", counts);
    model.addAttribute("submissionWindow", inspectionService.submissionWindow());
    model.addAttribute("userDisplayName", displayName(authentication));
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

  public record CountyView(String countyNumber, String countyName, int submissions) {}

  public record SchoolView(
      String orgNumber, String schoolNumber, String schoolName, String status, int submissions) {}
}
