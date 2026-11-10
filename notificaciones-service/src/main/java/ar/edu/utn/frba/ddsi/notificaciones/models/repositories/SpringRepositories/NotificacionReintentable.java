package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories;

import java.util.UUID;

/** Proyección del scheduler: no arrastra el Mensaje EAGER de la notificación. */
public record NotificacionReintentable(UUID id, Integer intentos,
                                       String tipoMedioDeContacto, String direccionDeContacto) {
}
