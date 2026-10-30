package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorPublicacionEventos;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RutaServiceTerminarRutaTest {

    @Mock private RepositorioRutas repoRutas;
    @Mock private RepositorioChoferes repoChoferes;
    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private GestorCamiones gestorCamiones;
    @Mock private RepositorioCamiones repoCamiones;
    @Mock private GestorPublicacionEventos gestorPublicacionEventos;

    @InjectMocks private RutaService rutaService;

    @Test
    @DisplayName("Terminar ruta no debe reingresar a depósito un ítem ya ENTREGADA")
    void noReingresaItemEntregado() {
        UUID idChofer = UUID.randomUUID();
        Chofer chofer = new Chofer(idChofer, "Chofer Test", true);

        Camion camion = new Camion("ABC123", 10.0, 3.0, 1000.0, true);
        camion.setChofer(chofer);

        Ruta rutaEnCurso = new Ruta(camion);
        rutaEnCurso.setEstado(EstadoRuta.EN_CURSO);

        UUID idDonacionEntregada = UUID.randomUUID();
        ItemEntrega itemEnRuta = new ItemEntrega(idDonacionEntregada, 5, UnidadDeMedida.UNIDADES, null);
        itemEnRuta.setEstado(EstadoEntrega.EN_CAMINO); // En la ruta persistida sigue EN_CAMINO

        Parada parada = new Parada();
        parada.getItems().add(itemEnRuta);
        rutaEnCurso.getParadas().add(parada);

        // En repoItemEntrega el ítem YA fue confirmado como ENTREGADA
        ItemEntrega itemActualizado = new ItemEntrega(idDonacionEntregada, 5, UnidadDeMedida.UNIDADES, null);
        itemActualizado.setEstado(EstadoEntrega.ENTREGADA);

        when(repoChoferes.findById(idChofer)).thenReturn(Optional.of(chofer));
        when(repoRutas.findByChoferYEstado(chofer, EstadoRuta.EN_CURSO)).thenReturn(Optional.of(rutaEnCurso));
        when(repoRutas.findById(rutaEnCurso.getIdRuta())).thenReturn(Optional.of(rutaEnCurso));
        when(repoItemEntrega.findAllById(any())).thenReturn(List.of(itemActualizado));
        when(repoItemEntrega.existsById(idDonacionEntregada)).thenReturn(true);

        rutaService.terminarRuta(idChofer);

        // Debe eliminar el ítem entregado de repoItemEntrega y NUNCA publicar reingreso
        verify(gestorPublicacionEventos, never()).publicarReingresoDeposito(any());
        verify(repoItemEntrega).deleteById(idDonacionEntregada);
    }
}
