package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.dto.FormularioDonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.PersonaDonanteDTO;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealizarDonacionControllerTest {

    @Test
    void showsRegistrationSuggestionWhenAccountIsNotDonor() {
        RealizarDonacionController controller = new RealizarDonacionController(new DonacionesClientStub());
        Authentication authentication = authentication("entidad@example.com", "ROLE_ENTIDAD");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.mostrarFormulario(authentication, model);

        assertEquals("donacion-perfil-error", view);
        assertTrue((Boolean) model.getAttribute("mostrarRegistro"));
    }

    @Test
    void usesDonorProfileFromAuthenticatedEmailInsteadOfSubmittedId() {
        UUID profileId = UUID.randomUUID();
        PersonaDonanteDTO donante = new PersonaDonanteDTO();
        donante.setId(profileId);
        donante.setTipoPersona("HUMANA");
        donante.setNombre("Donante");
        DonacionesClientStub client = new DonacionesClientStub();
        client.donantes = List.of(donante);
        RealizarDonacionController controller = new RealizarDonacionController(client);
        FormularioDonacionDTO formulario = new FormularioDonacionDTO();
        formulario.setIdDonante(UUID.randomUUID());
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(formulario, "formulario");

        String view = controller.crearDonacion(formulario, bindingResult,
                authentication("donante@example.com", "ROLE_DONANTE"), new ExtendedModelMap());

        assertEquals("redirect:/donacion-exitosa", view);
        assertEquals(profileId, formulario.getIdDonante());
        assertEquals(formulario, client.formularioEnviado);
    }

    @Test
    void showsDedicatedDonationSuccessView() {
        RealizarDonacionController controller = new RealizarDonacionController(new DonacionesClientStub());

        assertEquals("donacion-exitosa", controller.mostrarDonacionExitosa());
    }

    @Test
    void showsRegistrationSuggestionWhenDonorProfileIsNotLinkedToLoginEmail() {
        RealizarDonacionController controller = new RealizarDonacionController(new DonacionesClientStub());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.mostrarFormulario(authentication("sin-perfil@example.com", "ROLE_DONANTE"), model);

        assertEquals("donacion-perfil-error", view);
        assertTrue((Boolean) model.getAttribute("mostrarRegistro"));
        assertEquals("El correo de tu cuenta no coincide con el correo de contacto de ningún perfil de donante.",
                model.getAttribute("error"));
    }

    @Test
    void explainsWhenMultipleDonorProfilesUseTheSameEmail() {
        PersonaDonanteDTO primerDonante = new PersonaDonanteDTO();
        primerDonante.setId(UUID.randomUUID());
        PersonaDonanteDTO segundoDonante = new PersonaDonanteDTO();
        segundoDonante.setId(UUID.randomUUID());
        DonacionesClientStub client = new DonacionesClientStub();
        client.donantes = List.of(primerDonante, segundoDonante);
        RealizarDonacionController controller = new RealizarDonacionController(client);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.mostrarFormulario(authentication("donante@example.com", "ROLE_DONANTE"), model);

        assertEquals("donacion-perfil-error", view);
        assertEquals(Boolean.FALSE, model.getAttribute("mostrarRegistro"));
        assertTrue(((String) model.getAttribute("error")).contains("más de un perfil"));
    }

    private static Authentication authentication(String email, String role) {
        OidcUser user = new OidcUser() {
            @Override
            public Map<String, Object> getClaims() {
                return Map.of("email", email);
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
                return Map.of("email", email);
            }

            @Override
            public List<SimpleGrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority(role));
            }

            @Override
            public String getName() {
                return email;
            }
        };
        return new UsernamePasswordAuthenticationToken(user, "password",
                List.of(new SimpleGrantedAuthority(role)));
    }

    private static class DonacionesClientStub extends DonacionesClient {
        private List<PersonaDonanteDTO> donantes = List.of();
        private FormularioDonacionDTO formularioEnviado;

        private DonacionesClientStub() {
            super(RestClient.builder().build(), "http://localhost");
        }

        @Override
        public List<PersonaDonanteDTO> buscarDonantesPorCorreo(String correo) {
            return donantes;
        }

        @Override
        public void crearDonacion(FormularioDonacionDTO formulario) {
            formularioEnviado = formulario;
        }
    }
}
