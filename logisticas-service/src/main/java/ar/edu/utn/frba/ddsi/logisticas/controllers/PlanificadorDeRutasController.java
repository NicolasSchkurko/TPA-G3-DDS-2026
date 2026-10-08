package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.Scheduler.PlanificadorDeRutasScheduler;
import ar.edu.utn.frba.ddsi.logisticas.services.PlanificadorRutasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/PlanificacionRutas")
@Tag(name = "Planificador de Rutas", description = "API para la interacción con el proveedor externo de planificación de rutas logísticas")
public class PlanificadorDeRutasController {

  private static final Logger log = LoggerFactory.getLogger(PlanificadorDeRutasController.class);

  private final PlanificadorDeRutasScheduler planificadorScheduler;
  private final PlanificadorRutasService planificadorService;

  @Autowired
  public PlanificadorDeRutasController(PlanificadorDeRutasScheduler planificadorScheduler,
                                       PlanificadorRutasService planificadorService) {
    this.planificadorScheduler = planificadorScheduler;
    this.planificadorService = planificadorService;
  }

  @Operation(summary = "Recibir rutas planificadas (Webhook/Callback)",
      description = "Punto de entrada para que el proveedor externo devuelva las rutas optimizadas. El sistema las procesa y asigna los camiones y choferes disponibles.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Rutas procesadas y guardadas exitosamente / Falta de choferes manejada correctamente"),
      @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (JSON malformado o IDs inexistentes)"),
      @ApiResponse(responseCode = "422", description = "Error de validación de negocio (ej: ruta rechazada por capacidad excedida)"),
      @ApiResponse(responseCode = "500", description = "Error interno del servidor")
  })
  @PostMapping("/callback")
  public ResponseEntity<String> recibirRutasPlanificadas(@RequestBody String jsonAsignacion) {
    try {
      List<Ruta> rutasGeneradas = planificadorService.procesarCallbackRutas(jsonAsignacion);
      if (rutasGeneradas.equals(planificadorService.asignarChoferes(rutasGeneradas))){
        return ResponseEntity.ok("Rutas procesadas y guardadas exitosamente en el sistema de logística.");
      } else {
        return ResponseEntity.ok("Lo sentimos, no pudimos asignar todas las rutas por falta de choferes");
      }

    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Error de validación del dominio: " + e.getMessage());

    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Datos de entrada inválidos: " + e.getMessage());

    } catch (Exception e) {
      log.error("Error al procesar callback de rutas", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Error interno del servidor. Por favor consulte los logs del sistema.");
    }
  }

  @Operation(summary = "Disparar planificación manual",
      description = "Fuerza la ejecución del algoritmo de planificación enviando las donaciones pendientes al proveedor externo de manera inmediata.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Proceso de planificación disparado con éxito"),
      @ApiResponse(responseCode = "500", description = "Error al intentar comunicar con el proveedor externo")
  })
  @PostMapping("/planificar-manual")
  public ResponseEntity<String> forzarPlanificacionManual() {
    try {
      planificadorScheduler.iniciarPlanificacionAutomatica();
      return ResponseEntity.ok("Proceso de planificación disparado. Aguardando respuesta del proveedor externo...");
    } catch (Exception e) {
      log.error("La planificación manual falló al dispararse", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Error al intentar comunicar con el proveedor externo: " + e.getMessage());
    }
  }
}