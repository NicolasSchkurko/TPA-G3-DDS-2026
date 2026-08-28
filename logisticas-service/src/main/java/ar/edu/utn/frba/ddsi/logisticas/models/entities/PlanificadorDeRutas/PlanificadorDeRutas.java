package ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExterno;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Setter
@Component
public class PlanificadorDeRutas {

  private static final int TAMANO_LOTE_MAXIMO = 100;

  // Se inyecta la dependencia del proveedor externo (puede ser un mock/simulador para el TP)
  private ProveedorRutasExterno proveedorExterno;

    /**
   * Toma las donaciones pendientes y las envía al proveedor externo en lotes.
   */
  public void iniciarPlanificacion(List<ItemEntrega> itemsPendientes, List<Camion> camionesDisponibles) {

    // Requerimiento: Procesar en lotes de máximo 100 donaciones
    for (int i = 0; i < itemsPendientes.size(); i += TAMANO_LOTE_MAXIMO) {
      int fin = Math.min(itemsPendientes.size(), i + TAMANO_LOTE_MAXIMO);
      List<ItemEntrega> lote = itemsPendientes.subList(i, fin);
      proveedorExterno.solicitarPlanificacion(lote, camionesDisponibles);
    }
  }

  /**
   * Paso 2: Ejecutado cuando el controlador HTTP recibe el POST en la URL de callback (/api/logistica/rutas/callback).
   * Reconstruye los objetos de dominio (Rutas, Paradas) a partir de la respuesta del proveedor.
   * @param itemsPorPatenteCamion Mapa que asocia la patente del camión con los IDs de las donaciones que debe llevar.
   * @param repositorioCamiones Lista/Repositorio de camiones para buscar las instancias.
   * @param repositorioItems Lista/Repositorio de ítems pendientes para buscar las instancias.
   */
  public List<Ruta> procesarCallbackRutas(
      Map<String, List<UUID>> itemsPorPatenteCamion,
      List<Camion> repositorioCamiones,
      List<ItemEntrega> repositorioItems) {

    List<Ruta> rutasGeneradas = new ArrayList<>();

    for (Map.Entry<String, List<UUID>> asignacion : itemsPorPatenteCamion.entrySet()) {
      String patente = asignacion.getKey();
      List<UUID> idsItemsAsignados = asignacion.getValue();

      // 1. Buscar el camión instanciado
      Camion camion = repositorioCamiones.stream()
                                         .filter(c -> c.getPatente().equals(patente))
                                         .findFirst()
                                         .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado con patente: " + patente));

      // 2. Instanciar la nueva Ruta
      Ruta nuevaRuta = new Ruta();
      nuevaRuta.setIdRuta(UUID.randomUUID());
      nuevaRuta.setCamionAsignado(camion);
      nuevaRuta.setFechaProgramada(LocalDate.now().plusDays(1)); // Se planifica para el día siguiente

      // 3. Vincular donaciones.
      // Al llamar a agregarEntrega, la clase Ruta agrupa automáticamente por Parada/Entidad.
      for (UUID idItem : idsItemsAsignados) {
        ItemEntrega item = repositorioItems.stream()
                                           .filter(i -> i.getIdDonacion().equals(idItem))
                                           .findFirst()
                                           .orElseThrow(() -> new IllegalArgumentException("Item pendiente no encontrado: " + idItem));

        nuevaRuta.agregarEntrega(item);
      }

      // 4. Validación de consistencia: El dominio es el responsable de verificar
      // que el proveedor externo no haya violado las reglas de negocio (capacidad).
      if (nuevaRuta.excedeCapacidadDelCamion()) {
        throw new IllegalStateException(
            "Error de Integración: El proveedor externo generó una ruta inválida que excede " +
                "la capacidad máxima (peso o volumen) del camión patente: " + patente
        );
      }

      // 5. Finalizar estado
      nuevaRuta.setEstado(EstadoRuta.PROGRAMADA);
      rutasGeneradas.add(nuevaRuta);
    }

    return rutasGeneradas;
  }
}