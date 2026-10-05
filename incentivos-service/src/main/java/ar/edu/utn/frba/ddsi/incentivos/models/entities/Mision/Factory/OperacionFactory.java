package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Locale;

@Component
public class OperacionFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Operacion conseguirOperacion(
            String tipoOperacion,
            Integer progresoObjetivo,
            Integer cantidad,
            Object valor
    ) {
        TipoOperacion tipo = resolverTipo(tipoOperacion);

        if (progresoObjetivo == null || progresoObjetivo <= 0) {
            throw new DatosInvalidosException(
                "El progreso objetivo debe ser mayor a cero, llegó: " + progresoObjetivo
            );
        }

        return switch (tipo) {

            case COINCIDENCIAS -> {

                if (valor == null || valor.toString().isBlank()) {
                    // Sin valor esperado la operación nunca coincide, así que la misión
                    // queda imposible de completar sin avisar. Es mejor rechazarla.
                    throw new DatosInvalidosException(
                        "La operación COINCIDENCIAS necesita un valorEsperado "
                            + "con el que comparar el atributo de impacto"
                    );
                }

                JsonNode jsonValor = MAPPER.valueToTree(valor);

                yield new CantidadCoincidencias(
                        progresoObjetivo,
                        jsonValor
                );
            }

            case SUPERA_CANTIDAD -> new SuperaCantidad(
                progresoObjetivo,
               cantidadPositiva(cantidad, tipo)
            );

            case VALORES_DISTINTOS -> new ValoresDistintos(
                progresoObjetivo,
                cantidadPositiva(cantidad, tipo)
            );
        };
    }

    private TipoOperacion resolverTipo(String tipoOperacion) {
        if (tipoOperacion == null || tipoOperacion.isBlank()) {
            throw new DatosInvalidosException(
                "La operación necesita un tipo. Se aceptan: " + List.of(TipoOperacion.values())
            );
        }

        try {
            return TipoOperacion.valueOf(tipoOperacion.toUpperCase(Locale.ROOT).trim());
        } catch (IllegalArgumentException exception) {
            throw new DatosInvalidosException(
                "'" + tipoOperacion + "' no es un tipo de operación válido. Se aceptan: "
                    + List.of(TipoOperacion.values())
            );
        }
    }

    private Integer cantidadPositiva(Integer cantidad, TipoOperacion tipo) {
        if (cantidad == null || cantidad <= 0) {
            // Sin cantidad, SuperaCantidad desempaca un null al comparar y revienta con
            // NullPointerException cuando llega una donación.
            throw new DatosInvalidosException(
                "La operación " + tipo + " necesita una cantidad mayor a cero, llegó: "
                    + cantidad
            );
        }
        return cantidad;
    }
}