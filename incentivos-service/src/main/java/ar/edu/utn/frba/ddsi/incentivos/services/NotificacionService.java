package ar.edu.utn.frba.ddsi.incentivos.services;

//q los import los haga intellij, estoy en el celu

@Service
public class NotificacionService {
    private final RestClient generalClient;

    public ProductoClientService(RestClient generalClient) {
        this.generalClient = generalClient;
    }

    public PerfilNotificacionDTO obtenerContactoDeNotificacionesPorId(UUID id) {
        return generalClient.get()
            .uri("http://localhost:8082/notificaciones/{id}", id)
            .retrieve()
            .body(PerfilNotificacionesDTO.class);
    }
}

}
