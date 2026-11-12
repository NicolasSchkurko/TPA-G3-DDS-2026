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
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorPublicacionEventos;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RutaService {
  private final RepositorioRutas repoRutas;
  private final RepositorioChoferes repoChoferes;
  private final GestorCamiones gestorCamiones;
  private final RepositorioCamiones repoCamiones;
  private final GestorPublicacionEventos gestorPublicacionEventos;
  private final GestorRutas gestorRutas;

  public RutaService(RepositorioRutas repoRutas,
                     RepositorioChoferes repoChoferes,
                     GestorCamiones gestorCamiones,
                     RepositorioCamiones repoCamiones,
                     GestorPublicacionEventos gestorPublicacionEventos,
                     GestorRutas gestorRutas) {
    this.repoRutas = repoRutas;
    this.repoChoferes = repoChoferes;
    this.gestorCamiones = gestorCamiones;
    this.repoCamiones = repoCamiones;
    this.gestorPublicacionEventos = gestorPublicacionEventos;
    this.gestorRutas = gestorRutas;
  }

  public Page<RutaDTO> findAll(Pageable pageable) {
      return repoRutas.findAll(pageable).map(ruta -> new RutaDTO().aplicar(ruta));
  }

  public RutaDTO findById(UUID idRuta) {
    Ruta ruta = repoRutas.findById(idRuta)
            .orElseThrow(() -> new RecursoNoEncontradoException("Ruta no encontrado"));
    return new RutaDTO().aplicar(ruta);
  }

  @Transactional
  public void iniciarRuta(UUID idChofer) {
    Ruta rutaActual = rutaDelChoferPorEstado(idChofer, EstadoRuta.PROGRAMADA);
    rutaActual = gestorRutas.obtenerRutaActualizada(rutaActual.getIdRuta());

    Ruta rutaConEvento = gestorPublicacionEventos.publicarInicioRuta(rutaActual);
    repoRutas.save(rutaConEvento);

    gestorRutas.guardarItemsDeRuta(rutaConEvento);

    rutaActual = gestorRutas.obtenerRutaActualizada(rutaActual.getIdRuta());
    rutaActual.setEstado(EstadoRuta.EN_CURSO);
    repoRutas.save(rutaActual);
  }

  @Transactional
  public void terminarRuta(UUID idChofer) {
    Ruta rutaActual = rutaDelChoferPorEstado(idChofer, EstadoRuta.EN_CURSO);
    rutaActual = gestorRutas.obtenerRutaActualizada(rutaActual.getIdRuta());

    for (Parada parada : rutaActual.getParadas()) {
      for (ItemEntrega item : parada.getItems()) {
        if (item.getEstado() != EstadoEntrega.ENTREGADA) {
          gestorPublicacionEventos.publicarReingresoDeposito(item);
        } else {
          gestorRutas.eliminarItemSiExiste(item.getIdDonacion());
        }
      }
    }

    Chofer chofer = repoChoferes.findById(idChofer).orElse(null);
    if (chofer == null && rutaActual.getCamionAsignado() != null) {
      chofer = rutaActual.getCamionAsignado().getChofer();
    }
    if (chofer != null) {
      chofer.disponible();
      repoChoferes.save(chofer);
    }

    if (rutaActual.getCamionAsignado() != null) {
      String patente = rutaActual.getCamionAsignado().getPatente();
      Camion camionDeRuta = (patente != null)
              ? repoCamiones.findById(patente).orElse(rutaActual.getCamionAsignado())
              : rutaActual.getCamionAsignado();
      gestorCamiones.resetearCamion(camionDeRuta);
      repoCamiones.save(camionDeRuta);
    }

    rutaActual.setEstado(EstadoRuta.FINALIZADA);
    repoRutas.save(rutaActual);
  }

  private Ruta rutaDelChoferPorEstado(UUID idChofer, EstadoRuta estado) {
    Chofer chofer = repoChoferes.findById(idChofer)
            .orElseThrow(() -> new RecursoNoEncontradoException("Chofer no encontrado"));
    return repoRutas.findFirstByCamionAsignadoChoferIdChoferAndEstado(chofer.getIdChofer(), estado)
            .orElseThrow(() -> new IllegalStateException(
                    "No se encontró una ruta en estado " + estado + " para el chofer " + idChofer));
  }
}
