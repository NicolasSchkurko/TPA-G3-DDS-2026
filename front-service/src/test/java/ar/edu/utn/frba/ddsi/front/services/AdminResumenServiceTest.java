package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.NecesidadDTO;
import ar.edu.utn.frba.ddsi.front.dto.logistica.LogisticaDTOs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminResumenServiceTest {

    @Mock
    private DonacionesClient donacionesClient;
    @Mock
    private LogisticasClient logisticasClient;

    private AdminResumenService service;

    @BeforeEach
    void setUp() {
        service = new AdminResumenService(donacionesClient, logisticasClient);
        // cache deshabilitado para ejercitar fallback en cada llamada
        ReflectionTestUtils.setField(service, "cacheMs", 0L);
    }

    @Test
    void cargaDatosReales() {
        DonacionDTO d1 = new DonacionDTO();
        d1.setEstado("EN_DEPOSITO");
        DonacionDTO d2 = new DonacionDTO();
        d2.setEstado("VENCIDO");

        EntidadBeneficiariaDTO ent = new EntidadBeneficiariaDTO();
        NecesidadDTO n = new NecesidadDTO();
        n.setActiva(true);
        ent.setNecesidades(List.of(n));

        CamionDTO cDisp = new CamionDTO();
        cDisp.setDisponible(true);
        CamionDTO cOcup = new CamionDTO();
        cOcup.setDisponible(false);

        LogisticaDTOs.Bienes noRec = new LogisticaDTOs.Bienes();
        LogisticaDTOs.Bien b = new LogisticaDTOs.Bien();
        b.setEstado("NO_RECIBIDA");
        noRec.setBienes(List.of(b));

        when(donacionesClient.obtenerTodasLasDonaciones()).thenReturn(List.of(d1, d2));
        when(donacionesClient.obtenerDonantes()).thenReturn(List.of());
        when(donacionesClient.obtenerEntidades()).thenReturn(List.of(ent));
        when(donacionesClient.obtenerResultadosMatchmaking()).thenReturn(List.of());
        when(logisticasClient.obtenerCamiones()).thenReturn(List.of(cDisp, cOcup));
        when(logisticasClient.obtenerEntregasNoRecibidas()).thenReturn(noRec);
        when(logisticasClient.obtenerRutasActivas()).thenReturn(List.of());

        AdminResumenService.Resumen r = service.obtenerResumen();

        assertThat(r.getTotalDonaciones()).isEqualTo(2);
        assertThat(r.getDonacionesPorEstado()).containsEntry("EN_DEPOSITO", 1L);
        assertThat(r.getDonacionesPorEstado()).containsEntry("VENCIDO", 1L);
        assertThat(r.getEntidades()).isEqualTo(1);
        assertThat(r.getNecesidadesActivas()).isEqualTo(1);
        assertThat(r.getCamionesDisponibles()).isEqualTo(1);
        assertThat(r.getCamionesOcupados()).isEqualTo(1);
        assertThat(r.getEntregasNoRecibidas()).isEqualTo(1);
        assertThat(r.isDesactualizado()).isFalse();
    }

    @Test
    void fallbackConservaUltimoDatoYMarcaDesactualizado() {
        DonacionDTO d1 = new DonacionDTO();
        d1.setEstado("EN_DEPOSITO");
        when(donacionesClient.obtenerTodasLasDonaciones()).thenReturn(List.of(d1));
        when(donacionesClient.obtenerDonantes()).thenReturn(List.of());
        when(donacionesClient.obtenerEntidades()).thenReturn(List.of());
        when(donacionesClient.obtenerResultadosMatchmaking()).thenReturn(List.of());
        when(logisticasClient.obtenerCamiones()).thenReturn(List.of());
        when(logisticasClient.obtenerEntregasNoRecibidas()).thenReturn(null);
        when(logisticasClient.obtenerRutasActivas()).thenReturn(List.of());

        AdminResumenService.Resumen primero = service.obtenerResumen();
        assertThat(primero.getTotalDonaciones()).isEqualTo(1);

        // Ahora los clientes fallan
        when(donacionesClient.obtenerTodasLasDonaciones()).thenThrow(new RuntimeException("boom"));

        AdminResumenService.Resumen segundo = service.obtenerResumen();
        assertThat(segundo.getTotalDonaciones()).isEqualTo(1);
        assertThat(segundo.isDesactualizado()).isTrue();
    }

    @Test
    void cacheHitNoRecarga() {
        ReflectionTestUtils.setField(service, "cacheMs", 60000L);
        when(donacionesClient.obtenerTodasLasDonaciones()).thenReturn(List.of());
        when(donacionesClient.obtenerDonantes()).thenReturn(List.of());
        when(donacionesClient.obtenerEntidades()).thenReturn(List.of());
        when(donacionesClient.obtenerResultadosMatchmaking()).thenReturn(List.of());
        when(logisticasClient.obtenerCamiones()).thenReturn(List.of());
        when(logisticasClient.obtenerEntregasNoRecibidas()).thenReturn(null);
        when(logisticasClient.obtenerRutasActivas()).thenReturn(List.of());

        service.obtenerResumen();
        service.obtenerResumen();
        verify(donacionesClient, times(1)).obtenerTodasLasDonaciones();
    }
}
