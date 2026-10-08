package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ResultadoLotePerfilesDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El 400 de Bean Validation tiene que dejar rastro en el log del servicio (es la cara de
 * incentivos del incidente en que donaciones mandó el alta sin nombreUsuario), y el alta en
 * lote valida y delega como corresponde.
 */
public class PerfilAltaValidacionTest {

  private MockMvc mockMvc;
  private PerfilService perfilService;
  private ListAppender<ILoggingEvent> captadorDeLogs;

  @BeforeEach
  void setUp() {
    perfilService = mock(PerfilService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new PerfilController(perfilService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
    captadorDeLogs = new ListAppender<>();
    captadorDeLogs.start();
    ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).addAppender(captadorDeLogs);
  }

  @AfterEach
  void tearDown() {
    ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).detachAppender(captadorDeLogs);
  }

  @Test
  @DisplayName("Un alta sin nombreUsuario responde 400 con detalle y deja el rechazo en el log")
  void altaSinNombreUsuario_rechazaYLoguea() throws Exception {
    PerfilDonanteDTO alta = new PerfilDonanteDTO();
    alta.setIdUsuario(UUID.randomUUID());
    // nombreUsuario ausente: el caso real que donaciones mandaba sin el campo.

    mockMvc.perform(post("/api/perfiles")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(alta)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.nombreUsuario").value("El donante requiere un nombre de usuario"));

    List<ILoggingEvent> rechazos = captadorDeLogs.list.stream()
        .filter(evento -> evento.getFormattedMessage().contains("Request rechazado por validación"))
        .toList();

    assertFalse(rechazos.isEmpty(), "el rechazo tiene que quedar en el log");
    assertTrue(rechazos.stream().anyMatch(evento -> evento.getLevel() == Level.WARN));
    assertTrue(rechazos.stream().map(ILoggingEvent::getFormattedMessage)
            .anyMatch(mensaje -> mensaje.contains("nombreUsuario")
                    && mensaje.contains("El donante requiere un nombre de usuario")),
        "el log tiene que decir qué campo falló y por qué");
  }

  @Test
  @DisplayName("El alta en lote delega en el servicio y devuelve creados/yaExistian/errores")
  void loteValido_delegaYDevuelveResultado() throws Exception {
    when(perfilService.crearPerfilesEnLote(any()))
        .thenReturn(new ResultadoLotePerfilesDTO(2, 1, List.of()));

    String body = new ObjectMapper().writeValueAsString(java.util.Map.of("perfiles", List.of(
        java.util.Map.of("idUsuario", UUID.randomUUID().toString(),
            "nombreUsuario", "Sofia", "role", "DONANTE"),
        java.util.Map.of("idUsuario", UUID.randomUUID().toString(),
            "nombreUsuario", "Ana", "role", "DONANTE"))));

    mockMvc.perform(post("/api/perfiles/lote")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.creados").value(2))
        .andExpect(jsonPath("$.yaExistian").value(1));

    verify(perfilService).crearPerfilesEnLote(any());
  }

  @Test
  @DisplayName("Un lote vacío se rechaza con 400 sin llegar al servicio")
  void loteVacio_rechazaSinLlamarAlServicio() throws Exception {
    mockMvc.perform(post("/api/perfiles/lote")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"perfiles\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.perfiles").value("El lote requiere al menos un perfil"));

    verify(perfilService, never()).crearPerfilesEnLote(any());
  }
}
