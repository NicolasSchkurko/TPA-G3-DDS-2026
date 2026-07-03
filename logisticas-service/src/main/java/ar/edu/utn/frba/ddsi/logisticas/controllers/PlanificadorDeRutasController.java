package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.services.PlanificadorDeRutasService;
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
@RequestMapping("/api/logistica/rutas")
public class PlanificadorDeRutasController {

  private final PlanificadorDeRutasService planificadorService;

  @Autowired
  public PlanificadorDeRutasController(PlanificadorDeRutasService planificadorService) {
    this.planificadorService = planificadorService;
  }

  /**
   * Endpoint: POST /api/logistica/rutas/callback
   * Este es el "Callback URL" que menciona la consigna.
   *
   * @param jsonAsignacion El JSON crudo recibido en el body.
   */
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
  @PostMapping("/planificar-manual")
  public ResponseEntity<String> forzarPlanificacionManual() {
    try {
      planificadorService.iniciarPlanificacionAutomatica();
      return ResponseEntity.ok("Proceso de planificación disparado. Aguardando respuesta del proveedor externo...");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al disparar la planificación.");
    }
  }
}