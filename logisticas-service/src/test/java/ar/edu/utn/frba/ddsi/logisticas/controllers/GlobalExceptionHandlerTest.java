package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.services.CamionService;
import ar.edu.utn.frba.ddsi.logisticas.services.RutaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("El advice traduce cada excepción al código HTTP que corresponde")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock private CamionService camionService;
    @Mock private RutaService rutaService;

    @Test
    @DisplayName("RecursoNoEncontrado → 404")
    void noEncontradoEs404() {
        ResponseEntity<String> response =
                handler.manejarNoEncontrado(new RecursoNoEncontradoException("no está"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("no está");
    }

    @Test
    @DisplayName("IllegalArgumentException (validación) → 400, no 404")
    void validacionEs400() {
        ResponseEntity<String> response =
                handler.manejarSolicitudInvalida(new IllegalArgumentException("falta el campo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("falta el campo");
    }

    @Test
    @DisplayName("IllegalStateException (precondición/estado) → 409, no 500")
    void estadoInvalidoEs409() {
        ResponseEntity<String> response =
                handler.manejarEstadoInvalido(new IllegalStateException("no hay ruta en curso"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("no hay ruta en curso");
    }

    @Test
    @DisplayName("Clave duplicada → 409")
    void duplicadoEs409() {
        ResponseEntity<String> response =
                handler.manejarClaveDuplicada(new DuplicateKeyException("E11000"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Ya existe un registro");
    }

    @Test
    @DisplayName("GET de un camión inexistente devuelve 404")
    void camionInexistenteDevuelve404() throws Exception {
        when(camionService.findById("NOPE"))
                .thenThrow(new RecursoNoEncontradoException("Camión no encontrado"));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new CamionController(camionService))
                .setControllerAdvice(handler)
                .build();

        mvc.perform(get("/api/camiones/NOPE")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/camiones/{patente}/estado sin 'disponible' devuelve 400, no 404")
    void disponibilidadSinCampoDevuelve400() throws Exception {
        when(camionService.cambiarDisponibilidad(eq("AAA111"), anyMap()))
                .thenThrow(new IllegalArgumentException("El campo 'disponible' es obligatorio."));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new CamionController(camionService))
                .setControllerAdvice(handler)
                .build();

        mvc.perform(patch("/api/camiones/AAA111/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/rutas/chofer/{id}/terminar sin ruta activa devuelve 409, no 500")
    void terminarSinRutaActivaDevuelve409() throws Exception {
        UUID idChofer = UUID.randomUUID();
        doThrow(new IllegalStateException("No se encontró una ruta en estado EN_CURSO"))
                .when(rutaService).terminarRuta(idChofer);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RutaController(rutaService))
                .setControllerAdvice(handler)
                .build();

        mvc.perform(patch("/api/rutas/chofer/{id}/terminar", idChofer))
                .andExpect(status().isConflict());
    }
}
