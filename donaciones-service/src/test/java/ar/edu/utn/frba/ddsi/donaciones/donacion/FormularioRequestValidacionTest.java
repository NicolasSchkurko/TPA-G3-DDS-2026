package ar.edu.utn.frba.ddsi.donaciones.donacion;

import ar.edu.utn.frba.ddsi.donaciones.controllers.DonacionController;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Punto 22 de PENDIENTES.md: los DTOs de entrada no tienen Bean Validation, así que
 * entraban cantidades negativas (`cantidad: -50` -> `Bien.peso = -50`) y bienes sin
 * `tipoBien`, que revientan más tarde en la segmentación y en los conteos.
 *
 * <p>Los tests llaman al endpoint real (`POST /donaciones/formulario`) con MockMvc
 * standalone: la validación tiene que cortar la entrada con un 400 **antes** de llegar
 * al service (el service está mockeado y si se lo invoca con un bien inválido, validó mal).
 */
public class FormularioRequestValidacionTest {

  private MockMvc mockMvc;
  private DonacionService donacionService;

  @BeforeEach
  void setUp() {
    donacionService = mock(DonacionService.class);
    DonacionController controller =
        new DonacionController(donacionService, mock(RabbitTemplate.class));
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  private String formularioCon(String bienJson) {
    return "{\"idDonante\":\"" + UUID.randomUUID() + "\","
        + "\"fechaRealizacion\":\"2026-10-01\","
        + "\"bienes\":[" + bienJson + "]}";
  }

  @Test
  @DisplayName("Un bien con cantidad negativa es rechazado con 400 y no llega al service")
  void bienConCantidadNegativa_responde400() throws Exception {
    mockMvc.perform(post("/donaciones/formulario")
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
  @DisplayName("Un bien sin tipoBien es rechazado con 400 en la entrada")
  void bienSinTipoBien_responde400() throws Exception {
    mockMvc.perform(post("/donaciones/formulario")
            .contentType(MediaType.APPLICATION_JSON)
            .content(formularioCon(
                "{\"descripcion\":\"arroz\",\"cantidad\":10,\"usado\":false}")))
        .andExpect(status().isBadRequest());

    verify(donacionService, never()).procesarFormulario(any());
  }

  @Test
  @DisplayName("Un formulario sin fechaRealizacion es rechazado con 400")
  void formularioSinFechaRealizacion_responde400() throws Exception {
    mockMvc.perform(post("/donaciones/formulario")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"idDonante\":\"" + UUID.randomUUID() + "\","
                + "\"bienes\":[{\"tipoBien\":\"CON_ESTADO\",\"descripcion\":\"arroz\","
                + "\"cantidad\":10,\"usado\":false}]}"))
        .andExpect(status().isBadRequest());

    verify(donacionService, never()).procesarFormulario(any());
  }

  @Test
  @DisplayName("Un formulario válido sigue llegando al service")
  void formularioValido_llegaAlService() throws Exception {
    when(donacionService.procesarFormulario(any())).thenReturn(List.of(new DonacionDTO()));

    mockMvc.perform(post("/donaciones/formulario")
            .contentType(MediaType.APPLICATION_JSON)
            .content(formularioCon(
                "{\"tipoBien\":\"CON_ESTADO\",\"descripcion\":\"arroz\",\"cantidad\":10,\"usado\":false}")))
        .andExpect(status().isOk());

    verify(donacionService).procesarFormulario(any());
  }
}
