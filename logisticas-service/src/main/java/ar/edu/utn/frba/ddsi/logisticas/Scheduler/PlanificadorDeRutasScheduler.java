package ar.edu.utn.frba.ddsi.logisticas.Scheduler;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExterno;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.camiones.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.items.RepositorioItemEntrega;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlanificadorDeRutasScheduler {

  private static final Logger log = LoggerFactory.getLogger(PlanificadorDeRutasScheduler.class);

  static final String ZonaPlanificacion = "America/Argentina/Buenos_Aires";
  private static final int TAMANO_LOTE_MAXIMO = 100;

  private final RepositorioItemEntrega repoItemEntrega;
  private final RepositorioCamiones repoCamiones;
  private final PlanificadorDeRutas planificadorDominio;

  @Autowired
  public PlanificadorDeRutasScheduler(
          ProveedorRutasExterno proveedorExterno,
          RepositorioItemEntrega repoItemEntrega,
          RepositorioCamiones repoCamiones) {
    this.planificadorDominio = new PlanificadorDeRutas();
    this.planificadorDominio.setProveedorExterno(proveedorExterno);
    this.repoItemEntrega = repoItemEntrega;
    this.repoCamiones = repoCamiones;
  }

  @Scheduled(cron = "0 0 2 * * ?", zone = ZonaPlanificacion)
  public void iniciarPlanificacionAutomatica() {
    List<ItemEntrega> itemsPendientes;
    List<Camion> camionesDisponibles;

    try {
      // Filtrar ítems en PENDIENTE que NO estén ya asignados a una ruta activa (PROGRAMADA o EN_CURSO)
      itemsPendientes = repoItemEntrega.findByEstado(EstadoEntrega.PENDIENTE).stream()
              .filter(item -> item.getParada() == null
                      || item.getParada().getRuta() == null
                      || item.getParada().getRuta().getEstado() == EstadoRuta.FINALIZADA)
              .collect(Collectors.toList());

      camionesDisponibles = repoCamiones.findAll().stream()
              .filter(Camion::getDisponible)
              .collect(Collectors.toList());

    } catch (Exception e) {
      log.error("No se pudo leer las donaciones pendientes ni los camiones, no se planifica hoy", e);
      return;
    }

    if (itemsPendientes.isEmpty()) {
      log.info("No hay donaciones pendientes para planificar hoy");
      return;
    }

    for (int inicio = 0; inicio < itemsPendientes.size(); inicio += TAMANO_LOTE_MAXIMO) {
      List<ItemEntrega> lote = itemsPendientes.subList(
              inicio, Math.min(inicio + TAMANO_LOTE_MAXIMO, itemsPendientes.size()));
      planificadorDominio.iniciarPlanificacion(lote, camionesDisponibles);
    }
  }
}