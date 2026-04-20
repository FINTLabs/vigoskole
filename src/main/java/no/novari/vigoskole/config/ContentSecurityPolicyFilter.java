package no.novari.vigoskole.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.web.filter.OncePerRequestFilter;

final class ContentSecurityPolicyFilter extends OncePerRequestFilter {

  static final String CSP_NONCE_ATTRIBUTE = "cspNonce";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String nonce = generateNonce();
    request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce);
    response.setHeader("Content-Security-Policy", buildPolicy(nonce));
    filterChain.doFilter(request, response);
  }

  private String generateNonce() {
    byte[] bytes = new byte[16];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String buildPolicy(String nonce) {
    return "default-src 'none'; "
        + "base-uri 'self'; "
        + "form-action 'self'; "
        + "frame-ancestors 'none'; "
        + "object-src 'none'; "
        + "img-src 'self' https://novari.no data:; "
        + "style-src 'self' 'nonce-"
        + nonce
        + "'; "
        + "script-src 'self' 'nonce-"
        + nonce
        + "'; "
        + "font-src 'self'; "
        + "connect-src 'self'; "
        + "manifest-src 'self';";
  }
}
