package ar.edu.utn.frba.ddsi.incentivos.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.controllers.PerfilController;
import ar.edu.utn.frba.ddsi.incentivos.dto.ActividadMensualDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.MetricasActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.InsigniaDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.MetricasActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PerfilControllerTest {
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PerfilService service;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new PerfilController(service)).build();
    }

    @Test
    public void obtenerMetricasPerfil_ok() throws Exception {
        UUID id = UUID.randomUUID();
        MetricasActividad metrica = mock(MetricasActividad.class);
        MetricasActividadDTO dto = new MetricasActividadDTO(YearMonth.now(), 10.0, 5.0);

        when(service.obtenerMetricasDonante(id)).thenReturn(metrica);
        when(service.convertirMetricaADTO(metrica)).thenReturn(dto);

        mockMvc.perform(get("/perfiles/{id}/comparativa", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void obtenerMetricasPerfil_notFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.obtenerMetricasDonante(id)).thenReturn(null);

        mockMvc.perform(get("/perfiles/{id}/comparativa", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void obtenerActividadPerfil_ok() throws Exception {
        UUID id = UUID.randomUUID();
        ActividadMensual actividad = mock(ActividadMensual.class);
        ActividadMensualDTO dto = new ActividadMensualDTO(YearMonth.now(), 1, 1);

        when(service.obtenerEvolucionHistorica(id)).thenReturn(Arrays.asList(actividad));
        when(service.convertirActividadADTO(any())).thenReturn(dto);

        mockMvc.perform(get("/perfiles/{id}/metricas", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void obtenerActividadPerfil_notFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.obtenerEvolucionHistorica(id)).thenReturn(null);

        mockMvc.perform(get("/perfiles/{id}/metricas", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void obtenerMisionPerfil_ok() throws Exception {
        UUID id = UUID.randomUUID();
        Mision mision = mock(Mision.class);
        MisionDTO dto = new MisionDTO("Mision Test", 2, 5);

        when(service.obtenerMisionPorID(id)).thenReturn(mision);
        when(service.convertirMisionADTO(mision)).thenReturn(dto);

        mockMvc.perform(get("/perfiles/{id}/mision", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void obtenerMisionPerfil_notFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.obtenerMisionPorID(id)).thenReturn(null);

        mockMvc.perform(get("/perfiles/{id}/mision", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void obtenerInsigniasPerfil_ok() throws Exception {
        UUID id = UUID.randomUUID();
        Insignia insignia = mock(Insignia.class);
        InsigniaDTO dto = new InsigniaDTO("Nombre", "Desc", "url", java.time.LocalDate.now());

        when(service.obtenerInsigniasPorID(id)).thenReturn(Arrays.asList(insignia));
        when(service.convertirInsigniaADTO(any())).thenReturn(dto);

        mockMvc.perform(get("/perfiles/{id}/insignias", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void obtenerInsigniasPerfil_notFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.obtenerInsigniasPorID(id)).thenReturn(null);

        mockMvc.perform(get("/perfiles/{id}/insignias", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
