package ar.edu.utn.frba.ddsi.logisticas.Scheduler;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExterno;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
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
  private final RepositorioRutas repoRutas;
  private final PlanificadorDeRutas planificadorDominio;

  public PlanificadorDeRutasScheduler(
          ProveedorRutasExterno proveedorExterno,
          RepositorioItemEntrega repoItemEntrega,
          RepositorioCamiones repoCamiones) {
    this(proveedorExterno, repoItemEntrega, repoCamiones, null);
  }

  @Autowired
  public PlanificadorDeRutasScheduler(
          ProveedorRutasExterno proveedorExterno,
          RepositorioItemEntrega repoItemEntrega,
          RepositorioCamiones repoCamiones,
          RepositorioRutas repoRutas) {
    this.planificadorDominio = new PlanificadorDeRutas();
    this.planificadorDominio.setProveedorExterno(proveedorExterno);
    this.repoItemEntrega = repoItemEntrega;
    this.repoCamiones = repoCamiones;
    this.repoRutas = repoRutas;
  }

  @Scheduled(cron = "0 0 2 * * ?", zone = ZonaPlanificacion)
  public void iniciarPlanificacionAutomatica() {
    List<ItemEntrega> itemsPendientes;
    List<Camion> camionesDisponibles;

    try {
      Set<UUID> idsItemsEnRutasActivas = (repoRutas != null)
              ? repoRutas.findAll().stream()
                  .filter(ruta -> ruta.getEstado() != null && ruta.getEstado() != EstadoRuta.FINALIZADA)
                  .flatMap(ruta -> ruta.obtenerTodosLosItems().stream())
                  .map(ItemEntrega::getIdDonacion)
                  .filter(Objects::nonNull)
                  .collect(Collectors.toSet())
              : Collections.emptySet();

      itemsPendientes = repoItemEntrega.findByEstado(EstadoEntrega.PENDIENTE).stream()
              .filter(item -> !idsItemsEnRutasActivas.contains(item.getIdDonacion()))
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

    planificadorDominio.iniciarPlanificacion(itemsPendientes, camionesDisponibles);
  }
}