package ar.edu.utn.frba.ddsi.notificaciones.controllers;

import ar.edu.utn.frba.ddsi.notificaciones.mappers.NotificacionMapper;
import ar.edu.utn.frba.ddsi.notificaciones.services.NotificadorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El borde HTTP de {@code POST /notificaciones}: validación del body y cuerpo de error uniforme. */
@WebMvcTest(NotificadorController.class)
class NotificadorControllerTest {

    private static final String BODY_COMPLETO = """
            {
              "medioDeContacto": "email",
              "direccionDeContacto": "ana@test.com",
              "asuntoMensaje": "Asunto",
              "cuerpoMensaje": "Cuerpo"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificadorService notificadorService;

    @MockBean
    private NotificacionMapper notificacionMapper;

    @Test
    void unBodyCompletoSeAceptaCon202YSeProcesa() throws Exception {
        mockMvc.perform(post("/api/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_COMPLETO))
                .andExpect(status().isAccepted());

        verify(notificadorService).procesarSolicitudDeNotificacion(any());
    }

    @Test
    void unBodySinAsuntoNiCuerpoSeRechazaCon400YNoLlegaAlServicio() throws Exception {
        String bodyIncompleto = """
                {
                  "medioDeContacto": "email",
                  "direccionDeContacto": "ana@test.com"
                }
                """;

        mockMvc.perform(post("/api/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyIncompleto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasItems(
                        "asuntoMensaje: es obligatorio",
                        "cuerpoMensaje: es obligatorio")));

        verifyNoInteractions(notificadorService);
    }

    @Test
    void unCampoEnBlancoSeRechazaIgualQueUnoAusente() throws Exception {
        String bodyConBlanco = """
                {
                  "medioDeContacto": "   ",
                  "direccionDeContacto": "ana@test.com",
                  "asuntoMensaje": "Asunto",
                  "cuerpoMensaje": "Cuerpo"
                }
                """;

        mockMvc.perform(post("/api/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConBlanco))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasItem("medioDeContacto: es obligatorio")));

        verifyNoInteractions(notificadorService);
    }

    @Test
    void unaViolacionDeIntegridadDevuelve400YNo500() throws Exception {
        doThrow(new DataIntegrityViolationException("Column 'asunto' cannot be null"))
                .when(notificadorService).procesarSolicitudDeNotificacion(any());

        mockMvc.perform(post("/api/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_COMPLETO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La solicitud no cumple una restricción de datos"))
                .andExpect(content().string(not(containsString("asunto"))));
    }

    @Test
    void unErrorInesperadoDevuelve500ConCuerpoGenericoSinElDetalleInterno() throws Exception {
        doThrow(new RuntimeException("detalle interno que no debe salir en la respuesta"))
                .when(notificadorService).procesarSolicitudDeNotificacion(any());

        mockMvc.perform(post("/api/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_COMPLETO))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error interno en el servidor"))
                .andExpect(content().string(not(containsString("detalle interno"))));
    }

    /** Si alguien le saca {@code extends ResponseEntityExceptionHandler} al handler, estos dos fallan. */
    @Test
    void unaRutaInexistenteSigueDevolviendo404YNo500() throws Exception {
        mockMvc.perform(get("/ruta-que-no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unMetodoNoSoportadoSigueDevolviendo405YNo500() throws Exception {
        mockMvc.perform(delete("/api/notificaciones"))
                .andExpect(status().isMethodNotAllowed());
    }
}
