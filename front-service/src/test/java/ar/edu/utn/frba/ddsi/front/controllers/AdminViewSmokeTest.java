package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.incentivos.IncentivosAdminDTOs;
import ar.edu.utn.frba.ddsi.front.services.AdminResumenService;
import ar.edu.utn.frba.ddsi.front.services.SeguimientoTokenService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @MockBean
    private AdminResumenService adminResumenService;

    @MockBean
    private SeguimientoTokenService seguimientoTokenService;

    @Test
    void adminViewsRenderWithoutBrokenTemplates() throws Exception {
        UUID idMision = UUID.randomUUID();
        IncentivosAdminDTOs.Mision mision = new IncentivosAdminDTOs.Mision();
        mision.setIdMision(idMision);
        mision.setNombreMision("Primera misión");
        mision.setDescripcion("Descripción");

        IncentivosAdminDTOs.Categoria categoria = new IncentivosAdminDTOs.Categoria();
        categoria.setIdCategoria(UUID.randomUUID());
        categoria.setNombre("Principiantes");
        categoria.setPosicionSecuencia(1);
        categoria.setMisiones(List.of(idMision));

        when(donacionesClient.obtenerDonantes()).thenReturn(List.of());
        when(donacionesClient.obtenerEntidades()).thenReturn(List.of());
        when(donacionesClient.obtenerTodasLasDonaciones()).thenReturn(List.of());
        when(incentivosClient.obtenerMisiones()).thenReturn(List.of(mision));
        when(incentivosClient.obtenerCategorias()).thenReturn(List.of(categoria));
        when(logisticasClient.obtenerCamiones()).thenReturn(List.of());
        when(logisticasClient.obtenerRutas()).thenReturn(List.of());
        when(logisticasClient.obtenerEntregasNoRecibidas()).thenReturn(null);
        when(logisticasClient.obtenerRutasActivas()).thenReturn(List.of());

        when(donacionesClient.obtenerResultadosMatchmaking()).thenReturn(List.of());
        when(adminResumenService.obtenerResumen()).thenReturn(new AdminResumenService.Resumen());

        for (String path : List.of("/admin", "/admin/donaciones", "/admin/donaciones?vista=matchmaking",
                "/admin/entidades", "/admin/donantes", "/admin/donaciones/nuevo",
                "/admin/donantes/nuevo", "/admin/entidades/nuevo", "/admin/camiones",
                "/admin/camiones/nuevo", "/admin/choferes", "/admin/choferes/nuevo",
                "/admin/rutas", "/admin/entregas-no-recibidas",
                "/admin/donaciones?vista=vencidas", "/admin/donantes/importar/" + java.util.UUID.randomUUID(),
                "/admin/incentivos")) {
            mockMvc.perform(get(path).session(sessionAdmin()))
                    .andExpect(status().isOk());
        }

        String incentivesPage = mockMvc.perform(get("/admin/incentivos").session(sessionAdmin()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(incentivesPage).containsPattern(
                "(?s)<form method=\"post\" id=\"deleteForm\" action=\"#\">\\s*"
                        + "<input type=\"hidden\" name=\"_csrf\" value=\"[^\"]+\"");
        assertThat(incentivesPage).contains("src=\"/js/core/toast.js\"");
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
