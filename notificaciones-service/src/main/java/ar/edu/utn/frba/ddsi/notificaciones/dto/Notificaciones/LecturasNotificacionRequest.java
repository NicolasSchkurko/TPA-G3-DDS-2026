package ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record LecturasNotificacionRequest(
        @NotBlank String usuarioId,
        @NotEmpty Set<@NotNull UUID> notificacionIds) {
}
