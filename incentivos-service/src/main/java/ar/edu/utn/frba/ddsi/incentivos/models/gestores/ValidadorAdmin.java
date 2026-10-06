package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Corta el paso a las operaciones de admin si el id recibido no corresponde a un
 * administrador.
 *
 * <p>La lista de administradores vive en {@code donaciones-service}, así que la
 * validación es una llamada HTTP. Es una excepción conocida y está anotada en
 * {@code PENDIENTES.md} (punto 1): la autorización de verdad debería salir del token de
 * seguridad, no de un id que manda el cliente en un header.
 */
@Component
public class ValidadorAdmin {
    private final DonacionClient donacionClient;

    public ValidadorAdmin(DonacionClient donacionClient) {
        this.donacionClient = donacionClient;
    }

    /**
     * Lanza {@link SecurityException} (que el handler traduce a 403) si el usuario no es
     * administrador o directamente no existe.
     *
     * <p>El mensaje es el mismo en los dos casos a propósito: si se distinguieran, un
     * atacante podría usar el endpoint para averiguar qué ids existen.
     */
    public void verificarPermisos(UUID idAdmin) {
        if (!donacionClient.verificarAdmin(idAdmin)) {
            throw new SecurityException("El usuario no tiene permisos de administrador o no existe.");
        }
    }
}
