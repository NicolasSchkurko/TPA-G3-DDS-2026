package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IncentivosDonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.mappers.DonacionMapper;
import ar.edu.utn.frba.ddsi.donaciones.mappers.MatchmakingMapper;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.DonacionFacade;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.SegmentadorDonaciones.SegmentadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.GestorNotificacionesEventos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDeResultadosMatchmaking;
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
  private final RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking;
  private final MatchmakingMapper mapperMatchmaking;
  private final DonacionMapper mapperDonacion;
//  private final AsignadorDonaciones asignador;
//  private final SegmentadorDonaciones segmentador;


  public DonacionService(RepositorioDonaciones repositorio,
                         RepositorioFormularios repositorioFormularios,
                         IncentivosClient incentivosClient,
                         RepositorioEntidadesBeneficiarias repositorioEntidades,
                         GestorNotificacionesEventos gestorNotificaciones, RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking,
                         MatchmakingMapper mapperMatchmaking,
                         DonacionMapper mapperDonacion
//                         AsignadorDonaciones asignador,
//                         SegmentadorDonaciones segmentador
                          ) {
    this.repositorio = repositorio;
    this.repositorioFormularios = repositorioFormularios;
    this.incentivosClient = incentivosClient;
    this.repositorioEntidades = repositorioEntidades;
    this.gestorNotificaciones = gestorNotificaciones;
    this.repositorioDeResultadosMatchmaking = repositorioDeResultadosMatchmaking;
    this.mapperMatchmaking= mapperMatchmaking;
    this.mapperDonacion =mapperDonacion;
  //  this.asignador = asignador;
  //  this.segmentador = segmentador;
  }

  public List<DonacionDTO> obtenerTodas() {
    return repositorio.findAll().stream()
                      .map(mapperDonacion::donaciontoDTO)
                      .collect(Collectors.toList());

  }

  public Optional<DonacionDTO> obtenerPorId(UUID id) {
    return repositorio.findById(id).map(mapperDonacion::donaciontoDTO);
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

    DonacionFacade donacionFacade = new DonacionFacade(new SegmentadorDonaciones(),
            new AsignadorDonaciones());

    donacionFacade.ejecutarAsignador(donacionesNoAsignadas, entidades);
    List<ResultadoMatchmaking> resultadosMatchmakings = donacionFacade.obtenerDonacionesPendientesDeAprobacion();
    repositorioDeResultadosMatchmaking.guardarResultados(resultadosMatchmakings);
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

      if (nuevoEstado == Estado.ASIGNADO) {
        IncentivosDonacionDTO dto = new IncentivosDonacionDTO();
        dto.setFechaEntrega(donacion.getFechaEntrega());
        dto.setCantidadBienes(donacion.sumaCantidadBienes());
        dto.setSubCategoria(donacion.getSubcategoria().getNombre());
        dto.setCategoria(donacion.getSubcategoria().getCategoria().getNombre());
        dto.setEntidadBeneficiaria(donacion.getEntidad().getRazonSocial());
        dto.setEstado(nuevoEstado.name());
        incentivosClient.notificarDonacionAsignada(donacion.getDonante().getId(), dto);
        gestorNotificaciones.notificarDonacionAsignadaAEntidadBeneficiaria(donacion);
      }

      return donacion;
    }

    throw new RuntimeException("Donación no encontrada con ID: " + id);
  }

  public List<ResultadoMatchmakingDTO> obtenerTodosLosResultadosMatchmaking() {
        return repositorioDeResultadosMatchmaking.findAll()
                .stream()
                .map(mapperMatchmaking::ResultadoToDTO)
                .toList();
  }


    public void asignarPropuesta(UUID donacionId, Integer posicion) {
        //Buscar resultado
        ResultadoMatchmaking resultado = repositorioDeResultadosMatchmaking.findByDonacionId(donacionId).orElseThrow(() -> new IllegalArgumentException(
                                        "No hay resultado de matchmaking para la donación " + donacionId
                                )
                        );


        if (posicion == null || posicion < 0 || posicion >= resultado.getPropuestasOrdenadas().size()) {
            throw new IllegalArgumentException("Posición de propuesta inválida");
        }

        PropuestaAsignacion propuesta = resultado.getPropuestasOrdenadas().get(posicion);

        Donacion donacion = resultado.getDonacion();

        if (donacion.getEstado()!=Estado.PENDIENTE_ASIGNACION) {
            throw new IllegalStateException("La donación ya está asignada");
        }

        AsignadorDonaciones.asignarDonacionAPropuesta(donacion,propuesta);
        DonacionDTO donacionAGuardar = mapperDonacion.donaciontoDTO(donacion);
        repositorio.actualizar(donacionId, donacionAGuardar);
        repositorioEntidades.save(donacion.getEntidad());
        repositorioDeResultadosMatchmaking.eliminarResultado(resultado);
    }

}
