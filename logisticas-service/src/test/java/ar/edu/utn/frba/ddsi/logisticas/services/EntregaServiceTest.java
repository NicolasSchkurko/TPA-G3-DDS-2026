package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.controllers.EntregaController;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.ActualizacionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienesDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.DireccionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Ciudad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Pais;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Provincia;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorPublicacionEventos;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("EntregaService")
class EntregaServiceTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    @DisplayName("actualizarEstado")
    class ActualizarEstado {

        private static final UUID ID_DONACION =
                UUID.fromString("11111111-1111-1111-1111-111111111111");

        @Mock private RepositorioItemEntrega repoItemEntrega;
        @Mock private RepositorioRutas repoRutas;
        @Mock private GestorPublicacionEventos gestorPublicacionEventos;
        @Mock private PlatformTransactionManager gestorTransacciones;

        @InjectMocks private EntregaService entregaService;

        private ItemEntrega item;

        @BeforeEach
        void setUp() {
            item = new ItemEntrega(ID_DONACION, 1, UnidadDeMedida.UNIDADES, null);
            when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.of(item));
            when(repoRutas.findFirstByParadasItemsIdDonacion(ID_DONACION)).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("Reingreso a depósito administrativo falla si el ítem está en estado ENTREGADA")
        void reingresoFallaSiItemEntregado() {
            item.setEstado(EstadoEntrega.ENTREGADA);

            ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();
            dto.setEstado("PENDIENTE");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> entregaService.actualizarEstado(ID_DONACION, dto));

            assertTrue(ex.getMessage().contains("NO_RECIBIDA"));
            verify(gestorPublicacionEventos, never()).publicarReingresoDeposito(any());
        }

        @Test
        @DisplayName("Reingreso a depósito administrativo falla si el ítem está en estado EN_CAMINO")
        void reingresoFallaSiItemEnCamino() {
            item.setEstado(EstadoEntrega.EN_CAMINO);

            ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();
            dto.setEstado("PENDIENTE");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> entregaService.actualizarEstado(ID_DONACION, dto));

            assertTrue(ex.getMessage().contains("NO_RECIBIDA"));
            verify(gestorPublicacionEventos, never()).publicarReingresoDeposito(any());
        }

        @Test
        @DisplayName("Reingreso a depósito administrativo falla si el ítem ya está en estado PENDIENTE")
        void reingresoFallaSiItemYaPendiente() {
            item.setEstado(EstadoEntrega.PENDIENTE);

            ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();
            dto.setEstado("PENDIENTE");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> entregaService.actualizarEstado(ID_DONACION, dto));

            assertTrue(ex.getMessage().contains("NO_RECIBIDA"));
            verify(gestorPublicacionEventos, never()).publicarReingresoDeposito(any());
        }

        @Test
        @DisplayName("Reingreso a depósito administrativo es exitoso si el ítem está en estado NO_RECIBIDA")
        void reingresoExitosoSiItemNoRecibida() {
            item.setEstado(EstadoEntrega.NO_RECIBIDA);

            ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();
            dto.setEstado("PENDIENTE");

            when(gestorPublicacionEventos.publicarReingresoDeposito(item)).thenReturn(item);

            assertDoesNotThrow(() -> entregaService.actualizarEstado(ID_DONACION, dto));

            verify(gestorPublicacionEventos, times(1)).publicarReingresoDeposito(item);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    @DisplayName("findAll y no-recibidas con datos parciales")
    class FindAllDatosParciales {

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

        @Test
        @DisplayName("findAll no revienta si falta unidad, estado, entidad, dirección o ciudad")
        void findAllToleraDatosParciales() {
            ItemEntrega sinNada = new ItemEntrega(UUID.randomUUID(), 1, null, null);
            sinNada.setEstado(null);

            ItemEntrega sinDireccion = new ItemEntrega(UUID.randomUUID(), 2,
                    UnidadDeMedida.KILOGRAMOS, new Entidad(UUID.randomUUID(), null));

            Direccion direccionSinCiudad = new Direccion();
            direccionSinCiudad.setCalle1("Calle");
            ItemEntrega sinCiudad = new ItemEntrega(UUID.randomUUID(), 3,
                    UnidadDeMedida.LITROS, new Entidad(UUID.randomUUID(), direccionSinCiudad));

            Direccion soloCiudad = new Direccion();
            soloCiudad.setCiudad(new Ciudad("CABA", null));
            ItemEntrega sinProvincia = new ItemEntrega(UUID.randomUUID(), 4,
                    UnidadDeMedida.UNIDADES, new Entidad(UUID.randomUUID(), soloCiudad));

            Provincia provinciaSinPais = new Provincia();
            provinciaSinPais.setNombre("Buenos Aires");
            Direccion ciudadSinPais = new Direccion();
            ciudadSinPais.setCiudad(new Ciudad("CABA", provinciaSinPais));
            ItemEntrega sinPais = new ItemEntrega(UUID.randomUUID(), 5,
                    UnidadDeMedida.UNIDADES, new Entidad(UUID.randomUUID(), ciudadSinPais));

            ItemEntrega completo = new ItemEntrega(UUID.randomUUID(), 6, UnidadDeMedida.UNIDADES,
                    new Entidad(UUID.randomUUID(), new Direccion("Av. Corrientes", null, 100, 1, "A",
                            "CABA", "Buenos Aires", "Argentina")));

            Pageable pageable = PageRequest.of(1, 6);
            List<ItemEntrega> items =
                    List.of(sinNada, sinDireccion, sinCiudad, sinProvincia, sinPais, completo);
            when(repoItemEntrega.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(items, pageable, 13));

            Page<BienDTO> resultado = servicio.findAll(pageable);

            verify(repoItemEntrega).findAll(pageable);
            assertThat(resultado.getTotalElements()).isEqualTo(13);
            assertThat(resultado.getContent()).hasSize(6);

            List<BienDTO> bienes = resultado.getContent();
            assertThat(bienes).extracting(BienDTO::getIdDonacion)
                    .containsExactly(sinNada.getIdDonacion(), sinDireccion.getIdDonacion(),
                            sinCiudad.getIdDonacion(), sinProvincia.getIdDonacion(),
                            sinPais.getIdDonacion(), completo.getIdDonacion());
            assertThat(bienes.get(0).getUnidadDeMedida()).isNull();
            assertThat(bienes.get(0).getEstado()).isNull();
            assertThat(bienes.get(0).getEntidadDestino()).isNull();
            assertThat(bienes.get(1).getEntidadDestino()).isNull();
            assertThat(bienes.get(2).getEntidadDestino().getCiudad()).isNull();
            assertThat(bienes.get(2).getEntidadDestino().getProvincia()).isNull();
            assertThat(bienes.get(2).getEntidadDestino().getPais()).isNull();
            assertThat(bienes.get(3).getEntidadDestino().getCiudad()).isEqualTo("CABA");
            assertThat(bienes.get(3).getEntidadDestino().getProvincia()).isNull();
            assertThat(bienes.get(3).getEntidadDestino().getPais()).isNull();
            assertThat(bienes.get(4).getEntidadDestino().getCiudad()).isEqualTo("CABA");
            assertThat(bienes.get(4).getEntidadDestino().getProvincia()).isEqualTo("Buenos Aires");
            assertThat(bienes.get(4).getEntidadDestino().getPais()).isNull();
            assertThat(bienes.get(5).getEntidadDestino().getCiudad()).isEqualTo("CABA");
            assertThat(bienes.get(5).getEntidadDestino().getProvincia()).isEqualTo("Buenos Aires");
            assertThat(bienes.get(5).getEntidadDestino().getPais()).isEqualTo("Argentina");
        }

        @Test
        @DisplayName("obtenerEntregasNoRecibidas también tolera ítems incompletos")
        void noRecibidasToleraDatosParciales() {
            ItemEntrega incompleto = new ItemEntrega(UUID.randomUUID(), 1, null, null);
            incompleto.setEstado(EstadoEntrega.NO_RECIBIDA);
            when(repoItemEntrega.findByEstado(EstadoEntrega.NO_RECIBIDA))
                    .thenReturn(List.of(incompleto));

            BienesDTO resultado = servicio.obtenerEntregasNoRecibidas();

            assertThat(resultado.getBienes()).hasSize(1);
            assertThat(resultado.getBienes().get(0).getUnidadDeMedida()).isNull();
            assertThat(resultado.getBienes().get(0).getEntidadDestino()).isNull();
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    @DisplayName("findById devuelve DTO")
    class FindByIdDevuelveDto {

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
            parada.agregarItem(item);
            ruta.getParadas().add(parada);
            item.getEventos().add(evento);
            return item;
        }

        private static ObjectMapper mapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }

        @Test
        @DisplayName("Devuelve un BienDTO, no la entidad de JPA")
        void devuelveElDtoYNoLaEntidad() {
            when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.of(itemConCiclo()));

            Object resultado = servicio.findById(ID_DONACION);

            assertThat(resultado).isInstanceOf(BienDTO.class);
        }

        @Test
        @DisplayName("El DTO trae los datos del item, incluido su estado")
        void elDtoTraeLosDatosDelItem() {
            when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.of(itemConCiclo()));

            BienDTO dto = (BienDTO) servicio.findById(ID_DONACION);

            assertThat(dto.getCantidad()).isEqualTo(3);
            assertThat(dto.getUnidadDeMedida()).isEqualTo("Kilogramos");
            assertThat(dto.getEstado()).isEqualTo("ENTREGADA");
        }

        @Test
        @DisplayName("El JSON sale limpio: sin id_parada, sin id_unidad_medida, sin el ciclo")
        void elJsonNoFiltraElEsquemaInterno() throws Exception {
            when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.of(itemConCiclo()));

            String json = mapper().writeValueAsString(servicio.findById(ID_DONACION));

            assertThat(json).doesNotContain("id_parada");
            assertThat(json).doesNotContain("id_unidad_medida");
            assertThat(json).doesNotContain("parada");
            assertThat(json).contains("\"cantidad\":3");
            assertThat(json).contains("ENTREGADA");
        }

        @Test
        @DisplayName("Un id inexistente tira RecursoNoEncontrado para que el advice devuelva 404")
        void unIdInexistenteSigueTirando() {
            when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.findById(ID_DONACION))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Entrega no encontrada");
        }

        @Test
        @DisplayName("El controller devuelve 200 con el DTO; si no existe, propaga para el advice")
        void elControllerMapeaLosDosCasos() {
            when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.of(itemConCiclo()));
            EntregaController controller = new EntregaController(servicio);

            assertThat(controller.obtenerPorId(ID_DONACION).getStatusCode().value()).isEqualTo(200);
            assertThat(controller.obtenerPorId(ID_DONACION).getBody()).isInstanceOf(BienDTO.class);

            when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> controller.obtenerPorId(ID_DONACION))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("La entidad cruda ya no se puede serializar en ciclo, por el @JsonIgnore")
        void laEntidadCrudaNoSeSerializaEnCiclo() throws Exception {
            assertThat(mapper().writeValueAsString(itemConCiclo())).contains("idDonacion");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    @DisplayName("idempotencia")
    class Idempotencia {

        private static final UUID ID_DONACION =
                UUID.fromString("11111111-1111-1111-1111-111111111111");
        private static final UUID ID_ENTIDAD =
                UUID.fromString("22222222-2222-2222-2222-222222222222");

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

        @BeforeEach
        void nadaRegistradoTodavia() {
            when(repoItemEntrega.existsById(any())).thenReturn(false);

            Pais pais = new Pais();
            pais.setIdPais("pais-1");
            pais.setNombre("Argentina");

            Provincia provincia = new Provincia();
            provincia.setIdProvincia("provincia-1");
            provincia.setNombre("Buenos Aires");
            provincia.setPais(pais);

            Ciudad ciudad = new Ciudad("CABA", provincia);
            ciudad.setIdCiudad("ciudad-1");

            Direccion direccion = new Direccion();
            direccion.setIdDireccion(UUID.randomUUID());
            direccion.setCiudad(ciudad);

            Entidad entidad = new Entidad(ID_ENTIDAD, direccion);

            when(repoPaises.save(any(Pais.class))).thenReturn(pais);
            when(repoProvincias.save(any(Provincia.class))).thenReturn(provincia);
            when(repoCiudades.save(any(Ciudad.class))).thenReturn(ciudad);
            when(repoDirecciones.save(any(Direccion.class))).thenReturn(direccion);
            when(repoEntidades.save(any(Entidad.class))).thenReturn(entidad);

            when(gestorTransacciones.getTransaction(any(TransactionDefinition.class)))
                    .thenReturn(new SimpleTransactionStatus());
        }

        private static BienDTO bien(Integer cantidad, String unidad) {
            return new BienDTO(cantidad, unidad, null, null, null, null, null);
        }

        private static EntregaDTO peticion(List<BienDTO> bienes, List<UUID> idsDonaciones) {
            DireccionDTO direccion = new DireccionDTO(ID_ENTIDAD, "Av. Corrientes", "100", 100, 1,
                    "A", "CABA", "Buenos Aires", "Argentina");
            return new EntregaDTO(idsDonaciones, bienes, direccion);
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
        @DisplayName("El mensaje repetido NO vuelve a guardar el item")
        void noVuelveAGuardarSiYaExiste() {
            when(repoItemEntrega.existsById(ID_DONACION)).thenReturn(true);

            servicio.procesarPeticion(peticionConUnBien());

            verify(repoItemEntrega, never()).save(any(ItemEntrega.class));
        }

        @Test
        @DisplayName("Un mensaje repetido no toca el estado de un item ya entregada")
        void noTocaElEstadoDeUnItemYaEntregado() {
            ItemEntrega entregado = new ItemEntrega();
            entregado.setIdDonacion(ID_DONACION);
            entregado.setEstado(EstadoEntrega.ENTREGADA);

            when(repoItemEntrega.existsById(ID_DONACION)).thenReturn(true);

            servicio.procesarPeticion(peticionConUnBien());

            assertThat(entregado.getEstado()).isEqualTo(EstadoEntrega.ENTREGADA);
            verify(repoItemEntrega, never()).save(any());
        }

        @Test
        @DisplayName("Una donacion con tres bienes resuelve el catalogo una vez, no tres")
        void resuelveElCatalogoUnaVezPorMensaje() {
            EntregaDTO dto = peticion(
                    Arrays.asList(bien(1, "KILOGRAMOS"), bien(2, "LITROS"), bien(3, "UNIDADES")),
                    Arrays.asList(
                            UUID.fromString("33333333-3333-3333-3333-333333333333"),
                            UUID.fromString("44444444-4444-4444-4444-444444444444"),
                            UUID.fromString("55555555-5555-5555-5555-555555555555")));

            servicio.procesarPeticion(dto);

            verify(repoItemEntrega, times(3)).save(any(ItemEntrega.class));
            verify(repoDirecciones, times(1)).save(any());
            verify(repoCiudades, times(1)).save(any());
            verify(repoProvincias, times(1)).save(any());
            verify(repoPaises, times(1)).save(any());
            verify(repoEntidades, times(1)).save(any());
        }

        @Test
        @DisplayName("Una donacion con un bien repetido y otro nuevo registra solo el nuevo")
        void registraSoloLosBienesNuevos() {
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
}
