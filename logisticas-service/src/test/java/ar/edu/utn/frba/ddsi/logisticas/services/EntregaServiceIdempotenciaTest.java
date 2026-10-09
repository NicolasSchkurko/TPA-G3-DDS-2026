package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienesDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.DireccionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorPublicacionEventos;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCiudades;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioDirecciones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEntidades;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioPaises;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioProvincias;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioUnidadesDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lo que hace falta para que <b>N instancias de logistica puedan compartir la cola y la base</b>.
 *
 * <p>Cada test cubre una condicion de las tres que hacen posible eso. La cuarta -la carrera de
 * dos inserciones simultaneas sobre el mismo {@code idDonacion}- la arbitra la clave primaria y
 * el listener la trata como resultado benigno; cubrirla con un test exigiria una base real
 * concurrent, y este modulo no tiene una embebida disponible en el build offline.
 *
 * <p>Los repositorios van mockeados a proposito: lo que se prueba es la <i>decision</i> del
 * servicio -que escribe y que no escribe cuando el mensaje se repite-, que es exactamente lo
 * que se rompio antes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EntregaService tiene que ser idempotente para que N instancias puedan compartir la base")
class EntregaServiceIdempotenciaTest {

    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private RepositorioRutas repoRutas;
    @Mock private RepositorioEntidades repoEntidades;
    @Mock private RepositorioDirecciones repoDirecciones;
    @Mock private RepositorioCiudades repoCiudades;
    @Mock private RepositorioProvincias repoProvincias;
    @Mock private RepositorioPaises repoPaises;
    @Mock private RepositorioUnidadesDeMedida repoUnidades;
    @Mock private GestorPublicacionEventos gestorPublicacionEventos;

    /**
     * El servicio abre sus propias transacciones con REQUIRES_NEW. El mock alcanza para que
     * TransactionTemplate tenga un gestor y ejecute el bloque; la persistencia real ya esta
     * mockeada en los repos.
     */
    @Mock private PlatformTransactionManager gestorTransacciones;

    @InjectMocks private EntregaService servicio;

    private static final UUID ID_DONACION =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_ENTIDAD =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void nadaRegistradoTodavia() {
        when(repoItemEntrega.existsById(any())).thenReturn(false);

        // El servicio abre sus propias transacciones con REQUIRES_NEW para aislar el
        // catalogo de los items. TransactionTemplate necesita un gestor y un
        // TransactionStatus para correr el bloque; la persistencia real ya esta
        // mockeada en los repositorios.
        when(gestorTransacciones.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
    }

    /** El DTO no tiene constructor vacio: se arma con el de 7 parametros. */
    private static BienDTO bien(Integer cantidad, String unidad) {
        return new BienDTO(cantidad, unidad, null, null, null, null, null);
    }

    /** Arma la peticion con la misma forma que usa el productor de donaciones-service. */
    private static EntregaDTO peticion(List<BienDTO> bienes, List<UUID> idsDonaciones) {
        DireccionDTO direccion = new DireccionDTO(ID_ENTIDAD, "Av. Corrientes", "100", 100, 1,
                "A", "CABA", "Buenos Aires", "Argentina");

        EntregaDTO dto = new EntregaDTO(List.of(UUID.randomUUID()),
                List.of(new BienDTO(1, "KILOGRAMOS", null, null, null, null, null)), direccion);
        return dto;
    }

    private static EntregaDTO peticionConUnBien() {
        return peticion(List.of(bien(3, "KILOGRAMOS")), List.of(ID_DONACION));
    }

    @Test
    @DisplayName("Una peticion nueva registra el item")
    void registraCuandoNoExiste() {
        servicio.procesarPeticion(peticionConUnBien());

        verify(repoItemEntrega, times(1)).save(any(ItemEntrega.class));
    }

    @Test
    @DisplayName("El mensaje repetido NO vuelve a guardar el item: esa era la perdida de datos")
    void noVuelveAGuardarSiYaExiste() {
        // Esto es lo que pasaba antes: el idDonacion es clave natural sin @GeneratedValue, asi
        // que save() iba por merge() y hacia un UPDATE que reescribia el estado con lo del
        // constructor, que es PENDIENTE. Una entrega confirmada volvia a PENDIENTE, sin error.
        when(repoItemEntrega.existsById(ID_DONACION)).thenReturn(true);

        servicio.procesarPeticion(peticionConUnBien());

        verify(repoItemEntrega, never()).save(any(ItemEntrega.class));
        verify(repoItemEntrega, never()).saveAndFlush(any(ItemEntrega.class));
    }

    @Test
    @DisplayName("Un mensaje repetido no toca el estado de un item ya entregada")
    void noTocaElEstadoDeUnItemYaEntregado() {
        ItemEntrega entregado = new ItemEntrega();
        entregado.setIdDonacion(ID_DONACION);
        entregado.setEstado(EstadoEntrega.ENTREGADA);

        when(repoItemEntrega.existsById(ID_DONACION)).thenReturn(true);

        servicio.procesarPeticion(peticionConUnBien());

        // No se guarda ninguna entidad, asi que el estado de la base sigue como estaba. Si se
        // hubiera reescrito, el item en memoria seria el del constructor, en PENDIENTE.
        assertThat(entregado.getEstado()).isEqualTo(EstadoEntrega.ENTREGADA);
        verify(repoItemEntrega, never()).save(any());
        verify(repoItemEntrega, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Una donacion con tres bienes resuelve el catalogo una vez, no tres")
    void resuelveElCatalogoUnaVezPorMensaje() {
        // Este es el punto 15 del backlog, y es la carrera que dos instancias se hacian: con el
        // catalogo dentro del for, dos donaciones para la misma entidad escriben las mismas
        // filas de Pais/Provincia/Ciudad/Direccion y la segunda pisa a la primera en silencio.
        EntregaDTO dto = peticion(
                Arrays.asList(bien(1, "KILOGRAMOS"), bien(2, "LITROS"), bien(3, "UNIDADES")),
                Arrays.asList(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        UUID.fromString("44444444-4444-4444-4444-444444444444"),
                        UUID.fromString("55555555-5555-5555-5555-555555555555")));

        servicio.procesarPeticion(dto);

        // Tres items, un solo catalogo.
        verify(repoItemEntrega, times(3)).save(any(ItemEntrega.class));
        verify(repoDirecciones, times(1)).save(any());
        verify(repoCiudades, times(1)).save(any());
        verify(repoProvincias, times(1)).save(any());
        verify(repoPaises, times(1)).save(any());
        // La entidad se guarda con saveAndFlush a proposito: el flush forzado hace visible
        // la violacion de clave primaria en el momento, que es lo que permite tratar la carrera
        // con otra instancia sin que se lleve por delante los items.
        verify(repoEntidades, times(1)).saveAndFlush(any());
    }

    @Test
    @DisplayName("Una donacion con un bien repetido y otro nuevo registra solo el nuevo")
    void registraSoloLosBienesNuevos() {
        // El caso que un `return` temprano rompia: si el primer bien ya estaba, con un `return`
        // se perdian los bienes siguientes que si eran nuevos. Tiene que ser un `continue`.
        UUID yaRegistrado = UUID.fromString("66666666-6666-6666-6666-666666666666");
        UUID nuevo = UUID.fromString("77777777-7777-7777-7777-777777777777");

        EntregaDTO dto = peticion(
                Arrays.asList(bien(1, "KILOGRAMOS"), bien(2, "LITROS")),
                Arrays.asList(yaRegistrado, nuevo));

        when(repoItemEntrega.existsById(yaRegistrado)).thenReturn(true);
        when(repoItemEntrega.existsById(nuevo)).thenReturn(false);

        servicio.procesarPeticion(dto);

        ArgumentCaptor<ItemEntrega> captor = ArgumentCaptor.forClass(ItemEntrega.class);
        verify(repoItemEntrega, times(1)).save(captor.capture());

        assertThat(captor.getValue().getIdDonacion()).isEqualTo(nuevo);
    }

    @Test
    @DisplayName("Todos los items se guardan en PENDIENTE: el registro nunca pisa un estado")
    void losItemsNuevosNacenEnPendiente() {
        servicio.procesarPeticion(peticionConUnBien());

        ArgumentCaptor<ItemEntrega> captor = ArgumentCaptor.forClass(ItemEntrega.class);
        verify(repoItemEntrega).save(captor.capture());

        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoEntrega.PENDIENTE);
        assertThat(captor.getValue().getIdDonacion()).isEqualTo(ID_DONACION);
    }

    @Test
    @DisplayName("Una peticion sin entidad beneficiaria se rechaza en vez de escribir a medias")
    void rechazaPeticionIncompleta() {
        EntregaDTO dto = peticionConUnBien();
        dto.setEntidadBeneficiaria(null);

        assertThatThrownBy(() -> servicio.procesarPeticion(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entidad beneficiaria");

        verify(repoItemEntrega, never()).save(any());
        verify(repoDirecciones, never()).save(any());
    }

    @Test
    @DisplayName("Si la cantidad de bienes no coincide con la de donaciones, se rechaza")
    void rechazaCantidadesDesalineadas() {
        EntregaDTO dto = peticionConUnBien();
        dto.setDonacionResumen(new BienesDTO(
                Arrays.asList(ID_DONACION, UUID.randomUUID()),
                List.of(bien(3, "KILOGRAMOS"))));

        assertThatThrownBy(() -> servicio.procesarPeticion(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no coincide");

        verify(repoItemEntrega, never()).save(any());
    }
}
