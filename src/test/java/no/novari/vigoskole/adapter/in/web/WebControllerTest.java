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
import no.novari.vigoskole.application.SchoolYearRepository;
import no.novari.vigoskole.application.SubmissionRepository;
import no.novari.vigoskole.domain.model.SchoolInfo;
import no.novari.vigoskole.domain.model.Submission;
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
  @Autowired private SubmissionRepository submissionRepository;
  @Autowired private SchoolYearRepository schoolYearRepository;

  private MockMvc mockMvc;
  private Submission submission;

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
    schoolYearRepository.save(TestData.schoolYearConfiguration());
    submission = TestData.submission(Submission.Status.ACCEPTED_WITH_WARNINGS);
    submissionRepository.save(submission);
  }

  @Test
  void shouldRenderSchoolYearOverview() throws Exception {
    mockMvc
        .perform(get("/ui/school-years").with(user("fylkesbruker")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Fake Vigo Skole")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Innsendinger for skoleår")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "href=\"/ui/school-years/new\">Opprett skoleår</a>")))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.containsString("cropped-novari_favicon-32x32.png")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "<a href=\"/ui/school-years/2025-2026\">2025-2026</a>")));
  }

  @Test
  void shouldRenderCreateSchoolYearPage() throws Exception {
    mockMvc
        .perform(get("/ui/school-years/new").with(user("fylkesbruker")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Opprett skoleår")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Liste over avgangselever")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Standpunktkarakterer")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Eksamenskarakterer")));
  }

  @Test
  void shouldRenderEditSchoolYearPage() throws Exception {
    mockMvc
        .perform(get("/ui/school-years/2025-2026/edit").with(user("fylkesbruker")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Rediger skoleår")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Lagre endringer")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"2025-2026\"")));
  }

  @Test
  void shouldCreateSchoolYearWithConfiguredWindows() throws Exception {
    mockMvc
        .perform(
            post("/ui/school-years")
                .with(user("fylkesbruker"))
                .with(csrf())
                .param("schoolYear", "2026-2027")
                .param("graduatingStudentsFrom", "2026-01-10")
                .param("graduatingStudentsTo", "2026-05-20")
                .param("finalGradesFrom", "2026-05-21")
                .param("finalGradesTo", "2026-06-15")
                .param("examGradesFrom", "2026-06-16")
                .param("examGradesTo", "2026-07-01"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ui/school-years"));

    assert schoolYearRepository.findBySchoolYear("2026-2027").isPresent();
  }

  @Test
  void shouldUpdateSchoolYearWindows() throws Exception {
    mockMvc
        .perform(
            post("/ui/school-years/2025-2026/edit")
                .with(user("fylkesbruker"))
                .with(csrf())
                .param("graduatingStudentsFrom", "2026-01-15")
                .param("graduatingStudentsTo", "2026-05-15")
                .param("finalGradesFrom", "2026-05-16")
                .param("finalGradesTo", "2026-06-10")
                .param("examGradesFrom", "2026-06-11")
                .param("examGradesTo", "2026-06-25"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ui/school-years/2025-2026"));

    org.assertj.core.api.Assertions.assertThat(
            schoolYearRepository
                .findBySchoolYear("2025-2026")
                .orElseThrow()
                .submissionWindows()
                .examGrades()
                .to())
        .isEqualTo(java.time.LocalDate.of(2026, 6, 25));
  }

  @Test
  void shouldDeleteSubmission() throws Exception {
    mockMvc
        .perform(
            post("/ui/submissions/" + submission.id() + "/delete")
                .with(user("fylkesbruker"))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            redirectedUrl(
                "/ui/school-years/2025-2026/counties/32/schools/" + TestData.SCHOOL_ORG_NUMBER));

    org.assertj.core.api.Assertions.assertThat(submissionRepository.findById(submission.id()))
        .isEmpty();
  }

  @Test
  void shouldDeleteSchoolSubmissions() throws Exception {
    mockMvc
        .perform(
            post("/ui/school-years/2025-2026/counties/32/schools/"
                    + TestData.SCHOOL_ORG_NUMBER
                    + "/delete")
                .with(user("fylkesbruker"))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ui/school-years/2025-2026/counties/32"));

    org.assertj.core.api.Assertions.assertThat(submissionRepository.findAll()).isEmpty();
  }

  @Test
  void shouldDeleteCountySubmissions() throws Exception {
    mockMvc
        .perform(
            post("/ui/school-years/2025-2026/counties/32/delete")
                .with(user("fylkesbruker"))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ui/school-years/2025-2026"));

    org.assertj.core.api.Assertions.assertThat(submissionRepository.findAll()).isEmpty();
  }

  @Test
  void shouldDeleteSchoolYearAndCascadeDeleteSubmissions() throws Exception {
    mockMvc
        .perform(post("/ui/school-years/2025-2026/delete").with(user("fylkesbruker")).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ui/school-years"));

    org.assertj.core.api.Assertions.assertThat(schoolYearRepository.findBySchoolYear("2025-2026"))
        .isEmpty();
    org.assertj.core.api.Assertions.assertThat(submissionRepository.findAll()).isEmpty();
  }

  @Test
  void shouldRenderSubmissionHierarchy() throws Exception {
    mockMvc
        .perform(get("/ui/school-years/2025-2026/counties/32").with(user("fylkesbruker")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Akershus")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Ås ungdomsskole")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString("Slett alle innsendinger for fylket")));
  }

  @Test
  void shouldRenderSchoolYearManagementPage() throws Exception {
    mockMvc
        .perform(get("/ui/school-years/2025-2026").with(user("fylkesbruker")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Rediger skoleår")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "Fylkeskommuner og innsendinger for valgt skoleår.")));
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
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Akershus")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Slett denne innsendingen")));
  }

  @Test
  void shouldShowErrorPageWhenOneDateIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/ui/school-years")
                .with(user("fylkesbruker"))
                .with(csrf())
                .param("schoolYear", "2027-2028")
                .param("graduatingStudentsFrom", "2026-01-10"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Feil i skoleårskonfigurasjon")))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.containsString("Innsendingsperiode mangler datoer.")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "href=\"/ui/school-years/new\">Tilbake til opprettelse av skoleår</a>")));
  }

  @Test
  void shouldShowErrorPageWhenSchoolYearIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/ui/school-years")
                .with(user("fylkesbruker"))
                .with(csrf())
                .param("schoolYear", "2026-2028")
                .param("graduatingStudentsFrom", "2026-01-10")
                .param("graduatingStudentsTo", "2026-05-20")
                .param("finalGradesFrom", "2026-05-21")
                .param("finalGradesTo", "2026-06-15")
                .param("examGradesFrom", "2026-06-16")
                .param("examGradesTo", "2026-07-01"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Skoleår er ugyldig.")));
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
