package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.IncentivosDonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.DonacionFacade;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.SegmentadorDonaciones.SegmentadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.GestorNotificacionesEventos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioFormularios;
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
  private final RepositorioFormularios repositorioFormularios;
  private final RepositorioEntidadesBeneficiarias repositorioEntidades;
  private final IncentivosClient incentivosClient;
  private final GestorNotificacionesEventos gestorNotificaciones;

  public DonacionService(RepositorioDonaciones repositorio,
                         RepositorioFormularios repositorioFormularios,
                         IncentivosClient incentivosClient,
                         RepositorioEntidadesBeneficiarias repositorioEntidades) {
  public DonacionService(
          RepositorioDonaciones repositorio,
          IncentivosClient incentivosClient,
          GestorNotificacionesEventos gestorNotificaciones
  ) {
    this.repositorio = repositorio;
    this.repositorioFormularios = repositorioFormularios;
    this.incentivosClient = incentivosClient;
    this.repositorioEntidades = repositorioEntidades;
    this.gestorNotificaciones = gestorNotificaciones;
  }

  public List<DonacionDTO> obtenerTodas() {
    return repositorio.findAll().stream()
                      .map(this::toDTO)
                      .collect(Collectors.toList());
  }

  public Optional<DonacionDTO> obtenerPorId(UUID id) {
    return repositorio.findById(id).map(this::toDTO);
  }

  public List<Donacion> procesarFormulario(PersonaDonante donante, List<Bien> bienes, LocalDate fechaRealizacion) {
    Formulario formulario = new Formulario(donante, bienes,  fechaRealizacion);
    repositorioFormularios.save(formulario);

    DonacionFacade donacionFacade = new DonacionFacade(
            new SegmentadorDonaciones(),
            new AsignadorDonaciones()
    );

    List<Donacion> donacionesProcesadas = donacionFacade.crearDonaciones(formulario); //ejecuto segmentacion
    repositorio.saveFormulario(donacionesProcesadas);

    return donacionesProcesadas;
  }

  public void asignarDonaciones() {
    List<Donacion> donacionesNoAsignadas = repositorio.findPendient();
    List<EntidadBeneficiaria> entidades = repositorioEntidades.findAll();

    DonacionFacade donacionFacade = new DonacionFacade(
            new SegmentadorDonaciones(),
            new AsignadorDonaciones()
    );

    donacionFacade.ejecutarAsignador(donacionesNoAsignadas, entidades);
  }

  public Donacion actualizarDonacion(UUID id, DonacionDTO actualizacion) {
    Optional<Donacion> existente = repositorio.findById(id);
    if (existente.isPresent()) {
      return repositorio.actualizar(existente.get().getId(), actualizacion);
    }
    throw new RuntimeException("Donación no encontrada con ID: " + id);
  }

  public void eliminarDonacion(UUID id) {
    repositorio.deleteById(id);
  }

  public Donacion cambiarEstado(UUID id, Estado nuevoEstado, String justificacion) {
    Optional<Donacion> donacionOpt = repositorio.findById(id);
    if (donacionOpt.isPresent()) {
      Donacion donacion = donacionOpt.get();
      donacion.actualizarEstado(nuevoEstado, justificacion);
      repositorio.save(donacion);

      IncentivosDonacionDTO dto = new IncentivosDonacionDTO();
      dto.setFechaEntrega(donacion.getFechaEntrega());
      dto.setCantidadBienes(donacion.sumaCantidadBienes());
      dto.setSubCategoria(donacion.getSubcategoria().getNombre());
      dto.setCategoria(donacion.getSubcategoria().getCategoria().getNombre());
      dto.setEntidadBeneficiaria(donacion.getEntidad().getRazonSocial());
      dto.setEstado(nuevoEstado.name());
      incentivosClient.notificarDonacionAsignada(donacion.getDonante().getId(), dto);
      if (nuevoEstado == Estado.ASIGNADO) {
        IncentivosDonacionDTO dto = new IncentivosDonacionDTO();
        dto.setIdUsuario(donacion.getDonante().getId());
        dto.setFechaEntrega(donacion.getFechaEntrega());
        dto.setCantidadBienes(donacion.sumaCantidadBienes());
        dto.setSubCategoria(donacion.getSubcategoria().getNombre());
        dto.setCategoria(donacion.getSubcategoria().getCategoria().getNombre());
        dto.setEntidadBeneficiaria(donacion.getEntidad().getRazonSocial());
        dto.setEstado(nuevoEstado.name());
        incentivosClient.notificarDonacionAsignada(dto);
        gestorNotificaciones.notificarDonacionAsignadaAEntidadBeneficiaria(donacion);
      }

      return donacion;
    }

    throw new RuntimeException("Donación no encontrada con ID: " + id);
  }

  public DonacionDTO toDTO(Donacion donacion) {
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
