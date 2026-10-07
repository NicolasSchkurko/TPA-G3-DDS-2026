package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Corta el paso a las operaciones de admin si el id recibido no corresponde a un
 * administrador. La lista vive en {@code donaciones-service}, así que la validación es una
 * llamada HTTP.
 */
@Component
public class ValidadorAdmin {
    private final DonacionClient donacionClient;

    public ValidadorAdmin(DonacionClient donacionClient) {
        this.donacionClient = donacionClient;
    }

    /**
     * Lanza {@link SecurityException} (403) si el usuario no es administrador o no existe.
     * El mensaje es el mismo en ambos casos para no filtrar qué ids existen.
     */
    public void verificarPermisos(UUID idAdmin) {
        if (!donacionClient.verificarAdmin(idAdmin)) {
            throw new SecurityException("El usuario no tiene permisos de administrador o no existe.");
        }
    }
}
