package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IncentivosDonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDeResultadosMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioNecesidades;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class GestorAsignaciones {
    private NotificacionesClient notificacionesClient;
    private RepositorioDonaciones repositorioDonaciones;
    private RepositorioNecesidades repositorioNecesidades;
    private IncentivosClient incentivosClient;
    private FabricaEstrategiasNotificacion fabricaEstrategiasNotificacion;

    public GestorAsignaciones(NotificacionesClient notificacionesClient,
                              RepositorioDonaciones repositorioDonaciones,
                              RepositorioNecesidades repositorioNecesidades,
                              IncentivosClient incentivosClient,
                              FabricaEstrategiasNotificacion fabricaEstrategiasNotificacion) {
        this.notificacionesClient = notificacionesClient;
        this.repositorioDonaciones = repositorioDonaciones;
        this.repositorioNecesidades = repositorioNecesidades;
        this.incentivosClient = incentivosClient;
        this.fabricaEstrategiasNotificacion = fabricaEstrategiasNotificacion;
    }

    public void asignarPropuesta(Donacion donacion, PropuestaAsignacion  propuesta) {
        asignarEntidad(donacion.getId(), propuesta.getEntidad());
        agregarDonacionANecesidad(propuesta.getNecesidad().getId(), donacion);
        notificarAsignacion(donacion);
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

        fabricaEstrategiasNotificacion.ejecutar(TipoEventoNotificacion.DONACION_ASIGNADA, donacion);
    }

    private void asignarEntidad(UUID donacionId, EntidadBeneficiaria entidad) {
        try {
            repositorioDonaciones.asignarEntidad(donacionId, entidad);
            System.out.println("Entidad asignada con éxito a la donación: " + donacionId);
        } catch (IllegalArgumentException e) {
            System.err.println("Error al asignar entidad: " + e.getMessage());
        }
    }

    //Lo agregué para poder registrar donaciones
    private void agregarDonacionANecesidad(UUID necesidadId, Donacion donacion) {
        try {
            repositorioNecesidades.agregarDonacion(necesidadId, donacion);
            System.out.println("Donación registrada con éxito en la necesidad: " + necesidadId);
        } catch (IllegalArgumentException e) {
            System.err.println("Error al registrar donación en necesidad: " + e.getMessage());
        }
    }

    private void notificarAsignacion(Donacion donacion) {
        try {
            if (donacion.getEntidad() != null && donacion.getEntidad().getPersonaJuridica() != null) {
                NotificacionDTO notifEntidad = new NotificacionDTO(
                        donacion.getEntidad().getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado().getTipo(),
                        donacion.getEntidad().getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado().getValor(),
                        "Se le ha asignado una nueva donación de la categoría: " + donacion.getSubcategoria().getNombre(), "Nueva Donación Asignada"
                );
                notificacionesClient.enviarNotificacion(notifEntidad);
            }
            if (donacion.getDonante() != null && donacion.getDonante().getPersona() != null) {
                NotificacionDTO notifDonante = getNotificacionDTO(donacion);
                notificacionesClient.enviarNotificacion(notifDonante);
            }
        } catch (Exception e) { System.err.println("Error al enviar notificaciones asíncronas: " + e.getMessage()); }
    }

    @NotNull
    private static NotificacionDTO getNotificacionDTO(Donacion donacion) {
        String rsEntidad = (donacion.getEntidad() != null && donacion.getEntidad().getPersonaJuridica() != null) ? donacion.getEntidad().getPersonaJuridica().getRazonSocial() : "una Entidad Beneficiaria";
        NotificacionDTO notifDonante = new NotificacionDTO(
                donacion.getDonante().getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado().getTipo(),
                donacion.getDonante().getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado().getValor(),
                "Su donación ha sido asignada a " + rsEntidad, "Donación Asignada a Entidad"
        );
        return notifDonante;
    }
}
