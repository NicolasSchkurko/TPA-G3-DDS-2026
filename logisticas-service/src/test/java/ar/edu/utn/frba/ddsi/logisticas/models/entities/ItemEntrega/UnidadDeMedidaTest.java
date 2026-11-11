package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.DireccionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Ciudad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Pais;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Provincia;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCatalogoUbicaciones;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRegistroEntregas;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.annotation.Version;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UnidadDeMedidaTest {

    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private RepositorioEntidades repoEntidades;
    @Mock private RepositorioDirecciones repoDirecciones;
    @Mock private RepositorioCiudades repoCiudades;
    @Mock private RepositorioProvincias repoProvincias;
    @Mock private RepositorioPaises repoPaises;
    @Mock private RepositorioUnidadesDeMedida repoUnidades;

    @Test
    @DisplayName("UnidadDeMedida no debe tener campo anotado con @Version para evitar bloqueos optimistas en instancias estáticas")
    void noPoseeAnotacionVersion() {
        boolean tieneVersion = Arrays.stream(UnidadDeMedida.class.getDeclaredFields())
                .anyMatch(f -> f.isAnnotationPresent(Version.class));

        assertThat(tieneVersion)
                .as("UnidadDeMedida no debe tener @Version porque sus instancias compartidas en memoria fallan con OptimisticLockingFailureException")
                .isFalse();
    }

    @Test
    @DisplayName("Las instancias estáticas deben tener identificadores deterministas estables entre reinicios")
    void instanciasEstaticasTienenIdentificadoresDeterministas() {
        UUID expectedUnidades = UUID.nameUUIDFromBytes("UNIDADES".getBytes(StandardCharsets.UTF_8));
        UUID expectedKg = UUID.nameUUIDFromBytes("KILOGRAMOS".getBytes(StandardCharsets.UTF_8));
        UUID expectedLitros = UUID.nameUUIDFromBytes("LITROS".getBytes(StandardCharsets.UTF_8));

        assertThat(UnidadDeMedida.UNIDADES.getIdUnidad())
                .as("El ID de UNIDADES debe ser determinista")
                .isEqualTo(expectedUnidades);

        assertThat(UnidadDeMedida.KILOGRAMOS.getIdUnidad())
                .as("El ID de KILOGRAMOS debe ser determinista")
                .isEqualTo(expectedKg);

        assertThat(UnidadDeMedida.LITROS.getIdUnidad())
                .as("El ID de LITROS debe ser determinista")
                .isEqualTo(expectedLitros);
    }

    @Test
    @DisplayName("El registro de entregas no persiste la unidad de medida estática por cada donación")
    void elRegistroNoPersisteUnidadEstaticaEnCadaDonacion() {
        GestorCatalogoUbicaciones catalogo = new GestorCatalogoUbicaciones(
                repoEntidades, repoDirecciones, repoCiudades, repoProvincias, repoPaises);
        GestorRegistroEntregas gestor = new GestorRegistroEntregas(repoItemEntrega, catalogo);

        UUID idDonacion = UUID.randomUUID();
        UUID idEntidad = UUID.randomUUID();

        BienDTO bien = new BienDTO();
        bien.setCantidad(5);
        bien.setUnidadDeMedida("UNIDADES");

        DireccionDTO direccionDTO = new DireccionDTO(
                idEntidad, "Medrano", null, 951, null, null, "CABA", "Buenos Aires", "Argentina"
        );

        EntregaDTO dto = new EntregaDTO(List.of(idDonacion), List.of(bien), direccionDTO);

        Pais pais = new Pais();
        pais.setIdPais("pais-1");
        when(repoPaises.findByNombreIgnoreCase(any())).thenReturn(Optional.of(pais));

        Provincia prov = new Provincia();
        prov.setIdProvincia("prov-1");
        when(repoProvincias.findByNombreIgnoreCaseAndPais_IdPais(any(), any())).thenReturn(Optional.of(prov));

        Ciudad ciudad = new Ciudad("CABA", prov);
        ciudad.setIdCiudad("ciudad-1");
        when(repoCiudades.findByNombreIgnoreCaseAndProvincia_IdProvincia(any(), any())).thenReturn(Optional.of(ciudad));

        Direccion dir = new Direccion();
        dir.setIdDireccion(UUID.randomUUID());
        when(repoDirecciones.save(any(Direccion.class))).thenReturn(dir);

        Entidad entidad = new Entidad(idEntidad, dir);
        when(repoEntidades.findById(any())).thenReturn(Optional.of(entidad));

        gestor.registrar(dto);

        // Verificamos que repoUnidades.save NO se invoque en cada donación
        verify(repoUnidades, never()).save(any());
        verify(repoItemEntrega, times(1)).save(any());
    }
}
