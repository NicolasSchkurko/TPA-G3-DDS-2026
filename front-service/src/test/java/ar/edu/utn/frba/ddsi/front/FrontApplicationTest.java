package ar.edu.utn.frba.ddsi.front;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.config.ProfileRedirectHandler;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.PersonaDonanteDTO;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.RestClient;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FrontApplicationTest.TestBeans.class)
class FrontApplicationTest {

    private static final UUID DONATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void registerPageRenders() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void registerPagePreselectsDonorWhenRegistrationIsSuggested() throws Exception {
        mockMvc.perform(get("/register").param("tipoRegistro", "DONANTE"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<option value=\"DONANTE\" selected=\"selected\">Donante</option>")));
    }

    @Test
    void donationFormRedirectsUnauthenticatedUsersToLogin() throws Exception {
        mockMvc.perform(get("/realizar-donacion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Location", org.hamcrest.Matchers.containsString("/login")));
    }

    @Test
    void loginSuccessReturnsToSavedDonationForm() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/realizar-donacion");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new HttpSessionRequestCache().saveRequest(request, response);

        new ProfileRedirectHandler().onAuthenticationSuccess(request, response,
                new UsernamePasswordAuthenticationToken("entidad", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ENTIDAD"))));

        org.junit.jupiter.api.Assertions.assertEquals("/realizar-donacion", response.getRedirectedUrl());
    }

    @Test
    void donationFormShowsAuthenticatedDonorsOwnProfile() throws Exception {
        mockMvc.perform(get("/realizar-donacion").session(sessionWithRole("ROLE_DONANTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Perfil de prueba")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("<select class=\"form-select\" id=\"idDonante\""))));
    }

    @Test
    void donationFormSuggestsRegistrationForNonDonorAccounts() throws Exception {
        mockMvc.perform(get("/realizar-donacion").session(sessionWithRole("ROLE_ENTIDAD")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Registrarse")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tu cuenta no tiene el rol de donante.")));
    }

    @Test
    void termsPageIsPublicAndRendersLegalSections() throws Exception {
        mockMvc.perform(get("/terminos"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Términos y condiciones")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Política de privacidad")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/terminos.css")));
    }

    @Test
    void donationDetailIsPublicAndShowsUnassignedEntity() throws Exception {
        mockMvc.perform(get("/donaciones/{id}", DONATION_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No asignada")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Camisas")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ROPA")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/detalle-donacion.css")));
    }

    @Test
    void donationListIsPublic() throws Exception {
        mockMvc.perform(get("/donaciones"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Listado de Donaciones")));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        DonacionesClient testDonacionesClient(RestClient restClient) {
            return new DonacionesClient(restClient, "http://localhost") {
                @Override
                public DonacionDTO obtenerDonacionPorId(UUID id) {
                    if (!DONATION_ID.equals(id)) {
                        return null;
                    }
                    DonacionDTO donacion = new DonacionDTO();
                    donacion.setId(id);
                    donacion.setDescripcion("Abrigo");
                    donacion.setDonanteName("Donante de prueba");
                    donacion.setCategoriaBienName("ROPA");
                    donacion.setSubcategoriaName("Camisas");
                    donacion.setCantidadTotalBienes(20);
                    donacion.setEstado("EN_PREPARACION");
                    return donacion;
                }

                @Override
                public java.util.List<DonacionDTO> obtenerTodasLasDonaciones() {
                    return Collections.emptyList();
                }

                @Override
                public java.util.List<PersonaDonanteDTO> buscarDonantesPorCorreo(String correo) {
                    if (!"donante@example.com".equals(correo)) {
                        return Collections.emptyList();
                    }
                    PersonaDonanteDTO donante = new PersonaDonanteDTO();
                    donante.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
                    donante.setTipoPersona("HUMANA");
                    donante.setNombre("Perfil");
                    donante.setApellido("de prueba");
                    return List.of(donante);
                }
            };
        }
    }

    private static MockHttpSession sessionWithRole(String role) {
        OidcUser user = new OidcUser() {
            @Override
            public Map<String, Object> getClaims() {
                return Map.of("email", "donante@example.com");
            }

            @Override
            public OidcUserInfo getUserInfo() {
                return null;
            }

            @Override
            public OidcIdToken getIdToken() {
                return null;
            }

            @Override
            public Map<String, Object> getAttributes() {
                return Map.of("email", "donante@example.com");
            }

            @Override
            public List<SimpleGrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority(role));
            }

            @Override
            public String getName() {
                return "donante@example.com";
            }
        };
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, "password",
                List.of(new SimpleGrantedAuthority(role))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
