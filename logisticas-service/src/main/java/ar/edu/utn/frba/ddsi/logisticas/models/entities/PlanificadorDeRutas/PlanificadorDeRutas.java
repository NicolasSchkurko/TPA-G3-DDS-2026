package ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExterno;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Setter
@Component
public class PlanificadorDeRutas {

  private static final int TAMANO_LOTE_MAXIMO = 100;

  private ProveedorRutasExterno proveedorExterno;

  /**
   * Manda las donaciones pendientes al proveedor externo en lotes de
   * {@value #TAMANO_LOTE_MAXIMO}.
   */
  public void iniciarPlanificacion(List<ItemEntrega> itemsPendientes, List<Camion> camionesDisponibles) {

    for (int i = 0; i < itemsPendientes.size(); i += TAMANO_LOTE_MAXIMO) {
      int fin = Math.min(itemsPendientes.size(), i + TAMANO_LOTE_MAXIMO);
      List<ItemEntrega> lote = itemsPendientes.subList(i, fin);
      proveedorExterno.solicitarPlanificacion(lote, camionesDisponibles);
    }
  }

  /**
   * Reconstruye las rutas que devolvio el proveedor externo.
   *
   * @param itemsPorPatenteCamion patente del camion -> ids de las donaciones que debe llevar
   */
  public List<Ruta> procesarCallbackRutas(
      Map<String, List<UUID>> itemsPorPatenteCamion,
      List<Camion> repositorioCamiones,
      List<ItemEntrega> repositorioItems) {

    List<Ruta> rutasGeneradas = new ArrayList<>();

    for (Map.Entry<String, List<UUID>> asignacion : itemsPorPatenteCamion.entrySet()) {
      String patente = asignacion.getKey();
      List<UUID> idsItemsAsignados = asignacion.getValue();

      Camion camion = repositorioCamiones.stream()
                                         .filter(c -> c.getPatente().equals(patente))
                                         .findFirst()
                                         .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado con patente: " + patente));

      Ruta nuevaRuta = new Ruta(camion);

      // agregarEntrega agrupa solo, creando una parada por entidad destino.
      for (UUID idItem : idsItemsAsignados) {
        ItemEntrega item = repositorioItems.stream()
                                           .filter(i -> i.getIdDonacion().equals(idItem))
                                           .findFirst()
                                           .orElseThrow(() -> new IllegalArgumentException("Item pendiente no encontrado: " + idItem));

        nuevaRuta.agregarEntrega(item);
      }

      // El dominio es quien valida que el proveedor no haya violado las reglas de negocio.
      if (nuevaRuta.excedeCapacidadDelCamion()) {
        throw new IllegalStateException(
            "Error de Integración: El proveedor externo generó una ruta inválida que excede " +
                "la capacidad máxima (peso o volumen) del camión patente: " + patente
        );
      }

      nuevaRuta.setEstado(EstadoRuta.PROGRAMADA);
      rutasGeneradas.add(nuevaRuta);
    }
    return rutasGeneradas;
  }
}