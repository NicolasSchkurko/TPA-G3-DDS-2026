package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanificadorRutasServiceAsignarChoferesTest {

    @Mock private RepositorioRutas repoRutas;
    @Mock private RepositorioChoferes repoChoferes;
    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private RepositorioCamiones repoCamiones;
    @Mock private RepositorioParadas repoParadas;
    @Mock private PlanificadorDeRutas planificadorDominio;

    @InjectMocks
    private PlanificadorRutasService service;

    private Chofer chofer1;
    private Chofer chofer2;

    @BeforeEach
    void setUp() {
        chofer1 = new Chofer(UUID.randomUUID(), "Chofer 1", true);
        chofer2 = new Chofer(UUID.randomUUID(), "Chofer 2", true);
    }

    @Test
    @DisplayName("No debe asignar un chofer que ya se encuentra asignado a una ruta activa")
    void noAsignaChoferYaPresenteEnRutaActiva() {
        Camion camionActivo = new Camion("ACT111", 10.0, 3.0, 1000.0, false);
        camionActivo.setChofer(chofer1);

        Ruta rutaActiva = new Ruta(camionActivo);
        rutaActiva.setEstado(EstadoRuta.PROGRAMADA);

        Camion nuevoCamion = new Camion("NUE222", 10.0, 3.0, 1000.0, true);
        Ruta nuevaRuta = new Ruta(nuevoCamion);
        nuevaRuta.setEstado(EstadoRuta.PROGRAMADA);

        when(repoRutas.findAll()).thenReturn(List.of(rutaActiva));
        // repoChoferes devuelve ambos choferes con disponible=true (por ejemplo si aún no se refrescó la BD)
        when(repoChoferes.findAll()).thenReturn(List.of(chofer1, chofer2));

        List<Ruta> resultado = service.asignarChoferes(List.of(nuevaRuta));

        assertEquals(1, resultado.size());
        Camion camionAsignado = resultado.get(0).getCamionAsignado();
        assertNotNull(camionAsignado);
        assertNotNull(camionAsignado.getChofer());
        assertEquals(chofer2.getIdChofer(), camionAsignado.getChofer().getIdChofer(),
                "Debe haber asignado al chofer 2, excluyendo al chofer 1 que ya está en una ruta activa");
    }
}
