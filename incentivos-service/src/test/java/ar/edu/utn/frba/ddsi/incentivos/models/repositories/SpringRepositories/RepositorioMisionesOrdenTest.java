package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import org.mockito.Answers;
import static org.mockito.Mockito.when;

/**
 * El orden en que el admin manda las misiones es el orden en que las ve el donante
 * (punto 27).
 *
 * <p>{@code Categoria.agregarMision} va asignando {@code posicion = size + 1}, así que el
 * orden de la lista es la secuencia de progresión. Como {@code findAllById} no tiene
 * {@code ORDER BY}, el orden con que llegaba desde la base no estaba garantizado y el
 * donante podía arrancar en otra misión.
 */
@DisplayName("conseguirMisiones: respeta el orden del admin")
class RepositorioMisionesOrdenTest {

    // Un mock normal de una interfaz no ejecuta los metodos default, que es justamente lo
// que se quiere probar. Con CALLS_REAL_METHODS corre conseguirMisiones de verdad y solo
// findAllById queda simulado.
private final RepositorioMisiones repo =
            mock(RepositorioMisiones.class, Answers.CALLS_REAL_METHODS);

    private static Mision misionConId(UUID id) {
        Mision mision = new Mision("M" + id, null, "d", "I" + id,
                null);
        mision.setIdMision(id);
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
