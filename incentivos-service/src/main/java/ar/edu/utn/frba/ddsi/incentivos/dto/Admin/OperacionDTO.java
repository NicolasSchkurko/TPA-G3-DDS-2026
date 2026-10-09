package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OperacionDTO {

    @NotBlank(message = "La operación requiere un tipo (COINCIDENCIAS, "
            + "VALORES_DISTINTOS o SUPERA_CANTIDAD)")
    private String tipoOperacion;

    @NotNull(message = "La operación requiere un progreso objetivo")
    @Positive(message = "El progreso objetivo debe ser mayor a cero")
    private Integer progresoObjetivo;

    /** Requerido solo para COINCIDENCIAS. Validador en MisionService.construirMision. */
    private String valorEsperado;

    /** Requerido para VALORES_DISTINTOS y SUPERA_CANTIDAD. Validador en construirMision. */
    private Integer cantidad;

    public OperacionDTO(String tipoOperacion,
                        Integer progresoObjetivo,
                        String valorEsperado,
                        Integer cantidad) {
        this.tipoOperacion = tipoOperacion;
        this.progresoObjetivo = progresoObjetivo;
        this.valorEsperado = valorEsperado;
        this.cantidad = cantidad;
    }

    public static OperacionDTO desdeEntidad(Operacion operacion) {
        if (operacion == null) {
            return null;
        }

        if (operacion instanceof CantidadCoincidencias coincidencias) {
            return new OperacionDTO(
                "COINCIDENCIAS",
                coincidencias.getProgresoObjetivo(),
                // asText() y no toString(): toString() devuelve el JSON entrecomillado.
                coincidencias.getValorEsperado() == null
                    ? null
                    : coincidencias.getValorEsperado().asText(),
                null
            );
        }

        if (operacion instanceof ValoresDistintos distintos) {
            return new OperacionDTO(
                "VALORES_DISTINTOS",
                distintos.getProgresoObjetivo(),
                null,
                distintos.getCantValoresDistintos()
            );
        }

        if (operacion instanceof SuperaCantidad superaCantidad) {
            return new OperacionDTO(
                "SUPERA_CANTIDAD",
                superaCantidad.getProgresoObjetivo(),
                null,
                superaCantidad.getCantidadEsperada()
            );
        }

        throw new IllegalArgumentException(
            "Tipo de operación no soportado: " + operacion.getClass().getSimpleName()
        );
    }
}
