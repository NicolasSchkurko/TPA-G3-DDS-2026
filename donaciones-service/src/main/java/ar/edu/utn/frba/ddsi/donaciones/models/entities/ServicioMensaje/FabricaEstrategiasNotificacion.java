package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provee la estrategia de notificación correspondiente a un tipo de evento.
 * Actúa como punto central de acceso a las estrategias registradas
 */

@Component
public class FabricaEstrategiasNotificacion {

    private final Map<TipoEventoNotificacion, EstrategiaNotificacion> estrategias;

    public FabricaEstrategiasNotificacion(
            List<EstrategiaNotificacion> estrategiasRegistradas) {

        this.estrategias = estrategiasRegistradas.stream()
                .collect(Collectors.toMap(
                        EstrategiaNotificacion::getTipoEvento,
                        Function.identity()
                ));

        // Fail-fast: si a una estrategia le falta el @Component (como pasaba con
        // NotificacionEntregaFallida), mejor romper al arrancar que la primera vez que se
        // dispare el evento en producción.
        for (TipoEventoNotificacion tipo : TipoEventoNotificacion.values()) {
            if (!this.estrategias.containsKey(tipo)) {
                throw new IllegalStateException(
                        "No hay una EstrategiaNotificacion registrada como bean para " + tipo);
            }
        }
    }

    public EstrategiaNotificacion obtenerEstrategia(TipoEventoNotificacion evento) {
        return estrategias.get(evento);
    }

    public void ejecutar(
            TipoEventoNotificacion evento,
            Object datos) {

        EstrategiaNotificacion estrategia = estrategias.get(evento);

        if (estrategia == null) {
            throw new IllegalArgumentException(
                    "No existe una estrategia para " + evento);
        }

        estrategia.ejecutar(datos);
    }
}
