package ar.edu.utn.frba.ddsi.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frba.ddsi.donaciones.dto.personadonante.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
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

@DisplayName("DonanteController")
class DonanteControllerTest {

  private DonanteService donanteService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    donanteService = mock(DonanteService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new DonanteController(donanteService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
        .build();
  }

  @Test
  @DisplayName("GET /api/personas devuelve Page de PersonaDonanteDTO")
  void getPersonasPaginado() throws Exception {
    PersonaDonanteDTO dto = new PersonaDonanteDTO();
    dto.setId(UUID.randomUUID());
    when(donanteService.listarTodas(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

    mockMvc.perform(get("/api/personas?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(dto.getId().toString()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("PUT /api/personas/{id} con validación inválida responde 400")
  void putPersonaValidacionInvalidaResponde400() throws Exception {
    UUID id = UUID.randomUUID();
    when(donanteService.actualizarPersona(eq(id), any(PersonaDonanteDTO.class)))
        .thenThrow(new IllegalArgumentException("Tipo de persona inválido: MASCOTA"));

    mockMvc.perform(put("/api/personas/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tipoPersona\":\"MASCOTA\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigoEstado").value(400))
        .andExpect(jsonPath("$.mensaje").value("Tipo de persona inválido: MASCOTA"));
  }

  @Test
  @DisplayName("PUT /api/personas/{id} inexistente responde 404")
  void putPersonaInexistenteResponde404() throws Exception {
    UUID id = UUID.randomUUID();
    when(donanteService.actualizarPersona(eq(id), any(PersonaDonanteDTO.class)))
        .thenThrow(new EntityNotFoundException("No se encontró persona con ID: " + id));

    mockMvc.perform(put("/api/personas/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tipoPersona\":\"HUMANA\",\"nombre\":\"Juan\",\"apellido\":\"Perez\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigoEstado").value(404));
  }

  @Test
  @DisplayName("DELETE /api/personas/{id}/medios-contacto responde 200 OK")
  void eliminarMedioContactoResponde200() throws Exception {
    UUID id = UUID.randomUUID();
    PersonaDonanteDTO dtoResp = new PersonaDonanteDTO();
    when(donanteService.eliminarMedioContacto(eq(id), any())).thenReturn(dtoResp);

    mockMvc.perform(delete("/api/personas/" + id + "/medios-contacto")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tipo\":\"MAIL\",\"valor\":\"user@test.com\"}"))
        .andExpect(status().isOk());
  }
}
