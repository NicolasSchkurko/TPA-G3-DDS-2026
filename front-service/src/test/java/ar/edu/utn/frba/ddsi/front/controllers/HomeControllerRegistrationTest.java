package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.clients.KeycloakRegistrationClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class HomeControllerRegistrationTest {

    @Test
    void preservesDonorFormValuesWhenEmailValidationFailsWithoutKeepingPassword() {
        HomeController controller = new HomeController(new DonacionesClientStub(), mock(IncentivosClient.class),
                new KeycloakRegistrationClientStub());
        ExtendedModelMap model = new ExtendedModelMap();
        Map<String, String> form = Map.of(
                "username", "nuevo-donante",
                "password", "password-segura",
                "tipoRegistro", "DONANTE",
                "tipoPersona", "HUMANA",
                "nombre", "Ana",
                "apellido", "Ejemplo",
                "valorContacto", ""
        );

        assertEquals("register", controller.register(form, model, null));

        @SuppressWarnings("unchecked")
        Map<String, String> valoresPreservados =
                (Map<String, String>) model.getAttribute("formularioRegistro");
        assertEquals("DONANTE", valoresPreservados.get("tipoRegistro"));
        assertEquals("Ana", valoresPreservados.get("nombre"));
        assertFalse(valoresPreservados.containsKey("password"));
        assertEquals("Ingresá un correo electrónico para vincular tu perfil con la cuenta.",
                model.getAttribute("error"));
    }

    @Test
    void requiresEmailBeforeCreatingBeneficiaryEntityAccount() {
        KeycloakRegistrationClientStub keycloakClient = new KeycloakRegistrationClientStub();
        HomeController controller = new HomeController(new DonacionesClientStub(), mock(IncentivosClient.class), keycloakClient);
        ExtendedModelMap model = new ExtendedModelMap();
        Map<String, String> form = Map.of(
                "username", "nueva-entidad",
                "password", "password-segura",
                "tipoRegistro", "ENTIDAD",
                "razonSocial", "Entidad Ejemplo"
        );

        assertEquals("register", controller.register(form, model, null));

        assertEquals("Ingresá un correo electrónico para vincular tu perfil con la cuenta.",
                model.getAttribute("error"));
        assertFalse(keycloakClient.created);
    }

    @Test
    void usesBeneficiaryEntityEmailForKeycloakAndProfileRegistration() {
        DonacionesClientStub donacionesClient = new DonacionesClientStub();
        KeycloakRegistrationClientStub keycloakClient = new KeycloakRegistrationClientStub();
        HomeController controller = new HomeController(donacionesClient, mock(IncentivosClient.class), keycloakClient);
        ExtendedModelMap model = new ExtendedModelMap();
        Map<String, String> form = Map.of(
                "username", "nueva-entidad",
                "password", "password-segura",
                "tipoRegistro", "ENTIDAD",
                "razonSocial", "Entidad Ejemplo",
                "correoElectronico", "entidad@example.com"
        );

        assertEquals("register", controller.register(form, model, null));

        assertEquals("entidad@example.com", keycloakClient.email);
        assertEquals("ENTIDAD", donacionesClient.tipoRegistro);
        assertEquals("entidad@example.com", donacionesClient.datos.get("correoElectronico"));
        assertEquals("Tu cuenta quedó creada. Ya podés ingresar con tu usuario y contraseña.",
                model.getAttribute("msg"));
    }

    @Test
    void showsProfileServiceValidationErrorAndRollsBackCreatedAccount() {
        DonacionesClientStub donacionesClient = new DonacionesClientStub();
        donacionesClient.error = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                HttpHeaders.EMPTY, "La dirección no tiene una ciudad válida".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        KeycloakRegistrationClientStub keycloakClient = new KeycloakRegistrationClientStub();
        HomeController controller = new HomeController(donacionesClient, mock(IncentivosClient.class), keycloakClient);
        ExtendedModelMap model = new ExtendedModelMap();
        Map<String, String> form = Map.of(
                "username", "nuevo-donante",
                "password", "password-segura",
                "tipoRegistro", "DONANTE",
                "tipoPersona", "HUMANA",
                "tipoContacto", "EMAIL",
                "valorContacto", "ana@example.com",
                "nombre", "Ana",
                "apellido", "Ejemplo"
        );

        assertEquals("register", controller.register(form, model, null));

        assertEquals("La dirección no tiene una ciudad válida", model.getAttribute("error"));
        assertTrue(keycloakClient.deleted);
    }

    private static class DonacionesClientStub extends DonacionesClient {
        private HttpClientErrorException error;
        private String tipoRegistro;
        private Map<String, String> datos;

        private DonacionesClientStub() {
            super(RestClient.builder().build(), "http://localhost");
        }

        @Override
        public void registrarUsuario(String tipoRegistro, Map<String, String> datos,
                                     String idKeycloak) {
            this.tipoRegistro = tipoRegistro;
            this.datos = datos;
            if (error != null) {
                throw error;
            }
        }
    }

    private static class KeycloakRegistrationClientStub extends KeycloakRegistrationClient {
        private boolean deleted;
        private boolean created;
        private String email;

        private KeycloakRegistrationClientStub() {
            super(RestClient.builder(), "http://localhost/token", "http://localhost/admin",
                    "realm", "client", "secret");
        }

        @Override
        public String crearUsuario(String username, String password, String role,
                                   String firstName, String lastName, String email) {
            created = true;
            this.email = email;
            return "test-user-id";
        }

        @Override
        public void eliminarUsuario(String userId) {
            deleted = true;
        }
    }
}
