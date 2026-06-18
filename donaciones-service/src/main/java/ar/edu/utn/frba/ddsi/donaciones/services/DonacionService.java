package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DonacionService {

  private final RepositorioDonaciones repositorio;

  public DonacionService(RepositorioDonaciones repositorio) {
    this.repositorio = repositorio;
  }

  public List<DonacionDTO> obtenerTodas() {
    return repositorio.findAll().stream()
                      .map(this::toDTO)
                      .collect(Collectors.toList());
  }

  public Optional<DonacionDTO> obtenerPorId(UUID id) {
    return repositorio.findById(id).map(this::toDTO);
  }

  public DonacionDTO crearDonacion(Donacion donacion) {
    if(donacion.getEstado() == null) {
      donacion.actualizarEstado(Estado.EN_DEPOSITO, "Ingreso inicial a sistema");
    }
    Donacion guardada = repositorio.save(donacion);
    return this.toDTO(guardada);
  }

  public List<DonacionDTO> procesarFormulario(PersonaDonante donante, List<Bien> bienes, LocalDate fechaRealizacion) {
    Formulario formulario = new Formulario(donante, bienes, fechaRealizacion);
    List<DonacionDTO> donacionesCreadas = new ArrayList<>();

    for (Donacion donacionSegmentada : formulario.getDonaciones()) {
      if (donacionSegmentada.getEstado() == null) {
        donacionSegmentada.actualizarEstado(Estado.EN_DEPOSITO, "Ingreso por segmentación de formulario");
      }
      Donacion guardada = repositorio.save(donacionSegmentada);
      donacionesCreadas.add(this.toDTO(guardada));
    }

    return donacionesCreadas;
  }

  public DonacionDTO actualizarDonacion(UUID id, Donacion donacionActualizada) {
    Optional<Donacion> existente = repositorio.findById(id);
    if (existente.isPresent()) {
      donacionActualizada.setId(id);
      Donacion guardada = repositorio.save(donacionActualizada);
      return this.toDTO(guardada);
    }
    throw new RuntimeException("Donación no encontrada con ID: " + id);
  }

  public void eliminarDonacion(UUID id) {
    repositorio.deleteById(id);
  }

  public DonacionDTO cambiarEstado(UUID id, Estado nuevoEstado, String justificacion) {
    Optional<Donacion> donacionOpt = repositorio.findById(id);
    if (donacionOpt.isPresent()) {
      Donacion donacion = donacionOpt.get();
      donacion.actualizarEstado(nuevoEstado, justificacion);
      Donacion guardada = repositorio.save(donacion);
      return this.toDTO(guardada);
    }
    throw new RuntimeException("Donación no encontrada con ID: " + id);
  }

  private DonacionDTO toDTO(Donacion donacion) {
    if (donacion == null) {
      return null;
    }

    DonacionDTO dto = new DonacionDTO();

    dto.setDonanteName(donacion.getDonante() != null ? donacion.getDonante().darNombre() : "Desconocido");
    dto.setEntidadBeneficiaria(donacion.getEntidad() != null ? donacion.getEntidad().getRazonSocial() : "No asignada");

    dto.setDescripcion(donacion.getDescripcion());
    dto.setEstado(donacion.getEstado() != null ? donacion.getEstado().name() : "N/A");

    dto.setSubcategoriaName(donacion.getSubcategoria() != null ? donacion.getSubcategoria().getNombre() : "N/A");
    dto.setCategoriaBienName("Categoría Pendiente");

    dto.setFechaEntrega(donacion.getFechaEntrega());
    dto.setCantidadTotalBienes(donacion.sumaCantidadBienes());
    dto.setBienes(new ArrayList<>());

    return dto;
  }
}