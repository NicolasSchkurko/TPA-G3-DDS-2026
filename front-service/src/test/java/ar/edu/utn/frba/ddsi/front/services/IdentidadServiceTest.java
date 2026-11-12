package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class IdentidadServiceTest {

    private static final String SUB = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final String EMAIL = "ana@example.com";
    private static final UUID ID_DONANTE = UUID.fromString("111e4567-e89b-12d3-a456-426614174000");

    private DonacionesClient donacionesClient;
    private IdentidadService service;

    @BeforeEach
    void setUp() {
        donacionesClient = org.mockito.Mockito.mock(DonacionesClient.class);
        service = new IdentidadService(donacionesClient);
    }

    private Authentication auth() {
        OidcIdToken idToken = new OidcIdToken("token-value", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("sub", SUB, "email", EMAIL, "email_verified", true));
        OidcUserInfo userInfo = new OidcUserInfo(Map.of("sub", SUB, "email", EMAIL));
        return new UsernamePasswordAuthenticationToken(
                new DefaultOidcUser(List.of(), idToken, userInfo), null, List.of());
    }

    private IdentidadDTO identidad(UUID donanteId, boolean vinculada) {
        IdentidadDTO dto = new IdentidadDTO();
        dto.setEmail(EMAIL);
        dto.setDonanteId(donanteId);
        dto.setVinculada(vinculada);
        dto.setRoles(donanteId == null ? List.of() : List.of("DONANTE"));
        return dto;
    }

    @Test
    void resuelvePorSubSinCaerAlEmail() {
        when(donacionesClient.buscarIdentidadPorSub(SUB)).thenReturn(identidad(ID_DONANTE, true));

        Optional<UUID> perfil = service.perfilIdActual(auth(), "DONANTE");

        assertEquals(Optional.of(ID_DONANTE), perfil);
        verify(donacionesClient, never()).vincularIdentidad(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void unPerfilHuerfanoSeReclamaAlVincularlo() {
        when(donacionesClient.buscarIdentidadPorSub(SUB)).thenReturn(null);
        when(donacionesClient.buscarIdentidadPorEmail(EMAIL)).thenReturn(identidad(ID_DONANTE, false));

        Optional<UUID> perfil = service.perfilIdActual(auth(), "DONANTE");

        assertEquals(Optional.of(ID_DONANTE), perfil);
        verify(donacionesClient).vincularIdentidad(SUB, "DONANTE", ID_DONANTE);
    }

    @Test
    void unPerfilVinculadoAtraCuentaNoSeReclama() {
        when(donacionesClient.buscarIdentidadPorSub(SUB)).thenReturn(null);
        when(donacionesClient.buscarIdentidadPorEmail(EMAIL)).thenReturn(identidad(ID_DONANTE, true));

        Optional<UUID> perfil = service.perfilIdActual(auth(), "DONANTE");

        assertEquals(Optional.empty(), perfil);
        org.junit.jupiter.api.Assertions.assertFalse(
                org.mockito.Mockito.mockingDetails(donacionesClient).getInvocations().stream()
                        .anyMatch(i -> i.getMethod().getName().equals("vincularIdentidad")));
    }

    @Test
    void siElVinculoFallaNoSeOtorgaAcceso() {
        when(donacionesClient.buscarIdentidadPorSub(SUB)).thenReturn(null);
        when(donacionesClient.buscarIdentidadPorEmail(EMAIL)).thenReturn(identidad(ID_DONANTE, false));
        org.mockito.Mockito.doThrow(new IllegalStateException("Ese perfil ya está vinculado a otra cuenta."))
                .when(donacionesClient).vincularIdentidad(anyString(), anyString(), any(UUID.class));

        Optional<UUID> perfil = service.perfilIdActual(auth(), "DONANTE");

        assertEquals(Optional.empty(), perfil);
    }

    @Test
    void sinIdentidadPorEmailNoSeReclamaNada() {
        when(donacionesClient.buscarIdentidadPorSub(SUB)).thenReturn(null);
        when(donacionesClient.buscarIdentidadPorEmail(EMAIL)).thenReturn(null);

        assertEquals(Optional.empty(), service.perfilIdActual(auth(), "DONANTE"));
    }
}
