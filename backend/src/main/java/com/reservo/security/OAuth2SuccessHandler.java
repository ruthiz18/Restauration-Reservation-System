package com.reservo.security;

import com.reservo.domain.User;
import com.reservo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * After a successful Google (OAuth2/OIDC) login, link or create the local account and hand the SPA a
 * first-party JWT. The token travels in the URL fragment so it is never sent to a server or logged.
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService authService;
    private final JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        if (email == null) {
            response.sendRedirect(frontendUrl + "/login?error=oauth_email");
            return;
        }
        User user = authService.findOrCreateOAuthUser(email, name);
        if (!user.isEnabled()) {
            response.sendRedirect(frontendUrl + "/login?error=disabled");
            return;
        }
        String token = jwtService.generate(user);
        response.sendRedirect(frontendUrl + "/oauth2/callback#token=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
    }
}
