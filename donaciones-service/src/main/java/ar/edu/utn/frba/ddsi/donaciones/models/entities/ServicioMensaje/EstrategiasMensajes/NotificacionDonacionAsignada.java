package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiasMensajes;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaMensaje.java.EstrategiaMensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.ServicioNotificaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionDonacionAsignada extends EstrategiaMensaje {

    public NotificacionDonacionAsignada(
            ServicioNotificaciones servicioNotificaciones) {

        super(servicioNotificaciones);
    }

    @Override
    public TipoEventoNotificacion getTipoEvento() {
        return TipoEventoNotificacion.DONACION_ASIGNADA;
    }

    @Override
    public void ejecutar(Object datos) {

        Donacion donacion = (Donacion) datos;

        Mensaje mensaje = new Mensaje(
                "Nueva donación asignada",
                String.format(
                        "Se asignó una donación a la entidad %s. Donación: %s. Cantidad total de bienes: %d. Fecha de entrega: %s.",
                        donacion.getEntidad().getPersonaJuridica().getRazonSocial(),
                        valorOTexto(
                                donacion.getDescripcion(),
                                "sin descripción"
                        ),
                        donacion.sumaCantidadBienes(),
                        donacion.getFechaEntrega() != null
                                ? donacion.getFechaEntrega().toString()
                                : "sin fecha definida"
                ),
                TipoDeMensaje.CAMBIO_ESTADO
        );

        servicioNotificaciones.enviarNotificacionAMediosDeContacto(
                donacion.getEntidad().getPersonaJuridica().getMediosDeContacto(),
                mensaje
        );

        servicioNotificaciones.enviarNotificacionAMediosDeContacto(
                donacion.getDonante().getPersona().getMediosDeContacto(),
                mensaje
        );
    }
}
