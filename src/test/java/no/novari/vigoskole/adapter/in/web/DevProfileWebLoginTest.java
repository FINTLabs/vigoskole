package no.novari.vigoskole.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("dev")
class DevProfileWebLoginTest {

  @Autowired private WebApplicationContext applicationContext;

  private MockMvc mockMvc;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("app.storage-directory", DevProfileWebLoginTest::newStorageDirectory);
  }

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(applicationContext)
            .apply(
                org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                    .springSecurity())
            .build();
  }

  @Test
  void shouldRedirectToLoginWhenWebUserIsAnonymous() throws Exception {
    mockMvc.perform(get("/ui/school-years")).andExpect(status().is3xxRedirection());
  }

  @Test
  void shouldRenderMainPageWithNovariBrandingAndUtf8() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().encoding("UTF-8"))
        .andExpect(content().string(containsString("Fake Vigo Skole")))
        .andExpect(content().string(containsString("novari_logo_primaer.svg")))
        .andExpect(content().string(containsString("cropped-novari_favicon-32x32.png")));
  }

  @Test
  void shouldRenderCustomLoginPageWithNovariBrandingAndUtf8() throws Exception {
    mockMvc
        .perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(content().encoding("UTF-8"))
        .andExpect(content().string(containsString("Fake Vigo Skole")))
        .andExpect(content().string(containsString("novari_logo_primaer.svg")))
        .andExpect(content().string(containsString("name=\"username\"")))
        .andExpect(content().string(containsString("cropped-novari_favicon-32x32.png")));
  }

  @Test
  void shouldAllowFormLoginWithoutOidcInDevProfile() throws Exception {
    MvcResult loginResult =
        mockMvc
            .perform(formLogin().user("dev").password("devpass"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/ui/school-years"))
            .andExpect(authenticated().withUsername("dev"))
            .andReturn();

    mockMvc
        .perform(
            get("/ui/school-years")
                .session(
                    (org.springframework.mock.web.MockHttpSession)
                        Objects.requireNonNull(loginResult.getRequest().getSession(false))))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("dev")));
  }

  private static String newStorageDirectory() {
    try {
      Path storageDirectory = Files.createTempDirectory("vigoskole-dev-web-login-test");
      return storageDirectory.toString();
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
