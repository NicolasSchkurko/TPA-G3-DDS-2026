package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.BienResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.FormularioRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.*;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DonacionService {
  private final GestorDonantes gestorDonantes;
  private final GestorDonaciones gestorDonaciones;
  private final GestorAsignaciones gestorAsignaciones;
  private final GestorFormulario gestorFormulario;
  private final GestorMatchmaking gestorMatchmaking;
  private final GestorNecesidades gestorNecesidades;
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioDonantes repositorioDonantes;
  private final RepositorioFormularios repositorioFormularios;
  private final RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias;
  private final RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking;
  private final RepositorioBienes repositorioBienes;

  public DonacionService(GestorDonantes gestorDonantes, GestorDonaciones gestorDonaciones,
                         GestorAsignaciones gestorAsignaciones,
                         GestorFormulario gestorFormulario, GestorMatchmaking gestorMatchmaking,
                         GestorNecesidades gestorNecesidades,
                         RepositorioDonaciones repositorioDonaciones,
                         RepositorioDonantes repositorioDonantes, RepositorioFormularios repositorioFormularios,
                         RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias,
                         RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking,
                         RepositorioBienes repositorioBienes) {
    this.gestorDonantes = gestorDonantes;
    this.gestorDonaciones = gestorDonaciones;
      this.gestorAsignaciones = gestorAsignaciones;
      this.gestorFormulario = gestorFormulario;
    this.gestorMatchmaking = gestorMatchmaking;
    this.gestorNecesidades = gestorNecesidades;
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioDonantes = repositorioDonantes;
      this.repositorioFormularios = repositorioFormularios;
      this.repositorioEntidadesBeneficiarias = repositorioEntidadesBeneficiarias;
    this.repositorioDeResultadosMatchmaking = repositorioDeResultadosMatchmaking;
    this.repositorioBienes = repositorioBienes;
  }

  public List<DonacionDTO> obtenerTodas() {
    return repositorioDonaciones.obtenerTodos().stream()
                           .map(DonacionDTO::from).collect(Collectors.toList());
  }

  public DonacionDTO obtenerPorId(UUID id) {
    return DonacionDTO.from(repositorioDonaciones.obtenerPorId(id).orElseThrow(() -> new IllegalArgumentException("No se encontró la donación")));
  }

  public List<DonacionDTO> procesarFormulario(FormularioRequestDTO request) {
    Donante donante = repositorioDonantes.buscarPorId(request.getIdDonante()).orElse(null);
    if (donante == null) throw new NullPointerException("No se encontró persona con ese ID");

    List<Bien> bienesNormal = request.getBienes() != null ? request.getBienes().stream().map(this::resolverBien).collect(Collectors.toList()) : List.of();
    bienesNormal.forEach(this::crearBien);

    Formulario formularioGenerado = new Formulario(donante, bienesNormal, request.getFechaRealizacion());
    repositorioFormularios.guardar(formularioGenerado);
    List<Donacion> donacionesProcesadas = gestorFormulario.procesarFormulario(formularioGenerado);

    repositorioDonaciones.guardarDonaciones(donacionesProcesadas);
    gestorDonantes.agregarFormularioADonante(donante.getId(), formularioGenerado);

    return donacionesProcesadas.stream().map(DonacionDTO::from).collect(Collectors.toList());
  }

  public void ejecutarMatchmakingADemanda() {
    AsignadorDonaciones asignadorDonaciones = new AsignadorDonaciones(gestorMatchmaking,gestorDonaciones,repositorioDeResultadosMatchmaking);
    List<Donacion> donacionesNoAsignadas = repositorioDonaciones.buscarDonacionesSinAsignar();
    List<EntidadBeneficiaria> entidades = repositorioEntidadesBeneficiarias.obtenerTodas();
    asignadorDonaciones.ejecutarMatchmakingBatch(donacionesNoAsignadas,entidades);
  }

  // A diferencia de dto.toDomain() (que sólo completa id/descripcion/bienes, dejando
  // donante/entidad/estado/subcategoria/fechaEntrega en null), acá partimos de la Donacion
  // existente y sólo pisamos los campos que vienen en el DTO. Con persistencia real (merge()),
  // guardar un objeto mayormente-null hubiera nuleado esas columnas en la base.
  public DonacionDTO actualizarDonacion(UUID id, DonacionDTO dto) {
    Donacion existente = repositorioDonaciones.obtenerPorId(id)
            .orElseThrow(() -> new RuntimeException("Donación no encontrada con ID: " + id));

    if (dto.getDescripcion() != null) {
      existente.setDescripcion(dto.getDescripcion());
    }
    if (dto.getBienes() != null) {
      List<Bien> bienesActualizados = dto.getBienes().stream().map(this::resolverBien).collect(Collectors.toList());
      bienesActualizados.forEach(this::crearBien);
      existente.setBienes(bienesActualizados);
    }

    return DonacionDTO.from(repositorioDonaciones.actualizar(existente.getId(), existente).get());
  }

  public void eliminarDonacion(UUID id) {
    repositorioDonaciones.eliminarPorId(id);
  }

  public DonacionDTO cambiarEstado(UUID id, String nuevoEstado, String justificacion) {
    return DonacionDTO.from(gestorDonaciones.cambiarEstado(id, nuevoEstado, justificacion));
  }

  public DonacionDTO marcarComoVencida(UUID id) {
    return DonacionDTO.from(gestorDonaciones.cambiarEstado(id, "VENCIDA", "Registrado como vencido por la administración."));
  }

  public List<ResultadoMatchmakingDTO> obtenerTodosLosResultadosMatchmaking() {
    return repositorioDeResultadosMatchmaking.findAll().stream()
                            .map(ResultadoMatchmakingDTO::from).collect(Collectors.toList());
  }

  public void asignarPropuesta(UUID donacionId, Integer posicion) {
    PropuestaAsignacion propuestaAsignacion = gestorMatchmaking.obtenerPropuestaSeleccionadaParaDonacion(donacionId, posicion);
    Donacion donacion = repositorioDonaciones.obtenerPorId(donacionId).orElseThrow(() -> new IllegalArgumentException("No se encontró la donación"));
    gestorAsignaciones.asignarPropuesta(donacion, propuestaAsignacion);
    eliminarResultadoMatchmaking(donacion.getId());
    gestorDonaciones.cambiarEstado(donacion.getId(), "ASIGNADO", "Donacion Asignada");
  }

  // Resuelve (o crea) la SubcategoriaBien del catálogo compartido ANTES de construir el Bien,
  // mismo patrón que usamos para Necesidad, para no romper merge() ni duplicar el catálogo.
  private Bien resolverBien(BienResumenDTO dto) {
    SubcategoriaBien subcategoria = gestorNecesidades.obtenerOCrearSubcategoria(dto.getCategoria(), dto.getSubcategoria());
    return dto.toDomain(subcategoria);
  }

  private void crearBien(Bien nuevoBien) {
    try {
      repositorioBienes.guardar(nuevoBien);
      System.out.println("Bien registrado con éxito con ID: " + nuevoBien.getId());
    } catch (IllegalArgumentException e) {
      System.err.println("Error al registrar bien: " + e.getMessage());
    }
  }

  private void eliminarResultadoMatchmaking(UUID donacionId){
    ResultadoMatchmaking resultado = repositorioDeResultadosMatchmaking.findByDonacionId(donacionId).orElseThrow(() -> new IllegalArgumentException(
                    "No hay resultado de matchmaking para la donación " + donacionId
            )
    );
    repositorioDeResultadosMatchmaking.eliminarResultado(resultado);
  }
}