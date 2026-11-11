package ar.edu.utn.frba.ddsi.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frba.ddsi.donaciones.services.AdminService;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("AdminController")
class AdminControllerTest {

  private AdminService adminService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    adminService = mock(AdminService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  @DisplayName("PUT /api/admins/{id} sin contacto responde 400")
  void putAdminValidacionInvalidaResponde400() throws Exception {
    UUID id = UUID.randomUUID();
    when(adminService.actualizarAdmin(eq(id), any(AdminDTO.class)))
        .thenThrow(new IllegalArgumentException("El administrador debe tener un medio de contacto asignado."));

    mockMvc.perform(put("/api/admins/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigoEstado").value(400))
        .andExpect(jsonPath("$.mensaje").value("El administrador debe tener un medio de contacto asignado."));
  }

  @Test
  @DisplayName("PUT /api/admins/{id} inexistente responde 404")
  void putAdminInexistenteResponde404() throws Exception {
    UUID id = UUID.randomUUID();
    when(adminService.actualizarAdmin(eq(id), any(AdminDTO.class)))
        .thenThrow(new EntityNotFoundException("No se encontró el administrador con ID: " + id));

    mockMvc.perform(put("/api/admins/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombreAMostrar\":\"Admin Principal\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigoEstado").value(404));
  }

  @Test
  @DisplayName("GET /api/admins/{id} inexistente responde 404")
  void getAdminInexistenteResponde404() throws Exception {
    UUID id = UUID.randomUUID();
    when(adminService.getAdminPorId(eq(id)))
        .thenThrow(new EntityNotFoundException("No se encontró el administrador con ID: " + id));

    mockMvc.perform(get("/api/admins/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigoEstado").value(404));
  }
}
