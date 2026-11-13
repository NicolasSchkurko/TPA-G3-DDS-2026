package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Donación pública para el landing: sin id de usuario ni nombre del donante.
 */
public record DonacionDestacadaDTO(
        UUID idDonacion,
        LocalDateTime fechaEntrega,
        Integer cantidadBienes,
        String categoria,
        String subCategoria,
        String entidadBeneficiaria) {
}
