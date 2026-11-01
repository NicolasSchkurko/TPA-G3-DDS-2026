package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.ActualizacionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorPublicacionEventos;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntregaServiceActualizarEstadoTest {

    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private RepositorioRutas repoRutas;
    @Mock private GestorPublicacionEventos gestorPublicacionEventos;
    @Mock private PlatformTransactionManager gestorTransacciones;

    @InjectMocks
    private EntregaService entregaService;

    private static final UUID ID_DONACION = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private ItemEntrega item;

    @BeforeEach
    void setUp() {
        item = new ItemEntrega(ID_DONACION, 1, UnidadDeMedida.UNIDADES, null);
        when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.of(item));
        when(repoRutas.findByIdDonacion(ID_DONACION)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Reingreso a depósito administrativo falla si el ítem está en estado ENTREGADA")
    void reingresoFallaSiItemEntregado() {
        item.setEstado(EstadoEntrega.ENTREGADA);

        ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();
        dto.setEstado("PENDIENTE");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            entregaService.actualizarEstado(ID_DONACION, dto);
        });

        assertTrue(ex.getMessage().contains("NO_RECIBIDA"));
        verify(gestorPublicacionEventos, never()).publicarReingresoDeposito(any());
    }

    @Test
    @DisplayName("Reingreso a depósito administrativo falla si el ítem está en estado EN_CAMINO")
    void reingresoFallaSiItemEnCamino() {
        item.setEstado(EstadoEntrega.EN_CAMINO);

        ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();
        dto.setEstado("PENDIENTE");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            entregaService.actualizarEstado(ID_DONACION, dto);
        });

        assertTrue(ex.getMessage().contains("NO_RECIBIDA"));
        verify(gestorPublicacionEventos, never()).publicarReingresoDeposito(any());
    }

    @Test
    @DisplayName("Reingreso a depósito administrativo falla si el ítem ya está en estado PENDIENTE")
    void reingresoFallaSiItemYaPendiente() {
        item.setEstado(EstadoEntrega.PENDIENTE);

        ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();
        dto.setEstado("PENDIENTE");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            entregaService.actualizarEstado(ID_DONACION, dto);
        });

        assertTrue(ex.getMessage().contains("NO_RECIBIDA"));
        verify(gestorPublicacionEventos, never()).publicarReingresoDeposito(any());
    }

    @Test
    @DisplayName("Reingreso a depósito administrativo es exitoso si el ítem está en estado NO_RECIBIDA")
    void reingresoExitosoSiItemNoRecibida() {
        item.setEstado(EstadoEntrega.NO_RECIBIDA);

        ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();
        dto.setEstado("PENDIENTE");

        when(gestorPublicacionEventos.publicarReingresoDeposito(item)).thenReturn(item);

        assertDoesNotThrow(() -> {
            entregaService.actualizarEstado(ID_DONACION, dto);
        });

        verify(gestorPublicacionEventos, times(1)).publicarReingresoDeposito(item);
    }
}
