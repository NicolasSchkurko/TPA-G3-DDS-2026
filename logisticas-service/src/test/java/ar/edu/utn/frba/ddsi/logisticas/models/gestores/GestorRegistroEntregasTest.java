package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienesDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.DireccionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Ciudad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Pais;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Provincia;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCiudades;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioDirecciones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEntidades;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioPaises;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioProvincias;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GestorRegistroEntregas")
class GestorRegistroEntregasTest {

    private static final UUID ID_DONACION =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_ENTIDAD =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private RepositorioEntidades repoEntidades;
    @Mock private RepositorioDirecciones repoDirecciones;
    @Mock private RepositorioCiudades repoCiudades;
    @Mock private RepositorioProvincias repoProvincias;
    @Mock private RepositorioPaises repoPaises;

    private GestorRegistroEntregas gestor;

    @BeforeEach
    void setUp() {
        GestorCatalogoUbicaciones catalogo = new GestorCatalogoUbicaciones(
                repoEntidades, repoDirecciones, repoCiudades, repoProvincias, repoPaises);
        gestor = new GestorRegistroEntregas(repoItemEntrega, catalogo);

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
    @DisplayName("Una petición nueva registra el ítem")
    void registraCuandoNoExiste() {
        gestor.registrar(peticionConUnBien());

        verify(repoItemEntrega, times(1)).save(any(ItemEntrega.class));
    }

    @Test
    @DisplayName("El mensaje repetido NO vuelve a guardar el ítem")
    void noVuelveAGuardarSiYaExiste() {
        when(repoItemEntrega.existsById(ID_DONACION)).thenReturn(true);

        gestor.registrar(peticionConUnBien());

        verify(repoItemEntrega, never()).save(any(ItemEntrega.class));
    }

    @Test
    @DisplayName("Una donación con tres bienes resuelve el catálogo una vez, no tres")
    void resuelveElCatalogoUnaVezPorMensaje() {
        EntregaDTO dto = peticion(
                Arrays.asList(bien(1, "KILOGRAMOS"), bien(2, "LITROS"), bien(3, "UNIDADES")),
                Arrays.asList(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        UUID.fromString("44444444-4444-4444-4444-444444444444"),
                        UUID.fromString("55555555-5555-5555-5555-555555555555")));

        gestor.registrar(dto);

        verify(repoItemEntrega, times(3)).save(any(ItemEntrega.class));
        verify(repoDirecciones, times(1)).save(any());
        verify(repoCiudades, times(1)).save(any());
        verify(repoProvincias, times(1)).save(any());
        verify(repoPaises, times(1)).save(any());
        verify(repoEntidades, times(1)).save(any());
    }

    @Test
    @DisplayName("Una donación con un bien repetido y otro nuevo registra solo el nuevo")
    void registraSoloLosBienesNuevos() {
        UUID yaRegistrado = UUID.fromString("66666666-6666-6666-6666-666666666666");
        UUID nuevo = UUID.fromString("77777777-7777-7777-7777-777777777777");

        EntregaDTO dto = peticion(
                Arrays.asList(bien(1, "KILOGRAMOS"), bien(2, "LITROS")),
                Arrays.asList(yaRegistrado, nuevo));

        when(repoItemEntrega.existsById(yaRegistrado)).thenReturn(true);
        when(repoItemEntrega.existsById(nuevo)).thenReturn(false);

        gestor.registrar(dto);

        ArgumentCaptor<ItemEntrega> captor = ArgumentCaptor.forClass(ItemEntrega.class);
        verify(repoItemEntrega, times(1)).save(captor.capture());

        assertThat(captor.getValue().getIdDonacion()).isEqualTo(nuevo);
    }

    @Test
    @DisplayName("Todos los ítems se guardan en PENDIENTE")
    void losItemsNuevosNacenEnPendiente() {
        gestor.registrar(peticionConUnBien());

        ArgumentCaptor<ItemEntrega> captor = ArgumentCaptor.forClass(ItemEntrega.class);
        verify(repoItemEntrega).save(captor.capture());

        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoEntrega.PENDIENTE);
        assertThat(captor.getValue().getIdDonacion()).isEqualTo(ID_DONACION);
    }

    @Test
    @DisplayName("Una petición sin entidad beneficiaria se rechaza antes de escribir")
    void rechazaPeticionIncompleta() {
        EntregaDTO dto = peticionConUnBien();
        dto.setEntidadBeneficiaria(null);

        assertThatThrownBy(() -> gestor.registrar(dto))
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

        assertThatThrownBy(() -> gestor.registrar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no coincide");

        verify(repoItemEntrega, never()).save(any());
    }
}
