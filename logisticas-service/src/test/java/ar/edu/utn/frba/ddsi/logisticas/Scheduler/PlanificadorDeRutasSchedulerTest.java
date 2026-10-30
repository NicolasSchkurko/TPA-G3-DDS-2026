package ar.edu.utn.frba.ddsi.logisticas.Scheduler;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExterno;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanificadorDeRutasSchedulerTest {

    @Mock private ProveedorRutasExterno proveedorExterno;
    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private RepositorioCamiones repoCamiones;
    @Mock private RepositorioRutas repoRutas;

    private PlanificadorDeRutasScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PlanificadorDeRutasScheduler(proveedorExterno, repoItemEntrega, repoCamiones, repoRutas);
    }

    @Test
    @DisplayName("No debe volver a enviar al proveedor externo ítems que ya pertenecen a una ruta activa")
    void noReplanificaItemsEnRutasActivas() {
        UUID idItemEnRuta = UUID.randomUUID();
        ItemEntrega itemEnRuta = new ItemEntrega(idItemEnRuta, 10, UnidadDeMedida.UNIDADES, null);
        itemEnRuta.setEstado(EstadoEntrega.PENDIENTE);

        UUID idItemNuevo = UUID.randomUUID();
        ItemEntrega itemNuevo = new ItemEntrega(idItemNuevo, 5, UnidadDeMedida.UNIDADES, null);
        itemNuevo.setEstado(EstadoEntrega.PENDIENTE);

        Camion camion = new Camion("ABC123", 20.0, 10.0, 2000.0, true);
        Ruta rutaActiva = new Ruta(camion);
        rutaActiva.setEstado(EstadoRuta.PROGRAMADA);

        Parada parada = new Parada();
        parada.getItems().add(itemEnRuta);
        rutaActiva.getParadas().add(parada);

        when(repoItemEntrega.findByEstado(EstadoEntrega.PENDIENTE)).thenReturn(List.of(itemEnRuta, itemNuevo));
        when(repoCamiones.findAll()).thenReturn(List.of(camion));
        when(repoRutas.findAll()).thenReturn(List.of(rutaActiva));

        scheduler.iniciarPlanificacionAutomatica();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ItemEntrega>> loteCaptor = ArgumentCaptor.forClass(List.class);
        verify(proveedorExterno).solicitarPlanificacion(loteCaptor.capture(), any());

        List<ItemEntrega> loteEnviado = loteCaptor.getValue();
        assertEquals(1, loteEnviado.size(), "El lote enviado debe contener únicamente los ítems no planificados");
        assertEquals(idItemNuevo, loteEnviado.get(0).getIdDonacion());
        assertFalse(loteEnviado.stream().anyMatch(i -> i.getIdDonacion().equals(idItemEnRuta)),
                "El ítem ya presente en una ruta PROGRAMADA no debe enviarse nuevamente");
    }

    @Test
    @DisplayName("Si todas las donaciones pendientes ya están en rutas activas, no solicita planificación")
    void noSolicitaPlanificacionSiTodosEstanEnRutasActivas() {
        UUID idItem = UUID.randomUUID();
        ItemEntrega item = new ItemEntrega(idItem, 10, UnidadDeMedida.UNIDADES, null);
        item.setEstado(EstadoEntrega.PENDIENTE);

        Camion camion = new Camion("ABC123", 20.0, 10.0, 2000.0, true);
        Ruta rutaActiva = new Ruta(camion);
        rutaActiva.setEstado(EstadoRuta.PROGRAMADA);

        Parada parada = new Parada();
        parada.getItems().add(item);
        rutaActiva.getParadas().add(parada);

        when(repoItemEntrega.findByEstado(EstadoEntrega.PENDIENTE)).thenReturn(List.of(item));
        when(repoRutas.findAll()).thenReturn(List.of(rutaActiva));

        scheduler.iniciarPlanificacionAutomatica();

        verify(proveedorExterno, never()).solicitarPlanificacion(any(), any());
    }
}
