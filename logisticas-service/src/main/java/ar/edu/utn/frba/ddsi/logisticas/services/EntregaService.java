package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.*;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Ciudad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Pais;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Provincia;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.*;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.*;
// El merge movio estos dos a subpaquetes. El wildcard de arriba no los alcanza, asi que van
// explicitos: el servicio los escribe contra findByIdDonacion() y findByEstado(), que solo
// existen en las versiones de subpaquete.
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EntregaService {

  private final RepositorioItemEntrega repoItemEntrega;
  private final RepositorioRutas repoRutas;
  private final RepositorioEntidades repoEntidades;
  private final RepositorioDirecciones repoDirecciones;
  private final RepositorioCiudades repoCiudades;
  private final RepositorioProvincias repoProvincias;
  private final RepositorioPaises repoPaises;
  private final RepositorioUnidadesDeMedida repoUnidades;
  private final GestorPublicacionEventos gestorPublicacionEventos;

  public EntregaService(RepositorioItemEntrega repoItemEntrega,
                        RepositorioRutas repoRutas,
                        RepositorioEntidades repoEntidades,
                        RepositorioDirecciones repoDirecciones,
                        RepositorioCiudades repoCiudades,
                        RepositorioProvincias repoProvincias,
                        RepositorioPaises repoPaises,
                        RepositorioUnidadesDeMedida repoUnidades,
                        GestorPublicacionEventos gestorPublicacionEventos) {
      this.repoItemEntrega = repoItemEntrega;
      this.repoRutas = repoRutas;
      this.repoEntidades = repoEntidades;
      this.repoDirecciones = repoDirecciones;
      this.repoCiudades = repoCiudades;
      this.repoProvincias = repoProvincias;
      this.repoPaises = repoPaises;
      this.repoUnidades = repoUnidades;
      this.gestorPublicacionEventos = gestorPublicacionEventos;
  }

  // --- MÉTODOS CRUD BÁSICOS ---
  public BienesDTO findAll() {
      List<ItemEntrega> items = repoItemEntrega.findAll();
      return new BienesDTO(items.stream().map(ItemEntrega::getIdDonacion).toList() , convertirItemsADTO(items));
  }

  public ItemEntrega findById(UUID id) {
    return repoItemEntrega.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Entrega no encontrada"));
  }

  public void delete(UUID id) {
    Optional<ItemEntrega> item = repoItemEntrega.findById(id);
    if(item.isPresent()){
      repoItemEntrega.deleteById(id);
    }
    else {
      throw new IllegalArgumentException("Entrega no encontrado");
    }
  }

  // --- MÉTODOS DE NEGOCIO ---
  public void procesarPeticion(EntregaDTO request) {
    if (request == null) return;

    if (request.getDonacionResumen() == null) return;

    if (request.getEntidadBeneficiaria() == null) return;

    List<UUID> idsDonaciones = request.getDonacionResumen().getIdsDonaciones();
    if (idsDonaciones == null) return;

    List<BienDTO> bienes = request.getDonacionResumen().getBienes();
    if (bienes == null) return;

    if (bienes.size() != idsDonaciones.size()) return;

    Direccion direccionEntidad = this.convertirDireccionDTO(request.getEntidadBeneficiaria());

    Pais pais = direccionEntidad.getCiudad().getProvincia().getPais();
    Optional<Pais> paisExistente = repoPaises.findByNombre(pais.getNombre());
    if (paisExistente.isPresent()) {
      pais = paisExistente.get();
    } else {
      pais = repoPaises.save(pais);
    }
    direccionEntidad.getCiudad().getProvincia().setPais(pais);

    Provincia provincia = direccionEntidad.getCiudad().getProvincia();
    Optional<Provincia> provinciaExistente = repoProvincias.findByNombre(provincia.getNombre());
    if (provinciaExistente.isPresent()) {
      provincia = provinciaExistente.get();
    } else {
      provincia = repoProvincias.save(provincia);
    }
    direccionEntidad.getCiudad().setProvincia(provincia);

    Ciudad ciudad = direccionEntidad.getCiudad();
    Optional<Ciudad> ciudadExistente = repoCiudades.findByNombre(ciudad.getNombre());
    if (ciudadExistente.isPresent()) {
      ciudad = ciudadExistente.get();
    } else {
      ciudad = repoCiudades.save(ciudad);
    }
    direccionEntidad.setCiudad(ciudad);
    repoDirecciones.save(direccionEntidad);

    Entidad nuevaEntidad = new Entidad(request.getEntidadBeneficiaria().getIdEntidad(), direccionEntidad);
    repoEntidades.save(nuevaEntidad);

    for (int j = 0; j < bienes.size(); j++) {
      BienDTO bien = bienes.get(j);

      // Mapeo mediante el switch delegado al servicio
      UnidadDeMedida unidadDominio = mapearUnidadDeMedida(bien.getUnidadDeMedida());
      repoUnidades.save(unidadDominio);

      ItemEntrega nuevoItem = new ItemEntrega(
              request.getDonacionResumen().getIdsDonaciones().get(j),
              bien.getCantidad(),
              unidadDominio,
              nuevaEntidad
      );
      repoItemEntrega.saveAndFlush(nuevoItem);
    }
  }

  /**
   * Mapea el String recibido desde el DTO a la instancia estática correspondiente del dominio.
   */
  private UnidadDeMedida mapearUnidadDeMedida(String unidadStr) {
    if (unidadStr == null) {
      throw new IllegalArgumentException("La unidad de medida no puede ser nula");
    }
      return switch (unidadStr.toUpperCase()) {
          case "UNIDADES", "UNIDAD" -> UnidadDeMedida.UNIDADES;
          case "KILOGRAMOS", "KG" -> UnidadDeMedida.KILOGRAMOS;
          case "LITROS", "L" -> UnidadDeMedida.LITROS;
          default -> throw new IllegalArgumentException("Unidad de medida no soportada: " + unidadStr);
      };
  }

  public void actualizarEstado(UUID idDonacion, ActualizacionEntregaDTO request) {
    ItemEntrega item = repoItemEntrega.findById(idDonacion)
            .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada con el ID proporcionado"));

    if (request.getEstado() == null) {
      throw new IllegalArgumentException("El estado no puede ser nulo");
    }

    switch (request.getEstado().toUpperCase()) {
      case "ENTREGADA":
        if(comprobarExistencia(request.getFotoUrl())) {
          throw new IllegalArgumentException("Se requiere una foto para confirmar la entrega exitosa.");
        }
        repoItemEntrega.saveAndFlush(gestorPublicacionEventos.publicarEntregaConfirmada(item, repoRutas.findByIdDonacion(item.getIdDonacion())
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la ruta correspondiente a la donación " + idDonacion)), request.getFotoUrl()));
        break;

      case "NO_RECIBIDA":
        if(comprobarExistencia(request.getJustificacion())) {
          throw new IllegalArgumentException("Se requiere justificar el motivo por el cual falló la entrega.");
        }
        repoItemEntrega.saveAndFlush(gestorPublicacionEventos.publicarEntregaFallida(item, repoRutas.findByIdDonacion(item.getIdDonacion())
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la ruta correspondiente a la donación " + idDonacion)), request.getJustificacion()));
        break;

      case "PENDIENTE":
        // Reingreso a depósito tras revisión de una entrega NO_RECIBIDA.
        // reingresarADeposito() ya valida que solo se pueda hacer desde NO_RECIBIDA.
        repoItemEntrega.saveAndFlush(gestorPublicacionEventos.publicarReingresoDeposito(item));
        break;

      default:
        throw new IllegalArgumentException("Estado no válido. Use ENTREGADA, NO_RECIBIDA o PENDIENTE.");
    }

    repoItemEntrega.saveAndFlush(item);
  }

  /**
   * Ítems en estado NO_RECIBIDA, pendientes de revisión (reingreso a depósito
   * o replanificación). El control de quién puede llamar a este endpoint
   * es responsabilidad del front/capa de autorización, no de este servicio.
   */

  private boolean comprobarExistencia(String elemento){
    return (elemento == null || elemento.trim().isEmpty());
  }

  public BienesDTO obtenerEntregasNoRecibidas() {
    List<ItemEntrega> items = repoItemEntrega.findByEstado(EstadoEntrega.NO_RECIBIDA);
    return new BienesDTO(items.stream().map(ItemEntrega::getIdDonacion).toList() , convertirItemsADTO(items));
  }

  private List<BienDTO> convertirItemsADTO(List<ItemEntrega> items){
    return items.stream().map(this::convertirABienDTO).toList();
  }

  private BienDTO convertirABienDTO(ItemEntrega item){
    return new BienDTO(item.getCantidad(), item.getUnidad().getNombre(), item.getEstado().toString(), item.getFechaCambioEstado(), item.getFotoComprobante(), convertirADireccionDTO(item.getEntidadDestino()), convertirEventosADTO(item.getEventos()));
  }

  private Direccion convertirDireccionDTO(DireccionDTO dto) {
    if (dto == null) return null;
    return new Direccion(
        dto.getCalleUno(), dto.getCalleDos(), dto.getAltura(),
        dto.getPiso(), dto.getDepartamento(), dto.getCiudad(),
        dto.getProvincia(), dto.getPais()
    );
  }

  private DireccionDTO convertirADireccionDTO(Entidad entidad){
    return new DireccionDTO(entidad.getIdEntidadBeneficiaria(), entidad.getDireccionDestino().getCalle1(), entidad.getDireccionDestino().getCalle2(), entidad.getDireccionDestino().getAltura(), entidad.getDireccionDestino().getPiso(), entidad.getDireccionDestino().getDepartamento(), entidad.getDireccionDestino().getCiudad().getNombre(), entidad.getDireccionDestino().getCiudad().getProvincia().getNombre(), entidad.getDireccionDestino().getCiudad().getProvincia().getPais().getNombre());
  }

  private List<EventoLogisticaDTO> convertirEventosADTO(List<EventoLogistica> eventos){
    if(eventos != null){
      return eventos.stream().map(this::convertirAEventoDTO).toList();
    }
    return new ArrayList<>();
  }

  private EventoLogisticaDTO convertirAEventoDTO(EventoLogistica evento){
    return new EventoLogisticaDTO(evento.getIdEvento(), evento.getTipoEvento(), evento.getReferenciaId(), evento.getJustificacion(), evento.getPayloadJson());
  }
}