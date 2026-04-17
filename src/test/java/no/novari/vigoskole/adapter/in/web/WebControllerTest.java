package no.novari.vigoskole.adapter.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import no.novari.vigoskole.TestData;
import no.novari.vigoskole.application.SchoolDirectoryPort;
import no.novari.vigoskole.application.SubmissionRepository;
import no.novari.vigoskole.application.SubmissionWindowRepository;
import no.novari.vigoskole.domain.model.SchoolInfo;
import no.novari.vigoskole.domain.model.Submission;
import no.novari.vigoskole.domain.model.SubmissionWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@DirtiesContext
class WebControllerTest {

  @Autowired private WebApplicationContext applicationContext;

  private MockMvc mockMvc;
  private Submission submission;

  @Autowired private SubmissionRepository submissionRepository;
  @Autowired private SubmissionWindowRepository submissionWindowRepository;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("app.storage-directory", WebControllerTest::newStorageDirectory);
  }

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(applicationContext)
            .apply(
                org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                    .springSecurity())
            .build();
    submissionWindowRepository.save(
        new SubmissionWindow(
            java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 12, 31)));
    submission = TestData.submission(Submission.Status.ACCEPTED_WITH_WARNINGS);
    submissionRepository.save(submission);
  }

  @Test
  void shouldRenderSubmissionHierarchy() throws Exception {
    mockMvc
        .perform(get("/ui/school-years/2025-2026/counties/32").with(user("fylkesbruker")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Akershus")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Ås ungdomsskole")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("ACCEPTED_WITH_WARNINGS")));
  }

  @Test
  void shouldRenderCountyShortNameOnSchoolYearOverview() throws Exception {
    mockMvc
        .perform(get("/ui/school-years/2025-2026").with(user("fylkesbruker")))
        .andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("<table class=\"table\">")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "<a href=\"/ui/school-years/2025-2026/counties/32\">Akershus</a>")));
  }

  @Test
  void shouldUpdateSubmissionWindow() throws Exception {
    mockMvc
        .perform(
            post("/ui/submission-window")
                .with(user("fylkesbruker"))
                .with(csrf())
                .param("from", "2026-02-01")
                .param("to", "2026-06-01"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ui/school-years"));
  }

  @Test
  void shouldRenderCountyAndMunicipalityNamesOnSubmissionPage() throws Exception {
    mockMvc
        .perform(get("/ui/submissions/" + submission.id()).with(user("fylkesbruker")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("16/04/2026 12:15")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("150491 00008")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Kommune:")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ÅS")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Fylke:")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Akershus")));
  }

  @Test
  void shouldShowConfiguredSubmissionWindowOnOverview() throws Exception {
    mockMvc
        .perform(get("/ui/school-years").with(user("fylkesbruker")))
        .andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("<table class=\"table\">")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "<a href=\"/ui/school-years/2025-2026\">2025-2026</a>")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("null innsendinger"))))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Nåværende konfigurert periode")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("01/01/2026")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("31/12/2026")));
  }

  @Test
  void shouldShowErrorPageWhenOneDateIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/ui/submission-window").with(user("fylkesbruker")).with(csrf()).param("from", ""))
        .andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Feil i innsendingsperiode")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString("startdato og sluttdato må være satt")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("01/01/2026")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("31/12/2026")));
  }

  @Test
  void shouldShowErrorPageWhenFromDateIsAfterToDate() throws Exception {
    mockMvc
        .perform(
            post("/ui/submission-window")
                .with(user("fylkesbruker"))
                .with(csrf())
                .param("from", "2026-07-01")
                .param("to", "2026-06-01"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Feil i innsendingsperiode")))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.containsString("fra-dato er satt etter til-dato")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "fra-dato er lik eller tidligere enn til-dato")));
  }

  private static String newStorageDirectory() {
    try {
      Path storageDirectory = Files.createTempDirectory("vigoskole-web-test");
      return storageDirectory.toString();
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @TestConfiguration
  static class SchoolDirectoryTestConfiguration {

    @Bean
    @Primary
    SchoolDirectoryPort schoolDirectoryPort() {
      return new SchoolDirectoryPort() {
        @Override
        public Optional<SchoolInfo> findLowerSecondarySchool(String orgNumber) {
          return Optional.of(TestData.schoolInfo());
        }

        @Override
        public Optional<String> findCountyShortName(String countyNumber) {
          return "32".equals(countyNumber) ? Optional.of("Akershus") : Optional.empty();
        }

        @Override
        public Optional<String> findMunicipalityName(String municipalityNumber) {
          return "3218".equals(municipalityNumber) ? Optional.of("ÅS") : Optional.empty();
        }
      };
    }
  }
}
