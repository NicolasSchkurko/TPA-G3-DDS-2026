package ar.edu.utn.frba.ddsi.notificaciones.dto;

import java.util.List;

/** Cuerpo de error uniforme: {@code detalles} trae un mensaje por cada campo inválido. */
public record ErrorResponseDTO(String mensaje, int status, List<String> detalles) {

    public ErrorResponseDTO(String mensaje, int status) {
        this(mensaje, status, List.of());
    }
}
