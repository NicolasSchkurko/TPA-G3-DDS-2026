package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaResponseDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEventoLogistica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lectura de eventos de trazabilidad por id: la que expone {@code GET /api/eventos}.
 *
 * <p><b>El off-by-one era el que más dolía.</b> El service pedía {@code id > desdeId - 1}, o
 * sea {@code id >= desdeId}: el evento con {@code id == desdeId} es exactamente el último que el
 * cliente ya procesó, y se lo volvía a mandar. El propio contrato del endpoint dice
 * "estrictamente mayor", y el código hacía otra cosa.
 *
 * <p><b>El repositorio tenía que dejar de filtrar en memoria.</b> Era un {@code default} method
 * con {@code findAll()} y un stream: traía la tabla entera y el orden dependía de lo que
 * MySQL tuviera ganas de devolver. Un cliente que consulta por id necesita el orden
 * garantizado, y eso solo lo puede dar la consulta.
 *
 * <p>Sin base embebida en el build offline, la consulta se verifica por su declaración —que sea
 * una derived query y no un método con cuerpo— y el criterio, por el id exacto que se le pasa
 * al repositorio.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("La lectura de eventos devuelve desde el id siguiente, en orden, y no explota con desdeId null")
class EventoLogisticaServiceTest {

    @Mock private RepositorioEventoLogistica repoEventos;

    private EventoLogisticaService servicio() {
        return new EventoLogisticaService(repoEventos);
    }

    private static EventoLogistica evento(long id) {
        EventoLogistica evento = new EventoLogistica("ENTREGA_CONFIRMADA", "uuid-" + id,
                LocalDateTime.now(), null);
        evento.setId(id);
        return evento;
    }

    // --- El off-by-one ---

    @Test
    @DisplayName("Consulta desde el id EXACTO: el repositorio ya devuelve los mayores")
    void consultaDesdeElIdExacto() {
        // El repositorio devuelve id > :id. Restarle uno aca convertia el "estrictamente mayor"
        // en "mayor o igual" y el cliente recibia una y otra vez su ultimo evento.
        when(repoEventos.findByIdGreaterThanOrderByIdAsc(15L)).thenReturn(List.of(evento(16)));

        servicio().obtenerEventosNuevos(15L);

        verify(repoEventos).findByIdGreaterThanOrderByIdAsc(15L);
    }

    @Test
    @DisplayName("No le resta uno al id que le manda el cliente")
    void noLeRestaUno() {
        when(repoEventos.findByIdGreaterThanOrderByIdAsc(anyLong())).thenReturn(List.of());

        servicio().obtenerEventosNuevos(100L);

        // 99 es el bug: el cliente volvia a recibir el evento 100, que ya habia procesado.
        verify(repoEventos, never()).findByIdGreaterThanOrderByIdAsc(99L);
        verify(repoEventos).findByIdGreaterThanOrderByIdAsc(100L);
    }

    @Test
    @DisplayName("Devuelve los eventos que el repositorio trae, ya convertidos a DTO")
    void devuelveLosEventosConvertidos() {
        when(repoEventos.findByIdGreaterThanOrderByIdAsc(15L))
                .thenReturn(List.of(evento(16), evento(17)));

        EventoLogisticaResponseDTO respuesta = servicio().obtenerEventosNuevos(15L);

        assertThat(respuesta.getEventos()).hasSize(2);
        assertThat(respuesta.getEventos()).extracting("id").containsExactly(16L, 17L);
        assertThat(respuesta.getEventos()).extracting("tipoEvento")
                .containsOnly("ENTREGA_CONFIRMADA");
    }

    // --- El null ---

    @Test
    @DisplayName("Un desdeId null no revienta: se interpreta como 0, o sea 'mandame todo'")
    void unDesdeIdNullNoRevienta() {
        // desdeId - 1 sobre un Long null desempaquetaba y lanzaba NullPointerException. Un
        // cliente que recien arranca y no manda cursor quiere todos los eventos, no un error.
        when(repoEventos.findByIdGreaterThanOrderByIdAsc(anyLong())).thenReturn(List.of(evento(1)));

        assertThatCode(() -> servicio().obtenerEventosNuevos(null)).doesNotThrowAnyException();

        verify(repoEventos).findByIdGreaterThanOrderByIdAsc(0L);
    }

    @Test
    @DisplayName("Desde 0 salen todos los eventos, que es lo que espera un cliente recien arrancado")
    void desdeCeroSaliendoTodos() {
        when(repoEventos.findByIdGreaterThanOrderByIdAsc(0L)).thenReturn(List.of(evento(1)));

        assertThat(servicio().obtenerEventosNuevos(null).getEventos()).hasSize(1);
        assertThat(servicio().obtenerEventosNuevos(0L).getEventos()).hasSize(1);
    }

    // --- El repositorio tiene que filtrar y ordenar en la base ---

    @Test
    @DisplayName("El repositorio es una derived query, no un default que filtra en memoria")
    void elRepositorioEsUnaDerivedQuery() throws NoSuchMethodException {
        Method metodo = RepositorioEventoLogistica.class
                .getMethod("findByIdGreaterThanOrderByIdAsc", Long.class);

        // Un default method trae el cuerpo puesto y por lo tanto no lo genera Spring Data:
        // el findAll() + stream de antes era Java, no SQL, y por eso no garantizaba el orden.
        assertThat(metodo.isDefault())
                .as("con cuerpo propio el ORDER BY no lo garantiza nadie")
                .isFalse();

        // Y el nombre tiene que seguir diciendo "ordenado ascendente", porque es lo que Spring
        // Data traduce a un ORDER BY.
        assertThat(metodo.getName()).isEqualTo("findByIdGreaterThanOrderByIdAsc");
    }
}
