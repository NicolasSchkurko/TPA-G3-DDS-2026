package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaResponseDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEventoLogistica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoLogisticaServiceObtenerEventosNuevosTest {

    @Mock
    private RepositorioEventoLogistica repoEventos;

    @InjectMocks
    private EventoLogisticaService service;

    @Test
    @DisplayName("Cuando desdeId es null, no debe lanzar NullPointerException y debe consultar desde 0L")
    void toleraNullSinNPE() {
        when(repoEventos.findByIdGreaterThanOrderByIdAsc(0L)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> {
            EventoLogisticaResponseDTO response = service.obtenerEventosNuevos(null);
            assertNotNull(response);
            assertTrue(response.getEventos().isEmpty());
        });

        verify(repoEventos).findByIdGreaterThanOrderByIdAsc(0L);
    }

    @Test
    @DisplayName("Cuando desdeId tiene un valor, no debe restarle 1 para no duplicar eventos")
    void noRestaUnoADesdeId() {
        when(repoEventos.findByIdGreaterThanOrderByIdAsc(15L)).thenReturn(Collections.emptyList());

        EventoLogisticaResponseDTO response = service.obtenerEventosNuevos(15L);
        assertNotNull(response);

        verify(repoEventos).findByIdGreaterThanOrderByIdAsc(15L);
        verify(repoEventos, never()).findByIdGreaterThanOrderByIdAsc(14L);
    }
}
