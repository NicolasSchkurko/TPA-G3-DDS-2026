package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDeResultadosMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioNecesidades;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GestorAsignaciones {
    private NotificacionesClient notificacionesClient;
    private RepositorioDonaciones repositorioDonaciones;
    private RepositorioNecesidades repositorioNecesidades;

    public GestorAsignaciones(NotificacionesClient notificacionesClient,
                              RepositorioDonaciones repositorioDonaciones,
                              RepositorioNecesidades repositorioNecesidades) {
        this.notificacionesClient = notificacionesClient;
        this.repositorioDonaciones = repositorioDonaciones;
        this.repositorioNecesidades = repositorioNecesidades;
    }

    public void asignarPropuesta(Donacion donacion, PropuestaAsignacion  propuesta) {
        asignarEntidad(donacion.getId(), propuesta.getEntidad());
        agregarDonacionANecesidad(propuesta.getNecesidad().getId(), donacion);
        notificarAsignacion(donacion);
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
