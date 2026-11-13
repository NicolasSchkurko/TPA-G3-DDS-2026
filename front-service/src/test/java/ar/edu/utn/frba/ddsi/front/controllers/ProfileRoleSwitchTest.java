package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.ChoferDTO;
import ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO;
import ar.edu.utn.frba.ddsi.front.dto.PersonaDonanteDTO;
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
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileRoleSwitchTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DonacionesClient donacionesClient;

    @MockBean
    private IncentivosClient incentivosClient;

    @MockBean
    private LogisticasClient logisticasClient;

    @Test
    void rolActivoChoferMuestraPerfilChofer() throws Exception {
        UUID donorId = UUID.randomUUID();
        IdentidadDTO identidad = new IdentidadDTO();
        identidad.setDonanteId(donorId);
        identidad.setRoles(List.of("DONANTE", "CHOFER"));
        when(donacionesClient.buscarIdentidadPorEmail(anyString())).thenReturn(identidad);

        PersonaDonanteDTO donante = new PersonaDonanteDTO();
        donante.setId(donorId);
        donante.setActivo(true);
        donante.setNombre("Donante");
        donante.setApellido("DDSI");
        when(donacionesClient.obtenerDonantePorId(donorId)).thenReturn(donante);

        ChoferDTO chofer = new ChoferDTO();
        chofer.setIdChofer(UUID.randomUUID());
        chofer.setNombre("Chofer DDSI");
        chofer.setEmail("user@ddsi.local");
        chofer.setActivo(true);
        when(logisticasClient.obtenerChoferes()).thenReturn(List.of(chofer));

        when(incentivosClient.obtenerPerfilPublico(anyString())).thenReturn(Map.of());
        when(incentivosClient.obtenerMision(anyString())).thenReturn(Map.of());
        when(incentivosClient.obtenerInsignias(anyString())).thenReturn(List.of());
        when(incentivosClient.obtenerPuestoRanking(anyString())).thenReturn(Map.of());
        when(incentivosClient.obtenerRankingActual()).thenReturn(Map.of());

        MockHttpSession session = sessionMulti("CHOFER");
        mockMvc.perform(get("/perfil").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chofer DDSI")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("profile-role-chip")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"next\" value=\"/perfil\"")));
    }

    @Test
    void rolActivoDonanteMuestraPerfilDonante() throws Exception {
        UUID donorId = UUID.randomUUID();
        IdentidadDTO identidad = new IdentidadDTO();
        identidad.setDonanteId(donorId);
        identidad.setRoles(List.of("DONANTE", "CHOFER"));
        when(donacionesClient.buscarIdentidadPorEmail(anyString())).thenReturn(identidad);

        PersonaDonanteDTO donante = new PersonaDonanteDTO();
        donante.setId(donorId);
        donante.setActivo(true);
        donante.setNombre("Donante");
        donante.setApellido("DDSI");
        when(donacionesClient.obtenerDonantePorId(donorId)).thenReturn(donante);

        ChoferDTO chofer = new ChoferDTO();
        chofer.setIdChofer(UUID.randomUUID());
        chofer.setNombre("Chofer DDSI");
        chofer.setEmail("user@ddsi.local");
        chofer.setActivo(true);
        when(logisticasClient.obtenerChoferes()).thenReturn(List.of(chofer));

        when(incentivosClient.obtenerPerfilPublico(anyString())).thenReturn(Map.of());
        when(incentivosClient.obtenerMision(anyString())).thenReturn(Map.of());
        when(incentivosClient.obtenerInsignias(anyString())).thenReturn(List.of());
        when(incentivosClient.obtenerPuestoRanking(anyString())).thenReturn(Map.of());
        when(incentivosClient.obtenerRankingActual()).thenReturn(Map.of());

        MockHttpSession session = sessionMulti("DONANTE");
        mockMvc.perform(get("/perfil").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Donante DDSI")));
    }

    @Test
    void cambiarRolConNextVuelveAlPerfil() {
        MockHttpSession session = new MockHttpSession();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", "password", List.of(new SimpleGrantedAuthority("ROLE_DONANTE")));

        String view = new RolController().cambiar("DONANTE", "/perfil", session, auth, new ExtendedModelMap());

        org.junit.jupiter.api.Assertions.assertEquals("redirect:/perfil", view);
        org.junit.jupiter.api.Assertions.assertEquals("DONANTE", session.getAttribute("rolActivo"));
    }

    @Test
    void cambiarRolConNextInseguroUsaDestinoDelRol() {
        MockHttpSession session = new MockHttpSession();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", "password", List.of(new SimpleGrantedAuthority("ROLE_DONANTE")));

        String view = new RolController().cambiar("DONANTE", "//evil.example", session, auth, new ExtendedModelMap());

        org.junit.jupiter.api.Assertions.assertEquals("redirect:/donante", view);
    }

    @Test
    void choferPuedeAbrirEdicionDeSuPerfil() throws Exception {
        UUID donorId = UUID.randomUUID();
        IdentidadDTO identidad = new IdentidadDTO();
        identidad.setDonanteId(donorId);
        identidad.setRoles(List.of("DONANTE", "CHOFER"));
        when(donacionesClient.buscarIdentidadPorEmail(anyString())).thenReturn(identidad);

        PersonaDonanteDTO donante = new PersonaDonanteDTO();
        donante.setId(donorId);
        donante.setActivo(true);
        donante.setNombre("Donante");
        donante.setApellido("DDSI");
        when(donacionesClient.obtenerDonantePorId(donorId)).thenReturn(donante);

        ChoferDTO chofer = new ChoferDTO();
        chofer.setIdChofer(UUID.randomUUID());
        chofer.setNombre("Chofer DDSI");
        chofer.setEmail("user@ddsi.local");
        chofer.setActivo(true);
        when(logisticasClient.obtenerChoferes()).thenReturn(List.of(chofer));

        MockHttpSession session = sessionMulti("CHOFER");
        mockMvc.perform(get("/perfil/chofer/editar").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Guardar cambios")));
    }

    private static MockHttpSession sessionMulti(String rolActivo) {
        OidcUser user = new OidcUser() {
            @Override public Map<String, Object> getClaims() { return Map.of("email", "user@ddsi.local", "email_verified", true); }
            @Override public OidcUserInfo getUserInfo() { return null; }
            @Override public OidcIdToken getIdToken() { return null; }
            @Override public Map<String, Object> getAttributes() { return getClaims(); }
            @Override public List<SimpleGrantedAuthority> getAuthorities() {
                return List.of(
                        new SimpleGrantedAuthority("ROLE_DONANTE"),
                        new SimpleGrantedAuthority("ROLE_CHOFER"));
            }
            @Override public String getName() { return "user@ddsi.local"; }
        };
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "password", List.of(
                        new SimpleGrantedAuthority("ROLE_DONANTE"),
                        new SimpleGrantedAuthority("ROLE_CHOFER"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.setAttribute("rolActivo", rolActivo);
        return session;
    }
}