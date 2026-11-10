package ar.edu.utn.frba.ddsi.donaciones.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personadonante.FormularioResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("DonacionController")
class DonacionControllerTest {

  private static final String RUTA_FORMULARIO = "/api/donaciones/formulario";

  private DonacionService donacionService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    donacionService = mock(DonacionService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new DonacionController(donacionService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
        .build();
  }

  @Nested
  @DisplayName("Paginación")
  class Paginacion {

    @Test
    @DisplayName("GET /api/donaciones devuelve Page de DonacionDTO")
    void getDonacionesPaginado() throws Exception {
      DonacionDTO dto = new DonacionDTO();
      dto.setId(UUID.randomUUID());
      when(donacionService.obtenerTodas(any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

      mockMvc.perform(get("/api/donaciones?page=0&size=10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(dto.getId().toString()))
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/donaciones/formularios devuelve Page de FormularioResumenDTO")
    void getFormulariosPaginado() throws Exception {
      FormularioResumenDTO dto = new FormularioResumenDTO();
      dto.setId(UUID.randomUUID());
      when(donacionService.obtenerFormularios(any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

      mockMvc.perform(get("/api/donaciones/formularios?page=0&size=10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(dto.getId().toString()))
          .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/donaciones/pendientes devuelve Page de ResultadoMatchmakingDTO")
    void getPendientesPaginado() throws Exception {
      ResultadoMatchmakingDTO dto = new ResultadoMatchmakingDTO();
      when(donacionService.obtenerTodosLosResultadosMatchmaking(any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

      mockMvc.perform(get("/api/donaciones/pendientes?page=0&size=10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(1));
    }
  }

  @Nested
  @DisplayName("Validación del formulario")
  class FormularioValidation {

    @Test
    @DisplayName("un bien con cantidad negativa responde 400 y no llega al service")
    void bienConCantidadNegativaResponde400() throws Exception {
      mockMvc.perform(post(RUTA_FORMULARIO)
              .contentType(MediaType.APPLICATION_JSON)
              .content(formularioCon(
                  "{\"tipoBien\":\"CON_ESTADO\",\"descripcion\":\"arroz\",\"cantidad\":-50,\"usado\":false}")))
          .andExpect(status().isBadRequest())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.mensaje", containsString("cantidad")))
          .andExpect(jsonPath("$.codigoEstado").value(400));

      verify(donacionService, never()).procesarFormulario(any());
    }

    @Test
    @DisplayName("un bien sin tipoBien responde 400")
    void bienSinTipoBienResponde400() throws Exception {
      mockMvc.perform(post(RUTA_FORMULARIO)
              .contentType(MediaType.APPLICATION_JSON)
              .content(formularioCon("{\"descripcion\":\"arroz\",\"cantidad\":10,\"usado\":false}")))
          .andExpect(status().isBadRequest());

      verify(donacionService, never()).procesarFormulario(any());
    }

    @Test
    @DisplayName("un formulario sin fechaRealizacion responde 400")
    void formularioSinFechaRealizacionResponde400() throws Exception {
      mockMvc.perform(post(RUTA_FORMULARIO)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"idDonante\":\"" + UUID.randomUUID() + "\","
                  + "\"bienes\":[{\"tipoBien\":\"CON_ESTADO\",\"descripcion\":\"arroz\","
                  + "\"cantidad\":10,\"usado\":false}]}"))
          .andExpect(status().isBadRequest());

      verify(donacionService, never()).procesarFormulario(any());
    }

    @Test
    @DisplayName("un formulario válido llega al service")
    void formularioValidoLlegaAlService() throws Exception {
      when(donacionService.procesarFormulario(any())).thenReturn(List.of(new DonacionDTO()));

      mockMvc.perform(post(RUTA_FORMULARIO)
              .contentType(MediaType.APPLICATION_JSON)
              .content(formularioCon(
                  "{\"tipoBien\":\"CON_ESTADO\",\"descripcion\":\"arroz\",\"cantidad\":10,\"usado\":false}")))
          .andExpect(status().isOk());

      verify(donacionService).procesarFormulario(any());
    }

    private String formularioCon(String bienJson) {
      return "{\"idDonante\":\"" + UUID.randomUUID() + "\","
          + "\"fechaRealizacion\":\"2026-10-01\","
          + "\"bienes\":[" + bienJson + "]}";
    }
  }
}
