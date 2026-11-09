package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.dto.rutas.RutaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.*;

import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RutaService {
  private final RepositorioRutas repoRutas;
  private final RepositorioChoferes repoChoferes;
  private final RepositorioItemEntrega repoItemEntrega;
  private final GestorCamiones gestorCamiones;
  private final RepositorioCamiones repoCamiones;
  private final GestorPublicacionEventos gestorPublicacionEventos;

  public RutaService(RepositorioRutas repoRutas,
                     RepositorioChoferes repoChoferes,
                     RepositorioItemEntrega repoItemEntrega,
                     GestorCamiones gestorCamiones,
                     RepositorioCamiones repoCamiones,
                     GestorPublicacionEventos gestorPublicacionEventos) {
    this.repoRutas = repoRutas;
    this.repoChoferes = repoChoferes;
    this.repoItemEntrega = repoItemEntrega;
    this.gestorCamiones = gestorCamiones;
    this.repoCamiones = repoCamiones;
    this.gestorPublicacionEventos = gestorPublicacionEventos;
  }

  // --- MÉTODOS CRUD ---
  public Page<RutaDTO> findAll(Pageable pageable) {
      return repoRutas.findAll(pageable).map(ruta -> new RutaDTO().aplicar(ruta));
  }

  public RutaDTO findById(UUID idRuta) {
    Ruta ruta = repoRutas.findById(idRuta)
            .orElseThrow(() -> new RecursoNoEncontradoException("Ruta no encontrado"));
    return new RutaDTO().aplicar(ruta);
  }

  // --- MÉTODOS DE NEGOCIO ---

  /** Inicia la ruta del chofer: la pone EN_CURSO y avisa por el broker. */
  @Transactional
  public void iniciarRuta(UUID idChofer) {
    Ruta rutaActual = rutaDelChoferPorEstado(
            idChofer,
            EstadoRuta.PROGRAMADA
    );

    Ruta rutaConEvento = gestorPublicacionEventos.publicarInicioRuta(rutaActual);
    repoRutas.save(rutaConEvento);

    List<Parada> paradas = rutaConEvento.getParadas();

    for (Parada parada : paradas) {
      parada.getItems().forEach(repoItemEntrega::save);
    }
    rutaActual = obtenerRutaActualizada(rutaActual.getIdRuta());
    rutaActual.setEstado(EstadoRuta.EN_CURSO);
    repoRutas.save(rutaActual);
  }

  public Ruta obtenerRutaActualizada(UUID idRuta) {
    Ruta ruta = repoRutas.findById(idRuta)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                    "No existe la ruta con id " + idRuta
            ));

    List<UUID> idsDonaciones = ruta.obtenerTodosLosItems()
            .stream()
            .map(ItemEntrega::getIdDonacion)
            .distinct()
            .toList();

    Map<UUID, ItemEntrega> itemsActualizados =
            repoItemEntrega.findAllById(idsDonaciones).stream().collect(Collectors.toMap(
                    ItemEntrega::getIdDonacion,
                    Function.identity()
            ));

    for (Parada parada : ruta.getParadas()) {
      List<ItemEntrega> itemsActualizadosDeParada =
              parada.getItems().stream()
                      .map(ItemEntrega::getIdDonacion)
                      .map(itemsActualizados::get)
                      .filter(Objects::nonNull)
                      .collect(Collectors.toList());

      parada.setItems(itemsActualizadosDeParada);
    }

    return ruta;
  }

  @Transactional
  public void terminarRuta(UUID idChofer) {
    Ruta rutaActual = rutaDelChoferPorEstado(idChofer, EstadoRuta.EN_CURSO);
    rutaActual = obtenerRutaActualizada(rutaActual.getIdRuta());

    // 2. Procesamos el estado de los ítems
    for (Parada parada : rutaActual.getParadas()) {
      for (ItemEntrega item : parada.getItems()) {
        if (item.getEstado() != EstadoEntrega.ENTREGADA) {
          gestorPublicacionEventos.publicarReingresoDeposito(item);
        } else {
          // Si fue entregado y existe en la BD, se elimina correctamente
          if (repoItemEntrega.existsById(item.getIdDonacion())) {
            repoItemEntrega.deleteById(item.getIdDonacion());
          }
        }
      }
    }

    // 3. Liberar chofer
    Chofer chofer = repoChoferes.findById(idChofer).orElse(null);
    if (chofer == null && rutaActual.getCamionAsignado() != null) {
      chofer = rutaActual.getCamionAsignado().getChofer();
    }
    if (chofer != null) {
      chofer.disponible();
      repoChoferes.save(chofer);
    }

    // 4. Liberar y desvincular camión
    if (rutaActual.getCamionAsignado() != null) {
      String patente = rutaActual.getCamionAsignado().getPatente();
      Camion camionDeRuta = (patente != null)
              ? repoCamiones.findById(patente).orElse(rutaActual.getCamionAsignado())
              : rutaActual.getCamionAsignado();
      camionDeRuta.disponible();
      camionDeRuta.eliminarChofer(); // Desvinculamos el chofer del camión
      gestorCamiones.resetearCamion(camionDeRuta);
      repoCamiones.save(camionDeRuta);
    }

    rutaActual.setEstado(EstadoRuta.FINALIZADA);
    repoRutas.save(rutaActual);
  }

  /** La ruta del chofer filtrada por su estado actual. */
  private Ruta rutaDelChoferPorEstado(UUID idChofer, EstadoRuta estado) {
    Chofer chofer = repoChoferes.findById(idChofer)
            .orElseThrow(() -> new RecursoNoEncontradoException("Chofer no encontrado"));
    return repoRutas.findFirstByCamionAsignadoChoferIdChoferAndEstado(chofer.getIdChofer(), estado)
            .orElseThrow(() -> new IllegalStateException(
                    "No se encontró una ruta en estado " + estado + " para el chofer " + idChofer));
  }

}