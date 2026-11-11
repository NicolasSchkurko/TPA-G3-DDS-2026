package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioParadas;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestorAsignacionChoferesTest {

    @Mock private RepositorioRutas repoRutas;
    @Mock private RepositorioChoferes repoChoferes;
    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private RepositorioCamiones repoCamiones;
    @Mock private RepositorioParadas repoParadas;
    @Mock private MongoTemplate mongoTemplate;

    private Chofer chofer1;
    private Chofer chofer2;

    @BeforeEach
    void setUp() {
        chofer1 = new Chofer(UUID.randomUUID(), "Chofer 1", true);
        chofer2 = new Chofer(UUID.randomUUID(), "Chofer 2", true);
    }

    private GestorAsignacionChoferes gestor() {
        return new GestorAsignacionChoferes(repoRutas, repoChoferes, repoItemEntrega,
                repoCamiones, repoParadas, mongoTemplate);
    }

    private void stubReclamacionExitosa() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Chofer.class)))
                .thenAnswer(inv -> {
                    UUID id = (UUID) ((Query) inv.getArgument(0)).getQueryObject().get("_id");
                    Chofer reclamado = List.of(chofer1, chofer2).stream()
                            .filter(c -> c.getIdChofer().equals(id))
                            .findFirst().orElse(null);
                    if (reclamado != null) {
                        reclamado.setVersion(1L);
                        reclamado.ocupado();
                    }
                    return reclamado;
                });
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

        when(repoRutas.findByEstadoIn(eq(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))))
                .thenReturn(List.of(rutaActiva));
        when(repoChoferes.findAll()).thenReturn(List.of(chofer1, chofer2));
        stubReclamacionExitosa();

        List<Ruta> resultado = gestor().asignarChoferes(List.of(nuevaRuta));

        assertEquals(1, resultado.size());
        Camion camionAsignado = resultado.get(0).getCamionAsignado();
        assertNotNull(camionAsignado);
        assertNotNull(camionAsignado.getChofer());
        assertEquals(chofer2.getIdChofer(), camionAsignado.getChofer().getIdChofer());
    }

    @Test
    @DisplayName("No debe asignar un chofer que fue dado de baja (inactivo)")
    void noAsignaChoferInactivo() {
        chofer1.darDeBaja();

        Camion nuevoCamion = new Camion("CAM333", 10.0, 3.0, 1000.0, true);
        Ruta nuevaRuta = new Ruta(nuevoCamion);
        nuevaRuta.setEstado(EstadoRuta.PROGRAMADA);

        when(repoRutas.findByEstadoIn(eq(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))))
                .thenReturn(List.of());
        when(repoChoferes.findAll()).thenReturn(List.of(chofer1, chofer2));
        stubReclamacionExitosa();

        List<Ruta> resultado = gestor().asignarChoferes(List.of(nuevaRuta));

        assertEquals(1, resultado.size());
        assertEquals(chofer2.getIdChofer(),
                resultado.get(0).getCamionAsignado().getChofer().getIdChofer());
    }

    @Test
    @DisplayName("Cuando hay más rutas que choferes disponibles, solo se persisten las asignadas")
    void soloPersisteRutasQueTienenChoferAsignado() {
        Camion camion1 = new Camion("CAM111", 10.0, 3.0, 1000.0, true);
        Ruta ruta1 = new Ruta(camion1);
        ruta1.setEstado(EstadoRuta.PROGRAMADA);

        Camion camion2 = new Camion("CAM222", 10.0, 3.0, 1000.0, true);
        Ruta ruta2 = new Ruta(camion2);
        ruta2.setEstado(EstadoRuta.PROGRAMADA);

        when(repoRutas.findByEstadoIn(eq(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))))
                .thenReturn(List.of());
        when(repoChoferes.findAll()).thenReturn(List.of(chofer1));
        stubReclamacionExitosa();

        List<Ruta> resultado = gestor().asignarChoferes(List.of(ruta1, ruta2));

        assertEquals(1, resultado.size());
        assertNotNull(resultado.get(0).getCamionAsignado().getChofer());
        verify(repoRutas, times(1)).save(resultado.get(0));
        assertFalse(chofer1.isDisponible());
        assertFalse(resultado.get(0).getCamionAsignado().getDisponible());
    }

    @Test
    @DisplayName("Cuando no hay choferes disponibles, no se asigna ni se persiste ninguna ruta")
    void noPersisteRutasSiNoHayChoferesDisponibles() {
        Ruta ruta = new Ruta(new Camion("CAM111", 10.0, 3.0, 1000.0, true));
        ruta.setEstado(EstadoRuta.PROGRAMADA);

        when(repoRutas.findByEstadoIn(eq(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))))
                .thenReturn(List.of());
        when(repoChoferes.findAll()).thenReturn(List.of());

        List<Ruta> resultado = gestor().asignarChoferes(List.of(ruta));

        assertTrue(resultado.isEmpty());
        verify(repoRutas, never()).save(any(Ruta.class));
        verify(repoCamiones, never()).save(any(Camion.class));
    }

    @Test
    @DisplayName("Si otra réplica ya reclamó el chofer, no se asigna ni se persiste la ruta")
    void noAsignaChoferReclamadoPorOtraReplica() {
        Camion camion = new Camion("CAM999", 10.0, 3.0, 1000.0, true);
        Ruta ruta = new Ruta(camion);
        ruta.setEstado(EstadoRuta.PROGRAMADA);

        when(repoRutas.findByEstadoIn(eq(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))))
                .thenReturn(List.of());
        when(repoChoferes.findAll()).thenReturn(List.of(chofer1));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Chofer.class))).thenReturn(null);

        List<Ruta> resultado = gestor().asignarChoferes(List.of(ruta));

        assertTrue(resultado.isEmpty());
        assertNull(camion.getChofer());
        verify(repoRutas, never()).save(any(Ruta.class));
        verify(repoCamiones, never()).save(any(Camion.class));
    }

    @Test
    @DisplayName("El claim filtra por disponible/activo y marca disponible=false con versión")
    void elClaimUsaFiltroYUpdateAtomicos() {
        Ruta ruta = new Ruta(new Camion("CAM777", 10.0, 3.0, 1000.0, true));
        ruta.setEstado(EstadoRuta.PROGRAMADA);

        when(repoRutas.findByEstadoIn(eq(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))))
                .thenReturn(List.of());
        when(repoChoferes.findAll()).thenReturn(List.of(chofer1));
        stubReclamacionExitosa();

        gestor().asignarChoferes(List.of(ruta));

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        ArgumentCaptor<FindAndModifyOptions> optionsCaptor =
                ArgumentCaptor.forClass(FindAndModifyOptions.class);
        verify(mongoTemplate).findAndModify(queryCaptor.capture(), updateCaptor.capture(),
                optionsCaptor.capture(), eq(Chofer.class));
        assertTrue(optionsCaptor.getValue().isReturnNew());

        Document query = queryCaptor.getValue().getQueryObject();
        assertEquals(chofer1.getIdChofer(), query.get("_id"));
        assertEquals(true, query.get("disponible"));
        assertEquals(new Document("$ne", false), query.get("activo"));

        Document update = updateCaptor.getValue().getUpdateObject();
        assertEquals(false, ((Document) update.get("$set")).get("disponible"));
        assertEquals(Integer.valueOf(1), ((Document) update.get("$inc")).get("version"));
    }

    @Test
    @DisplayName("Si el primer chequeo falla, reintenta con otro chofer y asigna una sola vez")
    void reintentaConOtroChoferSiElPrimeroFueReclamado() {
        Camion camion = new Camion("CAM888", 10.0, 3.0, 1000.0, true);
        Ruta ruta = new Ruta(camion);
        ruta.setEstado(EstadoRuta.PROGRAMADA);

        when(repoRutas.findByEstadoIn(eq(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))))
                .thenReturn(List.of());
        when(repoChoferes.findAll()).thenReturn(List.of(chofer1, chofer2));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Chofer.class)))
                .thenReturn(null, chofer2);

        List<Ruta> resultado = gestor().asignarChoferes(List.of(ruta));

        assertEquals(1, resultado.size());
        assertEquals(chofer2.getIdChofer(), camion.getChofer().getIdChofer());
        verify(repoRutas, times(1)).save(ruta);
        verify(repoCamiones, times(1)).save(camion);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(2)).findAndModify(queryCaptor.capture(), any(Update.class),
                any(FindAndModifyOptions.class), eq(Chofer.class));
        assertNotEquals(queryCaptor.getAllValues().get(0).getQueryObject().get("_id"),
                queryCaptor.getAllValues().get(1).getQueryObject().get("_id"));
    }

    @Test
    @DisplayName("Una ruta sin camión no se persiste ni consume chofer")
    void rutaSinCamionNoSePersiste() {
        Ruta ruta = new Ruta(null);
        ruta.setEstado(EstadoRuta.PROGRAMADA);

        when(repoRutas.findByEstadoIn(eq(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))))
                .thenReturn(List.of());
        when(repoChoferes.findAll()).thenReturn(List.of(chofer1));

        List<Ruta> resultado = gestor().asignarChoferes(List.of(ruta));

        assertTrue(resultado.isEmpty());
        verify(repoRutas, never()).save(any(Ruta.class));
        verify(repoCamiones, never()).save(any(Camion.class));
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Chofer.class));
    }

    @Test
    @DisplayName("Si falla el guardado posterior, libera el chofer reclamado y propaga el error")
    void liberaChoferSiFallaElGuardado() {
        Ruta ruta = new Ruta(new Camion("CAM666", 10.0, 3.0, 1000.0, true));
        ruta.setEstado(EstadoRuta.PROGRAMADA);

        when(repoRutas.findByEstadoIn(eq(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))))
                .thenReturn(List.of());
        when(repoChoferes.findAll()).thenReturn(List.of(chofer1));
        stubReclamacionExitosa();
        when(repoCamiones.save(any(Camion.class))).thenThrow(new RuntimeException("falló el guardado"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> gestor().asignarChoferes(List.of(ruta)));
        assertEquals("falló el guardado", thrown.getMessage());

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(Chofer.class));

        Document query = queryCaptor.getValue().getQueryObject();
        assertEquals(chofer1.getIdChofer(), query.get("_id"));
        assertEquals(Long.valueOf(1), query.get("version"));
        assertEquals(new Document("$ne", false), query.get("activo"));

        Document update = updateCaptor.getValue().getUpdateObject();
        assertEquals(true, ((Document) update.get("$set")).get("disponible"));
        assertEquals(Integer.valueOf(1), ((Document) update.get("$inc")).get("version"));
    }

    @Test
    @DisplayName("Si falla una parada no se persiste la ruta y se libera el chofer")
    void noPersisteRutaSiFallaUnaParada() {
        Ruta ruta = new Ruta(new Camion("CAM555", 10.0, 3.0, 1000.0, true));
        ruta.setEstado(EstadoRuta.PROGRAMADA);
        ruta.setParadas(List.of(new Parada()));

        when(repoRutas.findByEstadoIn(eq(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))))
                .thenReturn(List.of());
        when(repoChoferes.findAll()).thenReturn(List.of(chofer1));
        stubReclamacionExitosa();
        when(repoParadas.save(any(Parada.class))).thenThrow(new RuntimeException("falló la parada"));

        assertThrows(RuntimeException.class, () -> gestor().asignarChoferes(List.of(ruta)));
        verify(repoRutas, never()).save(any(Ruta.class));
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Chofer.class));
    }

    @Test
    @DisplayName("Si falla la liberación se conserva la excepción original")
    void siFallaLaLiberacionConservaLaExcepcionOriginal() {
        Ruta ruta = new Ruta(new Camion("CAM444", 10.0, 3.0, 1000.0, true));
        ruta.setEstado(EstadoRuta.PROGRAMADA);

        when(repoRutas.findByEstadoIn(eq(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))))
                .thenReturn(List.of());
        when(repoChoferes.findAll()).thenReturn(List.of(chofer1));
        stubReclamacionExitosa();
        when(repoCamiones.save(any(Camion.class))).thenThrow(new RuntimeException("original"));
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Chofer.class)))
                .thenThrow(new RuntimeException("release"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> gestor().asignarChoferes(List.of(ruta)));
        assertEquals("original", thrown.getMessage());
        assertEquals(1, thrown.getSuppressed().length);
        assertEquals("release", thrown.getSuppressed()[0].getMessage());
    }

    @Test
    @DisplayName("El camión guardado tras el callback tiene la ocupación de la ruta")
    void persisteLaCargaDelCamionAsignado() {
        Camion camion = new Camion("AAA111", 10.0, 3.0, 1000.0, true);
        Chofer chofer = new Chofer(UUID.randomUUID(), "Chofer", true);

        Direccion direccion = new Direccion("Calle", null, 100, null, null,
                "Buenos Aires", "CABA", "Argentina");
        Entidad entidad = new Entidad(UUID.randomUUID(), direccion);
        ItemEntrega item = new ItemEntrega(UUID.randomUUID(), 10, UnidadDeMedida.KILOGRAMOS, entidad);

        PlanificadorDeRutas planner = new PlanificadorDeRutas();
        List<Ruta> rutas = planner.procesarCallbackRutas(
                Map.of(camion.getPatente(), List.of(item.getIdDonacion())),
                List.of(camion),
                List.of(item));

        when(repoRutas.findByEstadoIn(eq(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))))
                .thenReturn(List.of());
        when(repoChoferes.findAll()).thenReturn(List.of(chofer));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Chofer.class))).thenReturn(chofer);

        gestor().asignarChoferes(rutas);

        ArgumentCaptor<Camion> captor = ArgumentCaptor.forClass(Camion.class);
        verify(repoCamiones).save(captor.capture());
        assertEquals(10.0, captor.getValue().getPesoOcupado());
        assertEquals(0.01, captor.getValue().getVolumenOcupado(), 0.0001);
    }
}
