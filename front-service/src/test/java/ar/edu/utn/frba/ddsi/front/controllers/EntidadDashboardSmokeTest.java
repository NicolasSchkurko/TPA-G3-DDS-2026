package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.SeguimientoDTO;
import ar.edu.utn.frba.ddsi.front.services.EntidadContext;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EntidadDashboardSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EntidadContext entidadContext;

    @Test
    void dashboardExponeLosDatosDePreviewDeLasDonaciones() throws Exception {
        UUID idEntidad = UUID.randomUUID();
        UUID idDonacion = UUID.randomUUID();
        EntidadBeneficiariaDTO entidad = entidad(idEntidad);
        DonacionDTO donacion = new DonacionDTO();
        donacion.setId(idDonacion);
        donacion.setDescripcion("Alimentos para el comedor");
        donacion.setEstado("ENTREGADO");

        SeguimientoDTO seguimiento = new SeguimientoDTO();
        when(entidadContext.entidad(any(), any())).thenReturn(entidad);
        when(entidadContext.entidadId(any(), any())).thenReturn(idEntidad);
        when(entidadContext.donaciones(any(), any())).thenReturn(List.of(donacion));
        when(entidadContext.entregasPorDonacion(any())).thenReturn(Map.of());
        when(entidadContext.seguimiento(any(), any())).thenReturn(seguimiento);

        mockMvc.perform(get("/entidad").session(sessionEntidad()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("ENTIDAD_DONACIONES")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString(idDonacion.toString())));

        mockMvc.perform(get("/entidad/donaciones").session(sessionEntidad()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString(
                                "href=\"/entidad?donacion=" + idDonacion + "\"")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Ver la donación en el mapa")));
    }

    private static EntidadBeneficiariaDTO entidad(UUID id) {
        EntidadBeneficiariaDTO entidad = new EntidadBeneficiariaDTO();
        entidad.setId(id);
        entidad.setRazonSocial("Comedor Los Piletones");
        entidad.setRubro("Asistencia social");
        DireccionDTO direccion = new DireccionDTO();
        direccion.setCalleUno("Av. Perito Moreno");
        direccion.setAltura(2400);
        direccion.setCiudad("Villa Soldati");
        direccion.setLatitud(-34.6538);
        direccion.setLongitud(-58.4478);
        entidad.setDireccion(direccion);
        return entidad;
    }

    private static MockHttpSession sessionEntidad() {
        OidcUser user = new OidcUser() {
            @Override public Map<String, Object> getClaims() { return Map.of("email", "entidad@ddsi.local"); }
            @Override public OidcUserInfo getUserInfo() { return null; }
            @Override public OidcIdToken getIdToken() { return null; }
            @Override public Map<String, Object> getAttributes() { return getClaims(); }
            @Override public List<SimpleGrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_ENTIDAD"));
            }
            @Override public String getName() { return "entidad@ddsi.local"; }
        };
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "password", List.of(new SimpleGrantedAuthority("ROLE_ENTIDAD"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
