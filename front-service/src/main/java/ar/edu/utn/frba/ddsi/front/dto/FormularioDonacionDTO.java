package ar.edu.utn.frba.ddsi.front.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FormularioDonacionDTO {

    private UUID idDonante;

    @NotNull(message = "Indicá la fecha de realización.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaRealizacion = LocalDate.now();

    @NotEmpty(message = "Agregá al menos un bien a la donación.")
    @Valid
    private List<BienDonacionDTO> bienes = new ArrayList<>(List.of(new BienDonacionDTO()));

    /**
     * Valida y normaliza lo que cargó el admin. Se usa en el panel de administración en vez de
     * {@code @Valid} porque ahí el formulario se repinta con los mensajes en castellano y con las
     * filas que el admin ya había tipeado, en lugar de mostrar los errores de binding crudos.
     *
     * <p>Las filas completamente vacías se descartan en silencio: el JS que agrega filas deja una
     * de más si el admin la abrió y no la usó.
     */
    public void validarYNormalizar() {
        if (idDonante == null) {
            throw new IllegalArgumentException("Elegí a qué donante corresponde la donación.");
        }
        if (fechaRealizacion == null) {
            throw new IllegalArgumentException("Indicá la fecha en la que se recibió la donación.");
        }
        if (fechaRealizacion.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de la donación no puede ser futura.");
        }

        bienes = BienDonacionDTO.normalizar(bienes);
        if (bienes.isEmpty()) {
            throw new IllegalArgumentException("Agregá al menos un bien a la donación.");
        }
    }
}
