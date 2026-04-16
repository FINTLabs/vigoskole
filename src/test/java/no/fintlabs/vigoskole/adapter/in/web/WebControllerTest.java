package no.fintlabs.vigoskole.adapter.in.web;

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
import no.fintlabs.vigoskole.TestData;
import no.fintlabs.vigoskole.application.SubmissionRepository;
import no.fintlabs.vigoskole.domain.model.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

  @Autowired private SubmissionRepository submissionRepository;

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
    submissionRepository.save(TestData.submission(Submission.Status.ACCEPTED_WITH_WARNINGS));
  }

  @Test
  void shouldRenderSubmissionHierarchy() throws Exception {
    mockMvc
        .perform(get("/ui/school-years/2025-2026/counties/03").with(user("fylkesbruker")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Test ungdomsskole")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("ACCEPTED_WITH_WARNINGS")));
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

  private static String newStorageDirectory() {
    try {
      Path storageDirectory = Files.createTempDirectory("vigoskole-web-test");
      return storageDirectory.toString();
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
