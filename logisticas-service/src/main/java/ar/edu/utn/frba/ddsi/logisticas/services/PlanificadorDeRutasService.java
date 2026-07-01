package ar.edu.utn.frba.ddsi.logisticas.services;


import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExterno;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


// Interfases de repositorio (mocks locales para ilustrar la dependencia con la base de datos).
interface CamionRepository {
  List<Camion> findAll();
}
interface ItemEntregaRepository {
  List<ItemEntrega> findAllById(List<UUID> ids);
  List<ItemEntrega> findByEstado(EstadoEntrega estado);
}
interface RutaRepository {
  void saveAll(List<Ruta> rutas);
}

@Service
public class PlanificadorDeRutasService {

  private final ProveedorRutasExterno proveedorExterno;
  private final CamionRepository camionRepository;
  private final ItemEntregaRepository itemRepository;
  private final RutaRepository rutaRepository;
  private PlanificadorDeRutas planificadorDominio;

  @Autowired
  public PlanificadorDeRutasService(
      ProveedorRutasExterno proveedorExterno,
      CamionRepository camionRepository,
      ItemEntregaRepository itemRepository,
      RutaRepository rutaRepository) {
    this.proveedorExterno = proveedorExterno;
    this.camionRepository = camionRepository;
    this.itemRepository = itemRepository;
    this.rutaRepository = rutaRepository;
    this.planificadorDominio = new PlanificadorDeRutas();
    this.planificadorDominio.setProveedorExterno(proveedorExterno);
  }

  @Scheduled(cron = "0 0 2 * * ?")
  public void iniciarPlanificacionAutomatica() {
    System.out.println("Iniciando proceso automático de planificación de rutas...");

    List<ItemEntrega> itemsPendientes;
    List<Camion> camionesDisponibles;

    try {
      itemsPendientes = itemRepository.findByEstado(EstadoEntrega.PENDIENTE);
      camionesDisponibles = camionRepository.findAll();
    } catch (Exception e) {
      System.err.println("Error de lectura en la base de datos: " + e.getMessage());
      return;
    }

    if (itemsPendientes.isEmpty()) {
      System.out.println("No hay donaciones pendientes para planificar hoy.");
      return;
    }
    planificadorDominio.iniciarPlanificacion(itemsPendientes, camionesDisponibles);
  }

  /**
   * Invocado por el Controller cuando llega el HTTP POST de callback desde el proveedor externo.
   * Ahora recibe un String crudo (JSON) y lo transforma acá dentro.
   */
  public List<Ruta> procesarCallbackRutas(String jsonAsignacion) {

    // 1. Transformar el JSON crudo a nuestro Map esperado usando Jackson
    ObjectMapper mapper = new ObjectMapper();
    Map<String, List<UUID>> asignacion;
    try {
      // El TypeReference le dice a Jackson exactamente en qué tipos de colecciones debe instanciarlo
      asignacion = mapper.readValue(jsonAsignacion, new TypeReference<Map<String, List<UUID>>>() {});
    } catch (Exception e) {
      // Si el JSON está malformado, lanzamos una excepción para que el Controller devuelva un Bad Request
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
      camionesDb = camionRepository.findAll();
      itemsDb = itemRepository.findAllById(todosLosIdsItems);
    } catch (Exception e) {
      throw new RuntimeException("Falla en la base de datos al recuperar información para el ruteo", e);
    }


    // 5. Pasar la responsabilidad al dominio para que instancie, agrupe en paradas y valide pesos
    List<Ruta> rutasGeneradas = planificadorDominio.procesarCallbackRutas(asignacion, camionesDb, itemsDb);

    // 6. Guardar el resultado final en la BD
    try {
      rutaRepository.saveAll(rutasGeneradas);
      System.out.println("Se guardaron exitosamente " + rutasGeneradas.size() + " rutas nuevas.");
    } catch (Exception e) {
      throw new RuntimeException("Error al persistir las nuevas rutas en la base de datos", e);
    }

    return rutasGeneradas;
  }
}
