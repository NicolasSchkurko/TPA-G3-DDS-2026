package ar.edu.utn.frba.ddsi.logisticas.Scheduler;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExterno;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;

import ar.edu.utn.frba.ddsi.logisticas.models.gestores.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlanificadorDeRutasScheduler {

  private final GestorItemEntrega gestorItemEntrega;
  private final GestorCamiones gestorCamiones;
  private final PlanificadorDeRutas planificadorDominio;

    @Autowired
  public PlanificadorDeRutasScheduler(
      ProveedorRutasExterno proveedorExterno,
      GestorItemEntrega gestorItemEntrega,
      GestorCamiones gestorCamiones) {
      this.planificadorDominio = new PlanificadorDeRutas();
    this.planificadorDominio.setProveedorExterno(proveedorExterno);
    this.gestorItemEntrega = gestorItemEntrega;
    this.gestorCamiones = gestorCamiones;
    }

  @Scheduled(cron = "0 0 2 * * ?")
  public void iniciarPlanificacionAutomatica() {
    System.out.println("Iniciando proceso automático de planificación de rutas...");

    List<ItemEntrega> itemsPendientes;
    List<Camion> camionesDisponibles;

    try {
      itemsPendientes = gestorItemEntrega.buscarPendientes();
      camionesDisponibles = gestorCamiones.listarCamiones().stream()
                                               .filter(Camion::getDisponible)
                                               .collect(Collectors.toList());

    } catch (Exception e) {
      System.err.println("Error de lectura en la base de datos: " + e.getMessage());
      return;
    }

    if (itemsPendientes.isEmpty()) {
      System.out.println("No hay donaciones pendientes para planificar hoy.");
      return;
    }

    // FIX ENTREGA 3: Restricción del proveedor externo a lotes de 100 como máximo
    for (int i = 0; i < itemsPendientes.size(); i += 100) {
      List<ItemEntrega> lote = itemsPendientes.subList(i, Math.min(i + 100, itemsPendientes.size()));
      planificadorDominio.iniciarPlanificacion(lote, camionesDisponibles);
    }
  }
}