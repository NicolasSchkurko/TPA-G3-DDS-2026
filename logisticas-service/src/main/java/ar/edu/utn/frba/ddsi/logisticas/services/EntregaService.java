package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.ActualizacionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.BienDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.PeticionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.Estado.*;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRutas;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EntregaService {

  private final GestorItemEntrega gestorItemEntrega;
  private final GestorRutas gestorRutas;
  private final EventoLogisticaService eventoService;

    public EntregaService(GestorItemEntrega gestorItemEntrega,  EventoLogisticaService eventoService, GestorRutas gestorRutas) {
      this.gestorItemEntrega = gestorItemEntrega;
      this.gestorRutas = gestorRutas;
      this.eventoService = eventoService;
    }

  // --- MÉTODOS CRUD BÁSICOS ---
  public List<ItemEntrega> findAll() {
    return gestorItemEntrega.listarItems();
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
      List<BienDTO> bienes = entregaActual.getDonacionResumen();
      if (bienes == null) continue;

      for (int j = 0; j < bienes.size(); j++) {
        BienDTO bien = bienes.get(j);
        Direccion direccionEntidad = this.convertirDireccionDTO(entregaActual.getEntidadBeneficiaria());

        // Mapeo mediante el switch delegado al servicio
        UnidadDeMedida unidadDominio = mapearUnidadDeMedida(bien.getUnidadDeMedida());

        ItemEntrega nuevoItem = new ItemEntrega(
            entregaActual.getIdsDonaciones().get(j),
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
        if (request.getFotoUrl() == null || request.getFotoUrl().trim().isEmpty()) {
          throw new IllegalArgumentException("Se requiere una foto para confirmar la entrega exitosa.");
        }
        eventoService.publicarEntregaConfirmada(item, gestorRutas.buscarRutaDeIdDonacion(item.getIdDonacion()), request.getFotoUrl());
        break;

      case "NO_RECIBIDA":
        if (request.getJustificacion() == null || request.getJustificacion().trim().isEmpty()) {
          throw new IllegalArgumentException("Se requiere justificar el motivo por el cual falló la entrega.");
        }
        eventoService.publicarEntregaFallida(item, gestorRutas.buscarRutaDeIdDonacion(item.getIdDonacion()), request.getJustificacion());
        break;

      case "PENDIENTE":
        // Reingreso a depósito tras revisión de una entrega NO_RECIBIDA.
        // reingresarADeposito() ya valida que solo se pueda hacer desde NO_RECIBIDA.
        eventoService.publicarReingresoDeposito(item);
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
  public List<ItemEntrega> obtenerEntregasNoRecibidas() {
    return gestorItemEntrega.buscarNoRecibidos();
  }

  private Direccion convertirDireccionDTO(DireccionDTO dto) {
    if (dto == null) return null;
    return new Direccion(
        dto.getCalleUno(), dto.getCalleDos(), dto.getAltura(),
        dto.getPiso(), dto.getDepartamento(), dto.getCiudad(),
        dto.getProvincia(), dto.getPais()
    );
  }
}