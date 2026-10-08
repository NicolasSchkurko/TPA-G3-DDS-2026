package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.controllers.EntregaController;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorPublicacionEventos;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCiudades;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioDirecciones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEntidades;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioPaises;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioProvincias;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioUnidadesDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.items.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.rutas.RepositorioRutas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Un endpoint que declara devolver un DTO no puede devolver la entidad de JPA.
 *
 * <p><b>Serializar la entidad cruda rompe de dos maneras a la vez.</b> Jackson sigue los
 * getters y entra en ciclo: {@code item.parada} → {@code parada.ruta} → {@code ruta.paradas} →
 * {@code parada.items} → {@code item.parada}, hasta reventar al construir el JSON. Y aunque no
 * hubiera ciclo, la respuesta exponía {@code id_parada} e {@code id_unidad_medida}: el esquema
 * interno, en un endpoint que el Swagger declara como {@code BienDTO}.
 *
 * <p><b>La inconsistencia lo delataba.</b> {@code GET /entregas} y
 * {@code GET /entregas/no-recibidas} sí pasaban por el mapper; solo {@code /entregas/{id} se
 * olvidaba. El test arma el ciclo completo —parada, ruta y sus paradas de vuelta al ítem— para
 * que una regresión a la entidad cruda reviente en vez de devolver algo que parece funcionar.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GET /entregas/{id} devuelve el DTO y no filtra el esquema interno")
class EntregaServiceFindByIdDevuelveDtoTest {

    private static final UUID ID_DONACION =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private RepositorioRutas repoRutas;
    @Mock private RepositorioEntidades repoEntidades;
    @Mock private RepositorioDirecciones repoDirecciones;
    @Mock private RepositorioCiudades repoCiudades;
    @Mock private RepositorioProvincias repoProvincias;
    @Mock private RepositorioPaises repoPaises;
    @Mock private RepositorioUnidadesDeMedida repoUnidades;
    @Mock private GestorPublicacionEventos gestorPublicacionEventos;
    @Mock private PlatformTransactionManager gestorTransacciones;

    @InjectMocks private EntregaService servicio;

    /**
     * Un ítem con el ciclo completo alrededor.
     *
     * <p>La parada apunta a una ruta que vuelve a contener esa misma parada. Es lo que hace
     * que serializar el ítem entre en recursion infinita: si el {@code @JsonIgnore} de la red
     * desapareciera, este objeto deja de poder serializarse.
     */
    private static ItemEntrega itemConCiclo() {
        Direccion direccion = new Direccion("Av. Corrientes", null, 100, 1, "A",
                "CABA", "Buenos Aires", "Argentina");
        Entidad entidad = new Entidad(
                UUID.fromString("22222222-2222-2222-2222-222222222222"), direccion);

        ItemEntrega item = new ItemEntrega(ID_DONACION, 3, UnidadDeMedida.KILOGRAMOS, entidad);
        item.setEstado(EstadoEntrega.ENTREGADA);
        item.setFechaCambioEstado(LocalDateTime.now());

        EventoLogistica evento = new EventoLogistica("ENTREGA_CONFIRMADA", ID_DONACION.toString(),
                LocalDateTime.now(), null);

        Ruta ruta = new Ruta(null);
        Parada parada = new Parada();
        parada.setRuta(ruta);
        parada.agregarItem(item);
        ruta.getParadas().add(parada);

        item.setParada(parada);
        item.getEventos().add(evento);
        return item;
    }

    /**
     * El mismo mapper que usa la app.
     *
     * <p>Un {@code ObjectMapper} pelado no serializa {@code LocalDateTime}: Spring Boot registra
     * el módulo de fechas por su cuenta, así que el test tiene que registrarlo también o
     * estaría midiendo una diferencia que en el servicio no existe.
     */
    private static ObjectMapper mapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Devuelve un BienDTO, no la entidad de JPA")
    void devuelveElDtoYNoLaEntidad() {
        when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.of(itemConCiclo()));

        Object resultado = servicio.findById(ID_DONACION);

        assertThat(resultado)
                .as("el endpoint declara BienDTO en el Swagger")
                .isInstanceOf(BienDTO.class);
    }

    @Test
    @DisplayName("El DTO trae los datos del item, incluido su estado")
    void elDtoTraeLosDatosDelItem() {
        when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.of(itemConCiclo()));

        Object resultado = servicio.findById(ID_DONACION);
        assertThat(resultado).isInstanceOf(BienDTO.class);
        BienDTO dto = (BienDTO) resultado;

        assertThat(dto.getCantidad()).isEqualTo(3);
        assertThat(dto.getUnidadDeMedida()).isEqualTo("Kilogramos");
        assertThat(dto.getEstado()).isEqualTo("ENTREGADA");
    }

    @Test
    @DisplayName("El JSON sale limpio: sin id_parada, sin id_unidad_medida, sin el ciclo")
    void elJsonNoFiltraElEsquemaInterno() throws Exception {
        when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.of(itemConCiclo()));

        ObjectMapper mapper = mapper();
        String json = mapper.writeValueAsString(servicio.findById(ID_DONACION));

        // Lo que la entidad cruda exponia y el DTO no debe.
        assertThat(json).doesNotContain("id_parada");
        assertThat(json).doesNotContain("id_unidad_medida");
        assertThat(json).doesNotContain("parada");
        // Y lo que sí tiene que estar, para que el endpoint sirva de algo.
        assertThat(json).contains("\"cantidad\":3");
        assertThat(json).contains("ENTREGADA");
    }

    @Test
    @DisplayName("Un id inexistente sigue tir IllegalArgumentException para el 404")
    void unIdInexistenteSigueTirando() {
        when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.findById(ID_DONACION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Entrega no encontrada");
    }

    @Test
    @DisplayName("El controller devuelve 200 con el DTO y 404 si no existe")
    void elControllerMapeaLosDosCasos() {
        when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.of(itemConCiclo()));
        EntregaController controller = new EntregaController(servicio);

        assertThat(controller.obtenerPorId(ID_DONACION).getStatusCode().value()).isEqualTo(200);
        assertThat(controller.obtenerPorId(ID_DONACION).getBody())
                .isInstanceOf(BienDTO.class);

        when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.empty());
        assertThat(controller.obtenerPorId(ID_DONACION).getStatusCode().value()).isEqualTo(404);
    }

    /** La red: aunque alguien devuelva la entidad cruda, no entra en ciclo. */
    @Test
    @DisplayName("La entidad cruda ya no se puede serializar en ciclo, por el @JsonIgnore")
    void laEntidadCrudaNoSeSerializaEnCiclo() throws Exception {
        // Sin esto, un endpoint que por error devuelva la entidad produce StackOverflowError:
        // el @JsonIgnore de ItemEntrega.parada corta la cadena.
        assertThat(mapper().writeValueAsString(itemConCiclo()))
                .contains("idDonacion");
    }
}