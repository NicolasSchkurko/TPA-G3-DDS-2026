package ar.edu.utn.frba.ddsi.logisticas.Scheduler;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExterno;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;

import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRutas;
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
  private final GestorRutas gestorRutas;
  private final GestorChoferes gestorChoferes;
  private final PlanificadorDeRutas planificadorDominio;

  @Autowired
  public PlanificadorDeRutasScheduler(
      ProveedorRutasExterno proveedorExterno,
      GestorItemEntrega gestorItemEntrega,
      GestorCamiones gestorCamiones,
      GestorRutas gestorRutas,
      GestorChoferes gestorChoferes) {
      this.planificadorDominio = new PlanificadorDeRutas();
    this.planificadorDominio.setProveedorExterno(proveedorExterno);
    this.gestorItemEntrega = gestorItemEntrega;
    this.gestorCamiones = gestorCamiones;
    this.gestorRutas = gestorRutas;
    this.gestorChoferes = gestorChoferes;
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

  /**
   * Invocado por el Controller cuando llega el HTTP POST de callback desde el proveedor externo.
   */
  public List<Ruta> procesarCallbackRutas(String jsonAsignacion) {
    // 1. Transformar el JSON crudo a nuestro Map esperado usando Jackson
    ObjectMapper mapper = new ObjectMapper();
    Map<String, List<UUID>> asignacion;
    try {
      asignacion = mapper.readValue(jsonAsignacion, new TypeReference<Map<String, List<UUID>>>() {});
    } catch (Exception e) {
      throw new IllegalArgumentException("El formato del JSON recibido no es válido", e);
    }

    // 2. Extraer todos los IDs de donaciones del mapa
    List<UUID> todosLosIdsItems = asignacion.values().stream()
                                            .flatMap(List::stream)
                                            .collect(Collectors.toList());

    List<Camion> camionesDb;
    List<ItemEntrega> itemsDb;

    // 3. Traer de la Base de Datos los camiones y los items necesarios
    try {
      camionesDb = gestorCamiones.listarCamiones();
      itemsDb = gestorItemEntrega.buscarItems(todosLosIdsItems);
    } catch (Exception e) {
      throw new RuntimeException("Falla en la base de datos al recuperar información para el ruteo", e);
    }

    // 5. Pasar la responsabilidad al dominio para que instancie, agrupe en paradas y valide pesos
    List<Ruta> rutasGeneradas = planificadorDominio.procesarCallbackRutas(asignacion, camionesDb, itemsDb);

    // 6. Guardar el resultado final en la BD
    try {
      rutasGeneradas.forEach(ruta -> ruta.setFechaProgramada(LocalDate.now().plusDays(1)));
      gestorRutas.guardarRutas(rutasGeneradas);
      System.out.println("Se guardaron exitosamente " + rutasGeneradas.size() + " rutas nuevas.");
    } catch (Exception e) {
      throw new RuntimeException("Error al persistir las nuevas rutas en la base de datos", e);
    }

    return rutasGeneradas;
  }

  /**
   * FIX: La asignación ahora respeta la eliminación de la dependencia bidireccional.
   * Se asigna el chofer al camión, y se marcan como ocupados usando los métodos del Repositorio actualizados.
   */
  public List<Ruta> asignarChoferes(List<Ruta> rutas){
    List<Chofer> choferesDisponibles = new ArrayList<>(
        gestorChoferes.listarChoferes()
                           .stream()
                           .filter(Chofer::isDisponible)
                           .toList()
    );
    Random random = new Random();

    if(rutas.size() <= choferesDisponibles.size()){
      for (Ruta ruta : rutas) {
        Chofer choferElegido = choferesDisponibles.get(random.nextInt(choferesDisponibles.size()));

        Camion camion = ruta.getCamionAsignado();
        if (camion != null) {
          camion.setChofer(choferElegido);
          camion.ocupado(); // Bloqueamos el camión
          choferElegido.ocupado(); // Bloqueamos al chofer

          // Actualizamos estados
          gestorCamiones.guardarCamion(camion);
          gestorChoferes.guardarChofer(choferElegido);
        }

        gestorRutas.guardarRuta(ruta);
        choferesDisponibles.remove(choferElegido);
      }
      return rutas;
    } else {
      List<Ruta> rutasAsignadas = new ArrayList<>();
      for (Chofer chofer : choferesDisponibles) {
        Ruta rutaElegida = rutas.get(random.nextInt(rutas.size()));

        Camion camion = rutaElegida.getCamionAsignado();
        if (camion != null) {
          camion.setChofer(chofer);
          camion.ocupado();
          chofer.ocupado();

          gestorCamiones.guardarCamion(camion);
          gestorChoferes.guardarChofer(chofer);
        }

        gestorRutas.guardarRuta(rutaElegida);

        rutas.remove(rutaElegida);
        rutasAsignadas.add(rutaElegida);
      }
      return rutasAsignadas;
    }
  }
}