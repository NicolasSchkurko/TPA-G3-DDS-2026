package ar.edu.utn.frba.ddsi.front.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Donación destacada enriquecida con la ubicación pública de su entidad receptora. */
public record DonacionDestacadaMapaDTO(
        UUID idDonacion,
        LocalDateTime fechaEntrega,
        Integer cantidadBienes,
        String categoria,
        String subCategoria,
        String entidadBeneficiaria,
        String estado,
        Double latitud,
        Double longitud,
        String ubicacion) {

    /** Etiqueta del estado real de la donación en donaciones-service. */
    public String estadoEtiqueta() {
        return DonacionDTO.etiquetaDeEstado(estado);
    }

    /** Clase de estado para el chip y el marcador del landing. */
    public String estadoMapa() {
        if (estado == null || estado.isBlank()) return "pending";
        return switch (estado.toUpperCase()) {
            case "ENTREGADO" -> "delivered";
            case "EN_TRASLADO" -> "transit";
            case "EN_DEPOSITO" -> "depot";
            default -> "pending";
        };
    }
}
