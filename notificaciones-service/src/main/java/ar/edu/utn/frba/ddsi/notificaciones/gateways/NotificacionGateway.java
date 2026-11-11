package ar.edu.utn.frba.ddsi.notificaciones.gateways;

import ar.edu.utn.frba.ddsi.notificaciones.dto.n8n.NotificacionPayload;

public interface NotificacionGateway {
    void enviar(NotificacionPayload payload);
}
