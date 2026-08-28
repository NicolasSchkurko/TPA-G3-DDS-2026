package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.*;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorEventos;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRutas;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EntregaService {

  private final GestorItemEntrega gestorItemEntrega;
  private final GestorRutas gestorRutas;
  private final GestorEventos gestorEventos;

    public EntregaService(GestorItemEntrega gestorItemEntrega,  GestorEventos gestorEventos, GestorRutas gestorRutas) {
      this.gestorItemEntrega = gestorItemEntrega;
      this.gestorRutas = gestorRutas;
      this.gestorEventos = gestorEventos;
    }

  // --- MÉTODOS CRUD BÁSICOS ---
  public BienesDTO findAll() {
      List<ItemEntrega> items = gestorItemEntrega.listarItems();
      return new BienesDTO(items.stream().map(ItemEntrega::getIdDonacion).toList() , convertirItemsADTO(items));
  }

  public ItemEntrega findById(UUID id) {
    return gestorItemEntrega.buscarItem(id);
  }

  public void delete(UUID id) {
    gestorItemEntrega.eliminarItem(id);
  }

  // --- MÉTODOS DE NEGOCIO ---
  public void procesarPeticion(PeticionEntregaDTO request) {
    if (request == null || request.getEntregas() == null) return;

    List<EntregaDTO> entregas = request.getEntregas();
    for (EntregaDTO entregaActual : entregas) {
      List<BienDTO> bienes = entregaActual.getDonacionResumen().getBienes();
      if (bienes == null) continue;

      for (int j = 0; j < bienes.size(); j++) {
        BienDTO bien = bienes.get(j);
        Direccion direccionEntidad = this.convertirDireccionDTO(entregaActual.getEntidadBeneficiaria());

        // Mapeo mediante el switch delegado al servicio
        UnidadDeMedida unidadDominio = mapearUnidadDeMedida(bien.getUnidadDeMedida());

        ItemEntrega nuevoItem = new ItemEntrega(
            entregaActual.getDonacionResumen().getIdsDonaciones().get(j),
            bien.getCantidad(),
            unidadDominio,
            new Entidad(entregaActual.getEntidadBeneficiaria().getIdEntidad(), direccionEntidad)
        );
        gestorItemEntrega.guardarItem(nuevoItem);
      }
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
    ItemEntrega item = gestorItemEntrega.buscarItem(idDonacion);
    if (item == null) {
      throw new IllegalArgumentException("Donación no encontrada con el ID proporcionado");
    }

    if (request.getEstado() == null) {
      throw new IllegalArgumentException("El estado no puede ser nulo");
    }

    switch (request.getEstado().toUpperCase()) {
      case "ENTREGADA":
        if(comprobarExistencia(request.getFotoUrl())) {
          throw new IllegalArgumentException("Se requiere una foto para confirmar la entrega exitosa.");
        }
        gestorItemEntrega.guardarItem(gestorEventos.publicarEntregaConfirmada(item, gestorRutas.buscarRutaDeIdDonacion(item.getIdDonacion()), request.getFotoUrl()));
        break;

      case "NO_RECIBIDA":
        if(comprobarExistencia(request.getJustificacion())) {
          throw new IllegalArgumentException("Se requiere justificar el motivo por el cual falló la entrega.");
        }
        gestorItemEntrega.guardarItem(gestorEventos.publicarEntregaFallida(item, gestorRutas.buscarRutaDeIdDonacion(item.getIdDonacion()), request.getJustificacion()));
        break;

      case "PENDIENTE":
        // Reingreso a depósito tras revisión de una entrega NO_RECIBIDA.
        // reingresarADeposito() ya valida que solo se pueda hacer desde NO_RECIBIDA.
        gestorItemEntrega.guardarItem(gestorEventos.publicarReingresoDeposito(item));
        break;

      default:
        throw new IllegalArgumentException("Estado no válido. Use ENTREGADA, NO_RECIBIDA o PENDIENTE.");
    }

    gestorItemEntrega.guardarItem(item);
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
    List<ItemEntrega> items = gestorItemEntrega.buscarNoRecibidos();
    return new BienesDTO(items.stream().map(ItemEntrega::getIdDonacion).toList() , convertirItemsADTO(items));
  }

  private List<BienDTO> convertirItemsADTO(List<ItemEntrega> items){
    return items.stream().map(this::convertirABienDTO).toList();
  }

  //TODO Arreglar eventos
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
    return new EventoLogisticaDTO(evento.getId(), evento.getTipoEvento(), evento.getReferenciaId(), evento.getJustificacion(), evento.getPayloadJson());
  }
}