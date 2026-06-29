package ar.edu.utn.frba.ddsi.notificaciones.gateways;

import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionPayload;

public interface NotificacionGateway {
    void enviar(NotificacionPayload payload);
}
