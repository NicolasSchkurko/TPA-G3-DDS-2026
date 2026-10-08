package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.ActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MetricasService: la actividad de un usuario inexistente es 404")
class MetricasServiceTest {

    private RepositorioDonaciones repositorioDonaciones;
    private RepositorioPerfiles repositorioPerfiles;
    private MetricasService service;

    @BeforeEach
    void setUp() {
        repositorioDonaciones = mock(RepositorioDonaciones.class);
        repositorioPerfiles = mock(RepositorioPerfiles.class);
        service = new MetricasService(repositorioDonaciones, repositorioPerfiles);
    }

    @Test
    @DisplayName("si el perfil no existe lanza InexistenteException y no consulta donaciones")
    void perfilInexistenteLanzaInexistente() {
        UUID idUsuario = UUID.randomUUID();
        when(repositorioPerfiles.existsByIdUsuario(idUsuario)).thenReturn(false);

        assertThatThrownBy(() -> service.obtenerEvolucionHistorica(idUsuario))
                .isInstanceOf(InexistenteException.class);

        verify(repositorioDonaciones, never()).obtenerEvolucionMensual(idUsuario);
    }

    @Test
    @DisplayName("si el perfil existe devuelve la actividad armada con las agregaciones")
    void perfilExistenteDevuelveActividad() {
        UUID idUsuario = UUID.randomUUID();
        when(repositorioPerfiles.existsByIdUsuario(idUsuario)).thenReturn(true);
        when(repositorioDonaciones.obtenerEvolucionMensual(idUsuario))
                .thenReturn(List.<Object[]>of(new Object[]{2026, 3, 5L, 2L}));
        when(repositorioDonaciones.obtenerTotalesDonaciones(idUsuario))
                .thenReturn(new Object[]{5L, 2L});

        ActividadDTO dto = service.obtenerEvolucionHistorica(idUsuario);

        assertThat(dto.getRegistros()).hasSize(1);
        assertThat(dto.getRegistros().get(0).getPeriodo()).isEqualTo(YearMonth.of(2026, 3));
        assertThat(dto.getRegistros().get(0).getCantidadDonaciones()).isEqualTo(5L);
        assertThat(dto.getTotalDonaciones()).isEqualTo(5L);
        assertThat(dto.getTotalOrganizaciones()).isEqualTo(2L);
    }
}
