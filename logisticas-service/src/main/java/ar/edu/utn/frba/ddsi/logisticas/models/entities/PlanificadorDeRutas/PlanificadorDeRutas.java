package ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExterno;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Setter
@Component
public class PlanificadorDeRutas {

  private static final Logger log = LoggerFactory.getLogger(PlanificadorDeRutas.class);
  private static final int TAMANO_LOTE_MAXIMO = 100;

  private ProveedorRutasExterno proveedorExterno;

  public void iniciarPlanificacion(List<ItemEntrega> itemsPendientes, List<Camion> camionesDisponibles) {
    if (itemsPendientes == null || itemsPendientes.isEmpty() || camionesDisponibles == null || camionesDisponibles.isEmpty()) {
      return;
    }

    int cantidadLotes = (int) Math.ceil((double) itemsPendientes.size() / TAMANO_LOTE_MAXIMO);
    int camionesPorLote = Math.max(1, (int) Math.ceil((double) camionesDisponibles.size() / cantidadLotes));

    int indiceCamion = 0;
    for (int i = 0; i < itemsPendientes.size(); i += TAMANO_LOTE_MAXIMO) {
      if (indiceCamion >= camionesDisponibles.size()) {
        log.warn("No hay suficientes camiones disponibles para cubrir todos los lotes de pendientes hoy.");
        break;
      }

      int finItems = Math.min(itemsPendientes.size(), i + TAMANO_LOTE_MAXIMO);
      List<ItemEntrega> loteItems = itemsPendientes.subList(i, finItems);

      int finCamiones = Math.min(camionesDisponibles.size(), indiceCamion + camionesPorLote);
      List<Camion> loteCamiones = camionesDisponibles.subList(indiceCamion, finCamiones);

      proveedorExterno.solicitarPlanificacion(loteItems, loteCamiones);
      indiceCamion = finCamiones;
    }
  }

  public List<Ruta> procesarCallbackRutas(
      Map<String, List<UUID>> itemsPorPatenteCamion,
      List<Camion> repositorioCamiones,
      List<ItemEntrega> repositorioItems) {

    List<Ruta> rutasGeneradas = new ArrayList<>();

    Map<String, Camion> camionesPorPatente = repositorioCamiones.stream()
        .collect(Collectors.toMap(Camion::getPatente, c -> c));

    Map<UUID, ItemEntrega> itemsPorId = repositorioItems.stream()
        .collect(Collectors.toMap(ItemEntrega::getIdDonacion, i -> i));

    for (Map.Entry<String, List<UUID>> asignacion : itemsPorPatenteCamion.entrySet()) {
      String patente = asignacion.getKey();
      List<UUID> idsItemsAsignados = asignacion.getValue();

      Camion camion = camionesPorPatente.get(patente);
      if (camion == null) {
        throw new IllegalArgumentException("Camión no encontrado con patente: " + patente);
      }

      Ruta nuevaRuta = new Ruta(camion);

      // agregarEntrega agrupa solo, creando una parada por entidad destino.
      for (UUID idItem : idsItemsAsignados) {
        ItemEntrega item = itemsPorId.get(idItem);
        if (item == null) {
          throw new IllegalArgumentException("Item pendiente no encontrado: " + idItem);
        }

        nuevaRuta.agregarEntrega(item);
      }

      // El dominio es quien valida que el proveedor no haya violado las reglas de negocio.
      if (nuevaRuta.excedeCapacidadDelCamion()) {
        throw new IllegalStateException(
            "Error de Integración: El proveedor externo generó una ruta inválida que excede " +
                "la capacidad máxima (peso o volumen) del camión patente: " + patente
        );
      }

      // La ocupación real del camión sale de la ruta generada. El simulador la calcula
      // sobre copias descartables, así que sin esto la base queda siempre en 0.
      camion.setPesoOcupado(nuevaRuta.pesoTotalCargadoKg());
      camion.setVolumenOcupado(nuevaRuta.volumenTotalCargadoM3());

      nuevaRuta.setEstado(EstadoRuta.PROGRAMADA);
      rutasGeneradas.add(nuevaRuta);
    }
    return rutasGeneradas;
  }
}