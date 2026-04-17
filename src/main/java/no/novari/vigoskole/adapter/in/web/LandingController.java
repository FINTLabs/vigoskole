package no.novari.vigoskole.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

  @GetMapping("/")
  public String root() {
    return "redirect:/ui/school-years";
  }
}
