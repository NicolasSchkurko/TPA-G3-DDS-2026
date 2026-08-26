package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IncentivosDonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.DonacionFacade;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.SegmentadorDonaciones.SegmentadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDeResultadosMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GestorDonaciones {
    private RepositorioDonaciones repositorioDonaciones;
    private RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking;
    private DonacionFacade donacionFacade;
    private IncentivosClient incentivosClient;
    private FabricaEstrategiasNotificacion fabricaEstrategiasNotificacion;

    public List<Donacion> obtenerTodasLasDonaciones() {
        return repositorioDonaciones.obtenerTodos();
    }

    public Optional<Donacion> obtenerDonacionPorId(UUID id) {
        return repositorioDonaciones.obtenerPorId(id);
    }

    public void eliminarDonacion(UUID id) {
        repositorioDonaciones.eliminarPorId(id);
    }

    public List<Donacion> listarPendientes() {
        return repositorioDonaciones.buscarEntregaPendiente();
    }

    public Donacion cambiarEstado(UUID id, String nuevoEstado, String justificacion) {
        Optional<Donacion> donacionOpt = repositorioDonaciones.obtenerPorId(id);

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
            repositorioDonaciones.guardar(donacion);

            if (nuevoEstado.toUpperCase().equals("ASIGNADO")) {
                IncentivosDonacionDTO dto = new IncentivosDonacionDTO();
                dto.setFechaEntrega(donacion.getFechaEntrega());
                dto.setCantidadBienes(donacion.sumaCantidadBienes());
                dto.setSubCategoria(donacion.getSubcategoria().getNombre());
                dto.setCategoria(donacion.getSubcategoria().getCategoria().getNombre());
                dto.setEntidadBeneficiaria(donacion.getEntidad().getPersonaJuridica().getRazonSocial());
                dto.setEstado(nuevoEstado);
                incentivosClient.notificarDonacionAsignada(donacion.getDonante().getId(), dto);

                EstrategiaNotificacion estrategia = fabricaEstrategiasNotificacion.obtenerEstrategia(
                        TipoEventoNotificacion.DONACION_ASIGNADA);
                estrategia.ejecutar(donacion);
            }

            return donacion;
        }

        throw new RuntimeException("Donación no encontrada con ID: " + id);
    }

    public Donacion actualizarDonacion(UUID id, Donacion actualizacion) {
        Optional<Donacion> existente = repositorioDonaciones.obtenerPorId(id);
        if (existente.isPresent()) {
            return repositorioDonaciones.actualizar(existente.get().getId(), actualizacion).get();
        }
        throw new RuntimeException("Donación no encontrada con ID: " + id);
    }

    public void asignarDonaciones(List<Donacion> donacionesNoAsignadas, List<EntidadBeneficiaria> entidades) {
        DonacionFacade donacionFacade = new DonacionFacade(new SegmentadorDonaciones(),
                new AsignadorDonaciones());

        donacionFacade.ejecutarAsignador(donacionesNoAsignadas, entidades);
        List<ResultadoMatchmaking> resultadosMatchmakings = donacionFacade.obtenerDonacionesPendientesDeAprobacion();
        repositorioDeResultadosMatchmaking.guardarResultados(resultadosMatchmakings);
    }

    public void guardarDonaciones(List<Donacion> donaciones){
        repositorioDonaciones.guardarDonaciones(donaciones);
    }
}
