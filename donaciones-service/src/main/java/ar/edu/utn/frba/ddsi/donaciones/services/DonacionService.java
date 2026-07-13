package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.BienResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IncentivosDonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.mappers.DonacionMapper;
import ar.edu.utn.frba.ddsi.donaciones.mappers.MatchmakingMapper;
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
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.*;
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
  private final FabricaEstrategiasNotificacion fabricaEstrategiasNotificacion;
  private final RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking;
  private final MatchmakingMapper mapperMatchmaking;
  private final DonacionMapper mapperDonacion;
  private final RepositorioDePersonas repositorioDePersonas;
//  private final AsignadorDonaciones asignador;
//  private final SegmentadorDonaciones segmentador;


  public DonacionService(RepositorioDonaciones repositorio,
                         RepositorioFormularios repositorioFormularios,
                         IncentivosClient incentivosClient,
                         RepositorioEntidadesBeneficiarias repositorioEntidades,
                         FabricaEstrategiasNotificacion fabricaEstrategiasNotificacion,
                         RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking,
                         MatchmakingMapper mapperMatchmaking,
                         DonacionMapper mapperDonacion,
                         RepositorioDePersonas repositorioDePersonas
//                         AsignadorDonaciones asignador,
//                         SegmentadorDonaciones segmentador
                          ) {
    this.repositorio = repositorio;
    this.repositorioFormularios = repositorioFormularios;
    this.incentivosClient = incentivosClient;
    this.repositorioEntidades = repositorioEntidades;
    this.fabricaEstrategiasNotificacion = fabricaEstrategiasNotificacion;
    this.repositorioDeResultadosMatchmaking = repositorioDeResultadosMatchmaking;
    this.mapperMatchmaking= mapperMatchmaking;
    this.mapperDonacion =mapperDonacion;
    this.repositorioDePersonas=repositorioDePersonas;
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

    public List<Donacion> procesarFormulario(UUID idDonante, List<BienResumenDTO> bienes, LocalDate fechaRealizacion) {

        Optional<PersonaDonante> donanteOptional = repositorioDePersonas.findById(idDonante);
        if (donanteOptional.isEmpty()) {
            throw new NullPointerException("no se encontro persona con ese id");
        }
        PersonaDonante donante = donanteOptional.get();

        List<Bien> bienesNormal = this.maptodto(bienes);

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

    private List<Bien> maptodto(List<BienResumenDTO> bienes) {
        List<Bien> b = new ArrayList<>();

        for (BienResumenDTO x : bienes) {
            Bien bienNormal;
            switch (x.getTipoBien().toUpperCase()) {
                case "CON_ESTADO":
                    CategoriaBien a = new CategoriaBien(x.getCategoria());
                    SubcategoriaBien p = new SubcategoriaBien(x.getSubcategoria(), a);
                    bienNormal = new BienConEstado(x.getDescripcion(),
                            p,
                            null,
                            x.getCantidad(),
                            mapToUM(x.getUnidadDeMedida()),
                            x.getUsado());
                    b.add(bienNormal);
                    break;
                case "PERECEDERO":
                    CategoriaBien d = new CategoriaBien(x.getCategoria());
                    SubcategoriaBien c = new SubcategoriaBien(x.getSubcategoria(), d);
                    bienNormal = new BienPerecedero(x.getDescripcion(),
                            c,
                            null,
                            x.getCantidad(),
                            mapToUM(x.getUnidadDeMedida()),
                            x.getFechaVencimiento());
                    b.add(bienNormal);
                    break;
            }
        }

        return b;
    }

    private UnidadDeMedida mapToUM(String unidad) {
        return unidad.toUpperCase().equals("KILOGRAMOS") ? UnidadDeMedida.KILOGRAMOS : UnidadDeMedida.LITROS;
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

    public Donacion cambiarEstado(UUID id, String nuevoEstado, String justificacion) {
        Optional<Donacion> donacionOpt = repositorio.findById(id);

        if (donacionOpt.isPresent()) {
            Estado e = null;

            switch (nuevoEstado.toUpperCase()) {
                case "EN_DEPOSITO":
                    e = Estado.EN_DEPOSITO;
                    break;
                case "PENDIENTE_ASIGNACION":
                    e = Estado.PENDIENTE_ASIGNACION;
                    break;
                case "ENTREGADO":
                    e = Estado.ENTREGADO;
                    break;
                case "VENCIDO":
                    e = Estado.VENCIDO;
                    break;
            }

            Donacion donacion = donacionOpt.get();
            donacion.actualizarEstado(e, justificacion);
            repositorio.save(donacion);

      if (nuevoEstado.toUpperCase().equals("ASIGNADO")) {
        IncentivosDonacionDTO dto = new IncentivosDonacionDTO();
        dto.setFechaEntrega(donacion.getFechaEntrega());
        dto.setCantidadBienes(donacion.sumaCantidadBienes());
        dto.setSubCategoria(donacion.getSubcategoria().getNombre());
        dto.setCategoria(donacion.getSubcategoria().getCategoria().getNombre());
        dto.setEntidadBeneficiaria(donacion.getEntidad().getRazonSocial());
        dto.setEstado(nuevoEstado);
        incentivosClient.notificarDonacionAsignada(donacion.getDonante().getId(), dto);

        EstrategiaNotificacion estrategia = fabricaEstrategiasNotificacion.obtenerEstrategia(TipoEventoNotificacion.DONACION_ASIGNADA_ENTIDAD_BENEFICIARIA);
        estrategia.ejecutar(donacion);
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

        if (donacion.getEstado() != Estado.PENDIENTE_ASIGNACION) {
            throw new IllegalStateException("La donación ya está asignada");
        }

        AsignadorDonaciones.asignarDonacionAPropuesta(donacion, propuesta);
        DonacionDTO donacionAGuardar = mapperDonacion.donaciontoDTO(donacion);
        repositorio.actualizar(donacionId, donacionAGuardar);
        repositorioEntidades.save(donacion.getEntidad());
        repositorioDeResultadosMatchmaking.eliminarResultado(resultado);
    }

}
