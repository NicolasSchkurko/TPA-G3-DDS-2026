package ar.edu.utn.frba.ddsi.front.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

public class KeycloakLogoutSuccessHandler implements LogoutSuccessHandler {
    private final String clientId;
    private final String logoutUri;
    private final String postLogoutRedirectUri;

    public KeycloakLogoutSuccessHandler(String clientId, String logoutUri, String postLogoutRedirectUri) {
        this.clientId = clientId;
        this.logoutUri = logoutUri;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        UriComponentsBuilder redirect = UriComponentsBuilder.fromUriString(logoutUri)
                .queryParam("client_id", clientId)
                .queryParam("post_logout_redirect_uri", postLogoutRedirectUri);
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            redirect.queryParam("id_token_hint", oidcUser.getIdToken().getTokenValue());
        }
        response.sendRedirect(redirect.encode().build().toUriString());
    }
}
