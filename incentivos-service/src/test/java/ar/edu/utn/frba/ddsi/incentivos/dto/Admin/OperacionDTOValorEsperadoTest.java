package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OperacionDTO: el valor esperado tiene que volver como texto plano")
class OperacionDTOValorEsperadoTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("el valor esperado vuelve sin las comillas del JSON")
    void elValorEsperadoVuelveSinComillas() {
        CantidadCoincidencias operacion =
                new CantidadCoincidencias(5, MAPPER.valueToTree("ENTREGADA"));

        OperacionDTO dto = OperacionDTO.desdeEntidad(operacion);

        // String.valueOf de un TextNode devuelve la representacion JSON, o sea
        // "ENTREGADA" CON comillas. Con eso el panel de admin hace GET -> PUT, la
        // mission se guarda con el valor entrecomillado, y despues ninguna donacion
        // con estado ENTREGADA coincide nunca.
        assertThat(dto.getValorEsperado()).isEqualTo("ENTREGADA");
    }

    @Test
    @DisplayName("el round-trip por el DTO no altera el valor esperado")
    void elRoundTripNoAlteraElValorEsperado() {
        CantidadCoincidencias original =
                new CantidadCoincidencias(5, MAPPER.valueToTree("ENTREGADA"));

        String vuelta = OperacionDTO.desdeEntidad(original).getValorEsperado();
        CantidadCoincidencias idaYVuelta = new CantidadCoincidencias(5, MAPPER.valueToTree(vuelta));

        // Lo que vuelve del DTO tiene que seguir siendo la MISMA regla, si no cada
        // edicion de la mision parece un cambio y reinicia el progreso de todos.
        assertThat(idaYVuelta.esEquivalenteA(original)).isTrue();
    }
}
