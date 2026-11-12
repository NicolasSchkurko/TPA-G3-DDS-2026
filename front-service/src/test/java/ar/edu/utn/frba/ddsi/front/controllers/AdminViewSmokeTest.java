package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminViewSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DonacionesClient donacionesClient;

    @MockBean
    private IncentivosClient incentivosClient;

    @MockBean
    private LogisticasClient logisticasClient;

    @Test
    void adminViewsRenderWithoutBrokenTemplates() throws Exception {
        when(donacionesClient.obtenerDonantes()).thenReturn(List.of());
        when(donacionesClient.obtenerEntidades()).thenReturn(List.of());
        when(donacionesClient.obtenerTodasLasDonaciones()).thenReturn(List.of());
        when(logisticasClient.obtenerCamiones()).thenReturn(List.of());

        when(donacionesClient.obtenerResultadosMatchmaking()).thenReturn(List.of());

        for (String path : List.of("/admin", "/admin/donaciones", "/admin/donaciones?vista=matchmaking",
                "/admin/entidades", "/admin/donantes", "/admin/donaciones/nuevo",
                "/admin/donantes/nuevo", "/admin/entidades/nuevo", "/camiones")) {
            mockMvc.perform(get(path).session(sessionAdmin()))
                    .andExpect(status().isOk());
        }
    }

    private static MockHttpSession sessionAdmin() {
        OidcUser user = new OidcUser() {
            @Override public Map<String, Object> getClaims() { return Map.of("email", "admin@ddsi.local"); }
            @Override public OidcUserInfo getUserInfo() { return null; }
            @Override public OidcIdToken getIdToken() { return null; }
            @Override public Map<String, Object> getAttributes() { return getClaims(); }
            @Override public List<SimpleGrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            @Override public String getName() { return "admin@ddsi.local"; }
        };
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
