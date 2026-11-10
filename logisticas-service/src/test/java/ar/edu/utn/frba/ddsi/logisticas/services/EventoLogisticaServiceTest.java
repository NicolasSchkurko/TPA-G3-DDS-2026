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

    @Test
    @DisplayName("Consulta desde el id EXACTO: el repositorio ya devuelve los mayores")
    void consultaDesdeElIdExacto() {
        when(repoEventos.findByIdGreaterThanOrderByIdAsc(15L)).thenReturn(List.of(evento(16)));

        servicio().obtenerEventosNuevos(15L);

        verify(repoEventos).findByIdGreaterThanOrderByIdAsc(15L);
    }

    @Test
    @DisplayName("No le resta uno al id que le manda el cliente")
    void noLeRestaUno() {
        when(repoEventos.findByIdGreaterThanOrderByIdAsc(anyLong())).thenReturn(List.of());

        servicio().obtenerEventosNuevos(100L);

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

    @Test
    @DisplayName("Un desdeId null no revienta: se interpreta como 0, o sea 'mandame todo'")
    void unDesdeIdNullNoRevienta() {
        when(repoEventos.findByIdGreaterThanOrderByIdAsc(anyLong())).thenReturn(List.of(evento(1)));

        assertThatCode(() -> servicio().obtenerEventosNuevos(null)).doesNotThrowAnyException();

        verify(repoEventos).findByIdGreaterThanOrderByIdAsc(0L);
    }

    @Test
    @DisplayName("El repositorio es una derived query, no un default que filtra en memoria")
    void elRepositorioEsUnaDerivedQuery() throws NoSuchMethodException {
        Method metodo = RepositorioEventoLogistica.class
                .getMethod("findByIdGreaterThanOrderByIdAsc", Long.class);

        assertThat(metodo.isDefault())
                .as("con cuerpo propio el ORDER BY no lo garantiza nadie")
                .isFalse();

        assertThat(metodo.getName()).isEqualTo("findByIdGreaterThanOrderByIdAsc");
    }
}
