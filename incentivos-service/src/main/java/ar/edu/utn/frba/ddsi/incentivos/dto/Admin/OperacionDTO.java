package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OperacionDTO {
    private String tipoOperacion;
    private Integer progresoObjetivo;
    private String valorEsperado;  // solo COINCIDENCIAS
    private Integer cantidad;      // VALORES_DISTINTOS o SUPERA_CANTIDAD

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
                String.valueOf(coincidencias.getValorEsperado()),
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