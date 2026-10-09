package ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Resumen de un Formulario para listarlo por API.
 *
 * <p>Existe para poder borrar los formularios de un Donante antes de borrar al Donante:
 * {@code Formulario.donante_id} es FK no nula sin cascade REMOVE, así que sin esta limpieza
 * el DELETE del donante responde 409. El consumidor conocido (la collection de Postman)
 * necesita al menos {@code id} y {@code donanteId}.
 */
@Getter
@Setter
public class FormularioResumenDTO {

    private UUID id;
    private UUID donanteId;
    private String donanteName;
    private LocalDate fechaRealizacion;

    public static FormularioResumenDTO from(Formulario formulario) {
        if (formulario == null) return null;

        FormularioResumenDTO dto = new FormularioResumenDTO();
        dto.setId(formulario.getId());
        dto.setFechaRealizacion(formulario.getFechaRealizacion());
        if (formulario.getDonante() != null) {
            dto.setDonanteId(formulario.getDonante().getId());
            if (formulario.getDonante().getPersona() != null) {
                dto.setDonanteName(formulario.getDonante().getPersona().getNombreDeUsuario());
            }
        }
        return dto;
    }
}
