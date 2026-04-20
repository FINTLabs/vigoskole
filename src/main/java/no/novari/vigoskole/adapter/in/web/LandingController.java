package no.novari.vigoskole.adapter.in.web;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LandingController {

  private final boolean devProfileActive;

  public LandingController(Environment environment) {
    this.devProfileActive = environment.acceptsProfiles(Profiles.of("dev"));
  }

  @GetMapping("/")
  public String root(Authentication authentication, Model model) {
    model.addAttribute("authenticated", isAuthenticated(authentication));
    return "landing";
  }

  @GetMapping("/login")
  public String login(
      Authentication authentication,
      @RequestParam(defaultValue = "false") boolean error,
      @RequestParam(defaultValue = "false") boolean logout,
      Model model) {
    if (isAuthenticated(authentication)) {
      return "redirect:/ui/school-years";
    }
    model.addAttribute("error", error);
    model.addAttribute("logout", logout);
    model.addAttribute("useFormLogin", devProfileActive);
    model.addAttribute("oidcLoginPath", "/oauth2/authorization/idporten");
    return "login";
  }

  private boolean isAuthenticated(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }
}
