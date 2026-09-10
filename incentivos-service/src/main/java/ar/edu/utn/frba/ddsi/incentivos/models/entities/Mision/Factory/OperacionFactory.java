package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        TipoOperacion tipo =
                TipoOperacion.valueOf(tipoOperacion.toUpperCase(Locale.ROOT));

        return switch (tipo) {

            case COINCIDENCIAS -> {

                JsonNode jsonValor = MAPPER.valueToTree(valor);

                yield new CantidadCoincidencias(
                        progresoObjetivo,
                        jsonValor
                );
            }

            case SUPERA_CANTIDAD ->
                    new SuperaCantidad(progresoObjetivo, cantidad);

            case VALORES_DISTINTOS ->
                    new ValoresDistintos(progresoObjetivo, cantidad);
        };
    }
}
