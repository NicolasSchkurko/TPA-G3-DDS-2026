package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("conseguirMisiones: respeta el orden del admin")
class RepositorioMisionesOrdenTest {

    // CALLS_REAL_METHODS corre conseguirMisiones de verdad; solo findAllById queda simulado.
    private final RepositorioMisiones repo =
            mock(RepositorioMisiones.class, Answers.CALLS_REAL_METHODS);

    private static Mision misionConId(UUID id) {
        Mision mision = new Mision("M" + id, null, "d", "I" + id,
                null);
        ReflectionTestUtils.setField(mision, "idMision", id);
        return mision;
    }

    @Test
    @DisplayName("devuelve las misiones en el orden pedido, no en el que devuelva la base")
    void devuelveEnElOrdenPedido() {
        UUID primero = UUID.randomUUID();
        UUID segundo = UUID.randomUUID();

        // La base responde en el orden contrario: sin reordenar, el donante arrancaria en
        // la segunda mision.
        when(repo.findAllById(any()))
                .thenReturn(List.of(misionConId(segundo), misionConId(primero)));

        List<Mision> resultado = repo.conseguirMisiones(List.of(primero, segundo));

        assertThat(resultado).extracting(Mision::getIdMision)
                            .containsExactly(primero, segundo);
    }

    @Test
    @DisplayName("un id repetido no altera el orden ni rompe nada")
    void unIdRepetidoNoAlteraElOrden() {
        UUID primero = UUID.randomUUID();
        UUID segundo = UUID.randomUUID();

        when(repo.findAllById(any()))
                .thenReturn(List.of(misionConId(segundo), misionConId(primero)));

        List<Mision> resultado = repo.conseguirMisiones(List.of(primero, primero, segundo));

        assertThat(resultado).extracting(Mision::getIdMision)
                            .containsExactly(primero, segundo);
    }

    @Test
    @DisplayName("si falta alguna misión responde un error claro")
    void siFaltaAlgunaMisionRespondeUnErrorClaro() {
        UUID existe = UUID.randomUUID();
        UUID noExiste = UUID.randomUUID();

        when(repo.findAllById(any())).thenReturn(List.of(misionConId(existe)));

        assertThatThrownBy(() -> repo.conseguirMisiones(List.of(existe, noExiste)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("no existen");
    }

    @Test
    @DisplayName("una lista vacía o nula no consulta nada")
    void unaListaVaciaNoConsultaNada() {
        assertThat(repo.conseguirMisiones(null)).isEmpty();
        assertThat(repo.conseguirMisiones(List.of())).isEmpty();
    }
}
