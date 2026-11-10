package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.KeycloakRegistrationClient;
import ar.edu.utn.frba.ddsi.front.services.IdentidadMappingStore;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeControllerRegistrationTest {

    private static IdentidadMappingStore mappingStoreDePrueba() {
        try {
            Path temp = Files.createTempFile("identidad-test", ".json");
            temp.toFile().deleteOnExit();
            return new IdentidadMappingStore(temp.toString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void preservesDonorFormValuesWhenEmailValidationFailsWithoutKeepingPassword() {
        HomeController controller = new HomeController(new DonacionesClientStub(),
                new KeycloakRegistrationClientStub(), mappingStoreDePrueba());
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

        assertEquals("register", controller.register(form, model));

        @SuppressWarnings("unchecked")
        Map<String, String> valoresPreservados =
                (Map<String, String>) model.getAttribute("formularioRegistro");
        assertEquals("DONANTE", valoresPreservados.get("tipoRegistro"));
        assertEquals("Ana", valoresPreservados.get("nombre"));
        assertFalse(valoresPreservados.containsKey("password"));
        assertEquals("Ingresá un correo electrónico válido (por ejemplo, nombre@dominio.com).",
                model.getAttribute("error"));
    }

    @Test
    void requiresAccountEmailBeforeCreatingBeneficiaryEntityAccount() {
        KeycloakRegistrationClientStub keycloakClient = new KeycloakRegistrationClientStub();
        HomeController controller = new HomeController(new DonacionesClientStub(), keycloakClient,
                mappingStoreDePrueba());
        ExtendedModelMap model = new ExtendedModelMap();
        Map<String, String> form = Map.of(
                "username", "nueva-entidad",
                "password", "password-segura",
                "tipoRegistro", "ENTIDAD",
                "razonSocial", "Entidad Ejemplo"
        );

        assertEquals("register", controller.register(form, model));

        assertEquals("Ingresá un correo electrónico válido (por ejemplo, nombre@dominio.com).",
                model.getAttribute("error"));
        assertFalse(keycloakClient.created);
    }

    @Test
    void usesAccountEmailForKeycloakAndProfileRegistration() {
        DonacionesClientStub donacionesClient = new DonacionesClientStub();
        KeycloakRegistrationClientStub keycloakClient = new KeycloakRegistrationClientStub();
        HomeController controller = new HomeController(donacionesClient, keycloakClient,
                mappingStoreDePrueba());
        ExtendedModelMap model = new ExtendedModelMap();
        Map<String, String> form = Map.of(
                "username", "nueva-entidad",
                "password", "password-segura",
                "tipoRegistro", "ENTIDAD",
                "email", "entidad@example.com",
                "razonSocial", "Entidad Ejemplo"
        );

        assertEquals("register", controller.register(form, model));

        assertEquals("entidad@example.com", keycloakClient.email);
        assertEquals("ENTIDAD", donacionesClient.tipoRegistro);
        assertEquals("entidad@example.com", donacionesClient.datos.get("email"));
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
        HomeController controller = new HomeController(donacionesClient, keycloakClient,
                mappingStoreDePrueba());
        ExtendedModelMap model = new ExtendedModelMap();
        Map<String, String> form = Map.of(
                "username", "nuevo-donante",
                "password", "password-segura",
                "tipoRegistro", "DONANTE",
                "tipoPersona", "HUMANA",
                "email", "ana@example.com",
                "tipoContacto", "EMAIL",
                "valorContacto", "ana@example.com",
                "nombre", "Ana",
                "apellido", "Ejemplo"
        );

        assertEquals("register", controller.register(form, model));

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
        public ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO buscarIdentidadPorEmail(String email) {
            return null;
        }

        @Override
        public java.util.UUID registrarUsuario(String tipoRegistro, Map<String, String> datos) {
            this.tipoRegistro = tipoRegistro;
            this.datos = datos;
            if (error != null) {
                throw error;
            }
            return java.util.UUID.randomUUID();
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
        public String crearUsuario(String username, String password, String email, String role,
                                   String firstName, String lastName) {
            created = true;
            this.email = email;
            return "test-user-id";
        }

        @Override
        public boolean existeUsuarioConEmail(String email) {
            return false;
        }

        @Override
        public boolean existeUsuarioConUsername(String username) {
            return false;
        }

        @Override
        public java.util.Set<String> rolesDeUsuariosConEmail(String email) {
            return java.util.Set.of();
        }

        @Override
        public void asignarRoles(String userId, java.util.Collection<String> roles) {
            // no-op: no hay Keycloak en los tests
        }

        @Override
        public void eliminarUsuario(String userId) {
            deleted = true;
        }
    }
}
