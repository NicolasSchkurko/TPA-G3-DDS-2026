package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RutaServiceIniciarRutaTest {

    @Mock private RepositorioRutas repoRutas;
    @Mock private RepositorioChoferes repoChoferes;
    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private GestorCamiones gestorCamiones;
    @Mock private RepositorioCamiones repoCamiones;
    @Mock private GestorPublicacionEventos gestorPublicacionEventos;

    @InjectMocks
    private RutaService rutaService;

    @Test
    @DisplayName("iniciarRuta debe persistir urlSeguimiento calculada y no perderla en recarga")
    void iniciarRutaConservaUrlSeguimiento() {
        UUID idChofer = UUID.randomUUID();
        Chofer chofer = new Chofer(idChofer, "Chofer Tracking", true);

        Camion camion = new Camion("TRK123", 10.0, 3.0, 1000.0, true);
        camion.setChofer(chofer);

        Ruta rutaProgramada = new Ruta(camion);
        rutaProgramada.setEstado(EstadoRuta.PROGRAMADA);
        rutaProgramada.setUrlSeguimiento(null); // Inicialmente null

        String urlEsperada = "https://donaciones-app.example.com/seguimiento/" + rutaProgramada.getIdRuta();

        when(repoChoferes.findById(idChofer)).thenReturn(Optional.of(chofer));
        when(repoRutas.findByChoferYEstado(chofer, EstadoRuta.PROGRAMADA)).thenReturn(Optional.of(rutaProgramada));
        when(gestorPublicacionEventos.publicarInicioRuta(rutaProgramada)).thenAnswer(inv -> {
            Ruta r = inv.getArgument(0);
            r.setUrlSeguimiento(urlEsperada);
            return r;
        });

        // Simular que obtenerRutaActualizada devuelve la ruta con la URL persistida
        when(repoRutas.findById(rutaProgramada.getIdRuta())).thenReturn(Optional.of(rutaProgramada));
        when(repoItemEntrega.findAllById(any())).thenReturn(List.of());

        rutaService.iniciarRuta(idChofer);

        // repoRutas.save debe haberse llamado al menos dos veces (inmediatamente tras publicar y al final con EN_CURSO)
        ArgumentCaptor<Ruta> rutaCaptor = ArgumentCaptor.forClass(Ruta.class);
        verify(repoRutas, atLeast(2)).save(rutaCaptor.capture());

        List<Ruta> guardadas = rutaCaptor.getAllValues();
        Ruta guardadaFinal = guardadas.get(guardadas.size() - 1);

        assertEquals(EstadoRuta.EN_CURSO, guardadaFinal.getEstado());
        assertEquals(urlEsperada, guardadaFinal.getUrlSeguimiento(),
                "La URL de seguimiento debe conservarse intacta tras iniciar la ruta");
    }
}
