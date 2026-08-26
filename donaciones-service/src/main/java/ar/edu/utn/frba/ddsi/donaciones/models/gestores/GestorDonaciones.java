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
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GestorDonaciones {
    private RepositorioDonaciones repositorioDonaciones;
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

        if (donacionOpt.isEmpty()) {
            throw new RuntimeException("Donación no encontrada con ID: " + id);
        }

        Estado estado = parseEstado(nuevoEstado);

        Donacion donacion = donacionOpt.get();
        donacion.actualizarEstado(estado, justificacion);
        repositorioDonaciones.guardar(donacion);

        procesarAccionesPostCambioEstado(donacion, estado, nuevoEstado);

        return donacion;
    }

    public Donacion actualizarDonacion(UUID id, Donacion actualizacion) {
        Optional<Donacion> existente = repositorioDonaciones.obtenerPorId(id);
        if (existente.isPresent()) {
            return repositorioDonaciones.actualizar(existente.get().getId(), actualizacion).get();
        }
        throw new RuntimeException("Donación no encontrada con ID: " + id);
    }

    public List<ResultadoMatchmaking> asignarDonaciones(List<Donacion> donacionesNoAsignadas, List<EntidadBeneficiaria> entidades) {
        DonacionFacade donacionFacade = new DonacionFacade(new SegmentadorDonaciones(),
                new AsignadorDonaciones());

        donacionFacade.ejecutarAsignador(donacionesNoAsignadas, entidades);
        return donacionFacade.obtenerDonacionesPendientesDeAprobacion();
    }

    private Estado parseEstado(String nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo");
        }

        switch (nuevoEstado.trim().toUpperCase()) {
            case "EN_DEPOSITO":
                return Estado.EN_DEPOSITO;
            case "PENDIENTE_ASIGNACION":
                return Estado.PENDIENTE_ASIGNACION;
            case "ENTREGADO":
                return Estado.ENTREGADO;
            case "VENCIDO":
                return Estado.VENCIDO;
            case "ASIGNADO":
                // Si tu enum tiene ASIGNADO, devuelve Estado.ASIGNADO; si no, quita esta línea.
                return Estado.ASIGNADO;
            default:
                throw new IllegalArgumentException("Estado desconocido: " + nuevoEstado);
        }
    }

    private void procesarAccionesPostCambioEstado(Donacion donacion, Estado estado, String nuevoEstado) {
        boolean esAsignado = "ASIGNADO".equalsIgnoreCase(nuevoEstado) || estado == Estado.ASIGNADO;
        if (!esAsignado) return;

        IncentivosDonacionDTO dto = new IncentivosDonacionDTO();
        dto.setFechaEntrega(donacion.getFechaEntrega());
        dto.setCantidadBienes(donacion.sumaCantidadBienes());
        dto.setSubCategoria(donacion.getSubcategoria().getNombre());
        dto.setCategoria(donacion.getSubcategoria().getCategoria().getNombre());
        dto.setEntidadBeneficiaria(donacion.getEntidad().getPersonaJuridica().getRazonSocial());
        dto.setEstado(nuevoEstado);

        // Nota: asumo que `incentivosClient` está correctamente inyectado en la clase
        incentivosClient.notificarDonacionAsignada(donacion.getDonante().getId(), dto);

        EstrategiaNotificacion estrategia = fabricaEstrategiasNotificacion.obtenerEstrategia(
            TipoEventoNotificacion.DONACION_ASIGNADA);
        estrategia.ejecutar(donacion);
    }
    public void guardarDonaciones(List<Donacion> donaciones){
        repositorioDonaciones.guardarDonaciones(donaciones);
    }

}
