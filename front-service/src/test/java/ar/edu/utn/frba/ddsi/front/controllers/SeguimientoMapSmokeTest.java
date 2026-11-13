package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.services.SeguimientoTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SeguimientoMapSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeguimientoTokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogisticasClient logisticasClient;

    @MockBean
    private DonacionesClient donacionesClient;

    @Test
    void elMapaUsaUnTokenYNoExponeElIdDeLaRuta() throws Exception {
        UUID idRuta = UUID.randomUUID();
        when(logisticasClient.obtenerRuta(any())).thenReturn(objectMapper.readTree(rutaJson(idRuta)));
        String token = tokenService.generar(idRuta);

        mockMvc.perform(get("/seguimiento/{token}", token).session(sessionAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AC482KP")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Depósito")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(idRuta.toString()))));
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        UUID idRuta = UUID.randomUUID();
        when(logisticasClient.obtenerRuta(any())).thenReturn(objectMapper.readTree(rutaJson(idRuta)));
        mockMvc.perform(get("/seguimiento/{token}", tokenService.generar(idRuta)))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void unTokenInvalidoVuelveAlInicio() throws Exception {
        mockMvc.perform(get("/seguimiento/{token}", "token-invalido"))
                .andExpect(status().is3xxRedirection());
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

    private static String rutaJson(UUID idRuta) {
        return """
                {
                  "idRuta": "%s",
                  "camionAsignado": {"patente": "AC482KP"},
                  "estado": "EN_CURSO",
                  "deposito": {"latitud": -34.5986317, "longitud": -58.4226973, "etiqueta": "Depósito"},
                  "paradas": [
                    {"direccion": {"calleUno": "Av. Perito Moreno", "altura": 2400, "latitud": -34.6538, "longitud": -58.4478,
                      "items": {"bienes": [{"estado": "ENTREGADA"}, {"estado": "EN_CAMINO"}]}}},
                    {"direccion": {"calleUno": "Av. La Plata", "altura": 1500, "latitud": -34.6375, "longitud": -58.3816,
                      "items": {"bienes": [{"estado": "EN_CAMINO"}]}}}
                  ]
                }
                """.formatted(idRuta);
    }
}
