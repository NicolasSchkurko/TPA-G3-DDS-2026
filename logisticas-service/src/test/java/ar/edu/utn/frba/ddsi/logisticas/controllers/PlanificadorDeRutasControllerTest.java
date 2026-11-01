package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.Scheduler.PlanificadorDeRutasScheduler;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.services.PlanificadorRutasService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanificadorDeRutasControllerTest {

    @Mock
    private PlanificadorDeRutasScheduler planificadorScheduler;

    @Mock
    private PlanificadorRutasService planificadorService;

    @InjectMocks
    private PlanificadorDeRutasController controller;

    private Ruta crearRutaMock(String patente) {
        return new Ruta(new Camion(patente, 10.0, 3.0, 1000.0, true));
    }

    // --- Tests para POST /api/PlanificacionRutas/callback ---

    @Test
    @DisplayName("Callback: cuando se asignan todas las rutas devuelve 200 OK con mensaje de éxito completo")
    void callbackTodasLasRutasAsignadasDevuelve200Exitoso() {
        Ruta r1 = crearRutaMock("AAA111");
        Ruta r2 = crearRutaMock("BBB222");

        List<Ruta> generadas = List.of(r1, r2);
        List<Ruta> asignadas = List.of(r1, r2);

        when(planificadorService.procesarCallbackRutas(anyString())).thenReturn(generadas);
        when(planificadorService.asignarChoferes(generadas)).thenReturn(asignadas);

        ResponseEntity<String> response = controller.recibirRutasPlanificadas("{\"dummy\": []}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Rutas procesadas y guardadas exitosamente en el sistema de logística.");
    }

    @Test
    @DisplayName("Callback: cuando hay menos choferes que rutas devuelve 200 OK con mensaje de asignación parcial")
    void callbackRutasParcialesPorFaltaDeChoferesDevuelve200ConMensajeAdvertencia() {
        Ruta r1 = crearRutaMock("AAA111");
        Ruta r2 = crearRutaMock("BBB222");

        List<Ruta> generadas = List.of(r1, r2);
        List<Ruta> asignadas = List.of(r1); // solo 1 chofer disponible

        when(planificadorService.procesarCallbackRutas(anyString())).thenReturn(generadas);
        when(planificadorService.asignarChoferes(generadas)).thenReturn(asignadas);

        ResponseEntity<String> response = controller.recibirRutasPlanificadas("{\"dummy\": []}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Lo sentimos, no pudimos asignar todas las rutas por falta de choferes");
    }

    @Test
    @DisplayName("Callback: cuando el dominio rechaza por validación de capacidad devuelve 422 Unprocessable Entity")
    void callbackErrorValidacionDominioDevuelve422() {
        when(planificadorService.procesarCallbackRutas(anyString()))
                .thenThrow(new IllegalStateException("El proveedor externo generó una ruta inválida que excede la capacidad"));

        ResponseEntity<String> response = controller.recibirRutasPlanificadas("{\"dummy\": []}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).contains("Error de validación del dominio: El proveedor externo generó una ruta inválida");
    }

    @Test
    @DisplayName("Callback: cuando los datos de entrada son inválidos o faltan ítems devuelve 400 Bad Request")
    void callbackDatosEntradaInvalidosDevuelve400() {
        when(planificadorService.procesarCallbackRutas(anyString()))
                .thenThrow(new IllegalArgumentException("Item pendiente no encontrado: 1234"));

        ResponseEntity<String> response = controller.recibirRutasPlanificadas("{\"dummy\": []}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Datos de entrada inválidos: Item pendiente no encontrado: 1234");
    }

    @Test
    @DisplayName("Callback: ante un error inesperado devuelve 500 Internal Server Error")
    void callbackErrorInesperadoDevuelve500() {
        when(planificadorService.procesarCallbackRutas(anyString()))
                .thenThrow(new RuntimeException("Fallo inesperado de conexión con MongoDB"));

        ResponseEntity<String> response = controller.recibirRutasPlanificadas("{\"dummy\": []}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("Error interno del servidor");
    }

    // --- Tests para POST /api/PlanificacionRutas/planificar-manual ---

    @Test
    @DisplayName("Planificación manual: ejecución exitosa devuelve 200 OK")
    void planificarManualExitosoDevuelve200() {
        doNothing().when(planificadorScheduler).iniciarPlanificacionAutomatica();

        ResponseEntity<String> response = controller.forzarPlanificacionManual();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Proceso de planificación disparado");
        verify(planificadorScheduler, times(1)).iniciarPlanificacionAutomatica();
    }

    @Test
    @DisplayName("Planificación manual: fallo del scheduler devuelve 500 Internal Server Error")
    void planificarManualFalloDevuelve500() {
        doThrow(new RuntimeException("Proveedor externo no responde"))
                .when(planificadorScheduler).iniciarPlanificacionAutomatica();

        ResponseEntity<String> response = controller.forzarPlanificacionManual();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("Error al intentar comunicar con el proveedor externo: Proveedor externo no responde");
    }
}
