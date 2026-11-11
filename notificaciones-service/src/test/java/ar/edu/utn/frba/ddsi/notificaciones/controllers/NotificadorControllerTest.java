package ar.edu.utn.frba.ddsi.notificaciones.controllers;

import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionNoReintentableException;
import ar.edu.utn.frba.ddsi.notificaciones.mappers.NotificacionMapper;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvio;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.services.NotificadorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El borde HTTP de {@code POST /notificaciones}: validación del body y cuerpo de error uniforme. */
@WebMvcTest(NotificadorController.class)
@Import(MedioDeEnvioFactory.class)
class NotificadorControllerTest {

    private static final String BODY_COMPLETO = """
            {
              "medioDeContacto": "email",
              "direccionDeContacto": "ana@test.com",
              "asuntoMensaje": "Asunto",
              "cuerpoMensaje": "Cuerpo"
            }
            """;

    @TestConfiguration
    static class MediosDeEnvioDePrueba {
        @Bean
        Map<String, MedioDeEnvio> mediosDeEnvio() {
            return Map.of(
                    "email", mock(MedioDeEnvio.class),
                    "telefono", mock(MedioDeEnvio.class),
                    "whatsapp", mock(MedioDeEnvio.class));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificadorService notificadorService;

    @MockBean
    private NotificacionMapper notificacionMapper;

    @Test
    void unBodyCompletoSeAceptaCon202YSeProcesa() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificadorService.procesarSolicitudDeNotificacion(any())).thenReturn(id);

        mockMvc.perform(post("/api/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_COMPLETO))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", endsWith("/api/notificaciones/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()));

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
    void reintentarUnaNotificacionPendienteDevuelve202() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/notificaciones/{id}/reintento", id))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(id.toString()));

        verify(notificadorService).reintentar(id);
    }

    @Test
    void reintentarUnaNotificacionInexistenteDevuelve404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "no existe"))
                .when(notificadorService).reintentar(any());

        mockMvc.perform(post("/api/notificaciones/{id}/reintento", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", containsString("no existe")));
    }

    @Test
    void reintentarUnaNotificacionYaEnviadaDevuelve409() throws Exception {
        doThrow(new NotificacionNoReintentableException(UUID.randomUUID()))
                .when(notificadorService).reintentar(any());

        mockMvc.perform(post("/api/notificaciones/{id}/reintento", UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("ya fue enviada o está en curso")));
    }

    @Test
    void unMedioDeContactoNoSoportadoSeRechazaCon400YNoLlegaAlServicio() throws Exception {
        String bodyConMedioInvalido = """
                {
                  "medioDeContacto": "telegram",
                  "direccionDeContacto": "ana@test.com",
                  "asuntoMensaje": "Asunto",
                  "cuerpoMensaje": "Cuerpo"
                }
                """;

        mockMvc.perform(post("/api/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConMedioInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("desconocido")));

        verifyNoInteractions(notificadorService);
    }

    @Test
    void unAliasDeUnMedioConocidoSeAceptaCon202() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificadorService.procesarSolicitudDeNotificacion(any())).thenReturn(id);

        String bodyConAlias = """
                {
                  "medioDeContacto": "MAIL",
                  "direccionDeContacto": "ana@test.com",
                  "asuntoMensaje": "Asunto",
                  "cuerpoMensaje": "Cuerpo"
                }
                """;

        mockMvc.perform(post("/api/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConAlias))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(id.toString()));

        verify(notificadorService).procesarSolicitudDeNotificacion(any());
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

    @Test
    void listaPaginadoYFiltraPorEstado() throws Exception {
        NotificacionDTO dto = new NotificacionDTO();
        dto.setId(UUID.randomUUID().toString());
        dto.setEstado("FALLIDA");
        when(notificadorService.listar(eq(EstadoNotificacion.FALLIDA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/api/notificaciones")
                        .param("estado", "FALLIDA")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(dto.getId()))
                .andExpect(jsonPath("$.content[0].estado").value("FALLIDA"))
                .andExpect(jsonPath("$.size").value(5));

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(notificadorService).listar(eq(EstadoNotificacion.FALLIDA), pagina.capture());
        assertEquals(5, pagina.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC,
                pagina.getValue().getSort().getOrderFor("fechaCreacion").getDirection());
    }

    @Test
    void listaSinEstadoLlamaAlServiceConNull() throws Exception {
        when(notificadorService.listar(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/notificaciones"))
                .andExpect(status().isOk());

        verify(notificadorService).listar(isNull(), any(Pageable.class));
    }

    @Test
    void unEstadoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/notificaciones").param("estado", "NO_EXISTE"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificadorService);
    }

    @Test
    void obtieneUnaNotificacionPorId() throws Exception {
        UUID id = UUID.randomUUID();
        Notificacion notificacion = new Notificacion("ana@test.com", "email",
                new Mensaje("Asunto", "Cuerpo"));
        NotificacionDTO dto = new NotificacionDTO();
        dto.setId(id.toString());
        when(notificadorService.obtenerPorId(id)).thenReturn(Optional.of(notificacion));
        when(notificacionMapper.notificacionDTO(notificacion)).thenReturn(dto);

        mockMvc.perform(get("/api/notificaciones/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        verify(notificadorService).obtenerPorId(id);
    }

    @Test
    void unaNotificacionInexistenteDevuelve404() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificadorService.obtenerPorId(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/notificaciones/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void unSortInvalidoDevuelve400() throws Exception {
        when(notificadorService.listar(any(), any(Pageable.class)))
                .thenThrow(new PropertyReferenceException(
                        "noExiste", TypeInformation.of(Notificacion.class), List.of()));

        mockMvc.perform(get("/api/notificaciones").param("sort", "noExiste"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("El orden solicitado no corresponde a un campo ordenable"));
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
