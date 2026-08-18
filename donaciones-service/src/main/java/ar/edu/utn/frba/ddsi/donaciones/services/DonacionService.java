package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IncentivosDonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.gestores.GestorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.gestores.GestorDonantes;
import ar.edu.utn.frba.ddsi.donaciones.gestores.GestorEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.DonacionFacade;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.SegmentadorDonaciones.SegmentadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DonacionService {

  private final RepositorioDonaciones repositorio;
  private final RepositorioFormularios repositorioFormularios;
  private final GestorEntidadesBeneficiarias gestorEntidades;
  private final IncentivosClient incentivosClient;
  private final FabricaEstrategiasNotificacion fabricaEstrategiasNotificacion;
  private final RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking;
  private final GestorDonantes gestorDonantes;
  private final GestorDonaciones gestorDonaciones;

  public DonacionService(RepositorioDonaciones repositorio,
                         RepositorioFormularios repositorioFormularios,
                         IncentivosClient incentivosClient,
                         GestorEntidadesBeneficiarias gestorEntidades,
                         FabricaEstrategiasNotificacion fabricaEstrategiasNotificacion,
                         RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking,
                         GestorDonantes gestorDonantes,
                         GestorDonaciones gestorDonaciones) {
    this.repositorio = repositorio;
    this.repositorioFormularios = repositorioFormularios;
    this.incentivosClient = incentivosClient;
    this.gestorEntidades = gestorEntidades;
    this.fabricaEstrategiasNotificacion = fabricaEstrategiasNotificacion;
    this.repositorioDeResultadosMatchmaking = repositorioDeResultadosMatchmaking;
    this.gestorDonantes = gestorDonantes;
    this.gestorDonaciones = gestorDonaciones;
  }

  public List<Donacion> obtenerTodas() {
    return repositorio.findAll();
  }

  public Optional<Donacion> obtenerPorId(UUID id) {
    return repositorio.findById(id);
  }

  public List<Donacion> procesarFormulario(UUID idDonante, List<Bien> bienesNormal, LocalDate fechaRealizacion) {

    Donante donante = gestorDonantes.obtenerDonante(idDonante);
    if (donante == null) {
      throw new NullPointerException("No se encontró persona con ese ID");
    }

    Formulario formulario = new Formulario(donante, bienesNormal, fechaRealizacion);
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
    List<EntidadBeneficiaria> entidades = gestorEntidades.listarTodasLasEntidades();

    gestorDonaciones.asignarDonaciones(donacionesNoAsignadas, entidades);
  }

  public Donacion actualizarDonacion(UUID id, Donacion actualizacion) {
    Optional<Donacion> existente = repositorio.findById(id);
    if (existente.isPresent()) {
      return repositorio.actualizar(existente.get().getId(), actualizacion);
    }
    throw new RuntimeException("Donación no encontrada con ID: " + id);
  }

  public void eliminarDonacion(UUID id) {
    repositorio.deleteById(id);
  }

  public Donacion cambiarEstado(UUID id, String nuevoEstado, String justificacion) {
    return gestorDonaciones.cambiarEstado(id, nuevoEstado, justificacion);
  }

  public List<ResultadoMatchmaking> obtenerTodosLosResultadosMatchmaking() {
    return repositorioDeResultadosMatchmaking.findAll();
  }

  public void asignarPropuesta(UUID donacionId, Integer posicion) {
    //Buscar resultado y asignar
    ResultadoMatchmaking resultado = repositorioDeResultadosMatchmaking.findByDonacionId(donacionId).orElseThrow(() -> new IllegalArgumentException(
                    "No hay resultado de matchmaking para la donación " + donacionId
            )
    );

    if (posicion == null || posicion < 0 || posicion >= resultado.getPropuestasOrdenadas().size()) {
      throw new IllegalArgumentException("Posición de propuesta inválida");
    }

    PropuestaAsignacion propuesta = resultado.getPropuestasOrdenadas().get(posicion);

    Donacion donacion = resultado.getDonacion();

    if (donacion.getEstado() != Estado.PENDIENTE_ASIGNACION) {
      throw new IllegalStateException("La donación ya está asignada");
    }

    AsignadorDonaciones.asignarDonacionAPropuesta(donacion, propuesta);
    repositorio.actualizar(donacionId, donacion);

    // Uso del Gestor de Entidades para actualizar la entidad si es necesario
    gestorEntidades.modificarEntidad(donacion.getEntidad().getId(), donacion.getEntidad());

    repositorioDeResultadosMatchmaking.eliminarResultado(resultado);
  }

}