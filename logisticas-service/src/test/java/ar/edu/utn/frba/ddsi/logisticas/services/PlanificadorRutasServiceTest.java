package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorAsignacionChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanificadorRutasServiceTest {

    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private RepositorioCamiones repoCamiones;
    @Mock private PlanificadorDeRutas planificadorDominio;
    @Mock private GestorAsignacionChoferes gestorAsignacionChoferes;

    private PlanificadorRutasService service() {
        return new PlanificadorRutasService(repoItemEntrega, repoCamiones,
                planificadorDominio, gestorAsignacionChoferes);
    }

    @Test
    @DisplayName("Punto 71: el callback resuelve todos los ítems con una sola consulta")
    void procesarCallbackRutasUsaUnaSolaConsultaDeItems() {
        UUID idItem = UUID.randomUUID();
        String json = "{\"CAM111\": [\"" + idItem + "\"]}";

        Camion camion = new Camion("CAM111", 10.0, 3.0, 1000.0, true);
        ItemEntrega item = new ItemEntrega();
        item.setIdDonacion(idItem);

        when(repoCamiones.findAllById(any())).thenReturn(List.of(camion));
        when(repoItemEntrega.findAllById(List.of(idItem))).thenReturn(List.of(item));
        when(planificadorDominio.procesarCallbackRutas(any(), any(), any())).thenReturn(List.of());

        service().procesarCallbackRutas(json);

        verify(repoItemEntrega, times(1)).findAllById(List.of(idItem));
        verify(repoItemEntrega, never()).findById(any());
    }

    @Test
    @DisplayName("Punto 71: si un id del callback no existe, responde con el id faltante")
    void procesarCallbackRutasFallaSiFaltaUnItem() {
        UUID idItem = UUID.randomUUID();
        String json = "{\"CAM111\": [\"" + idItem + "\"]}";

        when(repoCamiones.findAllById(any()))
                .thenReturn(List.of(new Camion("CAM111", 10.0, 3.0, 1000.0, true)));
        when(repoItemEntrega.findAllById(List.of(idItem))).thenReturn(List.of());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service().procesarCallbackRutas(json));

        assertTrue(error.getMessage().contains(idItem.toString()));
    }

    @Test
    @DisplayName("El callback no asigna ni persiste rutas: solo devuelve el resultado del dominio")
    void elCallbackNoAsignaNiPersisteRutas() {
        UUID idItem = UUID.randomUUID();
        String json = "{\"CAM111\": [\"" + idItem + "\"]}";

        Camion camion = new Camion("CAM111", 10.0, 3.0, 1000.0, true);
        ItemEntrega item = new ItemEntrega();
        item.setIdDonacion(idItem);

        when(repoCamiones.findAllById(any())).thenReturn(List.of(camion));
        when(repoItemEntrega.findAllById(List.of(idItem))).thenReturn(List.of(item));
        when(planificadorDominio.procesarCallbackRutas(any(), any(), any())).thenReturn(List.of());

        service().procesarCallbackRutas(json);

        verify(planificadorDominio, times(1)).procesarCallbackRutas(any(), any(), any());
        verify(gestorAsignacionChoferes, never()).asignarChoferes(any());
    }

    @Test
    @DisplayName("asignarChoferes delega en el gestor")
    void asignarChoferesDelegaEnElGestor() {
        List<ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta> rutas = List.of();
        when(gestorAsignacionChoferes.asignarChoferes(rutas)).thenReturn(rutas);

        assertEquals(rutas, service().asignarChoferes(rutas));
        verify(gestorAsignacionChoferes).asignarChoferes(rutas);
    }
}
