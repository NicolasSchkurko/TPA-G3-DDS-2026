package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.rutas.RutaDTO;
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
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RutaService")
class RutaServiceTest {

    @Mock private RepositorioRutas repoRutas;
    @Mock private RepositorioChoferes repoChoferes;
    @Mock private RepositorioItemEntrega repoItemEntrega;
    private final GestorCamiones gestorCamiones = new GestorCamiones();
    @Mock private RepositorioCamiones repoCamiones;
    @Mock private GestorPublicacionEventos gestorPublicacionEventos;

    private RutaService rutaService;

    @BeforeEach
    void setUp() {
        GestorRutas gestorRutas = new GestorRutas(repoRutas, repoItemEntrega);
        rutaService = new RutaService(repoRutas, repoChoferes, gestorCamiones,
                repoCamiones, gestorPublicacionEventos, gestorRutas);
    }

    // --- iniciar ruta ---

    @Test
    @DisplayName("iniciarRuta debe persistir urlSeguimiento calculada y no perderla en recarga")
    void iniciarRutaConservaUrlSeguimiento() {
        UUID idChofer = UUID.randomUUID();
        Chofer chofer = new Chofer(idChofer, "Chofer Tracking", true);

        Camion camion = new Camion("TRK123", 10.0, 3.0, 1000.0, true);
        camion.setChofer(chofer);

        Ruta rutaProgramada = new Ruta(camion);
        rutaProgramada.setEstado(EstadoRuta.PROGRAMADA);
        rutaProgramada.setUrlSeguimiento(null);

        String urlEsperada = "http://localhost:8080/seguimiento/" + rutaProgramada.getIdRuta();

        when(repoChoferes.findById(idChofer)).thenReturn(Optional.of(chofer));
        when(repoRutas.findFirstByCamionAsignadoChoferIdChoferAndEstado(chofer.getIdChofer(), EstadoRuta.PROGRAMADA)).thenReturn(Optional.of(rutaProgramada));
        when(gestorPublicacionEventos.publicarInicioRuta(rutaProgramada)).thenAnswer(inv -> {
            Ruta r = inv.getArgument(0);
            r.setUrlSeguimiento(urlEsperada);
            return r;
        });
        when(repoRutas.findById(rutaProgramada.getIdRuta())).thenReturn(Optional.of(rutaProgramada));
        when(repoItemEntrega.findAllById(any())).thenReturn(List.of());

        rutaService.iniciarRuta(idChofer);

        ArgumentCaptor<Ruta> rutaCaptor = ArgumentCaptor.forClass(Ruta.class);
        verify(repoRutas, atLeast(2)).save(rutaCaptor.capture());

        List<Ruta> guardadas = rutaCaptor.getAllValues();
        Ruta guardadaFinal = guardadas.get(guardadas.size() - 1);

        assertEquals(EstadoRuta.EN_CURSO, guardadaFinal.getEstado());
        assertEquals(urlEsperada, guardadaFinal.getUrlSeguimiento(),
                "La URL de seguimiento debe conservarse intacta tras iniciar la ruta");
    }

    // --- terminar ruta ---

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
        itemEnRuta.setEstado(EstadoEntrega.EN_CAMINO);

        Parada parada = new Parada();
        parada.getItems().add(itemEnRuta);
        rutaEnCurso.getParadas().add(parada);

        ItemEntrega itemActualizado = new ItemEntrega(idDonacionEntregada, 5, UnidadDeMedida.UNIDADES, null);
        itemActualizado.setEstado(EstadoEntrega.ENTREGADA);

        when(repoChoferes.findById(idChofer)).thenReturn(Optional.of(chofer));
        when(repoRutas.findFirstByCamionAsignadoChoferIdChoferAndEstado(chofer.getIdChofer(), EstadoRuta.EN_CURSO)).thenReturn(Optional.of(rutaEnCurso));
        when(repoRutas.findById(rutaEnCurso.getIdRuta())).thenReturn(Optional.of(rutaEnCurso));
        when(repoItemEntrega.findAllById(any())).thenReturn(List.of(itemActualizado));
        when(repoItemEntrega.existsById(idDonacionEntregada)).thenReturn(true);

        rutaService.terminarRuta(idChofer);

        verify(gestorPublicacionEventos, never()).publicarReingresoDeposito(any());
        verify(repoItemEntrega).deleteById(idDonacionEntregada);
    }

    @Test
    @DisplayName("terminarRuta debe persistir instancias actualizadas de Chofer y Camion desde repositorios")
    void persistirInstanciasActualizadasDeChoferYCamion() {
        UUID idChofer = UUID.randomUUID();
        String patente = "AAA111";

        Chofer choferEmbebido = new Chofer(idChofer, "Nombre Viejo", false);
        choferEmbebido.setVersion(1L);

        Camion camionEmbebido = new Camion("AAA111", 10.0, 3.0, 1000.0, false);
        camionEmbebido.setChofer(choferEmbebido);
        camionEmbebido.setVersion(1L);

        Ruta rutaEnCurso = new Ruta(camionEmbebido);
        rutaEnCurso.setEstado(EstadoRuta.EN_CURSO);

        Chofer choferFresco = new Chofer(idChofer, "Nombre Actualizado", false);
        choferFresco.setVersion(2L);

        Camion camionFresco = new Camion("AAA111", 10.0, 3.0, 1000.0, false);
        camionFresco.setChofer(choferFresco);
        camionFresco.setVersion(2L);

        when(repoChoferes.findById(idChofer)).thenReturn(Optional.of(choferFresco));
        when(repoRutas.findFirstByCamionAsignadoChoferIdChoferAndEstado(choferFresco.getIdChofer(), EstadoRuta.EN_CURSO)).thenReturn(Optional.of(rutaEnCurso));
        when(repoRutas.findById(rutaEnCurso.getIdRuta())).thenReturn(Optional.of(rutaEnCurso));
        when(repoCamiones.findById(patente)).thenReturn(Optional.of(camionFresco));

        rutaService.terminarRuta(idChofer);

        verify(repoChoferes).save(choferFresco);
        verify(repoChoferes, never()).save(choferEmbebido);
        assertTrue(choferFresco.isDisponible(), "El chofer actualizado debe quedar marcado como disponible");

        verify(repoCamiones).save(camionFresco);
        verify(repoCamiones, never()).save(camionEmbebido);
        assertTrue(camionFresco.getDisponible(), "El camión actualizado debe quedar marcado como disponible");
        assertNull(camionFresco.getChofer(), "El camión actualizado debe quedar desvinculado de su chofer");
    }

    @Test
    @DisplayName("terminarRuta no debe lanzar NullPointerException si la ruta no tiene camión asignado")
    void noLanzaNpeSiRutaNoTieneCamion() {
        UUID idChofer = UUID.randomUUID();
        Chofer choferFresco = new Chofer(idChofer, "Chofer", false);

        Ruta rutaEnCurso = new Ruta();
        rutaEnCurso.setIdRuta(UUID.randomUUID());
        rutaEnCurso.setParadas(new ArrayList<>());
        rutaEnCurso.setCamionAsignado(null);
        rutaEnCurso.setEstado(EstadoRuta.EN_CURSO);

        when(repoChoferes.findById(idChofer)).thenReturn(Optional.of(choferFresco));
        when(repoRutas.findFirstByCamionAsignadoChoferIdChoferAndEstado(choferFresco.getIdChofer(), EstadoRuta.EN_CURSO)).thenReturn(Optional.of(rutaEnCurso));
        when(repoRutas.findById(rutaEnCurso.getIdRuta())).thenReturn(Optional.of(rutaEnCurso));

        assertDoesNotThrow(() -> rutaService.terminarRuta(idChofer));
        verify(repoChoferes).save(choferFresco);
    }

    // --- paginación ---

    @Test
    @DisplayName("Se pasa el Pageable al repositorio y se devuelve la página")
    void sePaginaEnElRepositorio() {
        Pageable pageable = PageRequest.of(0, 2);
        Ruta ruta = new Ruta(new Camion("BBB222", 10.0, 3.0, 1000.0, true));
        ruta.setIdRuta(UUID.randomUUID());
        ruta.setEstado(EstadoRuta.PROGRAMADA);
        when(repoRutas.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(ruta), pageable, 1));

        Page<RutaDTO> resultado = rutaService.findAll(pageable);

        verify(repoRutas).findAll(pageable);
        assertThat(resultado.getContent()).extracting(RutaDTO::getIdRuta)
                .containsExactly(ruta.getIdRuta());
    }

    @Test
    @DisplayName("findByChofer devuelve el historial paginado del chofer")
    void historialPaginadoPorChofer() {
        UUID idChofer = UUID.randomUUID();
        Chofer chofer = new Chofer(idChofer, "Chofer Historial", true);
        Pageable pageable = PageRequest.of(0, 8);
        Ruta ruta = new Ruta(new Camion("HIS123", 10.0, 3.0, 1000.0, true));
        ruta.setIdRuta(UUID.randomUUID());
        ruta.setEstado(EstadoRuta.FINALIZADA);

        when(repoChoferes.findById(idChofer)).thenReturn(Optional.of(chofer));
        when(repoRutas.findByCamionAsignadoChoferIdChofer(idChofer, pageable))
                .thenReturn(new PageImpl<>(List.of(ruta), pageable, 1));

        Page<RutaDTO> resultado = rutaService.findByChofer(idChofer, pageable);

        verify(repoRutas).findByCamionAsignadoChoferIdChofer(idChofer, pageable);
        assertThat(resultado.getContent()).extracting(RutaDTO::getIdRuta)
                .containsExactly(ruta.getIdRuta());
        assertThat(resultado.getContent().get(0).getDeposito().getEtiqueta()).isEqualTo("Depósito");
        assertThat(resultado.getContent().get(0).getDeposito().getLatitud()).isEqualTo(-34.5986317);
    }

    @Test
    @DisplayName("findActiva prioriza la ruta en curso sobre la programada")
    void rutaActivaPriorizaEnCurso() {
        UUID idChofer = UUID.randomUUID();
        Chofer chofer = new Chofer(idChofer, "Chofer Activo", true);
        Ruta enCurso = new Ruta(new Camion("CUR123", 10.0, 3.0, 1000.0, true));
        enCurso.setIdRuta(UUID.randomUUID());
        enCurso.setEstado(EstadoRuta.EN_CURSO);

        when(repoChoferes.findById(idChofer)).thenReturn(Optional.of(chofer));
        when(repoRutas.findFirstByCamionAsignadoChoferIdChoferAndEstado(idChofer, EstadoRuta.EN_CURSO))
                .thenReturn(Optional.of(enCurso));

        Optional<RutaDTO> activa = rutaService.findActiva(idChofer);

        assertThat(activa).isPresent();
        assertThat(activa.get().getIdRuta()).isEqualTo(enCurso.getIdRuta());
    }
}
