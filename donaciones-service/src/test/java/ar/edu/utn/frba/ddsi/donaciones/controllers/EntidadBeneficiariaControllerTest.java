package ar.edu.utn.frba.ddsi.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("EntidadBeneficiariaController")
class EntidadBeneficiariaControllerTest {

  private EntidadBeneficiariaService entidadService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    entidadService = mock(EntidadBeneficiariaService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new EntidadBeneficiariaController(entidadService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
        .build();
  }

  @Test
  @DisplayName("GET /api/entidades devuelve Page de EntidadBeneficiariaDTO")
  void getEntidadesPaginado() throws Exception {
    EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
    dto.setId(UUID.randomUUID());
    when(entidadService.obtenerTodas(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

    mockMvc.perform(get("/api/entidades?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(dto.getId().toString()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("PUT /api/entidades/{id} con razón social vacía responde 400")
  void putEntidadValidacionInvalidaResponde400() throws Exception {
    UUID id = UUID.randomUUID();
    when(entidadService.actualizarEntidad(eq(id), any(EntidadBeneficiariaDTO.class)))
        .thenThrow(new IllegalArgumentException("La razón social es obligatoria para una persona jurídica."));

    mockMvc.perform(put("/api/entidades/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigoEstado").value(400))
        .andExpect(jsonPath("$.mensaje").value("La razón social es obligatoria para una persona jurídica."));
  }

  @Test
  @DisplayName("PUT /api/entidades/{id} inexistente responde 404")
  void putEntidadInexistenteResponde404() throws Exception {
    UUID id = UUID.randomUUID();
    when(entidadService.actualizarEntidad(eq(id), any(EntidadBeneficiariaDTO.class)))
        .thenThrow(new EntityNotFoundException("No se encontró la entidad con ID: " + id));

    mockMvc.perform(put("/api/entidades/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"razonSocial\":\"Comedor Central\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigoEstado").value(404));
  }
}
