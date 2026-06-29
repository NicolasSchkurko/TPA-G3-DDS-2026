package ar.edu.utn.frba.ddsi.notificaciones.gateways;

import ar.edu.utn.frba.ddsi.notificaciones.clientes.N8nClient;
import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionPayload;
import org.springframework.stereotype.Service;

@Service
public class N8nGateway implements NotificacionGateway {

    private final N8nClient client;

    public N8nGateway(N8nClient client) {
        this.client = client;
    }

    @Override
    public void enviar(NotificacionPayload payload) {
        client.enviarNotificacion(payload);
    }
}