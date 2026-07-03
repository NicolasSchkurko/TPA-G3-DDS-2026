package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.ActualizacionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.BienDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.PeticionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EntregaService {

  private final RepositorioItemEntrega repositorioItemEntrega;
  private final EventoLogisticaService eventoService;

  public EntregaService(RepositorioItemEntrega repositorioItemEntrega, EventoLogisticaService eventoService) {
    this.repositorioItemEntrega = repositorioItemEntrega;
    this.eventoService = eventoService;
  }

  // --- MÉTODOS CRUD BÁSICOS ---
  public List<ItemEntrega> findAll() {
    return repositorioItemEntrega.findAll();
  }

  public ItemEntrega findById(UUID id) {
    ItemEntrega item = repositorioItemEntrega.findById(id);
    if (item == null) throw new IllegalArgumentException("Entrega no encontrada");
    return item;
  }

  public void delete(UUID id) {
    if (repositorioItemEntrega.findById(id) == null) {
      throw new IllegalArgumentException("Entrega no encontrada");
    }
    repositorioItemEntrega.deleteById(id);
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
        repositorioItemEntrega.save(nuevoItem);
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
    switch (unidadStr.toUpperCase()) {
      case "UNIDADES":
      case "UNIDAD":
        return UnidadDeMedida.UNIDADES;
      case "KILOGRAMOS":
      case "KG":
        return UnidadDeMedida.KILOGRAMOS;
      case "LITROS":
      case "L":
        return UnidadDeMedida.LITROS;
      default:
        throw new IllegalArgumentException("Unidad de medida no soportada: " + unidadStr);
    }
  }

  public void actualizarEstado(UUID idDonacion, ActualizacionEntregaDTO request) {
    ItemEntrega item = repositorioItemEntrega.findById(idDonacion);
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
        item.confirmarEntrega(request.getFotoUrl());
        eventoService.publicarEntregaConfirmada(item);
        break;

      case "NO_RECIBIDA":
        if (request.getJustificacion() == null || request.getJustificacion().trim().isEmpty()) {
          throw new IllegalArgumentException("Se requiere justificar el motivo por el cual falló la entrega.");
        }
        item.marcarNoRecibida();
        eventoService.publicarEntregaFallida(item, request.getJustificacion());
        break;

      case "PENDIENTE":
        item.reingresarADeposito();
        eventoService.publicarReingresoDeposito(item);
        break;

      default:
        throw new IllegalArgumentException("Estado no válido. Use ENTREGADA, NO_RECIBIDA o PENDIENTE.");
    }

    repositorioItemEntrega.save(item);
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