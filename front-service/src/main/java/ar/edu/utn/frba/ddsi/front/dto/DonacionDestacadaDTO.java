package ar.edu.utn.frba.ddsi.front.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Donación pública que devuelve incentivos para el landing. */
public record DonacionDestacadaDTO(
        UUID idDonacion,
        LocalDateTime fechaEntrega,
        Integer cantidadBienes,
        String categoria,
        String subCategoria,
        String entidadBeneficiaria) {
}
