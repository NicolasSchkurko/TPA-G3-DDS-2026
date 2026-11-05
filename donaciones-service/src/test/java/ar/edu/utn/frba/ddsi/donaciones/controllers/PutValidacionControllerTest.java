package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frba.ddsi.donaciones.services.AdminService;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Punto 56 de PENDIENTES.md:
 * Los PUT de personas, entidades y admins no deben convertir fallos de validación (IllegalArgumentException)
 * en un 404 mudo. Deben responder 400 Bad Request si la validación falla y 404 Not Found si la entidad no existe.
 */
class PutValidacionControllerTest {

  private DonanteService donanteService;
  private EntidadBeneficiariaService entidadService;
  private AdminService adminService;

  private MockMvc donanteMvc;
  private MockMvc entidadMvc;
  private MockMvc adminMvc;

  @BeforeEach
  void setUp() {
    donanteService = mock(DonanteService.class);
    entidadService = mock(EntidadBeneficiariaService.class);
    adminService = mock(AdminService.class);

    donanteMvc = MockMvcBuilders.standaloneSetup(new DonanteController(donanteService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    entidadMvc = MockMvcBuilders.standaloneSetup(new EntidadBeneficiariaController(entidadService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    adminMvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  @DisplayName("PUT /api/personas/{id} con body que lanza IllegalArgumentException responde 400 Bad Request")
  void putPersona_validacionInvalida_responde400() throws Exception {
    UUID id = UUID.randomUUID();
    when(donanteService.actualizarPersona(eq(id), any(PersonaDonanteDTO.class)))
        .thenThrow(new IllegalArgumentException("Tipo de persona inválido: MASCOTA"));

    donanteMvc.perform(put("/api/personas/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tipoPersona\":\"MASCOTA\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigoEstado").value(400))
        .andExpect(jsonPath("$.mensaje").value("Tipo de persona inválido: MASCOTA"));
  }

  @Test
  @DisplayName("PUT /api/personas/{id} inexistente responde 404 Not Found")
  void putPersona_inexistente_responde404() throws Exception {
    UUID id = UUID.randomUUID();
    when(donanteService.actualizarPersona(eq(id), any(PersonaDonanteDTO.class)))
        .thenThrow(new EntityNotFoundException("No se encontró persona con ID: " + id));

    donanteMvc.perform(put("/api/personas/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tipoPersona\":\"HUMANA\",\"nombre\":\"Juan\",\"apellido\":\"Perez\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigoEstado").value(404));
  }

  @Test
  @DisplayName("PUT /api/entidades/{id} con razón social vacía responde 400 Bad Request")
  void putEntidad_validacionInvalida_responde400() throws Exception {
    UUID id = UUID.randomUUID();
    when(entidadService.actualizarEntidad(eq(id), any(EntidadBeneficiariaDTO.class)))
        .thenThrow(new IllegalArgumentException("La razón social es obligatoria para una persona jurídica."));

    entidadMvc.perform(put("/api/entidades/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigoEstado").value(400))
        .andExpect(jsonPath("$.mensaje").value("La razón social es obligatoria para una persona jurídica."));
  }

  @Test
  @DisplayName("PUT /api/entidades/{id} inexistente responde 404 Not Found")
  void putEntidad_inexistente_responde404() throws Exception {
    UUID id = UUID.randomUUID();
    when(entidadService.actualizarEntidad(eq(id), any(EntidadBeneficiariaDTO.class)))
        .thenThrow(new EntityNotFoundException("No se encontró la entidad con ID: " + id));

    entidadMvc.perform(put("/api/entidades/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"razonSocial\":\"Comedor Central\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigoEstado").value(404));
  }

  @Test
  @DisplayName("PUT /api/admins/{id} sin contacto responde 400 Bad Request")
  void putAdmin_validacionInvalida_responde400() throws Exception {
    UUID id = UUID.randomUUID();
    when(adminService.actualizarAdmin(eq(id), any(AdminDTO.class)))
        .thenThrow(new IllegalArgumentException("El administrador debe tener un medio de contacto asignado."));

    adminMvc.perform(put("/api/admins/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigoEstado").value(400))
        .andExpect(jsonPath("$.mensaje").value("El administrador debe tener un medio de contacto asignado."));
  }

  @Test
  @DisplayName("PUT /api/admins/{id} inexistente responde 404 Not Found")
  void putAdmin_inexistente_responde404() throws Exception {
    UUID id = UUID.randomUUID();
    when(adminService.actualizarAdmin(eq(id), any(AdminDTO.class)))
        .thenThrow(new EntityNotFoundException("No se encontró el administrador con ID: " + id));

    adminMvc.perform(put("/api/admins/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombreAMostrar\":\"Admin Principal\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigoEstado").value(404));
  }

  @Test
  @DisplayName("GET /api/admins/{id} inexistente responde 404 Not Found")
  void getAdmin_inexistente_responde404() throws Exception {
    UUID id = UUID.randomUUID();
    when(adminService.getAdminPorId(eq(id)))
        .thenThrow(new EntityNotFoundException("No se encontró el administrador con ID: " + id));

    adminMvc.perform(get("/api/admins/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigoEstado").value(404));
  }
}
