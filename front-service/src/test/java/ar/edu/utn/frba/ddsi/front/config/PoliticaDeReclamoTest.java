package ar.edu.utn.frba.ddsi.front.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoliticaDeReclamoTest {

    private static OidcIdToken idToken(boolean emailVerified) {
        return new OidcIdToken("token-value", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("sub", "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "email_verified", emailVerified));
    }

    @Test
    void conVerificacionDesactivadaSeAceptaCualquierPerfilHuerfano() {
        PoliticaDeReclamo politica = new PoliticaDeReclamo(false);

        assertTrue(politica.puedeReclamar(false, false));
        assertTrue(politica.puedeReclamar(false, true));
        assertTrue(politica.emailVerificado(idToken(false)));
    }

    @Test
    void aunConVerificacionDesactivadaUnPerfilVinculadoNoSeReclama() {
        PoliticaDeReclamo politica = new PoliticaDeReclamo(false);

        assertFalse(politica.puedeReclamar(true, false));
        assertFalse(politica.puedeReclamar(true, true));
    }

    @Test
    void conVerificacionActivadaAdemasHaceFaltaElEmailVerificado() {
        PoliticaDeReclamo politica = new PoliticaDeReclamo(true);

        assertTrue(politica.puedeReclamar(false, true));
        assertFalse(politica.puedeReclamar(false, false));
        assertFalse(politica.puedeReclamar(true, true));

        assertTrue(politica.emailVerificado(idToken(true)));
        assertFalse(politica.emailVerificado(idToken(false)));
        assertFalse(politica.emailVerificado((OidcIdToken) null));
    }
}
