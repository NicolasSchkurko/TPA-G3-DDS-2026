package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.FormularioResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Punto 38 de PENDIENTES.md:
 * Los 5 listados principales deben soportar paginación mediante Pageable y devolver Page<DTO>:
 * - GET /api/donaciones
 * - GET /api/donaciones/formularios
 * - GET /api/donaciones/pendientes
 * - GET /api/personas
 * - GET /api/entidades
 */
class PaginacionListadosControllerTest {

  private DonacionService donacionService;
  private DonanteService donanteService;
  private EntidadBeneficiariaService entidadService;

  private MockMvc donacionMvc;
  private MockMvc donanteMvc;
  private MockMvc entidadMvc;

  @BeforeEach
  void setUp() {
    donacionService = mock(DonacionService.class);
    donanteService = mock(DonanteService.class);
    entidadService = mock(EntidadBeneficiariaService.class);

    donacionMvc = MockMvcBuilders.standaloneSetup(new DonacionController(donacionService, mock(RabbitTemplate.class)))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
        .build();

    donanteMvc = MockMvcBuilders.standaloneSetup(new DonanteController(donanteService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
        .build();

    entidadMvc = MockMvcBuilders.standaloneSetup(new EntidadBeneficiariaController(entidadService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
        .build();
  }

  @Test
  @DisplayName("GET /api/donaciones devuelve Page de DonacionDTO con metadatos de paginación")
  void getDonaciones_paginado() throws Exception {
    DonacionDTO dto = new DonacionDTO();
    dto.setId(UUID.randomUUID());
    when(donacionService.obtenerTodas(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

    donacionMvc.perform(get("/api/donaciones?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(dto.getId().toString()))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  @DisplayName("GET /api/donaciones/formularios devuelve Page de FormularioResumenDTO con metadatos")
  void getFormularios_paginado() throws Exception {
    FormularioResumenDTO dto = new FormularioResumenDTO();
    dto.setId(UUID.randomUUID());
    when(donacionService.obtenerFormularios(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

    donacionMvc.perform(get("/api/donaciones/formularios?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(dto.getId().toString()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("GET /api/donaciones/pendientes devuelve Page de ResultadoMatchmakingDTO con metadatos")
  void getPendientes_paginado() throws Exception {
    ResultadoMatchmakingDTO dto = new ResultadoMatchmakingDTO();
    when(donacionService.obtenerTodosLosResultadosMatchmaking(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

    donacionMvc.perform(get("/api/donaciones/pendientes?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("GET /api/personas devuelve Page de PersonaDonanteDTO con metadatos")
  void getPersonas_paginado() throws Exception {
    PersonaDonanteDTO dto = new PersonaDonanteDTO();
    dto.setId(UUID.randomUUID());
    when(donanteService.listarTodas(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

    donanteMvc.perform(get("/api/personas?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(dto.getId().toString()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("GET /api/entidades devuelve Page de EntidadBeneficiariaDTO con metadatos")
  void getEntidades_paginado() throws Exception {
    EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
    dto.setId(UUID.randomUUID());
    when(entidadService.obtenerTodas(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

    entidadMvc.perform(get("/api/entidades?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(dto.getId().toString()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }
}
