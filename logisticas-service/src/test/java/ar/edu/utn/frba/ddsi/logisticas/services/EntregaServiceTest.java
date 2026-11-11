package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.ActualizacionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.DireccionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Ciudad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Provincia;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCicloEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRegistroEntregas;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EntregaService")
class EntregaServiceTest {

    private static final UUID ID_DONACION =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private GestorRegistroEntregas gestorRegistroEntregas;
    @Mock private GestorCicloEntrega gestorCicloEntrega;

    @InjectMocks private EntregaService servicio;

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    @DisplayName("actualizarEstado")
    class ActualizarEstado {

        @Mock private RepositorioItemEntrega repoItemEntrega;
        @Mock private GestorRegistroEntregas gestorRegistroEntregas;
        @Mock private GestorCicloEntrega gestorCicloEntrega;

        @InjectMocks private EntregaService servicio;

        @Test
        @DisplayName("ENTREGADA delega en el gestor de ciclo")
        void entregadaDelegaEnElGestor() {
            ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();
            dto.setEstado("ENTREGADA");
            dto.setFotoUrl("http://foto.jpg");

            servicio.actualizarEstado(ID_DONACION, dto);

            verify(gestorCicloEntrega).confirmar(ID_DONACION, "http://foto.jpg");
        }

        @Test
        @DisplayName("NO_RECIBIDA delega en el gestor de ciclo")
        void noRecibidaDelegaEnElGestor() {
            ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();
            dto.setEstado("NO_RECIBIDA");
            dto.setJustificacion("faltante");

            servicio.actualizarEstado(ID_DONACION, dto);

            verify(gestorCicloEntrega).marcarFallida(ID_DONACION, "faltante");
        }

        @Test
        @DisplayName("PENDIENTE delega en el gestor de ciclo")
        void pendienteDelegaEnElGestor() {
            ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();
            dto.setEstado("PENDIENTE");

            servicio.actualizarEstado(ID_DONACION, dto);

            verify(gestorCicloEntrega).reingresar(ID_DONACION);
        }

        @Test
        @DisplayName("Un estado desconocido se rechaza antes de tocar el gestor")
        void estadoDesconocidoNoTocaElGestor() {
            ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();
            dto.setEstado("PERDIDA");

            assertThatThrownBy(() -> servicio.actualizarEstado(ID_DONACION, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Estado no válido");

            verify(gestorCicloEntrega, never()).confirmar(any(), any());
            verify(gestorCicloEntrega, never()).marcarFallida(any(), any());
            verify(gestorCicloEntrega, never()).reingresar(any());
        }

        @Test
        @DisplayName("Un estado nulo se rechaza")
        void estadoNuloSeRechaza() {
            ActualizacionEntregaDTO dto = new ActualizacionEntregaDTO();

            assertThrows(IllegalArgumentException.class,
                    () -> servicio.actualizarEstado(ID_DONACION, dto));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    @DisplayName("findAll y no-recibidas con datos parciales")
    class FindAllDatosParciales {

        @Mock private RepositorioItemEntrega repoItemEntrega;
        @Mock private GestorRegistroEntregas gestorRegistroEntregas;
        @Mock private GestorCicloEntrega gestorCicloEntrega;

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
        @DisplayName("findAll no revienta si el ítem tiene la lista de eventos en null")
        void findAllToleraEventosNull() {
            ItemEntrega sinEventos = new ItemEntrega(UUID.randomUUID(), 1, UnidadDeMedida.UNIDADES, null);
            sinEventos.setEventos(null);
            when(repoItemEntrega.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sinEventos)));

            Page<BienDTO> resultado = servicio.findAll(PageRequest.of(0, 10));

            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).getEventos()).isEmpty();
        }

        @Test
        @DisplayName("obtenerEntregasNoRecibidas también tolera ítems incompletos")
        void noRecibidasToleraDatosParciales() {
            ItemEntrega incompleto = new ItemEntrega(UUID.randomUUID(), 1, null, null);
            incompleto.setEstado(EstadoEntrega.NO_RECIBIDA);
            when(repoItemEntrega.findByEstado(EstadoEntrega.NO_RECIBIDA))
                    .thenReturn(List.of(incompleto));

            var resultado = servicio.obtenerEntregasNoRecibidas();

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

        @Mock private RepositorioItemEntrega repoItemEntrega;
        @Mock private GestorRegistroEntregas gestorRegistroEntregas;
        @Mock private GestorCicloEntrega gestorCicloEntrega;

        @InjectMocks private EntregaService servicio;

        private static ItemEntrega itemConCiclo() {
            Direccion direccion = new Direccion("Av. Corrientes", null, 100, 1, "A",
                    "CABA", "Buenos Aires", "Argentina");
            Entidad entidad = new Entidad(
                    UUID.fromString("22222222-2222-2222-2222-222222222222"), direccion);

            ItemEntrega item = new ItemEntrega(ID_DONACION, 3, UnidadDeMedida.KILOGRAMOS, entidad);
            item.setEstado(EstadoEntrega.ENTREGADA);
            item.setFechaCambioEstado(java.time.LocalDateTime.now());
            item.getEventos().add(new ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica(
                    "ENTREGA_CONFIRMADA", ID_DONACION.toString(), java.time.LocalDateTime.now(), null));
            return item;
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
        @DisplayName("Un id inexistente tira RecursoNoEncontrado para que el advice devuelva 404")
        void unIdInexistenteSigueTirando() {
            when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.findById(ID_DONACION))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Entrega no encontrada");
        }
    }

    @Test
    @DisplayName("procesarPeticion delega el registro en el gestor")
    void procesarPeticionDelegaEnElGestor() {
        EntregaDTO dto = new EntregaDTO(List.of(ID_DONACION),
                List.of(new BienDTO(1, "UNIDADES", null, null, null, null, null)),
                new DireccionDTO(UUID.randomUUID(), "Calle", null, 100, null, null,
                        "CABA", "CABA", "Argentina"));

        servicio.procesarPeticion(dto);

        verify(gestorRegistroEntregas).registrar(dto);
    }

    @Test
    @DisplayName("delete responde 404 si el ítem no existe")
    void deleteSinItemTiraNoEncontrado() {
        when(repoItemEntrega.existsById(ID_DONACION)).thenReturn(false);

        assertThatThrownBy(() -> servicio.delete(ID_DONACION))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
