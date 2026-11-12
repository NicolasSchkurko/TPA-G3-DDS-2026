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
        Double latitud,
        Double longitud,
        String ubicacion) {
}
