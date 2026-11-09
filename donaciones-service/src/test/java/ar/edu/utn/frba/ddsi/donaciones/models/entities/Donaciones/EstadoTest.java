package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Punto 42: el enum cubre los estados del enunciado")
class EstadoTest {

    @Test
    @DisplayName("están los siete estados de trazabilidad del enunciado")
    void enumContieneLosEstadosDelEnunciado() {
        List<String> requeridos = List.of(
                "EN_DEPOSITO",
                "ASIGNADO",
                "LISTA_PARA_ENTREGAR",
                "EN_TRASLADO",
                "ENTREGADO",
                "ENTREGA_FALLIDA",
                "VENCIDO"
        );

        for (String requerido : requeridos) {
            assertTrue(Arrays.stream(Estado.values()).anyMatch(estado -> estado.name().equals(requerido)),
                    "falta el estado " + requerido + " en el enum");
        }
    }
}
