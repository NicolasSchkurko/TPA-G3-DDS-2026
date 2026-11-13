package ar.edu.utn.frba.ddsi.front.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Única regla de reclamo de perfiles de dominio en todo el front.
 *
 * <p>Un perfil solo puede reclamarse si está huérfano ({@code keycloak_sub == null}) y,
 * si la verificación de email está habilitada, si el correo de la cuenta está verificado.
 * Ningún otro lugar del código debería tomar esta decisión.</p>
 *
 * <p>La verificación está habilitada por defecto; al desactivarla explícitamente alcanza con
 * que el perfil esté huérfano.</p>
 */
@Component
public class PoliticaDeReclamo {

    private final boolean verificacionHabilitada;

    public PoliticaDeReclamo(@Value("${keycloak.verification.enabled:true}") boolean verificacionHabilitada) {
        this.verificacionHabilitada = verificacionHabilitada;
    }

    /** Si la verificación de email es obligatoria para reclamar un perfil. */
    public boolean isVerificacionHabilitada() {
        return verificacionHabilitada;
    }

    /**
     * Decide si el perfil indicado puede reclamarse con el email indicado.
     *
     * @param perfilYaVinculado true si el perfil ya tiene un sub de Keycloak (no se puede robar)
     * @param emailVerificado   true si el correo de la cuenta está verificado
     */
    public boolean puedeReclamar(boolean perfilYaVinculado, boolean emailVerificado) {
        if (perfilYaVinculado) {
            return false;
        }
        return !verificacionHabilitada || emailVerificado;
    }

    /** Lee {@code email_verified} del token de la cuenta. Con la verificación desactivada, devuelve true. */
    public boolean emailVerificado(OidcIdToken token) {
        if (!verificacionHabilitada) {
            return true;
        }
        if (token == null) {
            return false;
        }
        return Boolean.TRUE.equals(token.getClaim("email_verified"));
    }

    /** Lee {@code email_verified} del principal autenticado. Con la verificación desactivada, devuelve true. */
    public boolean emailVerificado(Authentication authentication) {
        if (!verificacionHabilitada) {
            return true;
        }
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return Boolean.TRUE.equals(oidcUser.getEmailVerified());
        }
        return false;
    }
}
