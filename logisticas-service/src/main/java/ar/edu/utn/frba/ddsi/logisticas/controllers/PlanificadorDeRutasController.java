package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.Scheduler.PlanificadorDeRutasScheduler;
import ar.edu.utn.frba.ddsi.logisticas.services.PlanificadorRutasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST que expone los endpoints requeridos para el Servicio de Logística.
 */
@RestController
@RequestMapping("/PlanificacionRutas")
@Tag(name = "Planificador de Rutas", description = "API para la interacción con el proveedor externo de planificación de rutas logísticas")
public class PlanificadorDeRutasController {

  private final PlanificadorDeRutasScheduler planificadorScheduler;
  private final PlanificadorRutasService planificadorService;

  @Autowired
  public PlanificadorDeRutasController(PlanificadorDeRutasScheduler planificadorScheduler,
                                       PlanificadorRutasService planificadorService) {
    this.planificadorScheduler = planificadorScheduler;
    this.planificadorService = planificadorService;
  }

  /**
   * Endpoint: POST /api/logistica/rutas/callback
   * Este es el "Callback URL" que menciona la consigna.
   *
   * @param jsonAsignacion El JSON crudo recibido en el body.
   */
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
      // Pasamos el JSON crudo directamente. El servicio se encarga de parsearlo a Map.
      List<Ruta> rutasGeneradas = planificadorService.procesarCallbackRutas(jsonAsignacion);
      if (rutasGeneradas.equals(planificadorService.asignarChoferes(rutasGeneradas))){
        // Retornamos 200 OK al proveedor externo para avisar que recibimos bien los datos
        return ResponseEntity.ok("Rutas procesadas y guardadas exitosamente en el sistema de logística.");
      } else {
        return ResponseEntity.ok("Lo sentimos, no pudimos asignar todas las rutas por falta de choferes");
      }

    } catch (IllegalStateException e) {
      // Si el objeto de dominio PlanificadorDeRutas rechaza la ruta por capacidad excedida
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Error de validación del dominio: " + e.getMessage());

    } catch (IllegalArgumentException e) {
      // Si el proveedor envía un JSON malformado o IDs que no existen
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Datos de entrada inválidos: " + e.getMessage());

    } catch (Exception e) {
      // Cualquier otro error interno de infraestructura (ej. base de datos caída)
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor: " + e.getMessage());
    }
  }

  /**
   * Endpoint: POST /api/logistica/rutas/planificar-manual
   * Útil para forzar la ejecución manualmente en caso de que el proceso automático falle o se requiera adelantar trabajo.
   */
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
      System.err.println("=================================");
      System.err.println("ERROR EN LA SIMULACIÓN");
      System.err.println("Tipo: " + e.getClass().getName());
      System.err.println("Mensaje: " + e.getMessage());
      e.printStackTrace();
      System.err.println("=================================");
      return null;
    }
  }
}