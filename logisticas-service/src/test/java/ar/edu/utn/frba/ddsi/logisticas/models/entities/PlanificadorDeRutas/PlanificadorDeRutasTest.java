package ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExterno;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanificadorDeRutasTest {

    @Mock
    private ProveedorRutasExterno proveedorExterno;

    @Test
    @DisplayName("Lotes de ítems deben recibir subconjuntos disjuntos de camiones disponibles")
    void lotesNoCompartenListaDeCamiones() {
        PlanificadorDeRutas planificador = new PlanificadorDeRutas();
        planificador.setProveedorExterno(proveedorExterno);

        // Creamos 150 ítems para forzar 2 lotes (100 y 50)
        List<ItemEntrega> items = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            ItemEntrega item = new ItemEntrega(UUID.randomUUID(), 1, UnidadDeMedida.UNIDADES, null);
            item.setEstado(EstadoEntrega.PENDIENTE);
            items.add(item);
        }

        Camion camionA = new Camion("AAA111", 10.0, 3.0, 1000.0, true);
        Camion camionB = new Camion("BBB222", 15.0, 3.5, 1500.0, true);
        List<Camion> camionesDisponibles = List.of(camionA, camionB);

        planificador.iniciarPlanificacion(items, camionesDisponibles);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Camion>> captorCamiones = ArgumentCaptor.forClass(List.class);
        verify(proveedorExterno, times(2)).solicitarPlanificacion(any(), captorCamiones.capture());

        List<List<Camion>> todosLosLotesCamiones = captorCamiones.getAllValues();
        assertEquals(2, todosLosLotesCamiones.size());

        List<Camion> camionesLote1 = todosLosLotesCamiones.get(0);
        List<Camion> camionesLote2 = todosLosLotesCamiones.get(1);

        assertFalse(camionesLote1.isEmpty(), "El lote 1 debe tener camiones asignados");
        assertFalse(camionesLote2.isEmpty(), "El lote 2 debe tener camiones asignados");

        Set<String> patentesLote1 = new HashSet<>(camionesLote1.stream().map(Camion::getPatente).toList());
        Set<String> patentesLote2 = new HashSet<>(camionesLote2.stream().map(Camion::getPatente).toList());

        // La intersección de patentes debe ser completamente vacía
        patentesLote1.retainAll(patentesLote2);
        assertTrue(patentesLote1.isEmpty(), "Ningún camión debe ser compartido entre dos lotes distintos del mismo día");
    }

    @Test
    @DisplayName("procesarCallbackRutas deja la ocupación del camión calculada desde la ruta")
    void procesarCallbackRutasActualizaLaCargaDelCamion() {
        PlanificadorDeRutas planificador = new PlanificadorDeRutas();

        Camion camion = new Camion("AAA111", 10.0, 3.0, 1000.0, true);
        Direccion direccion = new Direccion("Calle", null, 100, null, null,
                "Buenos Aires", "CABA", "Argentina");
        Entidad entidad = new Entidad(UUID.randomUUID(), direccion);
        ItemEntrega item = new ItemEntrega(UUID.randomUUID(), 10, UnidadDeMedida.KILOGRAMOS, entidad);

        List<Ruta> rutas = planificador.procesarCallbackRutas(
                Map.of(camion.getPatente(), List.of(item.getIdDonacion())),
                List.of(camion),
                List.of(item));

        assertEquals(1, rutas.size());
        assertEquals(10.0, camion.getPesoOcupado());
        assertEquals(0.01, camion.getVolumenOcupado(), 0.0001);
        assertEquals(camion.getPesoOcupado(), rutas.get(0).pesoTotalCargadoKg());
        assertEquals(camion.getVolumenOcupado(), rutas.get(0).volumenTotalCargadoM3());
    }
}
