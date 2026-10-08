package ar.edu.utn.frba.ddsi.incentivos.controllers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El 400 de Bean Validation tiene que dejar rastro en el log del servicio: es la cara de
 * incentivos del incidente en que donaciones mandó el alta sin nombreUsuario y el rechazo
 * solo se veía del lado que enviaba.
 */
public class PerfilAltaValidacionTest {

  private MockMvc mockMvc;
  private ListAppender<ILoggingEvent> captadorDeLogs;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new PerfilController(mock(PerfilService.class)))
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
}
