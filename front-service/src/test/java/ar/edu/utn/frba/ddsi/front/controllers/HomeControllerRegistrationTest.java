package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.KeycloakRegistrationClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeControllerRegistrationTest {

    @Test
    void preservesDonorFormValuesWhenEmailValidationFailsWithoutKeepingPassword() {
        HomeController controller = new HomeController(new DonacionesClientStub(),
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
        HomeController controller = new HomeController(new DonacionesClientStub(), keycloakClient);
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
        HomeController controller = new HomeController(donacionesClient, keycloakClient);
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
    void sendsTheKeycloakSubWhenCreatingTheProfile() {
        DonacionesClientStub donacionesClient = new DonacionesClientStub();
        KeycloakRegistrationClientStub keycloakClient = new KeycloakRegistrationClientStub();
        HomeController controller = new HomeController(donacionesClient, keycloakClient);
        ExtendedModelMap model = new ExtendedModelMap();
        Map<String, String> form = Map.of(
                "username", "nueva-entidad",
                "password", "password-segura",
                "tipoRegistro", "ENTIDAD",
                "email", "entidad@example.com",
                "razonSocial", "Entidad Ejemplo"
        );

        assertEquals("register", controller.register(form, model));

        assertEquals("test-user-id", donacionesClient.datos.get("keycloakSub"));
    }

    @Test
    void showsProfileServiceValidationErrorAndRollsBackCreatedAccount() {
        DonacionesClientStub donacionesClient = new DonacionesClientStub();
        donacionesClient.error = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                HttpHeaders.EMPTY, "La dirección no tiene una ciudad válida".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        KeycloakRegistrationClientStub keycloakClient = new KeycloakRegistrationClientStub();
        HomeController controller = new HomeController(donacionesClient, keycloakClient);
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


    @Test
    void unPerfilHuerfanoExistenteSeReclamaConLaCuentaNueva() {
        DonacionesClientStub donacionesClient = new DonacionesClientStub();
        UUID perfilId = UUID.fromString("222e4567-e89b-12d3-a456-426614174000");
        donacionesClient.identidad = entidadIdentidad(perfilId, false);
        KeycloakRegistrationClientStub keycloakClient = new KeycloakRegistrationClientStub();
        HomeController controller = new HomeController(donacionesClient, keycloakClient);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("register", controller.register(formularioEntidad("entidad@example.com"), model));

        assertEquals("Tu cuenta quedó creada. Ya podés ingresar con tu usuario y contraseña.",
                model.getAttribute("msg"));
        assertTrue(donacionesClient.vincularLlamado);
        assertEquals("ENTIDAD", donacionesClient.vinculadoConRol);
        assertEquals(perfilId, donacionesClient.vinculadoConPerfil);
        org.junit.jupiter.api.Assertions.assertFalse(keycloakClient.deleted);
    }

    @Test
    void unPerfilVinculadoAOtraCuentaNoSeReclamaYSeRevierteLaCuenta() {
        DonacionesClientStub donacionesClient = new DonacionesClientStub();
        UUID perfilId = UUID.fromString("222e4567-e89b-12d3-a456-426614174000");
        donacionesClient.identidad = entidadIdentidad(perfilId, true);
        donacionesClient.vincularError =
                new IllegalStateException("Ese perfil ya está vinculado a otra cuenta.");
        KeycloakRegistrationClientStub keycloakClient = new KeycloakRegistrationClientStub();
        HomeController controller = new HomeController(donacionesClient, keycloakClient);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("register", controller.register(formularioEntidad("entidad@example.com"), model));

        assertEquals("Ese perfil ya está vinculado a otra cuenta. Iniciá sesión con la cuenta original "
                        + "o contactá a un administrador.",
                model.getAttribute("error"));
        assertTrue(keycloakClient.deleted);
        org.junit.jupiter.api.Assertions.assertFalse(donacionesClient.vincularLlamado);
    }

    @Test
    void siElVinculoDelPerfilFallaSeRevierteLaCuenta() {
        DonacionesClientStub donacionesClient = new DonacionesClientStub();
        UUID perfilId = UUID.fromString("222e4567-e89b-12d3-a456-426614174000");
        donacionesClient.identidad = entidadIdentidad(perfilId, false);
        donacionesClient.vincularError =
                new IllegalStateException("Ese perfil ya está vinculado a otra cuenta.");
        KeycloakRegistrationClientStub keycloakClient = new KeycloakRegistrationClientStub();
        HomeController controller = new HomeController(donacionesClient, keycloakClient);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("register", controller.register(formularioEntidad("entidad@example.com"), model));

        assertEquals("Ese perfil ya está vinculado a otra cuenta. Iniciá sesión con la cuenta original "
                        + "o contactá a un administrador.",
                model.getAttribute("error"));
        assertTrue(keycloakClient.deleted);
    }

    private static Map<String, String> formularioEntidad(String email) {
        return Map.of(
                "username", "nueva-entidad",
                "password", "password-segura",
                "tipoRegistro", "ENTIDAD",
                "email", email,
                "razonSocial", "Entidad Ejemplo"
        );
    }

    private static ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO entidadIdentidad(UUID entidadId, boolean vinculada) {
        ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO dto = new ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO();
        dto.setEmail("entidad@example.com");
        dto.setEntidadId(entidadId);
        dto.setVinculada(vinculada);
        dto.setRoles(java.util.List.of("ENTIDAD"));
        return dto;
    }

    private static class DonacionesClientStub extends DonacionesClient {
        private HttpClientErrorException error;
        private String tipoRegistro;
        private Map<String, String> datos;

        private DonacionesClientStub() {
            super(RestClient.builder().build(), "http://localhost");
        }

        private ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO identidad;
        private RuntimeException vincularError;
        private boolean vincularLlamado;
        private String vinculadoConRol;
        private UUID vinculadoConPerfil;

        @Override
        public ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO buscarIdentidadPorEmail(String email) {
            return identidad;
        }

        @Override
        public ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO buscarIdentidadPorSub(String sub) {
            return null;
        }

        @Override
        public void vincularIdentidad(String sub, String rol, UUID perfilId) {
            vincularLlamado = true;
            if (vincularError != null) {
                throw vincularError;
            }
            vinculadoConRol = rol;
            vinculadoConPerfil = perfilId;
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
