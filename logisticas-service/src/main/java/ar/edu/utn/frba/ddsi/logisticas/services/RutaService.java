package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienesDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.DireccionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.rutas.ParadaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.rutas.RutaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.rutas.RutasDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
  public RutasDTO findAll() {
      return convertirARutasDTO(repoRutas.findAll());
  }

  public RutaDTO findById(UUID idRuta) {
    return convertirARutaDTO(repoRutas.findById(idRuta)
            .orElseThrow(() -> new IllegalArgumentException("Ruta no encontrado")));
  }

  /*
  public Ruta create(Ruta ruta) {
      return gestorRutas.guardarRuta(ruta);
    }

  public Ruta update(UUID id, Ruta rutaActualizada) {
    return gestorRutas.actualizarRuta(id, rutaActualizada);
  }

  public void delete(UUID idRuta) {
    gestorRutas.eliminarRuta(idRuta);
  }
   */

  // --- MÉTODOS DE NEGOCIO ---

  public void iniciarRuta(UUID idChofer) {
    Ruta rutaActual = repoRutas.findByChofer(repoChoferes.findById(idChofer).orElseThrow(() -> new IllegalArgumentException("Chofer no encontrado")))
            .orElseThrow(() -> new IllegalStateException("No se encontró la ruta correspondiente al chofer " + idChofer));

    repoRutas.actualizarEstado(rutaActual, EstadoRuta.EN_CURSO);
    List<Parada> paradas = gestorPublicacionEventos.publicarInicioRuta(rutaActual).getParadas();
    for(Parada parada : paradas) {
        parada.getItems().forEach(repoItemEntrega::saveAndFlush);
    }
  }

  public void terminarRuta(UUID idChofer) {
    Ruta rutaActual = repoRutas.findByChofer(repoChoferes.findById(idChofer).orElseThrow(() -> new IllegalArgumentException("Chofer no encontrado")))
            .orElseThrow(() -> new IllegalStateException("No se encontró la ruta correspondiente al chofer " + idChofer));

    repoRutas.actualizarEstado(rutaActual, EstadoRuta.FINALIZADA);
    for(Parada parada : rutaActual.getParadas()){
      for(ItemEntrega item : parada.getItems()){
        if (item.getEstado() != EstadoEntrega.ENTREGADA) {
          gestorPublicacionEventos.publicarReingresoDeposito(item);
        } else {
          Optional<ItemEntrega> itemEncontrado = repoItemEntrega.findById(item.getIdDonacion());
          if(itemEncontrado.isPresent()){
            repoItemEntrega.deleteById(item.getIdDonacion());
            throw new IllegalArgumentException("Entrega no encontrada");
          }
        }
      }
    }
    Chofer chofer = rutaActual.getCamionAsignado().getChofer();
    chofer.disponible();
    repoChoferes.save(chofer);
    Camion camionDeRuta = rutaActual.getCamionAsignado();
    camionDeRuta.disponible();
    repoCamiones.save(camionDeRuta);

    Optional<Camion> camion = repoCamiones.findByChofer_IdChofer(idChofer);
    if (camion.isPresent()) {
      camion.get().eliminarChofer();
      gestorCamiones.resetearCamion(camion.get());
      throw new IllegalArgumentException("Camión no encontrado");
    }
  }

  private RutasDTO convertirARutasDTO(List<Ruta> rutas){
    return new RutasDTO(rutas.stream().map(this::convertirARutaDTO).toList());
  }

  private RutaDTO convertirARutaDTO(Ruta ruta){
    return new RutaDTO(ruta.getIdRuta(), convertirADTO(ruta.getCamionAsignado()), ruta.getFechaProgramada(), ruta.getEstado().toString(), ruta.getUrlSeguimiento(), convertirAParadasDTO(ruta.getParadas()));
  }

  private CamionDTO convertirADTO(Camion camion){
    if (camion == null) return null;
    CamionDTO dto = new CamionDTO();
    if (camion.getChofer() != null) {
      dto.setIdChofer(camion.getChofer().getIdChofer());
    }
    dto.setPatente(camion.getPatente());
    dto.setCapacidadVolumen(camion.getCapacidadVolumen());
    dto.setAltura(camion.getAltura());
    dto.setCapacidadCarga(camion.getCapacidadCarga());
    dto.setDisponible(camion.getDisponible());
    return dto;
  }

  private List<ParadaDTO> convertirAParadasDTO(List<Parada> paradas){
    return paradas.stream().map(this::convertirAParadaDTO).toList();
  }

  private ParadaDTO convertirAParadaDTO(Parada parada){
    return new ParadaDTO(convertirADireccionDTO(parada.getEntidadDestino()), new BienesDTO(obtenerIdDonaciones(parada.getItems()), convertirItemsADTO(parada.getItems())));
  }

  private DireccionDTO convertirADireccionDTO(Entidad entidad){
    return new DireccionDTO(entidad.getIdEntidadBeneficiaria(), entidad.getDireccionDestino().getCalle1(), entidad.getDireccionDestino().getCalle2(), entidad.getDireccionDestino().getAltura(), entidad.getDireccionDestino().getPiso(), entidad.getDireccionDestino().getDepartamento(), entidad.getDireccionDestino().getCiudad().getNombre(), entidad.getDireccionDestino().getCiudad().getProvincia().getNombre(), entidad.getDireccionDestino().getCiudad().getProvincia().getPais().getNombre());
  }

  private List<BienDTO> convertirItemsADTO(List<ItemEntrega> items){
    return items.stream().map(this::convertirABienDTO).toList();
  }

  private BienDTO convertirABienDTO(ItemEntrega item){
    return new BienDTO(item.getCantidad(), item.getUnidad().getNombre(), item.getEstado().toString(), item.getFechaCambioEstado(), item.getFotoComprobante(), convertirADireccionDTO(item.getEntidadDestino()), convertirEventosADTO(item.getEventos()));
  }

  private List<EventoLogisticaDTO> convertirEventosADTO(List<EventoLogistica> eventos){
    if(eventos != null){
      return eventos.stream().map(this::convertirAEventoDTO).toList();
    }
    return new ArrayList<>();
  }

  private EventoLogisticaDTO convertirAEventoDTO(EventoLogistica evento){
    return new EventoLogisticaDTO(evento.getId(), evento.getTipoEvento(), evento.getReferenciaId(), evento.getJustificacion(), evento.getPayloadJson());
  }

  private List<UUID> obtenerIdDonaciones(List<ItemEntrega> items){
    return items.stream().map(ItemEntrega::getIdDonacion).toList();
  }
}